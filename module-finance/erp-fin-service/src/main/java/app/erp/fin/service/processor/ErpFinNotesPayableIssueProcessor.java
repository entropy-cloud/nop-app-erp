package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinNotesPayable;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpFinNotesPayable issue per-mutation Processor（R6.1，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含签发编排；共享 protected helper 单一真相源在 {@link ErpFinNotesPayableProcessor}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpFinNotesPayableIssueProcessor {

    @Inject
    ErpFinNotesPayableProcessor facade;

    public ErpFinNotesPayable issue(String notesId, IServiceContext context) {
        ErpFinNotesPayable note = facade.requireNote(notesId, context);
        if (facade.isAlreadyIssued(note)) {
            return note;
        }
        // issue 为 initial 态写入（§9.2 选项 c），守卫有意收窄为 Bean assertCanIssue（null/ISSUED 合法）。
        facade.validateTransitionForIssue(note, context);
        facade.requireAmountPositive(note, context);
        facade.reserveCreditIfNeeded(note, context);
        return facade.doIssue(notesId, note, context);
    }
}
