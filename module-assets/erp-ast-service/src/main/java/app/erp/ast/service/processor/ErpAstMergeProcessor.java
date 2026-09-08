package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.AssetMergePostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstMergeApprovalStateMachine;
import app.erp.ast.service.statemachine.ErpAstMergeDocumentStateMachine;
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
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产合并编排 Processor（Facade + Processor 两层结构，镜像 {@link ErpAstSplitProcessor}）。
 *
 * <p>submitForApproval 校验所有源资产状态/类别/币种一致；approve 触发多步执行（汇总 → 建卡 → 源处置 → 业财过账 → 回写）。
 * reverseApprove 抛 {@code ERR_AST_MERGE_REVERSE_NOT_SUPPORTED}（owner doc {@code split-merge.md} §关键业务规则 5 不可逆契约）。
 */
public class ErpAstMergeProcessor {

    static final int VALUE_SCALE = 4;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    AssetMergePostingDispatcher postingDispatcher;

    @Inject
    ErpAstMergeSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpAstMergeApproveProcessor approveProcessor;

    @Inject
    ErpAstMergeRejectProcessor rejectProcessor;

    @Inject
    ErpAstMergeReverseApproveProcessor reverseApproveProcessor;

    @Inject
    ErpAstMergeWithdrawApprovalProcessor withdrawApprovalProcessor;

    @Inject
    ErpAstMergeCancelProcessor cancelProcessor;

    @Inject
    ErpAstMergeApprovalStateMachine approvalStateMachine;

    @Inject
    ErpAstMergeDocumentStateMachine documentStateMachine;

