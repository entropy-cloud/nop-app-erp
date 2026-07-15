
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    public ErpCtContractVersionBizModel() {
        setEntityName(ErpCtContractVersion.class.getName());
    }

    @Override
    @BizMutation
    public ErpCtContractVersion finalizeVersion(@Name("versionId") Long versionId, IServiceContext context) {
        ErpCtContractVersion version = requireVersion(versionId, context);
        if (!Objects.equals(version.getStatus(), ErpCtConstants.VERSION_STATUS_DRAFT)) {
            throw illegalTransition(version, ErpCtConstants.VERSION_STATUS_DRAFT);
        }
        version.setStatus(ErpCtConstants.VERSION_STATUS_FINALIZED);
        updateEntity(version, null, context);
        return version;
    }

    @Override
    @BizMutation
    public ErpCtContractVersion signVersion(@Name("versionId") Long versionId, IServiceContext context) {
        ErpCtContractVersion version = requireVersion(versionId, context);
        // 仅当前版本可签署
        if (!Boolean.TRUE.equals(version.getIsCurrent())) {
            throw new NopException(ErpCtErrors.ERR_CT_VERSION_NOT_CURRENT)
                    .param(ErpCtErrors.ARG_CONTRACT_CODE, version.getContractId())
                    .param(ErpCtErrors.ARG_VERSION_NO, version.getVersionNo());
        }
        if (!Objects.equals(version.getStatus(), ErpCtConstants.VERSION_STATUS_FINALIZED)) {
            throw illegalTransition(version, ErpCtConstants.VERSION_STATUS_FINALIZED);
        }

        // 原子翻转：同合同其他版本 isCurrent=false
        IEntityDao<ErpCtContractVersion> dao = dao();
        for (ErpCtContractVersion sibling : findSiblings(version.getContractId(), context)) {
            if (!Objects.equals(sibling.getId(), version.getId()) && Boolean.TRUE.equals(sibling.getIsCurrent())) {
                sibling.setIsCurrent(false);
                dao.updateEntity(sibling);
            }
        }

        version.setStatus(ErpCtConstants.VERSION_STATUS_SIGNED);
        version.setIsCurrent(true);
        version.setApprovedAt(CoreMetrics.currentTimestamp());
        dao.updateEntity(version);
        return version;
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
        return new NopException(ErpCtErrors.ERR_CT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCtErrors.ARG_CONTRACT_CODE, version.getContractId())
                .param(ErpCtErrors.ARG_CURRENT_STATUS, version.getStatus())
                .param(ErpCtErrors.ARG_EXPECTED_STATUS, expected);
    }

}
