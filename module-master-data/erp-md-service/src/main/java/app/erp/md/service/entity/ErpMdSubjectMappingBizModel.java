
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.md.biz.IErpMdSubjectMappingBiz;
import app.erp.md.dao.entity.ErpMdSubjectMapping;

@BizModel("ErpMdSubjectMapping")
public class ErpMdSubjectMappingBizModel extends CrudBizModel<ErpMdSubjectMapping> implements IErpMdSubjectMappingBiz{
    public ErpMdSubjectMappingBizModel(){
        setEntityName(ErpMdSubjectMapping.class.getName());
    }
}
