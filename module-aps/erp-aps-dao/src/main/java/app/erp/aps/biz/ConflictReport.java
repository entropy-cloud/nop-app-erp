package app.erp.aps.biz;

import io.nop.api.core.annotations.data.DataBean;

/**
 * 排产冲突报告条目（无法排定的工序 + 原因）。{@code @DataBean} 暴露给 GraphQL。
 */
@DataBean
public class ConflictReport {

    private Long operationOrderId;
    private String code;
    private String reason;

    public ConflictReport() {
    }

    public ConflictReport(Long operationOrderId, String code, String reason) {
        this.operationOrderId = operationOrderId;
        this.code = code;
        this.reason = reason;
    }

    public Long getOperationOrderId() {
        return operationOrderId;
    }

    public void setOperationOrderId(Long operationOrderId) {
        this.operationOrderId = operationOrderId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
