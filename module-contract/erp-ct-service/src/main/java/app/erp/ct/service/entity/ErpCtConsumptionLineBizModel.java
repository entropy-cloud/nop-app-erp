package app.erp.ct.service.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.common.service.MaskHelper;
import app.erp.ct.biz.ErpCtConsumptionPeriodSummarizeResult;
import app.erp.ct.biz.IErpCtConsumptionLineBiz;
import app.erp.ct.service.processor.ErpCtConsumptionPeriodSummarizeProcessor;
import app.erp.contract.dao.entity.ErpCtConsumptionLine;

@BizModel("ErpCtConsumptionLine")
public class ErpCtConsumptionLineBizModel extends CrudBizModel<ErpCtConsumptionLine> implements IErpCtConsumptionLineBiz{

    @Inject
    ErpCtConsumptionPeriodSummarizeProcessor periodSummarizeProcessor;

    public ErpCtConsumptionLineBizModel(){
        setEntityName(ErpCtConsumptionLine.class.getName());
    }

    @Override
    @BizMutation
    public ErpCtConsumptionPeriodSummarizeResult periodSummarize(@Name("contractLineId") Long contractLineId,
                                                                 @Name("fromDate") LocalDate fromDate,
                                                                 @Name("toDate") LocalDate toDate,
                                                                 @Name("invoiceTerm") String invoiceTerm,
                                                                 @Name("planDate") LocalDate planDate,
                                                                 IServiceContext context) {
        return periodSummarizeProcessor.periodSummarize(
                contractLineId, fromDate, toDate, invoiceTerm, planDate, context);
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 合同审批人/合同专员；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> CT_AMOUNT_ROLES = Set.of(MaskHelper.ROLE_CT_APPROVER, MaskHelper.ROLE_CT_CLERK);

    @BizLoader("amount")
    public BigDecimal amountMask(@ContextSource ErpCtConsumptionLine entity) {
        return MaskHelper.maskDecimal(entity.getAmount(), CT_AMOUNT_ROLES, entity, "amount");
    }

}
