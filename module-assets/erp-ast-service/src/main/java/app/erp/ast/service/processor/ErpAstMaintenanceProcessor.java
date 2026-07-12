package app.erp.ast.service.processor;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.dao.entity.ErpAstMaintenanceCost;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.MaintenanceCapitalizationPostingDispatcher;
import app.erp.ast.service.posting.MaintenanceExpensePostingDispatcher;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 资产维修编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 *
 * <p>维修工单状态机：DRAFT→SUBMITTED→IN_PROGRESS→COMPLETED→POSTED（+ CANCELLED）。
 * 费用归集在 IN_PROGRESS 态；裁决处置（CAPITALIZE/EXPENSE）在 COMPLETED 态。
 * post 按 treatment 分派：CAPITALIZE 路径（原值增量 + 折旧重算 + MAINTENANCE_CAPITALIZATION 凭证）
 * 或 EXPENSE 路径（MAINTENANCE_EXPENSE 凭证）。reverse 红冲纠错。
 *
 * <p>详见 owner doc {@code docs/design/assets/maintenance.md}。
 */
public class ErpAstMaintenanceProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    MaintenanceExpensePostingDispatcher expenseDispatcher;

    @Inject
    MaintenanceCapitalizationPostingDispatcher capitalizationDispatcher;

    @Inject
    IErpAstDepreciationScheduleBiz depreciationScheduleBiz;

    // ---------- public actions ----------

    public ErpAstMaintenance createMaintenance(Long assetId, String code, String name, String businessDate,
                                                Long maintenanceVisitId, String reason, IServiceContext context) {
        ErpAstAsset asset = requireAsset(assetId);
        validateAssetNotTerminal(asset, context);

        IEntityDao<ErpAstMaintenance> dao = maintenanceDao();
        ErpAstMaintenance maintenance = dao.newEntity();
        maintenance.setCode(code);
        maintenance.setName(name);
        maintenance.setOrgId(asset.getOrgId());
        maintenance.setAssetId(assetId);
        maintenance.setMaintenanceVisitId(maintenanceVisitId);
        maintenance.setStatus(ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        maintenance.setBusinessDate(parseDate(businessDate));
        maintenance.setCurrencyId(asset.getCurrencyId());
        maintenance.setCapitalizedAmount(BigDecimal.ZERO);
        maintenance.setTotalCostAmount(BigDecimal.ZERO);
        maintenance.setPosted(false);
        maintenance.setReversed(false);
        maintenance.setReason(reason);
        dao.saveEntity(maintenance);
        return maintenance;
    }

    public ErpAstMaintenance submit(Long id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_DRAFT, "submit");
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED);
        maintenanceDao().updateEntity(m);
        return m;
    }

    public ErpAstMaintenance startWork(Long id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED, "startWork");
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS);
        maintenanceDao().updateEntity(m);
        return m;
    }

    public ErpAstMaintenance completeWork(Long id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS, "completeWork");
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_COMPLETED);
        maintenanceDao().updateEntity(m);
        return m;
    }

    public ErpAstMaintenance decideTreatment(Long id, String treatment, BigDecimal capitalizedAmount,
                                              IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_COMPLETED, "decideTreatment");
        if (!Objects.equals(treatment, ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)
                && !Objects.equals(treatment, ErpAstConstants.MAINTENANCE_TREATMENT_EXPENSE)) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode())
                    .param(ErpAstErrors.ARG_TREATMENT, treatment);
        }
        BigDecimal totalCost = aggregateCost(id);
        BigDecimal capAmount = capitalizedAmount != null ? capitalizedAmount : totalCost;

        if (Objects.equals(treatment, ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)) {
            BigDecimal threshold = AppConfig.var(ErpAstConstants.CONFIG_MAINTENANCE_CAPITALIZE_THRESHOLD,
                    BigDecimal.ZERO);
            if (threshold.signum() > 0 && capAmount.compareTo(threshold) < 0) {
                throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_CAPITALIZE_BELOW_THRESHOLD)
                        .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode())
                        .param(ErpAstErrors.ARG_AMOUNT, capAmount)
                        .param(ErpAstErrors.ARG_THRESHOLD, threshold);
            }
        }

        m.setTreatment(treatment);
        m.setCapitalizedAmount(capAmount);
        m.setTotalCostAmount(totalCost);
        maintenanceDao().updateEntity(m);
        return m;
    }

    public ErpAstMaintenance approve(Long id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        if (Objects.equals(m.getStatus(), ErpAstConstants.MAINTENANCE_STATUS_POSTED)) {
            return m;
        }
        if (!Objects.equals(m.getStatus(), ErpAstConstants.MAINTENANCE_STATUS_COMPLETED)) {
            throw illegalTransition(m, m.getStatus(), "COMPLETED");
        }
        m.setApprovedBy(currentUserId());
        m.setApprovedAt(CoreMetrics.currentDateTime());
        maintenanceDao().updateEntity(m);
        return m;
    }

    public ErpAstMaintenance post(Long id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_COMPLETED, "post");
        if (Boolean.TRUE.equals(m.getPosted())) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_ALREADY_POSTED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        if (m.getTreatment() == null) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        if (isApprovalRequired()) {
            if (m.getApprovedAt() == null) {
                throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED)
                        .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
            }
        }
        BigDecimal totalCost = aggregateCost(id);
        if (totalCost.signum() <= 0) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_NO_COST)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        m.setTotalCostAmount(totalCost);

        ErpAstAsset asset = requireAsset(m.getAssetId());
        ErpAstAssetCategory category = asset.getCategoryId() == null ? null
                : daoProvider.daoFor(ErpAstAssetCategory.class).getEntityById(asset.getCategoryId());

        Long voucherId;
        if (Objects.equals(m.getTreatment(), ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)) {
            applyTreatmentCapitalize(m, asset, context);
            orm().flushSession();
            voucherId = capitalizationDispatcher.tryPost(m, asset, category);
        } else {
            voucherId = expenseDispatcher.tryPost(m, asset, category);
        }

        m = reload(id);
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_POSTED);
        LocalDateTime now = CoreMetrics.currentDateTime();
        if (voucherId != null) {
            m.setPosted(true);
            m.setPostedAt(now);
            m.setPostedBy(currentUserId());
        }
        maintenanceDao().updateEntity(m);
        return m;
    }

    public ErpAstMaintenance cancel(Long id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        String status = m.getStatus();
        if (!Objects.equals(status, ErpAstConstants.MAINTENANCE_STATUS_DRAFT)
                && !Objects.equals(status, ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED)) {
            throw illegalTransition(m, status, "DRAFT 或 SUBMITTED");
        }
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_CANCELLED);
        maintenanceDao().updateEntity(m);
        return m;
    }

    public ErpAstMaintenance reverse(Long id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        if (Boolean.TRUE.equals(m.getReversed())) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_ALREADY_REVERSED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        if (!Boolean.TRUE.equals(m.getPosted())) {
            throw illegalTransition(m, m.getStatus(), "POSTED");
        }

        if (Objects.equals(m.getTreatment(), ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)) {
            capitalizationDispatcher.reverse(m);
            rollbackCapitalization(m, context);
        } else {
            expenseDispatcher.reverse(m);
        }

        m = reload(id);
        m.setPosted(false);
        m.setPostedAt(null);
        m.setPostedBy(null);
        m.setReversed(true);
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_COMPLETED);
        maintenanceDao().updateEntity(m);
        return m;
    }

    // ---------- step：资本化/费用化裁决执行（protected，下游可逐个覆盖） ----------

    protected void applyTreatmentCapitalize(ErpAstMaintenance m, ErpAstAsset asset, IServiceContext context) {
        BigDecimal increment = nz(m.getCapitalizedAmount());
        if (increment.signum() <= 0) {
            return;
        }
        // 资产卡片原值 += 增量，净值同步调整
        asset.setOriginalValue(nz(asset.getOriginalValue()).add(increment));
        asset.setCurrentValue(nz(asset.getCurrentValue()).add(increment));
        asset.setNetBookValue(nz(asset.getNetBookValue()).add(increment));
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);

        // 折旧计划重算（config-gated）
        if (shouldAdjustDepreciationBase()) {
            depreciationScheduleBiz.recalculateForCapitalizationMaintenance(asset.getId(), increment, context);
        }
    }

    protected void rollbackCapitalization(ErpAstMaintenance m, IServiceContext context) {
        ErpAstAsset asset = requireAsset(m.getAssetId());
        BigDecimal increment = nz(m.getCapitalizedAmount());
        if (increment.signum() <= 0) {
            return;
        }
        asset.setOriginalValue(nz(asset.getOriginalValue()).subtract(increment));
        asset.setCurrentValue(nz(asset.getCurrentValue()).subtract(increment));
        asset.setNetBookValue(nz(asset.getNetBookValue()).subtract(increment));
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(asset);

        if (shouldAdjustDepreciationBase()) {
            // 回退重算：用负增量删除重算生成的条目并恢复（负增量使基数回到原值）
            depreciationScheduleBiz.recalculateForCapitalizationMaintenance(asset.getId(), increment.negate(),
                    context);
        }
    }

    protected boolean shouldAdjustDepreciationBase() {
        return AppConfig.var(ErpAstConstants.CONFIG_MAINTENANCE_CAP_ADJUST_DEPRECIATION_BASE, true);
    }

    public boolean isApprovalRequired() {
        return AppConfig.var(ErpAstConstants.CONFIG_MAINTENANCE_REQUIRE_APPROVAL, true);
    }

    // ---------- step：迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransition(ErpAstMaintenance m, String expected, String action) {
        String current = m.getStatus();
        if (!Objects.equals(current, expected)) {
            throw illegalTransition(m, current, expected);
        }
    }

    protected void validateAssetNotTerminal(ErpAstAsset asset, IServiceContext context) {
        String status = asset.getStatus();
        if (Objects.equals(status, ErpAstConstants.ASSET_STATUS_SCRAPPED)
                || Objects.equals(status, ErpAstConstants.ASSET_STATUS_SOLD)
                || Objects.equals(status, ErpAstConstants.ASSET_STATUS_DISPOSED)) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_ASSET_TERMINAL)
                    .param(ErpAstErrors.ARG_ASSET_CODE, asset.getCode());
        }
    }

    // ---------- 费用归集辅助 ----------

    protected BigDecimal aggregateCost(Long maintenanceId) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpAstMaintenanceCost line : findCostLines(maintenanceId)) {
            total = total.add(nz(line.getAmount()));
        }
        return total;
    }

    protected List<ErpAstMaintenanceCost> findCostLines(Long maintenanceId) {
        IEntityDao<ErpAstMaintenanceCost> dao = daoProvider.daoFor(ErpAstMaintenanceCost.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("maintenanceId", maintenanceId));
        return dao.findAllByQuery(q);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstMaintenance requireMaintenance(Long id, IServiceContext context) {
        ErpAstMaintenance m = maintenanceDao().getEntityById(id);
        if (m == null) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_NOT_FOUND)
                    .param(ErpAstErrors.ARG_MAINTENANCE_ID, id);
        }
        return m;
    }

    protected ErpAstAsset requireAsset(Long assetId) {
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        if (asset == null) {
            throw new NopException(ErpAstErrors.ERR_ASSET_NOT_FOUND)
                    .param(ErpAstErrors.ARG_ASSET_ID, assetId);
        }
        return asset;
    }

    protected ErpAstMaintenance reload(Long id) {
        return maintenanceDao().getEntityById(id);
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpAstMaintenance> maintenanceDao() {
        return daoProvider.daoFor(ErpAstMaintenance.class);
    }

    protected IOrmTemplate orm() {
        return ((IOrmEntityDao<?>) maintenanceDao()).getOrmTemplate();
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return CoreMetrics.today();
        }
        try {
            return LocalDate.parse(dateStr.trim());
        } catch (Exception e) {
            return CoreMetrics.today();
        }
    }

    protected NopException illegalTransition(ErpAstMaintenance m, String current, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
