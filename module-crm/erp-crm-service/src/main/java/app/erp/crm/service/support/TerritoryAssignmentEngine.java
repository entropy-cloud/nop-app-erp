package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.core.lang.json.JsonTool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 区域分配引擎。按 priority 遍历 {@code isActive=true} 规则，{@link ConditionMatcher} 按 conditionType 解析
 * conditionValue JSON 匹配 lead 字段；命中返回 territoryId/teamId/assignmentMethod，未命中走 isDefault 兜底。
 *
 * <p>对齐 {@code docs/design/crm/territory.md §业务规则 2 线索自动分配 / §实现注记 2 团队成员模型缺失 → MANUAL 降级}：
 * 本期不实现 ROUND_ROBIN/LOAD_BALANCED 挑人逻辑；遇到非 MANUAL 方法按 MANUAL 语义降级（仅回写 territory/team）。
 *
 * <p>纯函数式 + 注入加载函数便于单测：{@link #assign(ErpCrmLead, List, ErpCrmTerritoryAssignmentRule)}
 * 不依赖 IoC，可独立构造 rules 与 lead 测试四类 conditionType 匹配。
 */
public class TerritoryAssignmentEngine {

    private final ConditionMatcher conditionMatcher;

    public TerritoryAssignmentEngine() {
        this(new ConditionMatcher());
    }

    public TerritoryAssignmentEngine(ConditionMatcher conditionMatcher) {
        this.conditionMatcher = conditionMatcher;
    }

    /**
     * 按 priority 遍历 rules 找首个匹配的 active 规则；无匹配则用 defaultRule。
     *
     * @param lead        待分配线索
     * @param rules       active 规则列表（priority 升序，default 规则可单独传入）
     * @param defaultRule 兜底规则（可为 null）
     * @return 分配结果（命中规则返回 territoryId/teamId/method；全无匹配返回 null，调用方标记"未分配"）
     */
    public AssignmentResult assign(ErpCrmLead lead,
                                    List<ErpCrmTerritoryAssignmentRule> rules,
                                    ErpCrmTerritoryAssignmentRule defaultRule) {
        List<ErpCrmTerritoryAssignmentRule> sorted = new ArrayList<>();
        if (rules != null) {
            for (ErpCrmTerritoryAssignmentRule r : rules) {
                if (r != null && Boolean.TRUE.equals(r.getIsActive())
                        && !Boolean.TRUE.equals(r.getIsDefault())) {
                    sorted.add(r);
                }
            }
        }
        sorted.sort(Comparator
                .comparingInt((ErpCrmTerritoryAssignmentRule r) ->
                        r.getPriority() != null ? r.getPriority() : Integer.MAX_VALUE)
                .thenComparing(r -> r.getId() != null ? r.getId() : Long.MAX_VALUE));

        for (ErpCrmTerritoryAssignmentRule rule : sorted) {
            if (conditionMatcher.matches(rule, lead)) {
                return toResult(rule);
            }
        }
        if (defaultRule != null && Boolean.TRUE.equals(defaultRule.getIsActive())) {
            return toResult(defaultRule);
        }
        return null;
    }

    protected AssignmentResult toResult(ErpCrmTerritoryAssignmentRule rule) {
        AssignmentResult result = new AssignmentResult();
        result.setTerritoryId(rule.getTerritoryId());
        result.setTeamId(rule.getGroupId());
        result.setAssignmentMethod(rule.getAssignmentMethod());
        // 降级：非 MANUAL 方法按 MANUAL 语义处理（不挑 ownerId，留空标记待分配）
        if (!ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL.equals(rule.getAssignmentMethod())) {
            result.setAssignmentMethod(ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL);
            result.setDegraded(true);
        }
        return result;
    }

    // ---------- 匹配器 ----------

    public static class ConditionMatcher {

        public boolean matches(ErpCrmTerritoryAssignmentRule rule, ErpCrmLead lead) {
            String type = rule.getConditionType();
            Map<String, Object> value = parse(rule.getConditionValue());
            if (value == null || value.isEmpty()) {
                return false;
            }
            switch (type == null ? "" : type) {
                case ErpCrmConstants.ASSIGNMENT_CONDITION_GEOGRAPHY:
                    return matchGeography(value, lead);
                case ErpCrmConstants.ASSIGNMENT_CONDITION_INDUSTRY:
                    return matchIndustry(value, lead);
                case ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOMER_SIZE:
                    return matchCustomerSize(value, lead);
                case ErpCrmConstants.ASSIGNMENT_CONDITION_CUSTOM_FIELD:
                    return matchCustomField(value, lead);
                default:
                    return false;
            }
        }

        /**
         * GEOGRAPHY：conditionValue 形如 {@code {"province": ["上海","浙江"]}}，匹配 lead.companyName 中是否包含省/市关键词。
         * （Lead 当前无独立 province 字段，从 companyName 派生为最简降级，对齐设计 §业务规则 2 备注）
         */
        protected boolean matchGeography(Map<String, Object> value, ErpCrmLead lead) {
            Object provinces = value.get("province");
            String companyName = lead.getCompanyName();
            if (companyName == null || provinces == null) {
                return false;
            }
            for (Object p : asList(provinces)) {
                if (p != null && companyName.contains(String.valueOf(p))) {
                    return true;
                }
            }
            return false;
        }

        /**
         * INDUSTRY：conditionValue 形如 {@code {"industryCode": ["manufacturing","finance"]}}，
         * 匹配 lead.department 是否包含关键词（Lead 无独立 industryCode 字段，department 临时承载行业信息）。
         */
        protected boolean matchIndustry(Map<String, Object> value, ErpCrmLead lead) {
            Object codes = value.get("industryCode");
            String department = lead.getDepartment();
            if (department == null || codes == null) {
                return false;
            }
            for (Object c : asList(codes)) {
                if (c != null && department.contains(String.valueOf(c))) {
                    return true;
                }
            }
            return false;
        }

        /**
         * CUSTOMER_SIZE：conditionValue 形如 {@code {"minEmployees":100,"maxEmployees":5000}}，
         * 通过 expectedRevenue 临时映射（Lead 当前无 companySize 字段，用 expectedRevenue 范围作为规模代理字段）。
         */
        protected boolean matchCustomerSize(Map<String, Object> value, ErpCrmLead lead) {
            BigDecimal revenue = lead.getExpectedRevenue();
            if (revenue == null) {
                return false;
            }
            BigDecimal min = toBigDecimal(value.get("minEmployees"));
            BigDecimal max = toBigDecimal(value.get("maxEmployees"));
            if (min != null && revenue.compareTo(min) < 0) {
                return false;
            }
            if (max != null && revenue.compareTo(max) > 0) {
                return false;
            }
            return true;
        }

        /**
         * CUSTOM_FIELD：conditionValue 形如 {@code {"sourceId":"WECHAT_ADS","utmSource":"baidu"}}，
         * 任意 lead 字段值匹配（字符串相等）。
         */
        protected boolean matchCustomField(Map<String, Object> value, ErpCrmLead lead) {
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                Object expected = entry.getValue();
                if (expected == null) {
                    continue;
                }
                Object actual = lead.prop_get(entry.getKey());
                if (actual != null && String.valueOf(actual).equals(String.valueOf(expected))) {
                    return true;
                }
            }
            return false;
        }

        protected Map<String, Object> parse(String json) {
            if (json == null || json.isEmpty()) {
                return Collections.emptyMap();
            }
            Object parsed = JsonTool.parseNonStrict(json);
            if (parsed instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) parsed;
                return m;
            }
            return Collections.emptyMap();
        }

        protected List<Object> asList(Object value) {
            if (value instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) value;
                return list;
            }
            return Collections.singletonList(value);
        }

        protected BigDecimal toBigDecimal(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            }
            if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            }
            try {
                return new BigDecimal(String.valueOf(value));
            } catch (NumberFormatException e) {
                return null;
            }
        }
    }

    // ---------- 结果 DTO ----------

    public static class AssignmentResult {
        private Long territoryId;
        private Long teamId;
        private String ownerId;
        private String assignmentMethod;
        private boolean degraded;

        public Long getTerritoryId() {
            return territoryId;
        }

        public void setTerritoryId(Long territoryId) {
            this.territoryId = territoryId;
        }

        public Long getTeamId() {
            return teamId;
        }

        public void setTeamId(Long teamId) {
            this.teamId = teamId;
        }

        public String getOwnerId() {
            return ownerId;
        }

        public void setOwnerId(String ownerId) {
            this.ownerId = ownerId;
        }

        public String getAssignmentMethod() {
            return assignmentMethod;
        }

        public void setAssignmentMethod(String assignmentMethod) {
            this.assignmentMethod = assignmentMethod;
        }

        public boolean isDegraded() {
            return degraded;
        }

        public void setDegraded(boolean degraded) {
            this.degraded = degraded;
        }
    }
}
