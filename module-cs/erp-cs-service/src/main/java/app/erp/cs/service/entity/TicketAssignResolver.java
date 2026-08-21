package app.erp.cs.service.entity;

import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTeam;
// bridge-main-053/054: ErpCrmTeam/ErpCrmTeamMember 为 crm 未迁移（M3.4）Long 实体，类型级引用（退役 owner M3.4）
import app.erp.crm.biz.IErpCrmTeamBiz;
import app.erp.crm.biz.IErpCrmTeamMemberBiz;
import app.erp.crm.dao.entity.ErpCrmTeam;
import app.erp.crm.dao.entity.ErpCrmTeamMember;
import app.erp.cs.service.ErpCsConfigs;
import app.erp.cs.service.ErpCsConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 工单自动分配解析器（RC-R1.65，P1-RC-054，UC-CS-01 ④⑤⑦⑧）。
 *
 * <p>候选池（plan D3 选项 A）：SLA 策略 teamId → {@link ErpCsTeam} → 按 code 相等约定映射同码
 * crm {@link ErpCrmTeam} → {@link IErpCrmTeamMemberBiz} 成员 userId 池（userId VARCHAR(36)
 * stdDomain=userId 与 {@code ErpCsTicket.assignedToId} 同型直接可赋）。无同码 crm 团队/无成员 → 池空 → ⑧ 异常路径。
 *
 * <p>算法（plan D4，镜像 R1.57 TeamMemberResolver 范式）：ROUND_ROBIN（上次分配的下一个，无历史首位）
 * / LEAST_OPEN（活跃工单最少者，平手取成员 id 升序首个）；纯函数 {@link #pickAssignee} 便于 mock 池单测。
 * 工单历史查询（上次分配/未结计数）由调用方 BizModel 经自有 findList 提供，避免 IErpCsTicketBiz 自注入环。
 */
public class TicketAssignResolver {

    @Inject
    IErpCrmTeamBiz crmTeamBiz;

    @Inject
    IErpCrmTeamMemberBiz crmTeamMemberBiz;

    /**
     * 解析候选池：cs 团队按 code 映射同码 crm 团队 → 成员 userId 列表（成员行 id 升序）。
     * csTeam 为 null / 无同码 crm 团队 / 无成员 → 空列表（调用方走 ⑧ 异常路径）。
     */
    public List<String> resolveCandidatePool(ErpCsTeam csTeam, IServiceContext context) {
        if (csTeam == null || csTeam.getCode() == null || csTeam.getCode().trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", csTeam.getCode()));
            q.setLimit(1);
            // bridge-main-055: findList 仅 code（VARCHAR）过滤，零 id 值穿越（退役 owner M3.4）
            List<ErpCrmTeam> matched = crmTeamBiz.findList(q, null, context);
            if (matched.isEmpty()) {
                return new ArrayList<>();
            }
            QueryBean mq = new QueryBean();
            // bridge-main-056: crm getId()（Long）→ crm teamId 过滤值，crm 域内 Long↔Long 自洽
            // （M3.4 翻转后 String↔String 仍自洽，无跨域类型穿越；退役 owner M3.4）
            mq.addFilter(eq("teamId", matched.get(0).getId()));
            mq.addOrderField("id", false);
            // bridge-main-056: findList 成员查询，id 值流见上（退役 owner M3.4）
            List<ErpCrmTeamMember> members = crmTeamMemberBiz.findList(mq, null, context);
            List<String> userIds = new ArrayList<>();
            for (ErpCrmTeamMember m : members) {
                if (m.getUserId() != null && !userIds.contains(m.getUserId())) {
                    userIds.add(m.getUserId());
                }
            }
            return userIds;
        } catch (RuntimeException e) {
            // 跨域解析失败降级：池空 → 调用方留 NEW + ⑧ 升级通知
            return new ArrayList<>();
        }
    }

    /**
     * 纯函数算法入口：按 config 指定方法挑人；池空/未知方法返回 null（降级语义）。
     *
     * @param method        {@code erp-cs.assign-method}（ROUND_ROBIN | LEAST_OPEN）
     * @param members       候选池（id 升序）
     * @param lastAssigned  候选池成员内最近一次已分配人（无历史 null）
     * @param openCounts    各成员当前活跃（ASSIGNED/IN_PROGRESS）工单计数（缺项按 0）
     */
    public static String pickAssignee(String method, List<String> members,
                                      String lastAssigned, Map<String, Integer> openCounts) {
        if (members == null || members.isEmpty()) {
            return null;
        }
        if (ErpCsConstants.ASSIGN_METHOD_LEAST_OPEN.equals(method)) {
            return pickLeastOpen(members, openCounts);
        }
        return pickRoundRobin(members, lastAssigned);
    }

    /** 当前 config 方法便捷入口。 */
    public String pickByConfig(List<String> members, String lastAssigned, Map<String, Integer> openCounts) {
        return pickAssignee(ErpCsConfigs.getAssignMethod(), members, lastAssigned, openCounts);
    }

    /** ROUND_ROBIN：上次分配成员的下一个（循环）；无历史/历史不在池内 → 首位。 */
    static String pickRoundRobin(List<String> members, String lastAssigned) {
        int index = lastAssigned != null ? members.indexOf(lastAssigned) : -1;
        if (index < 0) {
            return members.get(0);
        }
        return members.get((index + 1) % members.size());
    }

    /** LEAST_OPEN：活跃计数最少者；平手取列表序（成员行 id 升序）首个。 */
    static String pickLeastOpen(List<String> members, Map<String, Integer> openCounts) {
        String picked = null;
        int min = Integer.MAX_VALUE;
        for (String member : members) {
            int count = openCounts == null ? 0 : openCounts.getOrDefault(member, 0);
            if (count < min) {
                min = count;
                picked = member;
            }
        }
        return picked;
    }

    /**
     * teamId 解析链（D3）：挂载策略 teamId（显式挂载/catalog 路径）→ 回退工单类型默认策略 teamId
     * （自动挂载主链——SlaPolicyMatcher 仅匹配 teamId IS NULL 策略）；均无 → null（池空 → ⑧）。
     * policy → team 经 ORM to-one 关系 getter，零 daoFor。
     */
    public static ErpCsTeam resolveTeam(ErpCsSlaPolicy attachedPolicy, ErpCsSlaPolicy typeDefaultPolicy) {
        ErpCsTeam team = teamOf(attachedPolicy);
        if (team != null) {
            return team;
        }
        return teamOf(typeDefaultPolicy);
    }

    private static ErpCsTeam teamOf(ErpCsSlaPolicy policy) {
        if (policy == null || policy.getTeamId() == null) {
            return null;
        }
        return policy.getTeam();
    }
}
