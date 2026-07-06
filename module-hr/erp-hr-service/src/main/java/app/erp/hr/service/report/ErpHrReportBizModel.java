package app.erp.hr.service.report;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import app.erp.hr.service.ErpHrErrors;
import app.erp.md.dao.entity.ErpMdPartner;
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
import io.nop.dao.api.IEntityDao;
import io.nop.report.core.engine.IReportEngine;
import io.nop.xlang.api.XLang;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * HR 域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/hr/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpFinReportBizModel} 域隔离范式（finance→{@code /fin/}，
 * manufacturing→{@code /mfg/}，inventory→{@code /inv/}，HR→{@code /hr/}）：不自建报表引擎，渲染逻辑全部委托给
 * {@code nop-report}；报表名经 {@link StringHelper#isValidVPath} 校验防路径注入。
 *
 * <p>两张种子报表：
 * <ul>
 *   <li>员工净余额报表：按员工聚合预支应收/报销应付净额（跨域经 {@link IErpFinArApItemBiz} 只读聚合 finance 辅助账，
 *       对齐 {@code expense-claim.md}）；</li>
 *   <li>薪酬模拟对比报表：源 vs 模拟三列对比 + 部门小计（同域 {@link ErpHrSalarySimulationItemAdjustment} 聚合，
 *       对齐 {@code payroll-simulation.md}）。</li>
 * </ul>
 */
@BizModel("ErpHrReport")
public class ErpHrReportBizModel {

    /** HR 报表模板 VFS 根路径（与 finance/manufacturing/inventory 域隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/hr/";

    /** 模板内统一引用的数据集变量名（单元格 {@code *=^ds!<field>}）。 */
    static final String DS_VAR = "ds";

    private static final Set<String> ALLOWED_RENDER_TYPES = new HashSet<>(Arrays.asList("html", "xlsx", "pdf"));

    @Inject
    IReportEngine reportEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpFinArApItemBiz arApItemBiz;

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
            throw new NopException(ErpHrErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpHrErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("hr-rpt");
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
     * <p>接受 {@code employee-net-balance}、{@code employee-net-balance.xpt.xml} 两种形式，统一补全到
     * {@code /nop/main/report/hr/employee-net-balance.xpt.xml}。
     */
    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpHrErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpHrErrors.ARG_REPORT_NAME, reportName);
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
    private void prepareDataset(String reportName, Map<String, Object> data, IServiceContext context) {
        String key = baseName(reportName);
        if (data.containsKey(DS_VAR)) return;
        switch (key) {
            case "employee-net-balance":
                data.put(DS_VAR, buildEmployeeNetBalanceDataset(context));
                break;
            case "payroll-simulation-comparison":
                data.put(DS_VAR, buildPayrollSimulationComparisonDataset(asLong(data, "simulationId"), context));
                break;
            default:
                // 未知报表：不自动装配，模板可经 beforeExpand 自行构造数据集
                break;
        }
    }

    private static Long asLong(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        return v == null ? null : Long.valueOf(v.toString());
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 员工净余额数据集：按员工聚合预支应收/报销应付净额，对齐 {@code expense-claim.md}。 */
    @BizQuery
    public List<Map<String, Object>> employeeNetBalanceData(IServiceContext context) {
        return buildEmployeeNetBalanceDataset(context);
    }

    /** 薪酬模拟对比数据集：源 vs 模拟三列对比 + 部门小计，对齐 {@code payroll-simulation.md}。 */
    @BizQuery
    public List<Map<String, Object>> payrollSimulationComparisonData(@Optional @Name("simulationId") Long simulationId,
                                                                     IServiceContext context) {
        return buildPayrollSimulationComparisonDataset(simulationId, context);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 员工净余额数据集。经 {@link IErpFinArApItemBiz#findOpenItems} 跨域只读聚合 finance 辅助账：
     * <ul>
     *   <li>预支余额 = {@code direction=RECEIVABLE} + {@code sourceBillType=EMPLOYEE_ADVANCE} 的 openAmountFunction 合计；</li>
     *   <li>报销余额 = {@code direction=PAYABLE} + {@code sourceBillType=EXPENSE_CLAIM} 的 openAmountFunctional 合计；</li>
     *   <li>净额 = 预支余额 − 报销余额（按 partnerId/员工分组）。</li>
     * </ul>
     *
     * <p>{@code findOpenItems(direction)} 不带 sourceBillType 过滤，故按方向取数后在聚合层按 sourceBillType 二次过滤，
     * 避免混入客户 AR / 供应商 AP（对齐 plan Phase 2 Decision + {@code ErpFinArApItemGenerator.java:150,153} 口径）。
     */
    List<Map<String, Object>> buildEmployeeNetBalanceDataset(IServiceContext context) {
        Map<Long, BigDecimal> advanceByPartner = sumEmployeeItems(
                ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_EMPLOYEE_ADVANCE, context);
        Map<Long, BigDecimal> expenseByPartner = sumEmployeeItems(
                ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_EXPENSE_CLAIM, context);
        Set<Long> partnerIds = new TreeSet<>();
        partnerIds.addAll(advanceByPartner.keySet());
        partnerIds.addAll(expenseByPartner.keySet());
        if (partnerIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> partnerNames = resolvePartnerNames(partnerIds);
        List<Map<String, Object>> rows = new ArrayList<>(partnerIds.size());
        for (Long partnerId : partnerIds) {
            BigDecimal advance = nz(advanceByPartner.get(partnerId));
            BigDecimal expense = nz(expenseByPartner.get(partnerId));
            BigDecimal net = advance.subtract(expense);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("partnerId", partnerId);
            r.put("partnerName", partnerNames.getOrDefault(partnerId, ""));
            r.put("advanceBalance", advance);
            r.put("expenseBalance", expense);
            r.put("netBalance", net);
            r.put("netDirection", net.signum() > 0 ? "员工欠公司" : (net.signum() < 0 ? "公司欠员工" : "结平"));
            rows.add(r);
        }
        return rows;
    }

    private Map<Long, BigDecimal> sumEmployeeItems(String direction, String sourceBillType, IServiceContext context) {
        List<ErpFinArApItem> items = arApItemBiz.findOpenItems(direction, context);
        if (items == null || items.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BigDecimal> sums = new HashMap<>();
        for (ErpFinArApItem item : items) {
            if (!sourceBillType.equals(item.getSourceBillType())) {
                continue;
            }
            Long partnerId = item.getPartnerId();
            if (partnerId == null) {
                continue;
            }
            sums.merge(partnerId, nz(item.getOpenAmountFunctional()), BigDecimal::add);
        }
        return sums;
    }

    private Map<Long, String> resolvePartnerNames(Set<Long> partnerIds) {
        // 按 partnerId 批量解析名称（ErpMdPartner 在 master-data，hr-service 经 finance-service 传递依赖可访问）
        Map<Long, String> names = new HashMap<>();
        if (partnerIds.isEmpty()) {
            return names;
        }
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.in("id", partnerIds));
        for (ErpMdPartner p : dao.findAllByQuery(q)) {
            names.put(p.getId(), p.getName());
        }
        return names;
    }

    /**
     * 薪酬模拟对比数据集。从同域 {@link ErpHrSalarySimulationItemAdjustment}（2208-3 落地）聚合：
     * 每行 = 员工 × 薪酬项目（employeeId/employeeName/departmentId/salaryItemCode/originalAmount/adjustedAmount/difference），
     * 末尾追加按部门小计行（对齐 {@code payroll-simulation.md} {@code getComparison}/{@code getDepartmentSummary} 口径）。
     */
    List<Map<String, Object>> buildPayrollSimulationComparisonDataset(Long simulationId, IServiceContext context) {
        if (simulationId == null) {
            return Collections.emptyList();
        }
        List<ErpHrSalarySimulationItemAdjustment> adjustments = loadAdjustments(simulationId);
        if (adjustments.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, ErpHrEmployee> employees = loadEmployees(adjustments);
        Map<Long, BigDecimal> deptDiff = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>(adjustments.size());
        for (ErpHrSalarySimulationItemAdjustment adj : adjustments) {
            BigDecimal original = nz(adj.getOriginalAmount());
            BigDecimal adjusted = nz(adj.getAdjustedAmount());
            BigDecimal diff = adjusted.subtract(original);
            ErpHrEmployee emp = employees.get(adj.getEmployeeId());
            Long departmentId = emp != null ? emp.getDepartmentId() : null;
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("simulationId", simulationId);
            r.put("employeeId", adj.getEmployeeId());
            r.put("employeeName", emp != null ? emp.getFullName() : null);
            r.put("departmentId", departmentId);
            r.put("salaryItemCode", adj.getSalaryItemCode());
            r.put("originalAmount", original);
            r.put("adjustedAmount", adjusted);
            r.put("difference", diff);
            r.put("rowType", "DETAIL");
            rows.add(r);
            if (departmentId != null) {
                deptDiff.merge(departmentId, diff, BigDecimal::add);
            }
        }
        // 部门小计行（对齐 getDepartmentSummary 口径）
        for (Map.Entry<Long, BigDecimal> e : deptDiff.entrySet()) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("departmentId", e.getKey());
            r.put("employeeName", "部门小计");
            r.put("difference", e.getValue());
            r.put("rowType", "DEPT_SUBTOTAL");
            rows.add(r);
        }
        return rows;
    }

    private List<ErpHrSalarySimulationItemAdjustment> loadAdjustments(Long simulationId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("simulationId", simulationId));
        q.addOrderField("employeeId", false);
        q.addOrderField("salaryItemCode", false);
        return daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class).findAllByQuery(q);
    }

    private Map<Long, ErpHrEmployee> loadEmployees(List<ErpHrSalarySimulationItemAdjustment> adjustments) {
        Set<Long> empIds = new HashSet<>();
        for (ErpHrSalarySimulationItemAdjustment adj : adjustments) {
            if (adj.getEmployeeId() != null) {
                empIds.add(adj.getEmployeeId());
            }
        }
        if (empIds.isEmpty()) {
            return Collections.emptyMap();
        }
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        QueryBean q = new QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.in("id", empIds));
        List<ErpHrEmployee> employees = dao.findAllByQuery(q);
        Map<Long, ErpHrEmployee> map = new HashMap<>();
        for (ErpHrEmployee e : employees) {
            map.put(e.getId(), e);
        }
        return map;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
