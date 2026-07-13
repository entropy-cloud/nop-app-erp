
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjProjectTypeBiz;
import app.erp.prj.dao.entity.ErpPrjProjectType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjProjectType")
public class ErpPrjProjectTypeBizModel extends CrudBizModel<ErpPrjProjectType> implements IErpPrjProjectTypeBiz{
    public ErpPrjProjectTypeBizModel(){
        setEntityName(ErpPrjProjectType.class.getName());
    }

    @BizLoader(forType = ErpPrjProjectType.class)
    public List<String> defaultSubjectName(@ContextSource List<ErpPrjProjectType> types) {
        orm().batchLoadProps(types, Collections.singleton("defaultSubject"));
        List<String> result = new ArrayList<>(types.size());
        for (ErpPrjProjectType type : types) {
            result.add(type.getDefaultSubject() != null ? type.getDefaultSubject().getName() : null);
        }
        return result;
    }
}
