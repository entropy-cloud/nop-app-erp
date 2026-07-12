package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DepreciationPostingDispatcher;
import app.erp.ast.service.service.DepreciationCalculator;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Objects;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 折旧计提编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpAstDepreciationScheduleBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>提供单资产 {@link #executeDepreciation} 与批量 {@link #executeBatchDepreciation}，按折旧方法计算本期折旧，
 * 更新计划条目与资产卡片汇总列，触发 DEPRECIATION 业财过账。期间控制 + 幂等重执行 + 批量错误隔离。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation}+{@code @SingleSession} 事务；{@code executeBatchDepreciation}
 * 声明于 dao 层 IBiz，供期末结账经 I*Biz 跨模块调用。
 */
public class ErpAstDepreciationScheduleProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpAstDepreciationScheduleProcessor.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    DepreciationPostingDispatcher postingDispatcher;

    public ErpAstDepreciationSchedule executeDepreciation(Long assetId, String period, IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        validateAssetInService(asset, context);
        requirePeriodOpen(period, context);

        ErpAstAssetCategory category = asset.getCategoryId() == null ? null
                : daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(asset.getCategoryId());
        String method = asset.getDepreciationMethod() != null ? asset.getDepreciationMethod()
                : (category != null && category.getDepreciationMethod() != null ? category.getDepreciationMethod()
                        : ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        int months = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths()
                : (category != null && category.getUsefulLifeMonths() != null ? category.getUsefulLifeMonths() : 0);

        ErpAstDepreciationSchedule schedule = findSchedule(assetId, period);
        boolean wasExecuted = schedule != null
                && Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED);

        // 幂等重执行：先红冲已过账凭证（硬前置）
        if (wasExecuted && Boolean.TRUE.equals(schedule.getPosted())) {
            postingDispatcher.reverse(asset, period);
            schedule = findSchedule(assetId, period);
        }

        // 回退本期旧折旧对资产卡片汇总的影响，恢复到本期前状态后重算
        if (schedule != null && schedule.getActualAmount() != null
                && schedule.getStatus() != null
                && Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED)) {
            BigDecimal oldAmount = schedule.getActualAmount();
            asset.setAccumulatedDepreciation(nz(asset.getAccumulatedDepreciation()).subtract(oldAmount));
            asset.setNetBookValue(nz(asset.getNetBookValue()).add(oldAmount));
        }

        int elapsed = countExecuted(assetId) - (wasExecuted ? 1 : 0);
        if (elapsed < 0) {
            elapsed = 0;
        }
        BigDecimal nbvRestored = nz(asset.getNetBookValue());
        BigDecimal amount = DepreciationCalculator.calculate(method, asset.getOriginalValue(),
                asset.getResidualValue(), nbvRestored, months, elapsed, null, null);

        BigDecimal newAccum = nz(asset.getAccumulatedDepreciation()).add(amount);
        BigDecimal newNbv = nbvRestored.subtract(amount);

        IEntityDao<ErpAstDepreciationSchedule> scheduleDao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        if (schedule == null) {
            schedule = scheduleDao.newEntity();
            schedule.setAssetId(assetId);
            schedule.setOrgId(asset.getOrgId());
            schedule.setPeriod(period);
            schedule.setPlannedAmount(BigDecimal.ZERO);
            schedule.setBusinessDate(periodFirstDay(period));
        }
        LocalDateTime now = CoreMetrics.currentDateTime();
        schedule.setActualAmount(amount);
        schedule.setAccumulatedDepreciation(newAccum);
        schedule.setNetBookValue(newNbv);
        schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_EXECUTED);
        schedule.setExecutedAt(now);
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        scheduleDao.saveOrUpdateEntity(schedule);

        // 资产卡片汇总列回写
        asset.setAccumulatedDepreciation(newAccum);
        asset.setNetBookValue(newNbv);
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
        orm().flushSession();

        // DEPRECIATION(70) 业财过账
        Long voucherId = postingDispatcher.tryPost(schedule, asset, category);
        schedule = findSchedule(assetId, period);
        if (voucherId != null) {
            schedule.setPosted(true);
            schedule.setPostedAt(now);
            schedule.setPostedBy(currentUserId());
            schedule.setVoucherId(voucherId);
            scheduleDao.saveOrUpdateEntity(schedule);
        }
        return schedule;
    }

    public int executeBatchDepreciation(String period, IServiceContext context) {
        // 期间控制（批量入口统一校验，避免逐资产重复查询）
        requirePeriodOpen(period, context);

        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE));
        List<ErpAstAsset> assets = dao.findAllByQuery(q);

        int processed = 0;
        for (ErpAstAsset asset : assets) {
            try {
                executeDepreciation(asset.getId(), period, context);
                processed++;
            } catch (Exception e) {
                // 错误隔离：单资产失败记录待人工，不影响他资产（§5.3）
                LOG.warn("批量折旧：资产 {} 期间 {} 计提失败，跳过：{}", asset.getCode(), period, e.getMessage());
            }
        }
        return processed;
    }

    public ErpAstDepreciationSchedule reverseDepreciation(Long assetId, String period, IServiceContext context) {
        ErpAstDepreciationSchedule schedule = findSchedule(assetId, period);
        if (schedule == null || schedule.getStatus() == null
                || !Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED)) {
            throw new NopException(ErpAstErrors.ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION)
                    .param(ErpAstErrors.ARG_CURRENT_STATUS, schedule != null ? schedule.getStatus() : null)
                    .param(ErpAstErrors.ARG_EXPECTED_STATUS, "EXECUTED");
        }
        ErpAstAsset asset = requireAsset(assetId);
        if (Boolean.TRUE.equals(schedule.getPosted())) {
            postingDispatcher.reverse(asset, period);
        }
        // 回滚资产卡片累计折旧/净值
        BigDecimal oldAmount = nz(schedule.getActualAmount());
        asset.setAccumulatedDepreciation(nz(asset.getAccumulatedDepreciation()).subtract(oldAmount));
        asset.setNetBookValue(nz(asset.getNetBookValue()).add(oldAmount));
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);

        schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_REVERSED);
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        daoProvider.daoFor(ErpAstDepreciationSchedule.class).saveOrUpdateEntity(schedule);
        return schedule;
    }

    /**
     * 资本化维修折旧计划重算（加性扩展，UC-AST-10）。删除未执行（PENDING）条目，按剩余使用年限重新摊销
     * （原值+增量 − 已计提累计折旧 − 残值）/ 剩余月数，残值约束保留。详见 maintenance.md §三。
     */
    public int recalculateForCapitalizationMaintenance(Long assetId, BigDecimal increment, IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        BigDecimal original = nz(asset.getOriginalValue());
        BigDecimal residual = nz(asset.getResidualValue());
        BigDecimal accumulated = nz(asset.getAccumulatedDepreciation());

        ErpAstAssetCategory category = asset.getCategoryId() == null ? null
                : daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(asset.getCategoryId());
        int totalMonths = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths()
                : (category != null && category.getUsefulLifeMonths() != null ? category.getUsefulLifeMonths() : 0);

        int executedMonths = countExecuted(assetId);
        int remainingMonths = totalMonths - executedMonths;

        IEntityDao<ErpAstDepreciationSchedule> scheduleDao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);

        // 删除所有未执行（PENDING）条目
        QueryBean pendingQ = new QueryBean();
        pendingQ.addFilter(and(eq("assetId", assetId), eq("status", ErpAstConstants.SCHEDULE_STATUS_PENDING)));
        for (ErpAstDepreciationSchedule s : scheduleDao.findAllByQuery(pendingQ)) {
            scheduleDao.deleteEntity(s);
        }

        int regenerated = 0;
        if (remainingMonths > 0) {
            // 新可折旧基数 = （原值 + 增量）− 残值 − 已计提累计折旧
            BigDecimal depreciableBase = original.add(nz(increment)).subtract(residual).subtract(accumulated);
            if (depreciableBase.signum() < 0) {
                depreciableBase = BigDecimal.ZERO;
            }
            BigDecimal monthly = depreciableBase.divide(BigDecimal.valueOf(remainingMonths), 4,
                    java.math.RoundingMode.HALF_UP);

            // 找到已执行的最大期间作为重建起点
            String lastExecutedPeriod = findLastExecutedPeriod(assetId);
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
                schedule.setAssetId(assetId);
                schedule.setOrgId(asset.getOrgId());
                schedule.setPeriod(period);
                schedule.setPlannedAmount(planned);
                schedule.setActualAmount(BigDecimal.ZERO);
                schedule.setAccumulatedDepreciation(BigDecimal.ZERO);
                schedule.setNetBookValue(original.add(nz(increment)).subtract(accumulated));
                schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
                schedule.setBusinessDate(periodMonth.atDay(1));
                scheduleDao.saveEntity(schedule);
                regenerated++;
            }
        }
        return regenerated;
    }

    protected String findLastExecutedPeriod(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED)));
        q.addOrderField("period", true);
        q.setLimit(1);
        List<ErpAstDepreciationSchedule> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0).getPeriod();
    }

    // ---------- step：业务规则校验（protected，下游可逐个覆盖） ----------

    protected void validateAssetInService(ErpAstAsset asset, IServiceContext context) {
        String assetStatus = asset.getStatus();
        if (assetStatus == null || !Objects.equals(assetStatus, ErpAstConstants.ASSET_STATUS_IN_SERVICE)) {
            throw new NopException(ErpAstErrors.ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
    }

    protected void requirePeriodOpen(String period, IServiceContext context) {
        ErpFinAccountingPeriod periodEntity = findPeriod(period);
        if (periodEntity == null) {
            throw new NopException(ErpAstErrors.ERR_DEPRECIATION_PERIOD_NOT_FOUND)
                    .param(ErpAstErrors.ARG_PERIOD, period);
        }
        if (periodEntity.getStatus() == null
                || !Objects.equals(periodEntity.getStatus(), ErpAstConstants.PERIOD_STATUS_OPEN)) {
            throw new NopException(ErpAstErrors.ERR_DEPRECIATION_PERIOD_CLOSED)
                    .param(ErpAstErrors.ARG_PERIOD, period);
        }
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstAsset requireAsset(Long assetId) {
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        if (asset == null) {
            throw new NopException(ErpAstErrors.ERR_ASSET_NOT_FOUND)
                    .param(ErpAstErrors.ARG_ASSET_ID, assetId);
        }
        return asset;
    }

    protected ErpFinAccountingPeriod findPeriod(String period) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", period));
        q.setLimit(1);
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected java.time.LocalDate periodFirstDay(String period) {
        try {
            return java.time.YearMonth.parse(period).atDay(1);
        } catch (Exception e) {
            return CoreMetrics.today();
        }
    }

    protected ErpAstDepreciationSchedule findSchedule(Long assetId, String period) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("period", period)));
        q.setLimit(1);
        List<ErpAstDepreciationSchedule> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected int countExecuted(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED)));
        return dao.findAllByQuery(q).size();
    }

    // ---------- misc helpers ----------

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) daoProvider.daoFor(ErpAstDepreciationSchedule.class)).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
