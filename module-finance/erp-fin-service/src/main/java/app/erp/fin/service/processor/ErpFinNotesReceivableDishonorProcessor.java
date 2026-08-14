package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.statemachine.ErpFinNotesReceivableStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinNotesReceivable dishonor per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含拒付转应收编排；共享 protected helper 单一真相源在 {@link ErpFinNotesReceivableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>守卫委托 {@link ErpFinNotesReceivableStateMachine#assertCanDishonor(String)}（writer 物理位置保留本类——
 * intentional legacy layout，迁移仅改守卫委托不改 writer 位置，见 plan Phase 1 Decision）。
 */
public class ErpFinNotesReceivableDishonorProcessor {

    @Inject
    ErpFinNotesReceivableProcessor facade;

    @Inject
    ErpFinNotesReceivableStateMachine stateMachine;

    public ErpFinNotesReceivable dishonor(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = facade.requireNote(notesId, context);
        try {
            stateMachine.assertCanDishonor(note.getStatus());
        } catch (NopException e) {
            throw facade.illegalTransition(note, e);
        }
        // 拒付转应收（treasury.md §规则3）：仅标记终态，转挂应收账款科目；后续催收/坏账属信用管理面 Non-Goal。
        note.setStatus(stateMachine.dishonorTargetStatus());
        facade.noteDao().updateEntity(note);
        return note;
    }
}
