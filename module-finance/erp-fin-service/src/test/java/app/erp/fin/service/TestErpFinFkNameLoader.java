package app.erp.fin.service;

import app.erp.fin.dao.entity.ErpFinReconciliation;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name + @BizLoader 批量加载）。
 *
 * <p>覆盖 ErpFinReconciliation（partnerName/currencyName/orgName）。
 * 经 {@link IGraphQLEngine} findList + FieldSelectionBean 触发 BizLoader 字段解析，
 * 验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 6101L;
    static final Long PARTNER_ID = 6201L;
    static final Long CURRENCY_ID = 6401L;
    static final Long ACCT_SCHEMA_ID = 6501L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testFinReconciliationFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "财务测试组织");
            seedPartner(PARTNER_ID, "往来单位Theta");
            seedCurrency(CURRENCY_ID, "人民币");
            seedAcctSchema(ACCT_SCHEMA_ID, ORG_ID, CURRENCY_ID);
            seedReconciliation(6701L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpFinReconciliation__findList",
                "id", "partnerName", "currencyName", "orgName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条核销单");
        Map<String, Object> first = rows.get(0);
        assertEquals("往来单位Theta", first.get("partnerName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("财务测试组织", first.get("orgName"));
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

    private void seedPartner(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("PTR-" + id);
        p.setName(name);
        p.setPartnerType("BOTH");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
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

    private void seedAcctSchema(long id, long orgId, long currencyId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode("AS-" + id);
        s.setName("账套" + id);
        s.setOrgId(orgId);
        s.setNature("ENTERPRISE");
        s.setFunctionalCurrencyId(currencyId);
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    private void seedReconciliation(long id) {
        IEntityDao<ErpFinReconciliation> dao = daoProvider.daoFor(ErpFinReconciliation.class);
        ErpFinReconciliation r = dao.newEntity();
        r.orm_propValue(1, id);
        r.setCode("REC-FK-" + id);
        r.setOrgId(ORG_ID);
        r.setAcctSchemaId(ACCT_SCHEMA_ID);
        r.setDirection("AP");
        r.setPartnerId(PARTNER_ID);
        r.setBusinessDate(LocalDate.of(2026, 7, 1));
        r.setCurrencyId(CURRENCY_ID);
        r.setExchangeRate(BigDecimal.ONE);
        r.setDocStatus(ErpFinConstants.RECON_STATUS_DRAFT);
        dao.saveEntity(r);
    }
}
