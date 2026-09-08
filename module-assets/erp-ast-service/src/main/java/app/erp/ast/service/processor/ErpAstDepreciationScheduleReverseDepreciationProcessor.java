package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DepreciationPostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstDepreciationScheduleStateMachine;
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

    @Inject
    ErpAstDepreciationScheduleStateMachine scheduleStateMachine;

    public ErpAstDepreciationSchedule reverseDepreciation(String assetId, String period, IServiceContext context) {
        ErpAstDepreciationSchedule schedule = facade.findSchedule(assetId, period);
        if (schedule == null) {
            throw new NopException(ErpAstErrors.ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, null)
                    .param(ErpAstErrors.ARG_EXPECTED_STATUS, "EXECUTED");
        }
        // 固定来源态守卫委托 StateMachine Bean（M4.41，契约 §4；Bean 直抛领域码 ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION
        // （模板无实体编号参数，无需同码补参），plan 2026-09-07-2200-1）
        scheduleStateMachine.assertCanReverse(schedule.getStatus());
        ErpAstAsset asset = facade.requireAsset(assetId);
        if (Boolean.TRUE.equals(schedule.getPosted())) {
            postingDispatcher.reverse(asset, period);
            // P1-CK-ast2-005：红冲事件同步派发 ErpAstDepreciationReversalListener——监听者已回退
            // 资产累计/净值并置 schedule.status=REVERSED。重载后以 REVERSED 标记跳过自身回退，
            // 避免双重应用（修复前无监听者时本方法负责回退）。
            schedule = facade.findSchedule(assetId, period);
        }
        boolean listenerRolledBack = schedule != null
                && Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_REVERSED);
        if (!listenerRolledBack) {
            BigDecimal oldAmount = ErpAstDepreciationScheduleProcessor.nz(schedule.getActualAmount());
            asset.setAccumulatedDepreciation(ErpAstDepreciationScheduleProcessor.nz(asset.getAccumulatedDepreciation()).subtract(oldAmount));
            asset.setNetBookValue(ErpAstDepreciationScheduleProcessor.nz(asset.getNetBookValue()).add(oldAmount));
            daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
        }

        schedule.setStatus(scheduleStateMachine.reverseTargetStatus());
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        daoProvider.daoFor(ErpAstDepreciationSchedule.class).saveOrUpdateEntity(schedule);
        return schedule;
    }
}
