package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.dao.entity.ErpAstSplitLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.AssetSplitPostingDispatcher;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.dao.IOrmEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产拆分编排 Processor（Facade + Processor 两层结构）。
 *
 * <p>标准审批动作经 xbiz 委托本类。submitForApproval 校验源资产状态与价值平衡约束；
 * approve 触发多步执行（价值分摊 → 建卡 → 源资产处置 → 业财过账 → 回写）。reverseApprove
 * 抛 {@code ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED}（owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 *
 * <p>每个 step 方法标记 protected，下游可逐个覆盖以产品化定制。
 */
public class ErpAstSplitProcessor {

    static final BigDecimal PROPORTION_TOLERANCE = new BigDecimal("0.000001");
    static final BigDecimal AMOUNT_TOLERANCE = new BigDecimal("0.01");
    static final BigDecimal ONE = BigDecimal.ONE;
    static final int VALUE_SCALE = 4;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    AssetSplitPostingDispatcher postingDispatcher;

    public ErpAstSplit submitForApproval(String id, IServiceContext context) {
        ErpAstSplit split = requireSplit(id, context);
        validateNotCancelled(split, context);
        validateTransitionForSubmit(split, context);
        ErpAstAsset source = loadSourceAsset(split);
        validateSourceAsset(split, source, context);
        validateLines(split, source, context);
        split.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        splitDao().updateEntity(split);
        return split;
    }

    public ErpAstSplit withdrawApproval(String id, IServiceContext context) {
        ErpAstSplit split = requireSplit(id, context);
        validateNotCancelled(split, context);
        validateTransitionForWithdraw(split, context);
        split.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        splitDao().updateEntity(split);
        return split;
    }

