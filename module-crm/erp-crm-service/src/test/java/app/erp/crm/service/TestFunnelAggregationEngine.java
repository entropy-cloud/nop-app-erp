package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmLostReason;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.support.FunnelAggregationEngine;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售漏斗聚合引擎单元测试（plan 2026-07-07-1430-3 §Phase 3）。
 *
 * <p>纯函数式：无 DB 依赖、无 IoC、无 XLang，使用 {@link BaseTestCase} 仅作帮助类基类。
 *
 * <p>覆盖：头度量 + 阶段明细（进入/流出/转化率/流失率/停留天数）+ 丢失原因 TOP N + stageName 快照 + 空数据零值 + 非法期间拒绝。
 */
public class TestFunnelAggregationEngine extends BaseTestCase {

    private final FunnelAggregationEngine engine = new FunnelAggregationEngine();

    private static final Long STAGE_NEW = 3001L;
    private static final Long STAGE_QUALIFIED = 3002L;
    private static final Long STAGE_WON = 3003L;
    private static final Long REASON_PRICE = 4001L;
    private static final Long REASON_COMPETITOR = 4002L;

    @Test
    public void testEmptyDataReturnsZeroStructure() {
        FunnelAggregationEngine.FunnelSnapshot snapshot = engine.aggregate(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, null, null,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyMap(), 5);

        assertEquals(0, snapshot.getTotalLeadsAtTop());
        assertEquals(0, snapshot.getTotalOpportunities());
        assertEquals(0, snapshot.getTotalWon());
        assertEquals(0, snapshot.getTotalLost());
        assertEquals(0, snapshot.getTotalRevenue().compareTo(BigDecimal.ZERO));
        assertEquals(0, snapshot.getLostRevenue().compareTo(BigDecimal.ZERO));
        assertEquals(0.0, snapshot.getAvgSalesCycleDays());
        assertTrue(snapshot.getStageMetrics().isEmpty(), "无 stages → 空明细列表");
        assertNotNull(snapshot.getCalculatedAt());
    }

