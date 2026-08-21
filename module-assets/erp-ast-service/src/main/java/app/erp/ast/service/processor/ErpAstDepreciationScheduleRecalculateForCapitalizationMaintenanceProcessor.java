package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpAstDepreciationSchedule recalculateForCapitalizationMaintenance per-mutation Processor（R6.3，
 * {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含资本化维修折旧计划重算编排（删除 PENDING 条目 + 按剩余使用年限重新摊销）；共享 protected helper 单一真相源在
 * {@link ErpAstDepreciationScheduleProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor {

    @Inject
    ErpAstDepreciationScheduleProcessor facade;

    @Inject
    IDaoProvider daoProvider;

    public int recalculateForCapitalizationMaintenance(String assetId, BigDecimal increment, IServiceContext context) {
        ErpAstAsset asset = facade.requireAsset(assetId);
        BigDecimal original = ErpAstDepreciationScheduleProcessor.nz(asset.getOriginalValue());
        BigDecimal residual = ErpAstDepreciationScheduleProcessor.nz(asset.getResidualValue());
        BigDecimal accumulated = ErpAstDepreciationScheduleProcessor.nz(asset.getAccumulatedDepreciation());

        ErpAstAssetCategory category = asset.getCategory();
        int totalMonths = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths()
                : (category != null && category.getUsefulLifeMonths() != null ? category.getUsefulLifeMonths() : 0);

        int executedMonths = facade.countExecuted(assetId);
        int remainingMonths = totalMonths - executedMonths;

        IEntityDao<ErpAstDepreciationSchedule> scheduleDao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);

        QueryBean pendingQ = new QueryBean();
        pendingQ.addFilter(and(eq("assetId", assetId), eq("status", ErpAstConstants.SCHEDULE_STATUS_PENDING)));
        for (ErpAstDepreciationSchedule s : scheduleDao.findAllByQuery(pendingQ)) {
            scheduleDao.deleteEntity(s);
        }

        int regenerated = 0;
        if (remainingMonths > 0) {
            BigDecimal depreciableBase = original.add(ErpAstDepreciationScheduleProcessor.nz(increment)).subtract(residual).subtract(accumulated);
            if (depreciableBase.signum() < 0) {
                depreciableBase = BigDecimal.ZERO;
            }
            BigDecimal monthly = depreciableBase.divide(BigDecimal.valueOf(remainingMonths), 4, RoundingMode.HALF_UP);

            String lastExecutedPeriod = facade.findLastExecutedPeriod(assetId);
            java.time.YearMonth baseMonth = lastExecutedPeriod != null
                    ? java.time.YearMonth.parse(lastExecutedPeriod).plusMonths(1)
                    : java.time.YearMonth.now();

            for (int i = 0; i < remainingMonths; i++) {
                java.time.YearMonth periodMonth = baseMonth.plusMonths(i);
                String period = periodMonth.toString();
                BigDecimal planned = (i == remainingMonths - 1)
                        ? depreciableBase.subtract(monthly.multiply(BigDecimal.valueOf(remainingMonths - 1)))
                        : monthly;

                ErpAstDepreciationSchedule schedule = scheduleDao.newEntity();
                schedule.setAssetId(String.valueOf(assetId));
                schedule.setOrgId(asset.getOrgId());
                schedule.setPeriod(period);
                schedule.setPlannedAmount(planned);
                schedule.setActualAmount(BigDecimal.ZERO);
                schedule.setAccumulatedDepreciation(BigDecimal.ZERO);
                schedule.setNetBookValue(original.add(ErpAstDepreciationScheduleProcessor.nz(increment)).subtract(accumulated));
                schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
                schedule.setBusinessDate(periodMonth.atDay(1));
                scheduleDao.saveEntity(schedule);
                regenerated++;
            }
        }
        return regenerated;
    }
}
