package app.erp.ct.service.processor;

import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.statemachine.ErpCtContractVersionStateMachine;
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
 * ErpCtContractVersion signVersion per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含合同版本签署编排（FINALIZED→SIGNED + isCurrent 原子翻转）；共享 protected helper 已随编排迁入。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpCtContractVersionSignVersionProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCtContractVersionStateMachine stateMachine;

    public ErpCtContractVersion signVersion(String versionId, IServiceContext context) {
        ErpCtContractVersion version = requireVersion(versionId);
        // 仅当前版本可签署（动态业务守卫，保留原位）
        if (!Boolean.TRUE.equals(version.getIsCurrent())) {
            throw new NopException(ErpCtErrors.ERR_CT_VERSION_NOT_CURRENT)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, version.getContractId())
                    .param(ErpCtErrors.ARG_VERSION_NO, version.getVersionNo());
        }
        try {
            stateMachine.assertCanSign(version.getStatus());
        } catch (NopException e) {
            throw illegalTransition(version, ErpCtConstants.VERSION_STATUS_FINALIZED, e);
        }

        // 原子翻转：同合同其他版本 isCurrent=false
        IEntityDao<ErpCtContractVersion> dao = dao();
        for (ErpCtContractVersion sibling : findSiblings(version.getContractId())) {
            if (!Objects.equals(sibling.getId(), version.getId()) && Boolean.TRUE.equals(sibling.getIsCurrent())) {
                sibling.setIsCurrent(false);
                dao.updateEntity(sibling);
            }
        }

        version.setStatus(stateMachine.signTargetStatus());
        version.setIsCurrent(true);
        version.setApprovedAt(CoreMetrics.currentTimestamp());
        dao.updateEntity(version);
        return version;
    }

    // ---------- helpers ----------

    protected ErpCtContractVersion requireVersion(String versionId) {
        ErpCtContractVersion version = dao().getEntityById(versionId);
        if (version == null) {
            throw new NopException(ErpCtErrors.ERR_CT_VERSION_NOT_CURRENT)
                    .param(ErpCtErrors.ARG_VERSION_NO, versionId);
        }
        return version;
    }

    @SuppressWarnings("unchecked")
    protected List<ErpCtContractVersion> findSiblings(String contractId) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        List<ErpCtContractVersion> list = dao().findAllByQuery(query);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    protected NopException illegalTransition(ErpCtContractVersion version, String expected) {
        return illegalTransition(version, expected, null);
    }

    /**
     * 领域非法迁移异常构造。可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7：
     * Bean 报 common 码 + action/fromStatus 元数据，Processor 映射领域码 + 实体编号/上下文，common 码作 cause 保留）。
     */
    protected NopException illegalTransition(ErpCtContractVersion version, String expected, Throwable cause) {
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, version.getContractId())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, version.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected IEntityDao<ErpCtContractVersion> dao() {
        return daoProvider.daoFor(ErpCtContractVersion.class);
    }
}
