package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.CapitalizationPostingDispatcher;
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
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产资本化（转固）状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * Facade {@code ErpAstAssetCapitalizationBizModel} 仅负责入口/事务/委托，编排委托本类。
 *
 * <p>配置余地：每个动作只编排步骤顺序，各步骤为 {@code protected} 方法、以 {@link IServiceContext} 为末参。
 * APPROVED 时建/激活 {@link ErpAstAsset} + 生成 {@link ErpAstDepreciationSchedule} 折旧计划 + CAPITALIZATION 业财过账。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation}+{@code @SingleSession} 事务；ORM Session 由 {@link #orm()} 获取。
 */
public class ErpAstAssetCapitalizationProcessor {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int SCALE = 4;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    CapitalizationPostingDispatcher postingDispatcher;

    public ErpAstAssetCapitalization submit(Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        validateNotCancelled(cap, context);
        validateTransitionForSubmit(cap, context);
        validateForApproval(cap, context);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        capDao().updateEntity(cap);
        return cap;
    }

    public ErpAstAssetCapitalization approve(Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        if (isAlreadyApproved(cap)) {
            return cap;
        }
        validateNotCancelled(cap, context);
        validateTransitionForApprove(cap, context);
        validateForApproval(cap, context);

        // 建卡 + 折旧计划生成（资本化单是转固凭证，资产卡片是其产物）
        ErpAstAsset asset = createAndActivateAsset(cap, context);
        generateDepreciationSchedule(cap, asset, context);
        orm().flushSession();

        // CAPITALIZATION 业财过账
        boolean posted = postingDispatcher.tryPost(cap);

        // 跨域 post 扰动会话脏跟踪，重新加载后置标志并显式持久化。
        cap = reload(id);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        cap.setApprovedBy(currentUserId());
        cap.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            markPosted(cap);
        }
        capDao().updateEntity(cap);
        return cap;
    }

    public ErpAstAssetCapitalization reject(Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        validateNotCancelled(cap, context);
        validateTransitionForReject(cap, context);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        capDao().updateEntity(cap);
        return cap;
    }

    public ErpAstAssetCapitalization reverseApprove(Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        if (isAlreadyRejected(cap)) {
            return cap;
        }
        validateTransitionForReverseApprove(cap, context);
        if (Boolean.TRUE.equals(cap.getPosted())) {
            // 红字冲销 CAPITALIZATION 凭证（硬前置）
            postingDispatcher.reverse(cap);
            // 回滚资本化产物：资产状态恢复草稿、折旧计划取消
            ErpAstAsset asset = findAssetByCode(resolveAssetCode(cap));
            if (asset != null) {
                asset.setStatus(ErpAstConstants.ASSET_STATUS_DRAFT);
                asset.setAccumulatedDepreciation(BigDecimal.ZERO);
                asset.setNetBookValue(asset.getOriginalValue());
                daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
                cancelSchedules(asset.getId());
            }
            cap = reload(id);
            clearPosted(cap);
        }
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        capDao().updateEntity(cap);
        return cap;
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpAstAssetCapitalization cap, IServiceContext context) {
        String status = currentApproveStatus(cap);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpAstConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(cap, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForApprove(ErpAstAssetCapitalization cap, IServiceContext context) {
        String status = currentApproveStatus(cap);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(cap, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpAstAssetCapitalization cap, IServiceContext context) {
        String status = currentApproveStatus(cap);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(cap, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpAstAssetCapitalization cap, IServiceContext context) {
        String status = currentApproveStatus(cap);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(cap, status, "APPROVED");
        }
    }

    protected void validateTransitionForCancel(ErpAstAssetCapitalization cap, IServiceContext context) {
        String docStatus = cap.getDocStatus();
        if (docStatus != null && Objects.equals(docStatus, ErpAstConstants.DOC_STATUS_CANCELLED)) {
            throw illegalDocTransition(cap, docStatus, "非已作废");
        }
    }

    // ---------- step：业务规则校验 ----------

    protected void validateForApproval(ErpAstAssetCapitalization cap, IServiceContext context) {
        if (cap.getCategoryId() == null) {
            throw new NopException(ErpAstErrors.ERR_CAPITALIZATION_CATEGORY_MISSING)
                    .param(ErpAstErrors.ARG_CAPITALIZATION_CODE, cap.getCode());
        }
        BigDecimal original = cap.getOriginalValue();
        if (original == null || original.signum() <= 0) {
            throw new NopException(ErpAstErrors.ERR_CAPITALIZATION_ORIGINAL_VALUE_INVALID)
                    .param(ErpAstErrors.ARG_CAPITALIZATION_CODE, cap.getCode())
                    .param(ErpAstErrors.ARG_AMOUNT, original);
        }
        ErpAstAssetCategory category = loadCategory(cap.getCategoryId());
        if (category == null || category.getUsefulLifeMonths() == null || category.getUsefulLifeMonths() <= 0
                || category.getDepreciationMethod() == null) {
            throw new NopException(ErpAstErrors.ERR_CAPITALIZATION_USEFUL_LIFE_MISSING)
                    .param(ErpAstErrors.ARG_CAPITALIZATION_CODE, cap.getCode());
        }
    }

    // ---------- approve side effects ----------

    protected ErpAstAsset createAndActivateAsset(ErpAstAssetCapitalization cap, IServiceContext context) {
        ErpAstAssetCategory category = loadCategory(cap.getCategoryId());
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset asset = dao.newEntity();
        asset.setCode(resolveAssetCode(cap));
        asset.setName(cap.getAssetName());
        asset.setOrgId(cap.getOrgId());
        asset.setCategoryId(cap.getCategoryId());
        asset.setAcquisitionDate(cap.getCapitalizationDate());
        asset.setCurrencyId(cap.getCurrencyId());
        asset.setOriginalValue(cap.getOriginalValue());
        asset.setCurrentValue(cap.getOriginalValue());
        asset.setResidualValue(BigDecimal.ZERO);
        asset.setDepreciationMethod(category.getDepreciationMethod());
        asset.setUsefulLifeMonths(category.getUsefulLifeMonths());
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setNetBookValue(cap.getOriginalValue());
        asset.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        dao.saveEntity(asset);
        return asset;
    }

    protected void generateDepreciationSchedule(ErpAstAssetCapitalization cap, ErpAstAsset asset,
                                                  IServiceContext context) {
        ErpAstAssetCategory category = loadCategory(cap.getCategoryId());
        String method = category.getDepreciationMethod();
        int months = category.getUsefulLifeMonths();
        BigDecimal original = cap.getOriginalValue();
        BigDecimal residual = nz(asset.getResidualValue());

        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        LocalDate baseDate = cap.getCapitalizationDate() != null ? cap.getCapitalizationDate()
                : CoreMetrics.today();
        // 折旧起始月：资本化次月起（§5.1 当月增加下月提）
        LocalDate start = baseDate.plusMonths(1);

        BigDecimal straightMonthly = BigDecimal.ZERO;
        if (Objects.equals(method, ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE)) {
            straightMonthly = original.subtract(residual).divide(BigDecimal.valueOf(months), SCALE, RoundingMode.HALF_UP);
        }

        for (int i = 0; i < months; i++) {
            LocalDate periodDate = start.plusMonths(i);
            String period = periodDate.format(PERIOD_FMT);
            BigDecimal planned = plannedAmount(method, i, months, original, residual, straightMonthly);

            ErpAstDepreciationSchedule schedule = dao.newEntity();
            schedule.setAssetId(asset.getId());
            schedule.setOrgId(cap.getOrgId());
            schedule.setPeriod(period);
            schedule.setPlannedAmount(planned);
            schedule.setActualAmount(BigDecimal.ZERO);
            schedule.setAccumulatedDepreciation(BigDecimal.ZERO);
            schedule.setNetBookValue(original);
            schedule.setStatus(ErpAstConstants.SCHEDULE_STATUS_PENDING);
            schedule.setBusinessDate(YearMonth.from(periodDate).atDay(1));
            dao.saveEntity(schedule);
        }
    }

    /**
     * 计划折旧额。直线法每期等额、最后一期按残值约束调整；双倍余额递减/工作量法的实际金额在折旧执行时
     * 按账面净值/工作量计算，计划额置 0 占位。
     */
    protected BigDecimal plannedAmount(String method, int monthIndex, int months, BigDecimal original,
                                       BigDecimal residual, BigDecimal straightMonthly) {
        if (Objects.equals(method, ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE)) {
            if (monthIndex == months - 1) {
                BigDecimal beforeLast = straightMonthly.multiply(BigDecimal.valueOf(months - 1));
                return original.subtract(residual).subtract(beforeLast);
            }
            return straightMonthly;
        }
        return BigDecimal.ZERO;
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstAssetCapitalization requireCap(Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = capDao().getEntityById(id);
        if (cap == null) {
            throw new NopException(ErpAstErrors.ERR_CAPITALIZATION_NOT_FOUND)
                    .param(ErpAstErrors.ARG_CAPITALIZATION_ID, id);
        }
        return cap;
    }

    protected void validateNotCancelled(ErpAstAssetCapitalization cap, IServiceContext context) {
        validateTransitionForCancel(cap, context);
    }

    protected ErpAstAssetCategory loadCategory(Long categoryId) {
        return daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(categoryId);
    }

    protected ErpAstAsset findAssetByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpAstAsset> list = daoProvider.daoFor(ErpAstAsset.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected void cancelSchedules(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider.daoFor(ErpAstDepreciationSchedule.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            s.setStatus(ErpAstConstants.SCHEDULE_STATUS_CANCELLED);
            dao.saveOrUpdateEntity(s);
        }
    }

    protected boolean isAlreadyApproved(ErpAstAssetCapitalization cap) {
        String status = cap.getApproveStatus();
        return status != null && Objects.equals(status, ErpAstConstants.APPROVE_STATUS_APPROVED);
    }

    protected boolean isAlreadyRejected(ErpAstAssetCapitalization cap) {
        String status = cap.getApproveStatus();
        return status != null && Objects.equals(status, ErpAstConstants.APPROVE_STATUS_REJECTED);
    }

    protected String currentApproveStatus(ErpAstAssetCapitalization cap) {
        String status = cap.getApproveStatus();
        return status != null ? status : ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    protected String resolveAssetCode(ErpAstAssetCapitalization cap) {
        if (cap.getAssetCode() != null && !cap.getAssetCode().trim().isEmpty()) {
            return cap.getAssetCode();
        }
        return "AST-" + cap.getId();
    }

    private void markPosted(ErpAstAssetCapitalization cap) {
        cap.setPosted(true);
        cap.setPostedAt(CoreMetrics.currentDateTime());
        cap.setPostedBy(currentUserId());
    }

    private void clearPosted(ErpAstAssetCapitalization cap) {
        cap.setPosted(false);
        cap.setPostedAt(null);
        cap.setPostedBy(null);
    }

    protected ErpAstAssetCapitalization reload(Long id) {
        return capDao().getEntityById(id);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpAstAssetCapitalization> capDao() {
        return daoProvider.daoFor(ErpAstAssetCapitalization.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) capDao()).getOrmTemplate();
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

    protected NopException illegalTransition(ErpAstAssetCapitalization cap, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_CAPITALIZATION_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_CAPITALIZATION_CODE, cap.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected NopException illegalDocTransition(ErpAstAssetCapitalization cap, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_CAPITALIZATION_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_CAPITALIZATION_CODE, cap.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
