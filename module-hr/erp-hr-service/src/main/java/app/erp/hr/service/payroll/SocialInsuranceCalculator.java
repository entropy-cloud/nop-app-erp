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
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

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
     */
    public BigDecimal[] calculate(Long employeeId, int year, int month) {
        ErpHrSocialInsuranceBase base = findBase(employeeId);
        if (base == null) {
            throw new NopException(ErpHrErrors.ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_YEAR, year)
                    .param(ErpHrErrors.ARG_MONTH, month);
        }
        List<ErpHrSocialInsuranceConfig> configs = findConfigs(base.getCityCode());
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
    public BigDecimal[] calculateHousingFund(Long employeeId, int year, int month) {
        ErpHrSocialInsuranceBase base = findBase(employeeId);
        if (base == null) {
            throw new NopException(ErpHrErrors.ERR_SOCIAL_INSURANCE_BASE_NOT_FOUND)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId)
                    .param(ErpHrErrors.ARG_YEAR, year)
                    .param(ErpHrErrors.ARG_MONTH, month);
        }
        ErpHrSocialInsuranceConfig fundCfg = findHousingFundConfig(base.getCityCode());
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

    ErpHrSocialInsuranceBase findBase(Long employeeId) {
        IEntityDao<ErpHrSocialInsuranceBase> dao = daoProvider.daoFor(ErpHrSocialInsuranceBase.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        q.setLimit(1);
        List<ErpHrSocialInsuranceBase> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    List<ErpHrSocialInsuranceConfig> findConfigs(String cityCode) {
        IEntityDao<ErpHrSocialInsuranceConfig> dao = daoProvider.daoFor(ErpHrSocialInsuranceConfig.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("cityCode", cityCode));
        return dao.findAllByQuery(q);
    }

    ErpHrSocialInsuranceConfig findHousingFundConfig(String cityCode) {
        for (ErpHrSocialInsuranceConfig cfg : findConfigs(cityCode)) {
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
