package app.erp.notify.service;

import app.erp.notify.dao.entity.ErpSysNotification;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-CK-notify-010-r3 / P2-CK-notify-009-r3（INSTANCE_NOT_FOUND 接线面）已读身份校验回归。
 *
 * <p>缺陷面：markAllRead 显式 userId 优先于 ctx（任意用户可批量置他人已读）；markRead 对不存在
 * 通知静默写孤儿已读行（INSTANCE_NOT_FOUND 已定义未接线）。
 *
 * <p>修复裁决：markRead/markAllRead 统一强制「ctx 用户 = 目标接收人」身份校验（ctx 无用户时保持
 * 系统内部调用兼容）；markRead 通知不存在抛 ERR_NOTIFY_INSTANCE_NOT_FOUND。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSysNotificationReadIdentity extends JunitAutoTestCase {

    static final String USER_A = "read-identity-user-a";
    static final String USER_B = "read-identity-user-b";

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testMarkAllReadRejectsOtherUserIdentity() {
        seedNotification(USER_B);
        setCurrentUser(USER_A);

        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpSysNotification__markAllRead",
                Map.of("userId", USER_B));
        assertTrue(resp.getStatus() != 0, "当前用户不得批量置他人已读: " + resp);
        assertEquals(ErpNotifyErrors.ERR_NOTIFY_USER_MISMATCH.getErrorCode(), resp.getCode(),
                "越权 markAllRead 拒绝码应为 ERR_NOTIFY_USER_MISMATCH");
    }

    @Test
    public void testMarkAllReadCtxUserFallbackAllowed() {
        seedNotification(USER_A);
        seedNotification(USER_A);
        setCurrentUser(USER_A);

        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpSysNotification__markAllRead", Map.of());
        assertEquals(0, resp.getStatus(), "ctx 用户回退路径应放行: " + resp);
        assertEquals(2, ((Number) resp.getData()).intValue(), "ctx 用户自身未读应全部标记");
    }

    @Test
    public void testMarkReadUnknownNotificationThrowsInstanceNotFound() {
        setCurrentUser(USER_A);
        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpSysNotification__markRead",
                Map.of("notificationId", "999999"));
        assertTrue(resp.getStatus() != 0, "通知不存在时 markRead 不得静默写孤儿已读行: " + resp);
        assertEquals(ErpNotifyErrors.ERR_NOTIFY_INSTANCE_NOT_FOUND.getErrorCode(), resp.getCode(),
                "通知不存在拒绝码应为 ERR_NOTIFY_INSTANCE_NOT_FOUND（notify-009 接线面）");
    }

    @Test
    public void testMarkReadOtherUsersNotificationRejected() {
        String id = seedNotification(USER_B);
        setCurrentUser(USER_A);

        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpSysNotification__markRead",
                Map.of("notificationId", id));
        assertTrue(resp.getStatus() != 0, "当前用户不得标记他人通知已读: " + resp);
        assertEquals(ErpNotifyErrors.ERR_NOTIFY_USER_MISMATCH.getErrorCode(), resp.getCode(),
                "越权 markRead 拒绝码应为 ERR_NOTIFY_USER_MISMATCH");
    }

    private String seedNotification(String recipientUserId) {
        return ormTemplate.runInSession(session -> {
            ErpSysNotification n = daoProvider.daoFor(ErpSysNotification.class).newEntity();
            n.setNotificationType("notify.read-identity-test");
            n.setRecipientUserId(recipientUserId);
            n.setStatus(ErpNotifyConstants.STATUS_SENT);
            n.setSubject("read-identity-test");
            daoProvider.daoFor(ErpSysNotification.class).saveEntity(n);
            return n.getId();
        });
    }

    private void setCurrentUser(String userId) {
        ContextProvider.getOrCreateContext().setUserId(userId);
        ContextProvider.getOrCreateContext().setUserName(userId);
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
