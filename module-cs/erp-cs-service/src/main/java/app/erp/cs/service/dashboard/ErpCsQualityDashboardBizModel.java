package app.erp.cs.service.dashboard;

import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTeam;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * 客服绩效聚合看板（{@code sla.md} §四 + {@code csat.md} §四 + {@code dashboards.md} §实现约定）。
 *
 * <p>服务型 BizObject（非实体聚合），镜像 {@code ErpFinDashboardBizModel} 域隔离范式。
 * 注入 {@link IDaoProvider}/{@link IOrmTemplate} 经 {@link QueryBean} 过滤后内存聚合。
 *
 * <p>三个 @BizQuery：
 * <ul>
 *   <li>{@link #getDashboardKpi} — SLA 达标率 / 平均解决时长 / 超时工单数 / 总工单数 / 平均首次响应时长。</li>
 *   <li>{@link #getTeamSlaRanking} — 按 team.name 分组 SLA 达标率 + 平均解决时长 + 工单数。
 *       注：{@link ErpCsTicket} 无 teamId 列，team 维度经 {@code slaPolicyId → ErpCsSlaPolicy.teamId → ErpCsTeam} 关联获取。</li>
 *   <li>{@link #getAgentCsatBreakdown} — 按 assignedToId 分组 AVG(csat/nps/ces) + 工单数，经 {@link ErpCsSurvey} 关联。</li>
 * </ul>
 */
@BizModel("ErpCsQualityDashboard")
public class ErpCsQualityDashboardBizModel {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ===================== KPI =====================

    @BizQuery
    public Map<String, Object> getDashboardKpi(@Optional @Name("startDate") String startDate,
                                                @Optional @Name("endDate") String endDate,
                                                IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpCsTicket> tickets = loadClosedTickets(startDate, endDate);
            Map<String, Object> kpi = new LinkedHashMap<>();
            int total = tickets.size();
            int slaCompleted = 0;
            int slaBreached = 0;
            long durationSumMs = 0L;
            int durationCount = 0;
            long firstResponseSumMs = 0L;
            int firstResponseCount = 0;

            for (ErpCsTicket t : tickets) {
                if (Boolean.TRUE.equals(t.getIsSlaCompleted())) {
                    slaCompleted++;
                } else {
                    slaBreached++;
                }
                if (t.getDuration() != null && t.getDuration() > 0) {
                    durationSumMs += t.getDuration() * 60_000L;
                    durationCount++;
                }
                if (t.getCreateTime() != null && t.getStartDateTime() != null) {
                    long diff = durationMsBetween(t.getCreateTime().toLocalDateTime(), t.getStartDateTime().toLocalDateTime());
                    if (diff >= 0) {
                        firstResponseSumMs += diff;
                        firstResponseCount++;
                    }
                }
            }

            BigDecimal slaRate = total > 0
                    ? BigDecimal.valueOf(slaCompleted).divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP)
                    : null;
            BigDecimal avgDurationHours = durationCount > 0
                    ? BigDecimal.valueOf(durationSumMs).divide(BigDecimal.valueOf(durationCount), 2, RoundingMode.HALF_UP)
                            .divide(BigDecimal.valueOf(3600000L), 2, RoundingMode.HALF_UP)
                    : null;
            BigDecimal avgFirstResponseHours = firstResponseCount > 0
                    ? BigDecimal.valueOf(firstResponseSumMs).divide(BigDecimal.valueOf(firstResponseCount), 2, RoundingMode.HALF_UP)
                            .divide(BigDecimal.valueOf(3600000L), 2, RoundingMode.HALF_UP)
                    : null;

            kpi.put("startDate", startDate);
            kpi.put("endDate", endDate);
            kpi.put("totalTickets", total);
            kpi.put("slaCompletedCount", slaCompleted);
            kpi.put("slaBreachedCount", slaBreached);
            kpi.put("slaCompletionRate", slaRate);
            kpi.put("avgResolutionHours", avgDurationHours);
            kpi.put("avgFirstResponseHours", avgFirstResponseHours);
            return kpi;
        });
    }

    // ===================== Team SLA Ranking =====================

    @BizQuery
    public List<Map<String, Object>> getTeamSlaRanking(@Optional @Name("startDate") String startDate,
                                                       @Optional @Name("endDate") String endDate,
                                                       IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpCsTicket> tickets = loadClosedTickets(startDate, endDate);
            if (tickets.isEmpty()) {
                return Collections.emptyList();
            }

            // 收集 slaPolicyId，批量加载 SlaPolicy → teamId 映射
            Set<Long> slaPolicyIds = new HashSet<>();
            for (ErpCsTicket t : tickets) {
                if (t.getSlaPolicyId() != null) {
                    slaPolicyIds.add(t.getSlaPolicyId());
                }
            }
            Map<Long, Long> policyToTeam = loadSlaPolicyTeamMap(slaPolicyIds);

            // 收集 teamId，批量加载 Team → name 映射
            Set<Long> teamIds = new HashSet<>(policyToTeam.values());
            Map<Long, String> teamNames = loadTeamNames(teamIds);

            // 按 teamId 聚合
            Map<Long, TeamAgg> agg = new LinkedHashMap<>();
            for (ErpCsTicket t : tickets) {
                Long teamId = t.getSlaPolicyId() == null ? null : policyToTeam.get(t.getSlaPolicyId());
                TeamAgg a = agg.computeIfAbsent(teamId, TeamAgg::new);
                a.totalTickets++;
                if (Boolean.TRUE.equals(t.getIsSlaCompleted())) {
                    a.slaCompleted++;
                }
                if (t.getDuration() != null && t.getDuration() > 0) {
                    a.durationSumMs += t.getDuration() * 60_000L;
                    a.durationCount++;
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (TeamAgg a : agg.values()) {
                BigDecimal slaRate = a.totalTickets > 0
                        ? BigDecimal.valueOf(a.slaCompleted).divide(BigDecimal.valueOf(a.totalTickets), 4, RoundingMode.HALF_UP)
                        : null;
                BigDecimal avgHours = a.durationCount > 0
                        ? BigDecimal.valueOf(a.durationSumMs).divide(BigDecimal.valueOf(a.durationCount), 2, RoundingMode.HALF_UP)
                                .divide(BigDecimal.valueOf(3600000L), 2, RoundingMode.HALF_UP)
                        : null;
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("teamId", a.teamId);
                r.put("teamName", a.teamId == null ? "(未分派)" : teamNames.getOrDefault(a.teamId, ""));
                r.put("totalTickets", a.totalTickets);
                r.put("slaCompletedCount", a.slaCompleted);
                r.put("slaCompletionRate", slaRate);
                r.put("avgResolutionHours", avgHours);
                rows.add(r);
            }

            // 按 slaCompleted DESC 排序（对齐 sla.md §4.3 ORDER BY slaCompleted DESC）
            rows.sort(Comparator.comparingInt((Map<String, Object> r) ->
                    toInt(r.get("slaCompletedCount"))).reversed());
            return rows;
        });
    }

    // ===================== Agent CSAT Breakdown =====================

    @BizQuery
    public List<Map<String, Object>> getAgentCsatBreakdown(@Optional @Name("startDate") String startDate,
                                                           @Optional @Name("endDate") String endDate,
                                                           IServiceContext context) {
        return ormTemplate.runInSession(session -> {
            List<ErpCsTicket> tickets = loadClosedTickets(startDate, endDate);
            if (tickets.isEmpty()) {
                return Collections.emptyList();
            }

            // 收集 ticketIds，批量加载 Survey 聚合
            Set<Long> ticketIds = new HashSet<>();
            for (ErpCsTicket t : tickets) {
                if (t.getId() != null) {
                    ticketIds.add(t.getId());
                }
            }
            Map<Long, SurveyAgg> surveyByTicket = loadSurveyByTicket(ticketIds);

            // ticketId → assignedToId 映射
            Map<Long, String> ticketToAgent = new HashMap<>();
            for (ErpCsTicket t : tickets) {
                if (t.getId() != null && t.getAssignedToId() != null) {
                    ticketToAgent.put(t.getId(), t.getAssignedToId());
                }
            }

            // 按 assignedToId 聚合
            Map<String, AgentAgg> agg = new LinkedHashMap<>();
            for (ErpCsTicket t : tickets) {
                String agentId = t.getAssignedToId();
                if (agentId == null) {
                    continue;
                }
                AgentAgg a = agg.computeIfAbsent(agentId, AgentAgg::new);
                a.ticketCount++;
                SurveyAgg sa = t.getId() == null ? null : surveyByTicket.get(t.getId());
                if (sa != null) {
                    a.surveyCount += sa.count;
                    a.csatSum += sa.csatSum;
                    a.npsSum += sa.npsSum;
                    a.cesSum += sa.cesSum;
                }
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            for (AgentAgg a : agg.values()) {
                Map<String, Object> r = new LinkedHashMap<>();
                r.put("agentId", a.agentId);
                r.put("ticketCount", a.ticketCount);
                r.put("surveyCount", a.surveyCount);
                r.put("avgCsat", avg(a.surveyCount, a.csatSum));
                r.put("avgNps", avg(a.surveyCount, a.npsSum));
                r.put("avgCes", avg(a.surveyCount, a.cesSum));
                rows.add(r);
            }
            return rows;
        });
    }

    // ===================== helpers =====================

    private List<ErpCsTicket> loadClosedTickets(String startDate, String endDate) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpCsConstants.TICKET_STATUS_CLOSED));
        LocalDate[] range = parseRange(startDate, endDate);
        if (range != null) {
            Timestamp startTs = Timestamp.valueOf(range[0].atStartOfDay());
            q.addFilter(ge("createTime", startTs));
            if (range[1] != null) {
                Timestamp endTs = Timestamp.valueOf(range[1].plusDays(1).atStartOfDay());
                q.addFilter(lt("createTime", endTs));
            }
        }
        return daoProvider.daoFor(ErpCsTicket.class).findAllByQuery(q);
    }

    private Map<Long, Long> loadSlaPolicyTeamMap(Set<Long> slaPolicyIds) {
        if (slaPolicyIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("id", slaPolicyIds));
        List<ErpCsSlaPolicy> policies = daoProvider.daoFor(ErpCsSlaPolicy.class).findAllByQuery(q);
        Map<Long, Long> map = new HashMap<>();
        for (ErpCsSlaPolicy p : policies) {
            map.put(p.getId(), p.getTeamId());
        }
        return map;
    }

    private Map<Long, String> loadTeamNames(Set<Long> teamIds) {
        if (teamIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> nonNull = new HashSet<>();
        for (Long id : teamIds) {
            if (id != null) {
                nonNull.add(id);
            }
        }
        if (nonNull.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("id", nonNull));
        List<ErpCsTeam> teams = daoProvider.daoFor(ErpCsTeam.class).findAllByQuery(q);
        Map<Long, String> names = new HashMap<>();
        for (ErpCsTeam t : teams) {
            names.put(t.getId(), t.getName());
        }
        return names;
    }

    private Map<Long, SurveyAgg> loadSurveyByTicket(Set<Long> ticketIds) {
        if (ticketIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("ticketId", ticketIds));
        List<ErpCsSurvey> surveys = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q);
        Map<Long, SurveyAgg> map = new HashMap<>();
        for (ErpCsSurvey s : surveys) {
            Long tId = s.getTicketId();
            if (tId == null) {
                continue;
            }
            SurveyAgg a = map.computeIfAbsent(tId, k -> new SurveyAgg());
            a.count++;
            a.csatSum += nz(s.getCsatScore());
            a.npsSum += nz(s.getNpsScore());
            a.cesSum += nz(s.getCesScore());
        }
        return map;
    }

    private static LocalDate[] parseRange(String startDate, String endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        LocalDate start = parseDate(startDate);
        LocalDate end = parseDate(endDate);
        return new LocalDate[]{start, end};
    }

    private static LocalDate parseDate(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(s.trim(), DATE_FMT);
        } catch (Exception e) {
            return null;
        }
    }

    private static long durationMsBetween(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null) {
            return -1;
        }
        return java.time.Duration.between(from, to).toMillis();
    }

    private static BigDecimal avg(int count, long sum) {
        if (count <= 0) {
            return null;
        }
        return BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
    }

    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private static int toInt(Object v) {
        if (v == null) {
            return 0;
        }
        if (v instanceof Number) {
            return ((Number) v).intValue();
        }
        return Integer.valueOf(String.valueOf(v));
    }

    // ===================== aggregator inner classes =====================

    private static class TeamAgg {
        final Long teamId;
        int totalTickets = 0;
        int slaCompleted = 0;
        long durationSumMs = 0L;
        int durationCount = 0;

        TeamAgg(Long teamId) {
            this.teamId = teamId;
        }
    }

    private static class AgentAgg {
        final String agentId;
        int ticketCount = 0;
        int surveyCount = 0;
        long csatSum = 0L;
        long npsSum = 0L;
        long cesSum = 0L;

        AgentAgg(String agentId) {
            this.agentId = agentId;
        }
    }

    private static class SurveyAgg {
        int count = 0;
        long csatSum = 0L;
        long npsSum = 0L;
        long cesSum = 0L;
    }
}
