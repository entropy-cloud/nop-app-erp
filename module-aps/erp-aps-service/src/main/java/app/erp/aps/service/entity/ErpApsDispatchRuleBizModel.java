
package app.erp.aps.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.aps.biz.IErpApsDispatchRuleBiz;
import app.erp.aps.dao.entity.ErpApsDispatchRule;

@BizModel("ErpApsDispatchRule")
public class ErpApsDispatchRuleBizModel extends CrudBizModel<ErpApsDispatchRule> implements IErpApsDispatchRuleBiz{
    public ErpApsDispatchRuleBizModel(){
        setEntityName(ErpApsDispatchRule.class.getName());
    }

}
