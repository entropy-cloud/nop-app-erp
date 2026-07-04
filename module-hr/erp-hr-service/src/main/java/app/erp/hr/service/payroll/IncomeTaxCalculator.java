package app.erp.hr.service.payroll;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrTaxConfig;
import app.erp.hr.dao.entity.ErpHrTaxSpecialDeduction;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.lt;

/**
 * 个税累计预扣法计算器（payroll.md §4.5）。从 {@link ErpHrSalary#cumulativeData}（或年度历史查询）
 * 取年初至上月累计应发/免征额/专项扣除/已预扣；累加当月 → 累计应纳税所得额 → 查 {@link ErpHrTaxConfig#taxBrackets}
 * 七级税率 → 累计应纳税额 − 累计已预扣 = 当月应纳；写回当月 cumulativeData。
 *
 * <p>累计数据 JSON 格式（存 ErpHrSalary.cumulativeData 字段）：
 * <pre>
 * {
 *   "cumulativeGross": 60000.00,       // 累计应发（含当月）
 *   "cumulativeThreshold": 25000.00,   // 累计免征额（含当月）
 *   "cumulativeSpecialDeduction": 6000.00,  // 累计专项扣除（社保+公积金个人部分，含当月）
 *   "cumulativeAdditionalDeduction": 0.00,  // 累计专项附加扣除（含当月）
 *   "cumulativeTaxableIncome": 29000.00,    // 累计应纳税所得额
 *   "cumulativeTaxAmount": 870.00,          // 累计应纳税额
 *   "cumulativePrepaidTax": 870.00          // 累计已预扣税额（含当月）
 * }
 * </pre>
 */
public class IncomeTaxCalculator {

    private static final String KEY_CUM_GROSS = "cumulativeGross";
    private static final String KEY_CUM_THRESHOLD = "cumulativeThreshold";
    private static final String KEY_CUM_SPECIAL_DEDUCTION = "cumulativeSpecialDeduction";
    private static final String KEY_CUM_ADDITIONAL_DEDUCTION = "cumulativeAdditionalDeduction";
    private static final String KEY_CUM_TAXABLE_INCOME = "cumulativeTaxableIncome";
    private static final String KEY_CUM_TAX_AMOUNT = "cumulativeTaxAmount";
    private static final String KEY_CUM_PREPAID_TAX = "cumulativePrepaidTax";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    TaxBracketParser bracketParser;

    /**
     * 计算当月个税（累计预扣法）。
     *
     * @param employeeId           员工
     * @param year/month           核算期
     * @param currentMonthGross    当月应发合计
     * @param currentMonthSpecialDeduction 当月专项扣除（社保+公积金个人部分）
     * @return [0]=当月应纳个税，[1]=写回的 cumulativeData JSON 字符串
     */
    public Object[] calculate(Long employeeId, int year, int month,
                              BigDecimal currentMonthGross, BigDecimal currentMonthSpecialDeduction) {
        ErpHrTaxConfig taxConfig = findTaxConfig(year);
        BigDecimal threshold = ErpHrConfigs.taxThresholdMonthly();

        BigDecimal additionalDeduction = sumSpecialDeduction(employeeId, year, month);

        java.util.Map<String, BigDecimal> prev = findPreviousCumulative(employeeId, year, month);
        BigDecimal cumGross = nz(prev.get(KEY_CUM_GROSS)).add(nz(currentMonthGross));
        BigDecimal cumThreshold = nz(prev.get(KEY_CUM_THRESHOLD)).add(threshold);
        BigDecimal cumSpecial = nz(prev.get(KEY_CUM_SPECIAL_DEDUCTION)).add(nz(currentMonthSpecialDeduction));
        BigDecimal cumAdditional = nz(prev.get(KEY_CUM_ADDITIONAL_DEDUCTION)).add(additionalDeduction);

        BigDecimal cumTaxableIncome = cumGross.subtract(cumThreshold).subtract(cumSpecial).subtract(cumAdditional);
        if (cumTaxableIncome.signum() < 0) {
            cumTaxableIncome = BigDecimal.ZERO;
        }

        List<TaxBracket> brackets = bracketParser.parse(taxConfig.getTaxBrackets());
        TaxBracket bracket = resolveBracket(brackets, cumTaxableIncome);
        BigDecimal cumTaxAmount = cumTaxableIncome.multiply(bracket.getRate())
                .subtract(bracket.getQuickDeduction());
        if (cumTaxAmount.signum() < 0) {
            cumTaxAmount = BigDecimal.ZERO;
        }

        BigDecimal cumPrepaidBefore = nz(prev.get(KEY_CUM_PREPAID_TAX));
        BigDecimal monthTax = cumTaxAmount.subtract(cumPrepaidBefore);
        if (monthTax.signum() < 0) {
            monthTax = BigDecimal.ZERO;
        }
        BigDecimal cumPrepaidAfter = cumPrepaidBefore.add(monthTax);

        int scale = ErpHrConfigs.salaryRoundingScale();
        monthTax = monthTax.setScale(scale, RoundingMode.HALF_UP);

        java.util.Map<String, Object> cumulativeData = new java.util.LinkedHashMap<>();
        cumulativeData.put(KEY_CUM_GROSS, cumGross.setScale(scale, RoundingMode.HALF_UP));
        cumulativeData.put(KEY_CUM_THRESHOLD, cumThreshold.setScale(scale, RoundingMode.HALF_UP));
        cumulativeData.put(KEY_CUM_SPECIAL_DEDUCTION, cumSpecial.setScale(scale, RoundingMode.HALF_UP));
        cumulativeData.put(KEY_CUM_ADDITIONAL_DEDUCTION, cumAdditional.setScale(scale, RoundingMode.HALF_UP));
        cumulativeData.put(KEY_CUM_TAXABLE_INCOME, cumTaxableIncome.setScale(scale, RoundingMode.HALF_UP));
        cumulativeData.put(KEY_CUM_TAX_AMOUNT, cumTaxAmount.setScale(scale, RoundingMode.HALF_UP));
        cumulativeData.put(KEY_CUM_PREPAID_TAX, cumPrepaidAfter.setScale(scale, RoundingMode.HALF_UP));

        return new Object[]{monthTax, io.nop.core.lang.json.JsonTool.serialize(cumulativeData, false)};
    }

