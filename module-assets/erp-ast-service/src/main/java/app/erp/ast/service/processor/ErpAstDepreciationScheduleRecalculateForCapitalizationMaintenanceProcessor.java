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

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpAstDepreciationSchedule）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
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
            // P1-CK-ast2-002 修复：基数按当前卡片状态重算（原值已含/已减增量——调用方
            // applyTreatmentCapitalize/rollbackCapitalization 先改 originalValue 再调本方法）。
            // 修复前 `.add(nz(increment))` 使增量双计（资本化后多提 X / 回退后少提 X）。
            BigDecimal depreciableBase = original.subtract(residual).subtract(accumulated);
            if (depreciableBase.signum() < 0) {
                depreciableBase = BigDecimal.ZERO;
            }
            BigDecimal monthly = depreciableBase.divide(BigDecimal.valueOf(remainingMonths), 4, RoundingMode.HALF_UP);

            String lastExecutedPeriod = facade.findLastExecutedPeriod(assetId);
            // 经 CoreMetrics IClock 时间线取当前月（与域冻结时钟测试扩展联动；直读 YearMonth.now()
            // 会绕过冻结时钟，使重算期次快照随月初滚动漂移——bug 2026-09-01-0058）
            java.time.YearMonth baseMonth = lastExecutedPeriod != null
                    ? java.time.YearMonth.parse(lastExecutedPeriod).plusMonths(1)
                    : java.time.YearMonth.from(io.nop.api.core.time.CoreMetrics.today());

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
                // P1-CK-ast2-002：计划行 NBV 同步去掉双计（原值已含增量）
                schedule.setNetBookValue(original.subtract(accumulated));
                schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
                schedule.setBusinessDate(periodMonth.atDay(1));
                scheduleDao.saveEntity(schedule);
                regenerated++;
            }
        }
        return regenerated;
    }
}
