package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import app.erp.fin.service.statemachine.ErpFinNotesReceivableStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinNotesReceivable collect per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含托收编排；共享 protected helper 单一真相源在 {@link ErpFinNotesReceivableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 *
 * <p>守卫委托 {@link ErpFinNotesReceivableStateMachine#assertCanCollect(String)}（writer 物理位置保留本类——
 * intentional legacy layout，迁移仅改守卫委托不改 writer 位置，见 plan Phase 1 Decision）。
 */
public class ErpFinNotesReceivableCollectProcessor {

    @Inject
    ErpFinNotesReceivableProcessor facade;

    @Inject
    ErpFinNotesReceivableStateMachine stateMachine;

    public ErpFinNotesReceivable collect(Long notesId, IServiceContext context) {
        ErpFinNotesReceivable note = facade.requireNote(notesId, context);
        try {
            stateMachine.assertCanCollect(note.getStatus());
        } catch (NopException e) {
            throw facade.illegalTransition(note, e);
        }
        note.setStatus(stateMachine.collectTargetStatus());
        facade.noteDao().updateEntity(note);
        return note;
    }
}
