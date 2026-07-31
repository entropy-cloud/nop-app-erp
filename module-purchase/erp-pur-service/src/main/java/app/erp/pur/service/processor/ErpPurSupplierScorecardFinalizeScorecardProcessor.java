package app.erp.pur.service.processor;

import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.pur.service.ScorecardStandingLinker;
import app.erp.pur.service.entity.ScorecardCalculator;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

/**
 * ErpPurSupplierScorecard finalizeScorecard per-mutation Processor（R6.5 类别 B，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含评分卡定稿编排（加载 → FINALIZED 守卫 → 计算 → 状态推进 → 保存 → standing=RED 跨域 AVL 联动）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPurSupplierScorecardFinalizeScorecardProcessor {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ScorecardCalculator scorecardCalculator;

    @Inject
    ScorecardStandingLinker standingLinker;

    public ErpPurSupplierScorecard finalizeScorecard(Long scorecardId, IServiceContext context) {
        ErpPurSupplierScorecard scorecard = requireScorecard(scorecardId, context);
        validateNotFinalized(scorecard, context);
        calculate(scorecard, context);
        doFinalize(scorecard, context);
        saveScorecard(scorecard, context);
        triggerRedStandingLink(scorecard, context);
        return scorecard;
    }

    protected ErpPurSupplierScorecard requireScorecard(Long scorecardId, IServiceContext context) {
        ErpPurSupplierScorecard scorecard = scorecardDao().getEntityById(scorecardId);
        if (scorecard == null) {
            throw new NopException(ErpPurErrors.ERR_SCORECARD_NOT_FOUND)
                    .param(ErpPurErrors.ARG_SCORECARD_ID, scorecardId);
        }
        return scorecard;
    }

    protected void validateNotFinalized(ErpPurSupplierScorecard scorecard, IServiceContext context) {
        if (scorecard.getStatus() != null
                && Objects.equals(scorecard.getStatus(), ErpPurConstants.SCORECARD_STATUS_FINALIZED)) {
            throw new NopException(ErpPurErrors.ERR_SCORECARD_ALREADY_FINALIZED)
                    .param(ErpPurErrors.ARG_SCORECARD_ID, scorecard.getId());
        }
    }

    protected void calculate(ErpPurSupplierScorecard scorecard, IServiceContext context) {
        scorecardCalculator.calculate(scorecard);
    }

    protected void doFinalize(ErpPurSupplierScorecard scorecard, IServiceContext context) {
        scorecard.setStatus(ErpPurConstants.SCORECARD_STATUS_FINALIZED);
    }

    protected void saveScorecard(ErpPurSupplierScorecard scorecard, IServiceContext context) {
        scorecardDao().updateEntity(scorecard);
    }

    protected void triggerRedStandingLink(ErpPurSupplierScorecard scorecard, IServiceContext context) {
        if (scorecard.getStanding() != null
                && Objects.equals(scorecard.getStanding(), ErpPurConstants.STANDING_RED)) {
            standingLinker.onScorecardRed(scorecard, context);
        }
    }

    protected IEntityDao<ErpPurSupplierScorecard> scorecardDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecard.class);
    }
}
