package app.erp.ast.dao.entity;

import io.nop.api.core.annotations.biz.BizObjName;
import app.erp.ast.dao.constants.ErpAstDocStatus;
import app.erp.ast.dao.entity._gen._ErpAstDisposal;

import java.util.Objects;


@BizObjName("ErpAstDisposal")
public class ErpAstDisposal extends _ErpAstDisposal{

    public boolean isApproved() {
        return Objects.equals(getApproveStatus(), ErpAstDocStatus.APPROVE_STATUS_APPROVED);
    }

    public boolean isRejected() {
        return Objects.equals(getApproveStatus(), ErpAstDocStatus.APPROVE_STATUS_REJECTED);
    }

    public boolean isCancelled() {
        return Objects.equals(getDocStatus(), ErpAstDocStatus.DOC_STATUS_CANCELLED);
    }

}
