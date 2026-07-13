
package app.erp.cs.service.entity;

import app.erp.cs.biz.IErpCsCannedResponseBiz;
import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.dao.entity.ErpCsCannedResponse;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.service.CannedResponseRenderer;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;

/**
 * 预设应答 BizModel（{@code canned-response.md} §一~§三）。
 *
 * <p>三个自定义方法：
 * <ul>
 *   <li>{@link #renderTemplate} — 系统变量解析 + 自定义变量覆盖 + {@link CannedResponseRenderer} 替换占位符。</li>
 *   <li>{@link #suggestForTicket} — 三级宏匹配（精确 > 类型 > 全局兜底）。</li>
 *   <li>{@link #applyCannedResponse} — 渲染 + usageCount+1 + TicketAction NOTE 审计。</li>
 * </ul>
 */
@BizModel("ErpCsCannedResponse")
public class ErpCsCannedResponseBizModel extends CrudBizModel<ErpCsCannedResponse> implements IErpCsCannedResponseBiz {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @Inject
    IErpCsTicketBiz ticketBiz;
    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;

    public ErpCsCannedResponseBizModel() {
        setEntityName(ErpCsCannedResponse.class.getName());
    }

    public void setTicketBiz(IErpCsTicketBiz ticketBiz) {
        this.ticketBiz = ticketBiz;
    }

    public void setTicketActionBiz(IErpCsTicketActionBiz ticketActionBiz) {
        this.ticketActionBiz = ticketActionBiz;
    }

    public void setMdPartnerBiz(IErpMdPartnerBiz mdPartnerBiz) {
        this.mdPartnerBiz = mdPartnerBiz;
    }

    // ===================== Phase 1：渲染 + 宏匹配 =====================

    @Override
    @BizQuery
    public String renderTemplate(@Name("cannedResponseId") Long cannedResponseId,
                                 @Name("ticketId") Long ticketId,
                                 @Optional @Name("customVariables") Map<String, String> customVariables,
                                 IServiceContext context) {
        ErpCsCannedResponse resp = requireCannedResponse(cannedResponseId, context);
        assertActive(resp);
        Map<String, String> systemVars = resolveSystemVars(resp, ticketId, context);
        return CannedResponseRenderer.render(resp.getContent(), resp.getVariableDefs(), systemVars, customVariables);
    }

    @Override
    @BizQuery
    public List<ErpCsCannedResponse> suggestForTicket(@Name("ticketId") Long ticketId,
                                                       IServiceContext context) {
        if (!ErpCsConfigs.isCannedResponseEnabled()) {
            return Collections.emptyList();
        }
        ErpCsTicket ticket = loadTicket(ticketId, context);
        if (ticket == null) {
            return Collections.emptyList();
        }
        Long ticketTypeId = ticket.getTicketTypeId();
        String priority = ticket.getPriority();
        int limit = ErpCsConfigs.getCannedResponseMacroCount();

        // 加载所有 active 预设应答（避免 XMeta 不支持 isNull 过滤，改在内存过滤 null 列）
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.addOrderField("sequence", false);
        List<ErpCsCannedResponse> all = findList(q, null, context);

        List<ErpCsCannedResponse> result = new ArrayList<>();
        Set<Long> collected = new java.util.HashSet<>();

        // 第一级：精确匹配 type + priority
        fillMatching(all, result, collected, ticketTypeId, priority, limit - result.size());
        if (result.size() >= limit) {
            return truncate(result, limit);
        }

        // 第二级：类型匹配 type（priority IS NULL）
        fillMatching(all, result, collected, ticketTypeId, null, limit - result.size());
        if (result.size() >= limit) {
            return truncate(result, limit);
        }

        // 第三级：全局兜底 type+priority 均空（macroTicketTypeId IS NULL AND macroPriority IS NULL）
        fillMatching(all, result, collected, null, null, limit - result.size());
        return truncate(result, limit);
    }

    // ===================== Phase 2：插入流程 + 审计 =====================

    @Override
    @BizMutation
    public String applyCannedResponse(@Name("cannedResponseId") Long cannedResponseId,
                                      @Name("ticketId") Long ticketId,
                                      @Optional @Name("customVariables") Map<String, String> customVariables,
                                      IServiceContext context) {
        ErpCsCannedResponse resp = requireCannedResponse(cannedResponseId, context);
        assertActive(resp);
        Map<String, String> systemVars = resolveSystemVars(resp, ticketId, context);
        String rendered = CannedResponseRenderer.render(resp.getContent(), resp.getVariableDefs(), systemVars, customVariables);

        // usageCount +1 持久化
        Integer cur = resp.getUsageCount();
        resp.setUsageCount(cur == null ? 1 : cur + 1);
        updateEntity(resp, null, context);

        // 写 TicketAction NOTE 审计
        writeNoteAction(ticketId, rendered, cannedResponseId, context);

        return rendered;
    }

    // ===================== helpers =====================

