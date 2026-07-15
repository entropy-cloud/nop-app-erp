package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.AssetMergePostingDispatcher;
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

    public ErpAstMerge submitForApproval(String id, IServiceContext context) {
        ErpAstMerge merge = requireMerge(id, context);
        validateNotCancelled(merge, context);
        validateTransitionForSubmit(merge, context);
        List<ErpAstMergeLine> lines = loadLines(merge);
        List<ErpAstAsset> sources = loadSources(lines);
        validateSources(merge, lines, sources, context);
        merge.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        mergeDao().updateEntity(merge);
        return merge;
    }

    public ErpAstMerge withdrawApproval(String id, IServiceContext context) {
        ErpAstMerge merge = requireMerge(id, context);
        validateNotCancelled(merge, context);
        validateTransitionForWithdraw(merge, context);
        merge.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        mergeDao().updateEntity(merge);
        return merge;
    }

    public ErpAstMerge approve(String id, IServiceContext context) {
        ErpAstMerge merge = requireMerge(id, context);
        if (isAlreadyApproved(merge)) {
            return merge;
        }
        validateNotCancelled(merge, context);
        validateTransitionForApprove(merge, context);

        List<ErpAstMergeLine> lines = loadLines(merge);
        List<ErpAstAsset> sources = loadSources(lines);
        validateSources(merge, lines, sources, context);

        // step 1: 幂等防护
        validateBeforeExecute(merge, context);

        // step 2: 汇总源资产 + 解析新卡片属性（折旧方法/剩余期间/投入日期）
        BigDecimal totalOriginal = sum(sources, ErpAstAsset::getOriginalValue);
        BigDecimal totalAccumDep = sum(sources, ErpAstAsset::getAccumulatedDepreciation);
        BigDecimal totalNbv = sum(sources, ErpAstAsset::getNetBookValue);
        String depreciationMethod = resolveDepreciationMethod(sources);
        int usefulLifeMonths = resolveUsefulLifeMonths(sources);
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
                depreciationMethod, usefulLifeMonths, acquisitionDate, context);

        // step 4: 各源资产处置（DISPOSED + 账面净值归零）
        disposeSourceAssets(sources);

        orm().flushSession();

        // step 5: 业财过账
        boolean posted = doPost(merge, sources, lines, target);

        // step 6: postProcess（targetAssetId 回写 + posted 三件套 + 终态）
        merge = reload(id);
        merge.setTargetAssetId(target.getId());
        merge.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        merge.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
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
        ErpAstMerge merge = requireMerge(id, context);
        validateNotCancelled(merge, context);
        validateTransitionForReject(merge, context);
        merge.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        mergeDao().updateEntity(merge);
        return merge;
    }

    /**
     * 合并执行后不可撤销（owner doc {@code split-merge.md} §关键业务规则 5）。错误更正走一般资产处置 + 新建流程。
     */
    public ErpAstMerge reverseApprove(String id, IServiceContext context) {
        ErpAstMerge merge = requireMerge(id, context);
        throw new NopException(ErpAstErrors.ERR_AST_MERGE_REVERSE_NOT_SUPPORTED)
                .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
    }

    public ErpAstMerge cancel(Long id, IServiceContext context) {
        ErpAstMerge merge = requireMerge(String.valueOf(id), context);
        validateTransitionForCancel(merge, context);
        merge.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        mergeDao().updateEntity(merge);
        return merge;
    }

    // ---------- step：迁移校验 ----------

    protected void validateTransitionForSubmit(ErpAstMerge merge, IServiceContext context) {
        String status = currentApproveStatus(merge);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpAstConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(merge, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpAstMerge merge, IServiceContext context) {
        String status = currentApproveStatus(merge);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(merge, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpAstMerge merge, IServiceContext context) {
        String status = currentApproveStatus(merge);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(merge, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpAstMerge merge, IServiceContext context) {
        String status = currentApproveStatus(merge);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(merge, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForCancel(ErpAstMerge merge, IServiceContext context) {
        String docStatus = merge.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_ACTIVE)) {
            throw illegalDocTransition(merge, docStatus, "非已生效");
        }
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(merge, docStatus, "非已作废");
        }
        if (Boolean.TRUE.equals(merge.getPosted())) {
            throw illegalDocTransition(merge, docStatus, "非已过账");
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
        Long firstCategory = null;
        Long firstCurrency = null;
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
        BigDecimal totalNbv = BigDecimal.ZERO;
        BigDecimal weighted = BigDecimal.ZERO;
        for (ErpAstAsset src : sources) {
            BigDecimal nbv = nz(src.getNetBookValue());
            Integer months = src.getUsefulLifeMonths();
            if (months == null || months <= 0) {
                continue;
            }
            totalNbv = totalNbv.add(nbv);
            weighted = weighted.add(nbv.multiply(BigDecimal.valueOf(months)));
        }
        if (totalNbv.signum() <= 0) {
            ErpAstAsset first = sources.isEmpty() ? null : sources.get(0);
            return first != null && first.getUsefulLifeMonths() != null ? first.getUsefulLifeMonths() : 0;
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
                                            String depreciationMethod, int usefulLifeMonths,
                                            LocalDate acquisitionDate, IServiceContext context) {
        ErpAstAsset first = sources.get(0);
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset target = dao.newEntity();
        target.setCode(generateTargetCode(merge));
        target.setName(merge.getCode() + "-合并目标");
        target.setOrgId(merge.getOrgId());
        target.setCategoryId(first.getCategoryId());
        target.setAcquisitionDate(acquisitionDate);
        target.setCurrencyId(merge.getCurrencyId() != null ? merge.getCurrencyId() : first.getCurrencyId());
        target.setOriginalValue(totalOriginal);
        target.setCurrentValue(totalOriginal);
        target.setResidualValue(BigDecimal.ZERO);
        target.setDepreciationMethod(depreciationMethod);
        target.setUsefulLifeMonths(usefulLifeMonths);
        target.setAccumulatedDepreciation(totalAccumDep);
        target.setNetBookValue(totalNbv);
        target.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        dao.saveEntity(target);

        generateDepreciationScheduleForTarget(merge, target, totalOriginal, depreciationMethod, usefulLifeMonths);
        return target;
    }

    protected String generateTargetCode(ErpAstMerge merge) {
        return "AST-MERGE-" + merge.getId();
    }

    protected void generateDepreciationScheduleForTarget(ErpAstMerge merge, ErpAstAsset target,
                                                         BigDecimal original, String method, int months) {
        if (method == null || months <= 0) {
            return;
        }
        if (!Objects.equals(method, ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE)) {
            return;
        }
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        BigDecimal depBase = original;
        BigDecimal monthlyAmount = depBase.divide(BigDecimal.valueOf(months), VALUE_SCALE, RoundingMode.HALF_UP);
        LocalDate baseDate = target.getAcquisitionDate() != null ? target.getAcquisitionDate() : CoreMetrics.today();
        LocalDate start = baseDate.plusMonths(1);
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
            schedule.setNetBookValue(original);
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
        String docStatus = merge.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(merge, docStatus, "非已作废");
        }
    }

    protected boolean isAlreadyApproved(ErpAstMerge merge) {
        String status = merge.getApproveStatus();
        return status != null && Objects.equals(status, ErpAstConstants.APPROVE_STATUS_APPROVED);
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
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        for (ErpAstMergeLine line : lines) {
            sources.add(line.getSourceAssetId() == null ? null : dao.getEntityById(line.getSourceAssetId()));
        }
        return sources;
    }

    protected ErpAstAssetCategory loadCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId);
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

    protected NopException illegalTransition(ErpAstMerge merge, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_MERGE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpAstMerge merge, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_MERGE_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
