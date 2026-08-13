
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.ct.service.ErpCtConstants;
import app.erp.ct.service.ErpCtErrors;
import app.erp.ct.service.processor.ErpCtContractVersionSignVersionProcessor;
import app.erp.ct.service.statemachine.ErpCtContractVersionStateMachine;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 合同版本 BizModel。版本状态机（定稿/签署）+ isCurrent 原子翻转
 * （对齐 {@code docs/design/contract/state-machine.md} §版本管理）。
 *
 * <p>签署（signVersion）原子操作：目标版本置 SIGNED + isCurrent=true，
 * 同合同其他版本 isCurrent=false。
 */
@BizModel("ErpCtContractVersion")
public class ErpCtContractVersionBizModel extends CrudBizModel<ErpCtContractVersion>
        implements IErpCtContractVersionBiz {

    @Inject
    ErpCtContractVersionSignVersionProcessor signVersionProcessor;

    @Inject
    ErpCtContractVersionStateMachine stateMachine;

    public ErpCtContractVersionBizModel() {
        setEntityName(ErpCtContractVersion.class.getName());
    }

    @Override
    @BizMutation
    public ErpCtContractVersion finalizeVersion(@Name("versionId") Long versionId, IServiceContext context) {
        ErpCtContractVersion version = requireVersion(versionId, context);
        try {
            stateMachine.assertCanFinalize(version.getStatus());
        } catch (NopException e) {
            throw illegalTransition(version, ErpCtConstants.VERSION_STATUS_DRAFT, e);
        }
        version.setStatus(stateMachine.finalizeTargetStatus());
        updateEntity(version, null, context);
        return version;
    }

    @Override
    @BizMutation
    public ErpCtContractVersion signVersion(@Name("versionId") Long versionId, IServiceContext context) {
        return signVersionProcessor.signVersion(versionId, context);
    }

    // ---------- helpers ----------

    protected ErpCtContractVersion requireVersion(Long versionId, IServiceContext context) {
        ErpCtContractVersion version = get(String.valueOf(versionId), false, context);
        if (version == null) {
            throw new NopException(ErpCtErrors.ERR_CT_VERSION_NOT_CURRENT)
                    .param(ErpCtErrors.ARG_VERSION_NO, versionId);
        }
        return version;
    }

    @SuppressWarnings("unchecked")
    protected List<ErpCtContractVersion> findSiblings(Long contractId, IServiceContext context) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("contractId", contractId));
        List<ErpCtContractVersion> list = findList(query, null, context);
        return list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    protected NopException illegalTransition(ErpCtContractVersion version, String expected) {
        return illegalTransition(version, expected, null);
    }

    /**
     * 领域非法迁移异常构造。可选 {@code cause} 保留 Bean 抛出的 common 层非法边报告（契约 §7：
     * Bean 报 common 码 + action/fromStatus 元数据，BizModel/Processor 映射领域码 + 实体编号/上下文，common 码作 cause 保留）。
     */
    protected NopException illegalTransition(ErpCtContractVersion version, String expected, Throwable cause) {
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, version.getContractId())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, version.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

}