    public ErpAstSplit approve(String id, IServiceContext context) {
        ErpAstSplit split = requireSplit(id, context);
        if (isAlreadyApproved(split)) {
            return split;
        }
        validateNotCancelled(split, context);
        validateTransitionForApprove(split, context);

        ErpAstAsset source = loadSourceAsset(split);
        validateSourceAsset(split, source, context);
        List<ErpAstSplitLine> lines = loadLines(split);
        validateLines(split, source, context);

        // step 1: 幂等防护
        validateBeforeExecute(split, context);

        // step 2: 价值分摊（计算每行 originalCost/accumDep/nbv，含最大项补差）
        computeAllocation(split, source, lines);

        // step 3: 创建 N 个目标资产卡片 + 折旧计划
        List<ErpAstAsset> targets = createTargetAssets(split, source, lines, context);

        // step 4: 源资产处置（DISPOSED + 账面净值归零）
        disposeSourceAsset(split, source);

        // 持久化分摊结果与目标资产回写
        for (int i = 0; i < lines.size(); i++) {
            ErpAstSplitLine line = lines.get(i);
            line.setTargetAssetId(targets.get(i).getId());
            splitLineDao().saveOrUpdateEntity(line);
        }
        orm().flushSession();

        // step 5: 业财过账（仅 post 路径，无 reverse）
        boolean posted = doPost(split, source, lines, targets);

        // step 6: postProcess（posted 三件套 + 终态）
        split = reload(id);
        split.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        split.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        split.setApprovedBy(currentUserId());
        split.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            LocalDateTime now = CoreMetrics.currentDateTime();
            split.setPosted(true);
            split.setPostedAt(now);
            split.setPostedBy(currentUserId());
        }
        splitDao().updateEntity(split);
        return split;
    }

    public ErpAstSplit reject(String id, IServiceContext context) {
        ErpAstSplit split = requireSplit(id, context);
        validateNotCancelled(split, context);
        validateTransitionForReject(split, context);
        split.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        splitDao().updateEntity(split);
        return split;
    }

    /**
     * 拆分执行后不可撤销（owner doc {@code split-merge.md} §关键业务规则 5）。错误更正走一般资产处置 + 新建流程。
     */
    public ErpAstSplit reverseApprove(String id, IServiceContext context) {
        ErpAstSplit split = requireSplit(id, context);
        throw new NopException(ErpAstErrors.ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED)
                .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode());
    }

    public ErpAstSplit cancel(Long id, IServiceContext context) {
        ErpAstSplit split = requireSplit(String.valueOf(id), context);
        validateTransitionForCancel(split, context);
        split.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        splitDao().updateEntity(split);
        return split;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpAstSplit split, IServiceContext context) {
        String status = currentApproveStatus(split);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpAstConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(split, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpAstSplit split, IServiceContext context) {
        String status = currentApproveStatus(split);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(split, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpAstSplit split, IServiceContext context) {
        String status = currentApproveStatus(split);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(split, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpAstSplit split, IServiceContext context) {
        String status = currentApproveStatus(split);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(split, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForCancel(ErpAstSplit split, IServiceContext context) {
        String docStatus = split.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_ACTIVE)) {
            throw illegalDocTransition(split, docStatus, "非已生效");
        }
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(split, docStatus, "非已作废");
        }
        if (Boolean.TRUE.equals(split.getPosted())) {
            throw illegalDocTransition(split, docStatus, "非已过账");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateSourceAsset(ErpAstSplit split, ErpAstAsset source, IServiceContext context) {
        if (source == null) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_SOURCE_NOT_IN_SERVICE)
                    .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode());
        }
        String status = source.getStatus();
        if (!Objects.equals(status, ErpAstConstants.ASSET_STATUS_IN_SERVICE)) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_SOURCE_NOT_IN_SERVICE)
                    .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode())
                    .param(ErpAstErrors.ARG_ASSET_CODE, source.getCode());
        }
        BigDecimal nbv = nz(source.getNetBookValue());
        if (nbv.signum() <= 0) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_INSUFFICIENT_NET_VALUE)
                    .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode())
                    .param(ErpAstErrors.ARG_AMOUNT, nbv);
        }
    }

    protected void validateLines(ErpAstSplit split, ErpAstAsset source, IServiceContext context) {
        List<ErpAstSplitLine> lines = loadLines(split);
        if (lines.isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_NO_LINES)
                    .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode());
        }
        Set<String> codes = new HashSet<>();
        boolean allowCrossCategory = AppConfig.var(
                ErpAstConstants.CONFIG_SPLIT_MERGE_ALLOW_CROSS_CATEGORY, false);
        BigDecimal sumProportion = BigDecimal.ZERO;
        BigDecimal sumFixedAmount = BigDecimal.ZERO;
        for (ErpAstSplitLine line : lines) {
            if (line.getTargetAssetCode() == null || !codes.add(line.getTargetAssetCode())) {
                throw new NopException(ErpAstErrors.ERR_AST_SPLIT_TARGET_ASSET_CODE_DUPLICATE)
                        .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode())
                        .param(ErpAstErrors.ARG_ASSET_CODE, line.getTargetAssetCode());
            }
            if (!allowCrossCategory && line.getCategoryId() != null
                    && source.getCategoryId() != null
                    && !Objects.equals(line.getCategoryId(), source.getCategoryId())) {
                throw new NopException(ErpAstErrors.ERR_AST_SPLIT_CROSS_CATEGORY_NOT_ALLOWED)
                        .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode());
            }
            BigDecimal prop = nz(line.getProportion());
            sumProportion = sumProportion.add(prop);
            sumFixedAmount = sumFixedAmount.add(nz(line.getOriginalCostAmount()));
        }
        // PROPORTIONAL 平衡：Σ 比例 ≈ 1（容差 0.000001）
        if (sumProportion.compareTo(BigDecimal.ZERO) != 0
                && sumProportion.subtract(ONE).abs().compareTo(PROPORTION_TOLERANCE) > 0) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_PROPORTION_NOT_BALANCED)
                    .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode())
                    .param(ErpAstErrors.ARG_ACTUAL, sumProportion)
                    .param(ErpAstErrors.ARG_EXPECTED, ONE);
        }
        // FIXED_AMOUNT 平衡：Σ 金额 ≈ 源原值（容差 0.01）
        BigDecimal sourceOriginal = nz(source.getOriginalValue());
        if (sumFixedAmount.compareTo(BigDecimal.ZERO) != 0
                && sumFixedAmount.subtract(sourceOriginal).abs().compareTo(AMOUNT_TOLERANCE) > 0) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_AMOUNT_NOT_BALANCED)
                    .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode())
                    .param(ErpAstErrors.ARG_ACTUAL, sumFixedAmount)
                    .param(ErpAstErrors.ARG_EXPECTED, sourceOriginal);
        }
    }

    // ---------- step：执行前幂等防护 ----------

    protected void validateBeforeExecute(ErpAstSplit split, IServiceContext context) {
        if (Boolean.TRUE.equals(split.getPosted())) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_ALREADY_POSTED)
                    .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode());
        }
    }

    // ---------- step：价值分摊 ----------

    protected void computeAllocation(ErpAstSplit split, ErpAstAsset source, List<ErpAstSplitLine> lines) {
        BigDecimal sourceOriginal = nz(source.getOriginalValue());
        BigDecimal sourceAccumDep = nz(source.getAccumulatedDepreciation());

        String roundingMode = AppConfig.var(ErpAstConstants.CONFIG_SPLIT_ROUNDING_MODE,
                ErpAstConstants.SPLIT_ROUNDING_MODE_DOWN_UP);

        // 判定模式：任一行 allocationMethod=FIXED_AMOUNT 视为固定金额模式（行级金额已给定）
        boolean fixedMode = lines.stream().anyMatch(this::isFixedAmountLine);

        if (!fixedMode) {
            // PROPORTIONAL：按比例分摊原值/累计折旧；最大项补差或全部四舍五入
            BigDecimal sumOriginal = BigDecimal.ZERO;
            BigDecimal sumAccumDep = BigDecimal.ZERO;
            int maxPropIndex = 0;
            BigDecimal maxProp = BigDecimal.ZERO;
            for (int i = 0; i < lines.size(); i++) {
                ErpAstSplitLine line = lines.get(i);
                BigDecimal prop = nz(line.getProportion());
                if (prop.compareTo(maxProp) > 0) {
                    maxProp = prop;
                    maxPropIndex = i;
                }
                BigDecimal orig = sourceOriginal.multiply(prop).setScale(VALUE_SCALE, RoundingMode.HALF_UP);
                BigDecimal dep = sourceAccumDep.multiply(prop).setScale(VALUE_SCALE, RoundingMode.HALF_UP);
                line.setOriginalCostAmount(orig);
                line.setAccumulatedDepreciationAmount(dep);
                sumOriginal = sumOriginal.add(orig);
                sumAccumDep = sumAccumDep.add(dep);
            }
            applyRemainder(lines, maxPropIndex, roundingMode, sourceOriginal, sumOriginal,
                    ErpAstConstants.BILL_DATA_ORIGINAL_VALUE);
            applyRemainderForAccumDep(lines, maxPropIndex, roundingMode, sourceAccumDep, sumAccumDep);
        }
        // FIXED_AMOUNT 模式下 originalCostAmount 已由用户给定，累计折旧按金额比例派生
        BigDecimal totalFixed = lines.stream().map(l -> nz(l.getOriginalCostAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        for (ErpAstSplitLine line : lines) {
            BigDecimal orig = nz(line.getOriginalCostAmount());
            BigDecimal ratio = sourceOriginal.signum() == 0 ? BigDecimal.ZERO
                    : orig.divide(sourceOriginal, VALUE_SCALE + 2, RoundingMode.HALF_UP);
            BigDecimal dep = sourceAccumDep.multiply(ratio).setScale(VALUE_SCALE, RoundingMode.HALF_UP);
            line.setAccumulatedDepreciationAmount(dep);
            totalFixed = totalFixed.add(BigDecimal.ZERO);
        }
        // 派生净值
        for (ErpAstSplitLine line : lines) {
            line.setNetBookValue(nz(line.getOriginalCostAmount()).subtract(nz(line.getAccumulatedDepreciationAmount())));
        }
    }

    protected boolean isFixedAmountLine(ErpAstSplitLine line) {
        return Objects.equals(line.getAllocationMethod(), ErpAstConstants.ALLOCATION_METHOD_FIXED_AMOUNT);
    }

    protected void applyRemainder(List<ErpAstSplitLine> lines, int maxIndex, String mode,
                                  BigDecimal target, BigDecimal current, String field) {
        BigDecimal remainder = target.subtract(current);
        if (remainder.signum() == 0) {
            return;
        }
        if (Objects.equals(mode, ErpAstConstants.SPLIT_ROUNDING_MODE_DOWN_UP)) {
            ErpAstSplitLine line = lines.get(maxIndex);
            line.setOriginalCostAmount(nz(line.getOriginalCostAmount()).add(remainder));
        }
        // ROUND_ALL：差额视为舍入误差，不调整（已在容差内）
    }

    protected void applyRemainderForAccumDep(List<ErpAstSplitLine> lines, int maxIndex, String mode,
                                             BigDecimal target, BigDecimal current) {
        BigDecimal remainder = target.subtract(current);
        if (remainder.signum() == 0) {
            return;
        }
        if (Objects.equals(mode, ErpAstConstants.SPLIT_ROUNDING_MODE_DOWN_UP)) {
            ErpAstSplitLine line = lines.get(maxIndex);
            line.setAccumulatedDepreciationAmount(nz(line.getAccumulatedDepreciationAmount()).add(remainder));
        }
    }

    // ---------- step：建卡 + 折旧计划 ----------

    protected List<ErpAstAsset> createTargetAssets(ErpAstSplit split, ErpAstAsset source,
                                                    List<ErpAstSplitLine> lines, IServiceContext context) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        List<ErpAstAsset> targets = new ArrayList<>();
        for (ErpAstSplitLine line : lines) {
            ErpAstAsset asset = dao.newEntity();
            asset.setCode(line.getTargetAssetCode());
            asset.setName(line.getTargetAssetName() != null ? line.getTargetAssetName() : line.getTargetAssetCode());
            asset.setOrgId(source.getOrgId());
            asset.setCategoryId(line.getCategoryId() != null ? line.getCategoryId() : source.getCategoryId());
            asset.setAcquisitionDate(source.getAcquisitionDate());
            asset.setCurrencyId(source.getCurrencyId());
            asset.setOriginalValue(nz(line.getOriginalCostAmount()));
            asset.setCurrentValue(nz(line.getOriginalCostAmount()));
            asset.setResidualValue(nz(source.getResidualValue()));
            asset.setDepreciationMethod(source.getDepreciationMethod());
            asset.setUsefulLifeMonths(source.getUsefulLifeMonths());
            asset.setAccumulatedDepreciation(nz(line.getAccumulatedDepreciationAmount()));
            asset.setNetBookValue(nz(line.getNetBookValue()));
            asset.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            dao.saveEntity(asset);
            targets.add(asset);

            generateDepreciationScheduleForTarget(split, source, line, asset);
        }
        return targets;
    }

    /**
     * 新卡片折旧计划生成。本期按 proportion 比例继承累计折旧金额（owner doc §关键业务规则 2）；
     * 月折旧额重算归 successor（Non-Goal）。这里仅按剩余折旧年限 × 月折旧额生成 PENDING 计划骨架。
     */
    protected void generateDepreciationScheduleForTarget(ErpAstSplit split, ErpAstAsset source,
                                                         ErpAstSplitLine line, ErpAstAsset target) {
        String method = source.getDepreciationMethod();
        Integer totalMonths = source.getUsefulLifeMonths();
        if (method == null || totalMonths == null || totalMonths <= 0) {
            return;
        }
        if (!Objects.equals(method, ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE)) {
            return;
        }
        BigDecimal original = nz(line.getOriginalCostAmount());
        BigDecimal proportion = nz(line.getProportion());
        BigDecimal residual = nz(source.getResidualValue()).multiply(proportion).setScale(VALUE_SCALE, RoundingMode.HALF_UP);
        int inheritedMonths = 0;
        if (proportion.signum() > 0) {
            BigDecimal sourceDepBase = nz(source.getOriginalValue()).subtract(nz(source.getResidualValue()));
            if (sourceDepBase.signum() > 0) {
                BigDecimal monthly = sourceDepBase.divide(BigDecimal.valueOf(totalMonths), VALUE_SCALE, RoundingMode.HALF_UP);
                if (monthly.signum() > 0) {
                    inheritedMonths = nz(source.getAccumulatedDepreciation())
                            .divide(monthly, 0, RoundingMode.HALF_UP).intValue();
                }
            }
        }
        int remainingMonths = Math.max(totalMonths - inheritedMonths, 1);

        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        BigDecimal depBase = original.subtract(residual);
        if (depBase.signum() < 0) {
            depBase = BigDecimal.ZERO;
        }
        BigDecimal monthlyAmount = depBase.divide(BigDecimal.valueOf(remainingMonths), VALUE_SCALE, RoundingMode.HALF_UP);

        LocalDate baseDate = source.getAcquisitionDate() != null ? source.getAcquisitionDate() : CoreMetrics.today();
        LocalDate start = baseDate.plusMonths(1);
        for (int i = 0; i < remainingMonths; i++) {
            LocalDate periodDate = start.plusMonths(i);
            ErpAstDepreciationSchedule schedule = dao.newEntity();
            schedule.setAssetId(target.getId());
            schedule.setOrgId(split.getOrgId());
            schedule.setPeriod(periodDate.getYear() + "-" + String.format("%02d", periodDate.getMonthValue()));
            BigDecimal planned = (i == remainingMonths - 1) ? depBase.subtract(monthlyAmount.multiply(BigDecimal.valueOf(remainingMonths - 1))) : monthlyAmount;
            schedule.setPlannedAmount(planned);
            schedule.setActualAmount(BigDecimal.ZERO);
            schedule.setAccumulatedDepreciation(BigDecimal.ZERO);
            schedule.setNetBookValue(original);
            schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
            schedule.setBusinessDate(periodDate.withDayOfMonth(1));
            dao.saveEntity(schedule);
        }
    }

    // ---------- step：源资产处置 ----------

    protected void disposeSourceAsset(ErpAstSplit split, ErpAstAsset source) {
        source.setStatus(ErpAstConstants.ASSET_STATUS_DISPOSED);
        source.setNetBookValue(BigDecimal.ZERO);
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(source);
    }

    // ---------- step：业财过账 ----------

    protected boolean doPost(ErpAstSplit split, ErpAstAsset source, List<ErpAstSplitLine> lines,
                             List<ErpAstAsset> targets) {
        return postingDispatcher.tryPost(split, source, lines, targets);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstSplit requireSplit(String id, IServiceContext context) {
        ErpAstSplit split = splitDao().getEntityById(id);
        if (split == null) {
            throw new NopException(ErpAstErrors.ERR_AST_SPLIT_NOT_FOUND)
                    .param(ErpAstErrors.ARG_SPLIT_ID, id);
        }
        return split;
    }

    protected void validateNotCancelled(ErpAstSplit split, IServiceContext context) {
        String docStatus = split.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(split, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpAstSplit split) {
        String status = split.getApproveStatus();
        return status != null && Objects.equals(status, ErpAstConstants.APPROVE_STATUS_APPROVED);
    }

    protected String currentApproveStatus(ErpAstSplit split) {
        String status = split.getApproveStatus();
        return status != null ? status : ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    protected ErpAstSplit reload(String id) {
        return splitDao().getEntityById(id);
    }

    protected ErpAstAsset loadSourceAsset(ErpAstSplit split) {
        if (split.getSourceAssetId() == null) {
            return null;
        }
        return daoProvider.daoFor(ErpAstAsset.class).getEntityById(split.getSourceAssetId());
    }

    protected List<ErpAstSplitLine> loadLines(ErpAstSplit split) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("splitId", split.getId()));
        q.addOrderField("lineNo", false);
        return splitLineDao().findAllByQuery(q);
    }

    protected ErpAstAssetCategory loadCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpAstSplit> splitDao() {
        return daoProvider.daoFor(ErpAstSplit.class);
    }

    protected IEntityDao<ErpAstSplitLine> splitLineDao() {
        return daoProvider.daoFor(ErpAstSplitLine.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) splitDao()).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected NopException illegalTransition(ErpAstSplit split, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_SPLIT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpAstSplit split, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_SPLIT_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }

    /** 供 Dispatcher 复用的行汇总科目解析（按行类别 subjectId 拆借方明细）。 */
    @SuppressWarnings("unused")
    private static Map<String, Object> lineMap(String subjectCode, String subjectName, BigDecimal amount, String memo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put(ErpAstConstants.BILL_DATA_LINE_SUBJECT_CODE, subjectCode);
        m.put(ErpAstConstants.BILL_DATA_LINE_SUBJECT_NAME, subjectName);
        m.put(ErpAstConstants.BILL_DATA_LINE_AMOUNT, amount);
        m.put(ErpAstConstants.BILL_DATA_LINE_MEMO, memo);
        return m;
    }
}
