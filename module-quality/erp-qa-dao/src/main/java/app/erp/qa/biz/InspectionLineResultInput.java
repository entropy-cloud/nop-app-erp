package app.erp.qa.biz;

import java.util.List;

/**
 * 质检单行级结果录入入参。每行可带 lineNo（行号）或 lineId（行主键）定位，measuredValue 为实测值文本，
 * result 为可选的人工覆盖结果码（erp-qa/inspection-result；不传则由行级评测自动判定）。
 */
public class InspectionLineResultInput {
    private Long lineId;
    private Integer lineNo;
    private String measuredValue;
    private String result;

    public Long getLineId() {
        return lineId;
    }

    public void setLineId(Long lineId) {
        this.lineId = lineId;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getMeasuredValue() {
        return measuredValue;
    }

    public void setMeasuredValue(String measuredValue) {
        this.measuredValue = measuredValue;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public static List<InspectionLineResultInput> of() {
        return new java.util.ArrayList<>();
    }
}
