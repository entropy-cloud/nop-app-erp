
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.common.service.MaskHelper;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.ct.biz.IErpCtContractLineBiz;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.processor.ErpCtContractActivateProcessor;
import app.erp.ct.service.processor.ErpCtContractAmendProcessor;
import app.erp.ct.service.statemachine.ErpCtContractStateMachine;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.biz.crud.EntityData;

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

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    @Inject
    IErpCtContractLineBiz contractLineBiz;

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
        return contract;
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
    public ErpCtContract terminate(@Name("contractId") Long contractId, IServiceContext context) {
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
        // 作废语义：InvoicePlan 无独立状态列，合同头 TERMINATED 后未开票计划经合同头隐式失效
        // （triggerInvoice 校验合同 ACTIVE 即拒绝，isInvoiced=false 永不可再触发）。
        // NEGOTIATION→TERMINATED 谈判破裂，未生效合同放弃，版本归档经 useLogicalDelete 既有语义
        // （NEGOTIATION 未生效，无需 signDate/version 归档差异；与 ACTIVE 路径仅 setStatus+updateEntity 一致）。
        contract.setStatus(stateMachine.terminateTargetStatus());
        updateEntity(contract, null, context);
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

    // ---------- helpers ----------

    protected ErpCtContract requireContract(Long contractId, IServiceContext context) {
        ErpCtContract contract = get(String.valueOf(contractId), false, context);
        if (contract == null) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId);
        }
        return contract;
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
