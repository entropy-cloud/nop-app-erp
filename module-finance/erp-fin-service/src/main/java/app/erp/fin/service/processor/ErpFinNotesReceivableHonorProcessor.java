package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinNotesReceivable honor per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含承兑编排；共享 protected helper 单一真相源在 {@link ErpFinNotesReceivableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinNotesReceivableHonorProcessor {

    @Inject
    ErpFinNotesReceivableProcessor facade;

    public ErpFinNotesReceivable honor(String notesId, IServiceContext context) {
        ErpFinNotesReceivable note = facade.requireNote(notesId, context);
        facade.validateTransitionForHonorOrDishonor(note, context);
        return facade.doHonor(notesId, note, context);
    }
}
