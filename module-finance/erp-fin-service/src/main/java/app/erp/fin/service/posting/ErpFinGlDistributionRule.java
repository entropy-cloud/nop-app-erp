package app.erp.fin.service.posting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * GlDistribution 科目分摊规则（RC-R1.41 / P1-RC-001，L1 UC-FIN-04/15）。
 *
 * <p>规则载体 = Bean 内静态规则表（2026-08-12 裁决 B 类：不物化 {@code ErpFinGlDistribution} ORM 实体，
 * 见 {@code cost-center.md §ErpFinGlDistribution} 裁决注记）。由 {@link ErpFinGlDistributionValidator}
 * 经 setter 注入（生产默认空表 = 零行为变更；下游经 beans.xml property 或 Delta 同名 bean 覆盖注入）。
 *
 * <p>匹配语义：{@code sourceSubjectCode}/{@code sourceCostCenterId} 为源匹配键（任一非空，二者同时非空时为
 * AND 语义）；命中规则的分录行按 {@code targets} 比例拆成多条目标行。
 */
public class ErpFinGlDistributionRule {

    private String ruleCode;
    /** 源科目编码匹配键（null = 不限定科目）。 */
    private String sourceSubjectCode;
    /** 源成本中心匹配键（null = 不限定成本中心）。 */
    private String sourceCostCenterId;
    /** 生效起始日期（null = 开放）。 */
    private LocalDate validFrom;
    /** 生效截止日期（null = 开放）。 */
    private LocalDate validTo;
    /** 启用态（false 的规则不参与匹配）。 */
    private boolean isActive = true;
    /** 目标行（targetCostCenterId + percent），Σpercent 必须 = 100。 */
    private List<GlDistributionTarget> targets = Collections.emptyList();

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getSourceSubjectCode() {
        return sourceSubjectCode;
    }

    public void setSourceSubjectCode(String sourceSubjectCode) {
        this.sourceSubjectCode = sourceSubjectCode;
    }

    public String getSourceCostCenterId() {
        return sourceCostCenterId;
    }

    public void setSourceCostCenterId(String sourceCostCenterId) {
        this.sourceCostCenterId = sourceCostCenterId;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public List<GlDistributionTarget> getTargets() {
        return targets;
    }

    public void setTargets(List<GlDistributionTarget> targets) {
        this.targets = targets == null ? Collections.emptyList() : targets;
    }

    /** 是否携带至少一个源匹配键（无源键的规则不匹配任何分录行）。 */
    public boolean hasSourceKey() {
        return sourceSubjectCode != null || sourceCostCenterId != null;
    }
}
