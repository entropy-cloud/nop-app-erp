package app.erp.drp.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.drp.biz.IErpInvDrpCrossDockBiz;
import app.erp.drp.dao.entity.ErpInvDrpCrossDock;
import app.erp.drp.service.processor.ErpInvDrpCrossDockProcessor;

/**
 * 越库执行记录 BizModel（RC-R1.81 / P1-RC-081，UC-DRP-07）。薄委派层：
 * 状态机 mutation 族（receiveMark/match/load/complete/cancel）+ purchase 收货审批后置 Facade
 * {@code markReceivedFromPurchase}（D1 裁决选项 A）委派 {@link ErpInvDrpCrossDockProcessor}
 * （protected step 可被 Delta 覆盖）。总门控 {@code erp-inv.drp-xdock-enabled} 默认 false。
 */
@BizModel("ErpInvDrpCrossDock")
public class ErpInvDrpCrossDockBizModel extends CrudBizModel<ErpInvDrpCrossDock> implements IErpInvDrpCrossDockBiz {

    @Inject
    ErpInvDrpCrossDockProcessor crossDockProcessor;

    public ErpInvDrpCrossDockBizModel() {
        setEntityName(ErpInvDrpCrossDock.class.getName());
    }

    public void setCrossDockProcessor(ErpInvDrpCrossDockProcessor crossDockProcessor) {
        this.crossDockProcessor = crossDockProcessor;
    }

    @Override
    @BizMutation
    public ErpInvDrpCrossDock receiveMark(@Name("id") Long id, @Name("inboundMoveId") Long inboundMoveId,
                                          IServiceContext context) {
        return crossDockProcessor.receiveMark(id, inboundMoveId, context);
    }

    @Override
    @BizMutation
    public ErpInvDrpCrossDock match(@Name("id") Long id, @Optional @Name("targetBillType") String targetBillType,
                                    @Optional @Name("targetBillCode") String targetBillCode, IServiceContext context) {
        return crossDockProcessor.match(id, targetBillType, targetBillCode, context);
    }

    @Override
    @BizMutation
    public ErpInvDrpCrossDock load(@Name("id") Long id, IServiceContext context) {
        return crossDockProcessor.load(id, context);
    }

    @Override
    @BizMutation
    public ErpInvDrpCrossDock complete(@Name("id") Long id, IServiceContext context) {
        return crossDockProcessor.complete(id, context);
    }

    @Override
    @BizMutation
    public ErpInvDrpCrossDock cancel(@Name("id") Long id, IServiceContext context) {
        return crossDockProcessor.cancel(id, context);
    }

    @Override
    @BizMutation
    public int markReceivedFromPurchase(@Name("purchaseOrderCode") String purchaseOrderCode,
                                        @Name("inboundMoveId") Long inboundMoveId,
                                        @Name("materialIds") List<Long> materialIds, IServiceContext context) {
        return crossDockProcessor.markReceivedFromPurchase(purchaseOrderCode, inboundMoveId, materialIds, context);
    }
}
