
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherLineBiz;
import app.erp.fin.dao.entity.ErpFinVoucherLine;

import java.util.List;

@BizModel("ErpFinVoucherLine")
public class ErpFinVoucherLineBizModel extends CrudBizModel<ErpFinVoucherLine> implements IErpFinVoucherLineBiz{
    public ErpFinVoucherLineBizModel(){
        setEntityName(ErpFinVoucherLine.class.getName());
    }

    // subjectCode/subjectName 为凭证行已持久化的冗余列，无需派生。

}
