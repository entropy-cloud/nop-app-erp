
package app.erp.ct.service.entity;

import app.erp.contract.dao.entity.ErpCtApprovalMatrix;
import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.ct.biz.IErpCtApprovalRecordBiz;
import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.approval.ErpCtApprovalWorkflowEngine;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 审批记录 BizModel（RC-R1.34，P1-RC-077，UC-CT-07）。
 *
 * <p>approve/reject/resubmit 三 mutation 编排链记录（approvalMatrixId != null）：
 * 引擎（{@link ErpCtApprovalWorkflowEngine}）承载匹配/生成/链状态推导，本类承载
 * 逐节点审批 + D3 超限锁定 + 通知派发。terminate 法务记录（approvalMatrixId=null）
 * 归 {@code ErpCtContractBizModel#approveTermination/rejectTermination}（D1 选项 B）。
 */
@BizModel("ErpCtApprovalRecord")
public class ErpCtApprovalRecordBizModel extends CrudBizModel<ErpCtApprovalRecord> implements IErpCtApprovalRecordBiz{

    @Inject
    ErpCtApprovalWorkflowEngine engine;

    @Inject
    IErpCtContractBiz contractBiz;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    public ErpCtApprovalRecordBizModel(){
        setEntityName(ErpCtApprovalRecord.class.getName());
    }

    @Override
    @BizMutation
    public ErpCtApprovalRecord approve(@Name("recordId") String recordId,
                                       @Optional @Name("comment") String comment,
                                       IServiceContext context) {
        ErpCtApprovalRecord record = requireChainRecord(recordId, context);
        guardPending(record);
        guardApprover(record, context);
        guardNotLocked(record.getContractId(), record.getApprovalOrder(), context);
        record.setApprovalStatus(ErpCtConstants.APPROVAL_STATUS_APPROVED);
        record.setApprovedAt(new Timestamp(CoreMetrics.currentTimeMillis()));
        if (comment != null) {
            record.setComment(comment);
        }
        updateEntity(record, null, context);
        activateNext(record.getContractId(), record.getApprovalOrder(), context);
        return record;
    }

    @Override
    @BizMutation
    public ErpCtApprovalRecord reject(@Name("recordId") String recordId,
                                      @Optional @Name("comment") String comment,
                                      IServiceContext context) {
        ErpCtApprovalRecord record = requireChainRecord(recordId, context);
        guardPending(record);
        guardApprover(record, context);
        guardNotLocked(record.getContractId(), record.getApprovalOrder(), context);
        record.setApprovalStatus(ErpCtConstants.APPROVAL_STATUS_REJECTED);
        record.setRejectedAt(new Timestamp(CoreMetrics.currentTimeMillis()));
        if (comment != null) {
            record.setComment(comment);
        }
        updateEntity(record, null, context);
        ErpCtContract contract = findContract(record.getContractId(), context);
        notifyRejected(contract, context);
        if (engine.rejectedCount(record.getContractId(), record.getApprovalOrder(), context)
                >= resolveMaxRetries()) {
            notifyLocked(contract, context);
        }
        return record;
    }

    @Override
    @BizMutation
    public int resubmit(@Name("contractId") String contractId, IServiceContext context) {
        ErpCtContract contract = findContract(contractId, context);
        if (contract == null
                || !ErpCtConstants.CONTRACT_STATUS_NEGOTIATION.equals(contract.getStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract == null ? null : contract.getCode())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, contract == null ? null : contract.getStatus())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        }
        ErpCtApprovalRecord rejected = engine.latestRejected(contractId, context);
        if (rejected == null) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_NO_REJECTED)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId);
        }
        guardNotLocked(contractId, rejected.getApprovalOrder(), context);
        List<ErpCtApprovalMatrix> nodes = engine.matchByAmount(contract, context);
        if (nodes.isEmpty()) {
            return 0;
        }
        int reactivated = 0;
        Integer fromOrder = rejected.getApprovalOrder();
        ErpCtApprovalRecord first = null;
        for (ErpCtApprovalMatrix node : nodes) {
            if (node.getApprovalOrder() < fromOrder) {
                continue;
            }
            ErpCtApprovalRecord record = newResubmitRecord(contract, node, context);
            record.setApprovalStatus(first == null
                    ? ErpCtConstants.APPROVAL_STATUS_PENDING
                    : ErpCtConstants.APPROVAL_STATUS_WAITING);
            saveEntity(record, null, context);
            if (first == null) {
                first = record;
            }
            reactivated++;
        }
        if (first != null) {
            notifyTask(contract, first, context);
        }
        return reactivated;
    }

    // ---------- guards ----------

    /** 链记录守卫：记录存在 + approvalMatrixId != null（terminate 记录走 contract BizModel 双轨）。 */
    protected ErpCtApprovalRecord requireChainRecord(String recordId, IServiceContext context) {
        ErpCtApprovalRecord record = get(recordId, false, context);
        if (record == null) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_RECORD_NOT_FOUND)
                    .param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, recordId);
        }
        if (record.getApprovalMatrixId() == null) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_ILLEGAL_STATUS)
                    .param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, recordId)
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, record.getApprovalStatus())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, "chain-record");
        }
        return record;
    }

    protected void guardPending(ErpCtApprovalRecord record) {
        if (!ErpCtConstants.APPROVAL_STATUS_PENDING.equals(record.getApprovalStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_ILLEGAL_STATUS)
                    .param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, record.getId())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, record.getApprovalStatus())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, ErpCtConstants.APPROVAL_STATUS_PENDING);
        }
    }

    /** 审批人守卫：approverId 非空时须 == 当前操作人；空（D2 无命中手工指定语义）放行任意操作员。 */
    protected void guardApprover(ErpCtApprovalRecord record, IServiceContext context) {
        String approverId = record.getApproverId();
        if (approverId == null || approverId.isBlank()) {
            return;
        }
        String userId = context == null ? null : context.getUserId();
        if (!approverId.equals(userId)) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_APPROVER_MISMATCH)
                    .param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, record.getId())
                    .param(ErpCtErrors.ARG_APPROVER_ID, approverId)
                    .param(ErpCtErrors.ARG_USER_ID, userId);
        }
    }

    /** D3 超限锁定守卫：派生驳回计数 ≥ max-retries 时拒绝（锁定需强制升级）。 */
    protected void guardNotLocked(String contractId, Integer approvalOrder, IServiceContext context) {
        if (engine.rejectedCount(contractId, approvalOrder, context) >= resolveMaxRetries()) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_LOCKED)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId)
                    .param(ErpCtErrors.ARG_APPROVAL_ORDER, approvalOrder)
                    .param(ErpCtErrors.ARG_MAX_RETRIES, resolveMaxRetries());
        }
    }

    // ---------- helpers ----------

    /** 激活下一节点：approvalOrder 大于当前节点的最小 order 的最新记录 → PENDING + 通知。 */
    protected void activateNext(String contractId, Integer currentOrder, IServiceContext context) {
        Integer nextOrder = null;
        for (ErpCtApprovalRecord record : engine.findRecords(contractId, context)) {
            if (record.getApprovalMatrixId() == null || record.getApprovalOrder() == null) {
                continue;
            }
            if (record.getApprovalOrder() > currentOrder
                    && (nextOrder == null || record.getApprovalOrder() < nextOrder)) {
                nextOrder = record.getApprovalOrder();
            }
        }
        if (nextOrder == null) {
            return;
        }
        ErpCtApprovalRecord next = engine.latestRecord(contractId, nextOrder, context);
        if (next == null || !ErpCtConstants.APPROVAL_STATUS_WAITING.equals(next.getApprovalStatus())) {
            return;
        }
        next.setApprovalStatus(ErpCtConstants.APPROVAL_STATUS_PENDING);
        updateEntity(next, null, context);
        notifyTask(findContract(contractId, context), next, context);
    }

    /** D7 resubmit 追加行：新记录行（approvalMatrixId 沿用节点矩阵 id，approverId 重新按 D2 解析）。 */
    protected ErpCtApprovalRecord newResubmitRecord(ErpCtContract contract, ErpCtApprovalMatrix node,
                                                    IServiceContext context) {
        ErpCtApprovalRecord record = newEntity();
        record.setContractId(contract.getId());
        record.setOrgId(contract.getOrgId());
        record.setApprovalMatrixId(node.getId());
        record.setApprovalOrder(node.getApprovalOrder());
        record.setApproverId(engine.resolveApproverId(node.getApproverRole(), context));
        return record;
    }

    protected ErpCtContract findContract(String contractId, IServiceContext context) {
        if (contractId == null) {
            return null;
        }
        return contractBiz.get(contractId, true, context);
    }

    protected int resolveMaxRetries() {
        return AppConfig.var(ErpCtConfigs.CFG_APPROVAL_MAX_RETRIES, ErpCtConfigs.DEFAULT_APPROVAL_MAX_RETRIES);
    }

    // ---------- 通知（best-effort，无 ACTIVE 模板静默跳过 R1.4 范式） ----------

    protected void notifyTask(ErpCtContract contract, ErpCtApprovalRecord record, IServiceContext context) {
        if (notificationBiz == null || contract == null || record == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("approvalOrder", record.getApprovalOrder());
        map.put("approverUserId", record.getApproverId());
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_APPROVAL_TASK, map, context);
    }

    protected void notifyRejected(ErpCtContract contract, IServiceContext context) {
        if (notificationBiz == null || contract == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("submitterUserId", contract.getCreatedBy());
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_APPROVAL_REJECTED, map, context);
    }

    protected void notifyLocked(ErpCtContract contract, IServiceContext context) {
        if (notificationBiz == null || contract == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("submitterUserId", contract.getCreatedBy());
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_APPROVAL_LOCKED, map, context);
    }
}
