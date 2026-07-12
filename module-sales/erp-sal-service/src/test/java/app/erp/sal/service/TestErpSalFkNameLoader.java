package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.FieldSelectionBean;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name + @BizLoader 批量加载）。
 *
 * <p>覆盖 ErpSalOrder（customerName/warehouseName/currencyName/orgName）
 * 与 ErpSalInvoice（customerName/currencyName/orgName）。
 * 经 {@link IGraphQLEngine} findList + FieldSelectionBean 触发 BizLoader 字段解析，
 * 验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long CUSTOMER_ID = 9201L;
    static final Long WAREHOUSE_ID = 9301L;
    static final Long CURRENCY_ID = 9401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSalOrderFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "销售测试组织");
            seedCustomer(CUSTOMER_ID, "客户Alpha");
            seedWarehouse(WAREHOUSE_ID, "成品仓");
            seedCurrency(CURRENCY_ID, "人民币");
            seedOrder(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpSalOrder__findList",
                "id", "customerName", "warehouseName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条订单");
        Map<String, Object> first = rows.get(0);
        assertEquals("客户Alpha", first.get("customerName"));
        assertEquals("成品仓", first.get("warehouseName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("销售测试组织", first.get("orgName"));
    }

    @Test
    public void testSalInvoiceFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "销售测试组织");
            seedCustomer(CUSTOMER_ID, "客户Beta");
            seedCurrency(CURRENCY_ID, "人民币");
            seedInvoice(8501L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpSalInvoice__findList",
                "id", "customerName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条发票");
        Map<String, Object> first = rows.get(0);
        assertEquals("客户Beta", first.get("customerName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("销售测试组织", first.get("orgName"));
    }

    @Test
    public void testSalDeliveryFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "销售测试组织");
            seedCustomer(CUSTOMER_ID, "客户Gamma");
            seedWarehouse(WAREHOUSE_ID, "成品仓");
            seedCurrency(CURRENCY_ID, "人民币");
            seedDelivery(8601L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpSalDelivery__findList",
                "id", "customerName", "warehouseName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条出库单");
        Map<String, Object> first = rows.get(0);
        assertEquals("客户Gamma", first.get("customerName"));
        assertEquals("成品仓", first.get("warehouseName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("销售测试组织", first.get("orgName"));
    }

    // ---------- query helper ----------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryWithSelection(String action, String... fields) {
        FieldSelectionBean selection = new FieldSelectionBean();
        for (String f : fields) {
            selection.addField(f);
        }
        ApiRequest<?> request = ApiRequest.build(Map.of());
        request.setSelection(selection);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                GraphQLOperationType.query, action, request);
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 查询成功");
        Object data = resp.getData();
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        return (List<Map<String, Object>>) ((Map<?, ?>) data).get("items");
    }

    // ---------- seed helpers ----------

    private void seedOrg(long id, String name) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("ORG-" + id);
        o.setName(name);
        o.setOrgType("COMPANY");
        o.setStatus("ACTIVE");
        dao.saveEntity(o);
    }

    private void seedCustomer(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("CUS-" + id);
        p.setName(name);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedWarehouse(long id, String name) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValue(1, id);
        w.setCode("WH-" + id);
        w.setName(name);
        w.setStatus("ACTIVE");
        dao.saveEntity(w);
    }

    private void seedCurrency(long id, String name) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CNY");
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedOrder(long id) {
        IEntityDao<ErpSalOrder> dao = daoProvider.daoFor(ErpSalOrder.class);
        ErpSalOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("SO-FK-" + id);
        o.setOrgId(ORG_ID);
        o.setCustomerId(CUSTOMER_ID);
        o.setWarehouseId(WAREHOUSE_ID);
        o.setBusinessDate(LocalDate.of(2026, 7, 1));
        o.setCurrencyId(CURRENCY_ID);
        o.setExchangeRate(BigDecimal.ONE);
        o.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        o.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(o);
    }

    private void seedInvoice(long id) {
        IEntityDao<ErpSalInvoice> dao = daoProvider.daoFor(ErpSalInvoice.class);
        ErpSalInvoice inv = dao.newEntity();
        inv.orm_propValue(1, id);
        inv.setCode("SI-FK-" + id);
        inv.setOrgId(ORG_ID);
        inv.setCustomerId(CUSTOMER_ID);
        inv.setInvoiceNo("INV-FK-" + id);
        inv.setBusinessDate(LocalDate.of(2026, 7, 1));
        inv.setCurrencyId(CURRENCY_ID);
        inv.setExchangeRate(BigDecimal.ONE);
        inv.setAmountSource(BigDecimal.ZERO);
        inv.setAmountFunctional(BigDecimal.ZERO);
        inv.setTotalAmount(BigDecimal.ZERO);
        inv.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        inv.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(inv);
    }

    private void seedDelivery(long id) {
        IEntityDao<ErpSalDelivery> dao = daoProvider.daoFor(ErpSalDelivery.class);
        ErpSalDelivery deliv = dao.newEntity();
        deliv.orm_propValue(1, id);
        deliv.setCode("DL-FK-" + id);
        deliv.setOrgId(ORG_ID);
        deliv.setCustomerId(CUSTOMER_ID);
        deliv.setWarehouseId(WAREHOUSE_ID);
        deliv.setBusinessDate(LocalDate.of(2026, 7, 1));
        deliv.setCurrencyId(CURRENCY_ID);
        deliv.setExchangeRate(BigDecimal.ONE);
        deliv.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        deliv.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(deliv);
    }
}
