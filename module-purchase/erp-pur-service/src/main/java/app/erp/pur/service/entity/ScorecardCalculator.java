package app.erp.pur.service.entity;

import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.util.SourceLocation;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.lang.eval.EvalExprProvider;
import io.nop.core.lang.eval.IEvalAction;
import io.nop.core.lang.eval.IEvalExprParser;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 供应商评分卡周期评分计算器（{@code docs/design/purchase/supplier-evaluation.md §业务规则2}）。
 *
 * <p>评分模型 = 维度(criteria) × 公式(formula) × 权重(weight)，公式引用变量(variable)取值：
 * <ol>
 *   <li>按 criteria 取 variable（variableName/value）→ 组装 inputs map</li>
 *   <li>criteria.formula 经平台 XLang 表达式引擎（{@link EvalExprProvider#getDefaultExprParser}）计算 score</li>
 *   <li>weightedScore = score × weight / 100；totalScore = Σ weightedScore</li>
 *   <li>按 warn/hold 阈值落 standing（GREEN/YELLOW/RED）</li>
 * </ol>
 *
 * <p>公式引擎选型 Decision（Task Route）：选用平台 XLang 表达式（{@code EvalExprProvider}）而非 nop-rule。
 * 理由：nop-rule 文档明确「纯算术/公式计算直接用 XLang 表达式即可，不必引入规则引擎」，
 * 且 purchase-service 已传递依赖 nop-core/nop-xlang，无需新增依赖。variable.path 取值仍由 Java 装配
 * （测试或装配器将值写入 variable.value，本计算器读取 value 喂入公式）。新增维度=配置 criteria+variable，不改代码。
 */
public class ScorecardCalculator {

    @Inject
    IDaoProvider daoProvider;

    public void calculate(ErpPurSupplierScorecard scorecard) {
        List<ErpPurSupplierScorecardCriteria> criterias = loadCriterias(scorecard.getId());
        if (criterias.isEmpty()) {
            throw new NopException(ErpPurErrors.ERR_SCORECARD_NO_CRITERIA)
                    .param(ErpPurErrors.ARG_SCORECARD_ID, scorecard.getId());
        }
        validateWeightSum(scorecard, criterias);

        BigDecimal totalScore = BigDecimal.ZERO;
        IEntityDao<ErpPurSupplierScorecardCriteria> criteriaDao = criteriaDao();
        for (ErpPurSupplierScorecardCriteria criteria : criterias) {
            Map<String, Object> inputs = buildInputs(criteria.getId());
            BigDecimal score = evalFormula(criteria.getFormula(), inputs, criteria.getCriteriaName());
            BigDecimal weight = nvl(criteria.getWeight());
            BigDecimal weightedScore = score.multiply(weight)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            criteria.setScore(score);
            criteria.setWeightedScore(weightedScore);
            criteriaDao.updateEntity(criteria);
            totalScore = totalScore.add(weightedScore);
        }
        scorecard.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        scorecard.setStanding(determineStanding(totalScore, scorecard));
    }

    protected void validateWeightSum(ErpPurSupplierScorecard scorecard,
                                     List<ErpPurSupplierScorecardCriteria> criterias) {
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpPurSupplierScorecardCriteria c : criterias) {
            sum = sum.add(nvl(c.getWeight()));
        }
        if (sum.compareTo(BigDecimal.valueOf(100)) != 0) {
            throw new NopException(ErpPurErrors.ERR_SCORECARD_WEIGHT_NOT_100)
                    .param(ErpPurErrors.ARG_SCORECARD_ID, scorecard.getId())
                    .param(ErpPurErrors.ARG_TOTAL_WEIGHT, sum);
        }
    }

    protected Map<String, Object> buildInputs(Long criteriaId) {
        Map<String, Object> inputs = new HashMap<>();
        for (ErpPurSupplierScorecardVariable v : loadVariables(criteriaId)) {
            inputs.put(v.getVariableName(), nvl(v.getValue()));
        }
        return inputs;
    }

    protected BigDecimal evalFormula(String formula, Map<String, Object> inputs, String criteriaName) {
        try {
            // 公式中变量名（如 pass_rate）由运行时 inputs 提供，编译期不预注册——放开未注册变量检查。
            // 公式纯算术表达式，用 SimpleExpr 编译（对齐 nop-rule 文档：纯算术直接用 XLang 表达式，无需规则引擎）。
            io.nop.xlang.api.ExprEvalAction action = io.nop.xlang.api.XLang.newCompileTool()
                    .allowUnregisteredScopeVar(true)
                    .compileSimpleExpr(SourceLocation.fromLine("scorecard-formula:" + criteriaName, 1), formula);
            Object result = action.invoke(EvalExprProvider.newEvalScope(inputs));
            return toBigDecimal(result);
        } catch (Exception e) {
            throw new NopException(ErpPurErrors.ERR_SCORECARD_FORMULA_EVAL_FAIL, e)
                    .param(ErpPurErrors.ARG_CRITERIA_NAME, criteriaName)
                    .param(ErpPurErrors.ARG_FORMULA, formula);
        }
    }

    /**
     * 按 warn/hold 阈值落 standing（{@code supplier-evaluation.md §业务规则3}）：
     * <ul>
     *   <li>totalScore ≥ warnThreshold → GREEN（正常询价）</li>
     *   <li>holdThreshold ≤ totalScore &lt; warnThreshold → YELLOW（RFQ warn）</li>
     *   <li>totalScore &lt; holdThreshold → RED（RFQ hold/prevent，触发 AVL SUSPENDED）</li>
     * </ul>
     * 阈值缺省回退默认（warn=80, hold=60），保证无配置时仍可落档。
     */
    protected int determineStanding(BigDecimal totalScore, ErpPurSupplierScorecard scorecard) {
        BigDecimal warn = nvl(scorecard.getWarnThreshold(), new BigDecimal("80"));
        BigDecimal hold = nvl(scorecard.getHoldThreshold(), new BigDecimal("60"));
        if (totalScore.compareTo(warn) >= 0) {
            return ErpPurConstants.STANDING_GREEN;
        }
        if (totalScore.compareTo(hold) >= 0) {
            return ErpPurConstants.STANDING_YELLOW;
        }
        return ErpPurConstants.STANDING_RED;
    }

    protected BigDecimal toBigDecimal(Object result) {
        if (result == null) {
            return BigDecimal.ZERO;
        }
        if (result instanceof BigDecimal) {
            return (BigDecimal) result;
        }
        if (result instanceof Number) {
            return new BigDecimal(result.toString());
        }
        return new BigDecimal(result.toString());
    }

    protected BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    protected BigDecimal nvl(BigDecimal v, BigDecimal defaultValue) {
        return v == null ? defaultValue : v;
    }

    protected List<ErpPurSupplierScorecardCriteria> loadCriterias(Long scorecardId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("scorecardId", scorecardId));
        return new ArrayList<>(criteriaDao().findAllByQuery(q));
    }

    protected List<ErpPurSupplierScorecardVariable> loadVariables(Long criteriaId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("criteriaId", criteriaId));
        return new ArrayList<>(variableDao().findAllByQuery(q));
    }

    protected IEntityDao<ErpPurSupplierScorecardCriteria> criteriaDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecardCriteria.class);
    }

    protected IEntityDao<ErpPurSupplierScorecardVariable> variableDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecardVariable.class);
    }
}
