package app.erp.fin.service.posting;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinNotesPayable;
import app.erp.fin.dao.entity.ErpFinNotesReceivable;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.sql.Timestamp;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * F2.1（P1-CK-fin-001）一期：finance 本域 posted 回写监听者。覆盖 EXPENSE_CLAIM/EMPLOYEE_ADVANCE/
 * NOTES_RECEIVABLE 系/NOTES_PAYABLE 系（billHeadCode=单据 code 直查，镜像 {@code PurReversalListener}
 * 的 switch/findByCode/markPosted 范式）。
 *
 * <p>已 true 跳过（防 version 无谓递增）；定位 miss no-op（分期部署优雅降级）。
 * EMPLOYEE_ADVANCE_SETTLE 合成码（EA-CASH-REPAY-*）无独立源单 → findByCode miss no-op。
 *
 * <p>监听者失败经 {@link ErpFinPostedListenerRegistry#dispatch} 的 try/catch 隔离，不阻断其他域监听者、
 * 不回滚 RETRIED；失败经 {@code ErpFinPostingExceptionRecorder} 落异常工作台（eventData 透传自愈）。
 */
public class FinPostedListener implements IErpFinVoucherPostedListener {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public void onVoucherPosted(VoucherPostedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        Timestamp now = CoreMetrics.currentTimestamp();
        if ("EXPENSE_CLAIM".equals(businessType)) {
            markPosted(findByCode(ErpFinExpenseClaim.class, event.getBillHeadCode()), now);
        } else if ("EMPLOYEE_ADVANCE".equals(businessType)) {
            markPosted(findByCode(ErpFinEmployeeAdvance.class, event.getBillHeadCode()), now);
        } else if (businessType.startsWith("NOTES_RECEIVABLE")) {
            markPosted(findByCode(ErpFinNotesReceivable.class, event.getBillHeadCode()), now);
        } else if (businessType.startsWith("NOTES_PAYABLE")) {
            markPosted(findByCode(ErpFinNotesPayable.class, event.getBillHeadCode()), now);
        }
    }

    private void markPosted(ErpFinExpenseClaim claim, Timestamp now) {
        if (claim == null || Boolean.TRUE.equals(claim.getPosted())) {
            return;
        }
        claim.setPosted(true);
        claim.setPostedAt(now);
        claim.setPostedBy(currentUserId());
        daoProvider.daoFor(ErpFinExpenseClaim.class).updateEntity(claim);
    }

    private void markPosted(ErpFinEmployeeAdvance advance, Timestamp now) {
        if (advance == null || Boolean.TRUE.equals(advance.getPosted())) {
            return;
        }
        advance.setPosted(true);
        advance.setPostedAt(now);
        advance.setPostedBy(currentUserId());
        daoProvider.daoFor(ErpFinEmployeeAdvance.class).updateEntity(advance);
    }

    private void markPosted(ErpFinNotesReceivable note, Timestamp now) {
        if (note == null || Boolean.TRUE.equals(note.getPosted())) {
            return;
        }
        note.setPosted(true);
        note.setPostedAt(now);
        note.setPostedBy(currentUserId());
        daoProvider.daoFor(ErpFinNotesReceivable.class).updateEntity(note);
    }

    private void markPosted(ErpFinNotesPayable note, Timestamp now) {
        if (note == null || Boolean.TRUE.equals(note.getPosted())) {
            return;
        }
        note.setPosted(true);
        note.setPostedAt(now);
        note.setPostedBy(currentUserId());
        daoProvider.daoFor(ErpFinNotesPayable.class).updateEntity(note);
    }

    private static String currentUserId() {
        IUserContext ctx = IUserContext.get();
        return ctx != null ? ctx.getUserId() : null;
    }

    protected <T extends IDaoEntity> T findByCode(Class<T> entityClass, String code) {
        if (code == null) {
            return null;
        }
        IEntityDao<T> dao = daoProvider.daoFor(entityClass);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        List<T> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
