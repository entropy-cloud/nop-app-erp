package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.statemachine.ErpCtContractStateMachine;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpCtContract amend per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含合同修订编排（ACTIVE→DRAFT + 新建版本 max+1 + isCurrent 原子翻转）；共享 protected helper 已随编排迁入。
 *
 * <p>固定来源态/目标态判断委托 {@link ErpCtContractStateMachine}（合同头 status 轴 Bean，契约 §4/§7）；
 * 动态业务副作用（新版本创建 + isCurrent 原子翻转）保留原位。非法边 Bean 抛 common 层码（含 {@code action}/
 * fromStatus 元数据），本 Processor 捕获后映射领域码 {@link ErpCtErrors#ERR_CT_ILLEGAL_STATUS_TRANSITION}
 * （+ contractCode/currentStatus/expectedStatus 实体编号/上下文，common 码作 cause 保留）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtContractAmendProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpCtContractVersionBiz contractVersionBiz;

    @Inject
    ErpCtContractStateMachine stateMachine;

    public ErpCtContract amend(Long contractId, IServiceContext context) {
        ErpCtContract contract = requireContract(contractId);
        try {
            stateMachine.assertCanAmend(contract.getStatus());
        } catch (NopException e) {
            throw illegalTransition(contract, ErpCtConstants.CONTRACT_STATUS_ACTIVE, e);
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
        contract.setStatus(stateMachine.amendTargetStatus());
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
        return illegalTransition(contract, expected, null);
    }

    /** 领域非法迁移异常构造；可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7）。 */
    protected NopException illegalTransition(ErpCtContract contract, String expected, Throwable cause) {
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, contract.getCode())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, contract.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected IEntityDao<ErpCtContract> dao() {
        return daoProvider.daoFor(ErpCtContract.class);
    }
}
