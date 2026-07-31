package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DepreciationPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * ErpAstDepreciationSchedule reverseDepreciation per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含反折旧编排（红冲凭证 + 回滚资产卡片累计折旧/净值 + 状态回退）；共享 protected helper 单一真相源在
 * {@link ErpAstDepreciationScheduleProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstDepreciationScheduleReverseDepreciationProcessor {

    @Inject
    ErpAstDepreciationScheduleProcessor facade;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    DepreciationPostingDispatcher postingDispatcher;

    public ErpAstDepreciationSchedule reverseDepreciation(Long assetId, String period, IServiceContext context) {
        ErpAstDepreciationSchedule schedule = facade.findSchedule(assetId, period);
        if (schedule == null || schedule.getStatus() == null
                || !Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED)) {
            throw new NopException(ErpAstErrors.ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, schedule != null ? schedule.getStatus() : null)
                    .param(ErpAstErrors.ARG_EXPECTED_STATUS, "EXECUTED");
        }
        ErpAstAsset asset = facade.requireAsset(assetId);
        if (Boolean.TRUE.equals(schedule.getPosted())) {
            postingDispatcher.reverse(asset, period);
        }
        BigDecimal oldAmount = ErpAstDepreciationScheduleProcessor.nz(schedule.getActualAmount());
        asset.setAccumulatedDepreciation(ErpAstDepreciationScheduleProcessor.nz(asset.getAccumulatedDepreciation()).subtract(oldAmount));
        asset.setNetBookValue(ErpAstDepreciationScheduleProcessor.nz(asset.getNetBookValue()).add(oldAmount));
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);

        schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_REVERSED);
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        daoProvider.daoFor(ErpAstDepreciationSchedule.class).saveOrUpdateEntity(schedule);
        return schedule;
    }
}
