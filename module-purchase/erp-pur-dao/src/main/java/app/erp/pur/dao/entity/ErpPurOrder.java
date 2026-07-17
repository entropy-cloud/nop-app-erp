package app.erp.pur.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity._gen._ErpPurOrder;

import java.util.Objects;


@BizObjName("ErpPurOrder")
public class ErpPurOrder extends _ErpPurOrder{

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
