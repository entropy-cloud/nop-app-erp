package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmCampaign;
import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.dao.entity.ErpCrmForecastPeriod;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.dao.entity.ErpCrmTeam;
import app.erp.crm.dao.entity.ErpCrmTerritory;
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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name/*Code + @BizLoader 批量加载）。
 *
 * <p>覆盖 CRM 域核心实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long PARTNER_ID = 9201L;
    static final Long CURRENCY_ID = 9401L;
    static final Long STAGE_ID = 9501L;
    static final Long CAMPAIGN_ID = 9601L;
    static final Long TEAM_ID = 9701L;
    static final Long TERRITORY_ID = 9801L;
    static final Long PERIOD_ID = 9901L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testLeadFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "CRM测试组织");
            seedPartner(PARTNER_ID, "客户Alpha");
            seedStage(STAGE_ID, "资格验证");
            seedCampaign(CAMPAIGN_ID, "春季营销");
            seedTeam(TEAM_ID, "华东团队");
            seedTerritory(TERRITORY_ID, "华东大区");
            seedLead(8001L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpCrmLead__findList",
                "id", "partnerName", "stageName", "campaignName", "teamName", "territoryName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条线索");
        Map<String, Object> first = rows.get(0);
        assertEquals("客户Alpha", first.get("partnerName"));
        assertEquals("资格验证", first.get("stageName"));
        assertEquals("春季营销", first.get("campaignName"));
        assertEquals("华东团队", first.get("teamName"));
        assertEquals("华东大区", first.get("territoryName"));
    }

    @Test
    public void testForecastFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "CRM测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedTeam(TEAM_ID, "华东团队");
            seedTerritory(TERRITORY_ID, "华东大区");
            seedForecastPeriod(PERIOD_ID);
            seedForecast(8101L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpCrmForecast__findList",
                "id", "periodCode", "territoryName", "teamName", "currencyName");
        assertNotNull(rows);
        assertEquals(false, rows.isEmpty(), "至少 1 条预测");
        Map<String, Object> first = rows.get(0);
        assertEquals("FP-9901", first.get("periodCode"));
        assertEquals("华东大区", first.get("territoryName"));
        assertEquals("华东团队", first.get("teamName"));
        assertEquals("人民币", first.get("currencyName"));
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
        p.setCode("CUS-" + id);
        p.setName(name);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(java.math.BigDecimal.ZERO);
        p.setPayableBalance(java.math.BigDecimal.ZERO);
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

    private void seedStage(long id, String stageName) {
        IEntityDao<ErpCrmStage> dao = daoProvider.daoFor(ErpCrmStage.class);
        ErpCrmStage s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode("STG-" + id);
        s.setStageName(stageName);
        s.orm_propValueByName("sequence", 1);
        dao.saveEntity(s);
    }

    private void seedCampaign(long id, String name) {
        IEntityDao<ErpCrmCampaign> dao = daoProvider.daoFor(ErpCrmCampaign.class);
        ErpCrmCampaign c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CMP-" + id);
        c.setName(name);
        c.setCampaignName(name);
        dao.saveEntity(c);
    }

    private void seedTeam(long id, String name) {
        IEntityDao<ErpCrmTeam> dao = daoProvider.daoFor(ErpCrmTeam.class);
        ErpCrmTeam t = dao.newEntity();
        t.orm_propValue(1, id);
        t.setCode("TM-" + id);
        t.setName(name);
        dao.saveEntity(t);
    }

    private void seedTerritory(long id, String name) {
        IEntityDao<ErpCrmTerritory> dao = daoProvider.daoFor(ErpCrmTerritory.class);
        ErpCrmTerritory t = dao.newEntity();
        t.orm_propValue(1, id);
        t.setCode("TER-" + id);
        t.setName(name);
        t.orm_propValueByName("territoryType", "REGION");
        dao.saveEntity(t);
    }

    private void seedForecastPeriod(long id) {
        IEntityDao<ErpCrmForecastPeriod> dao = daoProvider.daoFor(ErpCrmForecastPeriod.class);
        ErpCrmForecastPeriod p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("FP-" + id);
        p.orm_propValueByName("periodType", "MONTHLY");
        p.setPeriodStart(java.time.LocalDate.of(2026, 7, 1));
        p.setPeriodEnd(java.time.LocalDate.of(2026, 7, 31));
        p.orm_propValueByName("status", "OPEN");
        dao.saveEntity(p);
    }

    private void seedLead(long id) {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        ErpCrmLead lead = dao.newEntity();
        lead.orm_propValue(1, id);
        lead.setCode("LD-" + id);
        lead.orm_propValueByName("leadType", "LEAD");
        lead.setOrgId(ORG_ID);
        lead.setPartnerId(PARTNER_ID);
        lead.setStageId(STAGE_ID);
        lead.setCampaignId(CAMPAIGN_ID);
        lead.setTeamId(TEAM_ID);
        lead.setTerritoryId(TERRITORY_ID);
        lead.orm_propValueByName("docStatus", "DRAFT");
        dao.saveEntity(lead);
    }

    private void seedForecast(long id) {
        IEntityDao<ErpCrmForecast> dao = daoProvider.daoFor(ErpCrmForecast.class);
        ErpCrmForecast f = dao.newEntity();
        f.orm_propValue(1, id);
        f.setOrgId(ORG_ID);
        f.setPeriodId(PERIOD_ID);
        f.setTerritoryId(TERRITORY_ID);
        f.setTeamId(TEAM_ID);
        f.setCurrencyId(CURRENCY_ID);
        dao.saveEntity(f);
    }
}
