
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.dao.entity.ErpPrjProject;

@BizModel("ErpPrjProject")
public class ErpPrjProjectBizModel extends CrudBizModel<ErpPrjProject> implements IErpPrjProjectBiz{
    public ErpPrjProjectBizModel(){
        setEntityName(ErpPrjProject.class.getName());
    }
}
