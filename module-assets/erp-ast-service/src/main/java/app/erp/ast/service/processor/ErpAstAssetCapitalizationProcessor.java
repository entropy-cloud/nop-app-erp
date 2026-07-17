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

public class ErpAstAssetCapitalizationProcessor {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int SCALE = 4;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    CapitalizationPostingDispatcher postingDispatcher;

    public ErpAstAssetCapitalization submitForApproval(String id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        validateNotCancelled(cap, context);
        validateTransitionForSubmit(cap, context);
        validateForApproval(cap, context);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        capDao().updateEntity(cap);
        return cap;
    }

    public ErpAstAssetCapitalization withdrawApproval(String id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        validateNotCancelled(cap, context);
        validateTransitionForWithdraw(cap, context);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        capDao().updateEntity(cap);
        return cap;
    }

    public ErpAstAssetCapitalization approve(String id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        if (cap.isApproved()) {
            return cap;
        }
        validateNotCancelled(cap, context);
        validateTransitionForApprove(cap, context);
        validateForApproval(cap, context);

        ErpAstAsset asset = createAndActivateAsset(cap, context);
        generateDepreciationSchedule(cap, asset, context);
        orm().flushSession();

        boolean posted = postingDispatcher.tryPost(cap);

        cap = reload(id);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        cap.setApprovedBy(currentUserId());
        cap.setApprovedAt(CoreMetrics.currentTimestamp());
        if (posted) {
            markPosted(cap);
        }
        capDao().updateEntity(cap);
        return cap;
    }

    public ErpAstAssetCapitalization reject(String id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        validateNotCancelled(cap, context);
        validateTransitionForReject(cap, context);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        capDao().updateEntity(cap);
        return cap;
    }

    public ErpAstAssetCapitalization reverseApprove(String id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        if (cap.isRejected()) {
            return cap;
        }
        validateTransitionForReverseApprove(cap, context);
        if (Boolean.TRUE.equals(cap.getPosted())) {
            postingDispatcher.reverse(cap);
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

    protected void validateTransitionForWithdraw(ErpAstAssetCapitalization cap, IServiceContext context) {
        String status = currentApproveStatus(cap);
        if (!Objects.equals(status, ErpAstConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(cap, status, "SUBMITTED");
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
        if (cap.isCancelled()) {
            throw illegalDocTransition(cap, cap.getDocStatus(), "非已作废");
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

    protected ErpAstAssetCapitalization requireCap(String id, IServiceContext context) {
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

    protected void markPosted(ErpAstAssetCapitalization cap) {
        cap.setPosted(true);
        cap.setPostedAt(CoreMetrics.currentTimestamp());
        cap.setPostedBy(currentUserId());
    }

    protected void clearPosted(ErpAstAssetCapitalization cap) {
        cap.setPosted(false);
        cap.setPostedAt(null);
        cap.setPostedBy(null);
    }

    protected ErpAstAssetCapitalization reload(String id) {
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

    protected static BigDecimal nz(BigDecimal v) {
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