    private ErpCsCannedResponse requireCannedResponse(Long id, IServiceContext context) {
        if (id == null) {
            throw new NopException(ErpCsErrors.ERR_CANNED_RESPONSE_NOT_FOUND)
                    .param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, id);
        }
        ErpCsCannedResponse resp = get(String.valueOf(id), false, context);
        if (resp == null) {
            throw new NopException(ErpCsErrors.ERR_CANNED_RESPONSE_NOT_FOUND)
                    .param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, id);
        }
        return resp;
    }

    private void assertActive(ErpCsCannedResponse resp) {
        if (!Boolean.TRUE.equals(resp.getIsActive())) {
            throw new NopException(ErpCsErrors.ERR_CANNED_RESPONSE_INACTIVE)
                    .param(ErpCsErrors.ARG_CANNED_RESPONSE_ID, resp.getId());
        }
    }

    private Map<String, String> resolveSystemVars(ErpCsCannedResponse resp, Long ticketId, IServiceContext context) {
        Map<String, String> vars = new LinkedHashMap<>();
        LocalDate today = CoreMetrics.currentDate();
        LocalDateTime now = CoreMetrics.currentDateTime();
        vars.put("{today}", today.format(DATE_FMT));
        vars.put("{now}", now.format(DATETIME_FMT));
        if (context != null && context.getUserId() != null) {
            vars.put("{agent_name}", context.getUserId());
        }

        if (ticketId != null) {
            ErpCsTicket ticket = loadTicket(ticketId, context);
            if (ticket != null) {
                if (ticket.getCode() != null) {
                    vars.put("{ticket_id}", ticket.getCode());
                }
                String customerName = resolveCustomerName(ticket.getCustomerId(), context);
                if (customerName != null) {
                    vars.put("{customer_name}", customerName);
                }
            }
        }
        return vars;
    }

    private ErpCsTicket loadTicket(Long ticketId, IServiceContext context) {
        if (ticketId == null) {
            return null;
        }
        return ticketBiz.get(String.valueOf(ticketId), false, context);
    }

    private String resolveCustomerName(Long customerId, IServiceContext context) {
        if (customerId == null) {
            return null;
        }
        try {
            ErpMdPartner partner = mdPartnerBiz.findById(customerId, context);
            return partner == null ? null : partner.getName();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 从已加载的 active 应答列表中筛选匹配指定 type/priority 组合的条目（内存过滤，避免 isNull 限制）。
     *
     * @param all              全部 active 应答（已按 sequence ASC 排序）
     * @param result           累积结果列表（追加匹配项）
     * @param collected        已收集的 id 集（去重）
     * @param macroTicketTypeId 非空=精确匹配该 type；空=要求该列为 null
     * @param macroPriority     非空=精确匹配该 priority；空=要求该列为 null
     * @param limit             本次最大补充条数
     */
    private static void fillMatching(List<ErpCsCannedResponse> all,
                                     List<ErpCsCannedResponse> result, Set<Long> collected,
                                     Long macroTicketTypeId, String macroPriority, int limit) {
        if (limit <= 0) {
            return;
        }
        int added = 0;
        for (ErpCsCannedResponse r : all) {
            if (added >= limit) {
                break;
            }
            if (r.getId() != null && collected.contains(r.getId())) {
                continue;
            }
            boolean typeMatch = macroTicketTypeId == null
                    ? r.getMacroTicketTypeId() == null
                    : macroTicketTypeId.equals(r.getMacroTicketTypeId());
            boolean priorityMatch = macroPriority == null
                    ? r.getMacroPriority() == null
                    : macroPriority.equals(r.getMacroPriority());
            if (typeMatch && priorityMatch) {
                result.add(r);
                if (r.getId() != null) {
                    collected.add(r.getId());
                }
                added++;
            }
        }
    }

    private static <T> List<T> truncate(List<T> list, int limit) {
        if (list.size() <= limit) {
            return list;
        }
        return new ArrayList<>(list.subList(0, limit));
    }

    private void writeNoteAction(Long ticketId, String content, Long cannedResponseId, IServiceContext context) {
        if (ticketId == null) {
            return;
        }
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticketId);
        action.setActionType(ErpCsConstants.ACTION_TYPE_NOTE);
        action.setContent(content);
        action.setOperatorId(context == null ? null : context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCsCannedResponse.class)
    public List<String> orgName(@ContextSource List<ErpCsCannedResponse> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsCannedResponse row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsCannedResponse.class)
    public List<String> categoryName(@ContextSource List<ErpCsCannedResponse> rows) {
        orm().batchLoadProps(rows, Collections.singleton("category"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsCannedResponse row : rows) {
            result.add(row.orm_attached() && row.getCategory() != null ? row.getCategory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCsCannedResponse.class)
    public List<String> ticketTypeName(@ContextSource List<ErpCsCannedResponse> rows) {
        orm().batchLoadProps(rows, Collections.singleton("macroTicketType"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCsCannedResponse row : rows) {
            result.add(row.orm_attached() && row.getMacroTicketType() != null ? row.getMacroTicketType().getName() : null);
        }
        return result;
    }

}
