package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinNotesReceivable receive per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含收到编排；共享 protected helper 单一真相源在 {@link ErpFinNotesReceivableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinNotesReceivableReceiveProcessor {

    @Inject
    ErpFinNotesReceivableProcessor facade;

    public ErpFinNotesReceivable receive(String notesId, IServiceContext context) {
        ErpFinNotesReceivable note = facade.requireNote(notesId, context);
        if (facade.isAlreadyReceived(note)) {
            return note;
        }
        // receive 为 initial 态写入（§9.2 选项 c），守卫有意收窄为 Bean assertCanReceive（null/RECEIVED 合法）。
        facade.validateTransitionForReceive(note, context);
        facade.requireAmountPositive(note, context);
        return facade.doReceive(notesId, note, context);
    }
}
