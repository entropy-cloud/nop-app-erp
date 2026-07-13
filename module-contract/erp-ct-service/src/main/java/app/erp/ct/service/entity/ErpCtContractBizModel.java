
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.biz.IErpCtContractBiz;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import io.nop.biz.crud.EntityData;

/**
 * 合同头 BizModel。合同全生命周期状态机 + 版本修订编排
 * （对齐 {@code docs/design/contract/state-machine.md}）。
 *
 * <p>状态迁移：DRAFT→NEGOTIATION→ACTIVE（签署）、ACTIVE↔SUSPENDED、
 * ACTIVE→DRAFT（amend 修订）、ACTIVE→EXPIRED/TERMINATED（终态）。
 * 非法迁移抛 {@link ErpCtErrors#ERR_CT_ILLEGAL_STATUS_TRANSITION}。
 *
 * <p>跨实体版本操作经注入 {@link IErpCtContractVersionBiz}（核心零污染 + 走权限管道）。
 */
@BizModel("ErpCtContract")
public class ErpCtContractBizModel extends CrudBizModel<ErpCtContract> implements IErpCtContractBiz {

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

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
        ErpCtContract contract = requireContract(contractId, context);
        if (!Objects.equals(contract.getStatus(), ErpCtConstants.CONTRACT_STATUS_NEGOTIATION)) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        }
        validateTypeDirectionCombo(contract);

        // 当前版本须已定稿（FINALIZED），则同步签署为 SIGNED；已签署则放行。
        ErpCtContractVersion current = findCurrentVersion(contract.getId(), context);
        if (current != null && Objects.equals(current.getStatus(), ErpCtConstants.VERSION_STATUS_FINALIZED)) {
            contractVersionBiz.signVersion(current.getId(), context);
        }

        contract.setStatus(ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        contract.setSignDate(CoreMetrics.today());
        updateEntity(contract, null, context);
        return contract;
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
        if (!Objects.equals(contract.getStatus(), ErpCtConstants.CONTRACT_STATUS_ACTIVE)) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        }
        // 作废语义：InvoicePlan 无独立状态列，合同头 TERMINATED 后未开票计划经合同头隐式失效
        // （triggerInvoice 校验合同 ACTIVE 即拒绝，isInvoiced=false 永不可再触发）。
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
        ErpCtContract contract = requireContract(contractId, context);
        if (!Objects.equals(contract.getStatus(), ErpCtConstants.CONTRACT_STATUS_ACTIVE)) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        }

        // 修订：新建版本（versionNo = max+1），原子翻转 isCurrent（旧版本 false，新版本 true）
        List<ErpCtContractVersion> versions = findVersions(contract.getId(), context);
        int maxVersionNo = 0;
        for (ErpCtContractVersion v : versions) {
            if (v.getVersionNo() != null && v.getVersionNo() > maxVersionNo) {
                maxVersionNo = v.getVersionNo();
            }
            if (Boolean.TRUE.equals(v.getIsCurrent())) {
                v.setIsCurrent(false);
                contractVersionBiz.updateEntity(v, null, context);
            }
        }

        ErpCtContractVersion newVersion = contractVersionBiz.newEntity();
        newVersion.setContractId(contract.getId());
        newVersion.setVersionNo(maxVersionNo + 1);
        newVersion.setVersionDate(CoreMetrics.today());
        newVersion.setIsCurrent(true);
        newVersion.setStatus(ErpCtConstants.VERSION_STATUS_DRAFT);
        contractVersionBiz.saveEntity(newVersion, null, context);

        // amend 期间合同头回 DRAFT
        contract.setStatus(ErpCtConstants.CONTRACT_STATUS_DRAFT);
        updateEntity(contract, null, context);
        return contract;
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

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtContract.class)
    public List<String> partnerName(@ContextSource List<ErpCtContract> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtContract row : rows) {
            result.add(row.orm_attached() && row.getPartner() != null ? row.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtContract.class)
    public List<String> templateName(@ContextSource List<ErpCtContract> rows) {
        orm().batchLoadProps(rows, Collections.singleton("template"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtContract row : rows) {
            result.add(row.orm_attached() && row.getTemplate() != null ? row.getTemplate().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtContract.class)
    public List<String> parentContractName(@ContextSource List<ErpCtContract> rows) {
        orm().batchLoadProps(rows, Collections.singleton("parentContract"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtContract row : rows) {
            result.add(row.orm_attached() && row.getParentContract() != null ? row.getParentContract().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtContract.class)
    public List<String> orgName(@ContextSource List<ErpCtContract> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtContract row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtContract.class)
    public List<String> currencyName(@ContextSource List<ErpCtContract> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtContract row : rows) {
            result.add(row.orm_attached() && row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

}
