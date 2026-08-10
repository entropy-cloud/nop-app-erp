
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
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.processor.ErpCtContractActivateProcessor;
import app.erp.ct.service.processor.ErpCtContractAmendProcessor;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.biz.crud.EntityData;

/**
 * 合同头 BizModel。合同全生命周期状态机 + 版本修订编排
 * （对齐 {@code docs/design/contract/state-machine.md}）。
 *
 * <p>状态迁移：DRAFT→NEGOTIATION→ACTIVE（签署）、ACTIVE↔SUSPENDED、
 * ACTIVE→DRAFT（amend 修订）、ACTIVE→EXPIRED/TERMINATED（终态）、
 * NEGOTIATION→TERMINATED（谈判破裂终态，未生效合同放弃）。
 * 非法迁移抛 {@link ErpCtErrors#ERR_CT_ILLEGAL_STATUS_TRANSITION}。
 *
 * <p>跨实体版本操作经注入 {@link IErpCtContractVersionBiz}（核心零污染 + 走权限管道）。
 */
@BizModel("ErpCtContract")
public class ErpCtContractBizModel extends CrudBizModel<ErpCtContract> implements IErpCtContractBiz {

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    @Inject
    ErpCtContractActivateProcessor activateProcessor;

    @Inject
    ErpCtContractAmendProcessor amendProcessor;

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
        if (!Objects.equals(contract.getStatus(), ErpCtConstants.CONTRACT_STATUS_ACTIVE)) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        }
        contract.setStatus(ErpCtConstants.CONTRACT_STATUS_SUSPENDED);
        updateEntity(contract, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract resume(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        if (!Objects.equals(contract.getStatus(), ErpCtConstants.CONTRACT_STATUS_SUSPENDED)) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_SUSPENDED);
        }
        contract.setStatus(ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        updateEntity(contract, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract terminate(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        String status = contract.getStatus();
        // 守卫接受 ACTIVE（生效合同提前终止）与 NEGOTIATION（谈判破裂放弃）两类源态
        // （对齐 state-machine.md §2 L34/L51 + §3 L58：NEGOTIATION 或后续态不可作废，只能 TERMINATED）。
        if (!Objects.equals(status, ErpCtConstants.CONTRACT_STATUS_ACTIVE)
                && !Objects.equals(status, ErpCtConstants.CONTRACT_STATUS_NEGOTIATION)) {
            throw illegalTransition(contract,
                    ErpCtConstants.CONTRACT_STATUS_ACTIVE + "/" + ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        }
        // 作废语义：InvoicePlan 无独立状态列，合同头 TERMINATED 后未开票计划经合同头隐式失效
        // （triggerInvoice 校验合同 ACTIVE 即拒绝，isInvoiced=false 永不可再触发）。
        // NEGOTIATION→TERMINATED 谈判破裂，未生效合同放弃，版本归档经 useLogicalDelete 既有语义
        // （NEGOTIATION 未生效，无需 signDate/version 归档差异；与 ACTIVE 路径仅 setStatus+updateEntity 一致）。
        contract.setStatus(ErpCtConstants.CONTRACT_STATUS_TERMINATED);
        updateEntity(contract, null, context);
        return contract;
    }

    @Override
    @BizMutation
    public ErpCtContract expire(@Name("contractId") Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId, context);
        if (!Objects.equals(contract.getStatus(), ErpCtConstants.CONTRACT_STATUS_ACTIVE)) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        }
        contract.setStatus(ErpCtConstants.CONTRACT_STATUS_EXPIRED);
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

    protected NopException illegalTransition(ErpCtContract contract, String expected) {
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, contract.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 合同审批人/合同专员；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> CT_AMOUNT_ROLES = Set.of(MaskHelper.ROLE_CT_APPROVER, MaskHelper.ROLE_CT_CLERK);

    @BizLoader("totalAmount")
    public BigDecimal totalAmountMask(@ContextSource ErpCtContract entity) {
        return MaskHelper.maskDecimal(entity.getTotalAmount(), CT_AMOUNT_ROLES);
    }

}
