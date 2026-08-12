
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import app.erp.pur.biz.IErpPurRfqBiz;
import app.erp.pur.dao.entity.ErpPurRfq;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.statemachine.ErpPurRfqDocumentStateMachine;

/**
 * 询价单 BizModel。
 *
 * <p><b>docStatus cancel 守卫（plan 2026-08-12-0918-1 Phase 3 Fix）</b>：{@link #cancel} 经
 * {@link ErpPurRfqDocumentStateMachine} 断言来源态合法（owner doc §2「非已作废」守卫）。
 * 迁移前 cancel 无守卫（允许幂等 CANCELLED→CANCELLED）→ 经层 2 四方对照裁定为 implementation drift → Fix：
 * 已作废询价单再 cancel 抛领域码 {@link ErpPurErrors#ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION}。
 */
@BizModel("ErpPurRfq")
public class ErpPurRfqBizModel extends CrudBizModel<ErpPurRfq> implements IErpPurRfqBiz {

    @Inject
    ErpPurRfqDocumentStateMachine stateMachine;

    public ErpPurRfqBizModel(){
        setEntityName(ErpPurRfq.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurRfq cancel(@Name("rfqId") Long rfqId, IServiceContext context) {
        ErpPurRfq rfq = requireEntity(String.valueOf(rfqId), null, context);
        try {
            stateMachine.assertCanCancel(rfq.getDocStatus());
        } catch (NopException e) {
            throw new NopException(ErpPurErrors.ERR_RFQ_ILLEGAL_DOC_STATUS_TRANSITION)
                    .param(ErpPurErrors.ARG_RFQ_CODE, rfq.getCode())
                    .param(ErpPurErrors.ARG_CURRENT_DOC_STATUS, rfq.getDocStatus())
                    .param(ErpPurErrors.ARG_EXPECTED_DOC_STATUS, "非已作废");
        }
        rfq.setDocStatus(stateMachine.cancelTargetStatus());
        updateEntity(rfq, null, context);
        return rfq;
    }
}
