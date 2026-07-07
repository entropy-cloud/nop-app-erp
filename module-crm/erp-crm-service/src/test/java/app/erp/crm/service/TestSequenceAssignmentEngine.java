package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmSequenceAssignment;
import app.erp.crm.service.support.SequenceAssignmentEngine;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 销售序列分配引擎单元测试（plan 2026-07-07-1430-3 §Phase 3）。
 *
 * <p>纯函数式：无 DB 依赖、无 IoC、无 XLang，使用 {@link BaseTestCase} 仅作帮助类基类。
 *
 * <p>覆盖：四 conditionType 匹配（LEAD_SOURCE/TERRITORY/PRODUCT_LINE/CUSTOM_FIELD）+ priority 平局（数值小者优先）+
 * default 兜底 + 无匹配不分配。
 */
public class TestSequenceAssignmentEngine extends BaseTestCase {

    private final SequenceAssignmentEngine engine = new SequenceAssignmentEngine();

    private static final Long SEQ_A = 7001L;
    private static final Long SEQ_B = 7002L;
    private static final Long SEQ_DEFAULT = 7003L;

    @Test
    public void testLeadSourceMatch() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "LEAD_SOURCE", "{\"sourceId\":[101,102]}", 10);
        ErpCrmLead lead = newLead(2001L);
        lead.setSourceId(102L);
        SequenceAssignmentEngine.AssignmentResult result = engine.assign(lead, Collections.singletonList(r), null);
        assertNotNull(result, "sourceId=102 命中 LEAD_SOURCE 规则");
        assertEquals(SEQ_A, result.getSequenceId());
        assertFalse(result.isFromDefault(), "命中具体规则，非 default");
    }

    @Test
    public void testLeadSourceSingleValue() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "LEAD_SOURCE", "{\"sourceId\":101}", 10);
        ErpCrmLead lead = newLead(2001L);
        lead.setSourceId(101L);
        assertEquals(SEQ_A, engine.assign(lead, Collections.singletonList(r), null).getSequenceId(),
                "sourceId=101 单值匹配");

        ErpCrmLead lead2 = newLead(2002L);
        lead2.setSourceId(999L);
        assertNull(engine.assign(lead2, Collections.singletonList(r), null),
                "sourceId=999 不在列表 → 不命中");
    }

    @Test
    public void testTerritoryMatch() {
        ErpCrmSequenceAssignment r = newRule(SEQ_B, "TERRITORY", "{\"territoryId\":[201,202]}", 5);
        ErpCrmLead lead = newLead(2003L);
        lead.setTerritoryId(202L);
        assertEquals(SEQ_B, engine.assign(lead, Collections.singletonList(r), null).getSequenceId());

        ErpCrmLead lead2 = newLead(2004L);
        lead2.setTerritoryId(999L);
        assertNull(engine.assign(lead2, Collections.singletonList(r), null));
    }

    @Test
    public void testProductLineMatch() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "PRODUCT_LINE", "{\"productLine\":[\"electronics\",\"toys\"]}", 10);
        ErpCrmLead lead = newLead(2005L);
        lead.setDepartment("electronics division");
        assertEquals(SEQ_A, engine.assign(lead, Collections.singletonList(r), null).getSequenceId(),
                "department 含 electronics 命中");

        ErpCrmLead lead2 = newLead(2006L);
        lead2.setUtmSource("toys-promo");
        assertEquals(SEQ_A, engine.assign(lead2, Collections.singletonList(r), null).getSequenceId(),
                "utmSource 含 toys 命中");

        ErpCrmLead lead3 = newLead(2007L);
        lead3.setDepartment("furniture");
        lead3.setUtmSource("furniture");
        assertNull(engine.assign(lead3, Collections.singletonList(r), null),
                "department/utmSource 均不匹配 → 不命中");
    }

    @Test
    public void testCustomFieldMatchRequiresOrm() {
        // CUSTOM_FIELD 路径经 lead.prop_get（依赖 ORM 元数据初始化）。
        // 纯单元测试（BaseTestCase，无 CoreInitialization）下 prop_get 抛 OrmException → 引擎按"无匹配"返回 null。
        // CUSTOM_FIELD 实际匹配由 TestErpCrmSequenceAndFunnel 集成测试覆盖（对齐 TerritoryAssignmentEngine 测试策略）。
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "CUSTOM_FIELD", "{\"utmSource\":\"baidu\"}", 10);
        ErpCrmLead lead = newLead(2008L);
        lead.setUtmSource("baidu");
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "纯单测无 ORM 初始化 → prop_get 失败被吞 → CUSTOM_FIELD 无匹配 → null（集成测试覆盖实际匹配）");
    }

    @Test
    public void testPriorityLowerWins() {
        ErpCrmSequenceAssignment r1 = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 5);
        ErpCrmSequenceAssignment r2 = newRule(SEQ_B, "TERRITORY", "{\"territoryId\":[201]}", 1);
        ErpCrmLead lead = newLead(2009L);
        lead.setTerritoryId(201L);
        // priority=1（r2）应优先于 priority=5（r1）
        assertEquals(SEQ_B, engine.assign(lead, Arrays.asList(r1, r2), null).getSequenceId(),
                "priority 数值小者优先");
    }

    @Test
    public void testDefaultFallback() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        ErpCrmSequenceAssignment defaultRule = newDefaultRule(SEQ_DEFAULT);
        ErpCrmLead lead = newLead(2010L);
        lead.setTerritoryId(999L); // 不匹配具体规则
        SequenceAssignmentEngine.AssignmentResult result = engine.assign(lead, Collections.singletonList(r), defaultRule);
        assertNotNull(result, "无具体命中 → 走 default");
        assertEquals(SEQ_DEFAULT, result.getSequenceId());
        assertTrue(result.isFromDefault(), "fromDefault=true");
    }

    @Test
    public void testNoMatchNoDefaultReturnsNull() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        ErpCrmLead lead = newLead(2011L);
        lead.setTerritoryId(999L);
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "无匹配且无 default → 返回 null（不分配）");
    }

    @Test
    public void testInactiveRuleSkipped() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        r.setIsActive(Boolean.FALSE);
        ErpCrmLead lead = newLead(2012L);
        lead.setTerritoryId(201L);
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "isActive=false 规则被跳过");
    }

    @Test
    public void testEmptyConditionValueSkipped() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "", 10);
        ErpCrmLead lead = newLead(2013L);
        lead.setTerritoryId(201L);
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "conditionValue 为空 → 不匹配");
    }

    @Test
    public void testSpecificRulePreferredOverDefault() {
        // 同时存在命中具体规则 + default：具体规则优先
        ErpCrmSequenceAssignment specific = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        ErpCrmSequenceAssignment defaultRule = newDefaultRule(SEQ_DEFAULT);
        ErpCrmLead lead = newLead(2014L);
        lead.setTerritoryId(201L);
        SequenceAssignmentEngine.AssignmentResult result = engine.assign(lead, Collections.singletonList(specific), defaultRule);
        assertEquals(SEQ_A, result.getSequenceId(), "具体命中优先于 default");
        assertFalse(result.isFromDefault());
    }

    // ---------- helpers ----------

    private ErpCrmLead newLead(Long id) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setOrgId(1301L);
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        return lead;
    }

    private ErpCrmSequenceAssignment newRule(Long seqId, String conditionType, String conditionValue, int priority) {
        ErpCrmSequenceAssignment r = new ErpCrmSequenceAssignment();
        r.setId(System.nanoTime());
        r.setSequenceId(seqId);
        r.setConditionType(conditionType);
        r.setConditionValue(conditionValue);
        r.setPriority(priority);
        r.setIsActive(Boolean.TRUE);
        r.setIsDefault(Boolean.FALSE);
        return r;
    }

    private ErpCrmSequenceAssignment newDefaultRule(Long seqId) {
        ErpCrmSequenceAssignment r = new ErpCrmSequenceAssignment();
        r.setId(System.nanoTime());
        r.setSequenceId(seqId);
        r.setIsActive(Boolean.TRUE);
        r.setIsDefault(Boolean.TRUE);
        r.setPriority(Integer.MAX_VALUE);
        return r;
    }
}
