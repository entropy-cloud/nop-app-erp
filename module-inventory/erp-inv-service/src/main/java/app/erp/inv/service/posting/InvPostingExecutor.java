package app.erp.inv.service.posting;

import app.erp.fin.service.posting.ErpFinPostingService;
import app.erp.fin.service.posting.PostingEvent;
import io.nop.api.core.annotations.txn.Transactional;
import io.nop.api.core.annotations.txn.TransactionPropagation;

import jakarta.inject.Inject;

/**
 * 存货过账执行器：以独立新事务（{@link TransactionPropagation#REQUIRES_NEW}）调用财务过账引擎，
 * 使过账失败不污染移动单主事务（cross-domain「失败不影响移动单终态」）。
 *
 * <p>独立事务边界保证：过账异常在本事务内回滚（无凭证落库），主事务（移动单 DONE+流水+余额）不受影响。
 * 调用方 {@link InvPostingDispatcher} 以 try/catch 包裹本方法返回值/异常。
 */
public class InvPostingExecutor {

    @Inject
    ErpFinPostingService postingService;

    @Transactional(propagation = TransactionPropagation.REQUIRES_NEW)
    public Long postEvent(PostingEvent event) {
        return postingService.post(event);
    }
}
