package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesPayable;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinNotesPayable dishonor per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含拒付编排；共享 protected helper 单一真相源在 {@link ErpFinNotesPayableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinNotesPayableDishonorProcessor {

    @Inject
    ErpFinNotesPayableProcessor facade;

    public ErpFinNotesPayable dishonor(Long notesId, IServiceContext context) {
        ErpFinNotesPayable note = facade.requireNote(notesId, context);
        facade.validateTransitionForDishonor(note, context);
        facade.releaseOccupiedCredit(note, context);
        facade.doDishonor(note, context);
        return note;
    }
}
