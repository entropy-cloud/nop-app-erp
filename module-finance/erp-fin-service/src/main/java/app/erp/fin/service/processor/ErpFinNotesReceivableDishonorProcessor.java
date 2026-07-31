package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.ErpFinConstants;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinNotesReceivable dishonor per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含拒付转应收编排；共享 protected helper 单一真相源在 {@link ErpFinNotesReceivableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinNotesReceivableDishonorProcessor {

    @Inject
    ErpFinNotesReceivableProcessor facade;

    public ErpFinNotesReceivable dishonor(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = facade.requireNote(notesId, context);
        facade.validateTransitionForHonorOrDishonor(note, context);
        // 拒付转应收（treasury.md §规则3）：仅标记终态，转挂应收账款科目；后续催收/坏账属信用管理面 Non-Goal。
        note.setStatus(ErpFinConstants.NOTES_RECV_DISHONORED);
        facade.noteDao().updateEntity(note);
        return note;
    }
}
