
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurRequisitionLineBiz;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;

import java.util.List;

@BizModel("ErpPurRequisitionLine")
public class ErpPurRequisitionLineBizModel extends CrudBizModel<ErpPurRequisitionLine> implements IErpPurRequisitionLineBiz{
    public ErpPurRequisitionLineBizModel(){
        setEntityName(ErpPurRequisitionLine.class.getName());
    }

}
