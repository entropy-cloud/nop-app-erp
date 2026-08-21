package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsEntitlement;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTeam;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.dao.entity.ErpCsTicketType;
import app.erp.cs.service.entity.TicketAssignResolver;
import app.erp.crm.dao.entity.ErpCrmTeam;
import app.erp.crm.dao.entity.ErpCrmTeamMember;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.auth.dao.entity.NopAuthRole;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.auth.dao.entity.NopAuthUserRole;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import io.nop.sys.dao.entity.NopSysCodeRule;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单创建自动富化端到端测试（RC-R1.65，P1-RC-054，UC-CS-01 ②-⑧ + UC-CS-09 reuse；
 * plan 2026-08-17-2125-1 Phase 3 测试组 ①-⑧）。
 *
 * <p>覆盖 save 路径富化：缺省填充（status=NEW + priority←ticketType 默认）+ 后置自动 matchAndAttachSla
 * （调用点守卫：slaPolicyId 与 deadlineDateTime 均空才触发——UC-CS-09 权益单次扣减 reuse）+
 * TK{YYYYMM}{SEQ4} 按月序列编号（派生 xmeta biz:codeRule + 自定义 CodeRuleVariable）+
 * 自动分配（config 门控 + 轮转/最少未结 + 无匹配留 NEW + 主管升级通知）+ 客户确认通知（IN_APP 占位）。
 *
 * <p>断言式测试（镜像 R1.37 TestErpLogDraftEscalationJob 范式：JunitAutoTestCase + 空 autotest.yaml 标记），
 * 不录制表快照——TK 编号/月序列行名内嵌真实时钟 yyyyMM（sysCalendar 不受冻结时钟控制），
 * 录制 CSV 会随月份漂移翻红。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsTicketCreateEnrichment extends JunitAutoTestCase {

    static final String CUSTOMER_ID = "5301";
    static final String TICKET_TYPE_ID = "6301";
    static final String CS_TEAM_ID = "6401";
    static final String TEAM_CODE = "TEAM-ENRICH";
    static final String POLICY_ATTACH_ID = "6501";   // teamId NULL（可被 matcher 匹配，resolveHours=8）
    static final String POLICY_TEAM_ID = "6502";     // teamId=CS_TEAM（类型默认策略——team 解析主链载体）
    static final String POLICY_ENT_ID = "6503";      // 权益覆盖策略（teamId NULL，resolveHours=2）
    // bridge-test-115: crm 未迁移（M3.4）Long 实体侧局部桥（crm seed 保持 Long，退役 owner M3.4）
    static final Long CRM_TEAM_ID = 6601L;
    static final String USER_A = "cs-enrich-user-a";
    static final String USER_B = "cs-enrich-user-b";
    static final String USER_C = "cs-enrich-user-c";
    static final String ENT_ID = "6801";
    static final String CS_SUPERVISOR = "cs-enrich-supervisor";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ① 缺省填充 + 后置自动挂载（config off 隔离分配维度） ----------

    @Test
    public void testCreateFillsDefaultsAndAutoAttachesSla() {
        seedCodeRule();
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedBaseSlaPolicies();

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_AUTO_ASSIGN_ON_CREATE, "false");
        try {
            LocalDateTime before = CoreMetrics.currentDateTime();
            ApiResponse<?> resp = saveTicket(ticketData("TK 缺省填充", null));
            assertEquals(0, resp.getStatus(), "save 应成功: " + resp);
            String id = idOf(resp);
            ErpCsTicket t = reload(id);

            assertEquals(ErpCsConstants.TICKET_STATUS_NEW, t.getStatus(), "缺省 status=NEW（fill-when-absent）");
            assertEquals(ErpCsConstants.TICKET_PRIORITY_NORMAL, t.getPriority(),
                    "缺省 priority ← ticketType.defaultPriority");
            assertEquals(POLICY_ATTACH_ID, t.getSlaPolicyId(), "后置自动挂载 SLA 策略");
            assertNotNull(t.getDeadlineDateTime(), "自动计算 deadlineDateTime");
            assertTrue(t.getDeadlineDateTime().toLocalDateTime().isAfter(before.plusHours(7)),
                    "deadline ≈ now + resolveHours(8)");
            assertTrue(t.getDeadlineDateTime().toLocalDateTime().isBefore(CoreMetrics.currentDateTime().plusHours(9)));
            assertTrue(isTkCode(t.getCode()), "不传 code 自动生成 TK 格式编号");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_AUTO_ASSIGN_ON_CREATE, "true");
        }
    }

    // ---------- ② 自动 deadline + 调用点守卫（UC-CS-09 单次扣减 reuse）+ 手动重匹配共存 ----------

    @Test
    public void testAutoDeadlineGuardAndEntitlementSingleConsume() {
        seedCodeRule();
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedBaseSlaPolicies();
        seedEntitlement(ENT_ID, CUSTOMER_ID, 5, 1);

        // a) 不传 slaPolicyId/deadline：自动挂载 = 权益匹配扣减 + deadline 三合一，恰好扣减一次（UC-CS-09 reuse）
        ApiResponse<?> r1 = saveTicket(ticketData("TK-ENT-REUSE", null));
        assertEquals(0, r1.getStatus(), "save 应成功: " + r1);
        ErpCsTicket t1 = reload(idOf(r1));
        assertEquals(POLICY_ENT_ID, t1.getSlaPolicyId(), "权益级 slaPolicyId 覆盖工单类型默认");
        assertNotNull(t1.getDeadlineDateTime(), "自动挂载计算 deadline");
        assertEquals(2, reloadEntitlement().getUsedTickets().intValue(),
                "save 自动挂载恰好扣减一次（usedTickets 1→2）");

        // b) 调用点守卫（显式 slaPolicyId、deadline 未设）：跳过自动挂载——不扣权益、不算 deadline
        Map<String, Object> data2 = ticketData("TK-ENT-GUARD-POLICY", null);
        data2.put("slaPolicyId", POLICY_TEAM_ID);
        ApiResponse<?> r2 = saveTicket(data2);
        assertEquals(0, r2.getStatus(), "save 应成功: " + r2);
        ErpCsTicket t2 = reload(idOf(r2));
        assertEquals(POLICY_TEAM_ID, t2.getSlaPolicyId(), "显式 slaPolicyId 不被覆盖");
        assertNull(t2.getDeadlineDateTime(), "守卫跳过：不自动计算 deadline");
        assertEquals(2, reloadEntitlement().getUsedTickets().intValue(), "守卫跳过：不重复扣减权益");

        // b') 手动 matchAndAttachSla mutation 仍可用（既有语义：扣减 + 补 deadline）
        ApiResponse<?> manual = rpc(mutation, "ErpCsTicket__matchAndAttachSla", Map.of("ticketId", idOf(r2)));
        assertEquals(0, manual.getStatus(), "手动 matchAndAttachSla 应成功: " + manual);
        assertNotNull(reload(idOf(r2)).getDeadlineDateTime(), "手动重匹配补算 deadline");
        assertEquals(3, reloadEntitlement().getUsedTickets().intValue(),
                "手动调用按既有语义扣减（单触发点设计，watch-only 残留）");

        // c) 调用点守卫（显式 deadline、slaPolicyId 未设）：同样跳过自动挂载
        LocalDateTime explicitDeadline = CoreMetrics.currentDateTime().plusHours(2).withNano(0);
        Map<String, Object> data3 = ticketData("TK-ENT-GUARD-DEADLINE", null);
        data3.put("deadlineDateTime", Timestamp.valueOf(explicitDeadline));
        ApiResponse<?> r3 = saveTicket(data3);
        assertEquals(0, r3.getStatus(), "save 应成功: " + r3);
        ErpCsTicket t3 = reload(idOf(r3));
        assertNull(t3.getSlaPolicyId(), "守卫跳过：不自动挂载策略");
        assertNotNull(t3.getDeadlineDateTime(), "显式 deadline 保持");
        assertEquals(explicitDeadline, t3.getDeadlineDateTime().toLocalDateTime(), "显式 deadline 保持不变");
        assertEquals(3, reloadEntitlement().getUsedTickets().intValue(), "守卫跳过：不扣减权益");
    }

    // ---------- ③ TK 编号：格式 + 按月序列递增 + 显式 code 共存（catalog TK-millis 同机制） ----------

    @Test
    public void testTkCodeFormatIncrementAndExplicitCoexistence() {
        seedCodeRule();
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedBaseSlaPolicies();

        ErpCsTicket first = reload(idOf(saveTicket(ticketData("TK-CODE-1", null))));
        assertTrue(isTkCode(first.getCode()), "生成 TK{YYYYMM}{SEQ4} 格式: " + first.getCode());
        assertEquals("0001", first.getCode().substring(first.getCode().length() - 4),
                "按月序列首号为 0001");

        ErpCsTicket second = reload(idOf(saveTicket(ticketData("TK-CODE-2", null))));
        assertEquals("0002", second.getCode().substring(second.getCode().length() - 4),
                "按月序列单调递增（0002）");
        assertFalse(first.getCode().equals(second.getCode()), "两单编号不重号");

        // 显式 code 不被 autoExpr 覆盖（catalog TK-<millis> 路径共存：同为显式 code 机制）
        Map<String, Object> data = ticketData("TK-CODE-EXPLICIT", ErpCsConstants.TICKET_PRIORITY_NORMAL);
        data.put("code", "TK-1700000001234");
        ErpCsTicket explicit = reload(idOf(saveTicket(data)));
        assertEquals("TK-1700000001234", explicit.getCode(), "显式 code 保持（autoExpr fill-when-absent）");
    }

    // ---------- ④ 自动分配成功：ASSIGNED + ASSIGN 审计 + 确认通知 + ⑦ 待办可见 ----------

    @Test
    public void testAutoAssignSuccessNotifiesAndTodoVisible() {
        seedCodeRule();
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedBaseSlaPolicies();
        seedTeamChain();
        seedNotifyTemplate("7301", ErpCsConstants.NOTIFY_EVENT_TICKET_CREATED,
                "USER_LIST", "{\"userIds\":[\"${submitterUserId}\"]}");

        ApiResponse<?> resp = saveTicket(ticketData("TK-AUTO-ASSIGN", null));
        assertEquals(0, resp.getStatus(), "save 应成功: " + resp);
        String id = idOf(resp);
        ErpCsTicket t = reload(id);

        assertEquals(ErpCsConstants.TICKET_STATUS_ASSIGNED, t.getStatus(), "自动分配成功 NEW→ASSIGNED");
        assertEquals(USER_A, t.getAssignedToId(), "ROUND_ROBIN 无历史取候选池首位");

        assertTrue(hasAction(id, ErpCsConstants.ACTION_TYPE_ASSIGN), "写 ASSIGN 审计");
        ErpCsTicketAction action = firstAction(id, ErpCsConstants.ACTION_TYPE_ASSIGN);
        assertEquals(ErpCsConstants.TICKET_STATUS_NEW, action.getFromStatus(), "审计 fromStatus=NEW");
        assertEquals(ErpCsConstants.TICKET_STATUS_ASSIGNED, action.getToStatus(), "审计 toStatus=ASSIGNED");

        // ⑥ 客户确认通知落库：接收人 = 提单人 createdBy（USER_LIST ${submitterUserId} 插值；
        // 测试框架默认上下文用户 autotest-ref，语义断言=接收人与 createdBy 一致）
        ErpCsTicket saved = reload(id);
        List<ErpSysNotification> created = notificationsOf(ErpCsConstants.NOTIFY_EVENT_TICKET_CREATED);
        assertEquals(1, created.size(), "创建确认通知恰好 1 条");
        assertNotNull(saved.getCreatedBy(), "工单 createdBy 落库（提单人）");
        assertEquals(saved.getCreatedBy(), created.get(0).getRecipientUserId(), "接收人=提单人 createdBy");

        // ⑦ 处理人待办列表可见：按 assignedToId + status=ASSIGNED 查询命中新工单
        QueryBean todo = new QueryBean();
        todo.addFilter(eq("assignedToId", USER_A));
        todo.addFilter(eq("status", ErpCsConstants.TICKET_STATUS_ASSIGNED));
        List<ErpCsTicket> todos = daoProvider.daoFor(ErpCsTicket.class).findAllByQuery(todo);
        assertTrue(todos.stream().anyMatch(x -> id.equals(x.getId())), "待办列表出现新工单（⑦）");
    }

    // ---------- ⑤ 无匹配池：留 NEW + 客服主管升级通知（ROLE 路径） ----------

    @Test
    public void testNoMatchPoolStaysNewWithEscalation() {
        seedCodeRule();
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedBaseSlaPolicies(); // 含 cs 团队（仅 cs 侧，无同码 crm 团队/成员 → 池空）
        seedSupervisorRole();
        seedNotifyTemplate("7302", ErpCsConstants.NOTIFY_EVENT_TICKET_ASSIGN_NO_MATCH,
                "ROLE", "{\"roles\":[\"客服主管\"]}");

        ApiResponse<?> resp = saveTicket(ticketData("TK-NO-MATCH", null));
        assertEquals(0, resp.getStatus(), "save 应成功: " + resp);
        String id = idOf(resp);
        ErpCsTicket t = reload(id);

        assertEquals(ErpCsConstants.TICKET_STATUS_NEW, t.getStatus(), "⑧ 无匹配留 NEW 待人工分派");
        assertNull(t.getAssignedToId(), "未分配处理人");
        assertFalse(hasAction(id, ErpCsConstants.ACTION_TYPE_ASSIGN), "无 ASSIGN 审计");
        assertNotNull(t.getCode(), "编号富化仍执行（⑧ 不阻断其余维度）");

        List<ErpSysNotification> escalations =
                notificationsOf(ErpCsConstants.NOTIFY_EVENT_TICKET_ASSIGN_NO_MATCH);
        assertEquals(1, escalations.size(), "升级通知恰好 1 条（ROLE 客服主管）");
        assertEquals(CS_SUPERVISOR, escalations.get(0).getRecipientUserId(), "接收人=客服主管角色成员");
    }

    // ---------- ⑥ config off：仅跳过分配维度（建议匹配/deadline/编号仍执行） ----------

    @Test
    public void testAutoAssignConfigOffSkipsAssignOnly() {
        seedCodeRule();
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedBaseSlaPolicies();
        seedTeamChain();

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_AUTO_ASSIGN_ON_CREATE, "false");
        try {
            ApiResponse<?> resp = saveTicket(ticketData("TK-CFG-OFF", null));
            assertEquals(0, resp.getStatus(), "save 应成功: " + resp);
            String id = idOf(resp);
            ErpCsTicket t = reload(id);

            assertEquals(ErpCsConstants.TICKET_STATUS_NEW, t.getStatus(), "config off 跳过分配（留 NEW）");
            assertNull(t.getAssignedToId(), "config off 不分配处理人");
            assertTrue(isTkCode(t.getCode()), "编号富化仍执行");
            assertEquals(POLICY_ATTACH_ID, t.getSlaPolicyId(), "SLA 挂载仍执行");
            assertNotNull(t.getDeadlineDateTime(), "deadline 富化仍执行");
            assertTrue(notificationsOf(ErpCsConstants.NOTIFY_EVENT_TICKET_ASSIGN_NO_MATCH).isEmpty(),
                    "config off 不触发无匹配升级通知");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_AUTO_ASSIGN_ON_CREATE, "true");
        }
    }

    // ---------- ⑦ 分配算法纯函数断言（mock 池） + 按月序列工具 ----------

    @Test
    public void testAssignAlgorithmPureFunctions() {
        List<String> pool = List.of(USER_A, USER_B, USER_C);

        // ROUND_ROBIN：上次分配的下一个；无历史/历史不在池内 → 首位
        assertEquals(USER_A, TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_ROUND_ROBIN, pool, null, Map.of()), "无历史取首位");
        assertEquals(USER_B, TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_ROUND_ROBIN, pool, USER_A, Map.of()), "a 的下一个是 b");
        assertEquals(USER_A, TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_ROUND_ROBIN, pool, USER_C, Map.of()), "c 的下一个回绕到 a");
        assertEquals(USER_A, TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_ROUND_ROBIN, pool, "not-in-pool", Map.of()),
                "历史不在池内取首位");

        // LEAST_OPEN：活跃计数最少者；平手取列表序首位
        assertEquals(USER_B, TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_LEAST_OPEN, pool, null, Map.of(USER_A, 2, USER_B, 0, USER_C, 1)),
                "最少未结者 b");
        assertEquals(USER_A, TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_LEAST_OPEN, pool, null, Map.of()),
                "全零平手取首位 a");
        assertEquals(USER_C, TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_LEAST_OPEN, pool, null, Map.of(USER_A, 3, USER_B, 2, USER_C, 1)),
                "最少未结者 c");

        // 空池/未知方法降级
        assertNull(TicketAssignResolver.pickAssignee(
                ErpCsConstants.ASSIGN_METHOD_ROUND_ROBIN, List.of(), null, Map.of()), "空池降级 null");

        // 按月序列格式（%04d 补零 + 超 4 位右截断回绕，public 常量 + 端到端 ③ 覆盖）
        assertTrue(app.erp.cs.service.entity.CsTicketMonthSeqCodeRuleVariable.SEQ_NAME_PREFIX
                .startsWith("cs_ticket_code_seq_"));
    }

    // ---------- ⑧ 轮转/最少未结端到端 ----------

    @Test
    public void testRoundRobinRotationAndLeastOpenEndToEnd() {
        seedCodeRule();
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedBaseSlaPolicies();
        seedTeamChain();

        // 预置 a 的历史已分配工单 → ROUND_ROBIN 取 a 的下一个 = b
        seedTicketRow("TK-HIST-A", ErpCsConstants.TICKET_STATUS_ASSIGNED, USER_A);
        ApiResponse<?> r1 = saveTicket(ticketData("TK-RR-ROTATE", null));
        assertEquals(0, r1.getStatus(), "save 应成功: " + r1);
        assertEquals(USER_B, reload(idOf(r1)).getAssignedToId(),
                "ROUND_ROBIN：上次分配 a 的下一个是 b");

        // LEAST_OPEN：b 已有 2 张活跃（历史 1 张 + 刚分配 1 张），a 有 1 张，c 有 0 张 → 取 c
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_ASSIGN_METHOD, ErpCsConstants.ASSIGN_METHOD_LEAST_OPEN);
        try {
            seedTicketRow("TK-HIST-B", ErpCsConstants.TICKET_STATUS_ASSIGNED, USER_B);
            ApiResponse<?> r2 = saveTicket(ticketData("TK-LEAST-OPEN", null));
            assertEquals(0, r2.getStatus(), "save 应成功: " + r2);
            assertEquals(USER_C, reload(idOf(r2)).getAssignedToId(),
                    "LEAST_OPEN：活跃工单最少者 c");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_ASSIGN_METHOD, ErpCsConstants.ASSIGN_METHOD_ROUND_ROBIN);
        }
    }

    // ---------- helpers ----------

    private static boolean isTkCode(String code) {
        return code != null && code.matches("^TK\\d{10}$");
    }

    private ErpCsTicket reload(String id) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(id);
    }

    private ErpCsEntitlement reloadEntitlement() {
        return daoProvider.daoFor(ErpCsEntitlement.class).getEntityById(ENT_ID);
    }

    private String idOf(ApiResponse<?> resp) {
        Object data = resp.getData();
        assertNotNull(data, "save 响应应含实体: " + resp);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) data;
        Object id = map.get("id");
        assertNotNull(id, "save 响应应含 id: " + resp);
        return String.valueOf(id);
    }

    private boolean hasAction(String ticketId, String actionType) {
        return firstAction(ticketId, actionType) != null;
    }

    private ErpCsTicketAction firstAction(String ticketId, String actionType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", actionType));
        q.setLimit(1);
        List<ErpCsTicketAction> list = daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpSysNotification> notificationsOf(String notificationType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", notificationType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    /** GraphQL save 输入：docStatus/approveStatus 显式提供（mandatory 无默认），富化字段（status/priority/code）按需省略。 */
    private Map<String, Object> ticketData(String subject, String priority) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subject", subject);
        data.put("customerId", CUSTOMER_ID);
        data.put("ticketTypeId", TICKET_TYPE_ID);
        data.put("docStatus", ErpCsConstants.DOC_STATUS_ACTIVE);
        data.put("approveStatus", ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
        if (priority != null) {
            data.put("priority", priority);
        }
        return data;
    }

    private ApiResponse<?> saveTicket(Map<String, Object> data) {
        return rpc(mutation, "ErpCsTicket__save", Map.of("data", data));
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedCodeRule() {
        ormTemplate.runInSession(() -> {
            IEntityDao<NopSysCodeRule> dao = daoProvider.daoFor(NopSysCodeRule.class);
            NopSysCodeRule rule = new NopSysCodeRule();
            rule.setName("cs-ticket-code");
            rule.setDisplayName("客服工单TK编号规则");
            rule.setCodePattern("TK{@year}{@month}{@csTicketMonthSeq:4}");
            rule.setSeqName("default");
            rule.setCreatedBy("system");
            rule.setCreateTime(new Timestamp(CoreMetrics.currentTimeMillis()));
            rule.setUpdatedBy("system");
            rule.setUpdateTime(new Timestamp(CoreMetrics.currentTimeMillis()));
            dao.saveEntity(rule);
        });
    }

    private void seedCustomer(String id, String name) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            ErpMdPartner p = new ErpMdPartner();
            p.orm_propValueByName("id", id);
            p.setCode("CUS-" + id);
            p.setName(name);
            p.orm_propValueByName("partnerType", "CUSTOMER");
            p.orm_propValueByName("status", "ACTIVE");
            dao.saveEntity(p);
        });
    }

    /** POLICY_ATTACH（teamId NULL，matcher 可匹配）+ POLICY_TEAM（类型默认策略，team 载体）+ 工单类型。 */
    private void seedBaseSlaPolicies() {
        seedCsTeam();
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsSlaPolicy> dao = daoProvider.daoFor(ErpCsSlaPolicy.class);

            ErpCsSlaPolicy attach = new ErpCsSlaPolicy();
            attach.orm_propValueByName("id", POLICY_ATTACH_ID);
            attach.setCode("SLA-ENRICH-ATTACH");
            attach.setName("富化测试-可匹配策略");
            attach.setTicketTypeId(TICKET_TYPE_ID);
            attach.setResolveHours(8);
            attach.setIsWorkingDays(false);
            dao.saveEntity(attach);

            ErpCsSlaPolicy team = new ErpCsSlaPolicy();
            team.orm_propValueByName("id", POLICY_TEAM_ID);
            team.setCode("SLA-ENRICH-TEAM");
            team.setName("富化测试-类型默认策略");
            team.setTicketTypeId(TICKET_TYPE_ID);
            team.setTeamId(CS_TEAM_ID);
            team.setResolveHours(4);
            team.setIsWorkingDays(false);
            dao.saveEntity(team);

            ErpCsSlaPolicy ent = new ErpCsSlaPolicy();
            ent.orm_propValueByName("id", POLICY_ENT_ID);
            ent.setCode("SLA-ENRICH-ENT");
            ent.setName("富化测试-权益覆盖策略");
            ent.setTicketTypeId(TICKET_TYPE_ID);
            ent.setResolveHours(2);
            ent.setIsWorkingDays(false);
            dao.saveEntity(ent);
        });
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicketType> dao = daoProvider.daoFor(ErpCsTicketType.class);
            ErpCsTicketType type = new ErpCsTicketType();
            type.orm_propValueByName("id", TICKET_TYPE_ID);
            type.setCode("TYPE-ENRICH");
            type.setName("富化测试工单类型");
            type.setDefaultPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
            type.setDefaultSlaPolicyId(POLICY_TEAM_ID);
            dao.saveEntity(type);
        });
    }

    private void seedCsTeam() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTeam> dao = daoProvider.daoFor(ErpCsTeam.class);
            ErpCsTeam team = new ErpCsTeam();
            team.orm_propValueByName("id", CS_TEAM_ID);
            team.setCode(TEAM_CODE);
            team.setName("富化测试客服团队");
            dao.saveEntity(team);
        });
    }

    /** 同码 crm 团队 + 三名成员（成员行 id 升序 = 候选池序 a→b→c）。 */
    private void seedTeamChain() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCrmTeam> teamDao = daoProvider.daoFor(ErpCrmTeam.class);
            ErpCrmTeam team = new ErpCrmTeam();
            team.orm_propValueByName("id", CRM_TEAM_ID);
            team.setCode(TEAM_CODE);
            team.setName("富化测试CRM团队");
            teamDao.saveEntity(team);

            IEntityDao<ErpCrmTeamMember> memberDao = daoProvider.daoFor(ErpCrmTeamMember.class);
            seedMemberInSession(memberDao, 6701L, CRM_TEAM_ID, USER_A);
            seedMemberInSession(memberDao, 6702L, CRM_TEAM_ID, USER_B);
            seedMemberInSession(memberDao, 6703L, CRM_TEAM_ID, USER_C);
        });
    }

    private void seedMemberInSession(IEntityDao<ErpCrmTeamMember> dao, Long id, Long teamId, String userId) {
        ErpCrmTeamMember member = new ErpCrmTeamMember();
        member.orm_propValueByName("id", id);
        member.setTeamId(teamId);
        member.setUserId(userId);
        dao.saveEntity(member);
    }

    private void seedEntitlement(String id, String partnerId, Integer maxTickets, Integer usedTickets) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsEntitlement> dao = daoProvider.daoFor(ErpCsEntitlement.class);
            ErpCsEntitlement e = new ErpCsEntitlement();
            e.orm_propValueByName("id", id);
            e.setCode("ENT-" + id);
            e.setPartnerId(partnerId);
            e.setServiceType(ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET);
            e.setStartDate(CoreMetrics.currentDate().minusDays(5));
            e.setEndDate(CoreMetrics.currentDate().plusDays(30));
            e.setMaxTickets(maxTickets);
            e.setUsedTickets(usedTickets);
            e.setIsActive(Boolean.TRUE);
            e.setSlaPolicyId(POLICY_ENT_ID);
            dao.saveEntity(e);
        });
    }

    private void seedNotifyTemplate(String id, String notificationType, String resolver, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(notificationType);
            t.setName("富化测试-" + notificationType);
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("工单通知: ${ticketCode}");
            t.setBodyTpl("工单 ${ticketCode}（ID ${ticketId}）");
            t.setRecipientResolver(resolver);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy("NONE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private void seedSupervisorRole() {
        ormTemplate.runInSession(() -> {
            IEntityDao<NopAuthUser> userDao = daoProvider.daoFor(NopAuthUser.class);
            NopAuthUser user = new NopAuthUser();
            user.setUserId(CS_SUPERVISOR);
            user.setUserName(CS_SUPERVISOR);
            user.setNickName(CS_SUPERVISOR);
            user.setPassword("dummy-pwd");
            user.setOpenId(CS_SUPERVISOR);
            user.orm_propValueByName("gender", 1);
            user.orm_propValueByName("userType", 0);
            user.orm_propValueByName("status", 1);
            user.orm_propValueByName("delFlag", 0);
            user.orm_propValueByName("tenantId", "0");
            userDao.saveEntity(user);

            IEntityDao<NopAuthRole> roleDao = daoProvider.daoFor(NopAuthRole.class);
            NopAuthRole role = new NopAuthRole();
            role.setRoleId("role-cs-supervisor");
            role.setRoleName("客服主管");
            roleDao.saveEntity(role);

            IEntityDao<NopAuthUserRole> urDao = daoProvider.daoFor(NopAuthUserRole.class);
            NopAuthUserRole ur = new NopAuthUserRole();
            ur.setUserId(CS_SUPERVISOR);
            ur.setRoleId("role-cs-supervisor");
            urDao.saveEntity(ur);
        });
    }

    /** dao 直插历史工单（ROUND_ROBIN 历史锚点 / LEAST_OPEN 活跃计数锚点）。 */
    private void seedTicketRow(String code, String status, String assignedToId) {
        String id = String.valueOf(7000 + Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(CUSTOMER_ID);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
            t.setStatus(status);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            t.setAssignedToId(assignedToId);
            t.setBusinessDate(LocalDate.of(2026, 8, 1));
            dao.saveEntity(t);
        });
    }
}
