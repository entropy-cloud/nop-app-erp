package app.erp.prj.service.cost;

import app.erp.prj.dao.entity.ErpPrjActivityType;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 工时成本率解析器。按优先级解析（{@code cost-collection.md §2.2}，实现偏差见计划 Task Route Decision）：
 * <ol>
 *   <li>{@link ErpPrjTimesheet#getCostRate()}（按单填写，最高优先级）。</li>
 *   <li>{@link ErpPrjActivityType#getCostRate()}（活动类型默认）。</li>
 *   <li>{@code erp-prj.default-labor-cost-rate}（全局默认 config）。</li>
 * </ol>
 *
 * <p><b>实现偏差</b>：设计 §2.2 声明「用户级 &gt; 角色级 &gt; 活动类型级」，但 ORM 中
 * {@code ErpPrjProjectUser.role} 为纯文本无费率列，无用户级/角色级独立费率载体。
 * 本期以「单填 &gt; 活动类型 &gt; 默认」实现；用户级/角色级独立费率为本期 Non-Goal。
 *
 * <p>三处皆无时抛 {@link ErpPrjErrors#ERR_COST_RATE_NOT_AVAILABLE}。
 */
public class CostRateResolver {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 解析工时成本率。返回非空 BigDecimal（&gt;= 0）。
     *
     * @param timesheet 已加载的工时实体（取 costRate/activityTypeId）
     * @param timesheetCode 用于异常上下文
     */
    public BigDecimal resolve(ErpPrjTimesheet timesheet, String timesheetCode) {
        BigDecimal rate = parseDecimal(timesheet.getCostRate());
        if (rate != null) {
            return rate;
        }

        Long activityTypeId = timesheet.getActivityTypeId();
        if (activityTypeId != null) {
            IEntityDao<ErpPrjActivityType> dao = daoProvider.daoFor(ErpPrjActivityType.class);
            ErpPrjActivityType activityType = dao.getEntityById(activityTypeId);
            if (activityType != null) {
                BigDecimal activityRate = parseDecimal(activityType.getCostRate());
                if (activityRate != null) {
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
