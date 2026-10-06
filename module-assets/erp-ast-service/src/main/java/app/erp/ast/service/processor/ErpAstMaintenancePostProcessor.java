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

    public ErpAstMaintenance post(String id, IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        // 固定来源态守卫委托 StateMachine Bean（M4.53，契约 §4；Bean 直抛领域码 + 调用点同码补参，plan 2026-09-07-2200-1）
        try {
            stateMachine.assertCanPost(m.getStatus());
        } catch (NopException e) {
            throw e.param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
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

        // P2-CK-ast2-009（分支 i，rollbackCapitalization 配对）：「过账成功才翻 POSTED」——修复前
        // L82 无条件 setStatus(postTargetStatus()) 在 voucherId 判定之前，GL 失败仍封死重试
        // （assertCanPost 要求 COMPLETED）且无告警；CAPITALIZE 路径失败时 applyTreatmentCapitalize
        // 已做资产原值/现值/净值三值增量（非幂等），不配对回滚则重试双计增量而 GL 仅一凭证。
        String voucherId;
        if (Objects.equals(m.getTreatment(), ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)) {
            facade.applyTreatmentCapitalize(m, asset, context);
            facade.orm().flushSession();
            voucherId = capitalizationDispatcher.tryPost(m, asset, category);
            if (voucherId == null) {
                // GL 失败 → 对称回滚资产增量（F1.2 对称回滚范式，rollback 原语既有），重试安全
                facade.rollbackCapitalization(m, context);
            }
        } else {
            voucherId = expenseDispatcher.tryPost(m, asset, category);
        }

        m = facade.reload(id);
        Timestamp now = CoreMetrics.currentTimestamp();
        if (voucherId != null) {
            m.setStatus(stateMachine.postTargetStatus());
            m.setPosted(true);
            m.setPostedAt(now);
            m.setPostedBy(facade.currentUserId());
        }
        // voucherId == null（GL 失败）：状态保持 COMPLETED 可重试；失败告警由两 dispatcher 的
        // dispatchFailureAlert 派发（镜像折旧 G4 分级），无 DeferredPostingSweepJob 覆盖。
        facade.maintenanceDao().updateEntity(m);
        return m;
    }
}
