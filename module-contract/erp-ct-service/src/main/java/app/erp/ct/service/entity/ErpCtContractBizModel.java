
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import app.erp.common.service.MaskHelper;
import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.contract.dao.entity.ErpCtInvoicePlan;
import app.erp.ct.biz.IErpCtApprovalRecordBiz;
import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.ct.biz.IErpCtContractLineBiz;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.biz.IErpCtInvoicePlanBiz;
import app.erp.ct.service.ErpCtConfigs;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.approval.ErpCtApprovalWorkflowEngine;
import app.erp.ct.service.processor.ErpCtContractActivateProcessor;
import app.erp.ct.service.processor.ErpCtContractAmendProcessor;
import app.erp.ct.service.statemachine.ErpCtContractStateMachine;
import app.erp.notify.biz.IErpSysNotificationBiz;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.dateBetween;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 合同头 BizModel。合同全生命周期状态机 + 版本修订编排
 * （对齐 {@code docs/design/contract/state-machine.md}）。
 *
 * <p>状态迁移：DRAFT→NEGOTIATION（submit 提交谈判，零版本建 v1）→ACTIVE（签署）、
 * ACTIVE↔SUSPENDED、ACTIVE→DRAFT（amend 修订）、DRAFT→ACTIVE（rejectAmend 驳回恢复）、
 * ACTIVE→EXPIRED/TERMINATED（终态）、NEGOTIATION→TERMINATED（谈判破裂终态，未生效合同放弃）。
 * 非法迁移抛 {@link ErpCtErrors#ERR_CT_ILLEGAL_STATUS_TRANSITION}。
 *
 * <p>跨实体版本操作经注入 {@link IErpCtContractVersionBiz}（核心零污染 + 走权限管道）；
 * 创建校验/提交校验经注入 {@link IErpCtContractLineBiz} 汇总行金额（RC-R1.32，D1 语义）。
 */
@BizModel("ErpCtContract")
public class ErpCtContractBizModel extends CrudBizModel<ErpCtContract> implements IErpCtContractBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpCtContractBizModel.class);

    @Inject
    IErpCtContractLineBiz contractLineBiz;

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    @Inject
    IErpCtInvoicePlanBiz contractInvoicePlanBiz;

    @Inject
    IErpCtApprovalRecordBiz approvalRecordBiz;

    @Inject
    ErpCtApprovalWorkflowEngine approvalEngine;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    @Inject
    ErpCtContractActivateProcessor activateProcessor;

    @Inject
    ErpCtContractAmendProcessor amendProcessor;

    @Inject
    ErpCtContractStateMachine stateMachine;

    public ErpCtContractBizModel() {
        setEntityName(ErpCtContract.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpCtContract> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        ErpCtContract entity = entityData.getEntity();
        if (entity.getBusinessDate() == null) {
            entity.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        }
        // 创建校验（D1 语义，RC-R1.32）：跨字段校验——totalAmount == Σ行金额 + startDate < endDate。
        // 行金额数据源 = in-memory to-many 实体图（entity.getLines()，仅同请求嵌套子表时非空——
        // 头先行行后加的 save 流在提交时经 submit 权威校验）；更新路径不校验（部分更新不触发全量重校验）。
        validateCreateFields(entity);
    }

    @Override
    @BizMutation
    public ErpCtContract submit(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        try {
            stateMachine.assertCanSubmitForNegotiation(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_DRAFT, e);
        }
        // 动态业务校验保留原位（D1 语义复用 + §2 前置条件「金额/条款/日期必填」由 submit 守卫生效）：
        // 行金额数据源 = DAO 查询（跨请求已落库行可靠，对齐 R1.8 totalHours 先例）。
        validateContractFields(contract, contractId, context);
        // 版本创建语义（D2/MAJOR-1）：零版本时自动创建 v1（versionNo=1、isCurrent=true、DRAFT）；
        // 已有版本时保留既有 DRAFT 当前版本不动（amend 场景：v2 已 isCurrent=true + DRAFT，submit 后维持，
        // 仅合同头 → NEGOTIATION）。activate 前置 finalizeVersion 契约（D6 watch-only 登记）。
        ensureVersionOnSubmit(contractId, context);
        contract.setStatus(stateMachine.submitTargetStatus());
        updateEntity(contract, null, context);
        // 审批引擎触发入口（RC-R1.34，UC-CT-07「经办人提交合同」）：config-gated——approval-enabled=false
        // 时零生成零阻塞（D1/Phase 2 Decision）；矩阵无匹配节点 = 无需审批（零记录）。
        generateApprovalRecordsIfEnabled(contract, context);
        return contract;
    }

    /**
     * 审批记录生成接线（RC-R1.34 submit 后置）：config-gated {@code erp-ct.approval-enabled}——
     * false 时跳过（既有行为零变化）；true 时按 totalAmount 匹配矩阵节点生成记录
     * （首 PENDING 其余 WAITING）并通知首节点审批人。
     */
    protected void generateApprovalRecordsIfEnabled(ErpCtContract contract, IServiceContext context) {
        if (!AppConfig.var(ErpCtConfigs.CFG_APPROVAL_ENABLED, false)) {
            return;
        }
        java.util.List<app.erp.contract.dao.entity.ErpCtApprovalMatrix> nodes =
                approvalEngine.matchByAmount(contract, context);
        if (nodes.isEmpty()) {
            return;
        }
        java.util.List<ErpCtApprovalRecord> records = approvalEngine.generateRecords(contract, nodes, context);
        if (!records.isEmpty()) {
            notifyApprovalTask(contract, records.get(0), context);
        }
    }

    @Override
    @BizMutation
    public ErpCtContract rejectAmend(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        try {
            stateMachine.assertCanRejectAmend(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_DRAFT, e);
        }
        // 恢复 D5 裁决目标（选项 B）：优先 status==SIGNED 中 versionNo 最大者，无 SIGNED 回落 FINALIZED 最大者
        // ——跨请求可行（amend 与 rejectAmend 独立事务，不依赖 amend 内存快照）+ 对重复 amend/reject 周期
        // 遗留 DRAFT 行免疫 + 防 finalize-then-reject 恢复未签署 FINALIZED 为 current。
        restoreCurrentVersion(contractId, context);
        contract.setStatus(stateMachine.rejectAmendTargetStatus());
        updateEntity(contract, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract activate(@Name("contractId") Long contractId, IServiceContext context) {
        return activateProcessor.activate(contractId, context);
    }

    @Override
    @BizMutation
    public ErpCtContract suspend(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        try {
            stateMachine.assertCanSuspend(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_ACTIVE, e);
        }
        contract.setStatus(stateMachine.suspendTargetStatus());
        updateEntity(contract, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract resume(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        try {
            stateMachine.assertCanResume(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_SUSPENDED, e);
        }
        contract.setStatus(stateMachine.resumeTargetStatus());
        updateEntity(contract, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract terminate(@Name("contractId") Long contractId,
                                   @Optional @Name("reason") String reason,
                                   @Optional @Name("attachmentId") Long attachmentId,
                                   IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        // 守卫接受 ACTIVE（生效合同提前终止）与 NEGOTIATION（谈判破裂放弃）两类源态
        // （对齐 state-machine.md §2 L34/L51 + §3 L58：NEGOTIATION 或后续态不可作废，只能 TERMINATED）。
        // 矩阵判定下沉 Bean（多源 {ACTIVE,NEGOTIATION}），非法边 Bean 抛 common 码，此处映射领域码。
        try {
            stateMachine.assertCanTerminate(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract,
                    ErpCtConstants.CONTRACT_STATUS_ACTIVE + "/" + ErpCtConstants.CONTRACT_STATUS_NEGOTIATION, e);
        }
        // 两段化（RC-R1.34，P1-RC-076，D1 选项 B）：发起终止申请 → 生成法务审批记录（PENDING），
        // 合同保持原状态；法务经 approveTermination 通过后执行终止操作，rejectTermination 驳回 → 原状态。
        // 法务门控为 state-machine.md §6 强制义务，不受 erp-ct.approval-enabled config 门控（D1 理由 1）。
        if (approvalEngine.hasPendingTermination(contractId, context)) {
            throw new NopException(ErpCtErrors.ERR_CT_TERMINATE_ALREADY_PENDING)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode());
        }
        ErpCtApprovalRecord record = approvalRecordBiz.newEntity();
        record.setContractId(contractId);
        record.setOrgId(contract.getOrgId());
        // approvalMatrixId=null = 终止法务记录判别（D1 选项 B，与链记录双轨区分）
        record.setApprovalOrder(1);
        record.setApproverId(approvalEngine.resolveApproverId(
                AppConfig.var(ErpCtConfigs.CFG_TERMINATE_APPROVER_ROLE,
                        ErpCtConfigs.DEFAULT_TERMINATE_APPROVER_ROLE), context));
        record.setApprovalStatus(ErpCtConstants.APPROVAL_STATUS_PENDING);
        record.setRemark(buildTerminationRemark(reason, attachmentId));
        approvalRecordBiz.saveEntity(record, null, context);
        notifyApprovalTask(contract, record, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract approveTermination(@Name("recordId") Long recordId,
                                            @Optional @Name("comment") String comment,
                                            IServiceContext context) {
        ErpCtApprovalRecord record = requireTerminationRecord(recordId, context);
        guardTerminationRecord(record, context);
        ErpCtContract contract = requireContract(record.getContractId(), context);
        // 执行终止操作（L1 UC-CT-06 step 3）：TERMINATED + 版本归档 + InvoicePlan 截停 + 善后 TODO 通知
        contract.setStatus(stateMachine.terminateTargetStatus());
        updateEntity(contract, null, context);
        archiveCurrentVersion(contract.getId(), context);
        haltUnexecutedInvoicePlans(contract.getId(), context);
        notifyWinddown(contract, record, context);
        // 审批记录通过态
        record.setApprovalStatus(ErpCtConstants.APPROVAL_STATUS_APPROVED);
        record.setApprovedAt(new Timestamp(CoreMetrics.currentTimeMillis()));
        if (comment != null) {
            record.setComment(comment);
        }
        approvalRecordBiz.updateEntity(record, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract rejectTermination(@Name("recordId") Long recordId,
                                           @Optional @Name("comment") String comment,
                                           IServiceContext context) {
        ErpCtApprovalRecord record = requireTerminationRecord(recordId, context);
        guardTerminationRecord(record, context);
        ErpCtContract contract = requireContract(record.getContractId(), context);
        // 法务驳回 → 合同保持原状态（L1 UC-CT-06 异常路径）
        record.setApprovalStatus(ErpCtConstants.APPROVAL_STATUS_REJECTED);
        record.setRejectedAt(new Timestamp(CoreMetrics.currentTimeMillis()));
        if (comment != null) {
            record.setComment(comment);
        }
        approvalRecordBiz.updateEntity(record, null, context);
        notifyTerminationRejected(contract, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract expire(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        try {
            stateMachine.assertCanExpire(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_ACTIVE, e);
        }
        contract.setStatus(stateMachine.expireTargetStatus());
        updateEntity(contract, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract amend(@Name("contractId") Long contractId, IServiceContext context) {
        return amendProcessor.amend(contractId, context);
    }

    @Override
    @BizQuery
    public List<ErpCtContract> scanExpiringContracts(@Optional @Name("warningDays") Integer warningDays,
                                                     IServiceContext context) {
        int window = warningDays != null ? warningDays : AppConfig.var(
                ErpCtConfigs.CFG_CONTRACT_EXPIRY_WARNING_DAYS_30,
                ErpCtConfigs.DEFAULT_CONTRACT_EXPIRY_WARNING_DAYS_30);
        LocalDate now = CoreMetrics.today();
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCtConstants.CONTRACT_STATUS_ACTIVE));
        q.addFilter(dateBetween("endDate", now, now.plusDays(window)));
        return findList(q, null, context);
    }

    /**
     * 批量推进已过期合同（UC-CT-05，RC-R1.35）：status=ACTIVE 且 endDate &lt; today 逐合同
     * expire（先 D3 未完成开票先完成，再 D4 config-gated 续期草稿，后 stateMachine 守卫置 EXPIRED）。
     * 逐条失败隔离（try/catch per contract，WARN 不阻断后续），对齐 hr
     * {@code ErpHrEmploymentContractExpireOverdueContractsProcessor} 范式。
     */
    @Override
    @BizMutation
    public List<ErpCtContract> expireOverdueContracts(IServiceContext context) {
        LocalDate now = CoreMetrics.today();
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCtConstants.CONTRACT_STATUS_ACTIVE));
        // endDate < now 语义：XMeta 过滤操作集不支持 lt/le（ObjMetaBasedFilterValidator 白名单
        // 仅 eq/in/dateBetween/dateTimeBetween——对齐 TriggerDuePlansProcessor:39-44 注记），
        // 以 dateBetween(epoch, today-1) 表达"早于 today"（无业务上早于 1970 的 endDate）。
        q.addFilter(dateBetween("endDate", LocalDate.of(1970, 1, 1), now.minusDays(1)));
        List<ErpCtContract> overdue = findList(q, null, context);
        if (overdue == null || overdue.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<ErpCtContract> expired = new java.util.ArrayList<>();
        for (ErpCtContract contract : overdue) {
            try {
                triggerDueInvoicesBeforeExpire(contract, context);
                createRenewalDraftIfEnabled(contract, context);
                // 复用既有状态机守卫语义（job 批量路径与手工 expire() 同一守卫）
                stateMachine.assertCanExpire(contract.getStatus());
                contract.setStatus(stateMachine.expireTargetStatus());
                updateEntity(contract, null, context);
                expired.add(contract);
            } catch (Exception ex) {
                LOG.warn("erp-ct-contract-expiry: 单条合同到期失败（隔离继续）：contractId={}, reason={}",
                        contract.getId(), ex.getMessage());
            }
        }
        return expired;
    }

    /**
     * D3 异常路径（L1「endDate 到达仍有未完成的开票计划 → 先完成开票再 EXPIRED」）：
     * expire 前对 isInvoiced=false 且 planDate ≤ today 的 InvoicePlan 逐条 triggerInvoice
     * （复用既有 Processor 生成 AP/AR 发票草稿）；触发失败逐条 try/catch 隔离，
     * 不影响 expire 主路径（D3 选项 A 裁决）。
     */
    protected void triggerDueInvoicesBeforeExpire(ErpCtContract contract, IServiceContext context) {
        List<ErpCtContractLine> lines = findLines(contract.getId(), context);
        if (lines.isEmpty()) {
            return;
        }
        List<Long> lineIds = new java.util.ArrayList<>();
        for (ErpCtContractLine line : lines) {
            lineIds.add(line.getId());
        }
        QueryBean query = new QueryBean();
        query.addFilter(io.nop.api.core.beans.FilterBeans.in("contractLineId", lineIds));
        query.addFilter(eq("isInvoiced", false));
        // planDate ≤ today 语义：dateBetween(epoch, today) 表达（同上白名单约束）
        query.addFilter(dateBetween("planDate", LocalDate.of(1970, 1, 1), CoreMetrics.today()));
        List<ErpCtInvoicePlan> plans = contractInvoicePlanBiz.findList(query, null, context);
        if (plans == null) {
            return;
        }
        for (ErpCtInvoicePlan plan : plans) {
            try {
                contractInvoicePlanBiz.triggerInvoice(plan.getId(), context);
            } catch (Exception ex) {
                LOG.warn("erp-ct-contract-expiry: 到期前开票触发失败（隔离继续）：contractId={}, planId={}, reason={}",
                        contract.getId(), plan.getId(), ex.getMessage());
            }
        }
    }

    /**
     * D4 续期草稿（config-gated {@code erp-ct.auto-create-renewal-draft} 默认 false；D4 选项 A 到期时创建）：
     * 为到期合同创建 DRAFT 续期草稿——合同头复制（code=原code+"-RN" 防 UK_CT_CONTRACT_CODE_ORG 冲突，
     * contractName/contractType/contractDirection/partnerId/currencyId/orgId/totalAmount 复制 +
     * startDate=原endDate+1 + endDate=原endDate+原时长）+ parentContractId 关联原合同。
     * 幂等守卫：已存在 parentContractId=原合同 且 status=DRAFT 的草稿时跳过（防重复运行重复建）。
     */
    protected void createRenewalDraftIfEnabled(ErpCtContract contract, IServiceContext context) {
        if (!AppConfig.var(ErpCtConfigs.CFG_AUTO_CREATE_RENEWAL_DRAFT,
                ErpCtConfigs.DEFAULT_AUTO_CREATE_RENEWAL_DRAFT)) {
            return;
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("parentContractId", contract.getId()));
        q.addFilter(eq("status", ErpCtConstants.CONTRACT_STATUS_DRAFT));
        List<ErpCtContract> existing = findList(q, null, context);
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        ErpCtContract draft = newEntity();
        draft.setCode(renewalDraftCode(contract.getCode()));
        draft.setOrgId(contract.getOrgId());
        draft.setContractName(contract.getContractName());
        draft.setContractType(contract.getContractType());
        draft.setContractDirection(contract.getContractDirection());
        draft.setPartnerId(contract.getPartnerId());
        draft.setCurrencyId(contract.getCurrencyId());
        draft.setTotalAmount(contract.getTotalAmount());
        if (contract.getStartDate() != null && contract.getEndDate() != null) {
            long durationDays = java.time.temporal.ChronoUnit.DAYS.between(contract.getStartDate(), contract.getEndDate());
            draft.setStartDate(contract.getEndDate().plusDays(1));
            draft.setEndDate(contract.getEndDate().plusDays(durationDays));
        }
        draft.setParentContractId(contract.getId());
        draft.setStatus(ErpCtConstants.CONTRACT_STATUS_DRAFT);
        draft.setBusinessDate(CoreMetrics.today());
        saveEntity(draft, null, context);
        LOG.info("erp-ct-contract-expiry: 自动创建续期草稿：parentContractId={}, draftCode={}",
                contract.getId(), draft.getCode());
    }

    /** 续期草稿 code：原 code + "-RN"（orderCode 精度 50，超长截断保后缀）。 */
    protected String renewalDraftCode(String originalCode) {
        String base = originalCode == null ? "" : originalCode;
        String suffix = "-RN";
        if (base.length() + suffix.length() <= 50) {
            return base + suffix;
        }
        return base.substring(0, 50 - suffix.length()) + suffix;
    }

    // ---------- helpers ----------

    protected ErpCtContract requireContract(Long contractId, IServiceContext context) {
        ErpCtContract contract = get(String.valueOf(contractId), false, context);
        if (contract == null) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId);
        }
        return contract;
    }

    // ---------- RC-R1.34 terminate 两段化 helpers（P1-RC-076） ----------

    /** 终止法务记录装载：存在 + approvalMatrixId=null（链记录归 ApprovalRecordBizModel 双轨）。 */
    protected ErpCtApprovalRecord requireTerminationRecord(Long recordId, IServiceContext context) {
        ErpCtApprovalRecord record = approvalRecordBiz.get(String.valueOf(recordId), false, context);
        if (record == null) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_RECORD_NOT_FOUND)
                    .param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, recordId);
        }
        if (record.getApprovalMatrixId() != null) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_ILLEGAL_STATUS)
                    .param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, recordId)
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, record.getApprovalStatus())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, "termination-record");
        }
        return record;
    }

    /** 终止记录守卫：PENDING + 审批人匹配（approverId 空 = 手工指定语义放行任意操作员）。 */
    protected void guardTerminationRecord(ErpCtApprovalRecord record, IServiceContext context) {
        if (!ErpCtConstants.APPROVAL_STATUS_PENDING.equals(record.getApprovalStatus())) {
            throw new NopException(ErpCtErrors.ERR_CT_APPROVAL_ILLEGAL_STATUS)
                    .param(ErpCtErrors.ARG_APPROVAL_RECORD_ID, record.getId())
                    .param(ErpCtErrors.ARG_CURRENT_STATUS, record.getApprovalStatus())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, ErpCtConstants.APPROVAL_STATUS_PENDING);
        }
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

    /** 终止申请 remark 承载（D1 裁决——零 ORM）：reason + 可选附件引用。 */
    protected String buildTerminationRemark(String reason, Long attachmentId) {
        StringBuilder sb = new StringBuilder();
        if (reason != null && !reason.isBlank()) {
            sb.append(reason);
        }
        if (attachmentId != null) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append("[附件:").append(attachmentId).append("]");
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    /** 当前版本归档（L1 UC-CT-06 step 3）：isCurrent=true 版本 → isCurrent=false。 */
    protected void archiveCurrentVersion(Long contractId, IServiceContext context) {
        ErpCtContractVersion current = findCurrentVersion(contractId, context);
        if (current != null && Boolean.TRUE.equals(current.getIsCurrent())) {
            current.setIsCurrent(false);
            contractVersionBiz.updateEntity(current, null, context);
        }
    }

    /**
     * InvoicePlan 显式截停（D4 选项 A）：未执行（isInvoiced=false）计划逐条逻辑删除
     * （useLogicalDelete 既有语义 delVersion=1，「标记作废」显式落库 + TERMINATED 隐式失效双保险）；
     * 已开票行保留（历史发票证据）。
     */
    protected void haltUnexecutedInvoicePlans(Long contractId, IServiceContext context) {
        List<ErpCtContractLine> lines = findLines(contractId, context);
        if (lines.isEmpty()) {
            return;
        }
        List<Long> lineIds = new java.util.ArrayList<>();
        for (ErpCtContractLine line : lines) {
            lineIds.add(line.getId());
        }
        QueryBean query = new QueryBean();
        query.addFilter(io.nop.api.core.beans.FilterBeans.in("contractLineId", lineIds));
        query.addFilter(eq("isInvoiced", false));
        List<ErpCtInvoicePlan> plans = contractInvoicePlanBiz.findList(query, null, context);
        if (plans == null) {
            return;
        }
        for (ErpCtInvoicePlan plan : plans) {
            contractInvoicePlanBiz.delete(String.valueOf(plan.getId()), context);
        }
    }

    /** 善后 TODO 通知（D5 选项 A）：事件 ct.terminate-winddown，接收人 = 合同经办人 createdBy。 */
    protected void notifyWinddown(ErpCtContract contract, ErpCtApprovalRecord record, IServiceContext context) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("submitterUserId", contract.getCreatedBy());
        map.put("terminationReason", record.getRemark());
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_TERMINATE_WINDDOWN, map, context);
    }

    /** 终止驳回通知（接收人 = 合同经办人）。 */
    protected void notifyTerminationRejected(ErpCtContract contract, IServiceContext context) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("submitterUserId", contract.getCreatedBy());
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_TERMINATE_REJECTED, map, context);
    }

    /** 审批待办通知（best-effort，无 ACTIVE 模板静默跳过 R1.4 范式）。 */
    protected void notifyApprovalTask(ErpCtContract contract, ErpCtApprovalRecord record, IServiceContext context) {
        if (notificationBiz == null || record == null) {
            return;
        }
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("contractId", contract.getId());
        map.put("contractCode", contract.getCode());
        map.put("approvalOrder", record.getApprovalOrder());
        map.put("approverUserId", record.getApproverId());
        notificationBiz.notify(ErpCtConstants.NOTIFY_EVENT_APPROVAL_TASK, map, context);
    }

    /**
     * 校验 contractType↔contractDirection 组合（{@code state-machine.md §审查提示}）：
     * PURCHASE→INBOUND、SALES→OUTBOUND。其他类型（EMPLOYMENT/SERVICE）不强制方向。
     */
    protected void validateTypeDirectionCombo(ErpCtContract contract) {
        String type = contract.getContractType();
        String direction = contract.getContractDirection();
        if (Objects.equals(type, ErpCtConstants.CONTRACT_TYPE_PURCHASE)
                && !Objects.equals(direction, ErpCtConstants.CONTRACT_DIRECTION_INBOUND)) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, ErpCtConstants.CONTRACT_DIRECTION_INBOUND);
        }
        if (Objects.equals(type, ErpCtConstants.CONTRACT_TYPE_SALES)
                && !Objects.equals(direction, ErpCtConstants.CONTRACT_DIRECTION_OUTBOUND)) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param(ErpCtErrors.ARG_EXPECTED_STATUS, ErpCtConstants.CONTRACT_DIRECTION_OUTBOUND);
        }
    }

    protected ErpCtContractVersion findCurrentVersion(Long contractId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        query.addFilter(eq("isCurrent", true));
        return contractVersionBiz.findFirst(query, null, context);
    }

    protected List<ErpCtContractVersion> findVersions(Long contractId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        return contractVersionBiz.findList(query, null, context);
    }

    // ---------- RC-R1.32 创建校验/版本族 helpers ----------

    /**
     * 创建校验（D1 语义，仅新建路径）：totalAmount == Σ行金额（有行时）+ startDate < endDate（两者非空时）。
     * 行金额数据源 = in-memory to-many 实体图（同请求嵌套子表）；无行时 totalAmount 可空亦可设。
     */
    protected void validateCreateFields(ErpCtContract contract) {
        validateDateRange(contract);
        io.nop.orm.IOrmEntitySet<ErpCtContractLine> lines = contract.getLines();
        if (lines == null || lines.isEmpty()) {
            return;
        }
        BigDecimal sum = sumLineAmounts(new java.util.ArrayList<>(lines));
        if (contract.getTotalAmount() == null || compareAmount(contract.getTotalAmount(), sum) != 0) {
            throw amountMismatch(contract, sum);
        }
    }

    /**
     * 提交校验（D1 语义复用，submit 权威门卫）：行金额数据源 = DAO 查询（跨请求已落库行可靠，
     * 对齐 R1.8 totalHours 先例 {@code ErpHrTimesheetBizModel#sumHoursByTimesheet}）。
     */
    protected void validateContractFields(ErpCtContract contract, Long contractId, IServiceContext context) {
        validateDateRange(contract);
        List<ErpCtContractLine> lines = findLines(contractId, context);
        if (lines.isEmpty()) {
            return;
        }
        BigDecimal sum = sumLineAmounts(lines);
        if (contract.getTotalAmount() == null || compareAmount(contract.getTotalAmount(), sum) != 0) {
            throw amountMismatch(contract, sum);
        }
    }

    /**
     * 版本创建语义（D2/MAJOR-1）：零版本时自动创建 v1（versionNo=1、isCurrent=true、VERSION_STATUS_DRAFT）；
     * 已有版本时零操作（保留既有 DRAFT 当前版本不动——amend 场景 v2 已 isCurrent=true + DRAFT）。
     */
    protected void ensureVersionOnSubmit(Long contractId, IServiceContext context) {
        List<ErpCtContractVersion> versions = findVersions(contractId, context);
        if (versions != null && !versions.isEmpty()) {
            return;
        }
        ErpCtContractVersion v1 = contractVersionBiz.newEntity();
        v1.setContractId(contractId);
        v1.setVersionNo(1);
        v1.setVersionDate(CoreMetrics.today());
        v1.setIsCurrent(true);
        v1.setStatus(ErpCtConstants.VERSION_STATUS_DRAFT);
        contractVersionBiz.saveEntity(v1, null, context);
    }

    /**
     * 恢复前任当前版本（D5 选项 B）：优先 status==SIGNED 中 versionNo 最大者，无 SIGNED 回落 FINALIZED 最大者；
     * 恢复目标 isCurrent=true，其余版本 isCurrent=false（原子翻转对齐 signVersion 语义）。
     * 边界（无 SIGNED/FINALIZED 候选——零版本 ACTIVE 合同 amend 后驳回）：清空遗留 DRAFT 版本 isCurrent，
     * 恢复「无 current 版本」前置不变量（防 ACTIVE+DRAFT-current 不一致态）。
     */
    protected void restoreCurrentVersion(Long contractId, IServiceContext context) {
        List<ErpCtContractVersion> versions = findVersions(contractId, context);
        if (versions == null || versions.isEmpty()) {
            return;
        }
        // 恢复目标（D5 选项 B）：优先 status==SIGNED 中 versionNo 最大者；无 SIGNED 回落 FINALIZED 最大者。
        ErpCtContractVersion signedMax = null;
        ErpCtContractVersion finalizedMax = null;
        for (ErpCtContractVersion v : versions) {
            if (ErpCtConstants.VERSION_STATUS_SIGNED.equals(v.getStatus())) {
                if (signedMax == null || greaterVersionNo(v, signedMax)) {
                    signedMax = v;
                }
            } else if (ErpCtConstants.VERSION_STATUS_FINALIZED.equals(v.getStatus())) {
                if (finalizedMax == null || greaterVersionNo(v, finalizedMax)) {
                    finalizedMax = v;
                }
            }
        }
        ErpCtContractVersion target = signedMax != null ? signedMax : finalizedMax;
        for (ErpCtContractVersion v : versions) {
            boolean isTarget = target != null && Objects.equals(v.getId(), target.getId());
            if (!Objects.equals(v.getIsCurrent(), isTarget)) {
                v.setIsCurrent(isTarget);
                contractVersionBiz.updateEntity(v, null, context);
            }
        }
    }

    private boolean greaterVersionNo(ErpCtContractVersion a, ErpCtContractVersion b) {
        return a.getVersionNo() != null
                && (b.getVersionNo() == null || a.getVersionNo() > b.getVersionNo());
    }

    protected List<ErpCtContractLine> findLines(Long contractId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        List<ErpCtContractLine> list = contractLineBiz.findList(query, null, context);
        return list == null ? java.util.Collections.emptyList() : list;
    }

    protected BigDecimal sumLineAmounts(java.util.List<ErpCtContractLine> lines) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpCtContractLine line : lines) {
            if (line.getAmount() != null) {
                sum = sum.add(line.getAmount());
            }
        }
        return sum;
    }

    /** 金额比对：双方 scale 4 HALF_UP 舍入后精确比较（容忍浮点舍入痕迹）。 */
    protected int compareAmount(BigDecimal a, BigDecimal b) {
        return a.setScale(4, RoundingMode.HALF_UP).compareTo(b.setScale(4, RoundingMode.HALF_UP));
    }

    protected void validateDateRange(ErpCtContract contract) {
        LocalDate start = contract.getStartDate();
        LocalDate end = contract.getEndDate();
        if (start != null && end != null && !start.isBefore(end)) {
            throw new NopException(ErpCtErrors.ERR_CT_DATE_RANGE_INVALID)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                    .param("startDate", start)
                    .param("endDate", end);
        }
    }

    protected NopException amountMismatch(ErpCtContract contract, BigDecimal sumLineAmount) {
        return new NopException(ErpCtErrors.ERR_CT_AMOUNT_MISMATCH)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                .param("totalAmount", contract.getTotalAmount())
                .param("sumLineAmount", sumLineAmount);
    }

    protected NopException illegalTransition(ErpCtContract contract, String expected) {
        return illegalTransition(contract, expected, null);
    }

    /**
     * 领域非法迁移异常构造。可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7：
     * Bean 报 common 码 + action/fromStatus 元数据，Processor 映射领域码 + 实体编号/上下文，common 码作 cause 保留）。
     */
    protected NopException illegalTransition(ErpCtContract contract, String expected, Throwable cause) {
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, contract.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 合同审批人/合同专员；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> CT_AMOUNT_ROLES = Set.of(MaskHelper.ROLE_CT_APPROVER, MaskHelper.ROLE_CT_CLERK);

    @BizLoader("totalAmount")
    public BigDecimal totalAmountMask(@ContextSource ErpCtContract entity) {
        return MaskHelper.maskDecimal(entity.getTotalAmount(), CT_AMOUNT_ROLES, entity, "totalAmount");
    }

}
