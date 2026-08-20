package app.erp.pur.service;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtVolumeDiscount;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.79（P1-RC-078，UC-CT-08 A）合同量折扣采购消费接线测试：
 * 订单行引用合同行时按实际数量匹配 {@code ErpCtVolumeDiscount} 区间带计算折后价
 * （{@code docs/design/contract/volume-discount.md §折扣应用逻辑}）。
 *
 * <p>覆盖：命中区间折后价/行金额/头合计 + remark 标记、覆盖价优先、无命中回退原价、
 * 数量跨档 approve 时点重算（D2 裁决选项 a）、config 关闭零应用（D3）、行级 GraphQL 保存即应用（冒烟）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurOrderCtDiscount extends JunitAutoTestCase {

    static final Long SUPPLIER_ID = 2101L;
    static final Long MATERIAL_ID = 4101L;
    static final Long UOM_ID = 5101L;
    static final Long CURRENCY_ID = 6101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    public void clearConfig() {
        System.clearProperty(ErpPurConstants.CONFIG_CT_DISCOUNT_ENABLED);
    }

    @Test
    public void testBandHitAppliesDiscountedPriceOnApprove() {
        ormTemplate.runInSession(session -> {
            seedActiveSupplier(SUPPLIER_ID);
            Long lineId = seedContractLine("CT-PUR-HIT", "100", null, MATERIAL_ID);
            seedBand(lineId, "101", "500", "5", null);
            seedBand(lineId, "501", null, "12", null);

            ErpPurOrder order = newOrder("PO-CT-HIT-001");
            newOrderLine(order.getId(), lineId, 1, "300", "100", MATERIAL_ID);
            return null;
        });

        Long orderId = orderIdByCode("PO-CT-HIT-001");
        assertEquals(0, submit(orderId).getStatus(), "提交应成功");
        assertEquals(0, approve(orderId).getStatus(), "审核应成功");

        ErpPurOrderLine line = firstLine(orderId);
        // 合同行标准单价 100 × (1 - 5%) = 95；行金额 = 300 × 95 = 28500（owner doc §折扣应用逻辑示例）
        assertEquals(0, new BigDecimal("95").compareTo(line.getUnitPrice()), "命中 101~500 区间 → 折后单价 95");
        assertEquals(0, new BigDecimal("28500").compareTo(line.getAmount()), "行金额 = 300 × 95 = 28500");
        assertNotNull(line.getRemark(), "折扣来源 remark 标记非空");
        assertTrue(line.getRemark().contains(ErpPurConstants.CT_DISCOUNT_REMARK_TAG),
                "remark 含 [CT_VOLUME_DISCOUNT] 标记: " + line.getRemark());
        ErpPurOrder approved = daoProvider.daoFor(ErpPurOrder.class).getEntityById(orderId);
        assertEquals(0, new BigDecimal("28500").compareTo(approved.getTotalAmount()),
                "头合计随行金额重算 = 28500");
    }

    @Test
    public void testOverridePriceTakesPrecedence() {
        ormTemplate.runInSession(session -> {
            seedActiveSupplier(SUPPLIER_ID);
            Long lineId = seedContractLine("CT-PUR-OVR", "100", null, MATERIAL_ID);
            seedBand(lineId, "501", null, "12", "88");

            ErpPurOrder order = newOrder("PO-CT-OVR-001");
            newOrderLine(order.getId(), lineId, 1, "600", "100", MATERIAL_ID);
            return null;
        });

        Long orderId = orderIdByCode("PO-CT-OVR-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpPurOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("88").compareTo(line.getUnitPrice()),
                "区间带覆盖价优先于折扣率 → 折后单价 88");
        assertEquals(0, new BigDecimal("52800").compareTo(line.getAmount()), "行金额 = 600 × 88 = 52800");
    }

    @Test
    public void testNoBandFallsBackToOriginalPrice() {
        ormTemplate.runInSession(session -> {
            seedActiveSupplier(SUPPLIER_ID);
            Long lineId = seedContractLine("CT-PUR-MISS", "100", null, MATERIAL_ID);
            seedBand(lineId, "101", "500", "5", null);

            ErpPurOrder order = newOrder("PO-CT-MISS-001");
            // qty=50 未命中任何区间（fromQty=101 起）
            newOrderLine(order.getId(), lineId, 1, "50", "90", MATERIAL_ID);
            return null;
        });

        Long orderId = orderIdByCode("PO-CT-MISS-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpPurOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("90").compareTo(line.getUnitPrice()),
                "无命中回退原价——行保持录入价 90");
        assertEquals(0, new BigDecimal("4500").compareTo(line.getAmount()), "行金额保持 50 × 90 = 4500");
    }

    @Test
    public void testQtyCrossBandRecalculatedOnApprove() {
        ormTemplate.runInSession(session -> {
            seedActiveSupplier(SUPPLIER_ID);
            Long lineId = seedContractLine("CT-PUR-CROSS", "100", null, MATERIAL_ID);
            seedBand(lineId, "101", "500", "5", null);

            ErpPurOrder order = newOrder("PO-CT-CROSS-001");
            ErpPurOrderLine line = newOrderLine(order.getId(), lineId, 1, "90", "100", MATERIAL_ID);
            // 提交前数量变更到 300（跨档）——SAVING 态实体直接改属性随 flush 落库；approve 时点以当前数量为准（D2）
            line.setQuantity(new BigDecimal("300"));
            return null;
        });

        Long orderId = orderIdByCode("PO-CT-CROSS-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpPurOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("95").compareTo(line.getUnitPrice()),
                "数量跨档（90→300）approve 重算命中 5% 档 → 折后单价 95");
        assertEquals(0, new BigDecimal("28500").compareTo(line.getAmount()), "重算行金额 = 28500");
    }

    @Test
    public void testConfigDisabledZeroApplication() {
        System.setProperty(ErpPurConstants.CONFIG_CT_DISCOUNT_ENABLED, "false");
        ormTemplate.runInSession(session -> {
            seedActiveSupplier(SUPPLIER_ID);
            Long lineId = seedContractLine("CT-PUR-OFF", "100", null, MATERIAL_ID);
            seedBand(lineId, "101", "500", "5", null);

            ErpPurOrder order = newOrder("PO-CT-OFF-001");
            newOrderLine(order.getId(), lineId, 1, "300", "100", MATERIAL_ID);
            return null;
        });

        Long orderId = orderIdByCode("PO-CT-OFF-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpPurOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("100").compareTo(line.getUnitPrice()),
                "config 关闭零应用——引用字段仅存储，行保持原价");
        assertEquals(0, new BigDecimal("30000").compareTo(line.getAmount()), "行金额保持 300 × 100");
    }

    @Test
    public void testLineSaveViaGraphQLAppliesDiscountImmediately() {
        Object[] refs = ormTemplate.runInSession(session -> {
            seedActiveSupplier(SUPPLIER_ID);
            seedUoM(UOM_ID);
            Long materialId = seedMaterial(MATERIAL_ID);
            Long lineId = seedContractLine("CT-PUR-GQL", "100", null, materialId);
            seedBand(lineId, "101", "500", "5", null);

            ErpPurOrder order = newOrder("PO-CT-GQL-001");
            ErpPurOrderLine plain = newOrderLine(order.getId(), null, 1, "10", "100", materialId);
            return new Object[]{plain.getId(), lineId, materialId};
        });
        Long orderLineId = (Long) refs[0];
        Long contractLineId = (Long) refs[1];
        Long materialId = (Long) refs[2];

        // 行级 GraphQL 保存（引用合同行）——保存时解析折后价写行金额（fill-when-absent）
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("id", String.valueOf(orderLineId));
        data.put("lineNo", 1);
        data.put("materialId", materialId);
        data.put("uoMId", UOM_ID);
        data.put("quantity", new BigDecimal("300"));
        data.put("unitPrice", new BigDecimal("100"));
        data.put("amount", new BigDecimal("30000"));
        data.put("ctContractLineId", contractLineId);
        ApiResponse<?> resp = executeRpc(mutation, "ErpPurOrderLine__update",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "行级 update 应成功: " + resp);

        ErpPurOrderLine line = firstLine(orderIdByCode("PO-CT-GQL-001"));
        assertEquals(0, new BigDecimal("95").compareTo(line.getUnitPrice()),
                "行级 GraphQL 保存即应用折后价 95（无需 approve）");
        assertEquals(0, new BigDecimal("28500").compareTo(line.getAmount()), "行金额 = 28500");
        assertTrue(line.getRemark().contains(ErpPurConstants.CT_DISCOUNT_REMARK_TAG), "remark 标记存在");
    }

    // ---------- helpers ----------

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

    private ErpPurOrder newOrder(String code) {
        ErpPurOrder order = new ErpPurOrder();
        order.setCode(code);
        order.setOrgId(1101L);
        order.setSupplierId(SUPPLIER_ID);
        order.setWarehouseId(3101L);
        order.setBusinessDate(LocalDate.of(2026, 8, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(BigDecimal.ONE);
        order.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setReceiveStatus(ErpPurConstants.RECEIVE_STATUS_UNRECEIVED);
        order.setPosted(false);
        daoProvider.daoFor(ErpPurOrder.class).saveEntity(order);
        return order;
    }

    private ErpPurOrderLine newOrderLine(Long orderId, Long ctContractLineId, int lineNo, String qty, String price, Long materialId) {
        IEntityDao<ErpPurOrderLine> dao = daoProvider.daoFor(ErpPurOrderLine.class);
        ErpPurOrderLine line = new ErpPurOrderLine();
        line.setOrderId(orderId);
        line.setLineNo(lineNo);
        line.setMaterialId(materialId);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(qty));
        line.setUnitPrice(new BigDecimal(price));
        line.setAmount(new BigDecimal(qty).multiply(new BigDecimal(price)));
        line.setCtContractLineId(ctContractLineId);
        dao.saveEntity(line);
        return line;
    }

    private Long seedContractLine(String contractCode, String unitPrice, String quantity, Long materialId) {
        IEntityDao<ErpCtContract> contractDao = daoProvider.daoFor(ErpCtContract.class);
        ErpCtContract contract = new ErpCtContract();
        contract.setCode(contractCode);
        contract.setContractName("量折扣测试合同-" + contractCode);
        contract.setContractType("PURCHASE");
        contract.setContractDirection("INBOUND");
        contract.setPartnerId(SUPPLIER_ID);
        contract.setCurrencyId(CURRENCY_ID);
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2027, 12, 31));
        contract.setBusinessDate(LocalDate.of(2026, 1, 1));
        contract.setStatus("ACTIVE");
        contractDao.saveEntity(contract);

        ErpCtContractLine contractLine = new ErpCtContractLine();
        contractLine.setContractId(contract.getId());
        contractLine.setLineNo(1);
        contractLine.setMaterialId(materialId);
        contractLine.setQuantity(quantity == null ? new BigDecimal("100000") : new BigDecimal(quantity));
        contractLine.setUnitPrice(new BigDecimal(unitPrice));
        contractLine.setAmount(new BigDecimal(unitPrice));
        daoProvider.daoFor(ErpCtContractLine.class).saveEntity(contractLine);
        return contractLine.getId();
    }

    private Long seedMaterial(Long id) {
        ErpMdMaterial m = new ErpMdMaterial();
        m.setId(id);
        m.setCode("MAT-" + id);
        m.setName("量折扣测试物料");
        m.setMaterialType("GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdMaterial.class).saveEntity(m);
        return id;
    }

    private void seedUoM(Long id) {
        ErpMdUoM u = new ErpMdUoM();
        u.setId(id);
        u.setCode("PCS-CT");
        u.setName("个");
        daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
    }

    private void seedBand(Long contractLineId, String from, String to, String percent, String overridePrice) {
        ErpCtVolumeDiscount band = new ErpCtVolumeDiscount();
        band.setContractLineId(contractLineId);
        band.setFromQty(new BigDecimal(from));
        if (to != null) {
            band.setToQty(new BigDecimal(to));
        }
        band.setDiscountPercent(new BigDecimal(percent));
        if (overridePrice != null) {
            band.setUnitPrice(new BigDecimal(overridePrice));
        }
        daoProvider.daoFor(ErpCtVolumeDiscount.class).saveEntity(band);
    }

    private void seedActiveSupplier(Long id) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("SUP-" + id);
        partner.setName("供应商" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
        dao.saveEntity(partner);
    }

    private Long orderIdByCode(String code) {
        return ormTemplate.runInSession(session -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            return daoProvider.daoFor(ErpPurOrder.class).findAllByQuery(q).get(0).getId();
        });
    }

    private ErpPurOrderLine firstLine(Long orderId) {
        return ormTemplate.runInSession(session -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("orderId", orderId));
            return daoProvider.daoFor(ErpPurOrderLine.class).findAllByQuery(q).get(0);
        });
    }
}
