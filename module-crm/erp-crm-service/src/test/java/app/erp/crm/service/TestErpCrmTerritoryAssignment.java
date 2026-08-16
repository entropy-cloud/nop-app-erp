package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmTeam;
import app.erp.crm.dao.entity.ErpCrmTeamMember;
import app.erp.crm.dao.entity.ErpCrmTerritory;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;
import app.erp.crm.service.support.TerritoryAssignmentEngine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRM 区域分配挑人引擎测试（plan 2026-08-16-1634-1 RC-R1.57，P1-RC-036）。
 *
 * <p>覆盖（①-⑤ 引擎单测：纯函数式 new TerritoryAssignmentEngine() + 假 TeamMemberResolver；
 * ⑥-⑨ 集成：config 门控 + GraphQL assignLead 回写 + defaultPrepareSave 自动分配 + _cases 快照）：
 * ① ROUND_ROBIN 轮流（上次 owner=A → 下一位 B；B → 循环回 A）；② ROUND_ROBIN 无历史 → 第一位；
 * ③ LOAD_BALANCED 最少线索（A=3/B=1 → B）；④ LOAD_BALANCED 平手 → 成员 id 升序首个；
 * ⑤ 无成员 / resolver 不可用 / 无团队 → MANUAL 降级 + degraded；
 * ⑥ config {@code erp-crm.territory.assignment-method-enabled}=false → MANUAL 降级；
 * ⑦ assignLead 集成回写 lead.ownerId；⑧ defaultPrepareSave 自动分配路径；⑨ 快照录制。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmTerritoryAssignment extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    static final Long ORG_ID = 1301L;
    static final Long TERRITORY_ID = 11001L;
    static final Long TEAM_ID = 11002L;
    static final Long RULE_ID = 11003L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ①-⑤ 引擎单测（纯函数式） ----------

    @Test
    public void testRoundRobinCyclesToNextMember() {
        TerritoryAssignmentEngine engine = newEngine();
        // ① ROUND_ROBIN 轮流：成员 [userA, userB]，上次分配 owner=userA → 返回 userB
        ErpCrmLead lead = newLead();
        ErpCrmTerritoryAssignmentRule rule = newRule(ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN, TEAM_ID);
        TerritoryAssignmentEngine.TeamMemberResolver resolver =
                fakeResolver(List.of("userA", "userB"), "userA", null);
        TerritoryAssignmentEngine.AssignmentResult result =
                engine.assign(lead, Collections.singletonList(rule), null, resolver, null);
        assertNotNull(result, "命中规则");
        assertEquals("userB", result.getOwnerId(), "上次 owner=userA → 下一位 userB");
        assertEquals(ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN, result.getAssignmentMethod(),
                "ROUND_ROBIN 方法保持不降级");
        assertTrue(!result.isDegraded(), "非降级");

        // 循环：上次 owner=userB → 回到 userA
        TerritoryAssignmentEngine.TeamMemberResolver resolver2 =
                fakeResolver(List.of("userA", "userB"), "userB", null);
        TerritoryAssignmentEngine.AssignmentResult result2 =
                engine.assign(lead, Collections.singletonList(rule), null, resolver2, null);
        assertEquals("userA", result2.getOwnerId(), "上次 owner=userB → 循环回 userA");
    }

    @Test
    public void testRoundRobinNoHistoryPicksFirst() {
        // ② ROUND_ROBIN 无历史记录 → 第一位成员
        TerritoryAssignmentEngine engine = newEngine();
        ErpCrmLead lead = newLead();
        ErpCrmTerritoryAssignmentRule rule = newRule(ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN, TEAM_ID);
        TerritoryAssignmentEngine.TeamMemberResolver resolver =
                fakeResolver(List.of("userA", "userB"), null, null);
        TerritoryAssignmentEngine.AssignmentResult result =
                engine.assign(lead, Collections.singletonList(rule), null, resolver, null);
        assertNotNull(result, "命中规则");
        assertEquals("userA", result.getOwnerId(), "无历史 → 第一位 userA");
    }

    @Test
    public void testLoadBalancedPicksLeastLoaded() {
        // ③ LOAD_BALANCED 最少线索：userA=3 / userB=1 → userB
        TerritoryAssignmentEngine engine = newEngine();
        ErpCrmLead lead = newLead();
        ErpCrmTerritoryAssignmentRule rule = newRule(ErpCrmConstants.ASSIGNMENT_METHOD_LOAD_BALANCED, TEAM_ID);
        TerritoryAssignmentEngine.TeamMemberResolver resolver =
                fakeResolver(List.of("userA", "userB"), null, Map.of("userA", 3, "userB", 1));
        TerritoryAssignmentEngine.AssignmentResult result =
                engine.assign(lead, Collections.singletonList(rule), null, resolver, null);
        assertNotNull(result, "命中规则");
        assertEquals("userB", result.getOwnerId(), "线索最少者 userB");
        assertEquals(ErpCrmConstants.ASSIGNMENT_METHOD_LOAD_BALANCED, result.getAssignmentMethod(),
                "LOAD_BALANCED 方法保持不降级");
    }

    @Test
    public void testLoadBalancedTiePicksFirstById() {
        // ④ LOAD_BALANCED 平手（userA=1 / userB=1）→ 成员列表序（id 升序）首个 userA
        TerritoryAssignmentEngine engine = newEngine();
        ErpCrmLead lead = newLead();
        ErpCrmTerritoryAssignmentRule rule = newRule(ErpCrmConstants.ASSIGNMENT_METHOD_LOAD_BALANCED, TEAM_ID);
        TerritoryAssignmentEngine.TeamMemberResolver resolver =
                fakeResolver(List.of("userA", "userB"), null, Map.of("userA", 1, "userB", 1));
        TerritoryAssignmentEngine.AssignmentResult result =
                engine.assign(lead, Collections.singletonList(rule), null, resolver, null);
        assertNotNull(result, "命中规则");
        assertEquals("userA", result.getOwnerId(), "平手 → 首个 userA");
    }

    @Test
    public void testNoMembersOrNoResolverDegradesManual() {
        // ⑤ 无成员 / resolver 不可用 / 无团队 → MANUAL 降级 + degraded=true + ownerId 空
        TerritoryAssignmentEngine engine = newEngine();
        ErpCrmLead lead = newLead();
        ErpCrmTerritoryAssignmentRule rule = newRule(ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN, TEAM_ID);

        // 5a：resolver 不可用（null）→ 降级
        TerritoryAssignmentEngine.AssignmentResult noResolver =
                engine.assign(lead, Collections.singletonList(rule), null, null, null);
        assertManualDegraded(noResolver, "resolver null → MANUAL 降级");

        // 5b：成员列表为空 → 降级
        TerritoryAssignmentEngine.TeamMemberResolver emptyResolver =
                fakeResolver(Collections.emptyList(), null, Collections.emptyMap());
        TerritoryAssignmentEngine.AssignmentResult emptyMembers =
                engine.assign(lead, Collections.singletonList(rule), null, emptyResolver, null);
        assertManualDegraded(emptyMembers, "空成员 → MANUAL 降级");

        // 5c：规则无团队（groupId=null）→ 降级
        ErpCrmTerritoryAssignmentRule noTeamRule =
                newRule(ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN, null);
        TerritoryAssignmentEngine.TeamMemberResolver anyResolver =
                fakeResolver(List.of("userA"), null, Collections.emptyMap());
        TerritoryAssignmentEngine.AssignmentResult noTeam =
                engine.assign(lead, Collections.singletonList(noTeamRule), null, anyResolver, null);
        assertManualDegraded(noTeam, "无团队 → MANUAL 降级");
    }

    // ---------- ⑥-⑨ 集成（GraphQL + DB） ----------

    @Test
    public void testConfigDisabledDegradesToManual() {
        // ⑥ config erp-crm.territory.assignment-method-enabled=false → assignLead 维持 MANUAL 降级零回归
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCrmConstants.CONFIG_TERRITORY_ASSIGNMENT_METHOD_ENABLED, Boolean.FALSE);
        try {
            Long leadId = 12001L;
            ormTemplate.runInSession(() -> {
                seedTerritory(TERRITORY_ID, "T-ASSIGN-CFG", ORG_ID);
                seedTeam(TEAM_ID, "TEAM-CFG");
                seedMember(TEAM_ID, "userA", 1L);
                seedMember(TEAM_ID, "userB", 2L);
                seedRule(RULE_ID, ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN, TEAM_ID);
                seedLead(leadId, "LEAD-CFG-1", "wechat");
            });
            ApiResponse<?> resp = assignLead(leadId);
            assertEquals(0, resp.getStatus(), "assignLead 应成功");
            ErpCrmLead lead = reloadLead(leadId);
            assertEquals(TERRITORY_ID, lead.getTerritoryId(), "territoryId 仍回写");
            assertEquals(TEAM_ID, lead.getTeamId(), "teamId 仍回写");
            assertNull(lead.getOwnerId(), "config 关闭 → 不挑人，ownerId 保持待分配");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCrmConstants.CONFIG_TERRITORY_ASSIGNMENT_METHOD_ENABLED, Boolean.TRUE);
        }
    }

    @Test
    public void testAssignLeadWritesOwnerId() {
        // ⑦ assignLead 集成：ROUND_ROBIN + 成员 [userA, userB] + 无历史 → lead.ownerId=userA 回写
        Long leadId = 12002L;
        ormTemplate.runInSession(() -> {
            seedTerritory(TERRITORY_ID, "T-ASSIGN-INT", ORG_ID);
            seedTeam(TEAM_ID, "TEAM-INT");
            seedMember(TEAM_ID, "userA", 1L);
            seedMember(TEAM_ID, "userB", 2L);
            seedRule(RULE_ID, ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN, TEAM_ID);
            seedLead(leadId, "LEAD-RR-1", "wechat");
        });
        ApiResponse<?> resp = assignLead(leadId);
        assertEquals(0, resp.getStatus(), "assignLead 应成功");
        ErpCrmLead lead = reloadLead(leadId);
        assertEquals(TERRITORY_ID, lead.getTerritoryId(), "territoryId 回写");
        assertEquals(TEAM_ID, lead.getTeamId(), "teamId 回写");
        assertEquals("userA", lead.getOwnerId(), "无历史 → 第一位成员 userA");
        output("assign_lead_response.json5", resp);
    }

    @Test
    public void testDefaultPrepareSaveAutoAssignsOwner() {
        // ⑧ defaultPrepareSave 自动分配路径：save 新建 Lead（未指派 owner）→ 规则匹配 + 挑人回写
        ormTemplate.runInSession(() -> {
            seedTerritory(TERRITORY_ID, "T-ASSIGN-AUTO", ORG_ID);
            seedTeam(TEAM_ID, "TEAM-AUTO");
            seedMember(TEAM_ID, "userA", 1L);
            seedMember(TEAM_ID, "userB", 2L);
            seedRule(RULE_ID, ErpCrmConstants.ASSIGNMENT_METHOD_LOAD_BALANCED, TEAM_ID);
        });
        ApiResponse<?> resp = saveLead("LEAD-AUTO-1", "wechat");
        assertEquals(0, resp.getStatus(), "save 应成功");
        ErpCrmLead lead = reloadByCode("LEAD-AUTO-1");
        assertNotNull(lead, "线索已创建");
        assertEquals(TERRITORY_ID, lead.getTerritoryId(), "auto-assign territoryId 回写");
        assertEquals(TEAM_ID, lead.getTeamId(), "auto-assign teamId 回写");
        assertEquals("userA", lead.getOwnerId(), "LOAD_BALANCED 平手（均 0 线索）→ 首个 userA");
        output("auto_assign_response.json5", resp);
    }

    // ---------- 引擎构造 helper ----------

    /** 恒真匹配器：纯单测（无 ORM 会话）下绕过 conditionValue 匹配，聚焦挑人语义。 */
    private static TerritoryAssignmentEngine newEngine() {
        return new TerritoryAssignmentEngine(new TerritoryAssignmentEngine.ConditionMatcher() {
            @Override
            public boolean matches(ErpCrmTerritoryAssignmentRule rule, ErpCrmLead lead) {
                return true;
            }
        });
    }

    private static TerritoryAssignmentEngine.TeamMemberResolver fakeResolver(
            List<String> members, String lastOwner, Map<String, Integer> counts) {
        return new TerritoryAssignmentEngine.TeamMemberResolver() {
            @Override
            public List<String> resolveTeamMemberUserIds(Long teamId, IServiceContext context) {
                return members;
            }

            @Override
            public String resolveLastAssignedOwner(Long teamId, IServiceContext context) {
                return lastOwner;
            }

            @Override
            public Map<String, Integer> countActiveLeadsByOwner(Long teamId, IServiceContext context) {
                return counts;
            }
        };
    }

    private static ErpCrmLead newLead() {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        lead.setUtmSource("wechat");
        return lead;
    }

    private static ErpCrmTerritoryAssignmentRule newRule(String method, Long groupId) {
        ErpCrmTerritoryAssignmentRule rule = new ErpCrmTerritoryAssignmentRule();
        rule.setPriority(1);
        rule.setTerritoryId(TERRITORY_ID);
        rule.setConditionType(ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD);
        rule.setConditionValue("{\"utmSource\":\"wechat\"}");
        rule.setAssignmentMethod(method);
        rule.setGroupId(groupId);
        rule.setIsDefault(Boolean.FALSE);
        rule.setIsActive(Boolean.TRUE);
        return rule;
    }

    private static void assertManualDegraded(TerritoryAssignmentEngine.AssignmentResult result, String msg) {
        assertNotNull(result, msg);
        assertEquals(ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL, result.getAssignmentMethod(),
                msg + " → 方法降级 MANUAL");
        assertTrue(result.isDegraded(), msg + " → degraded=true");
        assertNull(result.getOwnerId(), msg + " → ownerId 留空待分配");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> assignLead(Long leadId) {
        return graphQLEngine.executeRpc(graphQLEngine.newRpcContext(
                GraphQLOperationType.mutation, "ErpCrmLead__assignLead",
                ApiRequest.build(Map.of("leadId", leadId))));
    }

    private ApiResponse<?> saveLead(String code, String utmSource) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("leadType", ErpCrmConstants.LEAD_TYPE_LEAD);
        data.put("docStatus", ErpCrmConstants.DOC_STATUS_NEW);
        data.put("contactName", "联系人" + code);
        data.put("utmSource", utmSource);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                GraphQLOperationType.mutation, "ErpCrmLead__save",
                ApiRequest.build(Map.of("data", data)));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedTerritory(Long id, String code, Long orgId) {
        IEntityDao<ErpCrmTerritory> dao = daoProvider.daoFor(ErpCrmTerritory.class);
        ErpCrmTerritory t = new ErpCrmTerritory();
        t.setId(id);
        t.setCode(code);
        t.setName(code);
        t.setOrgId(orgId);
        t.setTerritoryType(ErpCrmConstants.TERRITORY_TYPE_REGION);
        t.setLevel(0);
        t.setFullPath("/" + code);
        t.setIsActive(Boolean.TRUE);
        t.setIsLeaf(Boolean.TRUE);
        t.setSortOrder(0);
        dao.saveEntity(t);
    }

    private void seedTeam(Long id, String code) {
        IEntityDao<ErpCrmTeam> dao = daoProvider.daoFor(ErpCrmTeam.class);
        ErpCrmTeam team = new ErpCrmTeam();
        team.setId(id);
        team.setCode(code);
        team.setName(code);
        team.setOrgId(ORG_ID);
        dao.saveEntity(team);
    }

    private void seedMember(Long teamId, String userId, long id) {
        IEntityDao<ErpCrmTeamMember> dao = daoProvider.daoFor(ErpCrmTeamMember.class);
        ErpCrmTeamMember member = new ErpCrmTeamMember();
        member.setId(id);
        member.setTeamId(teamId);
        member.setUserId(userId);
        dao.saveEntity(member);
    }

    private void seedRule(Long id, String method, Long groupId) {
        IEntityDao<ErpCrmTerritoryAssignmentRule> dao = daoProvider.daoFor(ErpCrmTerritoryAssignmentRule.class);
        ErpCrmTerritoryAssignmentRule rule = new ErpCrmTerritoryAssignmentRule();
        rule.setId(id);
        rule.setOrgId(ORG_ID);
        rule.setRuleName("RR-RULE-" + id);
        rule.setPriority(10);
        rule.setTerritoryId(TERRITORY_ID);
        rule.setConditionType(ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD);
        rule.setConditionValue("{\"utmSource\":\"wechat\"}");
        rule.setAssignmentMethod(method);
        rule.setGroupId(groupId);
        rule.setIsDefault(Boolean.FALSE);
        rule.setIsActive(Boolean.TRUE);
        dao.saveEntity(rule);
    }

    private void seedLead(Long id, String code, String utmSource) {
        IEntityDao<ErpCrmLead> dao = daoProvider.daoFor(ErpCrmLead.class);
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        lead.setContactName("联系人" + id);
        lead.setUtmSource(utmSource);
        dao.saveEntity(lead);
    }

    // ---------- reload helpers ----------

    private ErpCrmLead reloadLead(Long id) {
        return daoProvider.daoFor(ErpCrmLead.class).getEntityById(id);
    }

    private ErpCrmLead reloadByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        return daoProvider.daoFor(ErpCrmLead.class).findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