    @Test
    public void testHeaderMetricsWonLostRevenue() {
        // 3 leads：1 won（CONVERTED, revenue=1000），1 lost（LOST, revenue=500），1 active opp（QUALIFIED, prob=50, revenue=2000）
        ErpCrmLead won = newLead(1001L, "OPP-WON", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_CONVERTED, new BigDecimal("1000"), 90, REASON_PRICE);
        ErpCrmLead lost = newLead(1002L, "OPP-LOST", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_LOST, new BigDecimal("500"), 0, REASON_COMPETITOR);
        ErpCrmLead active = newLead(1003L, "OPP-ACTIVE", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_QUALIFIED, new BigDecimal("2000"), 50, null);

        List<ErpCrmStage> stages = Arrays.asList(
                newStage(STAGE_NEW, "新线索", 10),
                newStage(STAGE_QUALIFIED, "已验证", 20),
                newStage(STAGE_WON, "赢单", 30));

        FunnelAggregationEngine.FunnelSnapshot snapshot = engine.aggregate(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, null, null,
                Collections.emptyList(), Arrays.asList(won, lost, active),
                stages, lostReasonMap(), 5);

        assertEquals(3, snapshot.getTotalLeadsAtTop(), "无 ConvLog → 退化为 leads.size()");
        assertEquals(3, snapshot.getTotalOpportunities());
        assertEquals(1, snapshot.getTotalWon());
        assertEquals(1, snapshot.getTotalLost());
        assertEquals(0, snapshot.getTotalRevenue().compareTo(new BigDecimal("1000")),
                "赢单 expectedRevenue=1000");
        assertEquals(0, snapshot.getLostRevenue().compareTo(new BigDecimal("500")),
                "丢单 expectedRevenue=500");
        // 加权：active opp = 2000 × 50 / 100 = 1000
        assertEquals(0, snapshot.getWeightedRevenue().compareTo(new BigDecimal("1000.00")));
        // avgDealSize = 1000 / 1 = 1000
        assertEquals(0, snapshot.getAvgDealSize().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    public void testStageMetricsConversionAndDropOff() {
        // 4 leads 流转：3 进入 NEW → 2 进入 QUALIFIED → 1 进入 WON；1 在 QUALIFIED 丢失
        LocalDateTime t0 = LocalDateTime.of(2026, 7, 1, 9, 0);
        ErpCrmLead l1 = newLead(2001L, "L1", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_CONVERTED, new BigDecimal("1000"), 90, null);
        ErpCrmLead l2 = newLead(2002L, "L2", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_QUALIFIED, new BigDecimal("800"), 50, null);
        ErpCrmLead l3 = newLead(2003L, "L3", ErpCrmConstants.LEAD_TYPE_LEAD,
                ErpCrmConstants.DOC_STATUS_NEW, null, 0, null);
        ErpCrmLead l4 = newLead(2004L, "L4", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_LOST, new BigDecimal("600"), 0, REASON_PRICE);

        List<ErpCrmLeadConvLog> logs = Arrays.asList(
                // L1: NEW → QUALIFIED → WON
                newLog(5001L, 2001L, null, STAGE_NEW, t0),
                newLog(5002L, 2001L, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(2)),
                newLog(5003L, 2001L, STAGE_QUALIFIED, STAGE_WON, t0.plusDays(7)),
                // L2: NEW → QUALIFIED
                newLog(5004L, 2002L, null, STAGE_NEW, t0),
                newLog(5005L, 2002L, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(3)),
                // L3: NEW
                newLog(5006L, 2003L, null, STAGE_NEW, t0),
                // L4: NEW → QUALIFIED（在 QUALIFIED 丢失）
                newLog(5007L, 2004L, null, STAGE_NEW, t0),
                newLog(5008L, 2004L, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(1)));

        List<ErpCrmStage> stages = Arrays.asList(
                newStage(STAGE_NEW, "新线索", 10),
                newStage(STAGE_QUALIFIED, "已验证", 20),
                newStage(STAGE_WON, "赢单", 30));

        FunnelAggregationEngine.FunnelSnapshot snapshot = engine.aggregate(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, null, null,
                logs, Arrays.asList(l1, l2, l3, l4),
                stages, lostReasonMap(), 5);

        List<ErpCrmFunnelStageMetrics> metrics = snapshot.getStageMetrics();
        assertEquals(3, metrics.size(), "3 阶段 → 3 行明细");

        // NEW 阶段：进入 4（L1/L2/L3/L4），流出 3（L1/L2/L4 fromStageId=NEW），丢失 0
        ErpCrmFunnelStageMetrics newM = findByStageId(metrics, STAGE_NEW);
        assertEquals("新线索", newM.getStageName(), "stageName 快照");
        assertEquals(10, newM.getStageOrder());
        assertEquals(4, newM.getLeadCountIn(), "4 leads 进入 NEW");
        assertEquals(3, newM.getLeadCountOut(), "3 leads 流出 NEW（L1, L2, L4）");
        assertEquals(0, newM.getLostCount(), "NEW 阶段无丢失");
        assertEquals(1, newM.getLeadCountRemaining(), "4 - 3 - 0 = 1（仅 L3 留在 NEW）");

        // QUALIFIED 阶段：进入 3（L1, L2, L4 toStageId=QUALIFIED），流出 1（L1 fromStageId=QUALIFIED），丢失 1（L4）
        ErpCrmFunnelStageMetrics qualM = findByStageId(metrics, STAGE_QUALIFIED);
        assertEquals(3, qualM.getLeadCountIn(), "3 leads 进入 QUALIFIED");
        assertEquals(1, qualM.getLeadCountOut(), "1 lead 流出 QUALIFIED（L1）");
        assertEquals(1, qualM.getLostCount(), "L4 在 QUALIFIED 丢失");
        // 转化率 = 进入 WON 的人数 / QUALIFIED 进入人数 = 1 / 3
        assertEquals(0.3333, qualM.getConversionRate(), 0.001, "nextStageIn=1 / leadCountIn=3");
        // 流失率 = lostCount / leadCountIn = 1 / 3
        assertEquals(0.3333, qualM.getDropOffRate(), 0.001);

        // 丢失原因 TOP N：QUALIFIED 阶段有 1 条丢失（REASON_PRICE）
        assertNotNull(qualM.getLostReasonTop(), "丢失原因 JSON 已生成");
        assertTrue(qualM.getLostReasonTop().contains("价格太高"), "TOP N JSON 含原因名称");

        // WON 阶段：进入 1（L1），无流出，无丢失
        ErpCrmFunnelStageMetrics wonM = findByStageId(metrics, STAGE_WON);
        assertEquals(1, wonM.getLeadCountIn());
        assertEquals(0, wonM.getConversionRate(), 0.0001, "无下一阶段 → 转化率 0");
    }

    @Test
    public void testAvgDaysInStage() {
        // L1 在 NEW 阶段停留 2 天（t0 → t0+2d）
        LocalDateTime t0 = LocalDateTime.of(2026, 7, 1, 9, 0);
        ErpCrmLead l1 = newLead(3001L, "L1", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_QUALIFIED, new BigDecimal("500"), 50, null);
        List<ErpCrmLeadConvLog> logs = Arrays.asList(
                newLog(6001L, 3001L, null, STAGE_NEW, t0),
                newLog(6002L, 3001L, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(2)));
        List<ErpCrmStage> stages = Arrays.asList(
                newStage(STAGE_NEW, "新线索", 10),
                newStage(STAGE_QUALIFIED, "已验证", 20));

        FunnelAggregationEngine.FunnelSnapshot snapshot = engine.aggregate(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, null, null,
                logs, Collections.singletonList(l1), stages, lostReasonMap(), 5);

        ErpCrmFunnelStageMetrics newM = findByStageId(snapshot.getStageMetrics(), STAGE_NEW);
        assertEquals(2.0, newM.getAvgDaysInStage(), 0.01, "L1 在 NEW 停留 2 天");
    }

    @Test
    public void testLostReasonTopNLimited() {
        // 3 个不同丢失原因（各 1 次），TOP N=2 → 仅返回 2 条
        ErpCrmLead l1 = newLead(4001L, "L1", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_LOST, new BigDecimal("100"), 0, REASON_PRICE);
        ErpCrmLead l2 = newLead(4002L, "L2", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_LOST, new BigDecimal("100"), 0, REASON_COMPETITOR);
        LocalDateTime t0 = LocalDateTime.of(2026, 7, 1, 9, 0);
        List<ErpCrmLeadConvLog> logs = Arrays.asList(
                newLog(7001L, 4001L, null, STAGE_NEW, t0),
                newLog(7002L, 4002L, null, STAGE_NEW, t0));
        List<ErpCrmStage> stages = Collections.singletonList(newStage(STAGE_NEW, "新线索", 10));

        FunnelAggregationEngine.FunnelSnapshot snapshot = engine.aggregate(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, null, null,
                logs, Arrays.asList(l1, l2), stages, lostReasonMap(), 2);

        ErpCrmFunnelStageMetrics newM = findByStageId(snapshot.getStageMetrics(), STAGE_NEW);
        // 2 条丢失，TOP N=2，均出现
        assertEquals(2, newM.getLostCount());
        assertNotNull(newM.getLostReasonTop());
        assertTrue(newM.getLostReasonTop().contains("价格太高"));
        assertTrue(newM.getLostReasonTop().contains("竞争对手"));
    }

    @Test
    public void testNoLostReasonsReturnsNullTop() {
        ErpCrmLead l1 = newLead(5001L, "L1", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                ErpCrmConstants.DOC_STATUS_QUALIFIED, new BigDecimal("500"), 50, null);
        List<ErpCrmStage> stages = Collections.singletonList(newStage(STAGE_NEW, "新线索", 10));
        FunnelAggregationEngine.FunnelSnapshot snapshot = engine.aggregate(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, null, null,
                Collections.emptyList(), Collections.singletonList(l1), stages, lostReasonMap(), 5);
        ErpCrmFunnelStageMetrics newM = findByStageId(snapshot.getStageMetrics(), STAGE_NEW);
        assertEquals(0, newM.getLostCount());
        assertNull(newM.getLostReasonTop(), "无丢失 → lostReasonTop=null");
    }

    @Test
    public void testInvalidPeriodRejects() {
        assertThrows(io.nop.api.core.exceptions.NopException.class, () -> engine.aggregate(
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1),
                null, null, null,
                Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyMap(), 5),
                "periodStart 晚于 periodEnd → ERR_FUNNEL_PERIOD_INVALID");
    }

    @Test
    public void testStageNameSnapshotPreserved() {
        // stageName 取聚合时刻的 stage 定义，后续 stage 改名不影响已生成快照
        List<ErpCrmStage> stages = Collections.singletonList(newStage(STAGE_NEW, "原始阶段名", 10));
        FunnelAggregationEngine.FunnelSnapshot snapshot = engine.aggregate(
                LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31),
                null, null, null,
                Collections.emptyList(), Collections.emptyList(), stages, lostReasonMap(), 5);
        assertEquals("原始阶段名", snapshot.getStageMetrics().get(0).getStageName());
    }

