
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurSupplierScorecardBiz;
import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.ScorecardStandingLinker;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import java.util.Objects;

/**
 * 供应商评分卡 BizModel。承载周期评分定稿引擎（{@code docs/design/purchase/supplier-evaluation.md §业务规则2/3/4}）。
 *
 * <p>{@link #finalizeScorecard} 委托 {@link ScorecardCalculator} 完成「criteria×formula×weight→totalScore→standing」，
 * 随后 status DRAFT→FINALIZED。standing=RED 时经 {@link ScorecardStandingLinker} 跨域调
 * {@code IErpMdSupplierApprovalBiz.suspendByPartner} 使 AVL 暂停立即生效（单事务，{@code §业务规则4}）。
 *
 * <p>跨域写（standing=RED→AVL SUSPENDED）拆 Phase 3 实现的 Linker 接入点；本类仅委托。
 */
@BizModel("ErpPurSupplierScorecard")
public class ErpPurSupplierScorecardBizModel extends CrudBizModel<ErpPurSupplierScorecard> implements IErpPurSupplierScorecardBiz {

    @Inject
    ScorecardCalculator scorecardCalculator;

    @Inject
    ScorecardStandingLinker standingLinker;

    public ErpPurSupplierScorecardBizModel() {
        setEntityName(ErpPurSupplierScorecard.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurSupplierScorecard finalizeScorecard(@Name("scorecardId") Long scorecardId, IServiceContext context) {
        ErpPurSupplierScorecard scorecard = requireScorecard(scorecardId);
        if (scorecard.getStatus() != null && Objects.equals(scorecard.getStatus(), ErpPurConstants.SCORECARD_STATUS_FINALIZED)) {
            throw new NopException(ErpPurErrors.ERR_SCORECARD_ALREADY_FINALIZED)
                    .param(ErpPurErrors.ARG_SCORECARD_ID, scorecardId);
        }

        scorecardCalculator.calculate(scorecard);
        scorecard.setStatus(ErpPurConstants.SCORECARD_STATUS_FINALIZED);
        dao().updateEntity(scorecard);

        // standing=RED → 跨域 AVL SUSPENDED 联动（Phase 3）。Linker 单事务跟随 @BizMutation。
        if (scorecard.getStanding() != null && Objects.equals(scorecard.getStanding(), ErpPurConstants.STANDING_RED)) {
            standingLinker.onScorecardRed(scorecard, context);
        }
        return scorecard;
    }

    protected ErpPurSupplierScorecard requireScorecard(Long scorecardId) {
        ErpPurSupplierScorecard scorecard = dao().getEntityById(scorecardId);
        if (scorecard == null) {
            throw new NopException(ErpPurErrors.ERR_SCORECARD_NOT_FOUND)
                    .param(ErpPurErrors.ARG_SCORECARD_ID, scorecardId);
        }
        return scorecard;
    }
}
