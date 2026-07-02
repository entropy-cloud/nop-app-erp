
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.DepreciationPostingDispatcher;
import app.erp.ast.service.service.DepreciationCalculator;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 折旧计划 BizModel（{@code depreciation-and-posting.md} §1/§5）。提供单资产 {@link #executeDepreciation}
 * 与批量 {@link #executeBatchDepreciation}，按折旧方法计算本期折旧（残值约束 §1.4），更新计划条目与资产卡片
 * 汇总列（accumulatedDepreciation/netBookValue），触发 DEPRECIATION(70) 业财过账。
 *
 * <p>期间控制：执行前校验目标期间 {@code ErpFinAccountingPeriod.status == OPEN(10)}（CLOSED(30) 拒绝，§关键规则1）。
 * 幂等性：同期间重复执行先红字冲销已执行折旧凭证再重新生成（§5.1）。错误隔离：批量中单资产失败不影响他资产（§5.3）。
 *
 * <p>{@code @BizMutation} 自动包装事务。{@code executeBatchDepreciation} 声明于 dao 层 IBiz，
 * 重新 codegen 后 Api 契约传播，供期末结账（1000-3）经 I*Biz 跨模块调用。
 */
@BizModel("ErpAstDepreciationSchedule")
public class ErpAstDepreciationScheduleBizModel extends CrudBizModel<ErpAstDepreciationSchedule>
        implements IErpAstDepreciationScheduleBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpAstDepreciationScheduleBizModel.class);

    @Inject
    DepreciationPostingDispatcher postingDispatcher;

    public ErpAstDepreciationScheduleBizModel() {
        setEntityName(ErpAstDepreciationSchedule.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDepreciationSchedule executeDepreciation(@Name("assetId") Long assetId,
                                                          @Name("period") String period,
                                                          IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        Integer assetStatus = asset.getStatus();
        if (assetStatus == null || assetStatus != ErpAstConstants.ASSET_STATUS_IN_SERVICE) {
            throw new NopException(ErpAstErrors.ERR_DEPRECIATION_ASSET_NOT_IN_SERVICE)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
        requirePeriodOpen(period);

        ErpAstAssetCategory category = asset.getCategoryId() == null ? null
                : daoProvider().daoFor(ErpAstAssetCategory.class).getEntityById(asset.getCategoryId());
        int method = asset.getDepreciationMethod() != null ? asset.getDepreciationMethod()
                : (category != null && category.getDepreciationMethod() != null ? category.getDepreciationMethod()
                        : ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        int months = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths()
                : (category != null && category.getUsefulLifeMonths() != null ? category.getUsefulLifeMonths() : 0);

        ErpAstDepreciationSchedule schedule = findSchedule(assetId, period);
        boolean wasExecuted = schedule != null
                && ErpAstConstants.SCHEDULE_STATUS_EXECUTED == schedule.getStatus();

        // 幂等重执行：先红冲已过账凭证（硬前置）
        if (wasExecuted && Boolean.TRUE.equals(schedule.getPosted())) {
            postingDispatcher.reverse(asset, period);
            schedule = findSchedule(assetId, period);
        }

        // 回退本期旧折旧对资产卡片汇总的影响，恢复到本期前状态后重算
        if (schedule != null && schedule.getActualAmount() != null
                && schedule.getStatus() != null
                && schedule.getStatus() == ErpAstConstants.SCHEDULE_STATUS_EXECUTED) {
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

        IEntityDao<ErpAstDepreciationSchedule> scheduleDao = daoProvider().daoFor(ErpAstDepreciationSchedule.class);
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
        daoProvider().daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
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

    @Override
    @BizMutation
    @SingleSession
    public int executeBatchDepreciation(@Name("period") String period, IServiceContext context) {        // 期间控制（批量入口统一校验，避免逐资产重复查询）
        requirePeriodOpen(period);

        IEntityDao<ErpAstAsset> dao = daoProvider().daoFor(ErpAstAsset.class);
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

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDepreciationSchedule reverseDepreciation(@Name("assetId") Long assetId,
                                                          @Name("period") String period,
                                                          IServiceContext context) {
        ErpAstDepreciationSchedule schedule = findSchedule(assetId, period);
        if (schedule == null || schedule.getStatus() == null
                || schedule.getStatus() != ErpAstConstants.SCHEDULE_STATUS_EXECUTED) {
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
        daoProvider().daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);

        schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_REVERSED);
        schedule.setPosted(false);
        schedule.setVoucherId(null);
        daoProvider().daoFor(ErpAstDepreciationSchedule.class).saveOrUpdateEntity(schedule);
        return schedule;
    }

    // ---------- helpers ----------

    private ErpAstAsset requireAsset(Long assetId) {
        ErpAstAsset asset = daoProvider().daoFor(ErpAstAsset.class).getEntityById(assetId);
        if (asset == null) {
            throw new NopException(ErpAstErrors.ERR_ASSET_NOT_FOUND)
                    .param(ErpAstErrors.ARG_ASSET_ID, assetId);
        }
        return asset;
    }

    private void requirePeriodOpen(String period) {
        ErpFinAccountingPeriod periodEntity = findPeriod(period);
        if (periodEntity == null) {
            throw new NopException(ErpAstErrors.ERR_DEPRECIATION_PERIOD_NOT_FOUND)
                    .param(ErpAstErrors.ARG_PERIOD, period);
        }
        if (periodEntity.getStatus() == null
                || periodEntity.getStatus() != ErpAstConstants.PERIOD_STATUS_OPEN) {
            throw new NopException(ErpAstErrors.ERR_DEPRECIATION_PERIOD_CLOSED)
                    .param(ErpAstErrors.ARG_PERIOD, period);
        }
    }

    private ErpFinAccountingPeriod findPeriod(String period) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider().daoFor(ErpFinAccountingPeriod.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", period));
        q.setLimit(1);
        List<ErpFinAccountingPeriod> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private java.time.LocalDate periodFirstDay(String period) {
        try {
            return java.time.YearMonth.parse(period).atDay(1);
        } catch (Exception e) {
            return CoreMetrics.today();
        }
    }

    private ErpAstDepreciationSchedule findSchedule(Long assetId, String period) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider().daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("period", period)));
        q.setLimit(1);
        List<ErpAstDepreciationSchedule> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private int countExecuted(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider().daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("assetId", assetId), eq("status", ErpAstConstants.SCHEDULE_STATUS_EXECUTED)));
        return dao.findAllByQuery(q).size();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
