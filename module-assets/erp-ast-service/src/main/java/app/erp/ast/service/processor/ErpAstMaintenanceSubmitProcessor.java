package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.statemachine.ErpAstMaintenanceStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpAstMaintenance submit per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含维修工单提交编排；共享 protected helper 单一真相源在 {@link ErpAstMaintenanceProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstMaintenanceSubmitProcessor {

    @Inject
    ErpAstMaintenanceProcessor facade;

    @Inject
    ErpAstMaintenanceStateMachine stateMachine;

    public ErpAstMaintenance submit(Long id, IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        // 固定来源态守卫委托 StateMachine Bean（M4.53，契约 §4/§7；Bean 抛 common 层码 → cause-chain 领域码）
        try {
            stateMachine.assertCanSubmit(m.getStatus());
        } catch (NopException e) {
            throw facade.mapIllegalTransition(e, m, ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        }
        m.setStatus(stateMachine.submitTargetStatus());
        facade.maintenanceDao().updateEntity(m);
        return m;
    }
}
