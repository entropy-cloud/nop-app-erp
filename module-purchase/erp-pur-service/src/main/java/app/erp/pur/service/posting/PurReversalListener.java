package app.erp.pur.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.service.posting.IErpFinVoucherReversedListener;
import app.erp.fin.service.posting.VoucherReversedEvent;
import app.erp.pur.dao.entity.ErpPurInvoice;
import app.erp.pur.dao.entity.ErpPurPayment;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.dao.entity.ErpPurReturn;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 采购域凭证红冲监听者（业财闭环方向二：财务侧红冲→采购单据回退）。
 *
 * <p>财务员直接红冲已过账凭证时，finance 引擎派发 {@link VoucherReversedEvent}，本监听者据此
 * 回退采购域源单状态（设计 {@code posting.md §冲销机制方向二 §实现策略 裁决4} 回退目标态表）：
 * <ul>
 *   <li>{@link ErpFinBusinessType#AP_INVOICE} → {@link ErpPurInvoice}：posted=false + approveStatus APPROVED→REJECTED</li>
 *   <li>{@link ErpFinBusinessType#PAYMENT} → {@link ErpPurPayment}：posted=false + approveStatus APPROVED→REJECTED</li>
 *   <li>{@link ErpFinBusinessType#PURCHASE_RETURN} → {@link ErpPurReturn}：posted=false + approveStatus APPROVED→REJECTED</li>
 *   <li>{@link ErpFinBusinessType#PURCHASE_INPUT} → {@link ErpPurReceive}：仅 posted=false（库存物理冲销独立于凭证红冲）</li>
 * </ul>
 *
 * <p>监听者失败经 {@code ErpFinReversalListenerRegistry.dispatch} 的 try/catch 隔离，不阻断其他域监听者、
 * 不回滚已过账红字凭证；失败落入 finance 5.1 异常工作台（{@code ErpFinPostingException}）。
 *
 * <p>跨实体访问：本监听者处于 purchase 域，回退自身域实体经 {@link IDaoProvider}（同模块实体直接持久化，
 * 参 {@code ErpPurInvoiceProcessor} 既有 reverseApprove 模式）；不反向调用 finance。
 */
public class PurReversalListener implements IErpFinVoucherReversedListener {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        switch (businessType) {
            case "AP_INVOICE":
                rollbackInvoice(event);
                break;
            case "PAYMENT":
                rollbackPayment(event);
                break;
            case "PURCHASE_RETURN":
                rollbackReturn(event);
                break;
            case "PURCHASE_INPUT":
                rollbackReceive(event);
                break;
            default:
                // 非采购域业务类型——忽略（其他域监听者处理）
                break;
        }
    }

    protected void rollbackInvoice(VoucherReversedEvent event) {
        ErpPurInvoice invoice = findByCode(ErpPurInvoice.class, event.getBillHeadCode());
        if (invoice == null || !Boolean.TRUE.equals(invoice.getPosted())) {
            return;
        }
        invoice.setPosted(false);
        invoice.setPostedAt(null);
        invoice.setPostedBy(null);
        if (Objects.equals(invoice.getApproveStatus(), ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        }
        daoProvider.daoFor(ErpPurInvoice.class).updateEntity(invoice);
    }

    protected void rollbackPayment(VoucherReversedEvent event) {
        ErpPurPayment payment = findByCode(ErpPurPayment.class, event.getBillHeadCode());
        if (payment == null || !Boolean.TRUE.equals(payment.getPosted())) {
            return;
        }
        payment.setPosted(false);
        payment.setPostedAt(null);
        payment.setPostedBy(null);
        if (Objects.equals(payment.getApproveStatus(), ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            payment.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        }
        daoProvider.daoFor(ErpPurPayment.class).updateEntity(payment);
    }

    protected void rollbackReturn(VoucherReversedEvent event) {
        ErpPurReturn returnOrder = findByCode(ErpPurReturn.class, event.getBillHeadCode());
        if (returnOrder == null || !Boolean.TRUE.equals(returnOrder.getPosted())) {
            return;
        }
        returnOrder.setPosted(false);
        returnOrder.setPostedAt(null);
        returnOrder.setPostedBy(null);
        if (Objects.equals(returnOrder.getApproveStatus(), ErpPurConstants.APPROVE_STATUS_APPROVED)) {
            returnOrder.setApproveStatus(ErpPurConstants.APPROVE_STATUS_REJECTED);
        }
        daoProvider.daoFor(ErpPurReturn.class).updateEntity(returnOrder);
    }

    protected void rollbackReceive(VoucherReversedEvent event) {
        ErpPurReceive receive = findByCode(ErpPurReceive.class, event.getBillHeadCode());
        if (receive == null || !Boolean.TRUE.equals(receive.getPosted())) {
            return;
        }
        // 库存物理冲销独立于凭证红冲（由业务侧 reverseApprove 链触发 stockMoveBiz.reverse）；
        // 财务侧红冲仅回退 posted 标志，保留 APPROVED 审计轨迹。
        receive.setPosted(false);
        receive.setPostedAt(null);
        receive.setPostedBy(null);
        daoProvider.daoFor(ErpPurReceive.class).updateEntity(receive);
    }

    protected <T extends IDaoEntity> T findByCode(Class<T> entityClass, String code) {
        if (code == null) {
            return null;
        }
        IEntityDao<T> dao = daoProvider.daoFor(entityClass);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        q.setLimit(1);
        java.util.List<T> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}
