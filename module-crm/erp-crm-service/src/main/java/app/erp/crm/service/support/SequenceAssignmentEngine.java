package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmSequenceAssignment;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.core.lang.json.JsonTool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * 销售序列自动分配引擎。按 priority 遍历 {@code isActive=true} 规则，{@link ConditionMatcher} 按 conditionType
 * 解析 conditionValue JSON 匹配 lead 字段；命中返回目标 {@code sequenceId}，未命中走 isDefault 兜底。
 *
 * <p>对齐 {@code docs/design/crm/sales-sequence.md §序列自动分配规则}：
 * conditionType ∈ LEAD_SOURCE/TERRITORY/PRODUCT_LINE/CUSTOM_FIELD，conditionValue 为 JSON 字符串。
 *
 * <p>纯函数式 + 注入加载函数便于单测：{@link #assign(ErpCrmLead, List, ErpCrmSequenceAssignment)}
 * 不依赖 IoC，可独立构造 rules 与 lead 测试四类 conditionType 匹配 + default 兜底 + 无匹配不分配。
 */
public class SequenceAssignmentEngine {

    private final ConditionMatcher conditionMatcher;

    public SequenceAssignmentEngine() {
        this(new ConditionMatcher());
    }

    public SequenceAssignmentEngine(ConditionMatcher conditionMatcher) {
        this.conditionMatcher = conditionMatcher;
    }

    /**
     * 按 priority 遍历 rules 找首个匹配的 active 规则；无匹配则用 defaultRule。
     *
     * @param lead        待分配线索
     * @param rules       active 规则列表（default 规则可单独传入）
     * @param defaultRule 兜底规则（可为 null）
     * @return 分配结果（命中规则返回 sequenceId + fromDefault 标记；全无匹配返回 null）
     */
    public AssignmentResult assign(ErpCrmLead lead,
                                    List<ErpCrmSequenceAssignment> rules,
                                    ErpCrmSequenceAssignment defaultRule) {
        List<ErpCrmSequenceAssignment> sorted = new ArrayList<>();
        if (rules != null) {
            for (ErpCrmSequenceAssignment r : rules) {
                if (r == null || !Boolean.TRUE.equals(r.getIsActive())
                        || Boolean.TRUE.equals(r.getIsDefault())) {
                    continue;
                }
                sorted.add(r);
            }
        }
        sorted.sort(Comparator
                .comparingInt((ErpCrmSequenceAssignment r) ->
                        r.getPriority() != null ? r.getPriority() : Integer.MAX_VALUE)
                .thenComparing(r -> r.getId() != null ? r.getId() : Long.MAX_VALUE));

        for (ErpCrmSequenceAssignment rule : sorted) {
            if (conditionMatcher.matches(rule, lead)) {
                return toResult(rule, false);
            }
        }
        if (defaultRule != null && Boolean.TRUE.equals(defaultRule.getIsActive())) {
            return toResult(defaultRule, true);
        }
        return null;
    }

    protected AssignmentResult toResult(ErpCrmSequenceAssignment rule, boolean fromDefault) {
        AssignmentResult result = new AssignmentResult();
        result.setSequenceId(rule.getSequenceId());
        result.setFromDefault(fromDefault);
        return result;
    }

    // ---------- 匹配器 ----------

    public static class ConditionMatcher {

        public boolean matches(ErpCrmSequenceAssignment rule, ErpCrmLead lead) {
            String type = rule.getConditionType();
            Map<String, Object> value = parse(rule.getConditionValue());
            if (value == null || value.isEmpty()) {
                return false;
            }
            switch (type == null ? "" : type) {
                case ErpCrmConstants.SEQ_ASSIGNMENT_CONDITION_LEAD_SOURCE:
                    return matchLeadSource(value, lead);
                case ErpCrmConstants.SEQ_ASSIGNMENT_CONDITION_TERRITORY:
                    return matchTerritory(value, lead);
                case ErpCrmConstants.SEQ_ASSIGNMENT_CONDITION_PRODUCT_LINE:
                    return matchProductLine(value, lead);
                case ErpCrmConstants.SEQ_ASSIGNMENT_CONDITION_CUSTOM_FIELD:
                    return matchCustomField(value, lead);
                default:
                    return false;
            }
        }

        /**
         * LEAD_SOURCE：conditionValue 形如 {@code {"sourceId":[101,102]}} 或 {@code {"sourceId":101}}，
         * 匹配 lead.sourceId 是否在列表/等于。
         */
        protected boolean matchLeadSource(Map<String, Object> value, ErpCrmLead lead) {
            Long sourceId = lead.getSourceId();
            if (sourceId == null) {
                return false;
            }
            Object expected = value.get("sourceId");
            return containsOrEquals(expected, sourceId);
        }

        /**
         * TERRITORY：conditionValue 形如 {@code {"territoryId":[201,202]}}，匹配 lead.territoryId。
         */
        protected boolean matchTerritory(Map<String, Object> value, ErpCrmLead lead) {
            Long territoryId = lead.getTerritoryId();
            if (territoryId == null) {
                return false;
            }
            Object expected = value.get("territoryId");
            return containsOrEquals(expected, territoryId);
        }

        /**
         * PRODUCT_LINE：conditionValue 形如 {@code {"productLine":["electronics","toys"]}}，
         * Lead 无独立 productLine 字段，临时用 department / utmSource 派生（与 TerritoryAssignmentEngine 派生范式一致）。
         */
        protected boolean matchProductLine(Map<String, Object> value, ErpCrmLead lead) {
            Object codes = value.get("productLine");
            if (codes == null) {
                return false;
            }
            String department = lead.getDepartment();
            String utm = lead.getUtmSource();
            for (Object c : asList(codes)) {
                if (c == null) {
                    continue;
                }
                String kw = String.valueOf(c);
                if (department != null && department.contains(kw)) {
                    return true;
                }
                if (utm != null && utm.contains(kw)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * CUSTOM_FIELD：conditionValue 形如 {@code {"utmSource":"baidu","jobTitle":"Manager"}}，
         * 任意 lead 字段值匹配（字符串相等）。prop_get 失败时（ORM 元数据未初始化）跳过该字段。
         */
        protected boolean matchCustomField(Map<String, Object> value, ErpCrmLead lead) {
            for (Map.Entry<String, Object> entry : value.entrySet()) {
                Object expected = entry.getValue();
                if (expected == null) {
                    continue;
                }
                Object actual;
                try {
                    actual = lead.prop_get(entry.getKey());
                } catch (Exception ignored) {
                    // ORM 元数据未初始化（如纯单测场景）→ 跳过该字段，不视为匹配
                    continue;
                }
                if (actual != null && String.valueOf(actual).equals(String.valueOf(expected))) {
                    return true;
                }
            }
            return false;
        }

        protected boolean containsOrEquals(Object expected, Long actual) {
            if (expected == null) {
                return false;
            }
            if (expected instanceof List) {
                for (Object o : (List<?>) expected) {
                    if (toLong(o) != null && toLong(o).equals(actual)) {
                        return true;
                    }
                }
                return false;
            }
            Long v = toLong(expected);
            return v != null && v.equals(actual);
        }

        protected Long toLong(Object value) {
            if (value == null) {
                return null;
            }
            if (value instanceof Number) {
                return ((Number) value).longValue();
            }
            try {
                return Long.parseLong(String.valueOf(value));
            } catch (NumberFormatException e) {
                return null;
            }
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
    }

    // ---------- 结果 DTO ----------

    public static class AssignmentResult {
        private Long sequenceId;
        private boolean fromDefault;

        public Long getSequenceId() {
            return sequenceId;
        }

        public void setSequenceId(Long sequenceId) {
            this.sequenceId = sequenceId;
        }

        public boolean isFromDefault() {
            return fromDefault;
        }

        public void setFromDefault(boolean fromDefault) {
            this.fromDefault = fromDefault;
        }
    }
}
