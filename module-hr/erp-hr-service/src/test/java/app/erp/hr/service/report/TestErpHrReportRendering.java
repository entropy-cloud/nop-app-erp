package app.erp.hr.service.report;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * HR 报表渲染端到端测试（plan 2026-07-06-1247-1 Phase 2/3 Proof）。
 *
 * <p>覆盖两张 HR 报表（员工净余额 / 薪酬模拟对比）的 {@code renderHtml}/{@code download(xlsx|pdf)}
 * 渲染管线、数据集口径断言、空数据集不报错、以及路径注入防护（非法 reportName 抛
 * {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1L;
    static final Long ACCT_SCHEMA_ID = 1L;
    static final Long CURRENCY_ID = 1L;
    static final Long PARTNER_EMP_1 = 5001L;
    static final Long PARTNER_EMP_2 = 5002L;
    static final Long DEPT_1 = 4101L;
    static final Long EMP_SIM_1 = 9001L;
    static final Long EMP_SIM_2 = 9002L;
    static final Long SIMULATION_ID = 8001L;

    @Inject
    ErpHrReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== Phase 2: 员工净余额报表 =====================

    @Test
    public void testEmployeeNetBalanceReportRenderHtml() {
        seedEmployeeNetBalanceBaseline();
        String html = reportBiz.renderHtml("employee-net-balance", null, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("EMP-NM-1"), "renderHtml 含员工名");
    }

    @Test
    public void testEmployeeNetBalanceReportDownloadXlsxAndPdf() {
        seedEmployeeNetBalanceBaseline();
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("employee-net-balance", renderType, null, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertNotNull(content, "download 内容非空: " + renderType);
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testEmployeeNetBalanceDataset() {
        seedEmployeeNetBalanceBaseline();
        List<Map<String, Object>> ds = reportBiz.buildEmployeeNetBalanceDataset(CTX);
        assertFalse(ds.isEmpty(), "员工净余额数据集非空");
        // emp1: 预支 1000 − 报销 300 = 净 700（员工欠公司）
        Map<String, Object> emp1 = findRowByPartner(ds, PARTNER_EMP_1);
        assertNotNull(emp1, "数据集含 emp1 行");
        assertEquals(0, bd("1000").compareTo(toBd(emp1.get("advanceBalance"))), "emp1 预支余额=1000");
        assertEquals(0, bd("300").compareTo(toBd(emp1.get("expenseBalance"))), "emp1 报销余额=300");
        assertEquals(0, bd("700").compareTo(toBd(emp1.get("netBalance"))), "emp1 净余额=700");
        assertEquals("员工欠公司", emp1.get("netDirection"), "净额>0 → 员工欠公司");
    }

    @Test
    public void testEmployeeNetBalanceDatasetEmployeeAdvanceOnly() {
        // 仅预支、无报销的员工：净额 = 预支全额
        seedPartner(PARTNER_EMP_2, "EMP-NM-2");
        seedArApItem(6002L, PARTNER_EMP_2, bd("500"),
                ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_EMPLOYEE_ADVANCE);
        List<Map<String, Object>> ds = reportBiz.buildEmployeeNetBalanceDataset(CTX);
        Map<String, Object> emp2 = findRowByPartner(ds, PARTNER_EMP_2);
        assertNotNull(emp2, "数据集含 emp2 行");
        assertEquals(0, bd("500").compareTo(toBd(emp2.get("netBalance"))), "emp2 净余额=500");
        assertEquals(0, BigDecimal.ZERO.compareTo(toBd(emp2.get("expenseBalance"))), "emp2 报销余额=0");
    }

    @Test
    public void testEmployeeNetBalanceExcludesNonEmployeeItems() {
        // 同方向但非员工 sourceBillType 的项不应混入（客户 AR 不计入员工净余额）
        seedPartner(PARTNER_EMP_1, "EMP-NM-1");
        seedArApItem(7001L, PARTNER_EMP_1, bd("9999"),
                ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        List<Map<String, Object>> ds = reportBiz.buildEmployeeNetBalanceDataset(CTX);
        assertTrue(ds.isEmpty(), "客户 AR 项不应进入员工净余额数据集");
    }

    @Test
    public void testEmployeeNetBalanceEmptyDatasetNoError() {
        // 无数据：不报错，返回空列表
        List<Map<String, Object>> ds = reportBiz.buildEmployeeNetBalanceDataset(CTX);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无员工辅助账 → 空列表");
    }

    // ===================== 路径注入防护 =====================

    @Test
    public void testPathInjectionRejected() {
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml("../etc/passwd", null, CTX),
                "非法 reportName（含 ../）抛 NopException");
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml(null, null, CTX),
                "空 reportName 抛 NopException");
        seedEmployeeNetBalanceBaseline();
        assertThrows(NopException.class,
                () -> reportBiz.download("employee-net-balance", "exe", null, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== Phase 3: 薪酬模拟对比报表 =====================

    @Test
    public void testPayrollSimulationComparisonReportRenderHtml() {
        seedPayrollSimulationBaseline();
        Map<String, Object> data = new HashMap<>();
        data.put("simulationId", SIMULATION_ID);
        String html = reportBiz.renderHtml("payroll-simulation-comparison", data, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("SIM-EMP-1"), "renderHtml 含员工名");
    }

    @Test
    public void testPayrollSimulationComparisonReportDownloadXlsxAndPdf() {
        seedPayrollSimulationBaseline();
        Map<String, Object> data = new HashMap<>();
        data.put("simulationId", SIMULATION_ID);
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("payroll-simulation-comparison", renderType, data, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertNotNull(content, "download 内容非空: " + renderType);
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testPayrollSimulationComparisonDataset() {
        seedPayrollSimulationBaseline();
        List<Map<String, Object>> ds = reportBiz.buildPayrollSimulationComparisonDataset(SIMULATION_ID, CTX);
        assertFalse(ds.isEmpty(), "模拟对比数据集非空");
        // emp1 basicSalary: 原 10000 → 调整 12000，差异 +2000
        Map<String, Object> basicRow = findRow(ds, EMP_SIM_1, "basicSalary");
        assertNotNull(basicRow, "数据集含 emp1 basicSalary 行");
        assertEquals("DETAIL", basicRow.get("rowType"), "明细行 rowType=DETAIL");
        assertEquals(0, bd("10000").compareTo(toBd(basicRow.get("originalAmount"))), "原值=10000");
        assertEquals(0, bd("12000").compareTo(toBd(basicRow.get("adjustedAmount"))), "调整值=12000");
        assertEquals(0, bd("2000").compareTo(toBd(basicRow.get("difference"))), "差异=+2000");
    }

    @Test
    public void testPayrollSimulationComparisonDeptSubtotal() {
        seedPayrollSimulationBaseline();
        List<Map<String, Object>> ds = reportBiz.buildPayrollSimulationComparisonDataset(SIMULATION_ID, CTX);
        // DEPT_1 含 emp1(+2000) + emp2(-500) → 部门小计差异 +1500
        Map<String, Object> subtotal = findDeptSubtotal(ds, DEPT_1);
        assertNotNull(subtotal, "数据集含部门小计行");
        assertEquals("DEPT_SUBTOTAL", subtotal.get("rowType"), "小计行 rowType=DEPT_SUBTOTAL");
        assertEquals(0, bd("1500").compareTo(toBd(subtotal.get("difference"))), "DEPT_1 小计差异=+1500");
    }

    @Test
    public void testPayrollSimulationComparisonEmptyDatasetNoError() {
        // 无调整项的模拟：不报错，返回空列表
        List<Map<String, Object>> ds = reportBiz.buildPayrollSimulationComparisonDataset(999999L, CTX);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "无调整项 → 空列表");
    }

    @Test
    public void testPayrollSimulationComparisonNullSimulationIdNoError() {
        List<Map<String, Object>> ds = reportBiz.buildPayrollSimulationComparisonDataset(null, CTX);
        assertNotNull(ds, "null simulationId 不报错");
        assertTrue(ds.isEmpty(), "null simulationId → 空列表");
    }

    // ===================== 数据准备 =====================

    private void seedEmployeeNetBalanceBaseline() {
        seedPartner(PARTNER_EMP_1, "EMP-NM-1");
        // emp1 预支 1000（应收方向）
        seedArApItem(6001L, PARTNER_EMP_1, bd("1000"),
                ErpFinConstants.DIRECTION_RECEIVABLE, ErpFinConstants.SOURCE_BILL_EMPLOYEE_ADVANCE);
        // emp1 报销 300（应付方向）
        seedArApItem(6011L, PARTNER_EMP_1, bd("300"),
                ErpFinConstants.DIRECTION_PAYABLE, ErpFinConstants.SOURCE_BILL_EXPENSE_CLAIM);
    }

    private void seedPartner(long id, String name) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            ErpMdPartner p = dao.newEntity();
            p.orm_propValue(1, id);
            p.setCode("P-" + id);
            p.setName(name);
            p.setPartnerType("EMPLOYEE");
            p.setStatus("ACTIVE");
            p.setReceivableBalance(BigDecimal.ZERO);
            p.setPayableBalance(BigDecimal.ZERO);
            dao.saveEntity(p);
        });
    }

    private void seedArApItem(long id, long partnerId, BigDecimal openAmount,
                              String direction, String sourceBillType) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
            ErpFinArApItem it = dao.newEntity();
            it.orm_propValue(1, id);
            it.setCode("HR-RPT-" + id);
            it.setOrgId(ORG_ID);
            it.setAcctSchemaId(ACCT_SCHEMA_ID);
            it.setDirection(direction);
            it.setPartnerId(partnerId);
            it.setSourceBillType(sourceBillType);
            it.setSourceBillCode("HR-BILL-" + id);
            it.setBusinessDate(LocalDate.of(2026, 7, 1));
            it.setCurrencyId(CURRENCY_ID);
            it.setExchangeRate(BigDecimal.ONE);
            it.setAmountSource(openAmount);
            it.setAmountFunctional(openAmount);
            it.setSettledAmountSource(BigDecimal.ZERO);
            it.setSettledAmountFunctional(BigDecimal.ZERO);
            it.setOpenAmountSource(openAmount);
            it.setOpenAmountFunctional(openAmount);
            it.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
            dao.saveEntity(it);
        });
    }

    private void seedPayrollSimulationBaseline() {
        seedEmployee(EMP_SIM_1, "SIM-EMP-1", DEPT_1);
        seedEmployee(EMP_SIM_2, "SIM-EMP-2", DEPT_1);
        // emp1 basicSalary: 10000 → 12000，差异 +2000
        seedAdjustment(9101L, EMP_SIM_1, "basicSalary", bd("10000"), bd("12000"));
        // emp2 performanceBonus: 3000 → 2500，差异 -500
        seedAdjustment(9102L, EMP_SIM_2, "performanceBonus", bd("3000"), bd("2500"));
    }

    private void seedEmployee(long id, String fullName, Long departmentId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
            ErpHrEmployee e = dao.newEntity();
            e.orm_propValue(1, id);
            e.setCode("EMP-" + id);
            e.setFirstName(fullName);
            e.setLastName(fullName);
            e.setFullName(fullName);
            e.orm_propValueByName("gender", "MALE");
            e.setHireDate(LocalDate.of(2024, 1, 1));
            e.orm_propValueByName("employmentStatus", "ACTIVE");
            e.orm_propValueByName("employeeType", "REGULAR");
            e.setDepartmentId(departmentId);
            dao.saveEntity(e);
        });
    }

    private void seedAdjustment(long id, long employeeId, String salaryItemCode,
                                BigDecimal original, BigDecimal adjusted) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpHrSalarySimulationItemAdjustment> dao =
                    daoProvider.daoFor(ErpHrSalarySimulationItemAdjustment.class);
            ErpHrSalarySimulationItemAdjustment adj = dao.newEntity();
            adj.orm_propValue(1, id);
            adj.setSimulationId(SIMULATION_ID);
            adj.setEmployeeId(employeeId);
            adj.setSalaryItemCode(salaryItemCode);
            adj.setOriginalAmount(original);
            adj.setAdjustedAmount(adjusted);
            dao.saveEntity(adj);
        });
    }

    // ---------- helpers ----------

    private static Map<String, Object> findRowByPartner(List<Map<String, Object>> ds, long partnerId) {
        for (Map<String, Object> row : ds) {
            if (Long.valueOf(partnerId).equals(row.get("partnerId"))) {
                return row;
            }
        }
        return null;
    }

    private static Map<String, Object> findRow(List<Map<String, Object>> ds, long employeeId, String salaryItemCode) {
        for (Map<String, Object> row : ds) {
            if ("DETAIL".equals(row.get("rowType"))
                    && Long.valueOf(employeeId).equals(row.get("employeeId"))
                    && salaryItemCode.equals(row.get("salaryItemCode"))) {
                return row;
            }
        }
        return null;
    }

    private static Map<String, Object> findDeptSubtotal(List<Map<String, Object>> ds, long departmentId) {
        for (Map<String, Object> row : ds) {
            if ("DEPT_SUBTOTAL".equals(row.get("rowType"))
                    && Long.valueOf(departmentId).equals(row.get("departmentId"))) {
                return row;
            }
        }
        return null;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(String.valueOf(v));
    }
}
