package app.erp.ast.service.processor;

import app.erp.ast.dao.ErpAstDaoConstants;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.audit.ErpAstAssetAuditRecorder;
import app.erp.ast.service.statemachine.ErpAstAssetStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpAstAsset）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpAstAsset suspend/resume per-mutation Processor（RC-R1.54，R6.3 {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 资产闲置状态机（L1 UC-AST-03）：suspend（IN_SERVICE→IDLE，暂停时点经 remark「闲置自 {date}」强制记录——
 * 闲置时长派生的时间基准）+ resume（IDLE→IN_SERVICE，恢复计提）。固定来源/目标态判断委托
 * {@link ErpAstAssetStateMachine} Bean（契约 §4；Bean 直抛领域码 + 调用点同码补参 assetCode，plan 2026-09-07-2200-1）。
 * 折旧行为语义由引擎侧天然满足：批量仅查 IN_SERVICE + validateAssetInService 拒绝 IDLE（闲置期间不计提）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstAssetSuspendResumeProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpAstAssetStateMachine assetStateMachine;

    @Inject
    ErpAstAssetAuditRecorder auditRecorder;

    /** 暂停时点 remark 标记前缀（闲置时长派生的时间基准，idleSince 列不落 ORM）。 */
    static final String IDLE_SINCE_PREFIX = "闲置自 ";

    public ErpAstAsset suspend(String assetId, IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        try {
            assetStateMachine.assertCanSuspend(asset.getStatus());
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
        String fromStatus = asset.getStatus();
        asset.setStatus(assetStateMachine.suspendTargetStatus());
        // 暂停时点强制记录（Phase 3 Decision：remark「闲置自 {date}」强制，非可选）
        String remark = asset.getRemark();
        String idleMark = IDLE_SINCE_PREFIX + CoreMetrics.today();
        asset.setRemark(remark == null || remark.trim().isEmpty() ? idleMark : remark + "；" + idleMark);
        assetDao().saveOrUpdateEntity(asset);
        auditRecorder.record(asset, ErpAstDaoConstants.AUDIT_EVENT_TYPE_STATUS_CHANGE,
                new ErpAstAssetAuditRecorder.Before(fromStatus, asset.getDepartmentId(), asset.getLocationId(), asset.getEmployeeId()),
                null, null, "Asset suspended (suspend)");
        return asset;
    }

    public ErpAstAsset resume(String assetId, IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        try {
            assetStateMachine.assertCanResume(asset.getStatus());
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
        String fromStatus = asset.getStatus();
        asset.setStatus(assetStateMachine.resumeTargetStatus());
        assetDao().saveOrUpdateEntity(asset);
        auditRecorder.record(asset, ErpAstDaoConstants.AUDIT_EVENT_TYPE_STATUS_CHANGE,
                new ErpAstAssetAuditRecorder.Before(fromStatus, asset.getDepartmentId(), asset.getLocationId(), asset.getEmployeeId()),
                null, null, "Asset resumed (resume)");
        return asset;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstAsset requireAsset(String assetId) {
        ErpAstAsset asset = assetDao().getEntityById(assetId);
        if (asset == null) {
            throw new NopException(ErpAstErrors.ERR_ASSET_NOT_FOUND)
                    .param(ErpAstErrors.ARG_ASSET_ID, assetId);
        }
        return asset;
    }

    protected IEntityDao<ErpAstAsset> assetDao() {
        return daoProvider.daoFor(ErpAstAsset.class);
    }
}