    // ---------- helpers ----------

    private ErpCrmFunnelStageMetrics findByStageId(List<ErpCrmFunnelStageMetrics> list, Long stageId) {
        return list.stream().filter(m -> stageId.equals(m.getStageId())).findFirst().orElseThrow();
    }

    private ErpCrmLead newLead(Long id, String code, String leadType, String docStatus,
                                BigDecimal revenue, int probability, Long lostReasonId) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(1301L);
        lead.setLeadType(leadType);
        lead.setDocStatus(docStatus);
        lead.setExpectedRevenue(revenue);
        lead.setProbability(probability);
        lead.setLostReasonId(lostReasonId);
        return lead;
    }

    private ErpCrmStage newStage(Long id, String name, int sequence) {
        ErpCrmStage stage = new ErpCrmStage();
        stage.setId(id);
        stage.setStageName(name);
        stage.setSequence(sequence);
        return stage;
    }

    private ErpCrmLeadConvLog newLog(Long id, Long leadId, Long fromStageId, Long toStageId, LocalDateTime changedAt) {
        ErpCrmLeadConvLog log = new ErpCrmLeadConvLog();
        log.setId(id);
        log.setLeadId(leadId);
        log.setFromStageId(fromStageId);
        log.setToStageId(toStageId);
        log.setChangedAt(Timestamp.valueOf(changedAt));
        return log;
    }

    private Map<Long, ErpCrmLostReason> lostReasonMap() {
        Map<Long, ErpCrmLostReason> map = new HashMap<>();
        ErpCrmLostReason r1 = new ErpCrmLostReason();
        r1.setId(REASON_PRICE);
        r1.setName("价格太高");
        ErpCrmLostReason r2 = new ErpCrmLostReason();
        r2.setId(REASON_COMPETITOR);
        r2.setName("竞争对手");
        map.put(REASON_PRICE, r1);
        map.put(REASON_COMPETITOR, r2);
        return map;
    }
}
