package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.ast.service.posting.MaintenanceCapitalizationPostingDispatcher;
import app.erp.ast.service.posting.MaintenanceExpensePostingDispatcher;
import app.erp.ast.service.statemachine.ErpAstMaintenanceStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpAstMaintenance reverse per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含维修红冲编排（CAPITALIZE 路径：红冲凭证+回退原值增量+折旧重算；EXPENSE 路径：红冲凭证）；
 * 共享 protected helper 单一真相源在 {@link ErpAstMaintenanceProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstMaintenanceReverseProcessor {

    @Inject
    ErpAstMaintenanceProcessor facade;

    @Inject
    MaintenanceExpensePostingDispatcher expenseDispatcher;

    @Inject
    MaintenanceCapitalizationPostingDispatcher capitalizationDispatcher;

    @Inject
    ErpAstMaintenanceStateMachine stateMachine;

    public ErpAstMaintenance reverse(String id, IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        if (Boolean.TRUE.equals(m.getReversed())) {
            throw new NopException(ErpAstErrors.ERR_AST_MAINTENANCE_ALREADY_REVERSED)
                    .param(ErpAstErrors.ARG_MAINTENANCE_CODE, m.getCode());
        }
        // posted boolean 为动态过账契约守卫（§11.2 M4 (iii) 不入轴），保留原位
        if (!Boolean.TRUE.equals(m.getPosted())) {
            throw facade.illegalTransition(m, m.getStatus(), "POSTED");
        }
        // 固定来源态守卫委托 StateMachine Bean（M4.53，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            stateMachine.assertCanReverse(m.getStatus());
        } catch (NopException e) {
            throw facade.mapIllegalTransition(e, m, ErpAstConstants.MAINTENANCE_STATUS_POSTED);
        }

        if (Objects.equals(m.getTreatment(), ErpAstConstants.MAINTENANCE_TREATMENT_CAPITALIZE)) {
            capitalizationDispatcher.reverse(m);
            facade.rollbackCapitalization(m, context);
        } else {
            expenseDispatcher.reverse(m);
        }

        m = facade.reload(id);
        m.setPosted(false);
        m.setPostedAt(null);
        m.setPostedBy(null);
        m.setReversed(true);
        m.setStatus(stateMachine.reverseTargetStatus());
        facade.maintenanceDao().updateEntity(m);
        return m;
    }
}
