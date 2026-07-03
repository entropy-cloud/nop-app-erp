package app.erp.fin.service.reconciliation;

import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.notIn;

/**
 * 往来余额缓存更新器。核销过账/红冲后重算 {@link ErpMdPartner#getReceivableBalance()} /
 * {@link ErpMdPartner#getPayableBalance()}，由辅助账（{@link ErpFinArApItem}）未核销余额驱动
 * （{@code ar-ap-reconciliation.md §余额计算}：余额 = Σ 对应方向辅助账 openAmountFunctional）。
 *
 * <p>机制 B（plan 裁定）：直接经 {@code daoProvider.daoFor(ErpMdPartner.class)} 更新缓存字段，
 * 因 master-data-service 为 test 作用域、且此处仅是简单字段重算，无需 IErpMdPartnerBiz 业务方法。
 */
public class PartnerBalanceUpdater {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 重算指定往来单位的应收/应付余额缓存（按辅助账未核销本位币金额汇总，排除 SETTLED/CANCELLED）。
     */
    public void refresh(Long partnerId) {
        if (partnerId == null) {
            return;
        }
        IEntityDao<ErpMdPartner> partnerDao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = partnerDao.getEntityById(partnerId);
        if (partner == null) {
            return;
        }
        partner.setReceivableBalance(sumOpen(partnerId, ErpFinConstants.DIRECTION_RECEIVABLE));
        partner.setPayableBalance(sumOpen(partnerId, ErpFinConstants.DIRECTION_PAYABLE));
    }

    protected BigDecimal sumOpen(Long partnerId, String direction) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        q.addFilter(eq("direction", direction));
        q.addFilter(notIn("status", java.util.Arrays.asList(
                ErpFinConstants.AR_AP_STATUS_SETTLED, ErpFinConstants.AR_AP_STATUS_CANCELLED)));
        List<ErpFinArApItem> items = dao.findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpFinArApItem item : items) {
            BigDecimal open = item.getOpenAmountFunctional();
            if (open != null) {
                sum = sum.add(open);
            }
        }
        return sum;
    }
}
