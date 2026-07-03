package app.erp.pur.service;

import app.erp.md.biz.IErpMdSupplierApprovalBiz;
import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 评分卡 standing → AVL/RFQ 联动桥（{@code docs/design/purchase/supplier-evaluation.md §业务规则4}）。
 *
 * <p>finalize 后 standing=RED → 跨域调 {@link IErpMdSupplierApprovalBiz#suspendByPartner}
 * 使 master-data AVL 暂停立即生效（purchase→master-data I*Biz，单事务跟随 finalizeScorecard 的 @BizMutation）。
 *
 * <p>独立为 Bean 而非内联：下游客户可派生覆盖联动策略（如改为通知队列异步暂停、附加风控记录），
 * 对齐产品化可定制性（{@code nop-backend-dev} P1：多步编排 protected step，下游可逐个覆盖）。
 */
public class ScorecardStandingLinker {

    @Inject
    IErpMdSupplierApprovalBiz supplierApprovalBiz;

    public void onScorecardRed(ErpPurSupplierScorecard scorecard, IServiceContext context) {
        if (scorecard == null || scorecard.getPartnerId() == null) {
            return;
        }
        supplierApprovalBiz.suspendByPartner(scorecard.getPartnerId(), context);
    }
}
