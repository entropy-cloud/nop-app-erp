package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DepreciationPostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstDepreciationScheduleStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;

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

    @Inject
    ErpAstDepreciationScheduleStateMachine scheduleStateMachine;

    public ErpAstDepreciationSchedule reverseDepreciation(Long assetId, String period, IServiceContext context) {
        ErpAstDepreciationSchedule schedule = facade.findSchedule(assetId, period);
        if (schedule == null) {
            throw new NopException(ErpAstErrors.ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, null)
                    .param(ErpAstErrors.ARG_EXPECTED_STATUS, "EXECUTED");
        }
        // 固定来源态守卫委托 StateMachine Bean（M4.41，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            scheduleStateMachine.assertCanReverse(schedule.getStatus());
        } catch (NopException e) {
            throw new NopException(ErpAstErrors.ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION, e)
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, schedule.getStatus())
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

        schedule.setStatus(scheduleStateMachine.reverseTargetStatus());
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        daoProvider.daoFor(ErpAstDepreciationSchedule.class).saveOrUpdateEntity(schedule);
        return schedule;
    }
}
