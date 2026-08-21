package app.erp.ct.service.job;

import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.ct.biz.IErpCtApprovalRecordBiz;
import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.approval.ErpCtApprovalWorkflowEngine;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
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
 * 合同审批超时升级 Job Bean（RC-R1.34，P1-RC-077 ④，UC-CT-07 异常「超时未处理（默认 72h）升级通知上一级」）。
 *
 * <p>R1.4 简单 job bean 范式（D6 裁决）：由 nop-job-local 的 {@code scheduler.yaml} 经
 * BeanMethodJobInvoker 反射调用 {@link #execute()}，触发频率由
 * {@code erp-ct-approval-timeout.job.yaml} 的 cronExpr 决定（默认每日 01:00）。
 *
 * <p>双层门控：job.yaml {@code nop.job.erp-ct-approval-timeout.enabled|cron-expr}（调度级）+
 * bean 内 {@code erp-ct.approval-timeout-cron} 配置空值跳过（「不调度」语义，对齐
 * {@code ErpHrLeaveApproverTimeoutJob} 双键范式）。扫描 PENDING 且 updateTime 超
 * {@code erp-ct.approval-timeout-hours}（默认 72）的审批记录，逐条升级通知：
 * 链记录（approvalMatrixId != null）→ 上一节点审批人（approvalOrder-1 最新记录 approverId），
 * 无则合同经办人；终止法务记录（approvalMatrixId=null）→ 合同经办人。单条失败隔离
 * （try/catch per record，不阻断后续）。
 */
public class ErpCtApprovalTimeoutEscalationJob {
    static final Logger LOG = LoggerFactory.getLogger(ErpCtApprovalTimeoutEscalationJob.class);

    /** 单次扫描最大条数（分页 limit 保护）。 */
    static final int SCAN_LIMIT = 200;

    @Inject
    IErpCtApprovalRecordBiz approvalRecordBiz;
    @Inject
    IErpCtContractBiz contractBiz;
    @Inject
    ErpCtApprovalWorkflowEngine approvalEngine;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    IOrmTemplate ormTemplate;

    public void setApprovalRecordBiz(IErpCtApprovalRecordBiz approvalRecordBiz) {
        this.approvalRecordBiz = approvalRecordBiz;
    }

    public void setContractBiz(IErpCtContractBiz contractBiz) {
        this.contractBiz = contractBiz;
    }

    public void setApprovalEngine(ErpCtApprovalWorkflowEngine approvalEngine) {
        this.approvalEngine = approvalEngine;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /**
     * 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。
     * cron 空值跳过；非空时扫描超时 PENDING 记录并逐条派发升级通知。
     */
    public void execute() {
        String cron = resolveCronConfig();
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-ct-approval-timeout-skipped: cron config empty (erp-ct.approval-timeout-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            int escalated = runTimeoutEscalation(ctx);
            LOG.info("erp-ct-approval-timeout-done: escalated={}", escalated);
        } catch (Exception e) {
            LOG.error("erp-ct-approval-timeout-failed", e);
        }
    }

    /**
     * 扫描超时 PENDING 审批记录并逐条升级通知；返回成功派发条数。
     * 扫描与读取在同一 ORM session 内完成（findList 返回的实体保持 MANAGED）。
     */
    protected int runTimeoutEscalation(IServiceContext ctx) {
        long timeoutHours = resolveTimeoutHours();
        LocalDateTime cutoff = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(CoreMetrics.currentTimeMillis() - timeoutHours * 3600_000L),
                java.time.ZoneId.systemDefault());
        return ormTemplate.runInSession(session -> {
            QueryBean q = new QueryBean();
            q.addFilter(eq("approvalStatus", ErpCtConstants.APPROVAL_STATUS_PENDING));
            // updateTime < now - timeoutHours 语义：以 datetimeBetween(epoch, cutoff) 表达
            // （对齐 ErpHrLeaveApproverTimeoutJob:126 先例，XMeta 过滤操作集不支持 lt）。
            q.addFilter(dateTimeBetween("updateTime",
                    LocalDateTime.of(1970, 1, 1, 0, 0), cutoff));
            q.setLimit(SCAN_LIMIT);
            List<ErpCtApprovalRecord> records = approvalRecordBiz.findList(q, null, ctx);
            if (records == null || records.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (ErpCtApprovalRecord record : records) {
                try {
                    if (escalateRecord(record, ctx)) {
                        count++;
                    }
                } catch (Exception e) {
                    LOG.warn("erp-ct-approval-timeout: 单条审批升级失败（隔离继续）：recordId={}, reason={}",
                            record.getId(), e.getMessage());
                }
            }
            return count;
        });
    }

    /**
     * 单条超时升级：解析升级接收人 → 派发通知。返回 true 表示已派发。
     * 链记录（approvalMatrixId != null）→ 上一节点审批人（approvalOrder-1 最新记录 approverId）；
     * 无上一节点或终止法务记录（approvalMatrixId=null）→ 合同经办人（createdBy）。
     */
    protected boolean escalateRecord(ErpCtApprovalRecord record, IServiceContext ctx) {
        String escalationUserId = resolveEscalationUserId(record, ctx);
        if (escalationUserId == null) {
            LOG.warn("erp-ct-approval-timeout: 无升级接收人（无上一节点审批人且无合同经办人），跳过：recordId={}",
                    record.getId());
            return false;
        }
        notifyEscalation(record, escalationUserId, ctx);
        return true;
    }

    /** 升级接收人解析：链记录取上一节点（approvalOrder-1）最新记录 approverId；兜底合同经办人 createdBy。 */
    protected String resolveEscalationUserId(ErpCtApprovalRecord record, IServiceContext ctx) {
        if (record.getApprovalMatrixId() != null && record.getApprovalOrder() != null
                && record.getApprovalOrder() > 1) {
            ErpCtApprovalRecord prev = approvalEngine.latestRecord(
                    record.getContractId(), record.getApprovalOrder() - 1, ctx);
            if (prev != null && !StringHelper.isEmpty(prev.getApproverId())) {
                return prev.getApproverId();
            }
        }
        ErpCtContract contract = findContract(record.getContractId(), ctx);
        return contract == null ? null : contract.getCreatedBy();
    }

    /**
     * 派发超时升级通知（config-gated：无 ACTIVE 模板时 notify 内部静默跳过）。
     * context 键对齐 {@code ct.approval-timeout-escalation} 模板约定：contractId/contractCode/
     * approvalOrder/escalationUserId，USER_LIST 接收人经 ${escalationUserId} 插值。
     */
    protected void notifyEscalation(ErpCtApprovalRecord record, String escalationUserId, IServiceContext ctx) {
        if (notificationBiz == null) {
            return;
        }
        ErpCtContract contract = findContract(record.getContractId(), ctx);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", record.getContractId());
        map.put("contractCode", contract == null ? null : contract.getCode());
        map.put("approvalOrder", record.getApprovalOrder());
        map.put("escalationUserId", escalationUserId);
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_APPROVAL_TIMEOUT_ESCALATION, map, ctx);
    }

    protected ErpCtContract findContract(String contractId, IServiceContext ctx) {
        if (contractId == null) {
            return null;
        }
        return contractBiz.get(contractId, true, ctx);
    }

    protected String resolveCronConfig() {
        return AppConfig.var(ErpCtConfigs.CFG_APPROVAL_TIMEOUT_CRON, "");
    }

    protected long resolveTimeoutHours() {
        return AppConfig.var(ErpCtConfigs.CFG_APPROVAL_TIMEOUT_HOURS, ErpCtConfigs.DEFAULT_APPROVAL_TIMEOUT_HOURS);
    }
}
