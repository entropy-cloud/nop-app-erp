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
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ErpAstDepreciationSchedule catchUpDepreciation per-mutation Processor（RC-R1.52，R6.3 {@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 方式B 当期一次性补提前期漏提额（L1 UC-AST-07，简化不追溯）：守卫链[资产存在 + 使用中（IDLE 不允许补提——闲置期无折旧义务，
 * 恢复经 resume 至 IN_SERVICE 后方可补提）+ requirePeriodOpen(currentPeriod)] + 逐漏提期复用 {@link DepreciationCalculator}
 * 补提计算（elapsed 含已执行期 + 漏提期序）+ 折旧计划落行 + 累计折旧/净值回写 + 汇总凭证生成
 * （单张凭证 + billHeadCode 后缀 #CATCHUP + 行 memo「补提 {periods}」标注，isCatchUp 列不落 ORM）。
 * 共享 protected helper 单一真相源在 {@link ErpAstDepreciationScheduleProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstDepreciationScheduleCatchUpDepreciationProcessor {

    @Inject
    ErpAstDepreciationScheduleProcessor facade;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    DepreciationPostingDispatcher postingDispatcher;

    @Inject
    ErpAstDepreciationScheduleStateMachine scheduleStateMachine;

    /**
     * 多月补提循环。漏提期按升序逐期补提，每期金额经 {@link DepreciationCalculator} 计算
     * （elapsed = 已执行期数 + 漏提期序），逐期落 EXECUTED 计划行并回写资产卡片累计折旧/净值；
     * 全部漏提期完成后在 {@code currentPeriod} 一次性汇总为单张凭证（Decision RC-R1.52-D1：单张汇总凭证）。
     * 已 EXECUTED 的漏提期跳过（幂等，不双计）；全部跳过或无漏提期时返回空列表且不生成凭证。
     *
     * @param assetId       资产卡片 ID
     * @param currentPeriod 补提入账的开放期间（补提凭证的记账期间）
     * @param missedPeriods 漏提期间列表（可含已结账期间；须不晚于 currentPeriod；可重复/乱序，内部去重升序）
     * @return 本次补提落行的折旧计划条目（未跳过的新增/更新行）
     */
    public List<ErpAstDepreciationSchedule> catchUpDepreciation(Long assetId, String currentPeriod,
                                                                List<String> missedPeriods, IServiceContext context) {
        ErpAstAsset asset = facade.requireAsset(assetId);
        facade.validateAssetInService(asset, context);
        facade.requirePeriodOpen(currentPeriod, context);

        List<String> periods = normalizeMissedPeriods(missedPeriods, currentPeriod);
        if (periods.isEmpty()) {
            return Collections.emptyList();
        }

        ErpAstAssetCategory category = asset.getCategory();
        String method = asset.getDepreciationMethod() != null ? asset.getDepreciationMethod()
                : (category != null && category.getDepreciationMethod() != null ? category.getDepreciationMethod()
                        : ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        int months = asset.getUsefulLifeMonths() != null ? asset.getUsefulLifeMonths()
                : (category != null && category.getUsefulLifeMonths() != null ? category.getUsefulLifeMonths() : 0);

        IEntityDao<ErpAstDepreciationSchedule> scheduleDao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        int elapsed = facade.countExecuted(assetId);
        BigDecimal total = BigDecimal.ZERO;
        List<ErpAstDepreciationSchedule> created = new ArrayList<>();
        Timestamp now = CoreMetrics.currentTimestamp();

        for (String period : periods) {
            ErpAstDepreciationSchedule schedule = facade.findSchedule(assetId, period);
            if (schedule != null && Objects.equals(schedule.getStatus(), ErpAstConstants.SCHEDULE_STATUS_EXECUTED)) {
                // 幂等：已补提/已计提期间跳过（不双计）
                continue;
            }
            BigDecimal amount = DepreciationCalculator.calculate(method, asset.getOriginalValue(),
                    asset.getResidualValue(), ErpAstDepreciationScheduleProcessor.nz(asset.getNetBookValue()),
                    months, elapsed, null, null);
            BigDecimal newAccum = ErpAstDepreciationScheduleProcessor.nz(asset.getAccumulatedDepreciation()).add(amount);
            BigDecimal newNbv = ErpAstDepreciationScheduleProcessor.nz(asset.getNetBookValue()).subtract(amount);

            if (schedule == null) {
                schedule = scheduleDao.newEntity();
                schedule.setAssetId(assetId);
                schedule.setOrgId(asset.getOrgId());
                schedule.setPeriod(period);
                schedule.setPlannedAmount(BigDecimal.ZERO);
                schedule.setBusinessDate(facade.periodFirstDay(period));
            }
            schedule.setActualAmount(amount);
            schedule.setAccumulatedDepreciation(newAccum);
            schedule.setNetBookValue(newNbv);
            schedule.setStatus(scheduleStateMachine.executeTargetStatus());
            schedule.setExecutedAt(now);
            schedule.setPosted(false);
            schedule.setVoucherId(null);
            try {
                scheduleDao.saveOrUpdateEntity(schedule);
            } catch (Exception e) {
                if (app.erp.common.service.UniqueConstraintHelper.isUniqueConstraintViolation(e)) {
                    throw new NopException(ErpAstErrors.ERR_AST_DEPRECIATION_ALREADY_EXECUTED)
                            .param(ErpAstErrors.ARG_ASSET_ID, assetId)
                            .param(ErpAstErrors.ARG_PERIOD, period);
                }
                throw e;
            }

            asset.setAccumulatedDepreciation(newAccum);
            asset.setNetBookValue(newNbv);
            created.add(schedule);
            total = total.add(amount);
            elapsed++;
        }

        if (created.isEmpty()) {
            return created;
        }
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
        facade.orm().flushSession();

        // 单张汇总凭证（Decision RC-R1.52-D1）：金额 = Σ漏提期补提额，记账期间 = currentPeriod（开放期间，
        // 已结账漏提期无法逐期过账——财务引擎按凭证日期 resolveOpenPeriod 落账）
        List<String> caughtPeriods = created.stream().map(ErpAstDepreciationSchedule::getPeriod)
                .sorted().collect(Collectors.toList());
        if (total.signum() != 0) {
            Long voucherId = postingDispatcher.tryPostCatchUp(asset, category, currentPeriod, total, caughtPeriods);
            if (voucherId != null) {
                for (ErpAstDepreciationSchedule s : created) {
                    s.setPosted(true);
                    s.setPostedAt(now);
                    s.setPostedBy(facade.currentUserId());
                    s.setVoucherId(voucherId);
                    scheduleDao.saveOrUpdateEntity(s);
                }
            }
        }
        return created;
    }

    /** 去重 + 升序 + 格式/时序守卫（漏提期须可解析且不晚于当前期间——补提仅覆盖前期漏提额与出售期当期，不提前记账未来期间）。 */
    protected List<String> normalizeMissedPeriods(List<String> missedPeriods, String currentPeriod) {
        if (missedPeriods == null || missedPeriods.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> unique = new LinkedHashSet<>();
        for (String p : missedPeriods) {
            if (p == null || p.trim().isEmpty()) {
                continue;
            }
            String norm = p.trim();
            try {
                YearMonth.parse(norm);
            } catch (Exception e) {
                throw new NopException(ErpAstErrors.ERR_DEPRECIATION_CATCHUP_PERIOD_INVALID)
                        .param(ErpAstErrors.ARG_PERIOD, norm)
                        .param(ErpAstErrors.ARG_CURRENT_PERIOD, currentPeriod);
            }
            if (norm.compareTo(currentPeriod) > 0) {
                throw new NopException(ErpAstErrors.ERR_DEPRECIATION_CATCHUP_PERIOD_INVALID)
                        .param(ErpAstErrors.ARG_PERIOD, norm)
                        .param(ErpAstErrors.ARG_CURRENT_PERIOD, currentPeriod);
            }
            unique.add(norm);
        }
        List<String> sorted = new ArrayList<>(unique);
        Collections.sort(sorted);
        return sorted;
    }
}
