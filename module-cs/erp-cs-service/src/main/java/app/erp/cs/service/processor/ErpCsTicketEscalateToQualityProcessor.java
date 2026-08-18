package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.qa.biz.IErpQaNonConformanceBiz;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCsTicket escalateToQuality per-mutation Processor（RC-R1.68 / P1-RC-057，UC-CS-06 工单升级为质量事件）。
 *
 * <p>双弱指针载体（B 类裁决，零 ORM 变更）：反向 = NCR {@code sourceType=CS_TICKET} +
 * {@code sourceCode=ticket.code}（qa orm 既有自由 VARCHAR 列）；正向 = {@code ErpCsTicketAction}
 * actionType=QUALITY_ESCALATE + content 携带 NCR code。残留风险：弱指针无 FK 强约束；ticket UK=(code,orgId)
 * 而 NCR 无 org 维度——跨组织同 code 工单理论上可交叉匹配（单组织部署无影响，owner doc 注记）。
 *
 * <p>quality 调用失败降级（L1 异常条款）：写 PENDING 审计行（content 携带重试自足载荷 JSON），
 * 不 rethrow——外层 @BizMutation 正常提交、工单状态保持；{@code ErpCsQualityEscalationRetryJob}
 * 扫描 PENDING 行经 {@link #retryPendingEscalation} 重试（创建前反查既有 NCR 防 crash 间隙重复创建）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCsTicketEscalateToQualityProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpCsTicketEscalateToQualityProcessor.class);

    /** 成功审计行 content 前缀（L1 ③ 关联 NCR 编号）。 */
    public static final String CONTENT_NCR_PREFIX = "NCR:";

    /** 重试队列审计行 content 前缀 + 载荷 JSON（defectDescription 截断上限，content 列 domain=content 2000 内）。 */
    public static final String CONTENT_PENDING_PREFIX = "PENDING:";
    static final int PENDING_DESCRIPTION_MAX_LENGTH = 1000;

    /** 重试计数后缀（追加在 PENDING content 尾部）。 */
    static final String RETRY_SUFFIX = "#retry=";
    static final Pattern RETRY_SUFFIX_PATTERN = Pattern.compile("#retry=(\\d+)$");

    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpQaNonConformanceBiz qaNcrBiz;

    public void setTicketActionBiz(IErpCsTicketActionBiz ticketActionBiz) {
        this.ticketActionBiz = ticketActionBiz;
    }

    public void setQaNcrBiz(IErpQaNonConformanceBiz qaNcrBiz) {
        this.qaNcrBiz = qaNcrBiz;
    }

    /**
     * 工单升级为质量事件（UC-CS-06 ①-④）。守卫链：config 门控 → IN_PROCESS（L1 前置）→
     * materialId/defectDescription 必填；成功写 QUALITY_ESCALATE 审计行（content=NCR:{code}），
     * 工单不改状态（L1 ④ NCR 流程独立）；quality 调用失败降级 PENDING 审计行（工单保持）。
     */
    public ErpCsTicket escalateToQuality(ErpCsTicket ticket, Long materialId, String defectDescription,
                                         String batchInfo, BigDecimal quantity, String severity,
                                         Long supplierId, IServiceContext context) {
        if (!ErpCsConfigs.isQualityEscalationEnabled()) {
            throw new NopException(ErpCsErrors.ERR_CS_QUALITY_ESCALATION_DISABLED)
                    .param(ErpCsErrors.ARG_TICKET_ID, ticket.getId());
        }
        String from = ticket.getStatus();
        if (!ErpCsConstants.TICKET_STATUS_IN_PROGRESS.equals(from)) {
            throw illegalTransition(ticket, from);
        }
        if (materialId == null) {
            throw paramRequired(ticket, "materialId");
        }
        if (StringHelper.isBlank(defectDescription)) {
            throw paramRequired(ticket, "defectDescription");
        }
        String effectiveSeverity = StringHelper.isBlank(severity)
                ? ErpCsConstants.QUALITY_SEVERITY_NORMAL : severity.trim();
        try {
            ErpQaNonConformance ncr = createNcr(ticket, materialId, defectDescription.trim(), batchInfo,
                    quantity, effectiveSeverity, supplierId, context);
            writeAction(ticket, ErpCsConstants.ACTION_TYPE_QUALITY_ESCALATE, from, from,
                    CONTENT_NCR_PREFIX + ncr.getCode(), context);
        } catch (Exception e) {
            // L1 异常条款：quality 域服务不可用 → 延迟创建 NCR，工单先保留状态，后台自动重试。
            // 不 rethrow（外层 @BizMutation 正常提交，PENDING 行与工单状态同事务落库）
            LOG.warn("cs-quality-escalation-degraded-pending: ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
            writeAction(ticket, ErpCsConstants.ACTION_TYPE_QUALITY_ESCALATE, from, from,
                    buildPendingContent(materialId, defectDescription.trim(), batchInfo, quantity,
                            effectiveSeverity, supplierId),
                    context);
        }
        return ticket;
    }

    /**
     * 重试单条 PENDING 审计行（D4）：解析载荷 → 创建前反查既有 NCR（sourceType+sourceCode，
     * 防 crash 间隙重复创建）→ 无则创建 → 原行 content 修正为 NCR:{code}；创建失败自增 #retry 计数。
     * 返回 true = 成功（含既有 NCR 幂等命中）。
     */
    public boolean retryPendingEscalation(ErpCsTicket ticket, ErpCsTicketAction pendingAction,
                                          IServiceContext context) {
        Map<String, Object> payload = parsePendingPayload(pendingAction.getContent());
        if (payload == null || ticket == null) {
            return false;
        }
        int attempted = parseRetryCount(pendingAction.getContent());
        if (attempted >= ErpCsConfigs.getQualityRetryMax()) {
            LOG.warn("cs-quality-escalation-retry-exceeded: ticketId={}, attempted={}, max={}",
                    pendingAction.getTicketId(), attempted, ErpCsConfigs.getQualityRetryMax());
            return false;
        }
        ErpQaNonConformance ncr = findExistingNcr(ticket.getCode(), context);
        if (ncr == null) {
            try {
                ncr = createNcr(ticket, asLong(payload.get("materialId")),
                        asText(payload.get("defectDescription")), asText(payload.get("batchInfo")),
                        asDecimal(payload.get("quantity")), asText(payload.get("severity")),
                        asLong(payload.get("supplierId")), context);
            } catch (Exception e) {
                LOG.warn("cs-quality-escalation-retry-failed: ticketId={}, attempted={}, reason={}",
                        pendingAction.getTicketId(), attempted, e.getMessage());
                bumpRetryCount(pendingAction, context);
                return false;
            }
        }
        pendingAction.setContent(CONTENT_NCR_PREFIX + ncr.getCode());
        ticketActionBiz.updateEntity(pendingAction, null, context);
        return true;
    }

    /** 工单关联 NCR 闭环结果投影（UC-CS-06 ⑤，经 qaNcrBiz 弱指针反查）。 */
    public List<Map<String, Object>> findQualityNcrs(ErpCsTicket ticket, IServiceContext context) {
        List<ErpQaNonConformance> ncrs = qaNcrBiz.findList(buildNcrLookupQuery(ticket.getCode()), null, context);
        return ncrs.stream().map(this::toNcrSummary).collect(java.util.stream.Collectors.toList());
    }

    // ---------- helpers ----------

    /**
     * 经 {@link IErpQaNonConformanceBiz#save} data map 创建 NCR（R1.31 save-map 先例）。
     * NCR code 显式构造 NCR-CS-{ticket.code}（镜像 quality 域 SPC 级联先例 SpcOutOfControlHandler
     * 的 NCR-SPC-{chart}-{subgroup} 显式 setCode 模式）。
     */
    protected ErpQaNonConformance createNcr(ErpCsTicket ticket, Long materialId, String defectDescription,
                                            String batchInfo, BigDecimal quantity, String severity,
                                            Long supplierId, IServiceContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "NCR-CS-" + ticket.getCode());
        data.put("ncrDate", CoreMetrics.today());
        data.put("sourceType", ErpCsConstants.NCR_SOURCE_TYPE_CS_TICKET);
        data.put("sourceCode", ticket.getCode());
        data.put("materialId", materialId);
        String description = defectDescription;
        if (!StringHelper.isBlank(batchInfo)) {
            description = description + "；批次：" + batchInfo.trim();
        }
        data.put("description", description);
        if (quantity != null) {
            data.put("quantity", quantity);
        }
        data.put("severity", StringHelper.isBlank(severity) ? ErpCsConstants.QUALITY_SEVERITY_NORMAL : severity);
        data.put("status", ErpCsConstants.NCR_STATUS_OPEN);
        if (supplierId != null) {
            data.put("supplierId", supplierId);
        }
        return qaNcrBiz.save(data, context);
    }

    /** 弱指针反查（sourceType=CS_TICKET + sourceCode=ticket.code）。 */
    protected ErpQaNonConformance findExistingNcr(String ticketCode, IServiceContext context) {
        List<ErpQaNonConformance> found = qaNcrBiz.findList(buildNcrLookupQuery(ticketCode), null, context);
        return found.isEmpty() ? null : found.get(0);
    }

    private static QueryBean buildNcrLookupQuery(String ticketCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceType", ErpCsConstants.NCR_SOURCE_TYPE_CS_TICKET));
        q.addFilter(eq("sourceCode", ticketCode));
        q.setLimit(1);
        return q;
    }

    private Map<String, Object> toNcrSummary(ErpQaNonConformance ncr) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", ncr.getCode());
        m.put("status", ncr.getStatus());
        m.put("severity", ncr.getSeverity());
        m.put("ncrDate", ncr.getNcrDate());
        m.put("resolvedAt", ncr.getResolvedAt());
        m.put("resolution", ncr.getResolution());
        return m;
    }

    /** PENDING 载荷（自足重建 NCR 参数；defectDescription 截断为摘要，content 列 2000 内）。 */
    static String buildPendingContent(Long materialId, String defectDescription, String batchInfo,
                                      BigDecimal quantity, String severity, Long supplierId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("materialId", materialId);
        payload.put("defectDescription", truncate(defectDescription, PENDING_DESCRIPTION_MAX_LENGTH));
        if (!StringHelper.isBlank(batchInfo)) {
            payload.put("batchInfo", batchInfo.trim());
        }
        if (quantity != null) {
            payload.put("quantity", quantity);
        }
        payload.put("severity", StringHelper.isBlank(severity) ? ErpCsConstants.QUALITY_SEVERITY_NORMAL : severity);
        if (supplierId != null) {
            payload.put("supplierId", supplierId);
        }
        return CONTENT_PENDING_PREFIX + JsonTool.serialize(payload, true);
    }

    /** 解析 PENDING content 为载荷 map；非 PENDING 前缀或 JSON 损坏返回 null。 */
    static Map<String, Object> parsePendingPayload(String content) {
        if (content == null || !content.startsWith(CONTENT_PENDING_PREFIX)) {
            return null;
        }
        String json = stripRetrySuffix(content).substring(CONTENT_PENDING_PREFIX.length());
        try {
            return JsonTool.parseBeanFromText(json, Map.class);
        } catch (Exception e) {
            return null;
        }
    }

    /** 解析 PENDING content 尾部 #retry={n} 计数；无后缀 = 0。 */
    static int parseRetryCount(String content) {
        if (content == null) {
            return 0;
        }
        Matcher m = RETRY_SUFFIX_PATTERN.matcher(content);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private static String stripRetrySuffix(String content) {
        return RETRY_SUFFIX_PATTERN.matcher(content).replaceFirst("");
    }

    private void bumpRetryCount(ErpCsTicketAction action, IServiceContext context) {
        int next = parseRetryCount(action.getContent()) + 1;
        action.setContent(stripRetrySuffix(action.getContent()) + RETRY_SUFFIX + next);
        ticketActionBiz.updateEntity(action, null, context);
    }

    private static String truncate(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen);
    }

    private static String asText(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static BigDecimal asDecimal(Object v) {
        if (v == null) {
            return null;
        }
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v instanceof Number) {
            return new BigDecimal(String.valueOf(v));
        }
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void writeAction(ErpCsTicket ticket, String actionType, String fromStatus, String toStatus,
                             String content, IServiceContext context) {
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticket.getId());
        action.setActionType(actionType);
        action.setFromStatus(fromStatus);
        action.setToStatus(toStatus);
        action.setContent(content);
        action.setOperatorId(context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    private NopException illegalTransition(ErpCsTicket ticket, String current) {
        return new NopException(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION)
                .param(ErpCsErrors.ARG_TICKET_CODE, ticket.getCode())
                .param(ErpCsErrors.ARG_CURRENT_STATUS, current)
                .param(ErpCsErrors.ARG_EXPECTED_STATUS, ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
    }

    private NopException paramRequired(ErpCsTicket ticket, String paramName) {
        return new NopException(ErpCsErrors.ERR_CS_QUALITY_ESCALATION_PARAM_REQUIRED)
                .param(ErpCsErrors.ARG_TICKET_ID, ticket.getId())
                .param(ErpCsErrors.ARG_PARAM_NAME, paramName);
    }
}
