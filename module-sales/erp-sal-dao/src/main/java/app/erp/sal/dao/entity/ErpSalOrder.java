package app.erp.sal.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.sal.dao.constants.ErpSalDocStatus;
import app.erp.sal.dao.entity._gen._ErpSalOrder;

import java.util.Objects;


@BizObjName("ErpSalOrder")
public class ErpSalOrder extends _ErpSalOrder{

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
