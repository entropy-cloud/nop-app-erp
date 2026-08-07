package app.erp.hr.service.job;

import app.erp.hr.biz.IErpHrDepartmentBiz;
import app.erp.hr.biz.IErpHrEmployeeBiz;
import app.erp.hr.biz.IErpHrLeaveRequestBiz;
import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.service.ErpHrConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.auth.biz.INopAuthUserBiz;
import io.nop.auth.dao.entity.NopAuthUser;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.dateTimeBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 定时休假审批超时转派 Job Bean（use-cases.md UC-HR-02⑦）。
 *
 * <p>由 nop-job-local 的 {@code scheduler.yaml} 经 BeanMethodJobInvoker 反射调用 {@link #execute()}。
 * 触发频率由 {@code erp-hr-leave-approver-timeout.job.yaml} 的 cronExpr 决定（设计默认每日 01:00）。
 *
 * <p>实际执行门控：{@code erp-hr.leave-approver-timeout-cron} 配置为空时跳过（"不调度"语义）；
 * 非空时扫描 {@code status=SUBMITTED} 且 {@code updateTime < now - timeoutHours} 的休假单，
 * 逐条转派：解析直接上级（{@code ErpHrEmployee.superiorId}）→ 兜底部门负责人
 * （{@code ErpHrDepartment.managerId}）→ 均缺失则跳过并 LOG.warn（可观测，不静默）。
 * 幂等守卫：{@code approverId} 已 == 目标人时跳过（防重复派发）。
 * 转派后经 {@link IErpSysNotificationBiz#notify}（{@code hr.leave-approver-timeout}）派发通知，
 * USER_LIST 模板 {@code {"userIds":["${superiorUserId}"]}} 从 context 插值接收人。
 * 单条失败隔离（try/catch per leave，不阻断后续）。镜像 {@code ErpHrContractExpiryJob} 范式
 * （双层门控 + 单条失败隔离 + 跨实体经 I*Biz 注入）。
 */
public class ErpHrLeaveApproverTimeoutJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpHrLeaveApproverTimeoutJob.class);

    /** 单次扫描最大条数（分页 limit 保护）。 */
    static final int SCAN_LIMIT = 200;

    @Inject
    IErpHrLeaveRequestBiz leaveRequestBiz;
    @Inject
    IErpHrEmployeeBiz employeeBiz;
    @Inject
    IErpHrDepartmentBiz departmentBiz;
    @Inject
    INopAuthUserBiz authUserBiz;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public void setLeaveRequestBiz(IErpHrLeaveRequestBiz leaveRequestBiz) {
        this.leaveRequestBiz = leaveRequestBiz;
    }

    public void setEmployeeBiz(IErpHrEmployeeBiz employeeBiz) {
        this.employeeBiz = employeeBiz;
    }

    public void setDepartmentBiz(IErpHrDepartmentBiz departmentBiz) {
        this.departmentBiz = departmentBiz;
    }

    public void setAuthUserBiz(INopAuthUserBiz authUserBiz) {
        this.authUserBiz = authUserBiz;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描超时休假单并逐条转派 + 派发通知。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-hr-leave-approver-timeout-skipped: cron config empty (erp-hr.leave-approver-timeout-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int escalated = runTimeoutEscalation(ctx);
            LOG.info("erp-hr-leave-approver-timeout-done: escalated={}", escalated);
        } catch (Exception e) {
            LOG.error("erp-hr-leave-approver-timeout-failed", e);
        }
    }

    /**
     * 扫描超时 SUBMITTED 休假单并逐条转派；返回成功转派条数。
     * 扫描与更新在同一 ORM session 内完成（findList 返回的实体保持 MANAGED，updateEntity 方可落库）。
     */
    protected int runTimeoutEscalation(IServiceContext ctx) {
        long timeoutHours = resolveTimeoutHours();
        LocalDateTime cutoff = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(CoreMetrics.currentTimeMillis() - timeoutHours * 3600_000L),
                java.time.ZoneId.systemDefault());
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("status", ErpHrConstants.LEAVE_STATUS_SUBMITTED));
            // updateTime < now - timeoutHours 语义：XMeta 默认过滤操作集仅允许
            // eq/in/dateBetween/dateTimeBetween（ObjMetaBasedFilterValidator.DEFAULT_ALLOW_FILTER_OP，
            // 不支持 lt——对齐 ErpCsTicketBizModel.findSlaWarnings:232 dateTimeBetween 先例），
            // 以 datetimeBetween(epoch, cutoff) 表达"早于 cutoff"（业务上无早于 epoch 的 updateTime）。
            q.addFilter(dateTimeBetween("updateTime",
                    LocalDateTime.of(1970, 1, 1, 0, 0), cutoff));
            q.setLimit(SCAN_LIMIT);
            List<ErpHrLeaveRequest> leaves = leaveRequestBiz.findList(q, null, ctx);
            if (leaves == null || leaves.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ErpHrLeaveRequest leave : leaves) {
                try {
                    if (escalateLeave(leave, ctx)) {
                        count++;
                    }
                } catch (Exception e) {
                    LOG.warn("erp-hr-leave-approver-timeout: 单条休假转派失败（隔离继续）：leaveId={}, reason={}",
                            leave.getId(), e.getMessage());
                }
            }
            return count;
        });
    }

    /**
     * 单条超时转派：解析转派目标 → 幂等守卫 → 回写 approverId → 派发通知。返回 true 表示已转派。
     */
    protected boolean escalateLeave(ErpHrLeaveRequest leave, IServiceContext ctx) {
        Long targetId = resolveEscalationTarget(leave.getEmployeeId(), ctx);
        if (targetId == null) {
            LOG.warn("erp-hr-leave-approver-timeout: 休假单无转派目标（无直接上级且无部门负责人），跳过：leaveId={}, employeeId={}",
                    leave.getId(), leave.getEmployeeId());
            return false;
        }
        // 幂等守卫：approverId 已 == 目标人则跳过（防重复派发；首扫后 updateEntity 刷新 updateTime 亦不再命中过滤）
        if (leave.getApproverId() != null && leave.getApproverId().equals(targetId)) {
            LOG.info("erp-hr-leave-approver-timeout: 幂等跳过（approverId 已为目标审批人）：leaveId={}", leave.getId());
            return false;
        }
        leave.setApproverId(targetId);
        leaveRequestBiz.updateEntity(leave, null, ctx);
        notifyEscalation(leave, targetId, ctx);
        return true;
    }

    /**
     * 转派目标解析链：直接上级（{@code ErpHrEmployee.superiorId}）→ 兜底部门负责人
     * （{@code ErpHrDepartment.managerId}，经 employee.departmentId 关联）；均缺失返回 null。
     */
    protected Long resolveEscalationTarget(Long employeeId, IServiceContext ctx) {
        ErpHrEmployee employee = employeeBiz.get(String.valueOf(employeeId), true, ctx);
        if (employee == null) {
            return null;
        }
        if (employee.getSuperiorId() != null) {
            return employee.getSuperiorId();
        }
        if (employee.getDepartmentId() != null) {
            ErpHrDepartment department = departmentBiz.get(String.valueOf(employee.getDepartmentId()), true, ctx);
            if (department != null && department.getManagerId() != null) {
                return department.getManagerId();
            }
        }
        return null;
    }

    /**
     * 派发超时转派通知（config-gated：无 ACTIVE 模板时 notify 内部静默跳过）。
     * context 键对齐 {@code hr.leave-approver-timeout} 模板约定：leaveCode/leaveType/employeeId/
     * submitterUserId/superiorUserId/superiorId，USER_LIST 接收人经 ${superiorUserId} 插值。
     */
    protected void notifyEscalation(ErpHrLeaveRequest leave, Long targetId, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("leaveCode", leave.getCode());
        map.put("leaveType", leave.getLeaveType());
        map.put("employeeId", leave.getEmployeeId());
        map.put("submitterUserId", resolveUserId(leave.getEmployeeId(), ctx));
        map.put("superiorId", targetId);
        map.put("superiorUserId", resolveUserId(targetId, ctx));
        notificationBiz.notify(ErpHrConstants.NOTIFY_EVENT_LEAVE_APPROVER_TIMEOUT, map, ctx);
    }

    /**
     * 员工 → 系统用户解析（userName 约定：{@code NopAuthUser.userName} == 员工工号 {@code code}）。
     * 无匹配用户时返回 null（通知接收人插值为空 → config-gated 跳过派发）。
     */
    protected String resolveUserId(Long employeeId, IServiceContext ctx) {
        if (employeeId == null) {
            return null;
        }
        ErpHrEmployee employee = employeeBiz.get(String.valueOf(employeeId), true, ctx);
        if (employee == null || StringHelper.isEmpty(employee.getCode())) {
            return null;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("userName", employee.getCode()));
        q.setLimit(1);
        NopAuthUser user = authUserBiz.findFirst(q, null, ctx);
        return user == null ? null : user.getUserId();
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpHrConstants.CONFIG_LEAVE_APPROVER_TIMEOUT_CRON, "");
    }

    protected long resolveTimeoutHours() {
        return AppConfig.var(ErpHrConstants.CONFIG_LEAVE_APPROVER_TIMEOUT_HOURS, 72);
    }
}
