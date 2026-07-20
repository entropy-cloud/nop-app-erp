package app.erp.cs.service.dashboard;

import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTeam;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 客服绩效聚合看板集成测试（plan 2026-07-11-1234-2 §Phase 4）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>getDashboardKpi：SLA 达标率 / 平均解决时长 / 超时工单数 / 总工单数。</li>
 *   <li>getTeamSlaRanking：按 team.name 分组 SLA 达标率 + 工单数（经 slaPolicy 关联）。</li>
 *   <li>getAgentCsatBreakdown：按 assignedToId 分组 CSAT/NPS/CES 均值。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsQualityDashboard extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();
    static final Long PARTNER_ID = 9801L;
    static final Long TICKET_TYPE_ID = 6201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpCsQualityDashboardBizModel dashboardBiz;

    @Test
    public void testKpiEmptyDatasetReturnsZeros() {
        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(0, kpi.get("totalTickets"));
        assertEquals(0, kpi.get("slaCompletedCount"));
        assertEquals(0, kpi.get("slaBreachedCount"));
        assertEquals(null, kpi.get("slaCompletionRate"));
    }

    @Test
    public void testKpiAggregation() {
        LocalDate today = CoreMetrics.today();
        ormTemplate.runInSession(() -> {
            seedClosedTicket(9501L, "TK-KPI-1", true, 120, today);
            seedClosedTicket(9502L, "TK-KPI-2", true, 60, today);
            seedClosedTicket(9503L, "TK-KPI-3", false, 30, today);
        });

        Map<String, Object> kpi = dashboardBiz.getDashboardKpi(null, null, CTX);
        assertEquals(3, kpi.get("totalTickets"), "3 张已关闭工单");
        assertEquals(2, kpi.get("slaCompletedCount"), "2 张 SLA 达标");
        assertEquals(1, kpi.get("slaBreachedCount"), "1 张 SLA 超时");
        BigDecimal rate = (BigDecimal) kpi.get("slaCompletionRate");
        assertNotNull(rate, "SLA 达标率不应为 null");
        assertEquals(0, rate.compareTo(new BigDecimal("0.6667")), "2/3 ≈ 0.6667");
    }

    @Test
    public void testTeamSlaRanking() {
        LocalDate today = CoreMetrics.today();
        ormTemplate.runInSession(() -> {
            Long teamId = seedTeam(9601L, "技术支持组");
            Long slaPolicyId = seedSlaPolicy(9602L, teamId);
            seedClosedTicketWithSla(9510L, "TK-TEAM-1", true, 60, today, slaPolicyId);
            seedClosedTicketWithSla(9511L, "TK-TEAM-2", false, 30, today, slaPolicyId);
        });

        List<Map<String, Object>> rows = dashboardBiz.getTeamSlaRanking(null, null, CTX);
        assertFalse(rows.isEmpty(), "团队排名应返回非空");
        boolean found = false;
        for (Map<String, Object> r : rows) {
            if ("技术支持组".equals(r.get("teamName"))) {
                found = true;
                assertEquals(2, r.get("totalTickets"), "技术支持组 2 张工单");
                assertEquals(1, r.get("slaCompletedCount"), "1 张 SLA 达标");
            }
        }
        assertTrue(found, "应包含技术支持组");
    }

    @Test
    public void testAgentCsatBreakdown() {
        LocalDate today = CoreMetrics.today();
        ormTemplate.runInSession(() -> {
            Long ticketId = seedClosedTicketAssigned(9520L, "TK-CSAT-1", true, 60, today, "agent-001");
            seedSurvey(9701L, ticketId, 5, 9, 4);
        });

        List<Map<String, Object>> rows = dashboardBiz.getAgentCsatBreakdown(null, null, CTX);
        assertFalse(rows.isEmpty(), "客服 CSAT 明细应返回非空");
        boolean found = false;
        for (Map<String, Object> r : rows) {
            if ("agent-001".equals(r.get("agentId"))) {
                found = true;
                assertEquals(1, r.get("surveyCount"), "1 条调查回复");
                BigDecimal avgCsat = (BigDecimal) r.get("avgCsat");
                assertEquals(0, avgCsat.compareTo(new BigDecimal("5.00")), "avgCsat=5");
            }
        }
        assertTrue(found, "应包含 agent-001");
    }

    // ---------- helpers ----------

    private void seedClosedTicket(Long id, String code, boolean slaCompleted, int durationMin, LocalDate createDate) {
        seedCustomerIfNeeded();
        IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
        ErpCsTicket t = new ErpCsTicket();
        t.orm_propValueByName("id", id);
        t.setCode(code);
        t.setSubject("工单-" + code);
        t.setCustomerId(PARTNER_ID);
        t.setTicketTypeId(TICKET_TYPE_ID);
        t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
        t.setStatus(ErpCsConstants.TICKET_STATUS_CLOSED);
        t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
        t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_APPROVED);
        t.setIsSlaCompleted(slaCompleted);
        t.setDuration(durationMin);
        t.setBusinessDate(createDate);
        t.orm_propValueByName("createTime", java.sql.Timestamp.valueOf(createDate.atStartOfDay()));
        dao.saveEntity(t);
    }

    private void seedClosedTicketWithSla(Long id, String code, boolean slaCompleted, int durationMin,
                                          LocalDate createDate, Long slaPolicyId) {
        seedCustomerIfNeeded();
        IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
        ErpCsTicket t = new ErpCsTicket();
        t.orm_propValueByName("id", id);
        t.setCode(code);
        t.setSubject("工单-" + code);
        t.setCustomerId(PARTNER_ID);
        t.setTicketTypeId(TICKET_TYPE_ID);
        t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
        t.setStatus(ErpCsConstants.TICKET_STATUS_CLOSED);
        t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
        t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_APPROVED);
        t.setIsSlaCompleted(slaCompleted);
        t.setDuration(durationMin);
        t.setSlaPolicyId(slaPolicyId);
        t.setBusinessDate(createDate);
        t.orm_propValueByName("createTime", java.sql.Timestamp.valueOf(createDate.atStartOfDay()));
        dao.saveEntity(t);
    }

    private Long seedClosedTicketAssigned(Long id, String code, boolean slaCompleted, int durationMin,
                                           LocalDate createDate, String assignedToId) {
        seedCustomerIfNeeded();
        IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
        ErpCsTicket t = new ErpCsTicket();
        t.orm_propValueByName("id", id);
        t.setCode(code);
        t.setSubject("工单-" + code);
        t.setCustomerId(PARTNER_ID);
        t.setTicketTypeId(TICKET_TYPE_ID);
        t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
        t.setStatus(ErpCsConstants.TICKET_STATUS_CLOSED);
        t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
        t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_APPROVED);
        t.setIsSlaCompleted(slaCompleted);
        t.setDuration(durationMin);
        t.setAssignedToId(assignedToId);
        t.setBusinessDate(createDate);
        t.orm_propValueByName("createTime", java.sql.Timestamp.valueOf(createDate.atStartOfDay()));
        dao.saveEntity(t);
        return id;
    }

    private boolean customerSeeded = false;

    private void seedCustomerIfNeeded() {
        if (customerSeeded) return;
        IEntityDao<app.erp.md.dao.entity.ErpMdPartner> dao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdPartner.class);
        app.erp.md.dao.entity.ErpMdPartner p = new app.erp.md.dao.entity.ErpMdPartner();
        p.orm_propValueByName("id", PARTNER_ID);
        p.setCode("CUS-" + PARTNER_ID);
        p.setName("测试客户");
        p.orm_propValueByName("partnerType", "CUSTOMER");
        p.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(p);
        customerSeeded = true;
    }

    private Long seedTeam(Long id, String name) {
        IEntityDao<ErpCsTeam> dao = daoProvider.daoFor(ErpCsTeam.class);
        ErpCsTeam team = new ErpCsTeam();
        team.orm_propValueByName("id", id);
        team.setCode("TEAM-" + id);
        team.setName(name);
        dao.saveEntity(team);
        return id;
    }

    private Long seedSlaPolicy(Long id, Long teamId) {
        IEntityDao<ErpCsSlaPolicy> dao = daoProvider.daoFor(ErpCsSlaPolicy.class);
        ErpCsSlaPolicy p = new ErpCsSlaPolicy();
        p.orm_propValueByName("id", id);
        p.setCode("SLA-" + id);
        p.setName("测试SLA");
        p.setTeamId(teamId);
        p.setResolveHours(48);
        p.setIsWorkingDays(false);
        dao.saveEntity(p);
        return id;
    }

    private void seedSurvey(Long id, Long ticketId, Integer csat, Integer nps, Integer ces) {
        IEntityDao<ErpCsSurvey> dao = daoProvider.daoFor(ErpCsSurvey.class);
        ErpCsSurvey s = new ErpCsSurvey();
        s.orm_propValueByName("id", id);
        s.setTicketId(ticketId);
        s.setCsatScore(csat);
        s.setNpsScore(nps);
        s.setCesScore(ces);
        dao.saveEntity(s);
    }
}
