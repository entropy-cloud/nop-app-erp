package app.erp.notify.service;

import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.auth.dao.entity.NopAuthRole;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.auth.dao.entity.NopAuthUserRole;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MA4 运行时确认测试（A4.2.182-A4.2.185，plan 2026-08-07-1533-1）：notify 接收人解析器运行时行为 + kill-switch 运行时行为。
 *
 * <p>覆盖 UC-SYS-03（ROLE/ORG/USER_LIST ${var} 插值运行时解析）+ UC-SYS-07（erp-notify.enabled 总开关运行时消费）。
 * 仅测试类目，不触生产代码。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSysNotificationRecipientResolverRuntime extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testRoleResolverMatchesRoleName() {
        // A4.2.182：ROLE resolver 运行时 roleName 匹配——种子 NopAuthRole(roleName) + NopAuthUser + NopAuthUserRole
        String roleName = "财务员";
        String userId = "rr-role-user-1";
        seedRole(roleName, userId);
        seedTemplate(7401L, "rr-role-event-1", "角色测试", "角色测试正文",
                ErpNotifyConstants.RESOLVER_ROLE, "{\"roles\":[\"" + roleName + "\"]}");

        assertEquals(0, notify("rr-role-event-1", Map.of()).getStatus());

        List<ErpSysNotification> list = notificationsOf(userId, "rr-role-event-1");
        assertEquals(1, list.size(), "ROLE resolver 应解析出种子角色用户: " + list.size());
        assertEquals(userId, list.get(0).getRecipientUserId());
        assertEquals(ErpNotifyConstants.STATUS_SENT, list.get(0).getStatus());
    }

    @Test
    public void testRoleResolverUnknownRoleNameConfigGatedEmpty() {
        // A4.2.182：不匹配角色名 → config-gated 空集（resolveRole:119-144 行为）
        String userId = "rr-role-user-2";
        seedRole("财务经理", userId);
        seedTemplate(7402L, "rr-role-event-2", "角色测试2", "角色测试正文2",
                ErpNotifyConstants.RESOLVER_ROLE, "{\"roles\":[\"不存在的角色\"]}");

        assertEquals(0, notify("rr-role-event-2", Map.of()).getStatus());

        assertTrue(notificationsOf(userId, "rr-role-event-2").isEmpty(),
                "不匹配角色名应 config-gated 返回空，无通知落库");
        assertTrue(notificationsOf("rr-role-user-2", "rr-role-event-2").isEmpty());
    }

    @Test
    public void testUserListVarInterpolation() {
        // A4.2.183：USER_LIST ${var} 动态插值——模板配置 ${submitterUserId} 从 context 插值（interpolateConfig:206-221）
        String dynamicUser = "rr-dynamic-user-1";
        seedTemplate(7403L, "rr-var-event-1", "变量测试", "变量测试正文",
                ErpNotifyConstants.RESOLVER_USER_LIST, "{\"userIds\":[\"${submitterUserId}\"]}");

        assertEquals(0, notify("rr-var-event-1", Map.of("submitterUserId", dynamicUser)).getStatus());

        List<ErpSysNotification> list = notificationsOf(dynamicUser, "rr-var-event-1");
        assertEquals(1, list.size(), "${submitterUserId} 应从 context 插值为动态用户: " + list.size());
        assertEquals(dynamicUser, list.get(0).getRecipientUserId());
        assertEquals(ErpNotifyConstants.STATUS_SENT, list.get(0).getStatus());
    }

    @Test
    public void testOrgResolverDeptIdExactMatch() {
        // A4.2.184：ORG resolver deptId 精确匹配——仅同 deptId 用户命中，无子部门递归（resolveOrg:158-173）
        seedAuthUser("rr-org-user-1", "D-100");
        seedAuthUser("rr-org-user-2", "D-200");
        seedTemplate(7405L, "rr-org-event-1", "组织测试", "组织测试正文",
                ErpNotifyConstants.RESOLVER_ORG, "{\"deptId\":\"D-100\"}");

        assertEquals(0, notify("rr-org-event-1", Map.of()).getStatus());

        List<ErpSysNotification> matched = notificationsOf("rr-org-user-1", "rr-org-event-1");
        assertEquals(1, matched.size(), "同 deptId 用户应精确命中: " + matched.size());
        List<ErpSysNotification> notMatched = notificationsOf("rr-org-user-2", "rr-org-event-1");
        assertTrue(notMatched.isEmpty(), "异 deptId 用户不应命中（无子部门递归）");
    }

    @Test
    public void testNotifyEnabledFalseStillDispatches() {
        // A4.2.185：erp-notify.enabled=false kill-switch 运行时行为——CONFIG_NOTIFY_ENABLED 零消费，通知仍落库（P2-RC-081 运行时证实）
        String userId = "rr-kill-user-1";
        AppConfig.getConfigProvider().assignConfigValue(ErpNotifyConfigs.CONFIG_NOTIFY_ENABLED, "false");
        try {
            seedTemplate(7406L, "rr-kill-event-1", "总开关测试", "总开关测试正文",
                    ErpNotifyConstants.RESOLVER_USER_LIST, "{\"userIds\":[\"" + userId + "\"]}");

            assertEquals(0, notify("rr-kill-event-1", Map.of()).getStatus());

            List<ErpSysNotification> list = notificationsOf(userId, "rr-kill-event-1");
            assertEquals(1, list.size(), "erp-notify.enabled=false 时通知仍应落库（kill-switch 失效）: " + list.size());
            assertEquals(ErpNotifyConstants.STATUS_SENT, list.get(0).getStatus());
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpNotifyConfigs.CONFIG_NOTIFY_ENABLED, "true");
        }
    }

    // ---------- helpers ----------

    private ApiResponse<?> notify(String eventType, Map<String, Object> context) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpSysNotification__notify",
                ApiRequest.build(Map.of("eventType", eventType, "context", context)));
        return graphQLEngine.executeRpc(ctx);
    }

    private List<ErpSysNotification> notificationsOf(String userId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private void seedTemplate(Long id, String notificationType, String subjectTpl, String bodyTpl,
                              String resolver, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", String.valueOf(id));
            t.setNotificationType(notificationType);
            t.setName("TPL-" + notificationType);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl(subjectTpl);
            t.setBodyTpl(bodyTpl);
            t.setRecipientResolver(resolver);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }

    private void seedRole(String roleName, String userId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<NopAuthRole> roleDao = daoProvider.daoFor(NopAuthRole.class);
            NopAuthRole role = new NopAuthRole();
            role.setRoleId("rr-role-" + roleName);
            role.setRoleName(roleName);
            roleDao.saveEntity(role);

            seedAuthUserInSession(userId, null);

            IEntityDao<NopAuthUserRole> urDao = daoProvider.daoFor(NopAuthUserRole.class);
            NopAuthUserRole ur = new NopAuthUserRole();
            ur.setUserId(userId);
            ur.setRoleId("rr-role-" + roleName);
            urDao.saveEntity(ur);
        });
    }

    private void seedAuthUser(String userId, String deptId) {
        ormTemplate.runInSession(() -> seedAuthUserInSession(userId, deptId));
    }

    private void seedAuthUserInSession(String userId, String deptId) {
        IEntityDao<NopAuthUser> userDao = daoProvider.daoFor(NopAuthUser.class);
        NopAuthUser user = new NopAuthUser();
        user.setUserId(userId);
        user.setUserName(userId);
        user.setNickName(userId);
        user.setPassword("dummy-pwd");
        user.setOpenId(userId);
        user.setGender(0);
        user.setUserType(0);
        user.setStatus(0);
        user.setTenantId("0");
        if (deptId != null) {
            user.setDeptId(deptId);
        }
        userDao.saveEntity(user);
    }
}
