
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinNotesReceivableBiz;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.processor.ErpFinNotesReceivableProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
    ErpFinNotesReceivableProcessor notesReceivableProcessor;

    public ErpFinNotesReceivableBizModel() {
        setEntityName(ErpFinNotesReceivable.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable receive(@Name("notesId") Long notesId, IServiceContext context) {
        return notesReceivableProcessor.receive(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable discount(@Name("notesId") Long notesId,
                                           @Name("discountDate") LocalDate discountDate,
                                           @Name("bankId") Long bankId,
                                           @Name("discountRate") BigDecimal discountRate,
                                           IServiceContext context) {
        return notesReceivableProcessor.discount(notesId, discountDate, bankId, discountRate, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable endorse(@Name("notesId") Long notesId,
                                          @Name("endorsementFromId") Long endorsementFromId,
                                          IServiceContext context) {
        return notesReceivableProcessor.endorse(notesId, endorsementFromId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable collect(@Name("notesId") Long notesId, IServiceContext context) {
        return notesReceivableProcessor.collect(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable honor(@Name("notesId") Long notesId, IServiceContext context) {
        return notesReceivableProcessor.honor(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable dishonor(@Name("notesId") Long notesId, IServiceContext context) {
        return notesReceivableProcessor.dishonor(notesId, context);
    }

    @Override
    @BizMutation
    public ErpFinNotesReceivable writeOff(@Name("notesId") Long notesId, IServiceContext context) {
        return notesReceivableProcessor.writeOff(notesId, context);
    }

    // ---------- 高价值外键名称解析（机制 D）----------
    // endorsementFromId（背书链路自引用）+ discountId（贴现明细，ErpFinNotesDiscount 无 code 列）保留原始 ID。

    @BizLoader(forType = ErpFinNotesReceivable.class)
    public List<String> orgName(@ContextSource List<ErpFinNotesReceivable> notes) {
        orm().batchLoadProps(notes, Collections.singleton("org"));
        List<String> result = new ArrayList<>(notes.size());
        for (ErpFinNotesReceivable note : notes) {
            result.add(note.getOrg() != null ? note.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinNotesReceivable.class)
    public List<String> currencyName(@ContextSource List<ErpFinNotesReceivable> notes) {
        orm().batchLoadProps(notes, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(notes.size());
        for (ErpFinNotesReceivable note : notes) {
            result.add(note.getCurrency() != null ? note.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinNotesReceivable.class)
    public List<String> partnerName(@ContextSource List<ErpFinNotesReceivable> notes) {
        orm().batchLoadProps(notes, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(notes.size());
        for (ErpFinNotesReceivable note : notes) {
            result.add(note.getPartner() != null ? note.getPartner().getName() : null);
        }
        return result;
    }
}
