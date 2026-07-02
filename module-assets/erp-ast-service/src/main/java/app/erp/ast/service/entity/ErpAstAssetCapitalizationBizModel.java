
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstAssetCapitalizationBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.CapitalizationPostingDispatcher;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * 资产资本化（转固）BizModel（{@code depreciation-and-posting.md} §2）。CRUD 之上实现三轴审批状态机
 * （UNSUBMITTED↔SUBMITTED→APPROVED/REJECTED）。APPROVED 时建/激活 {@link ErpAstAsset}（继承类别折旧方法/使用年限，
 * status→IN_SERVICE，累计折旧=0，净值=原值）+ 生成 {@link ErpAstDepreciationSchedule} 折旧计划（按折旧方法 + 残值 +
 * 使用年限每期一条 PENDING，折旧次月起；直线法每期等额，最后一期按残值约束调整）+ 触发 CAPITALIZATION(80) 业财过账
 * （借固定资产 / 贷在建工程或银行存款）。
 *
 * <p>{@code @BizMutation} 自动包装事务（不叠加 {@code @Transactional}），每迁移校验前置态，违例抛
 * {@link NopException}+{@link ErpAstErrors} 作用域码。过账失败吞异常保持 APPROVED+posted=false（兜底重试）。
 */
@BizModel("ErpAstAssetCapitalization")
public class ErpAstAssetCapitalizationBizModel extends CrudBizModel<ErpAstAssetCapitalization>
        implements IErpAstAssetCapitalizationBiz {

    private static final DateTimeFormatter PERIOD_FMT = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final int SCALE = 4;

    @Inject
    CapitalizationPostingDispatcher postingDispatcher;

    public ErpAstAssetCapitalizationBizModel() {
        setEntityName(ErpAstAssetCapitalization.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization submit(@Name("id") Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        requireNotCancelled(cap);
        Integer status = currentApproveStatus(cap);
        if (status != ErpAstConstants.APPROVE_STATUS_UNSUBMITTED
                && status != ErpAstConstants.APPROVE_STATUS_REJECTED) {
            throw illegalTransition(cap, status, "UNSUBMITTED 或 REJECTED");
        }
        validateForApproval(cap);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(cap);
        return cap;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization approve(@Name("id") Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        Integer status = currentApproveStatus(cap);
        if (status == ErpAstConstants.APPROVE_STATUS_APPROVED) {
            return cap;
        }
        requireNotCancelled(cap);
        if (status != ErpAstConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(cap, status, "SUBMITTED");
        }
        validateForApproval(cap);

        // 建卡 + 折旧计划生成（资本化单是转固凭证，资产卡片是其产物）
        ErpAstAsset asset = createAndActivateAsset(cap);
        generateDepreciationSchedule(cap, asset);
        orm().flushSession();

        // CAPITALIZATION 业财过账
        boolean posted = postingDispatcher.tryPost(cap);

        // 跨域 post 扰动会话脏跟踪，重新加载后置标志并显式持久化（对齐 ErpFinExpenseClaimBizModel）
        cap = requireEntity(String.valueOf(id), null, context);
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_APPROVED);
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_ACTIVE);
        cap.setApprovedBy(currentUserId());
        cap.setApprovedAt(CoreMetrics.currentDateTime());
        if (posted) {
            cap.setPosted(true);
            cap.setPostedAt(CoreMetrics.currentDateTime());
            cap.setPostedBy(currentUserId());
        }
        dao().updateEntity(cap);
        return cap;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization reject(@Name("id") Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        requireNotCancelled(cap);
        Integer status = currentApproveStatus(cap);
        if (status != ErpAstConstants.APPROVE_STATUS_SUBMITTED) {
            throw illegalTransition(cap, status, "SUBMITTED");
        }
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        dao().updateEntity(cap);
        return cap;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstAssetCapitalization reverseApprove(@Name("id") Long id, IServiceContext context) {
        ErpAstAssetCapitalization cap = requireCap(id, context);
        Integer status = currentApproveStatus(cap);
        if (status == ErpAstConstants.APPROVE_STATUS_REJECTED) {
            return cap;
        }
        if (status != ErpAstConstants.APPROVE_STATUS_APPROVED) {
            throw illegalTransition(cap, status, "APPROVED");
        }
        if (Boolean.TRUE.equals(cap.getPosted())) {
            // 红字冲销 CAPITALIZATION 凭证（硬前置）
            postingDispatcher.reverse(cap);
            // 回滚资本化产物：资产状态恢复草稿、折旧计划取消
            ErpAstAsset asset = findAssetByCode(resolveAssetCode(cap));
            if (asset != null) {
                asset.setStatus(ErpAstConstants.ASSET_STATUS_DRAFT);
                asset.setAccumulatedDepreciation(java.math.BigDecimal.ZERO);
                asset.setNetBookValue(asset.getOriginalValue());
                daoProvider().daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);
                cancelSchedules(asset.getId());
            }
            cap = requireEntity(String.valueOf(id), null, context);
            cap.setPosted(false);
            cap.setPostedAt(null);
            cap.setPostedBy(null);
        }
        cap.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        cap.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        dao().updateEntity(cap);
        return cap;
    }

    // ---------- validation ----------

    private void validateForApproval(ErpAstAssetCapitalization cap) {
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

    private ErpAstAsset createAndActivateAsset(ErpAstAssetCapitalization cap) {
        ErpAstAssetCategory category = loadCategory(cap.getCategoryId());
        IEntityDao<ErpAstAsset> dao = daoProvider().daoFor(ErpAstAsset.class);
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

    private void generateDepreciationSchedule(ErpAstAssetCapitalization cap, ErpAstAsset asset) {
        ErpAstAssetCategory category = loadCategory(cap.getCategoryId());
        int method = category.getDepreciationMethod();
        int months = category.getUsefulLifeMonths();
        BigDecimal original = cap.getOriginalValue();
        BigDecimal residual = nz(asset.getResidualValue());

        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider().daoFor(ErpAstDepreciationSchedule.class);
        LocalDate baseDate = cap.getCapitalizationDate() != null ? cap.getCapitalizationDate()
                : CoreMetrics.today();
        // 折旧起始月：资本化次月起（§5.1 当月增加下月提）
        LocalDate start = baseDate.plusMonths(1);

        BigDecimal straightMonthly = BigDecimal.ZERO;
        if (method == ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE) {
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
     * （Phase 3 {@code DepreciationCalculator}）按账面净值/工作量计算，计划额置 0 占位。
     */
    private BigDecimal plannedAmount(int method, int monthIndex, int months, BigDecimal original,
                                     BigDecimal residual, BigDecimal straightMonthly) {
        if (method == ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE) {
            if (monthIndex == months - 1) {
                BigDecimal beforeLast = straightMonthly.multiply(BigDecimal.valueOf(months - 1));
                return original.subtract(residual).subtract(beforeLast);
            }
            return straightMonthly;
        }
        return BigDecimal.ZERO;
    }

    // ---------- helpers ----------

    private ErpAstAssetCapitalization requireCap(Long id, IServiceContext context) {
        return requireEntity(String.valueOf(id), null, context);
    }

    private ErpAstAssetCategory loadCategory(Long categoryId) {
        return daoProvider().daoFor(ErpAstAssetCategory.class).getEntityById(categoryId);
    }

    private ErpAstAsset findAssetByCode(String code) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
        java.util.List<ErpAstAsset> list = daoProvider().daoFor(ErpAstAsset.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void cancelSchedules(Long assetId) {
        IEntityDao<ErpAstDepreciationSchedule> dao = daoProvider().daoFor(ErpAstDepreciationSchedule.class);
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("assetId", assetId));
        for (ErpAstDepreciationSchedule s : dao.findAllByQuery(q)) {
            s.setStatus(ErpAstConstants.SCHEDULE_STATUS_CANCELLED);
            dao.saveOrUpdateEntity(s);
        }
    }

    private void requireNotCancelled(ErpAstAssetCapitalization cap) {
        Integer docStatus = cap.getDocStatus();
        if (docStatus != null && docStatus == ErpAstConstants.DOC_STATUS_CANCELLED) {
            throw illegalDocTransition(cap, docStatus, "非已作废");
        }
    }

    private Integer currentApproveStatus(ErpAstAssetCapitalization cap) {
        Integer status = cap.getApproveStatus();
        return status != null ? status : ErpAstConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    private String resolveAssetCode(ErpAstAssetCapitalization cap) {
        if (cap.getAssetCode() != null && !cap.getAssetCode().trim().isEmpty()) {
            return cap.getAssetCode();
        }
        return "AST-" + cap.getId();
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

    private NopException illegalTransition(ErpAstAssetCapitalization cap, Integer current, String expected) {
        return new NopException(ErpAstErrors.ERR_CAPITALIZATION_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_CAPITALIZATION_CODE, cap.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    private NopException illegalDocTransition(ErpAstAssetCapitalization cap, Integer current, String expected) {
        return new NopException(ErpAstErrors.ERR_CAPITALIZATION_ILLEGAL_DOC_TRANSITION)
                .param(ErpAstErrors.ARG_CAPITALIZATION_CODE, cap.getCode())
                .param(ErpAstErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_DOC_STATUS, expected);
    }
}
