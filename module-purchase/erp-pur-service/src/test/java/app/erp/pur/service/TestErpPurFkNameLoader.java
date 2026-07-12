package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
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
 * <p>覆盖 ErpPurOrder（supplierName/warehouseName/currencyName/orgName）
 * 与 ErpPurInvoice（supplierName/currencyName/orgName）。
 * 经 {@link IGraphQLEngine} findList + FieldSelectionBean 触发 BizLoader 字段解析，
 * 验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 8101L;
    static final Long SUPPLIER_ID = 8201L;
    static final Long WAREHOUSE_ID = 8301L;
    static final Long CURRENCY_ID = 8401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testPurOrderFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "采购测试组织");
            seedSupplier(SUPPLIER_ID, "供应商Gamma");
            seedWarehouse(WAREHOUSE_ID, "原料仓");
            seedCurrency(CURRENCY_ID, "人民币");
            seedOrder(7001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpPurOrder__findList",
                "id", "supplierName", "warehouseName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条订单");
        Map<String, Object> first = rows.get(0);
        assertEquals("供应商Gamma", first.get("supplierName"));
        assertEquals("原料仓", first.get("warehouseName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("采购测试组织", first.get("orgName"));
    }

    @Test
    public void testPurInvoiceFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "采购测试组织");
            seedSupplier(SUPPLIER_ID, "供应商Delta");
            seedCurrency(CURRENCY_ID, "人民币");
            seedInvoice(7501L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpPurInvoice__findList",
                "id", "supplierName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条发票");
        Map<String, Object> first = rows.get(0);
        assertEquals("供应商Delta", first.get("supplierName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("采购测试组织", first.get("orgName"));
    }

    @Test
    public void testPurReceiveFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "采购测试组织");
            seedSupplier(SUPPLIER_ID, "供应商Epsilon");
            seedWarehouse(WAREHOUSE_ID, "原料仓");
            seedCurrency(CURRENCY_ID, "人民币");
            seedReceive(7601L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpPurReceive__findList",
                "id", "supplierName", "warehouseName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条入库单");
        Map<String, Object> first = rows.get(0);
        assertEquals("供应商Epsilon", first.get("supplierName"));
        assertEquals("原料仓", first.get("warehouseName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("采购测试组织", first.get("orgName"));
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

    private void seedSupplier(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("SUP-" + id);
        p.setName(name);
        p.setPartnerType("VENDOR");
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
        IEntityDao<ErpPurOrder> dao = daoProvider.daoFor(ErpPurOrder.class);
        ErpPurOrder o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("PO-FK-" + id);
        o.setOrgId(ORG_ID);
        o.setSupplierId(SUPPLIER_ID);
        o.setWarehouseId(WAREHOUSE_ID);
        o.setBusinessDate(LocalDate.of(2026, 7, 1));
        o.setCurrencyId(CURRENCY_ID);
        o.setExchangeRate(BigDecimal.ONE);
        o.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        o.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(o);
    }

    private void seedInvoice(long id) {
        IEntityDao<ErpPurInvoice> dao = daoProvider.daoFor(ErpPurInvoice.class);
        ErpPurInvoice inv = dao.newEntity();
        inv.orm_propValue(1, id);
        inv.setCode("PI-FK-" + id);
        inv.setOrgId(ORG_ID);
        inv.setSupplierId(SUPPLIER_ID);
        inv.setInvoiceNo("INV-FK-" + id);
        inv.setBusinessDate(LocalDate.of(2026, 7, 1));
        inv.setCurrencyId(CURRENCY_ID);
        inv.setExchangeRate(BigDecimal.ONE);
        inv.setAmountSource(BigDecimal.ZERO);
        inv.setAmountFunctional(BigDecimal.ZERO);
        inv.setTotalAmount(BigDecimal.ZERO);
        inv.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        inv.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(inv);
    }

    private void seedReceive(long id) {
        IEntityDao<ErpPurReceive> dao = daoProvider.daoFor(ErpPurReceive.class);
        ErpPurReceive recv = dao.newEntity();
        recv.orm_propValue(1, id);
        recv.setCode("RC-FK-" + id);
        recv.setOrgId(ORG_ID);
        recv.setSupplierId(SUPPLIER_ID);
        recv.setWarehouseId(WAREHOUSE_ID);
        recv.setBusinessDate(LocalDate.of(2026, 7, 1));
        recv.setCurrencyId(CURRENCY_ID);
        recv.setExchangeRate(BigDecimal.ONE);
        recv.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
        recv.setApproveStatus(ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(recv);
    }
}
