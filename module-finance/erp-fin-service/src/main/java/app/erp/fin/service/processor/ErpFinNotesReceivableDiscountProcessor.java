package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesDiscount;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ErpFinNotesReceivable discount per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含贴现编排；共享 protected helper 单一真相源在 {@link ErpFinNotesReceivableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinNotesReceivableDiscountProcessor {

    @Inject
    ErpFinNotesReceivableProcessor facade;

    public ErpFinNotesReceivable discount(String notesId, LocalDate discountDate, String bankId,
                                          BigDecimal discountRate, BigDecimal exchangeRate, IServiceContext context) {
        ErpFinNotesReceivable note = facade.requireNote(notesId, context);
        facade.validateTransitionForDiscount(note, context);
        facade.requireDiscountInputs(note, discountDate, bankId, discountRate, exchangeRate, context);
        ErpFinNotesDiscount discount = facade.buildDiscount(note, discountDate, bankId, discountRate, exchangeRate);
        return facade.doDiscount(notesId, note, discount, context);
    }
}
