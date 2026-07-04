package app.erp.inv.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.service.posting.IErpFinVoucherReversedListener;
import app.erp.fin.service.posting.VoucherReversedEvent;
import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockTake;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 库存域凭证红冲监听者（业财闭环方向二：财务侧红冲→库存单据回退）。
 *
 * <p>财务员直接红冲已过账凭证时，finance 引擎派发 {@link VoucherReversedEvent}，本监听者据此
 * 回退库存域源单状态（设计 {@code posting.md §冲销机制方向二 §实现策略 裁决4} 回退目标态表）：
 * <ul>
 *   <li>{@link ErpFinBusinessType#OWNERSHIP_TRANSFER} → {@link ErpInvOwnershipTransfer}：仅 posted=false</li>
 *   <li>{@link ErpFinBusinessType#INTER_TRANSFER} → {@link ErpInvTransferOrder}：仅 posted=false</li>
 * </ul>
 *
 * <p>库存单据无 approveStatus 审核轴（与 purchase/sales 不同），仅 {@code posted} 翻转。
 * 物理库存冲销独立于凭证红冲（由业务侧状态机触发）；财务侧红冲仅回退 posted 标志。
 *
 * <p>监听者失败经 {@code ErpFinReversalListenerRegistry.dispatch} 的 try/catch 隔离，不阻断其他域监听者、
 * 不回滚已过账红字凭证；失败落入 finance 5.1 异常工作台。
 */
public class InvReversalListener implements IErpFinVoucherReversedListener {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        switch (businessType) {
            case "OWNERSHIP_TRANSFER":
                rollbackOwnershipTransfer(event);
                break;
            case "INTER_TRANSFER":
                rollbackTransferOrder(event);
                break;
            default:
                break;
        }
    }

    protected void rollbackOwnershipTransfer(VoucherReversedEvent event) {
        ErpInvOwnershipTransfer transfer = findByCode(ErpInvOwnershipTransfer.class, event.getBillHeadCode());
        if (transfer == null || !Boolean.TRUE.equals(transfer.getPosted())) {
            return;
        }
        transfer.setPosted(false);
        transfer.setPostedAt(null);
        transfer.setPostedBy(null);
        daoProvider.daoFor(ErpInvOwnershipTransfer.class).updateEntity(transfer);
    }

    protected void rollbackTransferOrder(VoucherReversedEvent event) {
        ErpInvTransferOrder order = findByCode(ErpInvTransferOrder.class, event.getBillHeadCode());
        if (order == null || !Boolean.TRUE.equals(order.getPosted())) {
            return;
        }
        order.setPosted(false);
        order.setPostedAt(null);
        order.setPostedBy(null);
        daoProvider.daoFor(ErpInvTransferOrder.class).updateEntity(order);
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
