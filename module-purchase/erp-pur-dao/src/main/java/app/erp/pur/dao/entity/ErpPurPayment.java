package app.erp.pur.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity._gen._ErpPurPayment;

import java.util.Objects;


@BizObjName("ErpPurPayment")
public class ErpPurPayment extends _ErpPurPayment{

    public boolean isApproved() {
        return Objects.equals(getApproveStatus(), ErpPurDocStatus.APPROVE_STATUS_APPROVED);
    }

    public boolean isRejected() {
        return Objects.equals(getApproveStatus(), ErpPurDocStatus.APPROVE_STATUS_REJECTED);
    }

    public boolean isCancelled() {
        return Objects.equals(getDocStatus(), ErpPurDocStatus.DOC_STATUS_CANCELLED);
    }

}
