package app.erp.cs.service.processor;

import app.erp.cs.biz.IErpCsTicketActionBiz;
import app.erp.cs.biz.IErpCsTicketBiz;
import app.erp.cs.biz.IErpCsTicketFulfillmentStepBiz;
import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTeam;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.cs.dao.entity.ErpCsTicketFulfillmentStep;
import app.erp.cs.dao.entity.ErpCsTicketType;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import app.erp.cs.service.ErpCsErrors;
import app.erp.cs.service.entity.TicketAssignResolver;
import app.erp.cs.service.statemachine.ErpCsTicketStateMachine;
import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.txn.TransactionPropagation;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.dao.txn.ITransactionTemplate;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * ErpCsCatalogFulfillment executeFulfillmentSteps per-mutation Processor（RC-R1.71 实化，
 * {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 *
 * <p>UC-CS-12 ②③④+后置+异常（L1 {@code use-cases.md:227-243}）：
 * <ul>
 *   <li>②actionConfig 驱动五动作：ASSIGN_TEAM/ASSIGN_AGENT（mode→{@link TicketAssignResolver} 真实分配）、
 *       REQUEST_APPROVAL（cs-local 轻量审批：step IN_PROGRESS + notify 审批人 + 超时自动审批）、
 *       CREATE_CHILD_TICKET（经 IErpCsTicketBiz.save 真实子工单 + 双向弱指针）、NOTIFY_CUSTOMER（notify 派发）、
 *       UPDATE_STATUS（状态机守卫真实迁移，同态幂等 DONE no-op）。CREATE_TICKET 保留 DONE 审计；INVOKE_WORKFLOW/
 *       CLOSE_TICKET 为 L1 未枚举边界（SKIPPED / 审计 DONE 占位，arm-index P1-RC-061 done 注记防重开）。</li>
 *   <li>③失败暂停：step FAILED + lastError + 中断（后续保持 PENDING）+ 管理员通知（cs.fulfillment-step-failed 7206）。</li>
 *   <li>④终态推进：末步执行前 ensureInProgress 铺底（NEW 自动指派→assign 边→ASSIGNED→start 边→IN_PROGRESS）；
 *       「按配置 RESOLVED」经尾部 UPDATE_STATUS(status=RESOLVED) 步骤组合达成。</li>
 *   <li>后置+异常：{@link ErpCsTicketFulfillmentStep} per-ticket 执行行（UK(ticketId, fulfillmentId) 幂等物化）
 *       承载「状态可跟踪，异常可重试」；retryFulfillment 手动重试（refresh 模板 actionConfig）+ job 自动重试
 *       （FAILED retryCount&lt;max）+ REQUEST_APPROVAL 超时自动审批；超限通知管理员人工介入。</li>
 * </ul>
 *
 * <p>下游可经 Delta beans.xml 同名 bean id 覆盖本类（protected step 方法为产品化扩展点）。
 */
public class ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCsCatalogFulfillmentExecuteFulfillmentStepsProcessor.class);

    static final int LAST_ERROR_MAX_LENGTH = 500;
    static final int SCAN_LIMIT = 200;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ITransactionTemplate transactionTemplate;

    @Inject
    IErpCsTicketBiz ticketBiz;
    @Inject
    IErpCsTicketActionBiz ticketActionBiz;
    @Inject
    IErpCsTicketFulfillmentStepBiz stepBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IErpMdPartnerBiz mdPartnerBiz;
    @Inject
    TicketAssignResolver ticketAssignResolver;
    @Inject
    ErpCsTicketStateMachine stateMachine;

    // ---------- 主入口：物化 + 链推进 ----------

    public List<ErpCsTicketFulfillmentStep> executeFulfillmentSteps(String catalogItemId, String ticketId,
                                                                    IServiceContext context) {
        if (catalogItemId == null || ticketId == null) {
            return new ArrayList<>();
        }
        List<ErpCsCatalogFulfillment> templates = loadTemplatesByCatalogItem(catalogItemId);
        if (templates.isEmpty()) {
            return new ArrayList<>();
        }
        ErpCsTicket ticket = ticketBiz.get(String.valueOf(ticketId), true, context);
        if (ticket == null) {
            return new ArrayList<>();
        }
        List<ErpCsTicketFulfillmentStep> steps = materializeSteps(templates, ticketId, context);
        runChain(ticket, steps, context);
        return steps;
    }

    // 步骤行状态写入统一走 ORM session 脏字段跟踪（saveEntity/findList 后实体即挂载当前 session，
    // setter 触发 dirty 标记，事务提交时统一 flush）：CrudBizModel.updateEntity 仅接受已从数据库加载的
    // MANAGED 实体，同事务内 materializeStep 刚 saveEntity（未 flush 的 pending-insert）实体会被
    // nop.err.orm.dao.update-entity-not-managed 拒绝（ErpCsTicketBizModel.doSaveEntity flush 先例同理）。

    /**
     * 物化执行行：per (ticketId, fulfillmentId) 存在即复用（UK 幂等），首建写入模板快照
     * （actionType/actionConfig/sequence/catalogItemId）。重试路径的 actionConfig 刷新见 {@link #retryFulfillment}。
     */
    protected List<ErpCsTicketFulfillmentStep> materializeSteps(List<ErpCsCatalogFulfillment> templates,
                                                                String ticketId, IServiceContext context) {
        List<ErpCsTicketFulfillmentStep> steps = new ArrayList<>();
        for (ErpCsCatalogFulfillment template : templates) {
            steps.add(materializeStep(template, ticketId, context));
        }
        steps.sort(Comparator.comparingInt(this::sequenceOfStep));
        return steps;
    }

    private ErpCsTicketFulfillmentStep materializeStep(ErpCsCatalogFulfillment template, String ticketId,
                                                       IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("fulfillmentId", template.getId()));
        q.setLimit(1);
        List<ErpCsTicketFulfillmentStep> existing = stepBiz.findList(q, null, context);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        ErpCsTicketFulfillmentStep step = stepBiz.newEntity();
        step.setTicketId(ticketId);
        step.setFulfillmentId(template.getId());
        step.setCatalogItemId(template.getCatalogItemId());
        step.setSequence(template.getSequence());
        step.setActionType(template.getActionType());
        step.setActionConfig(template.getActionConfig());
        step.setStatus(ErpCsConstants.FULFILLMENT_STEP_PENDING);
        step.setRetryCount(0);
        stepBiz.saveEntity(step, null, context);
        return step;
    }

    /**
     * 链推进（②①④核心循环）：DONE/SKIPPED 跳过；FAILED/IN_PROGRESS（待审批）中断（后续保持 PENDING）；
     * 最后一个 PENDING 步骤执行前 {@link #ensureInProgress} 铺底（D4 时序——尾部 UPDATE_STATUS(RESOLVED)
     * 组合依赖末步前已 IN_PROGRESS）。
     */
    protected void runChain(ErpCsTicket ticket, List<ErpCsTicketFulfillmentStep> steps, IServiceContext context) {
        int lastPendingIndex = -1;
        for (int i = 0; i < steps.size(); i++) {
            if (ErpCsConstants.FULFILLMENT_STEP_PENDING.equals(steps.get(i).getStatus())) {
                lastPendingIndex = i;
            }
        }
        for (int i = 0; i < steps.size(); i++) {
            ErpCsTicketFulfillmentStep step = steps.get(i);
            String status = step.getStatus();
            if (ErpCsConstants.FULFILLMENT_STEP_DONE.equals(status)
                    || ErpCsConstants.FULFILLMENT_STEP_SKIPPED.equals(status)) {
                continue;
            }
            if (ErpCsConstants.FULFILLMENT_STEP_FAILED.equals(status)
                    || ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS.equals(status)) {
                break;
            }
            if (i == lastPendingIndex) {
                ensureInProgress(ticket, context);
            }
            String result;
            try {
                result = executeStep(step, ticket, context);
            } catch (Exception e) {
                markFailed(step, ticket, e.getMessage(), context);
                LOG.warn("fulfillment-step-failed: ticketId={}, stepId={}, actionType={}, reason={}",
                        ticket.getId(), step.getId(), step.getActionType(), e.getMessage());
                break;
            }
            if (ErpCsConstants.FULFILLMENT_RESULT_FAILED.equals(result)
                    || ErpCsConstants.FULFILLMENT_RESULT_IN_PROGRESS.equals(result)) {
                break;
            }
        }
    }

    /** 链恢复推进（审批通过/超时自动审批/重试成功后）：重载工单与步骤行续跑。 */
    public void continueChain(String ticketId, IServiceContext context) {
        ErpCsTicket ticket = ticketBiz.get(String.valueOf(ticketId), true, context);
        if (ticket == null) {
            return;
        }
        runChain(ticket, loadStepsByTicket(ticketId, context), context);
    }

    // ---------- 单步执行（actionConfig 驱动五动作实化） ----------

    /**
     * 执行单步履行。返回执行结果标识（DONE / SKIPPED / FAILED / IN_PROGRESS）。
     * protected 以允许下游覆盖（产品化扩展点）。
     */
    protected String executeStep(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, IServiceContext context) {
        String actionType = step.getActionType();
        if (actionType == null) {
            markSkipped(step, ticket, "SKIPPED: 未知 actionType(null)", context);
            return ErpCsConstants.FULFILLMENT_RESULT_SKIPPED;
        }
        Map<String, Object> config = parseActionConfig(step.getActionConfig());
        switch (actionType) {
            case ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET:
                // 工单已由 createFromCatalog 创建，保留 DONE 审计语义（主单已建）
                markDone(step, ticket, "DONE: 工单已建", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_TEAM:
            case ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_AGENT:
                return executeAssign(step, ticket, config, context);
            case ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL:
                return executeRequestApproval(step, ticket, config, context);
            case ErpCsConstants.FULFILLMENT_ACTION_CREATE_CHILD_TICKET:
                return executeCreateChildTicket(step, ticket, config, context);
            case ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER:
                return executeNotifyCustomer(step, ticket, config, context);
            case ErpCsConstants.FULFILLMENT_ACTION_UPDATE_STATUS:
                return executeUpdateStatus(step, ticket, config, context);
            case ErpCsConstants.FULFILLMENT_ACTION_CLOSE_TICKET:
                // L1 UC-CS-12 ② 未枚举值：维持审计 DONE 占位（arm-index P1-RC-061 done 注记边界）
                markDone(step, ticket, "DONE: 关闭动作已登记（L1 未枚举，审计占位）", context);
                return ErpCsConstants.FULFILLMENT_RESULT_DONE;
            case ErpCsConstants.FULFILLMENT_ACTION_INVOKE_WORKFLOW:
                // L1 UC-CS-12 ② 未枚举值：nop-workflow 集成 successor（plan Deferred 注记）
                markSkipped(step, ticket, "SKIPPED: INVOKE_WORKFLOW 归 successor（nop-workflow 集成）", context);
                return ErpCsConstants.FULFILLMENT_RESULT_SKIPPED;
            default:
                markSkipped(step, ticket, "SKIPPED: 未知 actionType " + actionType, context);
                return ErpCsConstants.FULFILLMENT_RESULT_SKIPPED;
        }
    }

    /**
     * ASSIGN_TEAM/ASSIGN_AGENT：config {mode: ROUND_ROBIN|LEAST_OPEN}（缺省 erp-cs.assign-method）→
     * R1.65 解析链（挂载策略 team → 类型默认策略 team）→ 同码 crm 团队成员池 → 纯函数挑人 →
     * assignedToId + ASSIGN 审计；状态迁移仅当 ticket 为 NEW（autoAssignOnCreate NEW-guard 同款守卫），
     * 非 NEW 幂等跳过迁移仅更新 assignedToId + 审计。池空 → FAILED（assignToRole 回退记入 lastError，
     * ROLE 告警路径由 7206 失败通知承载）。
     */
    protected String executeAssign(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket,
                                   Map<String, Object> config, IServiceContext context) {
        String mode = configValue(config, "mode");
        String method = StringHelper.isEmpty(mode) ? ErpCsConfigs.getAssignMethod() : mode;
        ErpCsSlaPolicy attached = ticket.getSlaPolicy();
        ErpCsSlaPolicy typeDefault = resolveTypeDefaultPolicy(ticket);
        ErpCsTeam team = TicketAssignResolver.resolveTeam(attached, typeDefault);
        List<String> pool = ticketAssignResolver.resolveCandidatePool(team, context);
        if (pool.isEmpty()) {
            markFailed(step, ticket, "分配失败：无可用处理人（候选池为空"
                    + roleNote(step) + "）", context);
            return ErpCsConstants.FULFILLMENT_RESULT_FAILED;
        }
        String assignee = ticketAssignResolver.pickAssignee(method, pool,
                findLastAssigned(pool, context), countOpenTickets(pool, context));
        if (assignee == null) {
            markFailed(step, ticket, "分配失败：算法未选出处理人（mode=" + method + "）", context);
            return ErpCsConstants.FULFILLMENT_RESULT_FAILED;
        }
        String from = ticket.getStatus();
        ticket.setAssignedToId(assignee);
        String toStatus = null;
        if (ErpCsConstants.TICKET_STATUS_NEW.equals(from)) {
            stateMachine.assertCanAssign(from);
            toStatus = stateMachine.assignTargetStatus();
            ticket.setStatus(toStatus);
        }
        ticketBiz.updateEntity(ticket, null, context);
        writeAudit(ticket, step.getActionType(), from, toStatus,
                "DONE: 履行步骤分配处理人 " + assignee + "（mode=" + method + "）", context);
        markStepDone(step, context);
        return ErpCsConstants.FULFILLMENT_RESULT_DONE;
    }

    /**
     * REQUEST_APPROVAL：config {approverRole?, timeoutHours?} → cs-local 轻量审批（否决 nop-workflow，plan D3）：
     * step IN_PROGRESS（等审批/超时）+ notify 审批人（ROLE approverRole 缺省客服主管）；审批经
     * {@link #approveFulfillmentStep}，超时自动审批经 {@link #autoApproveTimedOut}。
     */
    protected String executeRequestApproval(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket,
                                            Map<String, Object> config, IServiceContext context) {
        String approverRole = configValue(config, "approverRole");
        if (StringHelper.isEmpty(approverRole)) {
            approverRole = ErpCsConstants.FULFILLMENT_DEFAULT_APPROVER_ROLE;
        }
        step.setStatus(ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS);
        step.setExecutedAt(CoreMetrics.currentTimestamp());
        step.setExecutedBy(operatorId(ticket, context));
        writeAudit(ticket, step.getActionType(), null, null,
                "IN_PROGRESS: 审批请求已发起（审批人角色: " + approverRole + "）", context);
        notifyApprovalRequest(step, ticket, approverRole, context);
        return ErpCsConstants.FULFILLMENT_RESULT_IN_PROGRESS;
    }

    /**
     * CREATE_CHILD_TICKET：经 IErpCsTicketBiz.save（R1.31 data map 先例）创建真实子工单——
     * subject=[子工单] 前缀、同 customerId/ticketTypeId、remark 承载 parentTicketCode 弱指针、
     * code 走 TK codeRule；父单写 TicketAction（content=子工单已创建）。双向弱指针无 ORM 亲子列（Non-Goal）。
     */
    protected String executeCreateChildTicket(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket,
                                              Map<String, Object> config, IServiceContext context) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("subject", "[子工单] " + ticket.getSubject());
        data.put("customerId", ticket.getCustomerId());
        data.put("ticketTypeId", ticket.getTicketTypeId());
        data.put("priority", ticket.getPriority());
        data.put("status", ErpCsConstants.TICKET_STATUS_NEW);
        data.put("docStatus", ErpCsConstants.DOC_STATUS_DRAFT);
        data.put("approveStatus", ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
        data.put("remark", "parentTicketCode=" + ticket.getCode());
        if (ticket.getSource() != null) {
            data.put("source", ticket.getSource());
        }
        ErpCsTicket child = ticketBiz.save(data, context);
        writeAudit(ticket, step.getActionType(), null, null,
                "DONE: 子工单已创建: " + child.getCode(), context);
        markStepDone(step, context);
        return ErpCsConstants.FULFILLMENT_RESULT_DONE;
    }

    /**
     * NOTIFY_CUSTOMER：notify 派发（模板 7207 cs.fulfillment-notify-customer，客户占位语境经客服员转达），
     * ctx = {ticketCode, catalogItemName, stepRemark}。notify 失败静默降级（不阻断链）。
     */
    protected String executeNotifyCustomer(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket,
                                           Map<String, Object> config, IServiceContext context) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("ticketId", ticket.getId());
        ctx.put("ticketCode", ticket.getCode());
        ctx.put("catalogItemName", catalogItemName(ticket));
        ctx.put("stepRemark", step.getRemark());
        try {
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_NOTIFY_CUSTOMER, ctx, context);
        } catch (Exception e) {
            LOG.warn("fulfillment-notify-customer 派发失败（降级，链继续）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
        writeAudit(ticket, step.getActionType(), null, null, "DONE: 客户通知已派发", context);
        markStepDone(step, context);
        return ErpCsConstants.FULFILLMENT_RESULT_DONE;
    }

    /**
     * UPDATE_STATUS：config {status}（必填）——缺省/非法 JSON → FAILED 配置错误；
     * target == 当前 status → 幂等 DONE no-op（防尾部 RESOLVED 重试/铺底后同态迁移被守卫误判失败）；
     * 经状态机迁移矩阵守卫真实 setStatus（非法迁移 → FAILED）。
     */
    protected String executeUpdateStatus(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket,
                                         Map<String, Object> config, IServiceContext context) {
        if (isBlankConfig(step.getActionConfig())) {
            markFailed(step, ticket, "配置错误：UPDATE_STATUS 缺少 actionConfig（须含 status）", context);
            return ErpCsConstants.FULFILLMENT_RESULT_FAILED;
        }
        String target = configValue(config, "status");
        if (StringHelper.isEmpty(target)) {
            markFailed(step, ticket, "配置错误：UPDATE_STATUS actionConfig 缺少 status", context);
            return ErpCsConstants.FULFILLMENT_RESULT_FAILED;
        }
        String from = ticket.getStatus();
        if (target.equals(from)) {
            writeAudit(ticket, step.getActionType(), from, target, "DONE: 状态已为 " + target + "（幂等 no-op）", context);
            markStepDone(step, context);
            return ErpCsConstants.FULFILLMENT_RESULT_DONE;
        }
        String action = transitionAction(from, target);
        if (action == null) {
            markFailed(step, ticket, "非法状态迁移: " + from + " → " + target, context);
            return ErpCsConstants.FULFILLMENT_RESULT_FAILED;
        }
        ticket.setStatus(target);
        ticketBiz.updateEntity(ticket, null, context);
        writeAudit(ticket, step.getActionType(), from, target,
                "DONE: 状态迁移 " + from + " → " + target + "（" + action + " 边）", context);
        markStepDone(step, context);
        return ErpCsConstants.FULFILLMENT_RESULT_DONE;
    }

    // ---------- 审批 / 重试（UC-CS-12 异常「可重试，最多 3 次」） ----------

    /**
     * cs-local 轻量审批 mutation（D3）：IN_PROGRESS 守卫；approved=true → DONE + 审计 + 链恢复推进；
     * approved=false → FAILED + retryCount 置 max（人工决定终局语义：阻断自动重试链）+ lastError=「审批驳回: {comment}」。
     */
    public ErpCsTicketFulfillmentStep approveFulfillmentStep(String stepId, boolean approved, String comment,
                                                             IServiceContext context) {
        ErpCsTicketFulfillmentStep step = requireStep(stepId, context);
        if (!ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS.equals(step.getStatus())) {
            throw new NopException(ErpCsErrors.ERR_CS_FULFILLMENT_STEP_NOT_OPEN)
                    .param(ErpCsErrors.ARG_STEP_ID, stepId)
                    .param(ErpCsErrors.ARG_CURRENT_STATUS, step.getStatus());
        }
        ErpCsTicket ticket = ticketBiz.get(String.valueOf(step.getTicketId()), true, context);
        if (approved) {
            step.setStatus(ErpCsConstants.FULFILLMENT_STEP_DONE);
            touchExecuted(step, ticket, context);
            writeAudit(ticket, step.getActionType(), null, null,
                    "DONE: 审批通过" + (StringHelper.isEmpty(comment) ? "" : ": " + comment), context);
            continueChain(step.getTicketId(), context);
        } else {
            step.setStatus(ErpCsConstants.FULFILLMENT_STEP_FAILED);
            step.setRetryCount(ErpCsConfigs.getFulfillmentRetryMax());
            step.setLastError(truncate("审批驳回: " + (StringHelper.isEmpty(comment) ? "" : comment)));
            touchExecuted(step, ticket, context);
            writeAudit(ticket, step.getActionType(), null, null,
                    "FAILED: 审批驳回" + (StringHelper.isEmpty(comment) ? "" : ": " + comment)
                            + "（retryCount 置 max，阻断自动重试）", context);
        }
        return step;
    }

    /**
     * 手动重试（D5）：仅 FAILED 步骤（IN_PROGRESS 待审批步骤不重执行）；retryCount+1 后刷新读取模板
     * actionConfig（修正配置即生效；快照列保留最后执行配置作审计）再重执行；retryCount &gt;= max 拒绝
     * + notify 管理员人工介入。重试成功 → 链恢复推进。
     */
    public List<ErpCsTicketFulfillmentStep> retryFulfillment(String ticketId, IServiceContext context) {
        ErpCsTicket ticket = requireTicket(ticketId, context);
        List<ErpCsTicketFulfillmentStep> steps = loadStepsByTicket(ticketId, context);
        int retryMax = ErpCsConfigs.getFulfillmentRetryMax();
        for (ErpCsTicketFulfillmentStep step : steps) {
            if (!ErpCsConstants.FULFILLMENT_STEP_FAILED.equals(step.getStatus())) {
                continue;
            }
            if (step.getRetryCount() != null && step.getRetryCount() >= retryMax) {
                notifyStepFailedRequireNew(step, ticket, "重试次数已达上限（" + retryMax + "），须人工介入", context);
                throw new NopException(ErpCsErrors.ERR_CS_FULFILLMENT_RETRY_EXCEEDED)
                        .param(ErpCsErrors.ARG_STEP_ID, step.getId())
                        .param(ErpCsErrors.ARG_ACTION_TYPE, step.getActionType())
                        .param(ErpCsErrors.ARG_RETRY_MAX, retryMax);
            }
            if (retryOneFailedStep(step, ticket, context)) {
                break;
            }
        }
        runChain(ticket, loadStepsByTicket(ticketId, context), context);
        return loadStepsByTicket(ticketId, context);
    }

    /**
     * job 自动重试入口（D5：逐条隔离由 {@link ErpCsFulfillmentRetryJob} 保证）：与手动入口同语义但
     * <b>不抛出</b>——retryCount &gt;= max 的 FAILED 步骤（含审批驳回终局行）跳过保留终态并通知管理员，
     * 避免单张工单的超限步骤阻断批内其他工单的自动恢复。
     *
     * @return 本工单实际重试的步骤数
     */
    public int retryForJob(String ticketId, IServiceContext context) {
        ErpCsTicket ticket = ticketBiz.get(String.valueOf(ticketId), true, context);
        if (ticket == null) {
            return 0;
        }
        int retryMax = ErpCsConfigs.getFulfillmentRetryMax();
        int retried = 0;
        for (ErpCsTicketFulfillmentStep step : loadStepsByTicket(ticketId, context)) {
            if (!ErpCsConstants.FULFILLMENT_STEP_FAILED.equals(step.getStatus())) {
                continue;
            }
            if (step.getRetryCount() != null && step.getRetryCount() >= retryMax) {
                notifyStepFailed(step, ticket, "重试次数已达上限（" + retryMax + "），须人工介入", context);
                continue;
            }
            retried++;
            if (retryOneFailedStep(step, ticket, context)) {
                break;
            }
        }
        if (retried > 0) {
            runChain(ticket, loadStepsByTicket(ticketId, context), context);
        }
        return retried;
    }

    /** 单个 FAILED 步骤重试（retryCount+1 + 刷新模板 actionConfig + 重执行）。@return true = 链再次中断（失败/待审批），调用方停止后续步骤 */
    private boolean retryOneFailedStep(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, IServiceContext context) {
        step.setRetryCount((step.getRetryCount() == null ? 0 : step.getRetryCount()) + 1);
        refreshActionConfigFromTemplate(step, context);
        touchExecuted(step, ticket, context);
        try {
            String result = executeStep(step, ticket, context);
            return ErpCsConstants.FULFILLMENT_RESULT_FAILED.equals(result)
                    || ErpCsConstants.FULFILLMENT_RESULT_IN_PROGRESS.equals(result);
        } catch (Exception e) {
            markFailed(step, ticket, e.getMessage(), context);
            return true;
        }
    }

    private ErpCsTicket requireTicket(String ticketId, IServiceContext context) {
        ErpCsTicket ticket = ticketBiz.get(String.valueOf(ticketId), true, context);
        if (ticket == null) {
            throw new NopException(ErpCsErrors.ERR_TICKET_NOT_FOUND)
                    .param(ErpCsErrors.ARG_TICKET_ID, ticketId);
        }
        return ticket;
    }

    /**
     * REQUEST_APPROVAL 超时自动审批（job 扫描入口）：IN_PROGRESS + REQUEST_APPROVAL +
     * now - executedAt &gt; timeoutHours（actionConfig 覆盖 &gt; config 兜底）→ 自动 DONE + 审计 + 链恢复推进。
     *
     * @return 本轮超时自动审批的步骤数
     */
    public int autoApproveTimedOut(IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCsConstants.FULFILLMENT_STEP_IN_PROGRESS));
        q.addFilter(eq("actionType", ErpCsConstants.FULFILLMENT_ACTION_REQUEST_APPROVAL));
        q.setLimit(SCAN_LIMIT);
        List<ErpCsTicketFulfillmentStep> pending = stepBiz.findList(q, null, context);
        int count = 0;
        Timestamp now = CoreMetrics.currentTimestamp();
        for (ErpCsTicketFulfillmentStep step : pending) {
            if (step.getExecutedAt() == null) {
                continue;
            }
            long timeoutMs = approvalTimeoutHours(step) * 3600_000L;
            if (now.getTime() - step.getExecutedAt().getTime() <= timeoutMs) {
                continue;
            }
            ErpCsTicket ticket = ticketBiz.get(String.valueOf(step.getTicketId()), true, context);
            if (ticket == null) {
                continue;
            }
            step.setStatus(ErpCsConstants.FULFILLMENT_STEP_DONE);
            touchExecuted(step, ticket, context);
            writeAudit(ticket, step.getActionType(), null, null,
                    "DONE: 超时自动审批（超过 " + approvalTimeoutHours(step) + " 小时）", context);
            continueChain(step.getTicketId(), context);
            count++;
        }
        return count;
    }

    /** 重试候选工单（job 扫描入口）：存在 FAILED 且 retryCount &lt; max 步骤的工单 id（去重 + limit）。 */
    public List<String> findRetryCandidateTicketIds(IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCsConstants.FULFILLMENT_STEP_FAILED));
        q.setLimit(SCAN_LIMIT);
        List<ErpCsTicketFulfillmentStep> failed = stepBiz.findList(q, null, context);
        int retryMax = ErpCsConfigs.getFulfillmentRetryMax();
        Set<String> ticketIds = new LinkedHashSet<>();
        for (ErpCsTicketFulfillmentStep step : failed) {
            if (step.getRetryCount() == null || step.getRetryCount() < retryMax) {
                ticketIds.add(step.getTicketId());
            }
        }
        return new ArrayList<>(ticketIds);
    }

    // ---------- 终态推进（D4） ----------

    /**
     * 迁移助手：NEW（无 assignedToId）→ 自动指派当前操作员（回退 createdBy）后经 assign 边 → ASSIGNED →
     * start 边 → IN_PROGRESS；ASSIGNED → start → IN_PROGRESS；≥IN_PROGRESS/终态 → 幂等跳过。
     * 调用时机 = 链推进执行最后一个步骤之前（含单步链，见 {@link #runChain}）。
     */
    protected void ensureInProgress(ErpCsTicket ticket, IServiceContext context) {
        String status = ticket.getStatus();
        if (stateMachine.isTerminal(status) || !ErpCsConstants.TICKET_STATUS_NEW.equals(status)
                && !ErpCsConstants.TICKET_STATUS_ASSIGNED.equals(status)) {
            return;
        }
        String from = status;
        if (ErpCsConstants.TICKET_STATUS_NEW.equals(status)) {
            if (ticket.getAssignedToId() == null) {
                ticket.setAssignedToId(operatorId(ticket, context));
            }
            stateMachine.assertCanAssign(from);
            ticket.setStatus(stateMachine.assignTargetStatus());
            writeAudit(ticket, ErpCsConstants.ACTION_TYPE_ASSIGN, from, stateMachine.assignTargetStatus(),
                    "履行链自动指派: " + ticket.getAssignedToId(), context);
            from = stateMachine.assignTargetStatus();
        }
        stateMachine.assertCanStart(from);
        ticket.setStatus(stateMachine.startTargetStatus());
        if (ticket.getStartDateTime() == null) {
            ticket.setStartDateTime(CoreMetrics.currentTimestamp());
        }
        writeAudit(ticket, ErpCsConstants.ACTION_TYPE_NOTE, from, stateMachine.startTargetStatus(),
                "履行链推进开始处理", context);
        ticketBiz.updateEntity(ticket, null, context);
    }

    // ---------- 内部：步骤状态标记 / 审计 / 通知 ----------

    private void markDone(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, String content,
                          IServiceContext context) {
        writeAudit(ticket, step.getActionType(), null, null, content, context);
        markStepDone(step, context);
    }

    private void markSkipped(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, String content,
                             IServiceContext context) {
        writeAudit(ticket, step.getActionType(), null, null, content, context);
        step.setStatus(ErpCsConstants.FULFILLMENT_STEP_SKIPPED);
        touchExecuted(step, ticket, context);
    }

    /** 步骤失败标记（③）：FAILED + lastError + 通知管理员（7206，静默降级）。审计与状态由调用面写。 */
    private void markFailed(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, String error,
                            IServiceContext context) {
        step.setStatus(ErpCsConstants.FULFILLMENT_STEP_FAILED);
        step.setLastError(truncate(error));
        touchExecuted(step, ticket, context);
        writeAudit(ticket, step.getActionType(), null, null,
                "FAILED: " + (error == null ? "未知错误" : error), context);
        notifyStepFailed(step, ticket, error, context);
    }

    private void markStepDone(ErpCsTicketFulfillmentStep step, IServiceContext context) {
        step.setStatus(ErpCsConstants.FULFILLMENT_STEP_DONE);
    }

    private void touchExecuted(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, IServiceContext context) {
        step.setExecutedAt(CoreMetrics.currentTimestamp());
        step.setExecutedBy(operatorId(ticket, context));
    }

    /** 管理员失败通知（7206 cs.fulfillment-step-failed，ROLE 客服主管；notify 失败静默降级）。 */
    private void notifyStepFailed(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, String error,
                                  IServiceContext context) {
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("customerName", resolveCustomerName(ticket, context));
            ctx.put("sequence", step.getSequence());
            ctx.put("actionType", step.getActionType());
            ctx.put("errorMsg", error);
            ctx.put("retryCount", step.getRetryCount());
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_STEP_FAILED, ctx, context);
        } catch (Exception e) {
            LOG.warn("fulfillment-step-failed 通知派发失败（降级）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
    }

    /**
     * 超限拒绝路径的管理员通知：REQUIRES_NEW 独立事务提交——随后的拒绝异常会回滚当前 mutation 事务，
     * 同事务内插入的通知行会被连带回滚丢失（镜像 CsTicketMonthSeqCodeRuleVariable 月行懒建边界范式）。
     */
    private void notifyStepFailedRequireNew(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket, String error,
                                            IServiceContext context) {
        try {
            ormTemplate.runInNewSession(session ->
                    transactionTemplate.runInTransaction(null, TransactionPropagation.REQUIRES_NEW, txn -> {
                        notifyStepFailed(step, ticket, error, context);
                        return null;
                    }));
        } catch (Exception e) {
            LOG.warn("fulfillment-step-failed 通知派发失败（降级）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
    }

    /** 审批请求通知（cs.fulfillment-approval-request，ROLE approverRole；无 ACTIVE 模板时 notify 静默跳过）。 */
    private void notifyApprovalRequest(ErpCsTicketFulfillmentStep step, ErpCsTicket ticket,
                                       String approverRole, IServiceContext context) {
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("ticketId", ticket.getId());
            ctx.put("ticketCode", ticket.getCode());
            ctx.put("customerName", resolveCustomerName(ticket, context));
            ctx.put("sequence", step.getSequence());
            ctx.put("approverRole", approverRole);
            notificationBiz.notify(ErpCsConstants.NOTIFY_EVENT_FULFILLMENT_APPROVAL_REQUEST, ctx, context);
        } catch (Exception e) {
            LOG.warn("fulfillment-approval-request 通知派发失败（降级）：ticketId={}, reason={}",
                    ticket.getId(), e.getMessage());
        }
    }

    private void writeAudit(ErpCsTicket ticket, String actionType, String fromStatus, String toStatus,
                            String content, IServiceContext context) {
        if (ticketActionBiz == null) {
            return;
        }
        ErpCsTicketAction action = ticketActionBiz.newEntity();
        action.setTicketId(ticket.getId());
        action.setActionType(actionType);
        action.setFromStatus(fromStatus);
        action.setToStatus(toStatus);
        action.setContent(content);
        action.setOperatorId(context.getUserId());
        ticketActionBiz.saveEntity(action, null, context);
    }

    // ---------- 内部：解析 / 查询辅助 ----------

    /** actionConfig 解析（JsonTool 容错）：空/非法 JSON → 空 Map（UPDATE_STATUS 分支自行显式 FAILED）。 */
    private Map<String, Object> parseActionConfig(String actionConfig) {
        if (StringHelper.isBlank(actionConfig)) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> parsed = JsonTool.parseBeanFromText(actionConfig, Map.class);
            return parsed == null ? new LinkedHashMap<>() : parsed;
        } catch (Exception e) {
            LOG.warn("fulfillment-action-config-parse-failed（按缺省策略执行）: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    private boolean isBlankConfig(String actionConfig) {
        return StringHelper.isBlank(actionConfig);
    }

    private String configValue(Map<String, Object> config, String key) {
        Object v = config.get(key);
        return v == null ? null : String.valueOf(v);
    }

    /** 重试刷新：从模板重读 actionConfig（修正配置即生效；首建快照语义见 {@link #materializeStep}）。 */
    private void refreshActionConfigFromTemplate(ErpCsTicketFulfillmentStep step, IServiceContext context) {
        ErpCsCatalogFulfillment template = step.getFulfillment();
        if (template != null && template.getActionConfig() != null) {
            step.setActionConfig(template.getActionConfig());
        }
    }

    private ErpCsTicketFulfillmentStep requireStep(String stepId, IServiceContext context) {
        if (stepId == null) {
            throw new NopException(ErpCsErrors.ERR_CS_FULFILLMENT_STEP_NOT_FOUND)
                    .param(ErpCsErrors.ARG_STEP_ID, stepId);
        }
        ErpCsTicketFulfillmentStep step = stepBiz.get(String.valueOf(stepId), true, context);
        if (step == null) {
            throw new NopException(ErpCsErrors.ERR_CS_FULFILLMENT_STEP_NOT_FOUND)
                    .param(ErpCsErrors.ARG_STEP_ID, stepId);
        }
        return step;
    }

    /** 状态机迁移矩阵查边：from→target 合法边存在返回 action 名，否则 null。 */
    private String transitionAction(String from, String target) {
        for (ErpCsTicketStateMachine.TransitionDefinition t : stateMachine.transitions()) {
            if (t.getFromStatus().equals(from) && t.getToStatus().equals(target)) {
                return t.getAction();
            }
        }
        return null;
    }

    private int approvalTimeoutHours(ErpCsTicketFulfillmentStep step) {
        Map<String, Object> config = parseActionConfig(step.getActionConfig());
        String raw = configValue(config, "timeoutHours");
        if (!StringHelper.isEmpty(raw)) {
            try {
                return Math.max(1, Integer.parseInt(raw.trim()));
            } catch (NumberFormatException ignore) {
                // 非法配置回退 config 兜底
            }
        }
        return ErpCsConfigs.getFulfillmentApprovalTimeoutHours();
    }

    private String operatorId(ErpCsTicket ticket, IServiceContext context) {
        if (context.getUserId() != null) {
            return context.getUserId();
        }
        return ticket == null ? null : ticket.getCreatedBy();
    }

    private String roleNote(ErpCsTicketFulfillmentStep step) {
        ErpCsCatalogFulfillment template = step.getFulfillment();
        if (template != null && !StringHelper.isEmpty(template.getAssignToRole())) {
            return "，assignToRole=" + template.getAssignToRole();
        }
        return "";
    }

    private String catalogItemName(ErpCsTicket ticket) {
        if (ticket.getCatalogItemId() == null) {
            return null;
        }
        ErpCsServiceCatalogItem item = ticket.getCatalogItem();
        return item == null ? null : item.getName();
    }

    private String resolveCustomerName(ErpCsTicket ticket, IServiceContext context) {
        if (ticket.getCustomerId() == null) {
            return null;
        }
        try {
            ErpMdPartner partner = mdPartnerBiz.findById(ticket.getCustomerId(), context);
            return partner == null ? null : partner.getName();
        } catch (Exception e) {
            return null;
        }
    }

    private ErpCsSlaPolicy resolveTypeDefaultPolicy(ErpCsTicket ticket) {
        if (ticket.getTicketTypeId() == null) {
            return null;
        }
        ErpCsTicketType type = ticket.getTicketType();
        return type == null ? null : type.getDefaultSlaPolicy();
    }

    /** ROUND_ROBIN 历史：候选池成员内最近一张已分配工单（镜像 ErpCsTicketBizModel 同名私有逻辑，经 IBiz 查询）。 */
    private String findLastAssigned(List<String> pool, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(in("assignedToId", pool));
        q.addOrderField("createTime", true);
        q.setLimit(1);
        List<ErpCsTicket> last = ticketBiz.findList(q, null, context);
        return last.isEmpty() ? null : last.get(0).getAssignedToId();
    }

    /** LEAST_OPEN 计数：候选成员活跃工单（ASSIGNED/IN_PROGRESS）分组计数。 */
    private Map<String, Integer> countOpenTickets(List<String> pool, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(in("assignedToId", pool));
        q.addFilter(in("status", java.util.Arrays.asList(
                ErpCsConstants.TICKET_STATUS_ASSIGNED, ErpCsConstants.TICKET_STATUS_IN_PROGRESS)));
        List<ErpCsTicket> open = ticketBiz.findList(q, null, context);
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ErpCsTicket t : open) {
            if (t.getAssignedToId() != null) {
                counts.merge(t.getAssignedToId(), 1, Integer::sum);
            }
        }
        return counts;
    }

    private List<ErpCsTicketFulfillmentStep> loadStepsByTicket(String ticketId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addOrderField("sequence", true);
        List<ErpCsTicketFulfillmentStep> steps = stepBiz.findList(q, null, context);
        steps.sort(Comparator.comparingInt(this::sequenceOfStep));
        return steps;
    }    private List<ErpCsCatalogFulfillment> loadTemplatesByCatalogItem(String catalogItemId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("catalogItemId", catalogItemId));
        // 模板行加载走 daoFor：本 Processor 属 biz_ErpCsCatalogFulfillment 调用链，注入自有 IBiz 会成环
        //（R1.33/51/56 per-mutation Processor daoFor 同型站点先例）
        IEntityDao<ErpCsCatalogFulfillment> dao = daoProvider.daoFor(ErpCsCatalogFulfillment.class);
        return dao.findAllByQuery(q);
    }

    private int sequenceOfStep(ErpCsTicketFulfillmentStep step) {
        Integer seq = step.getSequence();
        return seq == null ? 0 : seq;
    }

    private static String truncate(String s) {
        if (s == null) {
            return null;
        }
        return s.length() <= LAST_ERROR_MAX_LENGTH ? s : s.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}
