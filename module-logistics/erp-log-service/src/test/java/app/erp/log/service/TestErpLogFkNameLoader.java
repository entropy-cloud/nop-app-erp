package app.erp.log.service;

import app.erp.log.dao.entity.ErpLogCarrier;
import app.erp.log.dao.entity.ErpLogCarrierConfig;
import app.erp.log.dao.entity.ErpLogShipment;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdOrganization;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name + @BizLoader 批量加载）。
 *
 * <p>覆盖 logistics 域核心实体 ErpLogShipment（承运人/承运配置/币种/发货人名称对齐）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpLogFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long CARRIER_ID = 9201L;
    static final Long CONFIG_ID = 9301L;
    static final Long CURRENCY_ID = 9401L;
    static final Long SHIPPER_ID = 9501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testShipmentFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "Log测试组织");
            seedCarrier(CARRIER_ID, "SF-EXPRESS");
            seedCarrierConfig(CONFIG_ID, CARRIER_ID, "CFG-PROD");
            seedCurrency(CURRENCY_ID, "人民币");
            seedShipper(SHIPPER_ID, "张三");
            seedShipment(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpLogShipment__findList",
                "id", "carrierName", "carrierConfigName", "freightCurrencyName", "shipperName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条发运单");
        Map<String, Object> first = rows.get(0);
        assertEquals("SF-EXPRESS", first.get("carrierName"));
        assertEquals("CFG-PROD", first.get("carrierConfigName"));
        assertEquals("人民币", first.get("freightCurrencyName"));
        assertEquals("张三", first.get("shipperName"));
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
        o.orm_propValueByName("orgType", "COMPANY");
        o.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(o);
    }

    private void seedCurrency(long id, String name) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CNY");
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedShipper(long id, String name) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee e = dao.newEntity();
        e.orm_propValue(1, id);
        e.setCode("EMP-" + id);
        e.setName(name);
        e.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(e);
    }

    private void seedCarrier(long id, String code) {
        IEntityDao<ErpLogCarrier> dao = daoProvider.daoFor(ErpLogCarrier.class);
        ErpLogCarrier c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode(code);
        c.orm_propValueByName("carrierName", code);
        c.orm_propValueByName("carrierType", "EXPRESS");
        c.orm_propValueByName("gatewayId", "GW-" + id);
        c.orm_propValueByName("isActive", Boolean.TRUE);
        dao.saveEntity(c);
    }

    private void seedCarrierConfig(long id, long carrierId, String configCode) {
        IEntityDao<ErpLogCarrierConfig> dao = daoProvider.daoFor(ErpLogCarrierConfig.class);
        ErpLogCarrierConfig c = dao.newEntity();
        c.orm_propValue(1, id);
        c.orm_propValueByName("carrierId", carrierId);
        c.orm_propValueByName("configCode", configCode);
        c.orm_propValueByName("isActive", Boolean.TRUE);
        dao.saveEntity(c);
    }

    private void seedShipment(long id) {
        IEntityDao<ErpLogShipment> dao = daoProvider.daoFor(ErpLogShipment.class);
        ErpLogShipment s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode("SHP-" + id);
        s.orm_propValueByName("orgId", ORG_ID);
        s.orm_propValueByName("carrierId", CARRIER_ID);
        s.orm_propValueByName("carrierConfigId", CONFIG_ID);
        s.orm_propValueByName("freightCurrencyId", CURRENCY_ID);
        s.orm_propValueByName("shipperId", SHIPPER_ID);
        s.orm_propValueByName("status", "DRAFT");
        s.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        dao.saveEntity(s);
    }
}
