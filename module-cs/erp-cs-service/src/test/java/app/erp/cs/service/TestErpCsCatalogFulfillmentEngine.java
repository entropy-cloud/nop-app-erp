package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.dao.entity.ErpCsTicketFulfillmentStep;
import app.erp.crm.dao.entity.ErpCrmTeam;
import app.erp.crm.dao.entity.ErpCrmTeamMember;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTeam;
import app.erp.cs.dao.entity.ErpCsTicketType;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import io.nop.sys.dao.entity.NopSysCodeRule;
import jakarta.inject.Inject;
import io.nop.auth.dao.entity.NopAuthRole;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.auth.dao.entity.NopAuthUserRole;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * cs 目录履行引擎测试（RC-R1.71，P1-RC-061，UC-CS-12 ②③④+后置+异常；
 * plan 2026-08-18-1849-3 Phase 1 Proof ①-⑩ + Phase 2 Proof ⑪-⑯）。
 *
 * <p>覆盖：①物化幂等 ②ASSIGN_TEAM RR 真实分配 ③REQUEST_APPROVAL→approve(true) ④审批驳回终局
 * ⑤NOTIFY_CUSTOMER 落库 ⑥UPDATE_STATUS 合法/非法/缺配置 ⑦CREATE_CHILD_TICKET 双向弱指针
 * ⑧失败中断+管理员通知 ⑨全 DONE→IN_PROGRESS（无 ASSIGN 链自动指派）⑩尾部 RESOLVED 组合；
 * ⑪手动重试恢复 ⑫重试超限拒绝 ⑬job 自动重试+超时自动审批 ⑭cron 空值跳过 ⑮进度投影 ⑯RPC 冒烟。
 *
 * <p>断言式测试 + 空 autotest.yaml 标记（镜像 R1.65/R1.68/R1.70 范式——TK 编号/审计时间含真实时钟，
 * 录制表快照会随日期漂移翻红）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsCatalogFulfillmentEngine extends JunitAutoTestCase {

    static final String CUSTOMER_ID = "9401";
    static final String TICKET_TYPE_ID = "9402";
    static final String CS_TEAM_ID = "9403";
    static final String TEAM_CODE = "TEAM-FULFILL";
    static final String POLICY_TEAM_ID = "9404";
    // bridge-test-113: crm 未迁移（M3.4）Long 实体侧局部桥（crm seed 保持 Long，退役 owner M3.4）
    static final Long CRM_TEAM_ID = 9405L;
    static final String USER_A = "cs-fulfill-user-a";
    static final String USER_B = "cs-fulfill-user-b";
    static final String CS_SUPERVISOR = "cs-fulfill-supervisor";
    static final String CS_AGENT = "cs-fulfill-agent";

    static final String CATALOG_ITEM_ID = "9410";
    static final String FULFILLMENT_ID_BASE = "9420";
    static final String TICKET_ID_BASE = "9450";
    static final String TEMPLATE_ID_BASE = "9301";
    static final String TEMPLATE_ID_BASE_1 = "9302";
    static final String TEMPLATE_ID_BASE_2 = "9303";
    static final String TEMPLATE_ID_BASE_3 = "9304";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    app.erp.cs.service.job.ErpCsFulfillmentRetryJob fulfillmentRetryJob;

    // ---------- ① 物化幂等：重复 executeFulfillmentSteps 复用行不重复 ----------

    @Test
    public void test01MaterializeIdempotent() {
        seedBase();
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET, null);
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9451", "TK-FUL-IDEM", null, null);

        ApiResponse<?> r1 = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, r1.getStatus(), "首次执行应成功: " + r1);

        assertEquals(2, stepsOf(ticketId).size(), "物化行数 = 模板数（2）");
        List<ErpCsTicketAction> audits1 = actionsOf(ticketId, ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET);

        ApiResponse<?> r2 = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, r2.getStatus(), "重复执行应成功: " + r2);

        assertEquals(2, stepsOf(ticketId).size(), "重复执行复用行不新增（UK 幂等）");
        assertEquals(audits1.size(), actionsOf(ticketId, ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET).size(),
                "DONE 步骤重跑不重复写审计");
        for (ErpCsTicketFulfillmentStep step : stepsOf(ticketId)) {
            assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, step.getStatus(),
                    "两步均 DONE（NOTIFY 模板缺失静默降级不阻断）");
        }
    }

    // ---------- ② ASSIGN_TEAM RR 真实分配 + NEW→ASSIGNED + ASSIGN 审计 ----------

    @Test
    public void test02AssignTeamRoundRobinRealAssignment() {
        seedBase();
        seedTeamChain();
        // 尾随 NOTIFY 步骤：ensureInProgress 仅在末步前触发——ASSIGN(seq1) 执行时工单仍 NEW，
        // NEW→ASSIGNED 迁移可达（单步 ASSIGN 链会被末步前铺底先推至 IN_PROGRESS，非本用例语义）
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_TEAM,
                "{\"mode\":\"ROUND_ROBIN\"}");
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9452", "TK-FUL-ASSIGN", POLICY_TEAM_ID, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "执行应成功: " + resp);

        ErpCsTicket t = reload(ticketId);
        assertEquals(USER_A, t.getAssignedToId(), "RR 无历史取候选池首位 a");
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, t.getStatus(),
                "全链完成 → IN_PROGRESS（末步 NOTIFY 前铺底）");

        List<ErpCsTicketAction> audits = actionsOf(ticketId, ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_TEAM);
        assertEquals(1, audits.size(), "写 ASSIGN_TEAM 履行审计（真实分配，fromStatus/toStatus 携带迁移）");
        assertEquals(ErpCsConstants.TICKET_STATUS_NEW, audits.get(0).getFromStatus(), "审计 fromStatus=NEW");
        assertEquals(ErpCsConstants.TICKET_STATUS_ASSIGNED, audits.get(0).getToStatus(), "审计 toStatus=ASSIGNED");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketId, 1).getStatus(), "步骤 DONE");
    }

    // ---------- ③ REQUEST_APPROVAL → IN_PROGRESS + notify 审批人 → approve(true) DONE ----------

    @Test
    public void test03RequestApprovalThenApproveTrue() {
        seedBase();
        seedRoleWithUser("role-fulfill-approver", "客服主管", CS_SUPERVISOR);
        seedTemplate(TEMPLATE_ID_BASE, ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_APPROVAL_REQUEST,
                "ROLE", "{\"roles\":[\"客服主管\"]}");
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL, null);
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9453", "TK-FUL-APPROVE", null, null);

        ApiResponse<?> r1 = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, r1.getStatus(), "执行应成功: " + r1);

        ErpCsTicketFulfillmentStep step1 = stepBySeq(ticketId, 1);
        ErpCsTicketFulfillmentStep step2 = stepBySeq(ticketId, 2);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS, step1.getStatus(), "审批步骤 IN_PROGRESS（等审批）");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_PENDING, step2.getStatus(), "链中断：后续保持 PENDING");
        assertNotNull(step1.getExecutedAt(), "executedAt 落库（超时判定基准）");

        List<ErpSysNotification> approvals =
                notificationsOf(ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_APPROVAL_REQUEST);
        assertEquals(1, approvals.size(), "审批请求通知恰好 1 条");
        assertEquals(CS_SUPERVISOR, approvals.get(0).getRecipientUserId(), "接收人=审批人角色（客服主管）成员");

        // approve(true) → step DONE + 链恢复推进（末步前 ensureInProgress 铺底）
        ApiResponse<?> r2 = rpc(mutation, "ErpCsCatalogFulfillment__approveFulfillmentStep",
                Map.of("stepId", step1.getId(), "approved", true, "comment", "同意方案"));
        assertEquals(0, r2.getStatus(), "approve(true) 应成功: " + r2);

        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, reloadStep(step1.getId()).getStatus(), "审批通过步骤 DONE");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketId, 2).getStatus(), "链恢复推进至末步 DONE");
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reload(ticketId).getStatus(),
                "全链完成 → 工单 IN_PROGRESS");
    }

    // ---------- ④ 审批驳回 → FAILED + retryCount=max + lastError 含驳回意见 ----------

    @Test
    public void test04ApproveRejectTerminal() {
        seedBase();
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL, null);
        String ticketId = seedTicket("9454", "TK-FUL-REJECT", null, null);

        ApiResponse<?> r1 = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, r1.getStatus(), "执行应成功: " + r1);
        ErpCsTicketFulfillmentStep step1 = stepBySeq(ticketId, 1);

        ApiResponse<?> r2 = rpc(mutation, "ErpCsCatalogFulfillment__approveFulfillmentStep",
                Map.of("stepId", step1.getId(), "approved", false, "comment", "方案不通过，请补充材料"));
        assertEquals(0, r2.getStatus(), "approve(false) 应成功: " + r2);

        ErpCsTicketFulfillmentStep reloaded = reloadStep(step1.getId());
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, reloaded.getStatus(), "驳回 → FAILED");
        assertEquals(3, reloaded.getRetryCount().intValue(), "retryCount 置 max（默认 3，阻断自动重试链）");
        assertTrue(reloaded.getLastError().contains("审批驳回"), "lastError 含驳回标识");
        assertTrue(reloaded.getLastError().contains("方案不通过，请补充材料"), "lastError 含驳回意见");
    }

    // ---------- ⑤ NOTIFY_CUSTOMER notify 落库 ----------

    @Test
    public void test05NotifyCustomerDispatched() {
        seedBase();
        seedRoleWithUser("role-fulfill-agent", "客服员", CS_AGENT);
        seedTemplate(TEMPLATE_ID_BASE_1, ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_NOTIFY_CUSTOMER,
                "ROLE", "{\"roles\":[\"客服员\"]}");
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9455", "TK-FUL-NOTIFY", null, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "执行应成功: " + resp);

        List<ErpSysNotification> notices =
                notificationsOf(ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_NOTIFY_CUSTOMER);
        assertEquals(1, notices.size(), "客户通知恰好 1 条（7207 客户占位语境经客服员转达）");
        assertEquals(CS_AGENT, notices.get(0).getRecipientUserId(), "接收人=客服员角色成员");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketId, 1).getStatus(), "步骤 DONE");
    }

    // ---------- ⑥ UPDATE_STATUS：合法迁移 + 非法迁移 FAILED + 缺配置 FAILED ----------

    @Test
    public void test06UpdateStatusLegalIllegalAndMissingConfig() {
        seedBase();

        // a) 合法迁移：NEW→ASSIGNED（assign 边），链尾 NOTIFY 前铺底 IN_PROGRESS
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"ASSIGNED\"}");
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketA = seedTicket("9456", "TK-FUL-LEGAL", null, null);
        ApiResponse<?> ra = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketA));
        assertEquals(0, ra.getStatus(), "合法链应成功: " + ra);
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reload(ticketA).getStatus(),
                "全链完成 → IN_PROGRESS（末步 NOTIFY 前铺底）");
        ErpCsTicketAction audit = actionsOf(ticketA, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS).get(0);
        assertEquals(ErpCsConstants.TICKET_STATUS_NEW, audit.getFromStatus(), "UPDATE_STATUS 审计 fromStatus=NEW");
        assertEquals(ErpCsConstants.TICKET_STATUS_ASSIGNED, audit.getToStatus(), "UPDATE_STATUS 审计 toStatus=ASSIGNED");

        // b) 非法迁移：NEW→CLOSED（矩阵无边）→ FAILED + 链中断
        String itemB = "9411";
        seedCatalogItem(itemB);
        seedFulfillment("9423", itemB, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"CLOSED\"}");
        seedFulfillment("9424", itemB, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketB = seedTicket("9457", "TK-FUL-ILLEGAL", null, null);
        ApiResponse<?> rb = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", itemB, "ticketId", ticketB));
        assertEquals(0, rb.getStatus(), "mutation 本身成功（步骤级失败不抛出）: " + rb);
        ErpCsTicketFulfillmentStep stepB1 = stepBySeq(ticketB, 1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, stepB1.getStatus(), "非法迁移 → FAILED");
        assertTrue(stepB1.getLastError().contains("非法状态迁移"), "lastError 含非法迁移描述");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_PENDING, stepBySeq(ticketB, 2).getStatus(), "链中断：后续 PENDING");

        // c) 缺配置：UPDATE_STATUS 无 actionConfig → FAILED 配置错误
        String itemC = "9412";
        seedCatalogItem(itemC);
        seedFulfillment("9425", itemC, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS, null);
        String ticketC = seedTicket("9458", "TK-FUL-NOCFG", null, null);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", itemC, "ticketId", ticketC));
        ErpCsTicketFulfillmentStep stepC1 = stepBySeq(ticketC, 1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, stepC1.getStatus(), "缺配置 → FAILED");
        assertTrue(stepC1.getLastError().contains("配置错误"), "lastError 含配置错误描述");
    }

    // ---------- ⑦ CREATE_CHILD_TICKET 子单创建 + 双向弱指针 ----------

    @Test
    public void test07CreateChildTicketWithWeakLinks() {
        seedBase();
        seedCodeRule();
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_CREATE_CHILD_TICKET, null);
        String ticketId = seedTicket("9459", "TK-FUL-PARENT", null, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "执行应成功: " + resp);

        List<ErpCsTicket> children = daoProvider.daoFor(ErpCsTicket.class).findAllByQuery(likeSubject("[子工单] "));
        assertEquals(1, children.size(), "真实子工单已创建");
        ErpCsTicket child = children.get(0);
        assertEquals("parentTicketCode=TK-FUL-PARENT", child.getRemark(),
                "子单 remark 承载 parentTicketCode 弱指针");
        assertTrue(child.getCode().startsWith("TK"), "子单 code 走 TK codeRule: " + child.getCode());
        assertEquals(CUSTOMER_ID, child.getCustomerId(), "同客户");
        assertEquals(TICKET_TYPE_ID, child.getTicketTypeId(), "同工单类型");

        List<ErpCsTicketAction> audits = actionsOf(ticketId, ErpCsConstants.FULFILLMENT_ACTION_CREATE_CHILD_TICKET);
        assertEquals(1, audits.size(), "父单写 TicketAction 反向弱指针");
        assertTrue(audits.get(0).getContent().contains(child.getCode()), "审计 content 含子单编号");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketId, 1).getStatus(), "步骤 DONE");
    }

    // ---------- ⑧ 失败中断（step2 失败 → step3 保持 PENDING）+ 管理员通知落库 ----------

    @Test
    public void test08FailurePausesChainAndNotifiesAdmin() {
        seedBase();
        seedRoleWithUser("role-fulfill-supervisor", "客服主管", CS_SUPERVISOR);
        seedTemplate(TEMPLATE_ID_BASE_2, ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_STEP_FAILED,
                "ROLE", "{\"roles\":[\"客服主管\"]}");
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"CLOSED\"}");
        seedFulfillment("9423", CATALOG_ITEM_ID, 3, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9460", "TK-FUL-PAUSE", null, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "执行应成功: " + resp);

        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketId, 1).getStatus(), "step1 DONE");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, stepBySeq(ticketId, 2).getStatus(), "step2 FAILED");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_PENDING, stepBySeq(ticketId, 3).getStatus(),
                "失败中断：step3 保持 PENDING（暂停流程）");
        assertNotNull(stepBySeq(ticketId, 2).getLastError(), "lastError 记录错误信息");
        assertNotNull(stepBySeq(ticketId, 2).getExecutedAt(), "executedAt 落库");

        List<ErpSysNotification> failures =
                notificationsOf(ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_STEP_FAILED);
        assertEquals(1, failures.size(), "管理员通知恰好 1 条（7206 ROLE 客服主管）");
        assertEquals(CS_SUPERVISOR, failures.get(0).getRecipientUserId(), "接收人=客服主管");
    }

    // ---------- ⑨ 全 DONE → IN_PROGRESS（无 ASSIGN 步骤链：NEW 自动指派路径） ----------

    @Test
    public void test09AllDoneAdvancesToInProgressWithAutoAssign() {
        seedBase();
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET, null);
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9461", "TK-FUL-ADVANCE", null, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "执行应成功: " + resp);

        ErpCsTicket t = reload(ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, t.getStatus(),
                "全部步骤完成 → 工单 IN_PROGRESS（L1 ④）");
        assertNotNull(t.getAssignedToId(), "无 ASSIGN 步骤链：ensureInProgress 自动指派当前操作员");
        assertNotNull(t.getStartDateTime(), "start 边记录开始处理时间");
        for (ErpCsTicketFulfillmentStep step : stepsOf(ticketId)) {
            assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, step.getStatus(), "两步均 DONE");
        }
    }

    // ---------- ⑩ 尾部 UPDATE_STATUS RESOLVED 组合（末步前铺底 → resolve 边） ----------

    @Test
    public void test10TailUpdateStatusResolvedCombo() {
        seedBase();
        seedTeamChain();
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_TEAM, null);
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"RESOLVED\"}");
        String ticketId = seedTicket("9462", "TK-FUL-RESOLVED", POLICY_TEAM_ID, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "执行应成功: " + resp);

        ErpCsTicket t = reload(ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_RESOLVED, t.getStatus(),
                "尾部 UPDATE_STATUS(RESOLVED) 组合：末步前铺底 IN_PROGRESS → resolve 边 RESOLVED（按配置 RESOLVED）");
        assertEquals(USER_A, t.getAssignedToId(), "ASSIGN 步骤已分配（RR 首位）");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketId, 2).getStatus(), "末步 DONE");

        // 幂等重跑：全 DONE 后不再二次推进，RESOLVED 同态迁移 no-op
        ApiResponse<?> again = rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(0, again.getStatus(), "重跑应成功: " + again);
        assertEquals(ErpCsConstants.TICKET_STATUS_RESOLVED, reload(ticketId).getStatus(), "已 RESOLVED 保持（幂等）");
    }

    // ---------- ⑪ 手动重试成功恢复（FAILED→刷新配置重执行→DONE→链推进；IN_PROGRESS 待审批步骤不被重执行） ----------

    @Test
    public void test11ManualRetryRecoversChain() {
        seedBase();
        // 票 A：UPDATE_STATUS 非法配置（NEW→CLOSED）失败 → 修正模板 → 手动重试恢复
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"CLOSED\"}");
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketA = seedTicket("9463", "TK-FUL-RETRY", null, null);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketA));
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, stepBySeq(ticketA, 1).getStatus(), "前置：step1 FAILED");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_PENDING, stepBySeq(ticketA, 2).getStatus(), "前置：step2 PENDING");

        // 修正模板 actionConfig（CLOSED → ASSIGNED）：重试应刷新读取模板配置（修正即生效）
        updateTemplateConfig("9421", "{\"status\":\"ASSIGNED\"}");

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__retryFulfillment",
                Map.of("ticketId", ticketA));
        assertEquals(0, resp.getStatus(), "手动重试应成功: " + resp);

        ErpCsTicketFulfillmentStep stepA1 = reloadStep(stepBySeq(ticketA, 1).getId());
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepA1.getStatus(), "刷新配置重执行后 step1 DONE");
        assertEquals(1, stepA1.getRetryCount().intValue(), "retryCount 递增 1");
        assertEquals("{\"status\":\"ASSIGNED\"}", stepA1.getActionConfig(), "actionConfig 已刷新为模板新值");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketA, 2).getStatus(), "链恢复推进至末步 DONE");
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reload(ticketA).getStatus(),
                "全链完成 → 工单 IN_PROGRESS");

        // 票 B：REQUEST_APPROVAL IN_PROGRESS（待审批）步骤不被重试重执行（审批请求审计不重复）
        String itemB = "9417";
        seedCatalogItem(itemB);
        seedFulfillment("9423", itemB, 1, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL, null);
        seedFulfillment("9424", itemB, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketB = seedTicket("9464", "TK-FUL-NORETRY", null, null);
        seedTicketOnItem(ticketB, itemB);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", itemB, "ticketId", ticketB));
        ErpCsTicketFulfillmentStep stepB1 = stepBySeq(ticketB, 1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS, stepB1.getStatus(), "前置：step1 待审批");
        int auditsBefore = actionsOf(ticketB, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL).size();

        ApiResponse<?> respB = rpc(mutation, "ErpCsCatalogFulfillment__retryFulfillment",
                Map.of("ticketId", ticketB));
        assertEquals(0, respB.getStatus(), "无 FAILED 步骤重试应成功（no-op）: " + respB);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS, reloadStep(stepB1.getId()).getStatus(),
                "IN_PROGRESS 待审批步骤不被重执行");
        assertEquals(auditsBefore, actionsOf(ticketB, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL).size(),
                "审批请求审计不重复写");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_PENDING, stepBySeq(ticketB, 2).getStatus(),
                "后续步骤保持 PENDING（等审批）");
    }

    // ---------- ⑫ 重试计数达上限 → 拒绝 + 管理员通知 ----------

    @Test
    public void test12RetryExceededRejectedAndNotifiesAdmin() {
        seedBase();
        seedRoleWithUser("role-fulfill-supervisor", "客服主管", CS_SUPERVISOR);
        seedTemplate(TEMPLATE_ID_BASE_3, ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_STEP_FAILED,
                "ROLE", "{\"roles\":[\"客服主管\"]}");
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"CLOSED\"}");
        String ticketId = seedTicket("9465", "TK-FUL-MAXRETRY", null, null);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        ErpCsTicketFulfillmentStep step1 = stepBySeq(ticketId, 1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, step1.getStatus(), "前置：step1 FAILED");

        // 重试计数已达上限（默认 3）→ 拒绝 + 通知管理员人工介入
        String failedStepId = step1.getId();
        ormTemplate.runInSession(() -> {
            ErpCsTicketFulfillmentStep s = daoProvider.daoFor(ErpCsTicketFulfillmentStep.class)
                    .getEntityById(failedStepId);
            s.setRetryCount(3);
        });

        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogFulfillment__retryFulfillment",
                Map.of("ticketId", ticketId));
        assertEquals(ErpCsErrors.ERR_CS_FULFILLMENT_RETRY_EXCEEDED.getErrorCode(), resp.getCode(),
                "重试超限应拒绝（ERR_CS_FULFILLMENT_RETRY_EXCEEDED）");

        ErpCsTicketFulfillmentStep reloaded = reloadStep(step1.getId());
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, reloaded.getStatus(), "超限终态 FAILED 保留");
        assertEquals(3, reloaded.getRetryCount().intValue(), "retryCount 不再递增");
        List<ErpSysNotification> failures =
                notificationsOf(ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_STEP_FAILED);
        assertEquals(2, failures.size(), "管理员通知 2 条（初始失败 1 + 超限拒绝 1，7206 ROLE 客服主管）");
        assertTrue(failures.stream().allMatch(n -> CS_SUPERVISOR.equals(n.getRecipientUserId())),
                "接收人均为客服主管");
    }

    // ---------- ⑬ job 自动重试 + 超时自动审批（timeoutHours 边界）+ 驳回步骤不被自动重试 ----------

    @Test
    public void test13JobAutoRetryAndTimeoutAutoApprove() {
        seedBase();

        // 票 A：REQUEST_APPROVAL 超时（timeoutHours=1，executedAt 回拨 2h）→ 自动审批 + 链恢复
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL,
                "{\"timeoutHours\":1}");
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketA = seedTicket("9466", "TK-FUL-TIMEOUT", null, null);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketA));
        ErpCsTicketFulfillmentStep stepA1 = stepBySeq(ticketA, 1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS, stepA1.getStatus(), "前置：票 A 待审批");
        ormTemplate.runInSession(() -> {
            ErpCsTicketFulfillmentStep s = daoProvider.daoFor(ErpCsTicketFulfillmentStep.class)
                    .getEntityById(stepA1.getId());
            s.setExecutedAt(new Timestamp(CoreMetrics.currentTimeMillis() - 2 * 3600_000L));
        });

        // 票 B：REQUEST_APPROVAL 未超时（executedAt 即时）→ 不自动审批
        String itemB = "9413";
        seedCatalogItem(itemB);
        seedFulfillment("9425", itemB, 1, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL,
                "{\"timeoutHours\":1}");
        String ticketB = seedTicket("9467", "TK-FUL-WITHIN", null, null);
        seedTicketOnItem(ticketB, itemB);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", itemB, "ticketId", ticketB));
        ErpCsTicketFulfillmentStep stepB1 = stepBySeq(ticketB, 1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS, stepB1.getStatus(), "前置：票 B 待审批");

        // 票 C：FAILED 未超限 → job 自动重试（刷新模板配置）恢复
        String itemC = "9414";
        seedCatalogItem(itemC);
        seedFulfillment("9426", itemC, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"CLOSED\"}");
        seedFulfillment("9427", itemC, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketC = seedTicket("9468", "TK-FUL-JOBRETRY", null, null);
        seedTicketOnItem(ticketC, itemC);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", itemC, "ticketId", ticketC));
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, stepBySeq(ticketC, 1).getStatus(), "前置：票 C FAILED");
        updateTemplateConfig("9426", "{\"status\":\"ASSIGNED\"}");

        // 票 D：审批驳回终局（retryCount=max）→ 不被自动重试
        String itemD = "9415";
        seedCatalogItem(itemD);
        seedFulfillment("9428", itemD, 1, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL, null);
        String ticketD = seedTicket("9469", "TK-FUL-REJECTED", null, null);
        seedTicketOnItem(ticketD, itemD);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", itemD, "ticketId", ticketD));
        ErpCsTicketFulfillmentStep stepD1 = stepBySeq(ticketD, 1);
        rpc(mutation, "ErpCsCatalogFulfillment__approveFulfillmentStep",
                Map.of("stepId", stepD1.getId(), "approved", false, "comment", "方案否决"));
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, reloadStep(stepD1.getId()).getStatus(),
                "前置：票 D 驳回终局 FAILED");

        withFulfillmentRetryCron(fulfillmentRetryJob::execute);

        // 票 A：超时自动审批 + 链恢复推进
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, reloadStep(stepA1.getId()).getStatus(),
                "超时（2h > timeoutHours=1）自动审批 → DONE");
        assertTrue(actionsOf(ticketA, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL).stream()
                        .anyMatch(a -> a.getContent() != null && a.getContent().contains("超时自动审批")),
                "审计含「超时自动审批」");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketA, 2).getStatus(), "票 A 链恢复至末步 DONE");
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reload(ticketA).getStatus(), "票 A 工单 IN_PROGRESS");
        // 票 B：未超时不动
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS, reloadStep(stepB1.getId()).getStatus(),
                "未超时（timeoutHours 窗口内）不自动审批");
        // 票 C：自动重试恢复
        ErpCsTicketFulfillmentStep stepC1 = stepBySeq(ticketC, 1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepC1.getStatus(), "job 自动重试恢复 → DONE");
        assertEquals(1, stepC1.getRetryCount().intValue(), "retryCount 递增 1");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, stepBySeq(ticketC, 2).getStatus(), "票 C 链恢复至末步 DONE");
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reload(ticketC).getStatus(), "票 C 工单 IN_PROGRESS");
        // 票 D：驳回终局不被自动重试
        ErpCsTicketFulfillmentStep reloadedD = reloadStep(stepD1.getId());
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, reloadedD.getStatus(), "驳回终局 FAILED 保留");
        assertEquals(3, reloadedD.getRetryCount().intValue(), "驳回 retryCount=max 不变（不被自动重试）");
    }

    // ---------- ⑭ cron 空值跳过 ----------

    @Test
    public void test14CronEmptySkipsExecution() {
        seedBase();
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"CLOSED\"}");
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9470", "TK-FUL-CRONSKIP", null, null);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, stepBySeq(ticketId, 1).getStatus(), "前置：step1 FAILED");

        fulfillmentRetryJob.execute(); // cron 未配置（默认空）= 「不调度」语义

        assertEquals(ErpCsConstants.FULFILLMENT_STEP_FAILED, stepBySeq(ticketId, 1).getStatus(),
                "cron 空值应跳过扫描（状态不变）");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_PENDING, stepBySeq(ticketId, 2).getStatus(),
                "cron 空值后续步骤保持 PENDING");
        assertEquals(0, stepBySeq(ticketId, 1).getRetryCount().intValue(), "cron 空值 retryCount 不变");
    }

    // ---------- ⑮ findFulfillmentProgress 投影 ----------

    @Test
    public void test15FindFulfillmentProgressProjection() {
        seedBase();
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET, null);
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL, null);
        seedFulfillment("9423", CATALOG_ITEM_ID, 3, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketId = seedTicket("9471", "TK-FUL-PROGRESS", null, null);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketId));

        ApiResponse<?> resp = rpc(query, "ErpCsCatalogFulfillment__findFulfillmentProgress",
                Map.of("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "进度查询应成功: " + resp);
        List<?> rows = (List<?>) resp.getData();
        assertEquals(3, rows.size(), "投影行数 = 物化步骤数");
        Map<?, ?> row0 = (Map<?, ?>) rows.get(0);
        assertEquals(1, ((Number) row0.get("sequence")).intValue(), "sequence 升序排列");
        assertEquals(ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET, row0.get("actionType"));
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, row0.get("status"));
        Map<?, ?> row1 = (Map<?, ?>) rows.get(1);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS, row1.get("status"), "审批步骤 IN_PROGRESS");
        assertNotNull(row1.get("executedAt"), "投影含 executedAt（超时判定基准）");
        Map<?, ?> row2 = (Map<?, ?>) rows.get(2);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_PENDING, row2.get("status"), "中断后续 PENDING");
        for (String key : new String[]{"stepId", "sequence", "actionType", "status", "retryCount",
                "lastError", "executedAt", "executedBy"}) {
            assertTrue(row0.containsKey(key), "投影含字段 " + key);
        }
    }

    // ---------- ⑯ GraphQL RPC 冒烟（retryFulfillment/approveFulfillmentStep） ----------

    @Test
    public void test16RpcSmokeRetryAndApprove() {
        seedBase();
        // 票 A：approveFulfillmentStep RPC 冒烟
        seedFulfillment("9421", CATALOG_ITEM_ID, 1, ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL, null);
        seedFulfillment("9422", CATALOG_ITEM_ID, 2, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER, null);
        String ticketA = seedTicket("9472", "TK-FUL-SMOKE-APPROVE", null, null);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", CATALOG_ITEM_ID, "ticketId", ticketA));
        ApiResponse<?> approve = rpc(mutation, "ErpCsCatalogFulfillment__approveFulfillmentStep",
                Map.of("stepId", stepBySeq(ticketA, 1).getId(), "approved", true, "comment", "RPC 冒烟"));
        assertEquals(0, approve.getStatus(), "approveFulfillmentStep RPC 冒烟: " + approve);
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE, ((Map<?, ?>) approve.getData()).get("status"),
                "响应体携带步骤终态字段");

        // 票 B：retryFulfillment RPC 冒烟（修正配置后恢复）——单步链铺底后工单 IN_PROGRESS，
        // 修正目标取 RESOLVED（resolve 边 IN_PROGRESS→RESOLVED 可达；ASSIGNED 自 IN_PROGRESS 无边不可达）
        String itemB = "9416";
        seedCatalogItem(itemB);
        seedFulfillment("9424", itemB, 1, ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS,
                "{\"status\":\"CLOSED\"}");
        String ticketB = seedTicket("9473", "TK-FUL-SMOKE-RETRY", null, null);
        seedTicketOnItem(ticketB, itemB);
        rpc(mutation, "ErpCsCatalogFulfillment__executeFulfillmentSteps",
                Map.of("catalogItemId", itemB, "ticketId", ticketB));
        updateTemplateConfig("9424", "{\"status\":\"RESOLVED\"}");
        ApiResponse<?> retry = rpc(mutation, "ErpCsCatalogFulfillment__retryFulfillment",
                Map.of("ticketId", ticketB));
        assertEquals(0, retry.getStatus(), "retryFulfillment RPC 冒烟: " + retry);
        List<?> retried = (List<?>) retry.getData();
        assertEquals(1, retried.size(), "响应体 = 步骤执行行列表");
        assertEquals(ErpCsConstants.FULFILLMENT_STEP_DONE,
                ((Map<?, ?>) retried.get(0)).get("status"), "响应体步骤状态 DONE");
        assertEquals(ErpCsConstants.TICKET_STATUS_RESOLVED, reload(ticketB).getStatus(),
                "重试后经 resolve 边达成 RESOLVED");
    }

    // ---------- helpers ----------

    /** job cron 门控注入（finally 复位空值，避免跨方法污染）。 */
    private void withFulfillmentRetryCron(Runnable action) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_FULFILLMENT_RETRY_CRON, "0 0/5 * * * ?");
        try {
            action.run();
        } finally {
            io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_FULFILLMENT_RETRY_CRON, "");
        }
    }

    /** 修正模板 actionConfig（重试路径「刷新读取模板配置，修正即生效」的驱动器）。 */
    private void updateTemplateConfig(String templateId, String actionConfig) {
        ormTemplate.runInSession(() -> {
            ErpCsCatalogFulfillment template =
                    daoProvider.daoFor(ErpCsCatalogFulfillment.class).getEntityById(templateId);
            template.setActionConfig(actionConfig);
        });
    }

    /** 绑定非默认目录项的工单种子修正（默认 {@link #seedTicket} 固定 CATALOG_ITEM_ID）。 */
    private void seedTicketOnItem(String ticketId, String catalogItemId) {
        ormTemplate.runInSession(() -> {
            ErpCsTicket t = daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
            t.setCatalogItemId(catalogItemId);
        });
    }

    private ErpCsTicket reload(String id) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(id);
    }

    private ErpCsTicketFulfillmentStep reloadStep(String id) {
        return daoProvider.daoFor(ErpCsTicketFulfillmentStep.class).getEntityById(id);
    }

    private List<ErpCsTicketFulfillmentStep> stepsOf(String ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        return daoProvider.daoFor(ErpCsTicketFulfillmentStep.class).findAllByQuery(q);
    }

    private ErpCsTicketFulfillmentStep stepBySeq(String ticketId, int sequence) {
        for (ErpCsTicketFulfillmentStep s : stepsOf(ticketId)) {
            if (s.getSequence() != null && s.getSequence() == sequence) {
                return s;
            }
        }
        return null;
    }

    private List<ErpCsTicketAction> actionsOf(String ticketId, String actionType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", actionType));
        return daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
    }

    private List<ErpSysNotification> notificationsOf(String notificationType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", notificationType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private static QueryBean likeSubject(String prefix) {
        QueryBean q = new QueryBean();
        // FilterBeans.like 不自动包通配符，须显式 % 前后缀（H2/MySQL 字面 [ ] 无特殊义）
        q.addFilter(io.nop.api.core.beans.FilterBeans.like("subject", "%" + prefix + "%"));
        return q;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seeds ----------

    private void seedBase() {
        seedCustomer(CUSTOMER_ID, "履行引擎客户");
        seedTicketType(TICKET_TYPE_ID);
        seedSlaPolicyTeam(POLICY_TEAM_ID, TICKET_TYPE_ID, CS_TEAM_ID);
        seedCsTeam(CS_TEAM_ID, TEAM_CODE);
        seedCatalogItem(CATALOG_ITEM_ID);
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

    private void seedTicketType(String id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicketType> dao = daoProvider.daoFor(ErpCsTicketType.class);
            ErpCsTicketType t = new ErpCsTicketType();
            t.orm_propValueByName("id", id);
            t.setCode("TT-" + id);
            t.setName("履行测试工单类型-" + id);
            t.setDefaultPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
            dao.saveEntity(t);
        });
    }

    private void seedSlaPolicyTeam(String id, String ticketTypeId, String teamId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsSlaPolicy> dao = daoProvider.daoFor(ErpCsSlaPolicy.class);
            ErpCsSlaPolicy p = new ErpCsSlaPolicy();
            p.orm_propValueByName("id", id);
            p.setCode("SLA-FUL-" + id);
            p.setName("履行测试团队策略");
            p.setTicketTypeId(ticketTypeId);
            p.setTeamId(teamId);
            p.setResolveHours(8);
            p.setIsWorkingDays(false);
            dao.saveEntity(p);
        });
    }

    private void seedCsTeam(String id, String code) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTeam> dao = daoProvider.daoFor(ErpCsTeam.class);
            ErpCsTeam team = new ErpCsTeam();
            team.orm_propValueByName("id", id);
            team.setCode(code);
            team.setName("履行测试客服团队");
            dao.saveEntity(team);
        });
    }

    /** 同码 crm 团队 + 两名成员（a→b，RR 无历史取首位 a）。 */
    private void seedTeamChain() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCrmTeam> teamDao = daoProvider.daoFor(ErpCrmTeam.class);
            ErpCrmTeam team = new ErpCrmTeam();
            team.orm_propValueByName("id", CRM_TEAM_ID);
            team.setCode(TEAM_CODE);
            team.setName("履行测试CRM团队");
            teamDao.saveEntity(team);

            IEntityDao<ErpCrmTeamMember> memberDao = daoProvider.daoFor(ErpCrmTeamMember.class);
            seedMember(memberDao, 9406L, CRM_TEAM_ID, USER_A);
            seedMember(memberDao, 9407L, CRM_TEAM_ID, USER_B);
        });
    }

    private void seedMember(IEntityDao<ErpCrmTeamMember> dao, Long id, Long teamId, String userId) {
        ErpCrmTeamMember m = new ErpCrmTeamMember();
        m.orm_propValueByName("id", id);
        m.setTeamId(teamId);
        m.setUserId(userId);
        dao.saveEntity(m);
    }

    private void seedCatalogItem(String id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsServiceCatalogItem> dao = daoProvider.daoFor(ErpCsServiceCatalogItem.class);
            ErpCsServiceCatalogItem item = new ErpCsServiceCatalogItem();
            item.orm_propValueByName("id", id);
            item.setCode("ITEM-" + id);
            item.setName("履行测试目录项-" + id);
            item.setTicketTypeId(TICKET_TYPE_ID);
            item.setIsActive(Boolean.TRUE);
            item.setIsPublic(Boolean.TRUE);
            dao.saveEntity(item);
        });
    }

    private void seedFulfillment(String id, String catalogItemId, int sequence, String actionType, String actionConfig) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsCatalogFulfillment> dao = daoProvider.daoFor(ErpCsCatalogFulfillment.class);
            ErpCsCatalogFulfillment f = new ErpCsCatalogFulfillment();
            f.orm_propValueByName("id", id);
            f.setCode("FUL-" + id);
            f.setCatalogItemId(catalogItemId);
            f.setSequence(sequence);
            f.setActionType(actionType);
            f.setActionConfig(actionConfig);
            f.setIsMandatory(false);
            dao.saveEntity(f);
        });
    }

    /** dao 直插工单（不走 save 管道，隔离富化干扰；status 默认 NEW）。 */
    private String seedTicket(String id, String code, String slaPolicyId, String assignedToId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(CUSTOMER_ID);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
            t.setStatus(ErpCsConstants.TICKET_STATUS_NEW);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            t.setSlaPolicyId(slaPolicyId);
            t.setAssignedToId(assignedToId);
            t.setCatalogItemId(CATALOG_ITEM_ID);
            t.setBusinessDate(LocalDate.of(2026, 8, 1));
            dao.saveEntity(t);
        });
        return id;
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
            rule.setCreateTime(new Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            rule.setUpdatedBy("system");
            rule.setUpdateTime(new Timestamp(io.nop.api.core.time.CoreMetrics.currentTimeMillis()));
            dao.saveEntity(rule);
        });
    }

    private void seedTemplate(String id, String notificationType, String resolver, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(notificationType);
            t.setName("履行测试-" + notificationType);
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("履行通知: ${ticketCode}");
            t.setBodyTpl("工单 ${ticketCode} 履行事件 ${notificationType}");
            t.setRecipientResolver(resolver);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy("NONE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    /** 角色角色名精确匹配 notify ROLE 解析（镜像 TestErpCsTicketCreateEnrichment.seedSupervisorRole）。 */
    private void seedRoleWithUser(String roleId, String roleName, String userId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<NopAuthUser> userDao = daoProvider.daoFor(NopAuthUser.class);
            NopAuthUser user = new NopAuthUser();
            user.setUserId(userId);
            user.setUserName(userId);
            user.setNickName(userId);
            user.setPassword("dummy-pwd");
            user.setOpenId(userId);
            user.orm_propValueByName("gender", 1);
            user.orm_propValueByName("userType", 0);
            user.orm_propValueByName("status", 1);
            user.orm_propValueByName("delFlag", 0);
            user.orm_propValueByName("tenantId", "0");
            userDao.saveEntity(user);

            IEntityDao<NopAuthRole> roleDao = daoProvider.daoFor(NopAuthRole.class);
            NopAuthRole role = new NopAuthRole();
            role.setRoleId(roleId);
            role.setRoleName(roleName);
            roleDao.saveEntity(role);

            IEntityDao<NopAuthUserRole> urDao = daoProvider.daoFor(NopAuthUserRole.class);
            NopAuthUserRole ur = new NopAuthUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            urDao.saveEntity(ur);
        });
    }
}
