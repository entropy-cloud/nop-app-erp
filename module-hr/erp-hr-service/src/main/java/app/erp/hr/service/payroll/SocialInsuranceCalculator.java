package app.erp.hr.service.payroll;

import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.isNull;
import static io.nop.api.core.beans.FilterBeans.le;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 社保计算器（payroll.md §2.4）。读 {@link ErpHrSocialInsuranceBase}（员工有效基数）
 * + {@link ErpHrSocialInsuranceConfig}（城市×险种比例），基数钳制 min(max(base, lowerLimit), upperLimit)，
 * 个人扣款 = Σ(基数×个人比例)，公司承担 = Σ(基数×公司比例)。
 *
 * <p>本类为纯计算组件（无事务/无状态），由 {@code PayrollCalculator} 编排调用。
 * 跨实体读经 {@link IDaoProvider}（配置数据，非业务实体 CRUD 管道；对齐 projects 域 TimesheetPostingDispatcher
 * 读主数据模式）。
 */
public class SocialInsuranceCalculator {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 计算社保（不含公积金）。返回 [0]=个人扣款合计，[1]=公司承担合计。
     *
     * <p>P1-CK-hr2-004：基数与配置读取按核算期有效区间过滤（effectiveFrom = null 视为开区间起点，
     * effectiveTo = null 视为开区间终点），同险种多行命中取 effectiveFrom 最新一行——年调后
     * 同城同险种多行并存不再重复计扣（payroll.md §2.2/§2.3）。
     */
    public BigDecimal[] calculate(String employeeId, int year, int month) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
        ErpHrSocialInsuranceBase base = findBase(employeeId, periodStart, periodEnd);
        if (base == null) {
            throw new NopException(ErpHrErrors.ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_YEAR, year)
                    .param(ErpHrErrors.ARG_MONTH, month);
        }
        List<ErpHrSocialInsuranceConfig> configs = dedupeLatestPerType(
                findConfigs(base.getCityCode(), periodStart, periodEnd));
        if (configs.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_SOCIAL_INSURANCE_CONFIG_NOT_FOUND)
                    .param(ErpHrErrors.ARG_CITY_CODE, base.getCityCode());
        }

        BigDecimal monthlyBase = nz(base.getSocialInsuranceBase());
        BigDecimal employeeTotal = BigDecimal.ZERO;
        BigDecimal companyTotal = BigDecimal.ZERO;
        for (ErpHrSocialInsuranceConfig cfg : configs) {
            if (ErpHrConstants.INSURANCE_HOUSING_FUND.equals(cfg.getInsuranceType())) {
                continue;
            }
            BigDecimal clamped = clamp(monthlyBase, cfg.getBaseLowerLimit(), cfg.getBaseUpperLimit());
            employeeTotal = employeeTotal.add(clamped.multiply(nz(cfg.getEmployeeRate())));
            companyTotal = companyTotal.add(clamped.multiply(nz(cfg.getCompanyRate())));
        }
        return new BigDecimal[]{employeeTotal, companyTotal};
    }

    /**
     * 计算公积金。返回 [0]=个人扣款，[1]=公司承担。优先用 housingFundBase，缺失回退社保基数。
     */
    public BigDecimal[] calculateHousingFund(String employeeId, int year, int month) {
        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);
        ErpHrSocialInsuranceBase base = findBase(employeeId, periodStart, periodEnd);
        if (base == null) {
            throw new NopException(ErpHrErrors.ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_YEAR, year)
                    .param(ErpHrErrors.ARG_MONTH, month);
        }
        ErpHrSocialInsuranceConfig fundCfg = findHousingFundConfig(base.getCityCode(), periodStart, periodEnd);
        if (fundCfg == null) {
            throw new NopException(ErpHrErrors.ERR_HOUSING_FUND_CONFIG_NOT_FOUND)
                    .param(ErpHrErrors.ARG_CITY_CODE, base.getCityCode());
        }
        BigDecimal fundBase = base.getHousingFundBase() != null ? nz(base.getHousingFundBase())
                : nz(base.getSocialInsuranceBase());
        BigDecimal clamped = clamp(fundBase, fundCfg.getBaseLowerLimit(), fundCfg.getBaseUpperLimit());
        BigDecimal employee = clamped.multiply(nz(fundCfg.getEmployeeRate()));
        BigDecimal company = clamped.multiply(nz(fundCfg.getCompanyRate()));
        return new BigDecimal[]{employee, company};
    }

    /**
     * 有效区间命中判定（P1-CK-hr2-004）：effectiveFrom = null 视为无限过去，effectiveTo = null
     * 视为无限未来；区间与核算月 [periodStart, periodEnd] 相交即命中（asOf = 月末口径，
     * plan 2026-09-11-1530-1 Phase 3 Decision）。
     */
    static boolean isEffective(LocalDate effectiveFrom, LocalDate effectiveTo,
                               LocalDate periodStart, LocalDate periodEnd) {
        return (effectiveFrom == null || !effectiveFrom.isAfter(periodEnd))
                && (effectiveTo == null || !effectiveTo.isBefore(periodStart));
    }

    /**
     * 同险种多行去重：取 effectiveFrom 最新一行（null 视为最旧）；effectiveFrom 相同取 id 较小者保证确定性。
     */
    static List<ErpHrSocialInsuranceConfig> dedupeLatestPerType(List<ErpHrSocialInsuranceConfig> configs) {
        Map<String, ErpHrSocialInsuranceConfig> byType = new LinkedHashMap<>();
        for (ErpHrSocialInsuranceConfig cfg : configs) {
            ErpHrSocialInsuranceConfig existing = byType.get(cfg.getInsuranceType());
            if (existing == null || isNewerEffective(cfg, existing)) {
                byType.put(cfg.getInsuranceType(), cfg);
            }
        }
        return List.copyOf(byType.values());
    }

    private static boolean isNewerEffective(ErpHrSocialInsuranceConfig candidate, ErpHrSocialInsuranceConfig existing) {
        LocalDate cFrom = candidate.getEffectiveFrom();
        LocalDate eFrom = existing.getEffectiveFrom();
        if (cFrom == null) {
            return false;
        }
        if (eFrom == null) {
            return true;
        }
        if (cFrom.isAfter(eFrom)) {
            return true;
        }
        if (cFrom.isEqual(eFrom)) {
            String cId = candidate.getId();
            String eId = existing.getId();
            return cId != null && eId != null && cId.compareTo(eId) < 0;
        }
        return false;
    }

    ErpHrSocialInsuranceBase findBase(String employeeId, LocalDate periodStart, LocalDate periodEnd) {
        IEntityDao<ErpHrSocialInsuranceBase> dao = daoProvider.daoFor(ErpHrSocialInsuranceBase.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                or(isNull("effectiveFrom"), le("effectiveFrom", periodEnd)),
                or(isNull("effectiveTo"), ge("effectiveTo", periodStart))));
        q.addOrderField("effectiveFrom", true);
        q.addOrderField("id", false);
        q.setLimit(1);
        List<ErpHrSocialInsuranceBase> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    List<ErpHrSocialInsuranceConfig> findConfigs(String cityCode, LocalDate periodStart, LocalDate periodEnd) {
        IEntityDao<ErpHrSocialInsuranceConfig> dao = daoProvider.daoFor(ErpHrSocialInsuranceConfig.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("cityCode", cityCode),
                or(isNull("effectiveFrom"), le("effectiveFrom", periodEnd)),
                or(isNull("effectiveTo"), ge("effectiveTo", periodStart))));
        return dao.findAllByQuery(q);
    }

    ErpHrSocialInsuranceConfig findHousingFundConfig(String cityCode, LocalDate periodStart, LocalDate periodEnd) {
        for (ErpHrSocialInsuranceConfig cfg : dedupeLatestPerType(findConfigs(cityCode, periodStart, periodEnd))) {
            if (ErpHrConstants.INSURANCE_HOUSING_FUND.equals(cfg.getInsuranceType())) {
                return cfg;
            }
        }
        return null;
    }

    static BigDecimal clamp(BigDecimal value, BigDecimal lower, BigDecimal upper) {
        BigDecimal v = nz(value);
        if (lower != null && v.compareTo(lower) < 0) {
            v = lower;
        }
        if (upper != null && v.compareTo(upper) > 0) {
            v = upper;
        }
        return v;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
