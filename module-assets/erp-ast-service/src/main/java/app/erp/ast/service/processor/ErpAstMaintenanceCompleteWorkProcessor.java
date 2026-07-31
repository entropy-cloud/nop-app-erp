package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpAstMaintenance completeWork per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含维修完工编排；共享 protected helper 单一真相源在 {@link ErpAstMaintenanceProcessor}（slim-to-S-delegation facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstMaintenanceCompleteWorkProcessor {

    @Inject
    ErpAstMaintenanceProcessor facade;

    public ErpAstMaintenance completeWork(Long id, IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        facade.validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS, "completeWork");
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_COMPLETED);
        facade.maintenanceDao().updateEntity(m);
        return m;
    }
}
