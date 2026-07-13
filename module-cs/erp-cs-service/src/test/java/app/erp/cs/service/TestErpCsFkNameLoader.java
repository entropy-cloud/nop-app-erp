package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsCatalogCategory;
import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketType;
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
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name/*Code + @BizLoader 批量加载）。
 *
 * <p>覆盖 CS 域核心实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long PARTNER_ID = 9201L;
    static final Long CONTACT_ID = 9202L;
    static final Long TICKET_TYPE_ID = 9301L;
    static final Long SLA_POLICY_ID = 9401L;
    static final Long CATEGORY_ID = 9501L;
    static final Long CATALOG_ITEM_ID = 9601L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testTicketFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID);
            seedPartner(PARTNER_ID, "客户Beta");
            seedPartner(CONTACT_ID, "联系人Gamma");
            seedTicketType(TICKET_TYPE_ID, "故障");
            seedSlaPolicy(SLA_POLICY_ID, "标准SLA");
            seedCatalogItem(CATALOG_ITEM_ID, "目录项Delta");
            seedTicket(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpCsTicket__findList",
                "id", "ticketTypeName", "slaPolicyName", "customerName", "contactName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条工单");
        Map<String, Object> first = rows.get(0);
        assertEquals("故障", first.get("ticketTypeName"));
        assertEquals("标准SLA", first.get("slaPolicyName"));
        assertEquals("客户Beta", first.get("customerName"));
        assertEquals("联系人Gamma", first.get("contactName"));
    }

    @Test
    public void testServiceCatalogItemFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID);
            seedTicketType(TICKET_TYPE_ID, "请求");
            seedSlaPolicy(SLA_POLICY_ID, "高级SLA");
            seedCatalogCategory(CATEGORY_ID, "基础设施");
            seedCatalogItem(8101L, "目录项Epsilon");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpCsServiceCatalogItem__findList",
                "id", "categoryName", "ticketTypeName", "slaPolicyName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条目录项");
        Map<String, Object> first = rows.get(0);
        assertEquals("基础设施", first.get("categoryName"));
        assertEquals("请求", first.get("ticketTypeName"));
        assertEquals("高级SLA", first.get("slaPolicyName"));
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

    private void seedOrg(long id) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("ORG-" + id);
        o.setName("CS测试组织");
        o.setOrgType("COMPANY");
        o.setStatus("ACTIVE");
        dao.saveEntity(o);
    }

    private void seedPartner(long id, String name) {
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

    private void seedTicketType(long id, String name) {
        IEntityDao<ErpCsTicketType> dao = daoProvider.daoFor(ErpCsTicketType.class);
        ErpCsTicketType t = dao.newEntity();
        t.orm_propValue(1, id);
        t.setCode("TT-" + id);
        t.setName(name);
        dao.saveEntity(t);
    }

    private void seedSlaPolicy(long id, String name) {
        IEntityDao<ErpCsSlaPolicy> dao = daoProvider.daoFor(ErpCsSlaPolicy.class);
        ErpCsSlaPolicy p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("SLA-" + id);
        p.setName(name);
        dao.saveEntity(p);
    }

    private void seedCatalogCategory(long id, String name) {
        IEntityDao<ErpCsCatalogCategory> dao = daoProvider.daoFor(ErpCsCatalogCategory.class);
        ErpCsCatalogCategory c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CC-" + id);
        c.setName(name);
        c.setOrgId(ORG_ID);
        dao.saveEntity(c);
    }

    private void seedCatalogItem(long id, String name) {
        IEntityDao<ErpCsServiceCatalogItem> dao = daoProvider.daoFor(ErpCsServiceCatalogItem.class);
        ErpCsServiceCatalogItem item = dao.newEntity();
        item.orm_propValue(1, id);
        item.setCode("CI-" + id);
        item.setName(name);
        item.setOrgId(ORG_ID);
        item.setCategoryId(CATEGORY_ID);
        item.setTicketTypeId(TICKET_TYPE_ID);
        item.setSlaPolicyId(SLA_POLICY_ID);
        dao.saveEntity(item);
    }

    private void seedTicket(long id) {
        IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
        ErpCsTicket ticket = dao.newEntity();
        ticket.orm_propValue(1, id);
        ticket.setCode("TK-" + id);
        ticket.setSubject("工单主题" + id);
        ticket.setOrgId(ORG_ID);
        ticket.setCustomerId(PARTNER_ID);
        ticket.setContactId(CONTACT_ID);
        ticket.setTicketTypeId(TICKET_TYPE_ID);
        ticket.setSlaPolicyId(SLA_POLICY_ID);
        ticket.setCatalogItemId(CATALOG_ITEM_ID);
        ticket.orm_propValueByName("priority", "MEDIUM");
        ticket.orm_propValueByName("status", "NEW");
        ticket.orm_propValueByName("docStatus", "DRAFT");
        ticket.orm_propValueByName("approveStatus", "UNSUBMITTED");
        ticket.setBusinessDate(LocalDate.of(2026, 7, 1));
        dao.saveEntity(ticket);
    }
}
