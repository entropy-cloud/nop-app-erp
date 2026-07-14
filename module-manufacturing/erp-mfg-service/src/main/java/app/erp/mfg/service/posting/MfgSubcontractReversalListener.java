package app.erp.mfg.service.posting;

import app.erp.fin.service.posting.IErpFinVoucherReversedListener;
import app.erp.fin.service.posting.VoucherReversedEvent;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 委外加工凭证红冲监听者（业财闭环方向二：财务侧红冲→业务单据回退，M5.2 覆盖 manufacturing 委外段）。
 *
 * <p>财务员直接红冲已过账凭证时，finance 引擎派发 {@link VoucherReversedEvent}，本监听者据此回退委外单状态
 * （设计 {@code posting.md §冲销机制方向二 §裁决4 回退目标态表}）。镜像 {@code PurReversalListener} 范式结构
 * （switch businessType → findByCode → posted==true 前置 → 回退），但<b>回退字段不同</b>：
 * PurReversalListener 回退 {@code approveStatus APPROVED→REJECTED}（采购单为审批轴驱动），
 * 本监听者回退 {@code docStatus→CANCELLED}（委外单为 docStatus 驱动，COMPLETED 时 approveStatus 已 APPROVED
 * 且 CANCELLED 为终态，无 approveStatus 回退）。
 *
 * <p>三段业务类型共用同一委外单，billHeadCode 为 {@code orderCode + "-SI"/"-SR"/"-SF"}，
 * 经去后缀反查委外单 code。幂等安全：{@code posted==false} 时 no-op，与域级 {@code reverseCompletion} 无双重处理。
 *
 * <p>监听者失败经 {@code ErpFinReversalListenerRegistry.dispatch} 的 try/catch 隔离，不阻断其他域监听者、
 * 不回滚已过账红字凭证。
 */
public class MfgSubcontractReversalListener implements IErpFinVoucherReversedListener {

    static final String SUFFIX_ISSUE = "-SI";
    static final String SUFFIX_RECEIPT = "-SR";
    static final String SUFFIX_FEE = "-SF";

    @Inject
    IDaoProvider daoProvider;

    @Override
    public void onVoucherReversed(VoucherReversedEvent event, IServiceContext context) {
        String businessType = event.getBusinessType();
        if (businessType == null) {
            return;
        }
        switch (businessType) {
            case "SUBCONTRACT_ISSUE":
            case "SUBCONTRACT_RECEIPT":
            case "SUBCONTRACT_FEE":
                rollbackSubcontractOrder(event);
                break;
            default:
                // 非委外域业务类型——忽略（其他域监听者处理）
                break;
        }
    }

    protected void rollbackSubcontractOrder(VoucherReversedEvent event) {
        String orderCode = stripBillHeadSuffix(event.getBillHeadCode());
        if (orderCode == null) {
            return;
        }
        ErpMfgSubcontractOrder order = findByCode(ErpMfgSubcontractOrder.class, orderCode);
        if (order == null || !Boolean.TRUE.equals(order.getPosted())) {
            return;
        }
        order.setPosted(false);
        order.setPostedAt(null);
        order.setPostedBy(null);
        order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_CANCELLED);
        daoProvider.daoFor(ErpMfgSubcontractOrder.class).updateEntity(order);
    }

    /**
     * 去除 billHeadCode 的委外段后缀（-SI/-SR/-SF），还原委外单 code。
     */
    protected String stripBillHeadSuffix(String billHeadCode) {
        if (billHeadCode == null) {
            return null;
        }
        if (billHeadCode.endsWith(SUFFIX_FEE)) {
            return billHeadCode.substring(0, billHeadCode.length() - SUFFIX_FEE.length());
        }
        if (billHeadCode.endsWith(SUFFIX_RECEIPT)) {
            return billHeadCode.substring(0, billHeadCode.length() - SUFFIX_RECEIPT.length());
        }
        if (billHeadCode.endsWith(SUFFIX_ISSUE)) {
            return billHeadCode.substring(0, billHeadCode.length() - SUFFIX_ISSUE.length());
        }
        return billHeadCode;
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
        if (list.isEmpty()) {
            return null;
        }
        return list.get(0);
    }
}
