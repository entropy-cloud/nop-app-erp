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

    public ErpAstDepreciationSchedule executeDepreciation(String assetId, String period, IServiceContext context) {
        ErpAstAsset asset = facade.requireAsset(assetId);
        facade.validateAssetInService(asset, context);
        facade.requirePeriodOpen(period, context);
        // P1-CK-ast2-003：当月增加下月提守卫——period 必须晚于资本化（获取）月份。
        // 修复前期末结账批量路径对资本化当月资产照常计提（计划外多提一个月）。
        if (asset.getAcquisitionDate() != null && period != null) {
            try {
                java.time.YearMonth periodYm = java.time.YearMonth.parse(period);
                java.time.YearMonth acquisitionYm = java.time.YearMonth.from(asset.getAcquisitionDate());
                if (!periodYm.isAfter(acquisitionYm)) {
                    throw new NopException(ErpAstErrors.ERR_DEPRECIATION_PERIOD_BEFORE_ACQUISITION)
                            .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode())
                            .param(ErpAstErrors.ARG_PERIOD, period)
                            .param(ErpAstErrors.ARG_ACQUISITION_DATE, asset.getAcquisitionDate().toString());
                }
            } catch (java.time.format.DateTimeParseException ignore) {
                // period 非 YYYY-MM 形态（防御），跳过守卫
            }
        }

        ErpAstAssetCategory category = asset.getCategory();
        String method = asset.getDepreciationMethod() != null ? asset.getDepreciationMethod()
                : (category != null && category.getDepreciationMethod() != null ? category.getDepreciationMethod()
                        : ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        // P1-CK-ast2-001：工作量法（UNITS）运行时零数据面（两个调用点均传 null 工作量参数，
        // ORM 无工作量列）——静默恒 0 掩盖漏提，改为显式业务错误（owner doc §十 登记 Deferred）。
        if (Objects.equals(method, ErpAstConstants.DEPRECIATION_METHOD_UNITS)) {
            throw new NopException(ErpAstErrors.ERR_DEPRECIATION_UNITS_NOT_CONFIGURED)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
        int months = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths()
                : (category != null && category.getUsefulLifeMonths() != null ? category.getUsefulLifeMonths() : 0);

        ErpAstDepreciationSchedule schedule = facade.findSchedule(assetId, period);
        boolean wasExecuted = schedule != null
                && Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);

        if (wasExecuted && Boolean.TRUE.equals(schedule.getPosted())) {
            postingDispatcher.reverse(asset, period);
            schedule = facade.findSchedule(assetId, period);
        }

        // 幂等重执行红冲（posted=true → 反向）经 ErpAstDepreciationReversalListener 同步回退：监听者置
        // schedule.status=REVERSED + posted=false 并回滚资产累计/净值——本块以 status==EXECUTED 为前置
        // 天然跳过（避免双重回退）；悬挂自愈路径（posted=false 无红冲、监听者不触发）本块仍负责回退。
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
            schedule.setAssetId(String.valueOf(assetId));
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

        String voucherId = postingDispatcher.tryPost(schedule, asset, category);
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
