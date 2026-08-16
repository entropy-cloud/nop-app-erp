package app.erp.crm.service.support;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmTerritoryAssignmentRule;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.core.context.IServiceContext;
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
 * <p>对齐 {@code docs/design/crm/territory.md §业务规则 2 线索自动分配 / §分配执行流程}（plan
 * 2026-08-16-1634-1 RC-R1.57）：ROUND_ROBIN/LOAD_BALANCED 经 {@link TeamMemberResolver} 挑人回写
 * ownerId；resolver 不可用 / 成员列表为空 / 查询失败 → 按 MANUAL 语义降级（degraded=true，仅回写 territory/team）。
 *
 * <p>纯函数式 + 注入加载函数便于单测：{@link #assign(ErpCrmLead, List, ErpCrmTerritoryAssignmentRule, TeamMemberResolver, IServiceContext)}
 * 不依赖 IoC，可独立构造 rules/lead/resolver 测试挑人语义。
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
     * 按 priority 遍历 rules 找首个匹配的 active 规则；无匹配则用 defaultRule（resolver 为 null，挑人降级 MANUAL）。
     *
     * @param lead        待分配线索
     * @param rules       active 规则列表（priority 升序，default 规则可单独传入）
     * @param defaultRule 兜底规则（可为 null）
     * @return 分配结果（命中规则返回 territoryId/teamId/method；全无匹配返回 null，调用方标记"未分配"）
     */
    public AssignmentResult assign(ErpCrmLead lead,
                                    List<ErpCrmTerritoryAssignmentRule> rules,
                                    ErpCrmTerritoryAssignmentRule defaultRule) {
        return assign(lead, rules, defaultRule, null, null);
    }

    /**
     * 同上，支持经 {@link TeamMemberResolver} 挑人（ROUND_ROBIN/LOAD_BALANCED）。
     *
     * @param teamMemberResolver 团队成员解析器（可为 null——非 MANUAL 方法按 MANUAL 降级，保持既有语义）
     */
    public AssignmentResult assign(ErpCrmLead lead,
                                    List<ErpCrmTerritoryAssignmentRule> rules,
                                    ErpCrmTerritoryAssignmentRule defaultRule,
                                    TeamMemberResolver teamMemberResolver,
                                    IServiceContext context) {
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
                return toResult(rule, teamMemberResolver, context);
            }
        }
        if (defaultRule != null && Boolean.TRUE.equals(defaultRule.getIsActive())) {
            return toResult(defaultRule, teamMemberResolver, context);
        }
        return null;
    }

    /**
     * 构造分配结果；非 MANUAL 方法经 resolver 挑人回写 ownerId，不可解析时按 MANUAL 降级（degraded=true）。
     */
    protected AssignmentResult toResult(ErpCrmTerritoryAssignmentRule rule,
                                        TeamMemberResolver teamMemberResolver,
                                        IServiceContext context) {
        AssignmentResult result = new AssignmentResult();
        result.setTerritoryId(rule.getTerritoryId());
        result.setTeamId(rule.getGroupId());
        result.setAssignmentMethod(rule.getAssignmentMethod());
        String method = rule.getAssignmentMethod();
        if (ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL.equals(method)) {
            return result;
        }
        // 非 MANUAL：resolver 不可用 / 无团队 / 成员不可解析 → MANUAL 降级（既有语义零变化）
        if (teamMemberResolver == null || rule.getGroupId() == null) {
            return degradeToManual(result);
        }
        Long teamId = rule.getGroupId();
        List<String> members;
        try {
            members = teamMemberResolver.resolveTeamMemberUserIds(teamId, context);
        } catch (RuntimeException e) {
            return degradeToManual(result);
        }
        if (members == null || members.isEmpty()) {
            return degradeToManual(result);
        }
        String ownerId = null;
        try {
            if (ErpCrmConstants.ASSIGNMENT_METHOD_ROUND_ROBIN.equals(method)) {
                ownerId = pickRoundRobin(teamId, members, teamMemberResolver, context);
            } else if (ErpCrmConstants.ASSIGNMENT_METHOD_LOAD_BALANCED.equals(method)) {
                ownerId = pickLoadBalanced(teamId, members, teamMemberResolver, context);
            }
        } catch (RuntimeException e) {
            return degradeToManual(result);
        }
        if (ownerId == null) {
            return degradeToManual(result);
        }
        result.setOwnerId(ownerId);
        return result;
    }

    /**
     * ROUND_ROBIN：取上次分配 owner 在成员列表中的下一位（循环）；无历史记录 → 第一位成员。
     */
    protected String pickRoundRobin(Long teamId, List<String> members,
                                    TeamMemberResolver teamMemberResolver, IServiceContext context) {
        String lastOwner = teamMemberResolver.resolveLastAssignedOwner(teamId, context);
        int index = lastOwner != null ? members.indexOf(lastOwner) : -1;
        if (index < 0) {
            return members.get(0);
        }
        return members.get((index + 1) % members.size());
    }

    /**
     * LOAD_BALANCED：取当前活跃线索最少的成员（平手按成员列表序[id 升序]首个）；无计数成员按 0 计。
     */
    protected String pickLoadBalanced(Long teamId, List<String> members,
                                      TeamMemberResolver teamMemberResolver, IServiceContext context) {
        Map<String, Integer> counts = teamMemberResolver.countActiveLeadsByOwner(teamId, context);
        String picked = null;
        int min = Integer.MAX_VALUE;
        for (String member : members) {
            int count = counts == null ? 0 : counts.getOrDefault(member, 0);
            if (count < min) {
                min = count;
                picked = member;
            }
        }
        return picked;
    }

    protected AssignmentResult degradeToManual(AssignmentResult result) {
        result.setAssignmentMethod(ErpCrmConstants.ASSIGNMENT_METHOD_MANUAL);
        result.setDegraded(true);
        return result;
    }

    /**
     * 团队成员解析接口（D4 选项 A，plan 2026-08-16-1634-1 RC-R1.57）：由调用方（BizModel）提供 dao 实现，
     * 引擎保持纯函数式可测。
     */
    public interface TeamMemberResolver {

        /**
         * 查 teamId 团队成员 userId 列表（按成员行 id 升序）；无成员返回空列表。
         */
        List<String> resolveTeamMemberUserIds(Long teamId, IServiceContext context);

        /**
         * 查 teamId 上次分配记录的 owner（lead.teamId=teamId 且 ownerId 非空，按 createTime desc limit 1）；无记录返回 null。
         */
        String resolveLastAssignedOwner(Long teamId, IServiceContext context);

        /**
         * 查 teamId 各成员当前活跃线索数（count lead.teamId=teamId 且 ownerId=member 且 docStatus 非
         * CONVERTED/LOST/CANCELLED）；无计数记录的成员视为 0。
         */
        Map<String, Integer> countActiveLeadsByOwner(Long teamId, IServiceContext context);
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
