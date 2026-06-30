
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsKnowledgeBaseBiz;
import app.erp.cs.dao.entity.ErpCsKnowledgeBase;

@BizModel("ErpCsKnowledgeBase")
public class ErpCsKnowledgeBaseBizModel extends CrudBizModel<ErpCsKnowledgeBase> implements IErpCsKnowledgeBaseBiz{
    public ErpCsKnowledgeBaseBizModel(){
        setEntityName(ErpCsKnowledgeBase.class.getName());
    }
}
