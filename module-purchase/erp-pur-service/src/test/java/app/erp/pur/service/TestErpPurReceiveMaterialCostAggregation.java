package app.erp.pur.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReceiveLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
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
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 采购入库审核 → 项目物料成本归集接线端到端单测（RC-R1.61 / P1-RC-049，方向 C：purchase 侧触发经既有边）。
 * 验证：
 * <ul>
 *   <li>入库审核（订单行标 projectId）→ 移动单生成后同事务归集 → {@code erp_prj_cost_collection_line}
 *       生成（costCategory=MATERIAL / sourceBillType=PURCHASE_RECEIVE / amount=入库行金额不含税）。</li>
 *   <li>订单行 projectId 为 null → 跳过归集，审核正常。</li>
 *   <li>STRICT 预算超限 → 审核被拒（{@code ERR_BUDGET_EXCEEDED}），入库单保持 SUBMITTED（L1 UC-PRJ-04
 *       「采购审核拒绝该笔归集」）。</li>
 * </ul>
 *
 * <p>归集守卫链（requireReferenceable / 预算检查 / 幂等）行为细节在 projects 侧
 * {@code TestErpPrjMaterialAggregation} 覆盖，本测试验证跨域接线链路真实生效。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurReceiveMaterialCostAggregation extends JunitAutoTestCase {

    static final Long ORG_ID = 1401L;
    static final Long SUPPLIER_ID = 2401L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long MATERIAL_ID = 4401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long ACCT_SCHEMA_ID = 7401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testApproveTriggersMaterialAggregation() {
        seedPrereqs();
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long projectId = seedProject("PRJ-PUR-001", "采购归集项目", ErpPrjConstants.PROJECT_STATUS_OPEN,
                    new BigDecimal("100000"));
            projectHolder[0] = projectId;
            Long orderId = newOrder("PO-MAT-001");
            newOrderLine(orderId, 9401L, 1, new BigDecimal("10"), new BigDecimal("5"), projectId);
            newReceive("PR-MAT-001", 9402L, orderId);
            newReceiveLine(9403L, 9402L, 9401L, new BigDecimal("10"), new BigDecimal("5"),
                    new BigDecimal("50"));
            return null;
        });

        assertEquals(0, submit(9402L).getStatus());
        ApiResponse<?> resp = approve(9402L);
        assertEquals(0, resp.getStatus(), "审核通过（项目 OPEN + 预算充足）");
        ErpPurReceive approved = daoProvider.daoFor(ErpPurReceive.class).getEntityById(9402L);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus());

        // 归集行已生成（sourceBillCode=入库单号-行号）
        ErpPrjCostCollectionLine line = findCollectionLine(
                ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE, "PR-MAT-001-1");
        assertNotNull(line, "入库审核后 MATERIAL 归集行生成");
        assertEquals(ErpPrjConstants.COST_CATEGORY_MATERIAL, line.getCostCategory());
        assertEquals(0, line.getAmount().compareTo(new BigDecimal("50.0000")),
                "amount=入库行金额(不含税) 50");
        ErpPrjCostCollection head = daoProvider.daoFor(ErpPrjCostCollection.class)
                .getEntityById(line.getCostCollectionId());
        assertEquals(projectHolder[0], head.getProjectId(),
                "归集行归属项目 = 订单行 projectId");
    }

    @Test
    public void testNullProjectIdSkipsAggregation() {
        seedPrereqs();
        ormTemplate.runInSession(session -> {
            seedActiveSupplier();
            Long orderId = newOrder("PO-MAT-NP-001");
            newOrderLine(orderId, 9501L, 1, new BigDecimal("10"), new BigDecimal("5"), null);
            newReceive("PR-MAT-NP-001", 9502L, orderId);
            newReceiveLine(9503L, 9502L, 9501L, new BigDecimal("10"), new BigDecimal("5"),
                    new BigDecimal("50"));
            return null;
        });

        assertEquals(0, submit(9502L).getStatus());
        ApiResponse<?> resp = approve(9502L);
        assertEquals(0, resp.getStatus(), "projectId null 行跳过归集，审核正常");
        assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE, "PR-MAT-NP-001-1"),
                "projectId null 不生成归集行");
    }

    @Test
    public void testStrictBudgetRejectsApproval() {
        System.setProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE, "STRICT");
        try {
            seedPrereqs();
            ormTemplate.runInSession(session -> {
                seedActiveSupplier();
                // 总预算 30 < 入库行金额 50 → STRICT 拒绝
                Long projectId = seedProject("PRJ-PUR-S-001", "STRICT 采购项目",
                        ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("30"));
                Long orderId = newOrder("PO-MAT-S-001");
                newOrderLine(orderId, 9601L, 1, new BigDecimal("10"), new BigDecimal("5"), projectId);
                newReceive("PR-MAT-S-001", 9602L, orderId);
                newReceiveLine(9603L, 9602L, 9601L, new BigDecimal("10"), new BigDecimal("5"),
                        new BigDecimal("50"));
                return null;
            });

            assertEquals(0, submit(9602L).getStatus());
            ApiResponse<?> resp = approve(9602L);
            assertEquals(ErpPrjErrors.ERR_BUDGET_EXCEEDED.getErrorCode(), resp.getCode(),
                    "STRICT 超预算 → 审核拒绝 ERR_BUDGET_EXCEEDED（L1 UC-PRJ-04）");
            ErpPurReceive receive = daoProvider.daoFor(ErpPurReceive.class).getEntityById(9602L);
            assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED, receive.getApproveStatus(),
                    "拒绝后入库单保持 SUBMITTED（未批准）");
            assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE,
                    "PR-MAT-S-001-1"), "拒绝时不生成归集行");
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE);
        }
    }

    @Test
    public void testMaterialAggregationDisabledByConfig() {
        System.setProperty(ErpPrjConstants.CONFIG_MATERIAL_AGGREGATION_ENABLED, "false");
        try {
            seedPrereqs();
            ormTemplate.runInSession(session -> {
                seedActiveSupplier();
                Long projectId = seedProject("PRJ-PUR-OFF-001", "禁用归集项目",
                        ErpPrjConstants.PROJECT_STATUS_OPEN, new BigDecimal("100000"));
                Long orderId = newOrder("PO-MAT-OFF-001");
                newOrderLine(orderId, 9701L, 1, new BigDecimal("10"), new BigDecimal("5"), projectId);
                newReceive("PR-MAT-OFF-001", 9702L, orderId);
                newReceiveLine(9703L, 9702L, 9701L, new BigDecimal("10"), new BigDecimal("5"),
                        new BigDecimal("50"));
                return null;
            });

            assertEquals(0, submit(9702L).getStatus());
            ApiResponse<?> resp = approve(9702L);
            assertEquals(0, resp.getStatus(), "config-gated 关闭时审核正常（零副作用）");
            assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE,
                    "PR-MAT-OFF-001-1"), "config-gated 关闭不生成归集行");
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_MATERIAL_AGGREGATION_ENABLED);
        }
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(receiveId))));
    }

    private ApiResponse<?> approve(Long receiveId) {
        return executeRpc(mutation, "ErpPurReceive__approve",
                ApiRequest.build(Map.of("id", String.valueOf(receiveId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedPrereqs() {
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品");
            seedSubject("2202", "应付账款-暂估");
            seedSubject("6401", "主营业务成本");
            seedAcctSchema();
            return null;
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setId(ACCT_SCHEMA_ID);
        schema.setCode("AS-" + ORG_ID);
        schema.setName("账套" + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.setStatus("ACTIVE");
        dao.saveEntity(schema);
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

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass("ASSET");
        subject.setDirection("DEBIT");
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private Long seedProject(String code, String name, String status, BigDecimal budget) {
        IEntityDao<ErpPrjProjectType> typeDao = daoProvider.daoFor(ErpPrjProjectType.class);
        ErpPrjProjectType type = new ErpPrjProjectType();
        type.setCode("PT-PUR-" + code);
        type.setName(name + "类型");
        typeDao.saveEntity(type);

        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId(ORG_ID);
        p.setProjectTypeId(type.getId());
        p.setCurrencyId(CURRENCY_ID);
        p.setStatus(status);
        p.setBudget(budget);
        p.setActualCost(BigDecimal.ZERO);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long newOrder(String code) {
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_ACTIVE);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        dao.saveEntity(order);
        return order.getId();
    }

    private void newOrderLine(Long orderId, Long lineId, int lineNo, BigDecimal qty, BigDecimal unitPrice,
                              Long projectId) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setId(lineId);
        line.setOrderId(orderId);
        line.setLineNo(lineNo);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(qty.multiply(unitPrice));
        line.setProjectId(projectId);
        dao.saveEntity(line);
    }

    private void newReceive(String code, Long receiveId, Long orderId) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive receive = new ErpPurReceive();
        receive.setId(receiveId);
        receive.setCode(code);
        receive.setOrgId(ORG_ID);
        receive.setOrderId(orderId);
        receive.setSupplierId(SUPPLIER_ID);
        receive.setWarehouseId(WAREHOUSE_ID);
        receive.setBusinessDate(LocalDate.of(2026, 7, 1));
        receive.setCurrencyId(CURRENCY_ID);
        receive.setExchangeRate(new BigDecimal("1"));
        receive.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        receive.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        receive.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        receive.setPosted(false);
        dao.saveEntity(receive);
    }

    private void newReceiveLine(Long lineId, Long receiveId, Long orderLineId, BigDecimal qty,
                                BigDecimal unitPrice, BigDecimal amount) {
        IEntityDao<ErpPurReceiveLine> dao = daoProvider.daoFor(ErpPurReceiveLine.class);
        ErpPurReceiveLine line = new ErpPurReceiveLine();
        line.setId(lineId);
        line.setReceiveId(receiveId);
        line.setLineNo(1);
        line.setOrderLineId(orderLineId);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(qty);
        line.setUnitPrice(unitPrice);
        line.setAmount(amount);
        dao.saveEntity(line);
    }

    private ErpPrjCostCollectionLine findCollectionLine(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        List<ErpPrjCostCollectionLine> lines = dao.findAllByQuery(q);
        return lines.isEmpty() ? null : lines.get(0);
    }
}
