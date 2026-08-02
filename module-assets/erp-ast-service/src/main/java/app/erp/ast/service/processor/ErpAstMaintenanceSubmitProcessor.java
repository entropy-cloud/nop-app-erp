package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.ErpAstConstants;
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

    public ErpAstMaintenance submit(Long id, IServiceContext context) {
        ErpAstMaintenance m = facade.requireMaintenance(id, context);
        facade.validateTransition(m, ErpAstConstants.MAINTENANCE_STATUS_DRAFT, "submit");
        m.setStatus(ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED);
        facade.maintenanceDao().updateEntity(m);
        return m;
    }
}
