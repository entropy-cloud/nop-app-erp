package app.erp.sal.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.service.posting.IErpFinVoucherReversedListener;
import app.erp.fin.service.posting.VoucherReversedEvent;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalInvoice;
import app.erp.sal.dao.entity.ErpSalReceipt;
import app.erp.sal.dao.entity.ErpSalReturn;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 销售域凭证红冲监听者（业财闭环方向二：财务侧红冲→销售单据回退）。
 *
 * <p>财务员直接红冲已过账凭证时，finance 引擎派发 {@link VoucherReversedEvent}，本监听者据此
 * 回退销售域源单状态（设计 {@code posting.md §冲销机制方向二 §实现策略 裁决4} 回退目标态表）：
 * <ul>
 *   <li>{@link ErpFinBusinessType#AR_INVOICE} → {@link ErpSalInvoice}：posted=false + approveStatus APPROVED→REJECTED</li>
 *   <li>{@link ErpFinBusinessType#RECEIPT} → {@link ErpSalReceipt}：posted=false + approveStatus APPROVED→REJECTED</li>
 *   <li>{@link ErpFinBusinessType#SALES_RETURN} → {@link ErpSalReturn}：posted=false + approveStatus APPROVED→REJECTED</li>
 *   <li>{@link ErpFinBusinessType#SALES_OUTPUT} → {@link ErpSalDelivery}：仅 posted=false（库存物理冲销独立于凭证红冲）</li>
 * </ul>
 *
 * <p>监听者失败经 {@code ErpFinReversalListenerRegistry.dispatch} 的 try/catch 隔离，不阻断其他域监听者、
 * 不回滚已过账红字凭证；失败落入 finance 5.1 异常工作台。
 */
public class SalReversalListener implements IErpFinVoucherReversedListener {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        switch (businessType) {
            case "AR_INVOICE":
                rollbackInvoice(event);
                break;
            case "RECEIPT":
                rollbackReceipt(event);
                break;
            case "SALES_RETURN":
                rollbackReturn(event);
                break;
            case "SALES_OUTPUT":
                rollbackDelivery(event);
                break;
            default:
                break;
        }
    }

    protected void rollbackInvoice(VoucherReversedEvent event) {
        ErpSalInvoice invoice = findByCode(ErpSalInvoice.class, event.getBillHeadCode());
        if (invoice == null || !Boolean.TRUE.equals(invoice.getPosted())) {
            return;
        }
        invoice.setPosted(false);
        invoice.setPostedAt(null);
        invoice.setPostedBy(null);
        if (Objects.equals(invoice.getApproveStatus(), ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            invoice.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        }
        daoProvider.daoFor(ErpSalInvoice.class).updateEntity(invoice);
    }

    protected void rollbackReceipt(VoucherReversedEvent event) {
        ErpSalReceipt receipt = findByCode(ErpSalReceipt.class, event.getBillHeadCode());
        if (receipt == null || !Boolean.TRUE.equals(receipt.getPosted())) {
            return;
        }
        receipt.setPosted(false);
        receipt.setPostedAt(null);
        receipt.setPostedBy(null);
        if (Objects.equals(receipt.getApproveStatus(), ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            receipt.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        }
        daoProvider.daoFor(ErpSalReceipt.class).updateEntity(receipt);
    }

    protected void rollbackReturn(VoucherReversedEvent event) {
        ErpSalReturn returnOrder = findByCode(ErpSalReturn.class, event.getBillHeadCode());
        if (returnOrder == null || !Boolean.TRUE.equals(returnOrder.getPosted())) {
            return;
        }
        returnOrder.setPosted(false);
        returnOrder.setPostedAt(null);
        returnOrder.setPostedBy(null);
        if (Objects.equals(returnOrder.getApproveStatus(), ErpSalConstants.APPROVE_STATUS_APPROVED)) {
            returnOrder.setApproveStatus(ErpSalConstants.APPROVE_STATUS_REJECTED);
        }
        daoProvider.daoFor(ErpSalReturn.class).updateEntity(returnOrder);
    }

    protected void rollbackDelivery(VoucherReversedEvent event) {
        ErpSalDelivery delivery = findByCode(ErpSalDelivery.class, event.getBillHeadCode());
        if (delivery == null || !Boolean.TRUE.equals(delivery.getPosted())) {
            return;
        }
        // 库存物理冲销独立于凭证红冲（由业务侧 reverseApprove 链触发）；
        // 财务侧红冲仅回退 posted 标志。
        delivery.setPosted(false);
        delivery.setPostedAt(null);
        delivery.setPostedBy(null);
        daoProvider.daoFor(ErpSalDelivery.class).updateEntity(delivery);
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
