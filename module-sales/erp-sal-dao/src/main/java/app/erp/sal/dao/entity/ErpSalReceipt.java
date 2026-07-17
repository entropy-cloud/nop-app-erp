package app.erp.sal.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.sal.dao.constants.ErpSalDocStatus;
import app.erp.sal.dao.entity._gen._ErpSalReceipt;

import java.util.Objects;


@BizObjName("ErpSalReceipt")
public class ErpSalReceipt extends _ErpSalReceipt{

    public boolean isApproved() {
        return Objects.equals(getApproveStatus(), ErpSalDocStatus.APPROVE_STATUS_APPROVED);
    }

    public boolean isRejected() {
        return Objects.equals(getApproveStatus(), ErpSalDocStatus.APPROVE_STATUS_REJECTED);
    }

    public boolean isCancelled() {
        return Objects.equals(getDocStatus(), ErpSalDocStatus.DOC_STATUS_CANCELLED);
    }

}
