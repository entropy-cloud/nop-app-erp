package app.erp.fin.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.fin.dao.constants.ErpFinDocStatus;
import app.erp.fin.dao.entity._gen._ErpFinBadDebt;

import java.util.Objects;


@BizObjName("ErpFinBadDebt")
public class ErpFinBadDebt extends _ErpFinBadDebt{

    public boolean isApproved() {
        return Objects.equals(getApprovalStatus(), ErpFinDocStatus.APPROVE_STATUS_APPROVED);
    }

}
