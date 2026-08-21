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
import app.erp.ast.service.statemachine.ErpAstMaintenanceStateMachine;
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
import java.sql.Timestamp;
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

    @Inject
    ErpAstMaintenanceApproveProcessor approveProcessor;

    @Inject
    ErpAstMaintenanceStateMachine stateMachine;

    // ---------- public actions ----------
    // D-mutation 公共入口（createMaintenance/submit/startWork/completeWork/decideTreatment/post/reverse）已按 R6.3
    // 拆为独立 per-mutation Processor。本 facade 处置 = slim-to-S-delegation-facade：
    // 保留 approve（S-mutation 单行委托）+ cancel（`:45` 单步状态翻转豁免）+ protected helper（单一真相源）。

    public ErpAstMaintenance approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpAstMaintenance cancel(String id, IServiceContext context) {
        ErpAstMaintenance m = requireMaintenance(id, context);
        String status = m.getStatus();
        // 固定来源态守卫委托 StateMachine Bean（M4.53，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            stateMachine.assertCanCancel(status);
        } catch (NopException e) {
            throw mapIllegalTransition(e, m, "DRAFT 或 SUBMITTED");
        }
        m.setStatus(stateMachine.cancelTargetStatus());
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
            depreciationScheduleBiz.recalculateForCapitalizationMaintenance(asset.getId(),
                    increment, context);
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
            depreciationScheduleBiz.recalculateForCapitalizationMaintenance(asset.getId(),
                    increment.negate(), context);
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

    protected BigDecimal aggregateCost(String maintenanceId) {
        BigDecimal total = BigDecimal.ZERO;
        for (ErpAstMaintenanceCost line : findCostLines(maintenanceId)) {
            total = total.add(nz(line.getAmount()));
        }
        return total;
    }

    protected List<ErpAstMaintenanceCost> findCostLines(String maintenanceId) {
        IEntityDao<ErpAstMaintenanceCost> dao = daoProvider.daoFor(ErpAstMaintenanceCost.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("maintenanceId", maintenanceId));
        return dao.findAllByQuery(q);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpAstMaintenance requireMaintenance(String id, IServiceContext context) {
        ErpAstMaintenance m = maintenanceDao().getEntityById(id);
        if (m == null) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_NOT_FOUND)
                    .param(ErpAstErrors.ARG_MAINTENANCE_ID, id);
        }
        return m;
    }

    protected ErpAstAsset requireAsset(String assetId) {
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        if (asset == null) {
            throw new NopException(ErpAstErrors.ERR_ASSET_NOT_FOUND)
                    .param(ErpAstErrors.ARG_ASSET_ID, assetId);
        }
        return asset;
    }

    protected ErpAstMaintenance reload(String id) {
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

    /**
     * Bean 非法边（common 层码）→ 领域码 cause-chain 映射（契约 §7；M4.53）。
     * 保持错误码值/参数形状与 {@link #illegalTransition} 一致。
     */
    protected NopException mapIllegalTransition(NopException beanException, ErpAstMaintenance m, String expected) {
        return new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_ILLEGAL_STATUS_TRANSITION, beanException)
                .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, m.getStatus())
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
