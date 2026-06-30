
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtApprovalMatrixBiz;
import app.erp.contract.dao.entity.ErpCtApprovalMatrix;

@BizModel("ErpCtApprovalMatrix")
public class ErpCtApprovalMatrixBizModel extends CrudBizModel<ErpCtApprovalMatrix> implements IErpCtApprovalMatrixBiz{
    public ErpCtApprovalMatrixBizModel(){
        setEntityName(ErpCtApprovalMatrix.class.getName());
    }
}
