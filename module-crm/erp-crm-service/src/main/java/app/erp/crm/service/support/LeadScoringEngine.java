package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadScore;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfig;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine;
import app.erp.crm.dao.entity.ErpCrmLeadScoreLine;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.processor.ErpCrmLeadProcessor;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 线索评分引擎（config 驱动）。加载 {@code isActive=true} 的 {@link ErpCrmLeadScoreConfig}，
 * 按 {@link ErpCrmLeadScoreConfigLine#getScoringMethod()}（LOOKUP/FORMULA/BOOLEAN）逐准则计分，
 * 归一化 {@code totalScore}（0-100），写 {@link ErpCrmLeadScore}+{@link ErpCrmLeadScoreLine}（append-only）。
 *
 * <p>Lead 当前分数 = 按 leadId + calculatedAt DESC 取最新一条（不扩 ORM，Option B 派生查询）。
 *
 * <p>对齐 {@code docs/design/crm/lead-scoring.md}（评分计算流程 / 阈值→动作矩阵 / 业务规则 2/6/7/8）。
 *
 * <p>评分方法约定（首版）：
 * <ul>
 *   <li>LOOKUP：{@code formula} 指定 Lead 属性名，{@code lookupTable} 为 {@code [{"value":"x","score":20}]}，
 *       匹配取 entry.score，无匹配取 0。</li>
 *   <li>BOOLEAN：{@code formula} 指定 Lead 属性名，{@code lookupTable} 为 {@code [{"value":"x"}]}，
 *       匹配取 maxScore，无匹配取 0。</li>
 *   <li>FORMULA：首版支持 {@code ENGAGEMENT_SCORE}（已完成事件计数，上限 maxScore）或字面量整数。</li>
 * </ul>
 */
public class LeadScoringEngine {

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpCrmLeadProcessor leadProcessor;

    /**
     * 重算指定线索的评分。无 active config 时返回 null（triggeredAction=NONE，不阻断 Lead 正常流程）。
     */
    public ErpCrmLeadScore recalculateScore(Long leadId, String triggerEvent, IServiceContext context) {
        ErpCrmLead lead = requireLead(leadId);
        ErpCrmLeadScoreConfig config = loadActiveConfig();
        if (config == null) {
            return null;
        }
        List<ErpCrmLeadScoreConfigLine> lines = loadConfigLines(config.getId());
        List<LineResult> results = new ArrayList<>();
        for (ErpCrmLeadScoreConfigLine line : lines) {
            results.add(scoreLine(lead, line, context));
        }

        int totalScore = normalize(results, lines);
        String triggeredAction = determineAction(totalScore, config, lead);
        boolean autoQualified = ErpCrmConstants.TRIGGERED_ACTION_AUTO_QUALIFY.equals(triggeredAction);

        ErpCrmLeadScore score = buildScoreRecord(lead, config, totalScore, results, triggerEvent, triggeredAction, autoQualified);
        scoreDao().saveEntity(score);

        for (LineResult result : results) {
            ErpCrmLeadScoreLine scoreLine = buildScoreLine(score, result);
            scoreLineDao().saveEntity(scoreLine);
        }

        if (autoQualified) {
            leadProcessor.qualify(leadId, context);
        }
        return score;
    }

    // ---------- 评分计算 ----------

    protected LineResult scoreLine(ErpCrmLead lead, ErpCrmLeadScoreConfigLine line, IServiceContext context) {
        String method = line.getScoringMethod();
        String rawValue = null;
        String lookupValue = null;
        int rawScore;
        switch (method == null ? "" : method) {
            case ErpCrmConstants.SCORING_METHOD_LOOKUP:
                rawValue = extractLeadField(lead, line.getFormula());
                LookupMatch lookupMatch = matchLookup(rawValue, line.getLookupTable());
                lookupValue = lookupMatch.matchedValue;
                rawScore = lookupMatch.score;
                break;
            case ErpCrmConstants.SCORING_METHOD_BOOLEAN:
                rawValue = extractLeadField(lead, line.getFormula());
                LookupMatch boolMatch = matchLookup(rawValue, line.getLookupTable());
                lookupValue = boolMatch.matchedValue;
                rawScore = boolMatch.matched ? (line.getMaxScore() != null ? line.getMaxScore() : 0) : 0;
                break;
            case ErpCrmConstants.SCORING_METHOD_FORMULA:
                rawScore = scoreByFormula(lead, line);
                rawValue = String.valueOf(rawScore);
                break;
            default:
                rawScore = 0;
        }
        int weight = line.getWeight() != null ? line.getWeight() : 0;
        int weightedScore = weight > 0 ? rawScore * weight : rawScore;
        return new LineResult(line, rawValue, lookupValue, rawScore, weightedScore);
    }

    /**
     * 归一化 totalScore = Σ(rawScore × weight) / Σ(maxScore × weight) × 100。
     * Σweight 为 0 时退化为 Σ(rawScore) / Σ(maxScore) × 100；Σ(maxScore × weight) 为 0 时 totalScore = 0。
     */
    protected int normalize(List<LineResult> results, List<ErpCrmLeadScoreConfigLine> lines) {
        BigDecimal sumRawWeighted = BigDecimal.ZERO;
        BigDecimal sumMaxWeighted = BigDecimal.ZERO;
        for (LineResult result : results) {
            int weight = result.line.getWeight() != null ? result.line.getWeight() : 0;
            int maxScore = result.line.getMaxScore() != null ? result.line.getMaxScore() : 0;
            if (weight > 0) {
                sumRawWeighted = sumRawWeighted.add(BigDecimal.valueOf(result.rawScore).multiply(BigDecimal.valueOf(weight)));
                sumMaxWeighted = sumMaxWeighted.add(BigDecimal.valueOf(maxScore).multiply(BigDecimal.valueOf(weight)));
            } else {
                sumRawWeighted = sumRawWeighted.add(BigDecimal.valueOf(result.rawScore));
                sumMaxWeighted = sumMaxWeighted.add(BigDecimal.valueOf(maxScore));
            }
        }
        if (sumMaxWeighted.signum() <= 0) {
            return 0;
        }
        return sumRawWeighted.multiply(BigDecimal.valueOf(100))
                .divide(sumMaxWeighted, 0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 阈值→动作矩阵：totalScore ≥ autoQualifyThreshold 且 leadType=LEAD 且 docStatus=NEW 且 config-gated → AUTO_QUALIFY；
     * minScoreForFollowUp ≤ totalScore < autoQualifyThreshold → NOTIFY_OWNER；否则 NONE。
     */
    protected String determineAction(int totalScore, ErpCrmLeadScoreConfig config, ErpCrmLead lead) {
        Integer autoThreshold = config.getAutoQualifyThreshold();
        Integer minFollowUp = config.getMinScoreForFollowUp();
        if (autoThreshold != null && totalScore >= autoThreshold) {
            boolean autoQualifyEnabled = io.nop.api.core.config.AppConfig.var(
                    ErpCrmConstants.CONFIG_LEAD_SCORING_AUTO_QUALIFY, Boolean.TRUE);
            boolean isLead = Objects.equals(lead.getLeadType(), ErpCrmConstants.LEAD_TYPE_LEAD);
            boolean isNew = Objects.equals(lead.getDocStatus(), ErpCrmConstants.DOC_STATUS_NEW);
            if (autoQualifyEnabled && isLead && isNew) {
                return ErpCrmConstants.TRIGGERED_ACTION_AUTO_QUALIFY;
            }
        }
        if (minFollowUp != null && totalScore >= minFollowUp) {
            return ErpCrmConstants.TRIGGERED_ACTION_NOTIFY_OWNER;
        }
        return ErpCrmConstants.TRIGGERED_ACTION_NONE;
    }

    // ---------- 评分方法实现 ----------

    protected String extractLeadField(ErpCrmLead lead, String propName) {
        if (propName == null || propName.trim().isEmpty()) {
            return null;
        }
        int propId = lead.orm_propId(propName.trim());
        if (propId <= 0) {
            return null;
        }
        Object value = lead.orm_propValue(propId);
        return value != null ? String.valueOf(value) : null;
    }

    protected LookupMatch matchLookup(String rawValue, String lookupTableJson) {
        if (rawValue == null || lookupTableJson == null || lookupTableJson.trim().isEmpty()) {
            return LookupMatch.empty();
        }
        List<Map<String, Object>> entries = parseLookupTable(lookupTableJson);
        for (Map<String, Object> entry : entries) {
            String entryValue = stringOf(entry.get("value"));
            if (Objects.equals(norm(entryValue), norm(rawValue))) {
                String label = stringOf(entry.get("label"));
                int score = intOf(entry.get("score"));
                return new LookupMatch(label != null ? label : entryValue, score, true);
            }
        }
        return LookupMatch.empty();
    }

    protected int scoreByFormula(ErpCrmLead lead, ErpCrmLeadScoreConfigLine line) {
        String formula = line.getFormula();
        if (formula == null || formula.trim().isEmpty()) {
            return 0;
        }
        if ("ENGAGEMENT_SCORE".equalsIgnoreCase(formula.trim())) {
            int count = countCompletedEvents(lead.getId());
            int max = line.getMaxScore() != null ? line.getMaxScore() : 30;
            return Math.min(count, max);
        }
        try {
            return Integer.parseInt(formValue(formula.trim(), lead));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 简单公式首版：纯数字直接返回；"count×N" 形式取已完成事件计数×N。
     */
    protected String formValue(String formula, ErpCrmLead lead) {
        int mulIdx = formula.indexOf('×');
        if (mulIdx < 0) {
            mulIdx = formula.indexOf('*');
        }
        if (mulIdx < 0) {
            return formula;
        }
        String base = formula.substring(0, mulIdx).trim();
        String factorStr = formula.substring(mulIdx + 1).trim();
        try {
            int factor = Integer.parseInt(factorStr);
            if ("count".equalsIgnoreCase(base)) {
                int count = countCompletedEvents(lead != null ? lead.getId() : null);
                return String.valueOf(count * factor);
            }
        } catch (NumberFormatException e) {
            // ignore
        }
        return "0";
    }

    protected int countCompletedEvents(Long leadId) {
        IEntityDao<ErpCrmEvent> dao = daoProvider.daoFor(ErpCrmEvent.class);
        QueryBean q = new QueryBean();
        if (leadId != null) {
            q.addFilter(eq("relatedLeadId", leadId));
        }
        q.addFilter(eq("status", ErpCrmConstants.EVENT_STATUS_COMPLETED));
        return (int) dao.findAllByQuery(q).size();
    }

    // ---------- 记录构建 ----------

    protected ErpCrmLeadScore buildScoreRecord(ErpCrmLead lead, ErpCrmLeadScoreConfig config,
                                               int totalScore, List<LineResult> results,
                                               String triggerEvent, String triggeredAction, boolean autoQualified) {
        ErpCrmLeadScore score = scoreDao().newEntity();
        score.setLeadId(lead.getId());
        score.setOrgId(lead.getOrgId());
        score.setConfigId(config.getId());
        score.setTotalScore(totalScore);
        score.setScoreBreakdown(buildBreakdownJson(results));
        score.setAutoQualified(autoQualified);
        score.setTriggeredAction(triggeredAction);
        score.setTriggerEvent(triggerEvent != null ? triggerEvent : ErpCrmConstants.TRIGGER_EVENT_MANUAL);
        score.setCalculatedAt(CoreMetrics.currentDateTime());
        return score;
    }

    protected ErpCrmLeadScoreLine buildScoreLine(ErpCrmLeadScore score, LineResult result) {
        ErpCrmLeadScoreConfigLine configLine = result.line;
        ErpCrmLeadScoreLine line = scoreLineDao().newEntity();
        line.setScoreId(score.getId());
        line.setOrgId(score.getOrgId());
        line.setConfigLineId(configLine.getId());
        line.setCriterionCode(configLine.getCriterionCode());
        line.setCriterionName(configLine.getCriterionName());
        line.setRawValue(result.rawValue);
        line.setLookupValue(result.lookupValue);
        line.setRawScore(result.rawScore);
        line.setWeightedScore(result.weightedScore);
        line.setSequence(configLine.getSequence());
        return line;
    }

    protected String buildBreakdownJson(List<LineResult> results) {
        List<Map<String, Object>> breakdown = new ArrayList<>();
        for (LineResult result : results) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("criterionCode", result.line.getCriterionCode());
            entry.put("rawScore", result.rawScore);
            entry.put("weightedScore", result.weightedScore);
            entry.put("weight", result.line.getWeight());
            breakdown.add(entry);
        }
        return io.nop.api.core.json.JSON.stringify(breakdown);
    }

    // ---------- 配置加载 ----------

    protected ErpCrmLeadScoreConfig loadActiveConfig() {
        IEntityDao<ErpCrmLeadScoreConfig> dao = daoProvider.daoFor(ErpCrmLeadScoreConfig.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("isActive", Boolean.TRUE));
        List<ErpCrmLeadScoreConfig> active = dao.findAllByQuery(q);
        if (active.size() > 1) {
            throw new NopException(ErpCrmErrors.ERR_MULTIPLE_ACTIVE_SCORE_CONFIG)
                    .param(ErpCrmErrors.ARG_ACTIVE_COUNT, active.size());
        }
        return active.isEmpty() ? null : active.get(0);
    }

    protected List<ErpCrmLeadScoreConfigLine> loadConfigLines(Long configId) {
        IEntityDao<ErpCrmLeadScoreConfigLine> dao = daoProvider.daoFor(ErpCrmLeadScoreConfigLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("configId", configId));
        q.addOrderField("sequence", false);
        return dao.findAllByQuery(q);
    }

    // ---------- 辅助 ----------

    protected ErpCrmLead requireLead(Long leadId) {
        ErpCrmLead lead = leadDao().getEntityById(leadId);
        if (lead == null) {
            throw new NopException(ErpCrmErrors.ERR_LEAD_NOT_FOUND)
                    .param(ErpCrmErrors.ARG_LEAD_ID, leadId);
        }
        return lead;
    }

    protected List<Map<String, Object>> parseLookupTable(String json) {
        Object parsed = io.nop.api.core.json.JSON.parse(json);
        if (parsed instanceof List) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (List<?>) parsed) {
                if (item instanceof Map) {
                    Map<String, Object> map = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> e : ((Map<?, ?>) item).entrySet()) {
                        map.put(String.valueOf(e.getKey()), e.getValue());
                    }
                    result.add(map);
                }
            }
            return result;
        }
        return new ArrayList<>();
    }

    protected String norm(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    protected String stringOf(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    protected int intOf(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    protected IEntityDao<ErpCrmLead> leadDao() {
        return daoProvider.daoFor(ErpCrmLead.class);
    }

    protected IEntityDao<ErpCrmLeadScore> scoreDao() {
        return daoProvider.daoFor(ErpCrmLeadScore.class);
    }

    protected IEntityDao<ErpCrmLeadScoreLine> scoreLineDao() {
        return daoProvider.daoFor(ErpCrmLeadScoreLine.class);
    }

    // ---------- 内部结构 ----------

    protected static class LineResult {
        final ErpCrmLeadScoreConfigLine line;
        final String rawValue;
        final String lookupValue;
        final int rawScore;
        final int weightedScore;

        LineResult(ErpCrmLeadScoreConfigLine line, String rawValue, String lookupValue, int rawScore, int weightedScore) {
            this.line = line;
            this.rawValue = rawValue;
            this.lookupValue = lookupValue;
            this.rawScore = rawScore;
            this.weightedScore = weightedScore;
        }
    }

    protected static class LookupMatch {
        final String matchedValue;
        final int score;
        final boolean matched;

        LookupMatch(String matchedValue, int score, boolean matched) {
            this.matchedValue = matchedValue;
            this.score = score;
            this.matched = matched;
        }

        static LookupMatch empty() {
            return new LookupMatch(null, 0, false);
        }
    }
}
