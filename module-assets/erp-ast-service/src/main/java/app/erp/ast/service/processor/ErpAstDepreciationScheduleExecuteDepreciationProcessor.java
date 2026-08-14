package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DepreciationPostingDispatcher;
import app.erp.ast.service.service.DepreciationCalculator;
import app.erp.ast.service.statemachine.ErpAstDepreciationScheduleStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * ErpAstDepreciationSchedule executeDepreciation per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含单资产折旧计提编排（计算 + 汇总回写 + DEPRECIATION 业财过账）；共享 protected helper 单一真相源在
 * {@link ErpAstDepreciationScheduleProcessor}（delete-after-extract facade，保留为 helper 持有者）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstDepreciationScheduleExecuteDepreciationProcessor {

    @Inject
    ErpAstDepreciationScheduleProcessor facade;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    DepreciationPostingDispatcher postingDispatcher;

    @Inject
    ErpAstDepreciationScheduleStateMachine scheduleStateMachine;

    public ErpAstDepreciationSchedule executeDepreciation(Long assetId, String period, IServiceContext context) {
        ErpAstAsset asset = facade.requireAsset(assetId);
        facade.validateAssetInService(asset, context);
        facade.requirePeriodOpen(period, context);

        ErpAstAssetCategory category = asset.getCategory();
        String method = asset.getDepreciationMethod() != null ? asset.getDepreciationMethod()
                : (category != null && category.getDepreciationMethod() != null ? category.getDepreciationMethod()
                        : ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        int months = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths()
                : (category != null && category.getUsefulLifeMonths() != null ? category.getUsefulLifeMonths() : 0);

        ErpAstDepreciationSchedule schedule = facade.findSchedule(assetId, period);
        boolean wasExecuted = schedule != null
                && Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);

        if (wasExecuted && Boolean.TRUE.equals(schedule.getPosted())) {
            postingDispatcher.reverse(asset, period);
            schedule = facade.findSchedule(assetId, period);
        }

        if (schedule != null && schedule.getActualAmount() != null
                && schedule.getStatus() != null
                && Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED)) {
            BigDecimal oldAmount = schedule.getActualAmount();
            asset.setAccumulatedDepreciation(ErpAstDepreciationScheduleProcessor.nz(asset.getAccumulatedDepreciation()).subtract(oldAmount));
            asset.setNetBookValue(ErpAstDepreciationScheduleProcessor.nz(asset.getNetBookValue()).add(oldAmount));
        }

        int elapsed = facade.countExecuted(assetId) - (wasExecuted ? 1 : 0);
        if (elapsed < 0) {
            elapsed = 0;
        }
        BigDecimal nbvRestored = ErpAstDepreciationScheduleProcessor.nz(asset.getNetBookValue());
        BigDecimal amount = DepreciationCalculator.calculate(method, asset.getOriginalValue(),
                asset.getResidualValue(), nbvRestored, months, elapsed, null, null);

        BigDecimal newAccum = ErpAstDepreciationScheduleProcessor.nz(asset.getAccumulatedDepreciation()).add(amount);
        BigDecimal newNbv = nbvRestored.subtract(amount);

        IEntityDao<ErpAstDepreciationSchedule> scheduleDao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        if (schedule == null) {
            schedule = scheduleDao.newEntity();
            schedule.setAssetId(assetId);
            schedule.setOrgId(asset.getOrgId());
            schedule.setPeriod(period);
            schedule.setPlannedAmount(BigDecimal.ZERO);
            schedule.setBusinessDate(facade.periodFirstDay(period));
        }
        Timestamp now = CoreMetrics.currentTimestamp();
        schedule.setActualAmount(amount);
        schedule.setAccumulatedDepreciation(newAccum);
        schedule.setNetBookValue(newNbv);
        // 目标态委托 StateMachine Bean（M4.41，契约 §4；重执行/幂等路径为动态编排逻辑保留原位）
        schedule.setStatus(scheduleStateMachine.executeTargetStatus());
        schedule.setExecutedAt(now);
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        try {
            scheduleDao.saveOrUpdateEntity(schedule);

            asset.setAccumulatedDepreciation(newAccum);
            asset.setNetBookValue(newNbv);
            daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
            facade.orm().flushSession();
        } catch (Exception e) {
            if (app.erp.common.service.UniqueConstraintHelper.isUniqueConstraintViolation(e)) {
                throw new NopException(ErpAstErrors.ERR_AST_DEPRECIATION_ALREADY_EXECUTED)
                        .param(ErpAstErrors.ARG_ASSET_ID, assetId)
                        .param(ErpAstErrors.ARG_PERIOD, period);
            }
            throw e;
        }

        Long voucherId = postingDispatcher.tryPost(schedule, asset, category);
        schedule = facade.findSchedule(assetId, period);
        if (voucherId != null) {
            schedule.setPosted(true);
            schedule.setPostedAt(now);
            schedule.setPostedBy(facade.currentUserId());
            schedule.setVoucherId(voucherId);
            scheduleDao.saveOrUpdateEntity(schedule);
        }
        return schedule;
    }
}
