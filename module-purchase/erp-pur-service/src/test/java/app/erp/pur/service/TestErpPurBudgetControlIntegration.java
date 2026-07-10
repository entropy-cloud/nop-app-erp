package app.erp.pur.service;

import app.erp.fin.biz.IErpFinBudgetScenarioBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.dao.entity.ErpFinBudgetControlLog;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预算控制跨域集成测试（plan 2026-07-10-1100-4 §Phase 3 Proof）。验证采购订单审核时预算控制 SPI
 * （{@code IErpFinBudgetControlBiz}）经 {@code ErpPurOrderProcessor.validateBusinessRulesForApprove} 钩子触发，
 * 按 {@code erp-fin.budget-check-enabled} + {@code erp-fin.budget-purchase-expense-subject-code} 配置门控。
 *
 * <p>覆盖：HARD 拦截 / WARN 放行写日志 / 无预算匹配放行（向后兼容）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:budget-control-test.yaml")
public class TestErpPurBudgetControlIntegration extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();
    private static final Long SUPPLIER_ID = 2101L;
    private static final Long WAREHOUSE_ID = 3101L;
    private static final Long MATERIAL_ID = 4101L;
    private static final Long UOM_ID = 5101L;
    private static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpFinBudgetScenarioBiz scenarioBiz;

    @Test
    public void testPurchaseOrderHardBlocked() {
        Long scenarioId = ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long periodId = seedPeriod("2026-07", 2026, 7);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.DC_CREDIT);
            // 预算 100（HARD），订单将申请 200 → 超预算拦截
            return seedDraftBudgetScenario("BUD-HARD", periodId, expense, income, new BigDecimal("100"),
                    ErpFinConstants.BUDGET_CONTROL_HARD);
        });
        // 审批预算方案（生成 BUDGET 影子凭证）—— 在 seed session 提交后调用，避免嵌套会话 update-entity-not-managed
        scenarioBiz.submit(scenarioId, CTX);
        scenarioBiz.approve(scenarioId, CTX);

        ErpPurOrder order = newOrder("PO-BUD-HARD", new BigDecimal("200"));
        ormTemplate.runInSession(() -> saveOrderWithLine(order));
        assertEquals(0, submit(order.getId()).getStatus(), "提交应成功");

        ApiResponse<?> bad = approve(order.getId());
        assertEquals(-1, bad.getStatus(), "HARD 超预算应拦截审核");
        assertEquals(ErpFinErrors.ERR_BUDGET_EXCEEDED.getErrorCode(), bad.getCode(),
                "应返回 ERR_BUDGET_EXCEEDED");
    }

    @Test
    public void testPurchaseOrderWarnPassedWithLog() {
        Long scenarioId = ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long periodId = seedPeriod("2026-08", 2026, 8);
            ErpMdSubject expense = seedSubject("6601", "销售费用", ErpFinConstants.DC_DEBIT);
            ErpMdSubject income = seedSubject("6001", "主营业务收入", ErpFinConstants.DC_CREDIT);
            return seedDraftBudgetScenario("BUD-WARN", periodId, expense, income, new BigDecimal("100"),
                    ErpFinConstants.BUDGET_CONTROL_WARN);
        });
        scenarioBiz.submit(scenarioId, CTX);
        scenarioBiz.approve(scenarioId, CTX);

        ErpPurOrder order = newOrderWithDate("PO-BUD-WARN", new BigDecimal("200"), LocalDate.of(2026, 8, 15));
        ormTemplate.runInSession(() -> saveOrderWithLine(order));
        assertEquals(0, submit(order.getId()).getStatus(), "提交应成功");

        ApiResponse<?> resp = approve(order.getId());
        assertEquals(0, resp.getStatus(), "WARN 模式超预算应放行审核");
        assertTrue(countControlLogs("PO-BUD-WARN") >= 1, "WARN 应写预算控制日志");
    }

    @Test
    public void testNoBudgetLineMatchedPasses() {
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            seedPeriod("2026-09", 2026, 9);
            seedSubject("6601", "销售费用", ErpFinConstants.DC_DEBIT);
            seedSubject("6001", "主营业务收入", ErpFinConstants.DC_CREDIT);
            // 无预算方案 → 无匹配预算行 → check 返回 PASS（向后兼容）
            return null;
        });

        ErpPurOrder order = newOrderWithDate("PO-BUD-NONE", new BigDecimal("9999"), LocalDate.of(2026, 9, 15));
        ormTemplate.runInSession(() -> saveOrderWithLine(order));
        assertEquals(0, submit(order.getId()).getStatus(), "提交应成功");

        assertEquals(0, approve(order.getId()).getStatus(),
                "无匹配预算行时审核放行（向后兼容：未预算的维度不控制）");
    }

    // ---------- helpers ----------

    private Long seedDraftBudgetScenario(String code, Long periodId, ErpMdSubject expense,
                                         ErpMdSubject income, BigDecimal amount, String controlLevel) {
        IEntityDao<ErpFinBudgetScenario> sDao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario s = sDao.newEntity();
        s.setCode(code);
        s.setName(code);
        s.setOrgId(1L);
        s.setAcctSchemaId(1L);
        s.setFiscalYear(2026);
        s.setScenarioType("ANNUAL");
        s.setCurrencyId(1L);
        s.setExchangeRate(BigDecimal.ONE);
        s.setControlLevel(controlLevel);
        s.setDocStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        s.setApproveStatus(ErpFinConstants.BUDGET_STATUS_DRAFT);
        sDao.saveEntity(s);

        IEntityDao<ErpFinBudgetLine> lDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        lDao.saveEntity(newBudgetLine(s.getId(), periodId, expense, amount, 1));
        lDao.saveEntity(newBudgetLine(s.getId(), periodId, income, amount, 2));
        return s.getId();
    }

    private ErpFinBudgetLine newBudgetLine(Long scenarioId, Long periodId, ErpMdSubject subject,
                                           BigDecimal amount, int lineNo) {
        ErpFinBudgetLine l = new ErpFinBudgetLine();
        l.setScenarioId(scenarioId);
        l.setLineNo(lineNo);
        l.setOrgId(1L);
        l.setAcctSchemaId(1L);
        l.setPeriodId(periodId);
        l.setSubjectId(subject.getId());
        l.setSubjectCode(subject.getCode());
        l.setBudgetAmountSource(amount);
        l.setBudgetAmountFunctional(amount);
        l.setCurrencyId(1L);
        l.setExchangeRate(BigDecimal.ONE);
        return l;
    }

    private Long seedPeriod(String code, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private ErpMdSubject seedSubject(String code, String name, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(ErpFinConstants.SUBJECT_CLASS_EXPENSE);
        s.setDirection(direction);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }

    private void seedActiveSupplier() {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(SUPPLIER_ID);
        partner.setCode("SUP-" + SUPPLIER_ID);
        partner.setName("供应商" + SUPPLIER_ID);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private ErpPurOrder newOrder(String code, BigDecimal totalWithTax) {
        return newOrderWithDate(code, totalWithTax, LocalDate.of(2026, 7, 15));
    }

    private ErpPurOrder newOrderWithDate(String code, BigDecimal totalWithTax, LocalDate date) {
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(code);
        order.setOrgId(1101L);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(date);
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(BigDecimal.ONE);
        order.setTotalAmountWithTax(totalWithTax);
        order.setTotalAmount(totalWithTax);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        order.setPosted(false);
        return order;
    }

    private void saveOrderWithLine(ErpPurOrder order) {
        daoProvider.daoFor(ErpPurOrder.class).saveEntity(order);
        IEntityDao<ErpPurOrderLine> lineDao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal("10"));
        line.setUnitPrice(order.getTotalAmountWithTax().divide(new BigDecimal("10")));
        line.setAmount(order.getTotalAmountWithTax());
        lineDao.saveEntity(line);
    }

    private int countControlLogs(String sourceBillCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillCode", sourceBillCode));
        return daoProvider.daoFor(ErpFinBudgetControlLog.class).findAllByQuery(q).size();
    }

    private ApiResponse<?> submit(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> approve(Long orderId) {
        return executeRpc(mutation, "ErpPurOrder__approve",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
