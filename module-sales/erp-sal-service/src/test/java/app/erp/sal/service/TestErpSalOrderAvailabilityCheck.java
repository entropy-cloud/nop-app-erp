package app.erp.sal.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 订单级可用量预校验测试矩阵（RC-R1.13，P1-RC-020）：
 * ① 默认 OFF 跳过（不设 config，库存不足订单 approve 通过——既有行为回归）；
 * ② WARN 不足放行；③ HARD 不足拒绝（approveStatus 保持 SUBMITTED）；
 * ④ 足够放行；⑤ 行级 warehouseId 回退订单头；⑥ 多行部分不足（HARD 拒绝）。
 *
 * <p>跨域余额读取经 {@code ErpSalOrderProcessor.validateOrderAvailability} → {@code IErpInvStockBalanceBiz.findList}
 * （ICrudBiz 管道，天然经 R1.29 组织隔离 transformer 过滤，config-gated 默认关）。
 * 余额种子直接以 DAO 写入 {@code erp_inv_stock_balance}（materialId+warehouseId+availableQuantity），
 * 不依赖 generateMove 业务链路（预校验只读查询，与出库强制校验的余额口径一致）。
 *
 * <p>config 显式赋值保证测试顺序无关（AppConfig 静态提供器跨方法持久，每方法开头显式设级）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalOrderAvailabilityCheck extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long CUSTOMER_ID = 2301L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long MATERIAL_ID = 4301L;
    static final Long MATERIAL_ID_2 = 4302L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testDefaultOffSkipsCheck() {
        setAvailabilityCheckLevel(ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_OFF);
        ErpSalOrder order = newOrder("SO-AVAIL-OFF-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, MATERIAL_ID, WAREHOUSE_ID, "10");
            seedBalance(MATERIAL_ID, WAREHOUSE_ID, "5");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> resp = approve(order.getId());
        assertEquals(0, resp.getStatus(), "默认 OFF → 库存不足(5<10) 不校验，approve 通过（既有行为回归）");
        output("response.json5", resp);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(order.getId()).getApproveStatus(),
                "默认 OFF → APPROVED");
    }

    @Test
    public void testWarnInsufficientAllows() {
        setAvailabilityCheckLevel(ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_WARN);
        ErpSalOrder order = newOrder("SO-AVAIL-WARN-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, MATERIAL_ID, WAREHOUSE_ID, "10");
            seedBalance(MATERIAL_ID, WAREHOUSE_ID, "5");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> resp = approve(order.getId());
        assertEquals(0, resp.getStatus(), "WARN 级别库存不足(5<10) → 记告警放行");
        output("response.json5", resp);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(order.getId()).getApproveStatus(),
                "WARN 不足放行 → APPROVED");
    }

    @Test
    public void testHardInsufficientRejects() {
        setAvailabilityCheckLevel(ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_HARD);
        ErpSalOrder order = newOrder("SO-AVAIL-HARD-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, MATERIAL_ID, WAREHOUSE_ID, "10");
            seedBalance(MATERIAL_ID, WAREHOUSE_ID, "5");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> bad = approve(order.getId());
        assertEquals(ErpSalErrors.ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT.getErrorCode(), bad.getCode(),
                "HARD 级别库存不足(5<10) → ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(order.getId()).getApproveStatus(),
                "HARD 拒绝 → approveStatus 保持 SUBMITTED");
        output("response.json5", bad);
    }

    @Test
    public void testHardSufficientAllows() {
        setAvailabilityCheckLevel(ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_HARD);
        ErpSalOrder order = newOrder("SO-AVAIL-OK-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            saveOrderWithLine(order, MATERIAL_ID, WAREHOUSE_ID, "10");
            seedBalance(MATERIAL_ID, WAREHOUSE_ID, "20");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> resp = approve(order.getId());
        assertEquals(0, resp.getStatus(), "HARD 级别库存充足(20>=10) → approve 通过");
        output("response.json5", resp);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(order.getId()).getApproveStatus(),
                "库存充足 → APPROVED");
    }

    @Test
    public void testLineWarehouseFallbackToOrderHead() {
        setAvailabilityCheckLevel(ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_HARD);
        ErpSalOrder order = newOrder("SO-AVAIL-FALLBACK-001", "100");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            // 行级 warehouseId = null → 回退订单头 warehouseId（命中余额）
            saveOrderWithLine(order, MATERIAL_ID, null, "10");
            seedBalance(MATERIAL_ID, WAREHOUSE_ID, "20");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> resp = approve(order.getId());
        assertEquals(0, resp.getStatus(), "行级 warehouseId null 回退订单头仓库 → 命中余额 20>=10 放行");
        output("response.json5", resp);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, reload(order.getId()).getApproveStatus(),
                "回退命中余额 → APPROVED");
    }

    @Test
    public void testHardMultiLinePartialInsufficientRejects() {
        setAvailabilityCheckLevel(ErpSalConstants.ORDER_AVAILABILITY_CHECK_LEVEL_HARD);
        ErpSalOrder order = newOrder("SO-AVAIL-MULTI-001", "200");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, null);
            // 行 1 充足（余额 20 >= 10），行 2 不足（余额 2 < 5）→ HARD 拒绝整单
            saveOrderWithLine(order, MATERIAL_ID, WAREHOUSE_ID, "10");
            saveSecondLine(order, MATERIAL_ID_2, WAREHOUSE_ID, "5");
            seedBalance(MATERIAL_ID, WAREHOUSE_ID, "20");
            seedBalance(MATERIAL_ID_2, WAREHOUSE_ID, "2");
        });

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> bad = approve(order.getId());
        assertEquals(ErpSalErrors.ERR_SAL_ORDER_AVAILABLE_INSUFFICIENT.getErrorCode(), bad.getCode(),
                "多行部分不足（行2 可用 2 < 需求 5）→ HARD 拒绝整单");
        assertEquals(ErpSalConstants.APPROVE_STATUS_SUBMITTED, reload(order.getId()).getApproveStatus(),
                "多行不足拒绝 → approveStatus 保持 SUBMITTED");
        output("response.json5", bad);
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> approve(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__approve",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- helpers ----------

    private ErpSalOrder reload(Long orderId) {
        return daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderId);
    }

    private ErpSalOrder newOrder(String code, String totalAmountWithTax) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(new BigDecimal("1"));
        order.setTotalAmountWithTax(new BigDecimal(totalAmountWithTax));
        order.setTotalAmount(new BigDecimal(totalAmountWithTax));
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        return order;
    }

    private void saveOrderWithLine(ErpSalOrder order, Long materialId, Long warehouseId, String quantity) {
        daoProvider.daoFor(ErpSalOrder.class).saveEntity(order);
        IEntityDao<ErpSalOrderLine> lineDao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(materialId);
        line.setWarehouseId(warehouseId);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal("10"));
        line.setAmount(new BigDecimal("100"));
        lineDao.saveEntity(line);
    }

    private void saveSecondLine(ErpSalOrder order, Long materialId, Long warehouseId, String quantity) {
        IEntityDao<ErpSalOrderLine> lineDao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(2);
        line.setMaterialId(materialId);
        line.setWarehouseId(warehouseId);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal("10"));
        line.setAmount(new BigDecimal("100"));
        lineDao.saveEntity(line);
    }

    /**
     * 直接 DAO 种子库存余额行（orgId+materialId+warehouseId+availableQuantity），
     * 供订单级预校验只读查询命中（无需 generateMove 业务链路）。
     */
    private void seedBalance(Long materialId, Long warehouseId, String qty) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        ErpInvStockBalance balance = dao.newEntity();
        balance.setOrgId(ORG_ID);
        balance.setMaterialId(materialId);
        balance.setWarehouseId(warehouseId);
        balance.setTotalQuantity(new BigDecimal(qty));
        balance.setAvailableQuantity(new BigDecimal(qty));
        dao.saveEntity(balance);
    }

    private void seedActiveCustomer(Long id, BigDecimal creditLimit) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        partner.setCreditLimit(creditLimit);
        dao.saveEntity(partner);
    }

    private void setAvailabilityCheckLevel(String level) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_ORDER_AVAILABILITY_CHECK_LEVEL, level);
    }
}
