
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstMaintenanceBiz;
import app.erp.ast.dao.entity.ErpAstMaintenance;
import app.erp.ast.service.processor.ErpAstMaintenanceApproveProcessor;
import app.erp.ast.service.processor.ErpAstMaintenanceCompleteWorkProcessor;
import app.erp.ast.service.processor.ErpAstMaintenanceCreateMaintenanceProcessor;
import app.erp.ast.service.processor.ErpAstMaintenanceDecideTreatmentProcessor;
import app.erp.ast.service.processor.ErpAstMaintenancePostProcessor;
import app.erp.ast.service.processor.ErpAstMaintenanceProcessor;
import app.erp.ast.service.processor.ErpAstMaintenanceReverseProcessor;
import app.erp.ast.service.processor.ErpAstMaintenanceStartWorkProcessor;
import app.erp.ast.service.processor.ErpAstMaintenanceSubmitProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 资产维修 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 维修工单状态机编排委托对应 per-mutation Processor（R6.3 拆分，protected step 方法，下游可逐 step 覆盖）；
 * approve（S-mutation）委托 {@link ErpAstMaintenanceApproveProcessor}；cancel（`:45` 单步状态翻转豁免）保留委托
 * {@link ErpAstMaintenanceProcessor}。
 *
 * <p>语义见 {@code docs/design/assets/maintenance.md}；{@code @BizMutation} 钉事务/会话边界。
 */
@BizModel("ErpAstMaintenance")
public class ErpAstMaintenanceBizModel extends CrudBizModel<ErpAstMaintenance>
        implements IErpAstMaintenanceBiz {

    @Inject
    ErpAstMaintenanceProcessor maintenanceProcessor;

    @Inject
    ErpAstMaintenanceApproveProcessor approveProcessor;

    @Inject
    ErpAstMaintenanceCreateMaintenanceProcessor createMaintenanceProcessor;

    @Inject
    ErpAstMaintenanceSubmitProcessor submitProcessor;

    @Inject
    ErpAstMaintenanceStartWorkProcessor startWorkProcessor;

    @Inject
    ErpAstMaintenanceCompleteWorkProcessor completeWorkProcessor;

    @Inject
    ErpAstMaintenanceDecideTreatmentProcessor decideTreatmentProcessor;

    @Inject
    ErpAstMaintenancePostProcessor postProcessor;

    @Inject
    ErpAstMaintenanceReverseProcessor reverseProcessor;

    public ErpAstMaintenanceBizModel() {
        setEntityName(ErpAstMaintenance.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstMaintenance createMaintenance(@Name("assetId") String assetId,
                                               @Name("code") String code,
                                               @Name("name") @Optional String name,
                                               @Name("businessDate") @Optional String businessDate,
                                               @Name("maintenanceVisitId") @Optional String maintenanceVisitId,
                                               @Name("reason") @Optional String reason,
                                               IServiceContext context) {
        return createMaintenanceProcessor.createMaintenance(assetId, code, name, businessDate, maintenanceVisitId,
                reason, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance submit(@Name("id") String id, IServiceContext context) {
        return submitProcessor.submit(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance startWork(@Name("id") String id, IServiceContext context) {
        return startWorkProcessor.startWork(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance completeWork(@Name("id") String id, IServiceContext context) {
        return completeWorkProcessor.completeWork(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance decideTreatment(@Name("id") String id,
                                             @Name("treatment") String treatment,
                                             @Name("capitalizedAmount") @Optional BigDecimal capitalizedAmount,
                                             IServiceContext context) {
        return decideTreatmentProcessor.decideTreatment(id, treatment, capitalizedAmount, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance approve(@Name("id") String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance post(@Name("id") String id, IServiceContext context) {
        return postProcessor.post(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance cancel(@Name("id") String id, IServiceContext context) {
        return maintenanceProcessor.cancel(id, context);
    }

    @Override
    @BizMutation
    public ErpAstMaintenance reverse(@Name("id") String id, IServiceContext context) {
        return reverseProcessor.reverse(id, context);
    }
}
