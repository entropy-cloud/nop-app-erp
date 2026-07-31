package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjProjectSettlementBiz;
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.processor.ErpPrjProjectSettlementApproveProcessor;
import app.erp.prj.service.processor.ErpPrjProjectSettlementCancelProcessor;
import app.erp.prj.service.processor.ErpPrjProjectSettlementCreateSettlementProcessor;
import app.erp.prj.service.processor.ErpPrjProjectSettlementRejectProcessor;
import app.erp.prj.service.processor.ErpPrjProjectSettlementReverseSettlementProcessor;
import app.erp.prj.service.processor.ErpPrjProjectSettlementSubmitForApprovalProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 项目结算单 BizModel（Facade，{@code profitability.md §关键流程 2/3}）。CRUD 之上承载三轴状态机入口，
 * 编排委托 {@link ErpPrjProjectSettlementProcessor}（processor-extension-pattern：Facade 入口 + Processor 编排）。
 *
 * <p>{@code approve} 末尾按 settlementType 分派：FINAL/INTERIM 仅过账；CLOSE 额外转固建卡 + 凭证。
 * {@code reverseSettlement} 红冲凭证 + 回退卡片状态。
 */
@BizModel("ErpPrjProjectSettlement")
public class ErpPrjProjectSettlementBizModel extends CrudBizModel<ErpPrjProjectSettlement>
        implements IErpPrjProjectSettlementBiz {

    @Inject
    ErpPrjProjectSettlementCreateSettlementProcessor createSettlementProcessor;

    @Inject
    ErpPrjProjectSettlementSubmitForApprovalProcessor submitForApprovalProcessor;

    @Inject
    ErpPrjProjectSettlementApproveProcessor approveProcessor;

    @Inject
    ErpPrjProjectSettlementRejectProcessor rejectProcessor;

    @Inject
    ErpPrjProjectSettlementCancelProcessor cancelProcessor;

    @Inject
    ErpPrjProjectSettlementReverseSettlementProcessor reverseSettlementProcessor;

    public ErpPrjProjectSettlementBizModel() {
        setEntityName(ErpPrjProjectSettlement.class.getName());
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement createSettlement(@Name("projectId") Long projectId,
                                                    @Name("settlementType") String settlementType,
                                                    IServiceContext context) {
        return createSettlementProcessor.createSettlement(projectId, settlementType, context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement submit(@Name("id") Long id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement approve(@Name("id") Long id, IServiceContext context) {
        return approveProcessor.approve(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement reject(@Name("id") Long id, IServiceContext context) {
        return rejectProcessor.reject(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement cancel(@Name("id") Long id, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(id), context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement reverseSettlement(@Name("settlementId") Long settlementId, IServiceContext context) {
        return reverseSettlementProcessor.reverseSettlement(settlementId, context);
    }

}
