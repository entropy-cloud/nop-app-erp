package app.erp.sal.service;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtVolumeDiscount;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * RC-R1.79（P1-RC-078，UC-CT-08 A）合同量折扣销售消费接线测试（采购侧 {@code TestErpPurOrderCtDiscount} 镜像）：
 * 订单行引用合同行时按实际数量匹配 {@code ErpCtVolumeDiscount} 区间带计算折后价，折扣列承载可见性
 * （discountRate/discountAmount/pricingSource=CT_VOLUME_DISCOUNT）。
 *
 * <p>覆盖：命中区间折后价/行金额/头合计 + pricingSource 标记、覆盖价优先、无命中回退原价、
 * 数量跨档 approve 时点重算（D2 裁决选项 a）、config 关闭零应用（D3）、行级 GraphQL 保存即应用（冒烟）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalOrderCtDiscount extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long CUSTOMER_ID = 2301L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long MATERIAL_ID = 4301L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @AfterEach
    public void clearConfig() {
        System.clearProperty(ErpSalConstants.CONFIG_CT_DISCOUNT_ENABLED);
    }

    @Test
    public void testBandHitAppliesDiscountedPriceOnApprove() {
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID);
            seedUoM(UOM_ID);
            seedMaterial(MATERIAL_ID);
            Long lineId = seedContractLine("CT-SAL-HIT", "100");
            seedBand(lineId, "101", "500", "5", null);
            seedBand(lineId, "501", null, "12", null);

            ErpSalOrder order = newOrder("SO-CT-HIT-001");
            newOrderLine(order.getId(), lineId, "300", "100");
            return null;
        });

        Long orderId = orderIdByCode("SO-CT-HIT-001");
        assertEquals(0, submit(orderId).getStatus(), "提交应成功");
        assertEquals(0, approve(orderId).getStatus(), "审核应成功");

        ErpSalOrderLine line = firstLine(orderId);
        // 合同行标准单价 100 × (1 - 5%) = 95；行金额 = 300 × 95 = 28500（owner doc §折扣应用逻辑示例）
        assertEquals(0, new BigDecimal("95").compareTo(line.getUnitPrice()), "命中 101~500 区间 → 折后单价 95");
        assertEquals(0, new BigDecimal("28500").compareTo(line.getAmount()), "行金额 = 300 × 95 = 28500");
        assertEquals(0, new BigDecimal("5.0000").compareTo(line.getDiscountRate()), "行折扣率 = 5%");
        assertEquals(0, new BigDecimal("1500").compareTo(line.getDiscountAmount()), "行折扣金额 = (100-95) × 300 = 1500");
        assertEquals(ErpSalConstants.PRICING_SOURCE_CT_VOLUME_DISCOUNT, line.getPricingSource(),
                "取价来源标记 CT_VOLUME_DISCOUNT（显式合同行引用优先于促销/目录价）");
        ErpSalOrder approved = daoProvider.daoFor(ErpSalOrder.class).getEntityById(orderId);
        assertEquals(0, new BigDecimal("28500").compareTo(approved.getTotalAmount()),
                "头合计随行金额重算 = 28500");
    }

    @Test
    public void testOverridePriceTakesPrecedence() {
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID);
            seedUoM(UOM_ID);
            seedMaterial(MATERIAL_ID);
            Long lineId = seedContractLine("CT-SAL-OVR", "100");
            seedBand(lineId, "501", null, "12", "88");

            ErpSalOrder order = newOrder("SO-CT-OVR-001");
            newOrderLine(order.getId(), lineId, "600", "100");
            return null;
        });

        Long orderId = orderIdByCode("SO-CT-OVR-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpSalOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("88").compareTo(line.getUnitPrice()),
                "区间带覆盖价优先于折扣率 → 折后单价 88");
        assertEquals(0, new BigDecimal("52800").compareTo(line.getAmount()), "行金额 = 600 × 88 = 52800");
        assertEquals(0, new BigDecimal("7200").compareTo(line.getDiscountAmount()), "行折扣金额 = (100-88) × 600 = 7200");
    }

    @Test
    public void testNoBandFallsBackToOriginalPrice() {
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID);
            seedUoM(UOM_ID);
            seedMaterial(MATERIAL_ID);
            Long lineId = seedContractLine("CT-SAL-MISS", "100");
            seedBand(lineId, "101", "500", "5", null);

            ErpSalOrder order = newOrder("SO-CT-MISS-001");
            // qty=50 未命中任何区间（fromQty=101 起）
            newOrderLine(order.getId(), lineId, "50", "90");
            return null;
        });

        Long orderId = orderIdByCode("SO-CT-MISS-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpSalOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("90").compareTo(line.getUnitPrice()),
                "无命中回退原价——行保持录入价 90");
        assertEquals(0, new BigDecimal("4500").compareTo(line.getAmount()), "行金额保持 50 × 90 = 4500");
    }

    @Test
    public void testQtyCrossBandRecalculatedOnApprove() {
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID);
            seedUoM(UOM_ID);
            seedMaterial(MATERIAL_ID);
            Long lineId = seedContractLine("CT-SAL-CROSS", "100");
            seedBand(lineId, "101", "500", "5", null);

            ErpSalOrder order = newOrder("SO-CT-CROSS-001");
            ErpSalOrderLine line = newOrderLine(order.getId(), lineId, "90", "100");
            // 提交前数量变更到 300（跨档）——SAVING 态直接改属性随 flush 落库；approve 时点以当前数量为准（D2）
            line.setQuantity(new BigDecimal("300"));
            return null;
        });

        Long orderId = orderIdByCode("SO-CT-CROSS-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpSalOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("95").compareTo(line.getUnitPrice()),
                "数量跨档（90→300）approve 重算命中 5% 档 → 折后单价 95");
        assertEquals(0, new BigDecimal("28500").compareTo(line.getAmount()), "重算行金额 = 28500");
    }

    @Test
    public void testConfigDisabledZeroApplication() {
        System.setProperty(ErpSalConstants.CONFIG_CT_DISCOUNT_ENABLED, "false");
        ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID);
            seedUoM(UOM_ID);
            seedMaterial(MATERIAL_ID);
            Long lineId = seedContractLine("CT-SAL-OFF", "100");
            seedBand(lineId, "101", "500", "5", null);

            ErpSalOrder order = newOrder("SO-CT-OFF-001");
            newOrderLine(order.getId(), lineId, "300", "100");
            return null;
        });

        Long orderId = orderIdByCode("SO-CT-OFF-001");
        assertEquals(0, submit(orderId).getStatus());
        assertEquals(0, approve(orderId).getStatus());

        ErpSalOrderLine line = firstLine(orderId);
        assertEquals(0, new BigDecimal("100").compareTo(line.getUnitPrice()),
                "config 关闭零应用——引用字段仅存储，行保持原价");
        assertEquals(0, new BigDecimal("30000").compareTo(line.getAmount()), "行金额保持 300 × 100");
    }

    @Test
    public void testLineSaveViaGraphQLAppliesDiscountImmediately() {
        Object[] refs = ormTemplate.runInSession(session -> {
            seedActiveCustomer(CUSTOMER_ID);
            seedUoM(UOM_ID);
            seedMaterial(MATERIAL_ID);
            Long lineId = seedContractLine("CT-SAL-GQL", "100");
            seedBand(lineId, "101", "500", "5", null);

            ErpSalOrder order = newOrder("SO-CT-GQL-001");
            ErpSalOrderLine plain = newOrderLine(order.getId(), null, "10", "100");
            return new Object[]{plain.getId(), lineId};
        });
        Long orderLineId = (Long) refs[0];
        Long contractLineId = (Long) refs[1];

        // 行级 GraphQL 保存（引用合同行）——保存时解析折后价写行金额（fill-when-absent）
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("id", String.valueOf(orderLineId));
        data.put("lineNo", 1);
        data.put("materialId", MATERIAL_ID);
        data.put("uoMId", UOM_ID);
        data.put("quantity", new BigDecimal("300"));
        data.put("unitPrice", new BigDecimal("100"));
        data.put("amount", new BigDecimal("30000"));
        data.put("ctContractLineId", contractLineId);
        ApiResponse<?> resp = executeRpc(mutation, "ErpSalOrderLine__update",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "行级 update 应成功: " + resp);

        ErpSalOrderLine line = firstLine(orderIdByCode("SO-CT-GQL-001"));
        assertEquals(0, new BigDecimal("95").compareTo(line.getUnitPrice()),
                "行级 GraphQL 保存即应用折后价 95（无需 approve）");
        assertEquals(0, new BigDecimal("28500").compareTo(line.getAmount()), "行金额 = 28500");
        assertEquals(ErpSalConstants.PRICING_SOURCE_CT_VOLUME_DISCOUNT, line.getPricingSource(),
                "pricingSource 即时标记");
        assertNotNull(line.getDiscountAmount(), "折扣列即时填充");
    }

    // ---------- helpers ----------

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

    private ErpSalOrder newOrder(String code) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 8, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(BigDecimal.ONE);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTotalAmountWithTax(BigDecimal.ZERO);
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        daoProvider.daoFor(ErpSalOrder.class).saveEntity(order);
        return order;
    }

    private ErpSalOrderLine newOrderLine(Long orderId, Long ctContractLineId, String qty, String price) {
        IEntityDao<ErpSalOrderLine> dao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(orderId);
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(qty));
        line.setUnitPrice(new BigDecimal(price));
        line.setAmount(new BigDecimal(qty).multiply(new BigDecimal(price)));
        line.setCtContractLineId(ctContractLineId);
        dao.saveEntity(line);
        return line;
    }

    private Long seedContractLine(String contractCode, String unitPrice) {
        ErpCtContract contract = new ErpCtContract();
        contract.setCode(contractCode);
        contract.setContractName("量折扣测试合同-" + contractCode);
        contract.setContractType("SALES");
        contract.setContractDirection("OUTBOUND");
        contract.setPartnerId(CUSTOMER_ID);
        contract.setCurrencyId(CURRENCY_ID);
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2027, 12, 31));
        contract.setBusinessDate(LocalDate.of(2026, 1, 1));
        contract.setStatus("ACTIVE");
        daoProvider.daoFor(ErpCtContract.class).saveEntity(contract);

        ErpCtContractLine contractLine = new ErpCtContractLine();
        contractLine.setContractId(contract.getId());
        contractLine.setLineNo(1);
        contractLine.setMaterialId(MATERIAL_ID);
        contractLine.setQuantity(new BigDecimal("100000"));
        contractLine.setUnitPrice(new BigDecimal(unitPrice));
        contractLine.setAmount(new BigDecimal(unitPrice));
        daoProvider.daoFor(ErpCtContractLine.class).saveEntity(contractLine);
        return contractLine.getId();
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

    private void seedActiveCustomer(Long id) {
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(partner);
    }

    private void seedMaterial(Long id) {
        ErpMdMaterial m = new ErpMdMaterial();
        m.setId(id);
        m.setCode("MAT-" + id);
        m.setName("量折扣测试物料");
        m.setMaterialType("GOODS");
        m.setUoMId(UOM_ID);
        m.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdMaterial.class).saveEntity(m);
    }

    private void seedUoM(Long id) {
        ErpMdUoM u = new ErpMdUoM();
        u.setId(id);
        u.setCode("PCS-CT");
        u.setName("个");
        daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
    }

    private Long orderIdByCode(String code) {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", code));
            return daoProvider.daoFor(ErpSalOrder.class).findAllByQuery(q).get(0).getId();
        });
    }

    private ErpSalOrderLine firstLine(Long orderId) {
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("orderId", orderId));
            return daoProvider.daoFor(ErpSalOrderLine.class).findAllByQuery(q).get(0);
        });
    }
}
