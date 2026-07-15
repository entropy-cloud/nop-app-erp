
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMaintenanceBiz;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.processor.ErpAstMaintenanceProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产维修 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 维修工单状态机编排委托 {@link ErpAstMaintenanceProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义见 {@code docs/design/assets/maintenance.md}；{@code @BizMutation} 钉事务/会话边界。
 */
@BizModel("ErpAstMaintenance")
public class ErpAstMaintenanceBizModel extends CrudBizModel<ErpAstMaintenance>
        implements IErpAstMaintenanceBiz {

    @Inject
    ErpAstMaintenanceProcessor maintenanceProcessor;

    public ErpAstMaintenanceBizModel() {
        setEntityName(ErpAstMaintenance.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstMaintenance createMaintenance(@Name("assetId") Long assetId,
                                               @Name("code") String code,
                                               @Name("name") @Optional String name,
                                               @Name("businessDate") @Optional String businessDate,
                                               @Name("maintenanceVisitId") @Optional Long maintenanceVisitId,
                                               @Name("reason") @Optional String reason,
                                               IServiceContext context) {
        return maintenanceProcessor.createMaintenance(assetId, code, name, businessDate, maintenanceVisitId, reason,
                context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance submit(@Name("id") Long id, IServiceContext context) {
        return maintenanceProcessor.submit(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance startWork(@Name("id") Long id, IServiceContext context) {
        return maintenanceProcessor.startWork(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance completeWork(@Name("id") Long id, IServiceContext context) {
        return maintenanceProcessor.completeWork(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance decideTreatment(@Name("id") Long id,
                                             @Name("treatment") String treatment,
                                             @Name("capitalizedAmount") @Optional BigDecimal capitalizedAmount,
                                             IServiceContext context) {
        return maintenanceProcessor.decideTreatment(id, treatment, capitalizedAmount, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance approve(@Name("id") Long id, IServiceContext context) {
        return maintenanceProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance post(@Name("id") Long id, IServiceContext context) {
        return maintenanceProcessor.post(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance cancel(@Name("id") Long id, IServiceContext context) {
        return maintenanceProcessor.cancel(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance reverse(@Name("id") Long id, IServiceContext context) {
        return maintenanceProcessor.reverse(id, context);
    }
}
