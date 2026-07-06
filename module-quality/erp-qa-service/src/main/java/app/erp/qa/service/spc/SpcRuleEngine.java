package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * SPC 判异规则引擎（{@code docs/design/quality/spc.md §关键流程 2/3}，plan 2026-07-07-0305-2 Phase 3）。
 *
 * <p>实现 Western Electric 规则 1~4（按 chart.ruleSet 逗号启用子集）：
 * <ol>
 *   <li>规则 1：单点超出 3σ（ucl/lcl 之外）。</li>
 *   <li>规则 2：连续 9 点落在中心线同一侧。</li>
 *   <li>规则 3：连续 6 点单调递增或递减。</li>
 *   <li>规则 4：连续 14 点交替上下（锯齿）。</li>
 * </ol>
 *
 * <p>规则算法为纯函数（{@link #evaluateRules(List, BigDecimal, BigDecimal, BigDecimal, Set)}），
 * 便于独立单测。失控样本经 {@code txn().afterCommit} 创建 NCR+CAPA（模式 B post-commit），
 * 由 {@code erp-qa.spc-auto-ncr-enabled} 门控（默认 true）。
 */
public class SpcRuleEngine {

    private static final Logger LOG = LoggerFactory.getLogger(SpcRuleEngine.class);

    static final String RULE_1 = "1";
    static final String RULE_2 = "2";
    static final String RULE_3 = "3";
    static final String RULE_4 = "4";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    SpcOutOfControlHandler outOfControlHandler;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    public void setOutOfControlHandler(SpcOutOfControlHandler outOfControlHandler) {
        this.outOfControlHandler = outOfControlHandler;
    }

    /**
     * 对指定 chart 的所有样本评估判异规则，回写每样本 violatedRules/isOutOfControl。
     * 失控样本经 post-commit 创建 NCR+CAPA（config-gated）。返回失控样本数。
     */
    public int evaluate(Long chartId, IServiceContext context) {
        IEntityDao<ErpQaSpcChart> chartDao = daoProvider.daoFor(ErpQaSpcChart.class);
        ErpQaSpcChart chart = chartDao.getEntityById(chartId);
        if (chart == null) {
            throw new NopException(ErpQaErrors.ERR_QA_SPC_CHART_NOT_FOUND)
                    .param(ErpQaErrors.ARG_CHART_ID, chartId);
        }
        if (chart.getUcl() == null || chart.getLcl() == null || chart.getCl() == null) {
            // 控制限未计算：无规则可评估
            return 0;
        }
        Set<String> enabledRules = parseRuleSet(chart.getRuleSet());
        if (enabledRules.isEmpty()) {
            return 0;
        }

        List<ErpQaSpcSample> samples = findSamplesOrdered(chartId);
        if (samples.isEmpty()) {
            return 0;
        }
        List<BigDecimal> means = new ArrayList<>(samples.size());
        for (ErpQaSpcSample s : samples) {
            means.add(s.getMean());
        }
        Map<Integer, Set<String>> violatedByIndex = evaluateRules(means, chart.getCl(), chart.getUcl(), chart.getLcl(), enabledRules);

        IEntityDao<ErpQaSpcSample> sampleDao = daoProvider.daoFor(ErpQaSpcSample.class);
        int outOfControlCount = 0;
        for (int i = 0; i < samples.size(); i++) {
            ErpQaSpcSample s = samples.get(i);
            Set<String> violated = violatedByIndex.get(i);
            String violatedRules = violated == null || violated.isEmpty() ? null : String.join(",", sortRules(violated));
            boolean isOutOfControl = violated != null && !violated.isEmpty();
            // 仅当变化时更新（避免无意义 update）
            if (!Objects.equals(violatedRules, s.getViolatedRules())
                    || isOutOfControl != Boolean.TRUE.equals(s.getIsOutOfControl())) {
                s.setViolatedRules(violatedRules);
                s.setIsOutOfControl(isOutOfControl);
                sampleDao.updateEntity(s);
            }
            if (isOutOfControl) {
                outOfControlCount++;
                try {
                    outOfControlHandler.cascadeNcrAndCapa(chart, s, violated, context);
                } catch (Exception e) {
                    LOG.warn("spc-out-of-control-cascade-failed: chartId={} sampleId={}",
                            chart.getId(), s.getId(), e);
                }
            }
        }
        return outOfControlCount;
    }

    /**
     * 纯函数：对一组 mean 序列应用启用的 Western Electric 规则。返回每个下标违反的规则编号集合。
     *
     * @param means        子组均值序列（按 subgroupNo 排序）
     * @param cl           中心线
     * @param ucl          控制上限
     * @param lcl          控制下限
     * @param enabledRules 启用的规则编号集合（"1".."4"）
     */
    public Map<Integer, Set<String>> evaluateRules(List<BigDecimal> means, BigDecimal cl, BigDecimal ucl,
                                                    BigDecimal lcl, Set<String> enabledRules) {
        Map<Integer, Set<String>> result = new HashMap<>();
        if (means == null || means.isEmpty() || cl == null || ucl == null || lcl == null) {
            return result;
        }
        int n = means.size();
        BigDecimal oneSigmaUpper = cl.add(ucl.subtract(cl).divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP));
        BigDecimal oneSigmaLower = cl.subtract(cl.subtract(lcl).divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP));
        BigDecimal twoSigmaUpper = cl.add(ucl.subtract(cl).divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP).multiply(new BigDecimal("2")));
        BigDecimal twoSigmaLower = cl.subtract(cl.subtract(lcl).divide(new BigDecimal("3"), 10, RoundingMode.HALF_UP).multiply(new BigDecimal("2")));

        for (int i = 0; i < n; i++) {
            BigDecimal v = means.get(i);
            if (v == null) {
                continue;
            }
            // 规则 1：单点超出 3σ
            if (enabledRules.contains(RULE_1)) {
                if (v.compareTo(ucl) > 0 || v.compareTo(lcl) < 0) {
                    result.computeIfAbsent(i, k -> new LinkedHashSet<>()).add(RULE_1);
                }
            }
            // 规则 2：连续 9 点同侧
            if (enabledRules.contains(RULE_2) && i >= 8) {
                boolean allSameSide = true;
                Boolean above = null;
                for (int k = i - 8; k <= i; k++) {
                    BigDecimal m = means.get(k);
                    if (m == null) {
                        allSameSide = false;
                        break;
                    }
                    boolean isAbove = m.compareTo(cl) > 0;
                    if (above == null) {
                        above = isAbove;
                    } else if (above != isAbove) {
                        allSameSide = false;
                        break;
                    }
                }
                if (allSameSide && above != null) {
                    for (int k = i - 8; k <= i; k++) {
                        result.computeIfAbsent(k, kk -> new LinkedHashSet<>()).add(RULE_2);
                    }
                }
            }
            // 规则 3：连续 6 点单调递增或递减
            if (enabledRules.contains(RULE_3) && i >= 5) {
                boolean increasing = true;
                boolean decreasing = true;
                for (int k = i - 5; k < i; k++) {
                    BigDecimal a = means.get(k);
                    BigDecimal b = means.get(k + 1);
                    if (a == null || b == null) {
                        increasing = false;
                        decreasing = false;
                        break;
                    }
                    int cmp = b.compareTo(a);
                    if (cmp <= 0) {
                        increasing = false;
                    }
                    if (cmp >= 0) {
                        decreasing = false;
                    }
                }
                if (increasing || decreasing) {
                    for (int k = i - 5; k <= i; k++) {
                        result.computeIfAbsent(k, kk -> new LinkedHashSet<>()).add(RULE_3);
                    }
                }
            }
            // 规则 4：连续 14 点交替上下（锯齿）
            if (enabledRules.contains(RULE_4) && i >= 13) {
                boolean alternating = true;
                Boolean prevAbove = null;
                for (int k = i - 13; k <= i; k++) {
                    BigDecimal m = means.get(k);
                    if (m == null) {
                        alternating = false;
                        break;
                    }
                    boolean above = m.compareTo(cl) > 0;
                    if (prevAbove != null && prevAbove == above) {
                        alternating = false;
                        break;
                    }
                    prevAbove = above;
                }
                if (alternating) {
                    for (int k = i - 13; k <= i; k++) {
                        result.computeIfAbsent(k, kk -> new LinkedHashSet<>()).add(RULE_4);
                    }
                }
            }
        }
        return result;
    }

    private List<ErpQaSpcSample> findSamplesOrdered(Long chartId) {
        IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        q.addOrderField("subgroupNo", false);
        return dao.findAllByQuery(q);
    }

    static Set<String> parseRuleSet(String ruleSet) {
        Set<String> result = new LinkedHashSet<>();
        if (StringHelper.isEmpty(ruleSet)) {
            return result;
        }
        for (String p : Arrays.asList(ruleSet.split(","))) {
            String t = p.trim();
            if (!t.isEmpty()) {
                result.add(t);
            }
        }
        return result;
    }

    static List<String> sortRules(Set<String> rules) {
        List<String> list = new ArrayList<>(rules);
        Collections.sort(list);
        return list;
    }
}
