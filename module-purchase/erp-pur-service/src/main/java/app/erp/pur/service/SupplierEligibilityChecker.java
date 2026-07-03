package app.erp.pur.service;

import app.erp.md.biz.IErpMdSupplierApprovalBiz;
import app.erp.md.dao.entity.ErpMdSupplierApproval;
import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 供应商询价/报价准入资格校验器（{@code docs/design/purchase/supplier-evaluation.md §业务规则3/5}）。
 *
 * <p>当供应商作为询价/报价收件人时（RFQ 头无 supplierId，供应商经报价单 ErpPurQuotation 参与询价，
 * 故校验落在报价单保存前置钩子）：
 * <ul>
 *   <li>AVL 准入 status ∈ {SUSPENDED, REJECTED} 或无有效资格 → {@link Decision#PREVENT}</li>
 *   <li>最新定稿评分卡 standing=RED → 配置 {@code erp-pur.scorecard-prevent-on-red}=true 时 PREVENT，否则 ALLOW（走 hold 审批）</li>
 *   <li>standing=YELLOW → {@link Decision#WARN}（提示评分偏低，不阻止）</li>
 *   <li>其余（GREEN / 无评分记录）→ {@link Decision#ALLOW}</li>
 * </ul>
 *
 * <p>独立为 Bean：下游可派生覆盖联动档位（如改为按物料类别细分、附加风控白名单），对齐产品化可定制性。
 */
public class SupplierEligibilityChecker {

    public enum Decision { ALLOW, WARN, PREVENT }

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpMdSupplierApprovalBiz supplierApprovalBiz;

    public Decision check(Long partnerId, io.nop.core.context.IServiceContext context) {
        if (partnerId == null) {
            return Decision.ALLOW;
        }
        ErpMdSupplierApproval approval = supplierApprovalBiz.findEffectiveByPartner(partnerId, context);
        if (approval == null) {
            return Decision.PREVENT;
        }
        String approvalStatus = approval.getStatus();
        if (approvalStatus != null
                && (Objects.equals(approvalStatus, ErpPurConstants.APPROVAL_STATUS_SUSPENDED)
                || Objects.equals(approvalStatus, ErpPurConstants.APPROVAL_STATUS_REJECTED))) {
            return Decision.PREVENT;
        }

        ErpPurSupplierScorecard latest = findLatestFinalizedScorecard(partnerId);
        if (latest == null || latest.getStanding() == null) {
            return Decision.ALLOW;
        }
        String standing = latest.getStanding();
        if (Objects.equals(standing, ErpPurConstants.STANDING_RED)) {
            return isPreventOnRed() ? Decision.PREVENT : Decision.ALLOW;
        }
        if (Objects.equals(standing, ErpPurConstants.STANDING_YELLOW)) {
            return Decision.WARN;
        }
        return Decision.ALLOW;
    }

    public boolean isPreventOnRed() {
        return AppConfig.var(ErpPurConstants.CONFIG_SCORECARD_PREVENT_ON_RED, true);
    }

    protected ErpPurSupplierScorecard findLatestFinalizedScorecard(Long partnerId) {
        // standing 为字典类型，xmeta 仅允许 eq/in 过滤；status=FINALIZED 用 eq。取最新周期。
        IEntityDao<ErpPurSupplierScorecard> dao = daoProvider.daoFor(ErpPurSupplierScorecard.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("partnerId", partnerId));
        q.addFilter(eq("status", ErpPurConstants.SCORECARD_STATUS_FINALIZED));
        q.addOrderField("periodTo", true);
        q.setLimit(1);
        List<ErpPurSupplierScorecard> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}
