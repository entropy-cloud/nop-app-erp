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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCtContract amend per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含合同修订编排（ACTIVE→DRAFT + 新建版本 max+1 + isCurrent 原子翻转）；共享 protected helper 已随编排迁入。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtContractAmendProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    public ErpCtContract amend(Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId);
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

    protected List<ErpCtContractVersion> findVersions(Long contractId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        List<ErpCtContractVersion> list = contractVersionBiz.findList(query, null, context);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
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
