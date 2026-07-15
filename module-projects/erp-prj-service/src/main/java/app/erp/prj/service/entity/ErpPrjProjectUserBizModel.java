
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjProjectUserBiz;
import app.erp.prj.dao.entity.ErpPrjProjectUser;

import java.util.List;

@BizModel("ErpPrjProjectUser")
public class ErpPrjProjectUserBizModel extends CrudBizModel<ErpPrjProjectUser> implements IErpPrjProjectUserBiz{
    public ErpPrjProjectUserBizModel(){
        setEntityName(ErpPrjProjectUser.class.getName());
    }

}
