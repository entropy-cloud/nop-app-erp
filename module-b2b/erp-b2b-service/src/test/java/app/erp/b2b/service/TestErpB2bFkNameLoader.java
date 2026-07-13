package app.erp.b2b.service;

import app.erp.b2b.dao.entity.ErpB2bAsn;
import app.erp.b2b.dao.entity.ErpB2bEdiDoc;
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
 * <p>覆盖 b2b 域核心实体 ErpB2bAsn（源 EDI 单据/合作伙伴名称对齐）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long PARTNER_ID = 9201L;
    static final Long EDI_DOC_ID = 9301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testAsnFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "B2B测试组织");
            seedPartner(PARTNER_ID, "EDI伙伴");
            seedEdiDoc(EDI_DOC_ID, "EDI-856-001");
            seedAsn(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpB2bAsn__findList",
                "id", "sourceEdiDocName", "partnerName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条 ASN");
        Map<String, Object> first = rows.get(0);
        assertEquals("EDI-856-001", first.get("sourceEdiDocName"));
        assertEquals("EDI伙伴", first.get("partnerName"));
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

    private void seedEdiDoc(long id, String code) {
        IEntityDao<ErpB2bEdiDoc> dao = daoProvider.daoFor(ErpB2bEdiDoc.class);
        ErpB2bEdiDoc d = dao.newEntity();
        d.orm_propValue(1, id);
        d.setCode(code);
        d.orm_propValueByName("state", "RECEIVED");
        d.orm_propValueByName("blockingLevel", "NONE");
        d.orm_propValueByName("retryCount", 0);
        d.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        dao.saveEntity(d);
    }

    private void seedAsn(long id) {
        IEntityDao<ErpB2bAsn> dao = daoProvider.daoFor(ErpB2bAsn.class);
        ErpB2bAsn a = dao.newEntity();
        a.orm_propValue(1, id);
        a.setCode("ASN-" + id);
        a.orm_propValueByName("orgId", ORG_ID);
        a.orm_propValueByName("sourceEdiDocId", EDI_DOC_ID);
        a.orm_propValueByName("partnerId", PARTNER_ID);
        a.orm_propValueByName("status", "DRAFT");
        a.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        dao.saveEntity(a);
    }
}
