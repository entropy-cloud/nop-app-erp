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
    /** 模拟实发变化告警阈值（默认 ±20%）。 */
    BigDecimal DEFAULT_SIM_NET_PAY_CHANGE_THRESHOLD = new BigDecimal("0.2");
    /** 模拟总额偏差告警阈值（默认 ±10%）。 */
    BigDecimal DEFAULT_SIM_TOTAL_CHANGE_THRESHOLD = new BigDecimal("0.1");
    /** 模拟个税跳档告警默认启用。 */
    boolean DEFAULT_SIM_TAX_BRACKET_JUMP_ALERT = true;
    /** 模拟自动转正式默认关闭（手动触发）。 */
    boolean DEFAULT_SIM_AUTO_CONVERT_ENABLED = false;

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

    static BigDecimal simulationNetPayChangeThreshold() {
        BigDecimal v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SIM_NET_PAY_CHANGE_THRESHOLD, DEFAULT_SIM_NET_PAY_CHANGE_THRESHOLD);
        return v == null ? DEFAULT_SIM_NET_PAY_CHANGE_THRESHOLD : v;
    }

    static BigDecimal simulationTotalChangeThreshold() {
        BigDecimal v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SIM_TOTAL_CHANGE_THRESHOLD, DEFAULT_SIM_TOTAL_CHANGE_THRESHOLD);
        return v == null ? DEFAULT_SIM_TOTAL_CHANGE_THRESHOLD : v;
    }

    static boolean simulationTaxBracketJumpAlert() {
        Boolean v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SIM_TAX_BRACKET_JUMP_ALERT, DEFAULT_SIM_TAX_BRACKET_JUMP_ALERT);
        return v == null ? DEFAULT_SIM_TAX_BRACKET_JUMP_ALERT : v;
    }

    static boolean simulationAutoConvertEnabled() {
        Boolean v = io.nop.api.core.config.AppConfig.var(
                ErpHrConstants.CONFIG_SIM_AUTO_CONVERT_ENABLED, DEFAULT_SIM_AUTO_CONVERT_ENABLED);
        return v == null ? DEFAULT_SIM_AUTO_CONVERT_ENABLED : v;
    }
}
