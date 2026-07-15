
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsCannedCategoryBiz;
import app.erp.cs.dao.entity.ErpCsCannedCategory;
import java.util.List;

@BizModel("ErpCsCannedCategory")
public class ErpCsCannedCategoryBizModel extends CrudBizModel<ErpCsCannedCategory> implements IErpCsCannedCategoryBiz{
    public ErpCsCannedCategoryBizModel(){
        setEntityName(ErpCsCannedCategory.class.getName());
    }

    

}
