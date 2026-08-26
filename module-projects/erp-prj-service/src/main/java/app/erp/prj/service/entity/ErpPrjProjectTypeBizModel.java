
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.prj.biz.IErpPrjProjectTypeBiz;
import app.erp.prj.dao.entity.ErpPrjProjectType;

import java.util.List;

@BizModel("ErpPrjProjectType")
public class ErpPrjProjectTypeBizModel extends AbstractErpCrudBizModel<ErpPrjProjectType> implements IErpPrjProjectTypeBiz{
    public ErpPrjProjectTypeBizModel(){
        setEntityName(ErpPrjProjectType.class.getName());
    }

}