    /**
     * 查询年度历史薪酬累计（年初至上月），汇总累计值。1 月无历史返回全 0。
     */
    java.util.Map<String, BigDecimal> findPreviousCumulative(Long employeeId, int year, int currentMonth) {
        java.util.Map<String, BigDecimal> result = new java.util.HashMap<>();
        if (currentMonth <= 1) {
            return result;
        }
        IEntityDao<ErpHrSalary> dao = daoProvider.daoFor(ErpHrSalary.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                eq("year", year),
                lt("month", currentMonth)));
        List<ErpHrSalary> history = dao.findAllByQuery(q);

        BigDecimal[] totals = new BigDecimal[]{
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO};
        for (ErpHrSalary s : history) {
            if (ErpHrConstants.APPROVAL_VOID.equals(s.getApprovalStatus())) {
                continue;
            }
            java.util.Map<String, BigDecimal> cd = parseCumulativeData(s.getCumulativeData());
            // 历史月只保留「累计至该月」的快照，取最大 month 的快照作为累计基础
            // 简化：直接用最近一个月（最大 month）的 cumulativeData 作为累计基础
        }
        if (!history.isEmpty()) {
            ErpHrSalary latest = null;
            for (ErpHrSalary s : history) {
                if (ErpHrConstants.APPROVAL_VOID.equals(s.getApprovalStatus())) {
                    continue;
                }
                if (latest == null || (s.getMonth() != null && s.getMonth() > latest.getMonth())) {
                    latest = s;
                }
            }
            if (latest != null && latest.getCumulativeData() != null) {
                result = parseCumulativeData(latest.getCumulativeData());
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    static java.util.Map<String, BigDecimal> parseCumulativeData(String json) {
        java.util.Map<String, BigDecimal> result = new java.util.HashMap<>();
        if (json == null || json.trim().isEmpty()) {
            return result;
        }
        try {
            Object parsed = io.nop.core.lang.json.JsonTool.parseNonStrict(json);
            if (parsed instanceof java.util.Map) {
                java.util.Map<String, Object> raw = (java.util.Map<String, Object>) parsed;
                for (java.util.Map.Entry<String, Object> e : raw.entrySet()) {
                    if (e.getValue() instanceof Number) {
                        result.put(e.getKey(), new BigDecimal(e.getValue().toString()));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    ErpHrTaxConfig findTaxConfig(int year) {
        IEntityDao<ErpHrTaxConfig> dao = daoProvider.daoFor(ErpHrTaxConfig.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("year", year));
        q.setLimit(1);
        List<ErpHrTaxConfig> list = dao.findAllByQuery(q);
        if (list.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_TAX_CONFIG_NOT_FOUND).param(ErpHrErrors.ARG_YEAR, year);
        }
        return list.get(0);
    }

    BigDecimal sumSpecialDeduction(Long employeeId, int year, int month) {
        IEntityDao<ErpHrTaxSpecialDeduction> dao = daoProvider.daoFor(ErpHrTaxSpecialDeduction.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("employeeId", employeeId), eq("year", year), eq("month", month)));
        List<ErpHrTaxSpecialDeduction> items = dao.findAllByQuery(q);
        BigDecimal total = BigDecimal.ZERO;
        for (ErpHrTaxSpecialDeduction d : items) {
            if (Boolean.TRUE.equals(d.getVerified()) && d.getMonthlyAmount() != null) {
                total = total.add(d.getMonthlyAmount());
            }
        }
        return total;
    }

    static TaxBracket resolveBracket(List<TaxBracket> brackets, BigDecimal cumulativeTaxableIncome) {
        TaxBracket selected = brackets.get(0);
        for (TaxBracket b : brackets) {
            if (cumulativeTaxableIncome.compareTo(b.getRangeUpperLimit()) > 0) {
                continue;
            }
            selected = b;
            break;
        }
        return selected;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
