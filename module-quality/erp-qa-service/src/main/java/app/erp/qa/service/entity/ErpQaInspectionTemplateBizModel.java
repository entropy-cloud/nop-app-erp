
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaInspectionTemplateBiz;
import app.erp.qa.dao.entity.ErpQaInspectionTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpQaInspectionTemplate")
public class ErpQaInspectionTemplateBizModel extends CrudBizModel<ErpQaInspectionTemplate> implements IErpQaInspectionTemplateBiz{
    public ErpQaInspectionTemplateBizModel(){
        setEntityName(ErpQaInspectionTemplate.class.getName());
    }

    @BizLoader(forType = ErpQaInspectionTemplate.class)
    public List<String> materialName(@ContextSource List<ErpQaInspectionTemplate> list) {
        orm().batchLoadProps(list, Collections.singleton("material"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaInspectionTemplate entity : list) {
            result.add(entity.getMaterial() != null ? entity.getMaterial().getName() : null);
        }
        return result;
    }
}