    public ErpAstMerge submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpAstMerge withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpAstMerge approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    protected ErpAstMerge executeApprove(String id, ErpAstMerge merge, List<ErpAstMergeLine> lines,
                                           List<ErpAstAsset> sources, IServiceContext context) {
        // step 1: 幂等防护
        validateBeforeExecute(merge, context);

        // step 2: 汇总源资产 + 解析新卡片属性（折旧方法/剩余期间/投入日期）
        BigDecimal totalOriginal = sum(sources, ErpAstAsset::getOriginalValue);
        BigDecimal totalAccumDep = sum(sources, ErpAstAsset::getAccumulatedDepreciation);
        BigDecimal totalNbv = sum(sources, ErpAstAsset::getNetBookValue);
        BigDecimal totalResidual = sum(sources, ErpAstAsset::getResidualValue);
        String depreciationMethod = resolveDepreciationMethod(sources);
        int usefulLifeMonths = resolveUsefulLifeMonths(sources);
        int elapsedMonths = resolveElapsedMonths(sources, usefulLifeMonths);
        LocalDate acquisitionDate = resolveEarliestAcquisitionDate(sources);

        // 写回各行贡献快照
        for (int i = 0; i < lines.size(); i++) {
            ErpAstMergeLine line = lines.get(i);
            ErpAstAsset src = sources.get(i);
            line.setOriginalCostAmount(nz(src.getOriginalValue()));
            line.setAccumulatedDepreciationAmount(nz(src.getAccumulatedDepreciation()));
            line.setNetBookValue(nz(src.getNetBookValue()));
            line.setContributionProportion(totalOriginal.signum() == 0 ? BigDecimal.ZERO
                    : nz(src.getOriginalValue()).divide(totalOriginal, VALUE_SCALE + 2, RoundingMode.HALF_UP));
            mergeLineDao().saveOrUpdateEntity(line);
        }

        // step 3: 创建 1 个目标资产卡片 + 折旧计划
        ErpAstAsset target = createTargetAsset(merge, sources, lines, totalOriginal, totalAccumDep, totalNbv,
                totalResidual, depreciationMethod, usefulLifeMonths, elapsedMonths, acquisitionDate, context);

        // step 4: 各源资产处置（DISPOSED + 账面净值归零）
        disposeSourceAssets(sources);

        orm().flushSession();

        // step 5: 业财过账
        boolean posted = doPost(merge, sources, lines, target);

        // step 6: postProcess（targetAssetId 回写 + posted 三件套 + 终态）
        merge = reload(id);
        merge.setTargetAssetId(target.getId());
        merge.setApproveStatus(approvalStateMachine.approveTargetStatus());
        merge.setDocStatus(documentStateMachine.approveTargetStatus());
        merge.setApprovedBy(currentUserId());
        merge.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            Timestamp now = CoreMetrics.currentTimestamp();
            merge.setPosted(true);
            merge.setPostedAt(now);
            merge.setPostedBy(currentUserId());
        }
        mergeDao().updateEntity(merge);
        return merge;
    }

    public ErpAstMerge reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    /**
     * 合并执行后不可撤销（owner doc {@code split-merge.md} §关键业务规则 5）。错误更正走一般资产处置 + 新建流程。
     */
    public ErpAstMerge reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpAstMerge cancel(String id, IServiceContext context) {
        return cancelProcessor.cancel(id, context);
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpAstMerge merge, IServiceContext context) {
        try {
            approvalStateMachine.assertCanSubmitForApproval(currentApproveStatus(merge));
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
        }
    }

    protected void validateTransitionForWithdraw(ErpAstMerge merge, IServiceContext context) {
        try {
            approvalStateMachine.assertCanWithdrawApproval(currentApproveStatus(merge));
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
        }
    }

    protected void validateTransitionForApprove(ErpAstMerge merge, IServiceContext context) {
        try {
            approvalStateMachine.assertCanApprove(currentApproveStatus(merge));
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
        }
    }

    protected void validateTransitionForReject(ErpAstMerge merge, IServiceContext context) {
        try {
            approvalStateMachine.assertCanReject(currentApproveStatus(merge));
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
        }
    }

    protected void validateTransitionForCancel(ErpAstMerge merge, IServiceContext context) {
        String docStatus = merge.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_ACTIVE)) {
            throw illegalDocTransition(merge, docStatus, "!" + ErpAstConstants.DOC_STATUS_ACTIVE);
        }
        if (documentStateMachine.isCancelled(docStatus)) {
            throw illegalDocTransition(merge, docStatus, "!" + ErpAstConstants.DOC_STATUS_CANCELLED);
        }
        if (Boolean.TRUE.equals(merge.getPosted())) {
            throw illegalDocTransition(merge, docStatus, "!POSTED");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateSources(ErpAstMerge merge, List<ErpAstMergeLine> lines, List<ErpAstAsset> sources,
                                   IServiceContext context) {
        if (lines.isEmpty()) {
            throw new NopException(ErpAstErrors.ERR_AST_MERGE_NO_SOURCES)
                    .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
        }
        boolean allowCrossCategory = AppConfig.var(
                ErpAstConstants.CONFIG_SPLIT_MERGE_ALLOW_CROSS_CATEGORY, false);
        String firstCategory = null;
        String firstCurrency = null;
        for (int i = 0; i < sources.size(); i++) {
            ErpAstAsset src = sources.get(i);
            if (src == null) {
                continue;
            }
            String status = src.getStatus();
            if (!Objects.equals(status, ErpAstConstants.ASSET_STATUS_IN_SERVICE)) {
                throw new NopException(ErpAstErrors.ERR_AST_MERGE_SOURCE_NOT_IN_SERVICE)
                        .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode())
                        .param(ErpAstErrors.ARG_ASSET_CODE, src.getCode());
            }
            if (firstCategory == null) {
                firstCategory = src.getCategoryId();
            } else if (!allowCrossCategory && src.getCategoryId() != null
                    && !Objects.equals(firstCategory, src.getCategoryId())) {
                throw new NopException(ErpAstErrors.ERR_AST_MERGE_CROSS_CATEGORY_NOT_ALLOWED)
                        .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
            }
            if (firstCurrency == null) {
                firstCurrency = src.getCurrencyId();
            } else if (src.getCurrencyId() != null
                    && !Objects.equals(firstCurrency, src.getCurrencyId())) {
                throw new NopException(ErpAstErrors.ERR_AST_MERGE_CROSS_CURRENCY_NOT_ALLOWED)
                        .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
            }
        }
    }

    // ---------- step：执行前幂等防护 ----------

    protected void validateBeforeExecute(ErpAstMerge merge, IServiceContext context) {
        if (Boolean.TRUE.equals(merge.getPosted())) {
            throw new NopException(ErpAstErrors.ERR_AST_MERGE_ALREADY_POSTED)
                    .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
        }
    }

    // ---------- step：新卡片属性解析 ----------

    protected String resolveDepreciationMethod(List<ErpAstAsset> sources) {
        String mode = AppConfig.var(ErpAstConstants.CONFIG_MERGE_DEPRECIATION_INHERIT,
                ErpAstConstants.MERGE_DEPRECIATION_INHERIT_WEIGHTED);
        if (Objects.equals(mode, ErpAstConstants.MERGE_DEPRECIATION_INHERIT_MAX)) {
            ErpAstAsset max = null;
            BigDecimal maxNbv = BigDecimal.ZERO;
            for (ErpAstAsset src : sources) {
                BigDecimal nbv = nz(src.getNetBookValue());
                if (max == null || nbv.compareTo(maxNbv) > 0) {
                    max = src;
                    maxNbv = nbv;
                }
            }
            return max != null ? max.getDepreciationMethod() : null;
        }
        // WEIGHTED：按净值加权（取净值最大项的方法作为代表，简化加权实现）
        return resolveWeightedMethod(sources);
    }

    protected String resolveWeightedMethod(List<ErpAstAsset> sources) {
        ErpAstAsset max = null;
        BigDecimal maxNbv = BigDecimal.ZERO;
        BigDecimal totalNbv = BigDecimal.ZERO;
        for (ErpAstAsset src : sources) {
            BigDecimal nbv = nz(src.getNetBookValue());
            totalNbv = totalNbv.add(nbv);
            if (max == null || nbv.compareTo(maxNbv) > 0) {
                max = src;
                maxNbv = nbv;
            }
        }
        return max != null ? max.getDepreciationMethod() : null;
    }

    protected int resolveUsefulLifeMonths(List<ErpAstAsset> sources) {
        // P1-CK-ast-006：剩余折旧期间按加权平均剩余期间取整（owner doc split-merge.md 字面）——
        // 修复前按全年限加权（已折旧期间计入基数，合并卡每月计提 = 全额原值/全年限 → 过度折旧）。
        BigDecimal totalNbv = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        for (ErpAstAsset src : sources) {
            BigDecimal nbv = nz(src.getNetBookValue());
            int remaining = remainingMonths(src);
            if (remaining <= 0) {
                continue;
            }
            totalNbv = totalNbv.add(nbv);
            weighted = weighted.add(nbv.multiply(BigDecimal.valueOf(remaining)));
        }
        if (totalNbv.signum() <= 0) {
            ErpAstAsset first = sources.isEmpty() ? null : sources.get(0);
            return first != null && first.getUsefulLifeMonths() != null ? first.getUsefulLifeMonths() : 0;
        }
        return weighted.divide(totalNbv, 0, RoundingMode.HALF_UP).intValue();
    }

    /** 单源剩余折旧期间 = max(有效年限 − 已执行期数(按累计折旧/月折旧推), 1)。 */
    protected int remainingMonths(ErpAstAsset src) {
        Integer months = src.getUsefulLifeMonths();
        if (months == null || months <= 0) {
            return 0;
        }
        int elapsed = inheritedMonths(src);
        return Math.max(months - elapsed, 1);
    }

    /** 已执行期数 = 累计折旧 / 月折旧额（直线法口径，镜像 ErpAstSplitProcessor 的 inheritedMonths 推导）。 */
    protected int inheritedMonths(ErpAstAsset src) {
        BigDecimal depBase = nz(src.getOriginalValue()).subtract(nz(src.getResidualValue()));
        Integer months = src.getUsefulLifeMonths();
        if (depBase.signum() <= 0 || months == null || months <= 0) {
            return 0;
        }
        BigDecimal monthly = depBase.divide(BigDecimal.valueOf(months), VALUE_SCALE, RoundingMode.HALF_UP);
        if (monthly.signum() <= 0) {
            return 0;
        }
        return nz(src.getAccumulatedDepreciation()).divide(monthly, 0, RoundingMode.HALF_UP).intValue();
    }

    /** 合并后已执行期数（按 NBV 加权）——用于折旧计划起点（P1-CK-ast-006）。 */
    protected int resolveElapsedMonths(List<ErpAstAsset> sources, int remainingLife) {
        BigDecimal totalNbv = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        for (ErpAstAsset src : sources) {
            BigDecimal nbv = nz(src.getNetBookValue());
            Integer months = src.getUsefulLifeMonths();
            if (months == null || months <= 0) {
                continue;
            }
            int elapsed = months - remainingMonths(src);
            if (elapsed < 0) {
                elapsed = 0;
            }
            totalNbv = totalNbv.add(nbv);
            weighted = weighted.add(nbv.multiply(BigDecimal.valueOf(elapsed)));
        }
        if (totalNbv.signum() <= 0) {
            return 0;
        }
        return weighted.divide(totalNbv, 0, RoundingMode.HALF_UP).intValue();
    }

    protected LocalDate resolveEarliestAcquisitionDate(List<ErpAstAsset> sources) {
        LocalDate earliest = null;
        for (ErpAstAsset src : sources) {
            if (src.getAcquisitionDate() != null
                    && (earliest == null || src.getAcquisitionDate().isBefore(earliest))) {
                earliest = src.getAcquisitionDate();
            }
        }
        return earliest;
    }

    // ---------- step：建卡 + 折旧计划 ----------

    protected ErpAstAsset createTargetAsset(ErpAstMerge merge, List<ErpAstAsset> sources, List<ErpAstMergeLine> lines,
                                            BigDecimal totalOriginal, BigDecimal totalAccumDep, BigDecimal totalNbv,
                                            BigDecimal totalResidual,
                                            String depreciationMethod, int usefulLifeMonths, int elapsedMonths,
                                            LocalDate acquisitionDate, IServiceContext context) {
        ErpAstAsset first = sources.get(0);
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset target = dao.newEntity();
        target.setCode(generateTargetCode(merge));
        target.setName(merge.getCode() + "-merge-target");
        target.setOrgId(merge.getOrgId());
        target.setCategoryId(first.getCategoryId());
        target.setAcquisitionDate(acquisitionDate);
        target.setCurrencyId(merge.getCurrencyId() != null ? merge.getCurrencyId() : first.getCurrencyId());
        target.setOriginalValue(totalOriginal);
        target.setCurrentValue(totalOriginal);
        // P1-CK-ast-006：合并卡残值 = Σ 源残值（修复前归零 → 残值约束失效，过度折旧）。
        target.setResidualValue(totalResidual);
        target.setDepreciationMethod(depreciationMethod);
        target.setUsefulLifeMonths(usefulLifeMonths);
        target.setAccumulatedDepreciation(totalAccumDep);
        target.setNetBookValue(totalNbv);
        target.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        dao.saveEntity(target);

        generateDepreciationScheduleForTarget(merge, target, totalNbv, totalResidual,
                depreciationMethod, usefulLifeMonths, elapsedMonths);
        return target;
    }

    protected String generateTargetCode(ErpAstMerge merge) {
        return "AST-MERGE-" + merge.getId();
    }

    protected void generateDepreciationScheduleForTarget(ErpAstMerge merge, ErpAstAsset target,
                                                         BigDecimal remainingNbv, BigDecimal residual,
                                                         String method, int months, int elapsedMonths) {
        if (method == null || months <= 0) {
            return;
        }
        if (!Objects.equals(method, ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE)) {
            return;
        }
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        // P1-CK-ast-006：计划基数 = 剩余可折旧净值 − 残值（修复前用全额原值 → 已折旧部分被再提一遍）。
        BigDecimal depBase = remainingNbv.subtract(residual);
        if (depBase.signum() < 0) {
            depBase = BigDecimal.ZERO;
        }
        BigDecimal monthlyAmount = depBase.divide(BigDecimal.valueOf(months), VALUE_SCALE, RoundingMode.HALF_UP);
        LocalDate baseDate = target.getAcquisitionDate() != null ? target.getAcquisitionDate() : CoreMetrics.today();
        // P1-CK-ast-006：计划起点 = 继承点（源已执行期数）次月（修复前从源购置次月起排覆盖历史）。
        LocalDate start = baseDate.plusMonths(elapsedMonths).plusMonths(1);
        for (int i = 0; i < months; i++) {
            LocalDate periodDate = start.plusMonths(i);
            ErpAstDepreciationSchedule schedule = dao.newEntity();
            schedule.setAssetId(target.getId());
            schedule.setOrgId(merge.getOrgId());
            schedule.setPeriod(periodDate.getYear() + "-" + String.format("%02d", periodDate.getMonthValue()));
            BigDecimal planned = (i == months - 1)
                    ? depBase.subtract(monthlyAmount.multiply(BigDecimal.valueOf(months - 1)))
                    : monthlyAmount;
            schedule.setPlannedAmount(planned);
            schedule.setActualAmount(BigDecimal.ZERO);
            schedule.setAccumulatedDepreciation(BigDecimal.ZERO);
            schedule.setNetBookValue(remainingNbv);
            schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
            schedule.setBusinessDate(periodDate.withDayOfMonth(1));
            dao.saveEntity(schedule);
        }
    }

    // ---------- step：源资产处置 ----------

    protected void disposeSourceAssets(List<ErpAstAsset> sources) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        for (ErpAstAsset src : sources) {
            src.setStatus(ErpAstConstants.ASSET_STATUS_DISPOSED);
            src.setNetBookValue(BigDecimal.ZERO);
            dao.saveOrUpdateEntity(src);
        }
    }

    // ---------- step：业财过账 ----------

    protected boolean doPost(ErpAstMerge merge, List<ErpAstAsset> sources, List<ErpAstMergeLine> lines,
                             ErpAstAsset target) {
        return postingDispatcher.tryPost(merge, sources, lines, target);
    }

    // ---------- 校验/查询辅助 ----------

    protected ErpAstMerge requireMerge(String id, IServiceContext context) {
        ErpAstMerge merge = mergeDao().getEntityById(id);
        if (merge == null) {
            throw new NopException(ErpAstErrors.ERR_AST_MERGE_NOT_FOUND)
                    .param(ErpAstErrors.ARG_MERGE_ID, id);
        }
        return merge;
    }

    protected void validateNotCancelled(ErpAstMerge merge, IServiceContext context) {
        if (merge.isCancelled()) {
            throw illegalDocTransition(merge, merge.getDocStatus(), "!" + ErpAstConstants.DOC_STATUS_CANCELLED);
        }
    }

    protected String currentApproveStatus(ErpAstMerge merge) {
        String status = merge.getApproveStatus();
        return status != null ? status : ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    protected ErpAstMerge reload(String id) {
        return mergeDao().getEntityById(id);
    }

    protected List<ErpAstMergeLine> loadLines(ErpAstMerge merge) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mergeId", merge.getId()));
        q.addOrderField("lineNo", false);
        return mergeLineDao().findAllByQuery(q);
    }

    protected List<ErpAstAsset> loadSources(List<ErpAstMergeLine> lines) {
        List<ErpAstAsset> sources = new ArrayList<>();
        for (ErpAstMergeLine line : lines) {
            sources.add(line.getSourceAsset());
        }
        return sources;
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpAstMerge> mergeDao() {
        return daoProvider.daoFor(ErpAstMerge.class);
    }

    protected IEntityDao<ErpAstMergeLine> mergeLineDao() {
        return daoProvider.daoFor(ErpAstMergeLine.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) mergeDao()).getOrmTemplate();
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

    protected static BigDecimal sum(List<ErpAstAsset> sources, java.util.function.Function<ErpAstAsset, BigDecimal> f) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpAstAsset src : sources) {
            total = total.add(nz(f.apply(src)));
        }
        return total;
    }

    /**
     * docStatus 轴非法迁移直抛领域码构造（if-throw 直抛守卫复用；SM 非法边已直抛领域码，plan 2026-09-07-2200-1）。
     */
    protected NopException illegalDocTransition(ErpAstMerge merge, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_MERGE_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
