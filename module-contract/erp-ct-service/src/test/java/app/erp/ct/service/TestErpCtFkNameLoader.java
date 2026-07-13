package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtTemplate;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
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
 * <p>覆盖 contract 域核心实体 ErpCtContract（客户/币种/合同模板名称对齐）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long PARTNER_ID = 9201L;
    static final Long CURRENCY_ID = 9301L;
    static final Long TEMPLATE_ID = 9401L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testContractFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "Ct测试组织");
            seedPartner(PARTNER_ID, "合同客户");
            seedCurrency(CURRENCY_ID, "人民币");
            seedTemplate(TEMPLATE_ID, "标准销售合同模板");
            seedContract(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpCtContract__findList",
                "id", "partnerName", "currencyName", "templateName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条合同");
        Map<String, Object> first = rows.get(0);
        assertEquals("合同客户", first.get("partnerName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("标准销售合同模板", first.get("templateName"));
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

    private void seedPartner(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("CUS-" + id);
        p.setName(name);
        p.orm_propValueByName("partnerType", "CUSTOMER");
        p.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(p);
    }

    private void seedCurrency(long id, String name) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CNY");
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedTemplate(long id, String name) {
        IEntityDao<ErpCtTemplate> dao = daoProvider.daoFor(ErpCtTemplate.class);
        ErpCtTemplate t = dao.newEntity();
        t.orm_propValue(1, id);
        t.setCode("TPL-" + id);
        t.setName(name);
        t.orm_propValueByName("contractType", "SALES");
        dao.saveEntity(t);
    }

    private void seedContract(long id) {
        IEntityDao<ErpCtContract> dao = daoProvider.daoFor(ErpCtContract.class);
        ErpCtContract c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CT-" + id);
        c.orm_propValueByName("contractName", "测试合同" + id);
        c.orm_propValueByName("contractType", "SALES");
        c.orm_propValueByName("contractDirection", "OUTBOUND");
        c.orm_propValueByName("orgId", ORG_ID);
        c.orm_propValueByName("partnerId", PARTNER_ID);
        c.orm_propValueByName("currencyId", CURRENCY_ID);
        c.orm_propValueByName("templateId", TEMPLATE_ID);
        c.orm_propValueByName("startDate", LocalDate.of(2026, 1, 1));
        c.orm_propValueByName("endDate", LocalDate.of(2026, 12, 31));
        c.orm_propValueByName("status", "DRAFT");
        c.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        dao.saveEntity(c);
    }
}
