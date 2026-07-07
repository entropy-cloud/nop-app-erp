package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmLeadFunnel;
import app.erp.crm.dao.entity.ErpCrmLostReason;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConstants;
import app.erp.crm.service.ErpCrmErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.lang.json.JsonTool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 销售漏斗聚合引擎。从 {@link ErpCrmLeadConvLog}（阶段流转日志）+ {@link ErpCrmLead}（当前状态）+ {@link ErpCrmStage}
 * （阶段定义）按 periodStart~periodEnd + 维度（territoryId/teamId/sourceId）聚合：
 *
 * <ul>
 *   <li>{@link ErpCrmLeadFunnel} 头：总量/商机/赢单/丢单/收入/周期/加权/平均单值；</li>
 *   <li>{@link ErpCrmFunnelStageMetrics} 明细：进入/流出/剩余/转化率/流失率/停留天数/丢失原因 TOP N JSON。</li>
 * </ul>
 *
 * <p>{@code stageName} 快照防阶段定义变更致历史错误（对齐 {@code docs/design/crm/lead-waterfall.md §聚合计算流程}）。
 *
 * <p>纯函数式 + 注入加载函数便于单测：{@link #aggregate} 接收预加载的 convLogs/leads/stages 列表，
 * 不依赖 IoC，可独立构造数据测试各度量 + 丢失原因 TOP N + stageName 快照 + 空数据零值。
 */
public class FunnelAggregationEngine {

    /**
     * 聚合漏斗快照。
     *
     * @param periodStart 期间开始（含）
     * @param periodEnd   期间结束（含）
     * @param territoryId 维度：区域（可空=全部）
     * @param teamId      维度：团队（可空=全部）
     * @param sourceId    维度：来源（可空=全部）
     * @param convLogs    期间内 ConvLog 列表（已按 changedAt 过滤）
     * @param leads       相关 Lead 列表（已按维度过滤）
     * @param stages      阶段定义列表（全量，按 sequence 排序）
     * @param lostReasons 丢失原因字典（id → ErpCrmLostReason，用于 lostReasonTop 名称解析）
     * @param topLostN    丢失原因 TOP N
     * @return 聚合结果（funnel 头 + 阶段明细列表）
     */
    public FunnelSnapshot aggregate(LocalDate periodStart, LocalDate periodEnd,
                                     Long territoryId, Long teamId, Long sourceId,
                                     List<ErpCrmLeadConvLog> convLogs,
                                     List<ErpCrmLead> leads,
                                     List<ErpCrmStage> stages,
                                     Map<Long, ErpCrmLostReason> lostReasons,
                                     int topLostN) {
        if (periodStart != null && periodEnd != null && periodStart.isAfter(periodEnd)) {
            throw new NopException(ErpCrmErrors.ERR_FUNNEL_PERIOD_INVALID)
                    .param(ErpCrmErrors.ARG_PERIOD_START, periodStart)
                    .param(ErpCrmErrors.ARG_PERIOD_END, periodEnd);
        }

        Map<Long, ErpCrmLead> leadById = new HashMap<>();
        for (ErpCrmLead l : leads) {
            leadById.put(l.getId(), l);
        }

        FunnelSnapshot snapshot = new FunnelSnapshot();
        snapshot.setPeriodStart(periodStart);
        snapshot.setPeriodEnd(periodEnd);
        snapshot.setTerritoryId(territoryId);
        snapshot.setTeamId(teamId);
        snapshot.setSourceId(sourceId);

        // 头度量
        computeHeader(snapshot, leads, convLogs, leadById);

        // 阶段明细
        List<ErpCrmFunnelStageMetrics> stageMetrics = computeStageMetrics(
                convLogs, leads, stages, lostReasons, leadById, topLostN);
        snapshot.setStageMetrics(stageMetrics);

        snapshot.setCalculatedAt(CoreMetrics.currentDateTime());
        return snapshot;
    }

    // ---------- 头度量 ----------

    protected void computeHeader(FunnelSnapshot snapshot,
                                  List<ErpCrmLead> leads,
                                  List<ErpCrmLeadConvLog> convLogs,
                                  Map<Long, ErpCrmLead> leadById) {
        int totalAtTop = 0;
        int totalOpps = 0;
        int totalWon = 0;
        int totalLost = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal lostRevenue = BigDecimal.ZERO;
        BigDecimal weightedRevenue = BigDecimal.ZERO;
        long wonCycleDaysSum = 0;
        int wonCycleCount = 0;

        // 顶部总量：进入首个阶段的去重 lead 数（无 ConvLog 时退化为 leads 数）
        if (convLogs != null && !convLogs.isEmpty()) {
            totalAtTop = (int) convLogs.stream()
                    .map(ErpCrmLeadConvLog::getLeadId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();
        } else {
            totalAtTop = leads.size();
        }

        for (ErpCrmLead lead : leads) {
            boolean isOpp = ErpCrmConstants.LEAD_TYPE_OPPORTUNITY.equals(lead.getLeadType());
            if (isOpp) {
                totalOpps++;
            }
            String status = lead.getDocStatus();
            BigDecimal revenue = nvl(lead.getExpectedRevenue());

            if (ErpCrmConstants.DOC_STATUS_CONVERTED.equals(status)) {
                totalWon++;
                totalRevenue = totalRevenue.add(revenue);
                // 销售周期：首条 ConvLog.changedAt → 末条（赢单）changedAt，回退到 lead 字段不可得时跳过
                long cycleDays = computeSalesCycleDays(lead.getId(), convLogs);
                if (cycleDays >= 0) {
                    wonCycleDaysSum += cycleDays;
                    wonCycleCount++;
                }
            } else if (ErpCrmConstants.DOC_STATUS_LOST.equals(status)) {
                totalLost++;
                lostRevenue = lostRevenue.add(revenue);
            } else if (isOpp && !ErpCrmConstants.DOC_STATUS_CANCELLED.equals(status)) {
                // 活跃商机加权收入：expectedRevenue × probability / 100
                int probability = lead.getProbability() != null ? lead.getProbability() : 0;
                weightedRevenue = weightedRevenue.add(
                        revenue.multiply(BigDecimal.valueOf(probability))
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
            }
        }

        BigDecimal avgDealSize = totalWon > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalWon), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        double avgCycleDays = wonCycleCount > 0
                ? (double) wonCycleDaysSum / wonCycleCount
                : 0.0;

        snapshot.setTotalLeadsAtTop(totalAtTop);
        snapshot.setTotalOpportunities(totalOpps);
        snapshot.setTotalWon(totalWon);
        snapshot.setTotalLost(totalLost);
        snapshot.setTotalRevenue(totalRevenue);
        snapshot.setLostRevenue(lostRevenue);
        snapshot.setWeightedRevenue(weightedRevenue);
        snapshot.setAvgDealSize(avgDealSize);
        snapshot.setAvgSalesCycleDays(avgCycleDays);
    }

    /**
     * 计算销售周期（天）：leadId 的首条与末条 ConvLog.changedAt 间隔。
     * 无 ConvLog 或仅一条返回 -1。
     */
    protected long computeSalesCycleDays(Long leadId, List<ErpCrmLeadConvLog> convLogs) {
        if (convLogs == null || convLogs.isEmpty() || leadId == null) {
            return -1;
        }
        List<LocalDateTime> times = convLogs.stream()
                .filter(l -> leadId.equals(l.getLeadId()) && l.getChangedAt() != null)
                .map(ErpCrmLeadConvLog::getChangedAt)
                .sorted()
                .collect(Collectors.toList());
        if (times.size() < 2) {
            return -1;
        }
        return java.time.Duration.between(times.get(0), times.get(times.size() - 1)).toDays();
    }

    // ---------- 阶段明细 ----------

    protected List<ErpCrmFunnelStageMetrics> computeStageMetrics(
            List<ErpCrmLeadConvLog> convLogs,
            List<ErpCrmLead> leads,
            List<ErpCrmStage> stages,
            Map<Long, ErpCrmLostReason> lostReasons,
            Map<Long, ErpCrmLead> leadById,
            int topLostN) {

        // 按阶段 sequence 排序
        List<ErpCrmStage> orderedStages = new ArrayList<>(stages);
        orderedStages.sort(Comparator
                .comparingInt((ErpCrmStage s) -> s.getSequence() != null ? s.getSequence() : Integer.MAX_VALUE)
                .thenComparing(s -> s.getId() != null ? s.getId() : Long.MAX_VALUE));

        // 按 stageId 索引 ConvLog
        Map<Long, List<ErpCrmLeadConvLog>> entryByStage = new HashMap<>();
        Map<Long, List<ErpCrmLeadConvLog>> exitByStage = new HashMap<>();
        if (convLogs != null) {
            for (ErpCrmLeadConvLog log : convLogs) {
                if (log.getToStageId() != null) {
                    entryByStage.computeIfAbsent(log.getToStageId(), k -> new ArrayList<>()).add(log);
                }
                if (log.getFromStageId() != null) {
                    exitByStage.computeIfAbsent(log.getFromStageId(), k -> new ArrayList<>()).add(log);
                }
            }
        }

        // 丢失归因：lead.lostReasonId 当前状态 → 按末次所在阶段（末条 ConvLog.toStageId）归因
        Map<Long, List<ErpCrmLead>> lostByStage = new HashMap<>();
        Map<Long, Long> lastStageByLead = computeLastStageByLead(convLogs);
        for (ErpCrmLead lead : leads) {
            if (ErpCrmConstants.DOC_STATUS_LOST.equals(lead.getDocStatus())) {
                Long lastStage = lastStageByLead.get(lead.getId());
                if (lastStage != null) {
                    lostByStage.computeIfAbsent(lastStage, k -> new ArrayList<>()).add(lead);
                }
            }
        }

        // 停留时长：按 leadId 收集进入各阶段的时间戳，计算与下一条流转的间隔
        Map<Long, Map<Long, Long>> daysInStageByLead = computeDaysInStage(convLogs);

        List<ErpCrmFunnelStageMetrics> result = new ArrayList<>();
        for (int i = 0; i < orderedStages.size(); i++) {
            ErpCrmStage stage = orderedStages.get(i);
            Long stageId = stage.getId();

            List<ErpCrmLeadConvLog> entries = entryByStage.getOrDefault(stageId, java.util.Collections.emptyList());
            List<ErpCrmLeadConvLog> exits = exitByStage.getOrDefault(stageId, java.util.Collections.emptyList());
            List<ErpCrmLead> lostLeads = lostByStage.getOrDefault(stageId, java.util.Collections.emptyList());

            int leadCountIn = (int) entries.stream().map(ErpCrmLeadConvLog::getLeadId).filter(java.util.Objects::nonNull).distinct().count();
            int leadCountOut = (int) exits.stream().map(ErpCrmLeadConvLog::getLeadId).filter(java.util.Objects::nonNull).distinct().count();
            int lostCount = lostLeads.size();
            BigDecimal lostAmount = BigDecimal.ZERO;
            for (ErpCrmLead l : lostLeads) {
                lostAmount = lostAmount.add(nvl(l.getExpectedRevenue()));
            }
            int remaining = Math.max(0, leadCountIn - leadCountOut - lostCount);

            // 转化率：进入下一阶段的人数 / 本阶段进入人数
            int nextStageIn = 0;
            if (i + 1 < orderedStages.size()) {
                Long nextStageId = orderedStages.get(i + 1).getId();
                nextStageIn = (int) entryByStage.getOrDefault(nextStageId, java.util.Collections.emptyList()).stream()
                        .map(ErpCrmLeadConvLog::getLeadId).filter(java.util.Objects::nonNull).distinct().count();
            }
            double conversionRate = leadCountIn > 0 ? (double) nextStageIn / leadCountIn : 0.0;
            double dropOffRate = leadCountIn > 0 ? (double) lostCount / leadCountIn : 0.0;

            // 平均停留天数
            double avgDays = computeAvgDaysInStage(stageId, daysInStageByLead);

            // 丢失原因 TOP N
            String lostReasonTop = computeLostReasonTop(lostLeads, lostReasons, topLostN);

            ErpCrmFunnelStageMetrics m = new ErpCrmFunnelStageMetrics();
            m.setStageId(stageId);
            m.setStageOrder(stage.getSequence());
            m.setStageName(stage.getStageName()); // stageName 快照
            m.setLeadCountIn(leadCountIn);
            m.setLeadCountOut(leadCountOut);
            m.setLeadCountRemaining(remaining);
            m.setConversionRate(round4(conversionRate));
            m.setDropOffRate(round4(dropOffRate));
            m.setAvgDaysInStage(round2(avgDays));
            m.setLostCount(lostCount);
            m.setLostAmount(lostAmount);
            m.setLostReasonTop(lostReasonTop);
            result.add(m);
        }
        return result;
    }

    /**
     * 计算每个 lead 末次所在阶段（末条 ConvLog.toStageId）。用于丢失归因。
     */
    protected Map<Long, Long> computeLastStageByLead(List<ErpCrmLeadConvLog> convLogs) {
        Map<Long, Long> result = new HashMap<>();
        if (convLogs == null) {
            return result;
        }
        List<ErpCrmLeadConvLog> sorted = convLogs.stream()
                .filter(l -> l.getLeadId() != null && l.getChangedAt() != null)
                .sorted(Comparator.comparing(ErpCrmLeadConvLog::getChangedAt))
                .collect(Collectors.toList());
        for (ErpCrmLeadConvLog log : sorted) {
            if (log.getToStageId() != null) {
                result.put(log.getLeadId(), log.getToStageId());
            }
        }
        return result;
    }

    /**
     * 计算每个 lead 在各阶段的停留天数。
     * key: leadId → (stageId → days)。停留 = 下一条流转的 changedAt - 本条 changedAt（同 stageId）。
     */
    protected Map<Long, Map<Long, Long>> computeDaysInStage(List<ErpCrmLeadConvLog> convLogs) {
        Map<Long, Map<Long, Long>> result = new HashMap<>();
        if (convLogs == null) {
            return result;
        }
        Map<Long, List<ErpCrmLeadConvLog>> byLead = convLogs.stream()
                .filter(l -> l.getLeadId() != null && l.getChangedAt() != null && l.getToStageId() != null)
                .collect(Collectors.groupingBy(ErpCrmLeadConvLog::getLeadId));
        for (Map.Entry<Long, List<ErpCrmLeadConvLog>> entry : byLead.entrySet()) {
            List<ErpCrmLeadConvLog> logs = new ArrayList<>(entry.getValue());
            logs.sort(Comparator.comparing(ErpCrmLeadConvLog::getChangedAt));
            Map<Long, Long> stageDays = new HashMap<>();
            for (int i = 0; i < logs.size() - 1; i++) {
                ErpCrmLeadConvLog cur = logs.get(i);
                ErpCrmLeadConvLog next = logs.get(i + 1);
                long days = java.time.Duration.between(cur.getChangedAt(), next.getChangedAt()).toDays();
                stageDays.merge(cur.getToStageId(), days, Long::sum);
            }
            result.put(entry.getKey(), stageDays);
        }
        return result;
    }

    protected double computeAvgDaysInStage(Long stageId, Map<Long, Map<Long, Long>> daysInStageByLead) {
        long sum = 0;
        int count = 0;
        for (Map<Long, Long> stageDays : daysInStageByLead.values()) {
            Long days = stageDays.get(stageId);
            if (days != null) {
                sum += days;
                count++;
            }
        }
        return count > 0 ? (double) sum / count : 0.0;
    }

    /**
     * 计算丢失原因 TOP N（JSON 字符串）：[{reasonName, lostReasonId, count}, ...] 按 count 降序。
     */
    protected String computeLostReasonTop(List<ErpCrmLead> lostLeads,
                                           Map<Long, ErpCrmLostReason> lostReasons,
                                           int topN) {
        if (lostLeads == null || lostLeads.isEmpty() || topN <= 0) {
            return null;
        }
        Map<Long, Integer> reasonCount = new HashMap<>();
        Map<Long, String> reasonName = new HashMap<>();
        int unknownCount = 0;
        for (ErpCrmLead l : lostLeads) {
            Long rid = l.getLostReasonId();
            if (rid == null) {
                unknownCount++;
                continue;
            }
            reasonCount.merge(rid, 1, Integer::sum);
            ErpCrmLostReason reason = lostReasons != null ? lostReasons.get(rid) : null;
            reasonName.putIfAbsent(rid, reason != null ? reason.getName() : ("reason-" + rid));
        }
        List<Map<String, Object>> list = new ArrayList<>();
        reasonCount.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(topN)
                .forEach(e -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("lostReasonId", e.getKey());
                    row.put("reasonName", reasonName.get(e.getKey()));
                    row.put("count", e.getValue());
                    list.add(row);
                });
        if (unknownCount > 0 && list.size() < topN) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("lostReasonId", null);
            row.put("reasonName", "(未知)");
            row.put("count", unknownCount);
            list.add(row);
        }
        return JsonTool.serialize(list, false);
    }

    protected BigDecimal nvl(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    protected Double round4(double value) {
        return Math.round(value * 10000) / 10000.0;
    }

    protected Double round2(double value) {
        return Math.round(value * 100) / 100.0;
    }

    // ---------- 快照 DTO ----------

    public static class FunnelSnapshot {
        private LocalDate periodStart;
        private LocalDate periodEnd;
        private Long territoryId;
        private Long teamId;
        private Long sourceId;
        private int totalLeadsAtTop;
        private int totalOpportunities;
        private int totalWon;
        private int totalLost;
        private BigDecimal totalRevenue = BigDecimal.ZERO;
        private BigDecimal lostRevenue = BigDecimal.ZERO;
        private BigDecimal weightedRevenue = BigDecimal.ZERO;
        private BigDecimal avgDealSize = BigDecimal.ZERO;
        private double avgSalesCycleDays;
        private LocalDateTime calculatedAt;
        private List<ErpCrmFunnelStageMetrics> stageMetrics = new ArrayList<>();

        public LocalDate getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
        public LocalDate getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(LocalDate periodEnd) { this.periodEnd = periodEnd; }
        public Long getTerritoryId() { return territoryId; }
        public void setTerritoryId(Long territoryId) { this.territoryId = territoryId; }
        public Long getTeamId() { return teamId; }
        public void setTeamId(Long teamId) { this.teamId = teamId; }
        public Long getSourceId() { return sourceId; }
        public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
        public int getTotalLeadsAtTop() { return totalLeadsAtTop; }
        public void setTotalLeadsAtTop(int totalLeadsAtTop) { this.totalLeadsAtTop = totalLeadsAtTop; }
        public int getTotalOpportunities() { return totalOpportunities; }
        public void setTotalOpportunities(int totalOpportunities) { this.totalOpportunities = totalOpportunities; }
        public int getTotalWon() { return totalWon; }
        public void setTotalWon(int totalWon) { this.totalWon = totalWon; }
        public int getTotalLost() { return totalLost; }
        public void setTotalLost(int totalLost) { this.totalLost = totalLost; }
        public BigDecimal getTotalRevenue() { return totalRevenue; }
        public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
        public BigDecimal getLostRevenue() { return lostRevenue; }
        public void setLostRevenue(BigDecimal lostRevenue) { this.lostRevenue = lostRevenue; }
        public BigDecimal getWeightedRevenue() { return weightedRevenue; }
        public void setWeightedRevenue(BigDecimal weightedRevenue) { this.weightedRevenue = weightedRevenue; }
        public BigDecimal getAvgDealSize() { return avgDealSize; }
        public void setAvgDealSize(BigDecimal avgDealSize) { this.avgDealSize = avgDealSize; }
        public double getAvgSalesCycleDays() { return avgSalesCycleDays; }
        public void setAvgSalesCycleDays(double avgSalesCycleDays) { this.avgSalesCycleDays = avgSalesCycleDays; }
        public LocalDateTime getCalculatedAt() { return calculatedAt; }
        public void setCalculatedAt(LocalDateTime calculatedAt) { this.calculatedAt = calculatedAt; }
        public List<ErpCrmFunnelStageMetrics> getStageMetrics() { return stageMetrics; }
        public void setStageMetrics(List<ErpCrmFunnelStageMetrics> stageMetrics) { this.stageMetrics = stageMetrics; }
    }
}
