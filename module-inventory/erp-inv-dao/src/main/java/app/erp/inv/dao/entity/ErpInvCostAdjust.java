package app.erp.inv.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.inv.dao.constants.ErpInvDocStatus;
import app.erp.inv.dao.entity._gen._ErpInvCostAdjust;

import java.util.Objects;


@BizObjName("ErpInvCostAdjust")
public class ErpInvCostAdjust extends _ErpInvCostAdjust{

    public boolean isApproved() {
        return Objects.equals(getApproveStatus(), ErpInvDocStatus.APPROVE_STATUS_APPROVED);
    }

    public boolean isRejected() {
        return Objects.equals(getApproveStatus(), ErpInvDocStatus.APPROVE_STATUS_REJECTED);
    }

    public boolean isCancelled() {
        return Objects.equals(getDocStatus(), ErpInvDocStatus.DOC_STATUS_CANCELLED);
    }

}
