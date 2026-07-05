package app.erp.qa.service.entity;

import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Objects;

import java.math.BigDecimal;
import java.util.List;

/**
 * 质检单行级评测 + 结果汇总器。权威：{@code docs/design/quality/state-machine.md §适用对象一}
 * + 计划 Task Route Decision（行级评测规则）。
 *
 * <p>行级评测：{@code specMin/specMax} vs {@code measuredValue} 数值比较（三者均为 VARCHAR，解析为 BigDecimal 比较）；
 * 解析失败视为不合格。规格上下限均空时按「实测值非空即合格」（外观类目测项，无数值规格）。
 *
 * <p>汇总：全行 ACCEPTED → ACCEPTED；含 REJECTED 且 allowConcession → CONDITIONAL；含 REJECTED 且未让步 → REJECTED。
 */
public final class InspectionResultEvaluator {

    private InspectionResultEvaluator() {
    }

    /** 评测单行结果：实测值落在 [specMin, specMax] 内 → ACCEPTED，否则 REJECTED。 */
    public static String evaluateLine(ErpQaInspectionLine line) {
        String measuredRaw = line.getMeasuredValue();
        BigDecimal measured = parseDecimal(measuredRaw);
        BigDecimal min = line.getSpecMin();
        BigDecimal max = line.getSpecMax();

        boolean hasMin = min != null;
        boolean hasMax = max != null;
        if (!hasMin && !hasMax) {
            // 非数值规格（外观类）：实测值非空即合格
            return measuredRaw != null
                    ? ErpQaConstants.INSPECTION_RESULT_ACCEPTED
                    : ErpQaConstants.INSPECTION_RESULT_REJECTED;
        }
        if (measured == null) {
            return ErpQaConstants.INSPECTION_RESULT_REJECTED;
        }
        if (hasMin && measured.compareTo(min) < 0) {
            return ErpQaConstants.INSPECTION_RESULT_REJECTED;
        }
        if (hasMax && measured.compareTo(max) > 0) {
            return ErpQaConstants.INSPECTION_RESULT_REJECTED;
        }
        return ErpQaConstants.INSPECTION_RESULT_ACCEPTED;
    }

    private static BigDecimal parseDecimal(String value) {
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 汇总全部行结果为质检单结果。
     *
     * @param lines           质检单行
     * @param allowConcession 是否允许让步接收（部分不合格 + 让步审批 → CONDITIONAL）
     * @param inspectionCode  质检单编码（异常上下文）
     * @return 汇总结果（ACCEPTED / CONDITIONAL / REJECTED）
     */
    public static String aggregate(List<ErpQaInspectionLine> lines, boolean allowConcession, String inspectionCode) {
        if (lines == null || lines.isEmpty()) {
            throw new NopException(ErpQaErrors.ERR_INSPECTION_LINES_EMPTY)
                    .param(ErpQaErrors.ARG_INSPECTION_CODE, inspectionCode);
        }
        boolean anyRejected = false;
        for (ErpQaInspectionLine line : lines) {
            String lineResult = line.getResult() == null
                    ? evaluateLine(line)
                    : line.getResult();
            if (Objects.equals(lineResult, ErpQaConstants.INSPECTION_RESULT_REJECTED)) {
                anyRejected = true;
            }
        }
        if (!anyRejected) {
            return ErpQaConstants.INSPECTION_RESULT_ACCEPTED;
        }
        return allowConcession
                ? ErpQaConstants.INSPECTION_RESULT_CONDITIONAL
                : ErpQaConstants.INSPECTION_RESULT_REJECTED;
    }
}
