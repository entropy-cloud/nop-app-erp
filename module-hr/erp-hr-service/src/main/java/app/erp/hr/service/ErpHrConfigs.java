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
    /** 排班调换默认需审批。 */
    boolean DEFAULT_SHIFT_REQUIRE_APPROVAL = true;
    /** 默认迟到宽容分钟数。 */
    int DEFAULT_SHIFT_GRACE_LATE_MINUTES = 15;
    /** 默认早退宽容分钟数。 */
    int DEFAULT_SHIFT_GRACE_EARLY_LEAVE_MINUTES = 15;
    /** 默认允许跨天班次。 */
    boolean DEFAULT_SHIFT_CROSS_DAY_ENABLED = true;

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

    static boolean shiftRequireApproval() {
        Boolean v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SHIFT_REQUIRE_APPROVAL, DEFAULT_SHIFT_REQUIRE_APPROVAL);
        return v == null ? DEFAULT_SHIFT_REQUIRE_APPROVAL : v;
    }

    static int shiftDefaultGraceLateMinutes() {
        Integer v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SHIFT_DEFAULT_GRACE_LATE_MINUTES, DEFAULT_SHIFT_GRACE_LATE_MINUTES);
        return v == null ? DEFAULT_SHIFT_GRACE_LATE_MINUTES : v;
    }

    static int shiftDefaultGraceEarlyLeaveMinutes() {
        Integer v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SHIFT_DEFAULT_GRACE_EARLY_LEAVE_MINUTES, DEFAULT_SHIFT_GRACE_EARLY_LEAVE_MINUTES);
        return v == null ? DEFAULT_SHIFT_GRACE_EARLY_LEAVE_MINUTES : v;
    }

    static boolean shiftCrossDayEnabled() {
        Boolean v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SHIFT_CROSS_DAY_ENABLED, DEFAULT_SHIFT_CROSS_DAY_ENABLED);
        return v == null ? DEFAULT_SHIFT_CROSS_DAY_ENABLED : v;
    }
}
