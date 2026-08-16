package app.erp.prj.service.cost;

import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjProjectUser;
import app.erp.prj.dao.entity.ErpPrjRole;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 工时成本率解析器。按优先级解析（{@code cost-collection.md §2.2} + 本计划 RC-R1.60 Phase 1 裁决）：
 * <ol>
 *   <li>{@link ErpPrjTimesheet#getCostRate()}（按单填写，显式录入优先，最高优先级）。</li>
 *   <li>{@link ErpPrjProjectUser#getCostRate()}（用户级费率——项目成员按 projectId+userId 查 costRate）。</li>
 *   <li>{@link ErpPrjRole#getCostRate()}（角色级费率——成员行 role 文本经 {@link ErpPrjRole#getCode()} 精确匹配 costRate）。</li>
 *   <li>{@link ErpPrjActivityType#getCostRate()}（活动类型默认）。</li>
 *   <li>{@code erp-prj.default-labor-cost-rate}（全局默认 config）。</li>
 * </ol>
 *
 * <p>L1（{@code use-cases.md:38}）三级优先级「用户费率 &gt; 角色费率 &gt; 活动类型费率」运行时成立；
 * 单填/全局默认按 RC-R1.60 Phase 1 D2 裁决归位（单填保持最高显式录入优先，全局默认兜底）。
 * 用户级/角色级费率为 null 或成员行/角色缺失时跳过对应 tier。
 *
 * <p>五处皆无时抛 {@link ErpPrjErrors#ERR_COST_RATE_NOT_AVAILABLE}。
 */
public class CostRateResolver {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 解析工时成本率。返回非空 BigDecimal（&gt;= 0）。
     *
     * @param timesheet 已加载的工时实体（取 costRate/activityTypeId/projectId/userId）
     * @param timesheetCode 用于异常上下文
     */
    public BigDecimal resolve(ErpPrjTimesheet timesheet, String timesheetCode) {
        BigDecimal rate = timesheet.getCostRate();
        if (rate != null && rate.signum() >= 0) {
            return rate;
        }

        // 用户级 > 角色级：同一次成员行查询承载两级（用户费率 + 角色文本）
        ErpPrjProjectUser member = findProjectMember(timesheet);
        if (member != null) {
            BigDecimal userRate = member.getCostRate();
            if (userRate != null && userRate.signum() >= 0) {
                return userRate;
            }
            String roleCode = member.getRole();
            if (roleCode != null && !roleCode.trim().isEmpty()) {
                BigDecimal roleRate = findRoleRate(roleCode.trim());
                if (roleRate != null) {
                    return roleRate;
                }
            }
        }

        Long activityTypeId = timesheet.getActivityTypeId();
        if (activityTypeId != null) {
            IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
            ErpPrjActivityType activityType = dao.getEntityById(activityTypeId);
            if (activityType != null) {
                BigDecimal activityRate = activityType.getCostRate();
                if (activityRate != null && activityRate.signum() >= 0) {
                    return activityRate;
                }
            }
        }

        String defaultRate = ErpPrjConfigs.defaultLaborCostRate();
        BigDecimal globalRate = parseDecimal(defaultRate);
        if (globalRate != null) {
            return globalRate;
        }

        throw new NopException(ErpPrjErrors.ERR_COST_RATE_NOT_AVAILABLE)
                .param(ErpPrjErrors.ARG_TIMESHEET_CODE, timesheetCode)
                .param(ErpPrjErrors.ARG_ACTIVITY_TYPE_ID, activityTypeId);
    }

    /**
     * 用户级费率载体：ErpPrjProjectUser 按 (projectId, userId) 查成员行（RC-R1.60 D3）。
     * 同域直查（非跨域）——与 activityType 查询同型；findFirstByQuery 单行查询不增 R1d 计数面。
     */
    private ErpPrjProjectUser findProjectMember(ErpPrjTimesheet timesheet) {
        if (timesheet.getProjectId() == null || timesheet.getUserId() == null) {
            return null;
        }
        QueryBean query = new QueryBean();
        query.addFilter(eq("projectId", timesheet.getProjectId()));
        query.addFilter(eq("userId", timesheet.getUserId()));
        query.setLimit(1);
        return daoProvider.daoFor(ErpPrjProjectUser.class).findFirstByQuery(query);
    }

    /**
     * 角色级费率载体：ErpPrjRole 按 code 精确匹配（RC-R1.60 D1：role 文本 trim 后与 code 精确相等）。
     */
    private BigDecimal findRoleRate(String roleCode) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("code", roleCode));
        query.setLimit(1);
        ErpPrjRole role = daoProvider.daoFor(ErpPrjRole.class).findFirstByQuery(query);
        if (role == null) {
            return null;
        }
        BigDecimal rate = role.getCostRate();
        return rate != null && rate.signum() >= 0 ? rate : null;
    }

    private BigDecimal parseDecimal(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            BigDecimal v = new BigDecimal(text.trim());
            return v.signum() >= 0 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 计算人工成本 = 工时 × 成本率。 */
    public static BigDecimal computeCostAmount(BigDecimal hours, BigDecimal costRate) {
        if (hours == null || costRate == null) {
            return BigDecimal.ZERO;
        }
        return hours.multiply(costRate);
    }

    /** 用于在异常参数中显示配置键名。 */
    public static String configKey() {
        return ErpPrjConstants.CONFIG_DEFAULT_LABOR_COST_RATE;
    }
}
