package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.statemachine.ErpAstAssetStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstAsset suspend/resume per-mutation Processor（RC-R1.54，R6.3 {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 资产闲置状态机（L1 UC-AST-03）：suspend（IN_SERVICE→IDLE，暂停时点经 remark「闲置自 {date}」强制记录——
 * 闲置时长派生的时间基准）+ resume（IDLE→IN_SERVICE，恢复计提）。固定来源/目标态判断委托
 * {@link ErpAstAssetStateMachine} Bean（契约 §4/§7，common 码作 cause → 领域码映射）。
 * 折旧行为语义由引擎侧天然满足：批量仅查 IN_SERVICE + validateAssetInService 拒绝 IDLE（闲置期间不计提）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstAssetSuspendResumeProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpAstAssetStateMachine assetStateMachine;

    /** 暂停时点 remark 标记前缀（闲置时长派生的时间基准，idleSince 列不落 ORM）。 */
    static final String IDLE_SINCE_PREFIX = "闲置自 ";

    public ErpAstAsset suspend(String assetId, IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        try {
            assetStateMachine.assertCanSuspend(asset.getStatus());
        } catch (NopException e) {
            throw illegalTransition(asset, e);
        }
        asset.setStatus(assetStateMachine.suspendTargetStatus());
        // 暂停时点强制记录（Phase 3 Decision：remark「闲置自 {date}」强制，非可选）
        String remark = asset.getRemark();
        String idleMark = IDLE_SINCE_PREFIX + CoreMetrics.today();
        asset.setRemark(remark == null || remark.trim().isEmpty() ? idleMark : remark + "；" + idleMark);
        assetDao().saveOrUpdateEntity(asset);
        return asset;
    }

    public ErpAstAsset resume(String assetId, IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        try {
            assetStateMachine.assertCanResume(asset.getStatus());
        } catch (NopException e) {
            throw illegalTransition(asset, e);
        }
        asset.setStatus(assetStateMachine.resumeTargetStatus());
        assetDao().saveOrUpdateEntity(asset);
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

    /**
     * Bean common 码 → 领域码映射（common 作 cause 保留，契约 §7）。参数由本层组装，对外不变。
     */
    protected NopException illegalTransition(ErpAstAsset asset, NopException cause) {
        return new NopException(ErpAstErrors.ERR_AST_ASSET_ILLEGAL_STATUS_TRANSITION, cause)
                .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, asset.getStatus())
                .param(ErpAstErrors.ARG_EXPECTED_STATUS,
                        ErpAstConstants.ASSET_STATUS_IN_SERVICE + " 或 " + ErpAstConstants.ASSET_STATUS_IDLE);
    }
}
