package app.erp.pur.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity._gen._ErpPurReturn;

import java.util.Objects;


@BizObjName("ErpPurReturn")
public class ErpPurReturn extends _ErpPurReturn{

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
