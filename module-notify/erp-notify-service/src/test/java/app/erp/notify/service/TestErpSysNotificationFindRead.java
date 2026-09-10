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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片④ notify-011-r3（DIM-T 面）：{@code findRead} 唯一消费路径回归——已读查询与
 * {@code findUnread} 对称（markRead 后移入已读集合、findUnread 不再返回、findRead 排除他人通知）。
 *
 * <p><b>DIM-F 半面注记（同控制点，不入本测试批）</b>：inbox「全部」tab 数据源仅拼接 findUnread
 * （已读通知在全部 tab 永不出现）+ selection 请求实体不存在的 {@code read} 幽灵字段——前端半面修复
 * 归前端批（page.yaml 冻结裁决下面向 flux/手写页的独立修复面），本批仅以本测试固定 findRead 服务端
 * 契约（owner doc inbox-patterns.md「全部 = findUnread + findRead 拼接」的服务端前提）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSysNotificationFindRead extends JunitAutoTestCase {

    static final String USER_A = "find-read-user-a";
    static final String USER_B = "find-read-user-b";

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    @SuppressWarnings("unchecked")
    public void testMarkReadMovesNotificationIntoFindRead() {
        String n1 = seedNotification(USER_A);
        String n2 = seedNotification(USER_A);
        seedNotification(USER_B);
        setCurrentUser(USER_A);

        assertEquals(2, unreadCount(), "前置：USER_A 两条未读");
        assertTrue(readIds().isEmpty(), "前置：零已读");

        ApiResponse<?> markResp = rpc(GraphQLOperationType.mutation, "ErpSysNotification__markRead",
                Map.of("notificationId", n1));
        assertEquals(0, markResp.getStatus(), "markRead 应成功: " + markResp);

        // findUnread 收缩、findRead 扩张且恰含 n1（含字段完整性）
        List<String> unread = unreadIds();
        assertEquals(1, unread.size(), "markRead 后未读收缩为 1");
        assertEquals(n2, unread.get(0), "未读集合应恰剩 n2");

        List<String> read = readIds();
        assertEquals(1, read.size(), "markRead 后已读恰 1 条");
        assertEquals(n1, read.get(0), "findRead 应返回被标记的 n1");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testFindReadIsolatedByUser() {
        seedNotification(USER_B);
        String bRead = seedNotification(USER_B);
        setCurrentUser(USER_B);

        ApiResponse<?> markResp = rpc(GraphQLOperationType.mutation, "ErpSysNotification__markRead",
                Map.of("notificationId", bRead));
        assertEquals(0, markResp.getStatus(), "USER_B 标记自身通知应成功: " + markResp);

        setCurrentUser(USER_A);
        List<String> readA = readIds();
        assertTrue(readA.isEmpty(), "USER_A 的 findRead 不得含 USER_B 已读通知");
        List<String> readB;
        setCurrentUser(USER_B);
        readB = readIds();
        assertEquals(1, readB.size(), "USER_B findRead 恰 1 条");
        assertEquals(bRead, readB.get(0));
    }

    private long unreadCount() {
        setCurrentUser(USER_A);
        ApiResponse<?> resp = rpc(GraphQLOperationType.query, "ErpSysNotification__countUnread", Map.of());
        return ((Number) resp.getData()).longValue();
    }

    @SuppressWarnings("unchecked")
    private List<String> unreadIds() {
        ApiResponse<?> resp = rpc(GraphQLOperationType.query, "ErpSysNotification__findUnread", Map.of());
        return idsOf(resp);
    }

    @SuppressWarnings("unchecked")
    private List<String> readIds() {
        ApiResponse<?> resp = rpc(GraphQLOperationType.query, "ErpSysNotification__findRead", Map.of());
        return idsOf(resp);
    }

    @SuppressWarnings("unchecked")
    private List<String> idsOf(ApiResponse<?> resp) {
        assertEquals(0, resp.getStatus(), "查询应成功: " + resp);
        Object data = resp.getData();
        assertTrue(data instanceof List, "findRead/findUnread 应返回列表: " + data);
        return ((List<Object>) data).stream()
                .map(o -> (String) ((Map<String, Object>) o).get("id"))
                .collect(java.util.stream.Collectors.toList());
    }

    private String seedNotification(String recipientUserId) {
        return ormTemplate.runInSession(session -> {
            ErpSysNotification n = daoProvider.daoFor(ErpSysNotification.class).newEntity();
            n.setNotificationType("notify.find-read-test");
            n.setRecipientUserId(recipientUserId);
            n.setStatus(ErpNotifyConstants.STATUS_SENT);
            n.setSubject("find-read-test");
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
