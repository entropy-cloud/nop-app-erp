package app.erp.qa.service.entity;

import java.math.BigDecimal;

/** 模板行规格（复制到质检单行用）。模板行无 parameterId，质检单行 parameterId 留空。 */
public final class TemplateLineSpec {
    private final String parameterName;
    private final BigDecimal specMin;
    private final BigDecimal specMax;
    private final String unit;

    public TemplateLineSpec(String parameterName, BigDecimal specMin, BigDecimal specMax, String unit) {
        this.parameterName = parameterName;
        this.specMin = specMin;
        this.specMax = specMax;
        this.unit = unit;
    }

    public String getParameterName() {
        return parameterName;
    }

    public BigDecimal getSpecMin() {
        return specMin;
    }

    public BigDecimal getSpecMax() {
        return specMax;
    }

    public String getUnit() {
        return unit;
    }
}
