package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.MaintenanceCapitalizationPostingDispatcher;
import app.erp.ast.service.posting.MaintenanceExpensePostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstMaintenanceStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Objects;

/**
 * ErpAstMaintenance post per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含维修过账编排（CAPITALIZE 路径：原值增量+折旧重算+MAINTENANCE_CAPITALIZATION 凭证；EXPENSE 路径：MAINTENANCE_EXPENSE 凭证）；
 * 共享 protected helper 单一真相源在 {@link ErpAstMaintenanceProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstMaintenancePostProcessor {

    @Inject
    ErpAstMaintenanceProcessor facade;

    @Inject
    MaintenanceExpensePostingDispatcher expenseDispatcher;

    @Inject
    MaintenanceCapitalizationPostingDispatcher capitalizationDispatcher;

    @Inject
    ErpAstMaintenanceStateMachine stateMachine;

    public ErpAstMaintenance post(Long id, IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        // 固定来源态守卫委托 StateMachine Bean（M4.53，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            stateMachine.assertCanPost(m.getStatus());
        } catch (NopException e) {
            throw facade.mapIllegalTransition(e, m, ErpAstConstants.MAINTENANCE_STATUS_COMPLETED);
        }
        if (Boolean.TRUE.equals(m.getPosted())) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_ALREADY_POSTED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        if (m.getTreatment() == null) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        if (facade.isApprovalRequired()) {
            if (m.getApprovedAt() == null) {
                throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_TREATMENT_NOT_DECIDED)
                        .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
            }
        }
        BigDecimal totalCost = facade.aggregateCost(id);
        if (totalCost.signum() <= 0) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_NO_COST)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        m.setTotalCostAmount(totalCost);

        ErpAstAsset asset = facade.requireAsset(m.getAssetId());
        ErpAstAssetCategory category = asset.getCategory();

        Long voucherId;
        if (Objects.equals(m.getTreatment(), ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)) {
            facade.applyTreatmentCapitalize(m, asset, context);
            facade.orm().flushSession();
            voucherId = capitalizationDispatcher.tryPost(m, asset, category);
        } else {
            voucherId = expenseDispatcher.tryPost(m, asset, category);
        }

        m = facade.reload(id);
        m.setStatus(stateMachine.postTargetStatus());
        Timestamp now = CoreMetrics.currentTimestamp();
        if (voucherId != null) {
            m.setPosted(true);
            m.setPostedAt(now);
            m.setPostedBy(facade.currentUserId());
        }
        facade.maintenanceDao().updateEntity(m);
        return m;
    }
}
