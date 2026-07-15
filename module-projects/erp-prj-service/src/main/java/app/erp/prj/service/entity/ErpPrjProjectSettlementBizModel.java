package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjProjectSettlementBiz;
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.processor.ErpPrjProjectSettlementProcessor;
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
    ErpPrjProjectSettlementProcessor settlementProcessor;

    public ErpPrjProjectSettlementBizModel() {
        setEntityName(ErpPrjProjectSettlement.class.getName());
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement createSettlement(@Name("projectId") Long projectId,
                                                    @Name("settlementType") String settlementType,
                                                    IServiceContext context) {
        return settlementProcessor.createSettlement(projectId, settlementType, context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement submit(@Name("id") Long id, IServiceContext context) {
        return settlementProcessor.submit(id, context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement approve(@Name("id") Long id, IServiceContext context) {
        return settlementProcessor.approve(id, context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement reject(@Name("id") Long id, IServiceContext context) {
        return settlementProcessor.reject(id, context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement cancel(@Name("id") Long id, IServiceContext context) {
        return settlementProcessor.cancel(id, context);
    }

    @Override
    @BizMutation
    public ErpPrjProjectSettlement reverseSettlement(@Name("settlementId") Long settlementId, IServiceContext context) {
        return settlementProcessor.reverseSettlement(settlementId, context);
    }

}
