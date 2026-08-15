package app.erp.fin.service.posting;

import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.exceptions.NopException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 科目分摊（GL Distribution）FactsValidator 实现（RC-R1.41 / P1-RC-001，L1 UC-FIN-04/15）。
 *
 * <p>在凭证写库前（Provider 产出分录后、借贷平衡校验前，见 {@link ErpFinPostingProcessor#generateFacts}）
 * 对命中分摊规则的分录行按比例拆行：
 * <ul>
 *   <li>命中规则（源 subjectCode/costCenterId 匹配 + 生效窗口 + 启用态）→ 拆成多条目标行
 *       （复制原 fact 字段 + 改 costCenterId + 金额 = 原金额 × percent/100，scale 4 HALF_UP，
 *       末行补差保证 Σ 拆分行金额 == 原行金额，平衡保持）；</li>
 *   <li>{@code amountSource}/{@code amountFunctional} 同比例拆分（仅原值非 null 时设置，
 *       保持 null 回退语义——persistVoucher 对 null 回退到 amount，单币种向后兼容）；</li>
 *   <li>规则 Σpercent 超出精度容忍（≠ 100）→ 抛 {@link ErpFinErrors#ERR_GL_DISTRIBUTION_PERCENT_SUM}
 *       拒绝过账（L1 断言逐字：若 Σ percent != 100 → 抛异常拒绝过账）。</li>
 * </ul>
 *
 * <p>规则载体 = Bean 内静态规则表（D1 裁决：不物化 {@code ErpFinGlDistribution} ORM 实体，2026-08-12 裁决
 * B 类；生产默认空规则表 = 零行为变更，分摊未启用时主路径不受影响；下游经 beans.xml property 或
 * Delta 同名 bean 覆盖注入规则）。
 *
 * <p>{@link #getOrder()} = 100（较高值，确保在其他 Validator 之后执行——L1 UC-FIN-15「getOrder() 较高」
 * + L2 {@code cost-center.md §业务规则 2}）。
 */
public class ErpFinGlDistributionValidator implements IErpFinFactsValidator {

    /** 执行顺序：较高值确保在其他 Validator 之后（L1 UC-FIN-15 + cost-center.md §业务规则 2）。 */
    public static final int ORDER = 100;

    /** Σpercent 校验精度容忍（百分数之和的浮点误差）。 */
    static final BigDecimal PERCENT_EPSILON = new BigDecimal("0.000001");

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private List<ErpFinGlDistributionRule> rules = Collections.emptyList();

    public void setRules(List<ErpFinGlDistributionRule> rules) {
        this.rules = rules == null ? Collections.emptyList() : rules;
    }

    public List<ErpFinGlDistributionRule> getRules() {
        return rules;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public List<VoucherFact> validate(List<VoucherFact> facts, AcctDocContext ctx) {
        if (facts == null || facts.isEmpty() || rules.isEmpty()) {
            return facts;
        }
        List<VoucherFact> result = new ArrayList<>(facts.size());
        for (VoucherFact fact : facts) {
            ErpFinGlDistributionRule rule = matchRule(fact, ctx);
            if (rule == null) {
                result.add(fact);
            } else {
                result.addAll(split(fact, rule));
            }
        }
        return result;
    }

    /** 匹配规则：启用态 + 生效窗口 + 源键（任一非空源键，AND 语义）。 */
    private ErpFinGlDistributionRule matchRule(VoucherFact fact, AcctDocContext ctx) {
        for (ErpFinGlDistributionRule rule : rules) {
            if (!rule.isActive()) {
                continue;
            }
            if (!isInWindow(rule, ctx == null ? null : ctx.getVoucherDate())) {
                continue;
            }
            if (rule.hasSourceKey() && matchesSource(rule, fact)) {
                return rule;
            }
        }
        return null;
    }

    private boolean matchesSource(ErpFinGlDistributionRule rule, VoucherFact fact) {
        if (rule.getSourceSubjectCode() != null && !rule.getSourceSubjectCode().equals(fact.getSubjectCode())) {
            return false;
        }
        if (rule.getSourceCostCenterId() != null && !rule.getSourceCostCenterId().equals(fact.getCostCenterId())) {
            return false;
        }
        return true;
    }

    private boolean isInWindow(ErpFinGlDistributionRule rule, LocalDate date) {
        if (date == null) {
            return true;
        }
        if (rule.getValidFrom() != null && date.isBefore(rule.getValidFrom())) {
            return false;
        }
        if (rule.getValidTo() != null && date.isAfter(rule.getValidTo())) {
            return false;
        }
        return true;
    }

    /** 按规则拆行：金额按 percent 比例（scale 4 HALF_UP），末行补差保证 Σ == 原行金额。 */
    private List<VoucherFact> split(VoucherFact fact, ErpFinGlDistributionRule rule) {
        List<GlDistributionTarget> targets = rule.getTargets();
        BigDecimal sum = percentSum(targets);
        if (sum.subtract(HUNDRED).abs().compareTo(PERCENT_EPSILON) > 0) {
            throw new NopException(ErpFinErrors.ERR_GL_DISTRIBUTION_PERCENT_SUM)
                    .param(ErpFinErrors.ARG_RULE_CODE, rule.getRuleCode())
                    .param(ErpFinErrors.ARG_PERCENT_SUM, sum.stripTrailingZeros().toPlainString());
        }

        BigDecimal amount = nz(fact.getAmount());
        BigDecimal amountSource = nz(fact.getAmountSource());
        BigDecimal amountFunctional = nz(fact.getAmountFunctional());

        List<VoucherFact> result = new ArrayList<>(targets.size());
        BigDecimal allocated = BigDecimal.ZERO;
        BigDecimal allocatedSource = BigDecimal.ZERO;
        BigDecimal allocatedFunctional = BigDecimal.ZERO;

        for (int i = 0; i < targets.size(); i++) {
            GlDistributionTarget target = targets.get(i);
            boolean last = (i == targets.size() - 1);
            VoucherFact copy = copyOf(fact);
            copy.setCostCenterId(target.getTargetCostCenterId());

            if (last) {
                // 末行补差：Σ 拆分行金额 == 原行金额（平衡保持，对齐 assertBalanced 语义）
                copy.setAmount(amount.subtract(allocated));
                if (fact.getAmountSource() != null) {
                    copy.setAmountSource(amountSource.subtract(allocatedSource));
                }
                if (fact.getAmountFunctional() != null) {
                    copy.setAmountFunctional(amountFunctional.subtract(allocatedFunctional));
                }
            } else {
                BigDecimal ratio = nz(target.getPercent()).divide(HUNDRED, 8, RoundingMode.HALF_UP);
                BigDecimal part = amount.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
                copy.setAmount(part);
                if (fact.getAmountSource() != null) {
                    BigDecimal partSource = amountSource.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
                    copy.setAmountSource(partSource);
                    allocatedSource = allocatedSource.add(partSource);
                }
                if (fact.getAmountFunctional() != null) {
                    BigDecimal partFunctional = amountFunctional.multiply(ratio).setScale(4, RoundingMode.HALF_UP);
                    copy.setAmountFunctional(partFunctional);
                    allocatedFunctional = allocatedFunctional.add(partFunctional);
                }
                allocated = allocated.add(part);
            }
            result.add(copy);
        }
        return result;
    }

    private BigDecimal percentSum(List<GlDistributionTarget> targets) {
        BigDecimal sum = BigDecimal.ZERO;
        for (GlDistributionTarget target : targets) {
            sum = sum.add(nz(target == null ? null : target.getPercent()));
        }
        return sum;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static VoucherFact copyOf(VoucherFact fact) {
        VoucherFact copy = new VoucherFact();
        copy.setSubjectCode(fact.getSubjectCode());
        copy.setSubjectId(fact.getSubjectId());
        copy.setSubjectName(fact.getSubjectName());
        copy.setDcDirection(fact.getDcDirection());
        copy.setAmountKey(fact.getAmountKey());
        copy.setAccountKey(fact.getAccountKey());
        copy.setMemo(fact.getMemo());
        copy.setOrgId(fact.getOrgId());
        copy.setPartnerId(fact.getPartnerId());
        copy.setDepartmentId(fact.getDepartmentId());
        copy.setProjectId(fact.getProjectId());
        copy.setWarehouseId(fact.getWarehouseId());
        copy.setMaterialId(fact.getMaterialId());
        copy.setBusinessType(fact.getBusinessType());
        return copy;
    }
}
