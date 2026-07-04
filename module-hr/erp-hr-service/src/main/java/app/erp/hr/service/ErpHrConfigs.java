package app.erp.hr.service;

import java.math.BigDecimal;

/**
 * HR 域配置默认值集中入口。所有 {@code erp-hr.*} 配置键经 {@link io.nop.api.core.config.AppConfig#var}
 * 读取时，统一经本接口提供默认值与解释器，避免散落在业务代码中。
 */
public interface ErpHrConfigs {

    /** 个税月起征点默认值（5000 元）。 */
    BigDecimal DEFAULT_TAX_THRESHOLD_MONTHLY = new BigDecimal("5000");
    /** 薪酬金额四舍五入默认小数位（2 位，到分）。 */
    int DEFAULT_SALARY_ROUNDING_SCALE = 2;

    static String defaultSocialInsuranceBaseCity() {
        String city = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_DEFAULT_SOCIAL_INSURANCE_BASE_CITY, "");
        if (city == null || city.trim().isEmpty()) {
            return null;
        }
        return city.trim();
    }

    static BigDecimal taxThresholdMonthly() {
        BigDecimal threshold = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_TAX_THRESHOLD_MONTHLY, DEFAULT_TAX_THRESHOLD_MONTHLY);
        return threshold == null ? DEFAULT_TAX_THRESHOLD_MONTHLY : threshold;
    }

    static String defaultPayrollSubjectCode() {
        String code = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "");
        if (code == null || code.trim().isEmpty()) {
            return null;
        }
        return code.trim();
    }

    static int salaryRoundingScale() {
        Integer scale = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SALARY_ROUNDING_SCALE, DEFAULT_SALARY_ROUNDING_SCALE);
        return scale == null ? DEFAULT_SALARY_ROUNDING_SCALE : scale;
    }
}
