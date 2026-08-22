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

    private static final String SEQ_A = "7001";
    private static final String SEQ_B = "7002";
    private static final String SEQ_DEFAULT = "7003";

    @Test
    public void testLeadSourceMatch() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "LEAD_SOURCE", "{\"sourceId\":[101,102]}", 10);
        ErpCrmLead lead = newLead("2001");
        lead.setSourceId("102");
        SequenceAssignmentEngine.AssignmentResult result = engine.assign(lead, Collections.singletonList(r), null);
        assertNotNull(result, "sourceId=102 命中 LEAD_SOURCE 规则");
        assertEquals(SEQ_A, result.getSequenceId());
        assertFalse(result.isFromDefault(), "命中具体规则，非 default");
    }

    @Test
    public void testLeadSourceSingleValue() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "LEAD_SOURCE", "{\"sourceId\":101}", 10);
        ErpCrmLead lead = newLead("2001");
        lead.setSourceId("101");
        assertEquals(SEQ_A, engine.assign(lead, Collections.singletonList(r), null).getSequenceId(),
                "sourceId=101 单值匹配");

        ErpCrmLead lead2 = newLead("2002");
        lead2.setSourceId("999");
        assertNull(engine.assign(lead2, Collections.singletonList(r), null),
                "sourceId=999 不在列表 → 不命中");
    }

    @Test
    public void testTerritoryMatch() {
        ErpCrmSequenceAssignment r = newRule(SEQ_B, "TERRITORY", "{\"territoryId\":[201,202]}", 5);
        ErpCrmLead lead = newLead("2003");
        lead.setTerritoryId("202");
        assertEquals(SEQ_B, engine.assign(lead, Collections.singletonList(r), null).getSequenceId());

        ErpCrmLead lead2 = newLead("2004");
        lead2.setTerritoryId("999");
        assertNull(engine.assign(lead2, Collections.singletonList(r), null));
    }

    @Test
    public void testProductLineMatch() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "PRODUCT_LINE", "{\"productLine\":[\"electronics\",\"toys\"]}", 10);
        ErpCrmLead lead = newLead("2005");
        lead.setDepartment("electronics division");
        assertEquals(SEQ_A, engine.assign(lead, Collections.singletonList(r), null).getSequenceId(),
                "department 含 electronics 命中");

        ErpCrmLead lead2 = newLead("2006");
        lead2.setUtmSource("toys-promo");
        assertEquals(SEQ_A, engine.assign(lead2, Collections.singletonList(r), null).getSequenceId(),
                "utmSource 含 toys 命中");

        ErpCrmLead lead3 = newLead("2007");
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
        ErpCrmLead lead = newLead("2008");
        lead.setUtmSource("baidu");
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "纯单测无 ORM 初始化 → prop_get 失败被吞 → CUSTOM_FIELD 无匹配 → null（集成测试覆盖实际匹配）");
    }

    @Test
    public void testPriorityLowerWins() {
        ErpCrmSequenceAssignment r1 = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 5);
        ErpCrmSequenceAssignment r2 = newRule(SEQ_B, "TERRITORY", "{\"territoryId\":[201]}", 1);
        ErpCrmLead lead = newLead("2009");
        lead.setTerritoryId("201");
        // priority=1（r2）应优先于 priority=5（r1）
        assertEquals(SEQ_B, engine.assign(lead, Arrays.asList(r1, r2), null).getSequenceId(),
                "priority 数值小者优先");
    }

    @Test
    public void testDefaultFallback() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        ErpCrmSequenceAssignment defaultRule = newDefaultRule(SEQ_DEFAULT);
        ErpCrmLead lead = newLead("2010");
        lead.setTerritoryId("999"); // 不匹配具体规则
        SequenceAssignmentEngine.AssignmentResult result = engine.assign(lead, Collections.singletonList(r), defaultRule);
        assertNotNull(result, "无具体命中 → 走 default");
        assertEquals(SEQ_DEFAULT, result.getSequenceId());
        assertTrue(result.isFromDefault(), "fromDefault=true");
    }

    @Test
    public void testNoMatchNoDefaultReturnsNull() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        ErpCrmLead lead = newLead("2011");
        lead.setTerritoryId("999");
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "无匹配且无 default → 返回 null（不分配）");
    }

    @Test
    public void testInactiveRuleSkipped() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        r.setIsActive(Boolean.FALSE);
        ErpCrmLead lead = newLead("2012");
        lead.setTerritoryId("201");
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "isActive=false 规则被跳过");
    }

    @Test
    public void testEmptyConditionValueSkipped() {
        ErpCrmSequenceAssignment r = newRule(SEQ_A, "TERRITORY", "", 10);
        ErpCrmLead lead = newLead("2013");
        lead.setTerritoryId("201");
        assertNull(engine.assign(lead, Collections.singletonList(r), null),
                "conditionValue 为空 → 不匹配");
    }

    @Test
    public void testSpecificRulePreferredOverDefault() {
        // 同时存在命中具体规则 + default：具体规则优先
        ErpCrmSequenceAssignment specific = newRule(SEQ_A, "TERRITORY", "{\"territoryId\":[201]}", 10);
        ErpCrmSequenceAssignment defaultRule = newDefaultRule(SEQ_DEFAULT);
        ErpCrmLead lead = newLead("2014");
        lead.setTerritoryId("201");
        SequenceAssignmentEngine.AssignmentResult result = engine.assign(lead, Collections.singletonList(specific), defaultRule);
        assertEquals(SEQ_A, result.getSequenceId(), "具体命中优先于 default");
        assertFalse(result.isFromDefault());
    }

    // ---------- helpers ----------

    private ErpCrmLead newLead(String id) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setOrgId("1301");
        lead.setLeadType(ErpCrmConstants.LEAD_TYPE_LEAD);
        lead.setDocStatus(ErpCrmConstants.DOC_STATUS_NEW);
        return lead;
    }

    private ErpCrmSequenceAssignment newRule(String seqId, String conditionType, String conditionValue, int priority) {
        ErpCrmSequenceAssignment r = new ErpCrmSequenceAssignment();
        r.setId(String.valueOf(System.nanoTime()));
        r.setSequenceId(seqId);
        r.setConditionType(conditionType);
        r.setConditionValue(conditionValue);
        r.setPriority(priority);
        r.setIsActive(Boolean.TRUE);
        r.setIsDefault(Boolean.FALSE);
        return r;
    }

    private ErpCrmSequenceAssignment newDefaultRule(String seqId) {
        ErpCrmSequenceAssignment r = new ErpCrmSequenceAssignment();
        r.setId(String.valueOf(System.nanoTime()));
        r.setSequenceId(seqId);
        r.setIsActive(Boolean.TRUE);
        r.setIsDefault(Boolean.TRUE);
        r.setPriority(Integer.MAX_VALUE);
        return r;
    }
}
