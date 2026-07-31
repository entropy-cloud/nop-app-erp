package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCtContract activate per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含合同激活编排（NEGOTIATION→ACTIVE + 当前版本定稿则同步签署）；共享 protected helper 已随编排迁入。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtContractActivateProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    public ErpCtContract activate(Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId);
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
        dao().updateEntity(contract);
        return contract;
    }

    // ---------- helpers ----------

    protected ErpCtContract requireContract(Long contractId) {
        ErpCtContract contract = dao().getEntityById(contractId);
        if (contract == null) {
            throw new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpCtErrors.ARG_CONTRACT_ID, contractId);
        }
        return contract;
    }

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

    protected NopException illegalTransition(ErpCtContract contract, String expected) {
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, contract.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected IEntityDao<ErpCtContract> dao() {
        return daoProvider.daoFor(ErpCtContract.class);
    }
}
