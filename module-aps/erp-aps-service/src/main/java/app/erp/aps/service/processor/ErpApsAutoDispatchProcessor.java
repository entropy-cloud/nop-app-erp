package app.erp.aps.service.processor;

import app.erp.aps.dao.entity.ErpApsDispatchLog;
import app.erp.aps.dao.entity.ErpApsDispatchRule;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.service.ErpApsConfigs;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.ErpApsErrors;
import app.erp.aps.service.statemachine.ErpApsOperationOrderStateMachine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkcenter;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * UC-APS-07 自动派工引擎 Processor（RC-R1.88 / P1-RC-090，auto-dispatch.md §二/三）。
 *
 * <p><b>D5 裁决（选项 A：inventory 可用量经 IDaoProvider 直查）</b>：物料齐套 = 工单 BOM 单层展开
 * （bomId 缺失回落产品默认 BOM）× plannedQuantity 对照 Σ{@code ErpInvStockBalance.availableQuantity}。
 * 否决选项 B（复用 mfg KitAvailabilityChecker 内部逻辑）：系 mfg-service 内部类非 dao 接口，须在 mfg 侧
 * 暴露 Facade + service 级依赖，破坏 aps 单模块测试（matrix §9.4 I*Biz 强注入 NoSuchBeanFailure 先例）；
 * mfg 多级展开/快照感知语义归 mfg 域内部，本引擎取单层简化口径（owner doc 实现注记）。
 *
 * <p><b>D6 裁决（选项 A：复用制造域既有建卡 seam）</b>：派工引擎不直接建 JobCard（跨域写否决）；
 * JobCard 由 mfg 既有 {@code erp-mfg-jobcard-auto-generate.job.yaml} +
 * {@code generatePendingJobCards/generateJobCardsFromSchedule} 幂等增量建卡承载——
 * {@code ApsLoadSourceProvider} 已扩展含 IN_PROGRESS（已派工）工序，派工先于日批建卡的工序
 * 下一轮建卡自然补齐，与 PLANNED 建卡通道无重叠（同 seam 幂等去重，零双卡）。
 *
 * <p>条件三维度简单口径（owner doc §2.3 + Deferred 裁决）：操作工排班无载体 → 条件结果 null 放行；
 * requireTooling=true 且仓库无工装载体 → null 放行 + LOG.warn（完整工装管理 successor）。
 *
 * <p>每 step 为 protected，下游可逐个覆盖（产品化）。
 */
public class ErpApsAutoDispatchProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpApsAutoDispatchProcessor.class);

    static final int DEFAULT_MAX_LOOKAHEAD_MINUTES = 120;
    static final int DEFAULT_DISPATCH_AHEAD_MINUTES = 15;
    static final int DEFAULT_PRIORITY = 50;

    /** 系统派工人标记（DispatchLog.dispatchedBy，auto-dispatch.md §4.1 AUTO=系统）。 */
    static final String DISPATCHED_BY_SYSTEM = "system";

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    @Inject
    ErpApsOperationOrderStateMachine stateMachine;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setStateMachine(ErpApsOperationOrderStateMachine stateMachine) {
        this.stateMachine = stateMachine;
    }

    // ---------- 自动派工扫描（auto-dispatch.md §2.1 DISPATCH_CHECK） ----------

    /**
     * 单轮扫描：逐工作中心按 DispatchRule 过滤 eligible PLANNED 工序，逐工序检查三维度条件，
     * 全满足→IN_PROGRESS + DispatchLog；缺料（窗口内）→ON_HOLD + 通知计划员；其余不满足跳过继续。
     * 全局开关 {@code erp-aps.auto-dispatch-enabled}（默认 false，auto-dispatch.md §5.2）门控。
     */
    public int scanOnce(IServiceContext context) {
        if (!io.nop.api.core.config.AppConfig.var(ErpApsConfigs.CONFIG_AUTO_DISPATCH_ENABLED,
                ErpApsConfigs.DEFAULT_AUTO_DISPATCH_ENABLED)) {
            LOG.info("erp-aps-auto-dispatch-skipped: global switch off (erp-aps.auto-dispatch-enabled)");
            return 0;
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        int dispatched = 0;
        for (ErpApsDispatchRule rule : loadRules()) {
            dispatched += dispatchForRule(rule, now, context);
        }
        return dispatched;
    }

    protected int dispatchForRule(ErpApsDispatchRule rule, LocalDateTime now, IServiceContext context) {
        if (!Boolean.TRUE.equals(rule.getEnableAuto())) {
            return 0; // 该工作中心不自动派工
        }
        if (rule.getHoldUntil() != null && rule.getHoldUntil().toLocalDateTime().isAfter(now)) {
            return 0; // 管理员暂停中
        }
        if (!withinEnabledHours(rule.getEnabledHours(), now.toLocalTime())) {
            return 0; // 允许时段窗口外（空=全天）
        }

        List<ErpApsOperationOrder> eligible = findEligibleOps(rule, now);
        if (eligible.isEmpty()) {
            return 0;
        }

        int maxConcurrent = resolveMaxConcurrentOps(rule);
        int running = countRunningOps(rule.getWorkcenterId());
        int slots = maxConcurrent - running;

        int dispatched = 0;
        for (ErpApsOperationOrder op : eligible) {
            if (slots <= 0) {
                break; // maxConcurrentOps 满额
            }
            DispatchConditions conditions = checkConditions(rule, op, context);
            if (conditions.allPassed()) {
                doDispatch(rule, op, ErpApsConstants.DISPATCH_TYPE_AUTO, conditions, null,
                        DISPATCHED_BY_SYSTEM, context);
                slots--;
                dispatched++;
            } else if (Boolean.FALSE.equals(conditions.materialAvailable)) {
                // L1 异常路径：派工窗口内缺料 → ON_HOLD + 通知计划员（保持态由 status dict 值承载）
                doShortageHold(rule, op, conditions, context);
            }
            // 其余条件不满足 → 跳过（等待下一轮）
        }
        return dispatched;
    }

    // ---------- step：规则加载与 eligible 过滤（protected，下游可覆盖） ----------

    protected List<ErpApsDispatchRule> loadRules() {
        QueryBean q = new QueryBean();
        q.addOrderField("workcenterId", false);
        q.addOrderField("id", false);
        return dispatchRuleDao().findAllByQuery(q);
    }

    protected List<ErpApsOperationOrder> findEligibleOps(ErpApsDispatchRule rule, LocalDateTime now) {
        int lookahead = rule.getMaxLookaheadMinutes() != null ? rule.getMaxLookaheadMinutes()
                : DEFAULT_MAX_LOOKAHEAD_MINUTES;
        int ahead = rule.getDispatchAheadMinutes() != null ? rule.getDispatchAheadMinutes()
                : DEFAULT_DISPATCH_AHEAD_MINUTES;

        QueryBean q = new QueryBean();
        // 保持态由 status dict 值承载（HOLD/ON_HOLD 天然排除——仅 PLANNED 可派）
        q.addFilter(eq("status", ErpApsConstants.OP_STATUS_PLANNED));
        q.addFilter(eq("machineId", rule.getWorkcenterId()));
        q.addFilter(le("plannedStartDateT", now.plusMinutes(lookahead)));
        q.addFilter(ge("plannedStartDateT", now.minusMinutes(ahead)));

        List<ErpApsOperationOrder> ops = opOrderDao().findAllByQuery(q);
        if (rule.getPriorityThreshold() != null) {
            List<ErpApsOperationOrder> filtered = new ArrayList<>();
            for (ErpApsOperationOrder op : ops) {
                int priority = op.getPriority() == null ? DEFAULT_PRIORITY : op.getPriority();
                if (priority <= rule.getPriorityThreshold()) {
                    filtered.add(op);
                }
            }
            ops = filtered;
        }
        // (plannedStartDateT ASC, priority ASC)
        ops.sort(Comparator
                .comparing(ErpApsOperationOrder::getPlannedStartDateT)
                .thenComparing(o -> o.getPriority() == null ? DEFAULT_PRIORITY : o.getPriority()));
        return ops;
    }

    protected boolean withinEnabledHours(String enabledHoursJson, LocalTime now) {
        if (enabledHoursJson == null || enabledHoursJson.isBlank()) {
            return true; // 空=全天
        }
        try {
            Object parsed = JsonTool.parse(enabledHoursJson);
            List<Map<String, Object>> windows = parsed instanceof List
                    ? (List<Map<String, Object>>) parsed : null;
            if (windows == null || windows.isEmpty()) {
                return true;
            }
            for (Map<String, Object> w : windows) {
                LocalTime start = LocalTime.parse(String.valueOf(w.get("start")));
                LocalTime end = LocalTime.parse(String.valueOf(w.get("end")));
                boolean hit = !now.isBefore(start) && now.isBefore(end);
                if (hit) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOG.warn("erp-aps-auto-dispatch enabledHours parse failed (treated as all-day): {}", e.getMessage());
            return true;
        }
    }

    protected int resolveMaxConcurrentOps(ErpApsDispatchRule rule) {
        if (rule.getMaxConcurrentOps() != null && rule.getMaxConcurrentOps() > 0) {
            return rule.getMaxConcurrentOps();
        }
        // 默认 = 工作中心 capacity（auto-dispatch.md §1.2；缺省 1）
        ErpMfgWorkcenter wc = daoProvider.daoFor(ErpMfgWorkcenter.class).getEntityById(rule.getWorkcenterId());
        if (wc != null && wc.getCapacity() != null) {
            return Math.max(1, wc.getCapacity().setScale(0, RoundingMode.CEILING).intValueExact());
        }
        return 1;
    }

    protected int countRunningOps(Long workcenterId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("machineId", workcenterId));
        q.addFilter(eq("status", ErpApsConstants.OP_STATUS_IN_PROGRESS));
        return opOrderDao().findAllByQuery(q).size();
    }

    // ---------- step：三维度条件检查 ----------

    protected DispatchConditions checkConditions(ErpApsDispatchRule rule, ErpApsOperationOrder op,
                                                 IServiceContext context) {
        DispatchConditions conditions = new DispatchConditions();
        conditions.materialAvailable = Boolean.TRUE.equals(rule.getRequireMaterial())
                ? checkMaterialAvailability(op) : null;
        conditions.operatorAvailable = Boolean.TRUE.equals(rule.getRequireOperator())
                ? checkOperatorAvailability(op) : null;
        conditions.toolingAvailable = Boolean.TRUE.equals(rule.getRequireTooling())
                ? checkToolingAvailability(op) : null;
        return conditions;
    }

    /**
     * D5 通道：工单 BOM 单层展开 × plannedQuantity 对照库存可用量（Σ 全仓 availableQuantity）。
     * 无工单/无 BOM/无子件行 → null（无需求视为满足，记条件结果 null）。
     */
    protected Boolean checkMaterialAvailability(ErpApsOperationOrder op) {
        ErpMfgWorkOrder wo = op.getWorkOrderId() == null ? null
                : daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(op.getWorkOrderId());
        if (wo == null) {
            return null;
        }
        ErpMfgBom bom = resolveBom(wo);
        if (bom == null) {
            return null;
        }
        List<ErpMfgBomLine> lines = loadBomLines(bom.getId());
        if (lines.isEmpty()) {
            return null;
        }
        BigDecimal bomQty = bom.getQty() == null || bom.getQty().signum() == 0 ? BigDecimal.ONE : bom.getQty();
        BigDecimal planned = wo.getPlannedQuantity() == null ? BigDecimal.ZERO : wo.getPlannedQuantity();

        Map<Long, BigDecimal> requiredByMaterial = new LinkedHashMap<>();
        for (ErpMfgBomLine line : lines) {
            if (line.getMaterialId() == null) {
                continue;
            }
            BigDecimal perUnit = line.getQuantity() == null ? BigDecimal.ZERO : line.getQuantity();
            BigDecimal required = perUnit.multiply(planned).divide(bomQty, 4, RoundingMode.HALF_UP);
            requiredByMaterial.merge(line.getMaterialId(), required, BigDecimal::add);
        }
        if (requiredByMaterial.isEmpty()) {
            return null;
        }
        for (Map.Entry<Long, BigDecimal> e : requiredByMaterial.entrySet()) {
            if (sumAvailable(e.getKey()).compareTo(e.getValue()) < 0) {
                return false;
            }
        }
        return true;
    }

    protected ErpMfgBom resolveBom(ErpMfgWorkOrder wo) {
        IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
        if (wo.getBomId() != null) {
            return dao.getEntityById(wo.getBomId());
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("productId", wo.getProductId()));
        q.addFilter(eq("isDefault", Boolean.TRUE));
        q.addFilter(eq("isActive", Boolean.TRUE));
        q.setLimit(1);
        List<ErpMfgBom> found = dao.findAllByQuery(q);
        return found.isEmpty() ? null : found.get(0);
    }

    protected List<ErpMfgBomLine> loadBomLines(Long bomId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("bomId", bomId));
        return daoProvider.daoFor(ErpMfgBomLine.class).findAllByQuery(q);
    }

    protected BigDecimal sumAvailable(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        BigDecimal total = BigDecimal.ZERO;
        for (ErpInvStockBalance b : daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q)) {
            if (b.getAvailableQuantity() != null) {
                total = total.add(b.getAvailableQuantity());
            }
        }
        return total;
    }

    /**
     * 操作工在岗检查（简单场景口径）：本仓库无工作中心排班载体 → 条件结果 null 放行
     * （auto-dispatch.md §2.3「简单场景：至少 1 名在岗操作工即可派工」；技能矩阵/排班深查 successor）。
     */
    protected Boolean checkOperatorAvailability(ErpApsOperationOrder op) {
        return null;
    }

    /**
     * 工装可用检查（Deferred But Adjudicated 口径）：仓库无工装主数据载体 → null 放行 + LOG.warn
     * （requireTooling 开关行为可测试非模糊；工装管理 successor）。
     */
    protected Boolean checkToolingAvailability(ErpApsOperationOrder op) {
        LOG.warn("erp-aps-auto-dispatch tooling check degraded (no tooling carrier): operationOrderId={}",
                op.getId());
        return null;
    }

    // ---------- step：派工执行 / 保持 / 缺料暂停 ----------

    protected void doDispatch(ErpApsDispatchRule rule, ErpApsOperationOrder op, String dispatchType,
                              DispatchConditions conditions, String note, String dispatchedBy,
                              IServiceContext context) {
        String previous = op.getStatus();
        stateMachine.assertCanStart(previous); // PLANNED→IN_PROGRESS 与 start 同边（矩阵权威）
        op.setStatus(stateMachine.startTargetStatus());
        op.setRealStartDateT(CoreMetrics.currentTimestamp());
        opOrderDao().updateEntity(op);

        ErpApsDispatchLog log = dispatchLogDao().newEntity();
        log.setOperationOrderId(op.getId());
        log.setWorkcenterId(op.getMachineId());
        log.setDispatchType(dispatchType);
        log.setPreviousStatus(previous);
        log.setNewStatus(op.getStatus());
        log.setDispatchedBy(dispatchedBy);
        log.setDispatchedAt(CoreMetrics.currentTimestamp());
        if (conditions != null) {
            log.setMaterialAvailable(conditions.materialAvailable);
            log.setOperatorAvailable(conditions.operatorAvailable);
            log.setToolingAvailable(conditions.toolingAvailable);
            log.setConditionCheckResult(conditions.toJson(rule));
        } else {
            // 手动强制派工跳过条件检查：三维结果留空，note 承载跳检原因
            log.setConditionCheckResult("{\"skipped\":true}");
        }
        log.setNote(note);
        dispatchLogDao().saveEntity(log);
    }

    /** 缺料暂停（L1 异常路径）：PLANNED→ON_HOLD + HOLD 型日志 + 通知计划员（模板无 ACTIVE 静默跳过）。 */
    protected void doShortageHold(ErpApsDispatchRule rule, ErpApsOperationOrder op,
                                  DispatchConditions conditions, IServiceContext context) {
        String previous = op.getStatus();
        op.setStatus(ErpApsConstants.OP_STATUS_ON_HOLD);
        opOrderDao().updateEntity(op);

        ErpApsDispatchLog log = dispatchLogDao().newEntity();
        log.setOperationOrderId(op.getId());
        log.setWorkcenterId(op.getMachineId());
        log.setDispatchType(ErpApsConstants.DISPATCH_TYPE_HOLD);
        log.setPreviousStatus(previous);
        log.setNewStatus(ErpApsConstants.OP_STATUS_ON_HOLD);
        log.setDispatchedBy(DISPATCHED_BY_SYSTEM);
        log.setDispatchedAt(CoreMetrics.currentTimestamp());
        log.setMaterialAvailable(conditions.materialAvailable);
        log.setOperatorAvailable(conditions.operatorAvailable);
        log.setToolingAvailable(conditions.toolingAvailable);
        log.setConditionCheckResult(conditions.toJson(rule));
        log.setNote("material-shortage-auto-hold");
        dispatchLogDao().saveEntity(log);

        notifyShortage(op, conditions, context);
    }

    protected void notifyShortage(ErpApsOperationOrder op, DispatchConditions conditions, IServiceContext context) {
        try {
            Map<String, Object> ctx = new LinkedHashMap<>();
            ctx.put("operationOrderId", op.getId());
            ctx.put("operationOrderCode", op.getCode());
            ctx.put("workcenterId", op.getMachineId());
            ctx.put("plannedStartDateT", op.getPlannedStartDateT() != null
                    ? op.getPlannedStartDateT().toString() : null);
            notificationBiz.notify(ErpApsConstants.NOTIFY_EVENT_DISPATCH_MATERIAL_SHORTAGE, ctx, context);
        } catch (Exception e) {
            LOG.warn("erp-aps-auto-dispatch shortage notify failed (degraded, main flow continues): operationOrderId={}, reason={}",
                    op.getId(), e.getMessage());
        }
    }

    // ---------- 手动 mutation 族（auto-dispatch.md §3.2/§3.3） ----------

    /** 手动强制派工：可跳过条件检查但原因（note）必填；dispatchType=MANUAL。 */
    public ErpApsOperationOrder dispatchManually(Long operationOrderId, String note, IServiceContext context) {
        ErpApsOperationOrder op = requireOp(operationOrderId);
        if (note == null || note.isBlank()) {
            throw new NopException(ErpApsErrors.ERR_APS_DISPATCH_REASON_REQUIRED)
                    .param(ErpApsErrors.ARG_OP_CODE, op.getCode());
        }
        doDispatch(null, op, ErpApsConstants.DISPATCH_TYPE_MANUAL, null, note,
                currentUserId(), context);
        return op;
    }

    /** 保持：PLANNED→HOLD（计划员暂不派工）。 */
    public ErpApsOperationOrder hold(Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder op = requireOp(operationOrderId);
        try {
            stateMachine.assertCanHold(op.getStatus());
        } catch (NopException e) {
            throw illegalTransition(op, ErpApsConstants.OP_STATUS_PLANNED, e);
        }
        String previous = op.getStatus();
        op.setStatus(stateMachine.holdTargetStatus());
        opOrderDao().updateEntity(op);
        writeHoldLog(op, previous, ErpApsConstants.DISPATCH_TYPE_HOLD, "manual hold",
                currentUserId());
        return op;
    }

    /** 解除保持：HOLD/ON_HOLD→PLANNED，重新进入自动派工检查循环。 */
    public ErpApsOperationOrder unhold(Long operationOrderId, IServiceContext context) {
        ErpApsOperationOrder op = requireOp(operationOrderId);
        try {
            stateMachine.assertCanUnhold(op.getStatus());
        } catch (NopException e) {
            throw illegalTransition(op,
                    ErpApsConstants.OP_STATUS_HOLD + "/" + ErpApsConstants.OP_STATUS_ON_HOLD, e);
        }
        String previous = op.getStatus();
        op.setStatus(stateMachine.unholdTargetStatus());
        opOrderDao().updateEntity(op);
        writeHoldLog(op, previous, ErpApsConstants.DISPATCH_TYPE_UNHOLD, "manual unhold",
                currentUserId());
        return op;
    }

    protected void writeHoldLog(ErpApsOperationOrder op, String previous, String dispatchType,
                                String note, String dispatchedBy) {
        ErpApsDispatchLog log = dispatchLogDao().newEntity();
        log.setOperationOrderId(op.getId());
        log.setWorkcenterId(op.getMachineId());
        log.setDispatchType(dispatchType);
        log.setPreviousStatus(previous);
        log.setNewStatus(op.getStatus());
        log.setDispatchedBy(dispatchedBy);
        log.setDispatchedAt(CoreMetrics.currentTimestamp());
        log.setNote(note);
        dispatchLogDao().saveEntity(log);
    }

    // ---------- 辅助 ----------

    /** 当前用户（无登录上下文时 null，如系统 job 路径）。 */
    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected ErpApsOperationOrder requireOp(Long operationOrderId) {
        ErpApsOperationOrder op = operationOrderId == null ? null : opOrderDao().getEntityById(operationOrderId);
        if (op == null) {
            throw new NopException(ErpApsErrors.ERR_APS_OP_ORDER_NOT_FOUND)
                    .param(ErpApsErrors.ARG_OP_ORDER_ID, operationOrderId);
        }
        return op;
    }

    protected NopException illegalTransition(ErpApsOperationOrder op, String expected, Throwable cause) {
        return new NopException(ErpApsErrors.ERR_APS_OP_ILLEGAL_TRANSITION, cause)
                .param(ErpApsErrors.ARG_OP_CODE, op.getCode())
                .param(ErpApsErrors.ARG_CURRENT_STATUS, op.getStatus())
                .param(ErpApsErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected IEntityDao<ErpApsOperationOrder> opOrderDao() {
        return daoProvider.daoFor(ErpApsOperationOrder.class);
    }

    protected IEntityDao<ErpApsDispatchRule> dispatchRuleDao() {
        return daoProvider.daoFor(ErpApsDispatchRule.class);
    }

    protected IEntityDao<ErpApsDispatchLog> dispatchLogDao() {
        return daoProvider.daoFor(ErpApsDispatchLog.class);
    }

    /** 三维度条件结果（null=该维度未要求或无载体降级放行）。 */
    public static final class DispatchConditions {
        Boolean materialAvailable;
        Boolean operatorAvailable;
        Boolean toolingAvailable;

        boolean allPassed() {
            return !Boolean.FALSE.equals(materialAvailable)
                    && !Boolean.FALSE.equals(operatorAvailable)
                    && !Boolean.FALSE.equals(toolingAvailable);
        }

        String toJson(ErpApsDispatchRule rule) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("ruleId", rule != null ? rule.getId() : null);
            m.put("material", materialAvailable);
            m.put("operator", operatorAvailable);
            m.put("tooling", toolingAvailable);
            return JsonTool.stringify(m);
        }
    }
}
