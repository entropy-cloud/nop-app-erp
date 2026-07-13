
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjActivityTypeBiz;
import app.erp.prj.dao.entity.ErpPrjActivityType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjActivityType")
public class ErpPrjActivityTypeBizModel extends CrudBizModel<ErpPrjActivityType> implements IErpPrjActivityTypeBiz{
    public ErpPrjActivityTypeBizModel(){
        setEntityName(ErpPrjActivityType.class.getName());
    }

    @BizLoader(forType = ErpPrjActivityType.class)
    public List<String> subjectName(@ContextSource List<ErpPrjActivityType> types) {
        orm().batchLoadProps(types, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(types.size());
        for (ErpPrjActivityType type : types) {
            result.add(type.getSubject() != null ? type.getSubject().getName() : null);
        }
        return result;
    }
}
