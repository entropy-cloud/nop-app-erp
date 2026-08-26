
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.pur.biz.IErpPurReturnLineBiz;
import app.erp.pur.dao.entity.ErpPurReturnLine;

import java.util.List;

@BizModel("ErpPurReturnLine")
public class ErpPurReturnLineBizModel extends AbstractErpCrudBizModel<ErpPurReturnLine> implements IErpPurReturnLineBiz{
    public ErpPurReturnLineBizModel(){
        setEntityName(ErpPurReturnLine.class.getName());
    }

}
