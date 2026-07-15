
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurOrderLineBiz;
import app.erp.pur.dao.entity.ErpPurOrderLine;

import java.util.List;

@BizModel("ErpPurOrderLine")
public class ErpPurOrderLineBizModel extends CrudBizModel<ErpPurOrderLine> implements IErpPurOrderLineBiz{
    public ErpPurOrderLineBizModel(){
        setEntityName(ErpPurOrderLine.class.getName());
    }

}
