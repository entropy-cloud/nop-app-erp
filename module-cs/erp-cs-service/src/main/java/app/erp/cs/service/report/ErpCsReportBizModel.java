package app.erp.cs.service.report;

import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketType;
import app.erp.cs.service.ErpCsErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.concurrent.executor.GlobalExecutors;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.tpl.ITemplateOutput;
import io.nop.core.resource.tpl.ITextTemplateOutput;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import io.nop.report.core.engine.IReportEngine;
import io.nop.xlang.api.XLang;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 客服域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/cs/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpHrReportBizModel} 域隔离范式（…CRM→{@code /crm/}，
 * customer-service→{@code /cs/}）：不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；报表名经
 * {@link StringHelper#isValidVPath} 校验防路径注入。
 *
 * <p>一张综合种子报表：工单 SLA/CSAT 综合统计表（{@link ErpCsTicket} SLA 命中/超时 +
 * {@link ErpCsSurvey} csat/nps 均值，对齐 {@code customer-service/sla.md}+{@code customer-service/csat.md}）。
 */
@BizModel("ErpCsReport")
public class ErpCsReportBizModel {

    /** 客服报表模板 VFS 根路径（与其他域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/cs/";

    /** 模板内统一引用的数据集变量名（单元格 {@code *=^ds!<field>}）。 */
    static final String DS_VAR = "ds";

    private static final Set<String> ALLOWED_RENDER_TYPES = new HashSet<>(Arrays.asList("html", "xlsx", "pdf"));

    @Inject
    IReportEngine reportEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== 渲染入口 =====================

    @BizQuery
    public String renderHtml(@Name("reportName") String reportName,
                             @Optional @Name("data") Map<String, Object> data,
                             IServiceContext context) {
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);
        ITextTemplateOutput output = reportEngine.getHtmlRenderer(path);
        return output.generateText(scope);
    }

    @BizQuery
    public WebContentBean download(@Name("reportName") String reportName,
                                   @Name("renderType") String renderType,
                                   @Optional @Name("data") Map<String, Object> data,
                                   IServiceContext context) {
        if (!ALLOWED_RENDER_TYPES.contains(renderType)) {
            throw new NopException(ErpCsErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpCsErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("cs-rpt");
        try {
            output.generateToResource(resource, scope);
            String fileName = baseName(reportName) + "." + renderType;
            WebContentBean content = new WebContentBean("application/octet-stream", resource.toFile(), fileName);
            GlobalExecutors.globalTimer().schedule(() -> {
                resource.delete();
                return null;
            }, 5, TimeUnit.MINUTES);
            return content;
        } catch (Exception e) {
            resource.delete();
            throw NopException.adapt(e);
        }
    }

    // ===================== 路径解析与防注入 =====================

    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpCsErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpCsErrors.ARG_REPORT_NAME, reportName);
        }
        String name = reportName;
        if (!name.endsWith(".xpt.xml") && !name.endsWith(".xpt.xlsx")) {
            name = name + ".xpt.xml";
        }
        return REPORT_PATH_PREFIX + name;
    }

    private String baseName(String reportName) {
        String n = reportName;
        int slash = n.lastIndexOf('/');
        if (slash >= 0) n = n.substring(slash + 1);
        if (n.endsWith(".xpt.xml")) n = StringHelper.removeTail(n, ".xpt.xml");
        else if (n.endsWith(".xpt.xlsx")) n = StringHelper.removeTail(n, ".xpt.xlsx");
        return n;
    }

    private Map<String, Object> mergeData(Map<String, Object> data) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (data != null) merged.putAll(data);
        return merged;
    }

    private void prepareDataset(String reportName, Map<String, Object> data, IServiceContext context) {
        String key = baseName(reportName);
        if (data.containsKey(DS_VAR)) return;
        switch (key) {
            case "ticket-sla-csat-summary":
                data.put(DS_VAR, buildTicketSlaCsatSummaryDataset(asString(data, "ticketType")));
                break;
            default:
                break;
        }
    }

    private static String asString(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        return v == null ? null : v.toString();
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 工单 SLA/CSAT 综合统计数据集：按工单类型聚合 SLA 命中 + CSAT/NPS 均值，对齐 {@code customer-service/sla.md}+{@code customer-service/csat.md}。 */
    @BizQuery
    public List<Map<String, Object>> ticketSlaCsatSummaryData(@Optional @Name("ticketType") String ticketType,
                                                               IServiceContext context) {
        return buildTicketSlaCsatSummaryDataset(ticketType);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 工单 SLA/CSAT 综合统计数据集。从 {@link ErpCsTicket}（可选 ticketTypeId 过滤）按 ticketTypeId 聚合：
     * totalTickets / slaCompletedCount（isSlaCompleted=true）/ slaBreachedCount（isSlaCompleted=false）+
     * {@link ErpCsSurvey}（关联 ticketId）csatScore/npsScore 均值，对齐 {@code customer-service/sla.md}+
     * {@code customer-service/csat.md}。无 ticketTypeId 时聚合为单一桶（ticketTypeId=null）。
     */
    List<Map<String, Object>> buildTicketSlaCsatSummaryDataset(String ticketType) {
        return ormTemplate.runInSession(session -> {
            List<ErpCsTicket> tickets = loadTickets(ticketType);
            if (tickets.isEmpty()) {
                return Collections.emptyList();
            }
            Map<Long, TicketAggregator> agg = new LinkedHashMap<>();
            Set<Long> ticketIds = new HashSet<>();
            for (ErpCsTicket t : tickets) {
                Long key = t.getTicketTypeId();
                TicketAggregator a = agg.computeIfAbsent(key, TicketAggregator::new);
                a.totalTickets++;
                if (Boolean.TRUE.equals(t.orm_propValueByName("isSlaCompleted"))) {
                    a.slaCompletedCount++;
                } else {
                    a.slaBreachedCount++;
                }
                if (t.getId() != null) ticketIds.add(t.getId());
            }
            Map<Long, SurveyAggregator> surveyAgg = aggregateSurveys(ticketIds);
            // 把 survey 均值分摊回每个 ticketType 桶（按 ticketId 查 ticketTypeId）
            Map<Long, Long> ticketTypeByTicket = new HashMap<>();
            for (ErpCsTicket t : tickets) {
                if (t.getId() != null) {
                    ticketTypeByTicket.put(t.getId(), t.getTicketTypeId());
                }
            }
            for (Map.Entry<Long, SurveyAggregator> e : surveyAgg.entrySet()) {
                Long ttId = ticketTypeByTicket.get(e.getKey());
                if (ttId == null) continue;
                TicketAggregator a = agg.get(ttId);
                if (a == null) continue;
                SurveyAggregator sa = e.getValue();
                a.surveyCount += sa.count;
                a.csatSum = a.csatSum.add(sa.csatSum);
                a.npsSum = a.npsSum.add(sa.npsSum);
            }
            Map<Long, String> typeNames = resolveTicketTypeNames(agg.keySet());
            List<Map<String, Object>> rows = new ArrayList<>(agg.size());
            for (TicketAggregator a : agg.values()) {
                BigDecimal avgCsat = a.surveyCount > 0
                        ? a.csatSum.divide(new BigDecimal(a.surveyCount), 2, RoundingMode.HALF_UP)
                        : null;
                BigDecimal avgNps = a.surveyCount > 0
                        ? a.npsSum.divide(new BigDecimal(a.surveyCount), 2, RoundingMode.HALF_UP)
                        : null;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("ticketTypeId", a.ticketTypeId);
                r.put("ticketTypeName", typeNames.getOrDefault(a.ticketTypeId, a.ticketTypeId == null ? "(全部)" : ""));
                r.put("totalTickets", a.totalTickets);
                r.put("slaCompletedCount", a.slaCompletedCount);
                r.put("slaBreachedCount", a.slaBreachedCount);
                r.put("surveyCount", a.surveyCount);
                r.put("avgCsat", avgCsat);
                r.put("avgNps", avgNps);
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpCsTicket> loadTickets(String ticketType) {
        QueryBean q = new QueryBean();
        if (ticketType != null) q.addFilter(eq("ticketTypeId", Long.valueOf(ticketType)));
        return daoProvider.daoFor(ErpCsTicket.class).findAllByQuery(q);
    }

    private Map<Long, SurveyAggregator> aggregateSurveys(Set<Long> ticketIds) {
        if (ticketIds.isEmpty()) return Collections.emptyMap();
        QueryBean q = new QueryBean();
        q.addFilter(in("ticketId", ticketIds));
        List<ErpCsSurvey> surveys = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q);
        Map<Long, SurveyAggregator> map = new HashMap<>();
        for (ErpCsSurvey s : surveys) {
            Long tId = s.getTicketId();
            if (tId == null) continue;
            SurveyAggregator a = map.computeIfAbsent(tId, k -> new SurveyAggregator());
            a.count++;
            a.csatSum = a.csatSum.add(nz(s.orm_propValueByName("csatScore")));
            a.npsSum = a.npsSum.add(nz(s.orm_propValueByName("npsScore")));
        }
        return map;
    }

    private Map<Long, String> resolveTicketTypeNames(Set<Long> typeIds) {
        if (typeIds.isEmpty()) return Collections.emptyMap();
        // 仅解析非 null 的 typeId；ErpCsTicketType 实体存在
        Set<Long> nonNull = new HashSet<>();
        for (Long id : typeIds) {
            if (id != null) nonNull.add(id);
        }
        if (nonNull.isEmpty()) return Collections.emptyMap();
        try {
            QueryBean q = new QueryBean();
            q.addFilter(in("id", nonNull));
            List<ErpCsTicketType> types =
                    daoProvider.daoFor(ErpCsTicketType.class).findAllByQuery(q);
            Map<Long, String> names = new HashMap<>();
            for (ErpCsTicketType t : types) {
                names.put(t.getId(), t.getName());
            }
            return names;
        } catch (Exception ex) {
            // ErpCsTicketType 不可用时降级为空名称
            return Collections.emptyMap();
        }
    }

    private static BigDecimal nz(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        try {
            return new BigDecimal(String.valueOf(v));
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private static class TicketAggregator {
        final Long ticketTypeId;
        int totalTickets = 0;
        int slaCompletedCount = 0;
        int slaBreachedCount = 0;
        int surveyCount = 0;
        BigDecimal csatSum = BigDecimal.ZERO;
        BigDecimal npsSum = BigDecimal.ZERO;

        TicketAggregator(Long ticketTypeId) {
            this.ticketTypeId = ticketTypeId;
        }
    }

    private static class SurveyAggregator {
        int count = 0;
        BigDecimal csatSum = BigDecimal.ZERO;
        BigDecimal npsSum = BigDecimal.ZERO;
    }
}
