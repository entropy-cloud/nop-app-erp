
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinNotesReceivableBiz;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.processor.ErpFinNotesReceivableCollectProcessor;
import app.erp.fin.service.processor.ErpFinNotesReceivableDiscountProcessor;
import app.erp.fin.service.processor.ErpFinNotesReceivableDishonorProcessor;
import app.erp.fin.service.processor.ErpFinNotesReceivableEndorseProcessor;
import app.erp.fin.service.processor.ErpFinNotesReceivableHonorProcessor;
import app.erp.fin.service.processor.ErpFinNotesReceivableReceiveProcessor;
import app.erp.fin.service.processor.ErpFinNotesReceivableWriteOffProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 应收票据 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 7 态状态机 + NOTES_RECEIVABLE 业财过账编排委托
 * {@link ErpFinNotesReceivableProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义与配置门控见 {@code treasury.md}；{@code @BizMutation} 钉事务/会话边界。
 */
@BizModel("ErpFinNotesReceivable")
public class ErpFinNotesReceivableBizModel extends CrudBizModel<ErpFinNotesReceivable> implements IErpFinNotesReceivableBiz {

    @Inject
    ErpFinNotesReceivableReceiveProcessor receiveProcessor;
    @Inject
    ErpFinNotesReceivableDiscountProcessor discountProcessor;
    @Inject
    ErpFinNotesReceivableEndorseProcessor endorseProcessor;
    @Inject
    ErpFinNotesReceivableCollectProcessor collectProcessor;
    @Inject
    ErpFinNotesReceivableHonorProcessor honorProcessor;
    @Inject
    ErpFinNotesReceivableDishonorProcessor dishonorProcessor;
    @Inject
    ErpFinNotesReceivableWriteOffProcessor writeOffProcessor;

    public ErpFinNotesReceivableBizModel() {
        setEntityName(ErpFinNotesReceivable.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable receive(@Name("notesId") String notesId, IServiceContext context) {
        return receiveProcessor.receive(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable discount(@Name("notesId") String notesId,
                                           @Name("discountDate") LocalDate discountDate,
                                           @Name("bankId") String bankId,
                                           @Name("discountRate") BigDecimal discountRate,
                                           @Optional @Name("exchangeRate") BigDecimal exchangeRate,
                                           IServiceContext context) {
        return discountProcessor.discount(notesId, discountDate, bankId, discountRate, exchangeRate, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable endorse(@Name("notesId") String notesId,
                                          @Name("endorsementFromId") String endorsementFromId,
                                          IServiceContext context) {
        return endorseProcessor.endorse(notesId, endorsementFromId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable collect(@Name("notesId") String notesId, IServiceContext context) {
        return collectProcessor.collect(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable honor(@Name("notesId") String notesId, IServiceContext context) {
        return honorProcessor.honor(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable dishonor(@Name("notesId") String notesId, IServiceContext context) {
        return dishonorProcessor.dishonor(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable writeOff(@Name("notesId") String notesId, IServiceContext context) {
        return writeOffProcessor.writeOff(notesId, context);
    }

    // endorsementFromId（背书链路自引用）+ discountId（贴现明细，ErpFinNotesDiscount 无 code 列）保留原始 ID。

}
