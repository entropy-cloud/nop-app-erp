package app.erp.fin.service.report;

import app.erp.fin.biz.IErpFinBudgetLineBiz;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinGlBalance;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.fin.service.metrics.ErpFinBusinessMetrics;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.concurrent.executor.GlobalExecutors;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.tpl.ITemplateOutput;
import io.nop.core.resource.tpl.ITextTemplateOutput;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import io.nop.report.core.engine.IReportEngine;
import io.nop.xlang.api.XLang;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.notIn;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 财务报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/fin/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>本类是平台默认路线的"应用层接线"——不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}。
 * 报表名经 {@link StringHelper#isValidVPath} 校验防路径注入（参考 {@code ReportDemoBizModel}）。
 * 报表数据集由本类的 {@code buildXxxDataset} 方法从既有 ORM 实体聚合后经 {@code IEvalScope} 传入模板，
 * 模板用 {@code *=^ds!<field>} 展开渲染。
 *
 * <p>五大种子报表：资产负债表 / 利润表 / 现金流量表 / AR-AP 账龄 / 期末结账报告。
 */
@BizModel("ErpFinReport")
public class ErpFinReportBizModel {

    /** 财务报表模板 VFS 根路径（与 runbook {@code generate-report.md} 约定一致）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/fin/";

    /** 模板内统一引用的数据集变量名（单元格 {@code *=^ds!<field>}）。 */
    static final String DS_VAR = "ds";

    private static final Set<String> ALLOWED_RENDER_TYPES = new HashSet<>(Arrays.asList("html", "xlsx", "pdf"));

    @Inject
    IReportEngine reportEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinBudgetLineBiz budgetLineBiz;

    // ===================== 渲染入口 =====================

    @BizQuery
    public String renderHtml(@Name("reportName") String reportName,
                             @Optional @Name("data") Map<String, Object> data,
                             IServiceContext context) {
        // observability.md §5.1 指标 6 path=report_render：报表渲染关键路径吞吐（app 层入口，不触及 nop-entropy 源码）
        ErpFinBusinessMetrics.recordReportRenderPathThroughput(null);
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged);
        scope.setLocalValues(merged);
        ITextTemplateOutput output = reportEngine.getHtmlRenderer(path);
        return output.generateText(scope);
    }

    @BizQuery
    public WebContentBean download(@Name("reportName") String reportName,
                                   @Name("renderType") String renderType,
                                   @Optional @Name("data") Map<String, Object> data,
                                   IServiceContext context) {
        // observability.md §5.1 指标 6 path=report_render：报表下载/渲染关键路径吞吐
        ErpFinBusinessMetrics.recordReportRenderPathThroughput(null);
        if (!ALLOWED_RENDER_TYPES.contains(renderType)) {
            throw new NopException(ErpFinErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpFinErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("fin-rpt");
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

    /**
     * 将报表名规范化为完整 VFS 路径，并校验防路径注入。
     *
     * <p>接受 {@code balance-sheet}、{@code balance-sheet.xpt.xml} 两种形式，统一补全到
     * {@code /nop/main/report/fin/balance-sheet.xpt.xml}。
     */
    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpFinErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpFinErrors.ARG_REPORT_NAME, reportName);
        }
        String name = reportName;
        if (!name.endsWith(".xpt.xml") && !name.endsWith(".xpt.xlsx")) {
            name = name + ".xpt.xml";
        }
        return REPORT_PATH_PREFIX + name;
    }

    /** 取报表名的去后缀短名（用于数据集路由与下载文件名）。 */
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

    /** 按报表短名自动装配数据集（调用方未预先提供时）。 */
    private void prepareDataset(String reportName, Map<String, Object> data) {
        String key = baseName(reportName);
        if (data.containsKey(DS_VAR) && !"cash-flow-statement".equals(key)) return;
        switch (key) {
            case "balance-sheet":
                data.put(DS_VAR, buildBalanceSheetDataset(asId(data, "periodId")));
                break;
            case "income-statement":
                data.put(DS_VAR, buildIncomeStatementDataset(asId(data, "periodId")));
                break;
            case "cash-flow-statement":
                // 直接法行（section=OPERATING/INVESTING/FINANCING）+ 间接法行（section=INDIRECT）双数据集
                if (!data.containsKey(DS_VAR)) {
                    data.put(DS_VAR, buildCashFlowDataset(asId(data, "periodId")));
                }
                data.put("indirectDs", buildIndirectCashFlowDataset(asId(data, "periodId")));
                break;
            case "ar-ap-aging":
                data.put(DS_VAR, buildArApAgingDataset(asDate(data, "asOfDate")));
                break;
            case "period-close-report":
                data.put(DS_VAR, buildPeriodCloseDataset(asId(data, "periodId")));
                break;
            case "budget-vs-actual":
                data.put(DS_VAR, buildBudgetVsActualDataset(asId(data, "acctSchemaId"),
                        asId(data, "periodId"), asId(data, "subjectId")));
                break;
            default:
                // 未知报表：不自动装配，模板可经 beforeExpand 自行构造数据集
                break;
        }
    }

    // 报表参数 id 归一为 String（fin id String 化后取参口径）
    private static String asId(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        if (v == null) return null;
        String s = v.toString();
        if (s.trim().isEmpty()) return null;
        return s;
    }

    private static LocalDate asDate(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        if (v == null) return null;
        if (v instanceof LocalDate) return (LocalDate) v;
        String s = v.toString();
        if (s.isEmpty()) return null;
        if (s.matches("\\d{13}")) return LocalDate.ofInstant(Instant.ofEpochMilli(Long.parseLong(s)), ZoneOffset.UTC);
        if (s.matches("\\d{10}")) return LocalDate.ofInstant(Instant.ofEpochSecond(Long.parseLong(s)), ZoneOffset.UTC);
        return LocalDate.parse(s);
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 预算对比数据集（budget.md §业务规则5）：委托 {@link IErpFinBudgetLineBiz#getBudgetVsActual} 聚合。 */
    public List<app.erp.fin.dao.dto.BudgetVsActualRow> buildBudgetVsActualDataset(String acctSchemaId,
                                                                                   String periodId, String subjectId) {
        return budgetLineBiz.getBudgetVsActual(acctSchemaId, periodId, subjectId,
                new io.nop.core.context.ServiceContextImpl());
    }

    /** 资产负债表数据集：按科目层级汇总资产/负债/权益期末余额。口径对齐 finance owner doc 与年度结转产物。 */
    @BizQuery
    public List<Map<String, Object>> balanceSheetData(@Name("periodId") String periodId, IServiceContext context) {
        return buildBalanceSheetDataset(periodId);
    }

    /** 利润表数据集：损益类科目本期累计发生额，对齐结转损益业务类型。 */
    @BizQuery
    public List<Map<String, Object>> incomeStatementData(@Name("periodId") String periodId, IServiceContext context) {
        return buildIncomeStatementDataset(periodId);
    }

    /** 现金流量表数据集：现金类科目本期净变动（直接法），按科目 cashFlowType 三分类（经营/投资/筹资）。 */
    @BizQuery
    public List<Map<String, Object>> cashFlowStatementData(@Name("periodId") String periodId, IServiceContext context) {
        List<Map<String, Object>> rows = new ArrayList<>(buildCashFlowDataset(periodId));
        rows.addAll(buildIndirectCashFlowDataset(periodId));
        return rows;
    }

    /** 间接法现金流量数据集：净利润 + 非现金项目 + 营运资金变动（RC-R1.45 / P1-RC-007）。 */
    @BizQuery
    public List<Map<String, Object>> indirectCashFlowData(@Name("periodId") String periodId, IServiceContext context) {
        return buildIndirectCashFlowDataset(periodId);
    }

    /** AR/AP 账龄数据集：按 0-30/31-60/61-90/90+ 分桶，区分应收应付，对齐 ar-ap-reconciliation §账龄。 */
    @BizQuery
    public List<Map<String, Object>> arApAgingData(@Optional @Name("asOfDate") LocalDate asOfDate,
                                                   IServiceContext context) {
        return buildArApAgingDataset(asOfDate);
    }

    /** 期末结账报告数据集：覆盖结转损益/汇兑重估/坏账计提/存货成本核算/期间状态，对齐 1000-3/0540-2 结账流程。 */
    @BizQuery
    public List<Map<String, Object>> periodCloseReportData(@Name("periodId") String periodId, IServiceContext context) {
        return buildPeriodCloseDataset(periodId);
    }

    // ===================== 数据集聚合实现 =====================

    List<Map<String, Object>> buildBalanceSheetDataset(String periodId) {
        return ormTemplate.runInSession(session -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpFinGlBalance b : loadGlBalances(periodId)) {
                ErpMdSubject s = b.getSubject();
                if (s == null || StringHelper.isEmpty(s.getSubjectClass())) continue;
                String section = sectionOf(s.getSubjectClass());
                if (section == null) continue;
                BigDecimal amount = balanceAmount(b, s);
                rows.add(rowOf(section, s.getCode(), s.getName(), amount));
            }
            return rows;
        });
    }

    List<Map<String, Object>> buildIncomeStatementDataset(String periodId) {
        return ormTemplate.runInSession(session -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpFinGlBalance b : loadGlBalances(periodId)) {
                ErpMdSubject s = b.getSubject();
                if (s == null) continue;
                String cls = s.getSubjectClass();
                if (!isPnlClass(cls)) continue;
                BigDecimal amount = periodActivity(b, s);
                rows.add(rowOf(cls, s.getCode(), s.getName(), amount));
            }
            return rows;
        });
    }

    /**
     * 现金流量表数据集（直接法）：现金类科目（isCashSubjectCode 前缀准入）本期净变动，
     * section 按科目 cashFlowType 分类（OPERATING/INVESTING/FINANCING，null/NON_CASH 回退 OPERATING）。
     * 与 {@link #buildIndirectCashFlowDataset} 同源（loadPostedVoucherLines），模板双数据集渲染。
     */
    List<Map<String, Object>> buildCashFlowDataset(String periodId) {
        return ormTemplate.runInSession(session -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            List<ErpFinVoucherLine> lines = loadPostedVoucherLines(periodId);
            for (ErpFinVoucherLine l : lines) {
                if (l.getSubjectCode() == null) continue;
                if (!isCashSubjectCode(l.getSubjectCode())) continue;
                BigDecimal debit = nz(l.getDebitAmount());
                BigDecimal credit = nz(l.getCreditAmount());
                BigDecimal net = debit.subtract(credit);
                if (net.signum() == 0) continue;
                String flowDir = net.signum() > 0
                        ? ErpFinConstants.CASH_FLOW_INFLOW
                        : ErpFinConstants.CASH_FLOW_OUTFLOW;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("section", sectionOfCashFlowType(l.getSubject()));
                r.put("code", l.getSubjectCode());
                r.put("name", l.getSubjectName());
                r.put("direction", flowDir);
                r.put("amount", net.abs());
                rows.add(r);
            }
            rows.sort((a, b) -> {
                int c = Integer.compare(sectionOrder((String) a.get("section")),
                        sectionOrder((String) b.get("section")));
                if (c != 0) return c;
                return String.valueOf(a.get("code")).compareTo(String.valueOf(b.get("code")));
            });
            return rows;
        });
    }

    /**
     * 间接法现金流量数据集（RC-R1.45 / P1-RC-007）：净利润 + 非现金项目 + 营运资金变动。
     *
     * <p>三组件基于同一已加载凭证行集内存聚合（D3 选项 A）：
     * <ul>
     *   <li>净利润 = 损益类科目（INCOME/EXPENSE/COST）net 聚合（收入 credit−debit / 费用成本 debit−credit），
     *       businessType=PERIOD_CLOSE 结转凭证行排除（对齐 ProfitLossClosingService 范式）；</li>
     *   <li>非现金项目 = cashFlowType=NON_CASH 损益类科目 Σ(debit−credit)（折旧/减值加回，非现金收入负向抵消）；</li>
     *   <li>营运资金变动 = 流动资产/负债科目 Σ(credit−debit)——资产增加减现金流/负债增加加现金流；
     *       科目判定 = ASSET 类且 direction=DEBIT 且非现金前缀且非 160x 非流动资产前缀 ∪ LIABILITY 类且 direction=CREDIT
     *       （CREDIT 方向资产[1231 坏账准备/1602 累计折旧/1604 减值准备]经 direction 规则排除，防与 NON_CASH 加回重复计算）。</li>
     * </ul>
     * 三组件共享 voucher 头侧 postingType 过滤（D3 子裁决）：postingType notIn(BUDGET, COMMITMENT)——
     * BUDGET/COMMITMENT 影子凭证的损益类行（6601/6001 等）不得污染净利润（对齐 RC-R1.46 模式）。
     */
    List<Map<String, Object>> buildIndirectCashFlowDataset(String periodId) {
        return ormTemplate.runInSession(session -> {
            BigDecimal netProfit = BigDecimal.ZERO;
            BigDecimal nonCash = BigDecimal.ZERO;
            BigDecimal workingCapital = BigDecimal.ZERO;
            List<ErpFinVoucherLine> lines = loadPostedVoucherLines(periodId, true);
            for (ErpFinVoucherLine l : lines) {
                ErpMdSubject s = l.getSubject();
                if (s == null) continue;
                String cls = s.getSubjectClass();
                BigDecimal debit = nz(l.getDebitAmount());
                BigDecimal credit = nz(l.getCreditAmount());
                if (isPnlClass(cls)) {
                    if (isPeriodCloseLine(l)) continue;
                    BigDecimal net = pnlNet(l, s);
                    if (ErpFinConstants.SUBJECT_CLASS_INCOME.equals(cls)) {
                        netProfit = netProfit.add(net);
                    } else {
                        netProfit = netProfit.subtract(net);
                    }
                    if (ErpFinConstants.CASH_FLOW_TYPE_NON_CASH.equals(s.getCashFlowType())) {
                        nonCash = nonCash.add(debit.subtract(credit));
                    }
                } else if (isWorkingCapitalSubject(s)) {
                    workingCapital = workingCapital.add(credit.subtract(debit));
                }
            }
            List<Map<String, Object>> rows = new ArrayList<>();
            rows.add(indirectRow(ErpFinConstants.CASH_FLOW_INDIRECT_NET_PROFIT, "净利润", netProfit));
            rows.add(indirectRow(ErpFinConstants.CASH_FLOW_INDIRECT_NON_CASH, "非现金项目", nonCash));
            rows.add(indirectRow(ErpFinConstants.CASH_FLOW_INDIRECT_WORKING_CAPITAL, "营运资金变动", workingCapital));
            rows.add(indirectRow(ErpFinConstants.CASH_FLOW_INDIRECT_NET,
                    "经营活动现金流量净额(间接法)", netProfit.add(nonCash).add(workingCapital)));
            return rows;
        });
    }

    List<Map<String, Object>> buildArApAgingDataset(LocalDate asOfDate) {
        return ormTemplate.runInSession(session -> {
            LocalDate today = asOfDate != null ? asOfDate : CoreMetrics.currentDate();
            IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
            List<ErpFinArApItem> items = dao.findAllByQuery(openItemsQuery());
            List<Map<String, Object>> rows = new ArrayList<>();
            for (ErpFinArApItem it : items) {
                LocalDate base = it.getDueDate() != null ? it.getDueDate() : it.getBusinessDate();
                if (base == null) base = it.getBusinessDate();
                long days = base != null ? ChronoUnit.DAYS.between(base, today) : 0L;
                if (days < 0) days = 0L;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("direction", it.getDirection());
                r.put("partnerId", it.getPartnerId());
                r.put("sourceBillCode", it.getSourceBillCode());
                r.put("businessDate", it.getBusinessDate());
                r.put("dueDate", it.getDueDate());
                r.put("openAmount", nz(it.getOpenAmountFunctional()));
                r.put("ageDays", days);
                r.put("bucket", bucketOf(days));
                rows.add(r);
            }
            return rows;
        });
    }

    List<Map<String, Object>> buildPeriodCloseDataset(String periodId) {
        return ormTemplate.runInSession(session -> {
            List<Map<String, Object>> rows = new ArrayList<>();
            if (periodId == null) return rows;

            ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
            if (period != null) {
                rows.add(metaRow("period", "期间编码", period.getCode()));
                rows.add(metaRow("period", "年度/月份", period.getYear() + "-" + period.getMonth()));
                rows.add(metaRow("period", "期间状态", period.getStatus()));
            }

            ErpFinAccountingPeriodStatus st = loadPeriodStatus(periodId);
        if (st != null) {
            rows.add(metaRow("module-status", "AR 模块", st.getArStatus()));
            rows.add(metaRow("module-status", "AP 模块", st.getApStatus()));
            rows.add(metaRow("module-status", "INV 模块", st.getInvStatus()));
            rows.add(metaRow("module-status", "GL 模块", st.getGlStatus()));
            rows.add(metaRow("module-status", "AST 模块", st.getAssetStatus()));
            rows.add(metaRow("voucher", "凭证总数", st.getTotalVouchers()));
            rows.add(metaRow("voucher", "已过账凭证", st.getPostedVouchers()));
            rows.add(metaRow("voucher", "未过账凭证", st.getUnpostedVouchers()));
        }

        rows.add(metaRow("voucher", "损益结转凭证数",
                countBillR(periodId, ErpFinBusinessType.PERIOD_CLOSE.name())));
        rows.add(metaRow("voucher", "汇兑重估凭证数",
                countBillR(periodId, ErpFinBusinessType.EXCHANGE_GAIN_LOSS.name())));

        return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpFinGlBalance> loadGlBalances(String periodId) {
        IEntityDao<ErpFinGlBalance> dao = daoProvider.daoFor(ErpFinGlBalance.class);
        List<ErpFinGlBalance> list;
        if (periodId == null) {
            // periodId 缺省时限定最近一个会计期间，避免全表回退（原全表加载会物化全表）。
            String latestPeriodId = findLatestPeriodId();
            if (latestPeriodId == null) {
                list = Collections.emptyList();
            } else {
                QueryBean q = new QueryBean();
                q.addFilter(eq("periodId", latestPeriodId));
                applyOrgAndSchemaScope(q, latestPeriodId);
                list = dao.findAllByQuery(q);
            }
        } else {
            QueryBean q = new QueryBean();
            q.addFilter(eq("periodId", periodId));
            applyOrgAndSchemaScope(q, periodId);
            list = dao.findAllByQuery(q);
        }
        list.sort(Comparator.comparing(ErpFinGlBalance::getSubjectId,
                Comparator.nullsFirst(Comparator.naturalOrder())));
        return list;
    }

    private String findLatestPeriodId() {
        IEntityDao<ErpFinAccountingPeriod> pDao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addOrderField("startDate", true);
        q.setLimit(1);
        List<ErpFinAccountingPeriod> latest = pDao.findAllByQuery(q);
        return latest.isEmpty() ? null : latest.get(0).getId();
    }

    private List<ErpFinVoucherLine> loadPostedVoucherLines(String periodId) {
        return loadPostedVoucherLines(periodId, false);
    }

    /**
     * 加载已过账凭证行。{@code excludeShadowPostings=true} 时在 voucher 头侧过滤
     * postingType notIn(BUDGET, COMMITMENT)（D3 子裁决——间接法三组件共享该过滤，
     * BUDGET/COMMITMENT 影子凭证的损益类行不得污染净利润聚合；直接法保持不过滤零回归）。
     */
    private List<ErpFinVoucherLine> loadPostedVoucherLines(String periodId, boolean excludeShadowPostings) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
        if (excludeShadowPostings) {
            vq.addFilter(or(isNull("postingType"), notIn("postingType",
                    Arrays.asList(ErpFinConstants.POSTING_TYPE_BUDGET, ErpFinConstants.POSTING_TYPE_COMMITMENT))));
        }
        if (periodId != null) {
            vq.addFilter(eq("periodId", periodId));
            applyOrgAndSchemaScope(vq, periodId);
        }
        List<ErpFinVoucher> vouchers = vDao.findAllByQuery(vq);
        if (vouchers.isEmpty()) return Collections.emptyList();
        Set<String> voucherIds = new HashSet<>();
        for (ErpFinVoucher v : vouchers) voucherIds.add(v.getId());
        QueryBean lq = new QueryBean();
        lq.addFilter(in("voucherId", voucherIds));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(lq);
    }

    private QueryBean openItemsQuery() {
        QueryBean q = new QueryBean();
        q.addFilter(in("status", Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_OPEN,
                ErpFinConstants.AR_AP_STATUS_PARTIAL)));
        return q;
    }

    private ErpFinAccountingPeriodStatus loadPeriodStatus(String periodId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        applySchemaScope(q, periodId);
        List<ErpFinAccountingPeriodStatus> list =
                daoProvider.daoFor(ErpFinAccountingPeriodStatus.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countBillR(String periodId, String businessType) {
        QueryBean vq = new QueryBean();
        vq.addFilter(eq("periodId", periodId));
        applyOrgAndSchemaScope(vq, periodId);
        List<ErpFinVoucher> vouchers = daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(vq);
        if (vouchers.isEmpty()) return 0;
        Set<String> voucherIds = new HashSet<>();
        for (ErpFinVoucher v : vouchers) voucherIds.add(v.getId());
        QueryBean bq = new QueryBean();
        bq.addFilter(eq("businessType", businessType));
        bq.addFilter(in("voucherId", voucherIds));
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(bq).size();
    }

    // ===================== 多账套/多组织读路径隔离 scope（P1-MA2-095）=====================
    // 多账套部署下报表按 periodId 聚合会双计；按期间所属组织 + 主账套补 filter 使单账套不双计。
    // scope 不可解析时（period.orgId 为空等）跳过 filter，保护单组织基线零回归。

    private String resolvePeriodOrgId(String periodId) {
        if (periodId == null) {
            return null;
        }
        ErpFinAccountingPeriod period = daoProvider.daoFor(ErpFinAccountingPeriod.class).getEntityById(periodId);
        return period != null ? period.getOrgId() : null;
    }

    private String resolveOrgSchemaId(String orgId) {
        return orgId != null ? AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId) : null;
    }

    /** 适用于同时含 orgId + acctSchemaId 列的实体（GlBalance/Voucher）。 */
    private void applyOrgAndSchemaScope(QueryBean q, String periodId) {
        String orgId = resolvePeriodOrgId(periodId);
        if (orgId == null) {
            return;
        }
        q.addFilter(eq("orgId", orgId));
        String schemaId = resolveOrgSchemaId(orgId);
        if (schemaId != null) {
            q.addFilter(eq("acctSchemaId", schemaId));
        }
    }

    /** 适用于仅含 acctSchemaId 列的实体（AccountingPeriodStatus）。 */
    private void applySchemaScope(QueryBean q, String periodId) {
        String orgId = resolvePeriodOrgId(periodId);
        String schemaId = resolveOrgSchemaId(orgId);
        if (schemaId != null) {
            q.addFilter(eq("acctSchemaId", schemaId));
        }
    }

    /** 科目大类 → 资产负债表段（ASSET/LIABILITY/EQUITY），损益类返回 null。 */
    private static String sectionOf(String subjectClass) {
        if (subjectClass == null) return null;
        switch (subjectClass) {
            case "ASSET":
                return "ASSET";
            case "LIABILITY":
                return "LIABILITY";
            case "EQUITY":
                return "EQUITY";
            default:
                return null;
        }
    }

    private static boolean isPnlClass(String cls) {
        return ErpFinConstants.SUBJECT_CLASS_INCOME.equals(cls)
                || ErpFinConstants.SUBJECT_CLASS_EXPENSE.equals(cls)
                || ErpFinConstants.SUBJECT_CLASS_COST.equals(cls);
    }

    /** 期末余额：借方科目取 closingDebit-closingCredit，贷方科目取 closingCredit-closingDebit。 */
    private static BigDecimal balanceAmount(ErpFinGlBalance b, ErpMdSubject s) {
        BigDecimal debit = nz(b.getClosingDebit());
        BigDecimal credit = nz(b.getClosingCredit());
        return ErpFinConstants.DC_DEBIT.equals(s.getDirection())
                ? debit.subtract(credit)
                : credit.subtract(debit);
    }

    /** 本期发生额净额：借方科目取 periodDebit-periodCredit，贷方科目取 periodCredit-periodDebit。 */
    private static BigDecimal periodActivity(ErpFinGlBalance b, ErpMdSubject s) {
        BigDecimal debit = nz(b.getPeriodDebit());
        BigDecimal credit = nz(b.getPeriodCredit());
        return ErpFinConstants.DC_DEBIT.equals(s.getDirection())
                ? debit.subtract(credit)
                : credit.subtract(debit);
    }

    private static boolean isCashSubjectCode(String code) {
        if (code == null) return false;
        return code.startsWith("1001") || code.startsWith("1002")
                || code.startsWith("1012") || code.startsWith("1031");
    }

    /** 科目现金流分类 → 直接法 section；null/NON_CASH 回退 OPERATING（D1 选项 A 零回归基线）。 */
    private static String sectionOfCashFlowType(ErpMdSubject s) {
        String t = s != null ? s.getCashFlowType() : null;
        if (ErpFinConstants.CASH_FLOW_TYPE_INVESTING.equals(t)) return "INVESTING";
        if (ErpFinConstants.CASH_FLOW_TYPE_FINANCING.equals(t)) return "FINANCING";
        return "OPERATING";
    }

    private static int sectionOrder(String section) {
        switch (section) {
            case "INVESTING":
                return 1;
            case "FINANCING":
                return 2;
            default:
                return 0;
        }
    }

    /** 损益类科目净额（收入：credit−debit；费用/成本：debit−credit），方向语义对齐 ProfitLossClosingService。 */
    private static BigDecimal pnlNet(ErpFinVoucherLine l, ErpMdSubject s) {
        BigDecimal debit = nz(l.getDebitAmount());
        BigDecimal credit = nz(l.getCreditAmount());
        return ErpFinConstants.DC_CREDIT.equals(s.getDirection())
                ? credit.subtract(debit)
                : debit.subtract(credit);
    }

    /** 损益结转凭证行排除（间接法净利润口径，对齐 ProfitLossClosingService）。 */
    private static boolean isPeriodCloseLine(ErpFinVoucherLine l) {
        String bt = l.getBusinessType();
        return bt != null && Objects.equals(bt, ErpFinBusinessType.PERIOD_CLOSE.name());
    }

    /**
     * 营运资金变动组件科目判定（D3 选项 A）：ASSET 类且 direction=DEBIT 且非现金前缀且非 160x 非流动资产前缀
     * ∪ LIABILITY 类且 direction=CREDIT。CREDIT 方向资产（1231 坏账准备/1602 累计折旧/1604 固定资产减值准备）
     * 经 direction 规则天然排除——其 P&L 对偶科目（6701/6602/6702）经 NON_CASH 加回，排除防重复计算。
     */
    private static boolean isWorkingCapitalSubject(ErpMdSubject s) {
        if (s == null || s.getSubjectClass() == null || s.getDirection() == null) return false;
        String code = s.getCode();
        if (code == null) return false;
        if (isCashSubjectCode(code)) return false;
        if (code.startsWith("160")) return false;
        String cls = s.getSubjectClass();
        if (ErpFinConstants.SUBJECT_CLASS_ASSET.equals(cls)) {
            return ErpFinConstants.DC_DEBIT.equals(s.getDirection());
        }
        if (ErpFinConstants.SUBJECT_CLASS_LIABILITY.equals(cls)) {
            return ErpFinConstants.DC_CREDIT.equals(s.getDirection());
        }
        return false;
    }

    /** 间接法组件行（section=INDIRECT + 组件键 + 中文名 + 符号方向 + 绝对值金额）。 */
    private static Map<String, Object> indirectRow(String key, String name, BigDecimal value) {
        BigDecimal v = nz(value);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("section", "INDIRECT");
        r.put("code", key);
        r.put("name", name);
        r.put("direction", v.signum() >= 0
                ? ErpFinConstants.CASH_FLOW_INFLOW
                : ErpFinConstants.CASH_FLOW_OUTFLOW);
        r.put("amount", v.abs());
        return r;
    }

    private static String bucketOf(long days) {
        if (days <= 30) return "0-30";
        if (days <= 60) return "31-60";
        if (days <= 90) return "61-90";
        return "90+";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static Map<String, Object> rowOf(String section, String code, String name, BigDecimal amount) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("section", section);
        r.put("code", code);
        r.put("name", name);
        r.put("amount", nz(amount));
        return r;
    }

    private static Map<String, Object> metaRow(String section, String label, Object value) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("section", section);
        r.put("label", label);
        r.put("value", value);
        return r;
    }
}
