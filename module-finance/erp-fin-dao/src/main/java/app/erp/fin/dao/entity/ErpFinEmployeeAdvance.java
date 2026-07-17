package app.erp.fin.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.fin.dao.constants.ErpFinDocStatus;
import app.erp.fin.dao.entity._gen._ErpFinEmployeeAdvance;

import java.util.Objects;


@BizObjName("ErpFinEmployeeAdvance")
public class ErpFinEmployeeAdvance extends _ErpFinEmployeeAdvance{

    public boolean isApproved() {
        return Objects.equals(getApproveStatus(), ErpFinDocStatus.APPROVE_STATUS_APPROVED);
    }

    public boolean isRejected() {
        return Objects.equals(getApproveStatus(), ErpFinDocStatus.APPROVE_STATUS_REJECTED);
    }

    public boolean isCancelled() {
        return Objects.equals(getDocStatus(), ErpFinDocStatus.DOC_STATUS_CANCELLED);
    }

}
