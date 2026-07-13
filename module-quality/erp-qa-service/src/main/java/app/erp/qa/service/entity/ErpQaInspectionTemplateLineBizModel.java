
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaInspectionTemplateLineBiz;
import app.erp.qa.dao.entity.ErpQaInspectionTemplateLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpQaInspectionTemplateLine")
public class ErpQaInspectionTemplateLineBizModel extends CrudBizModel<ErpQaInspectionTemplateLine> implements IErpQaInspectionTemplateLineBiz{
    public ErpQaInspectionTemplateLineBizModel(){
        setEntityName(ErpQaInspectionTemplateLine.class.getName());
    }

    @BizLoader(forType = ErpQaInspectionTemplateLine.class)
    public List<String> templateCode(@ContextSource List<ErpQaInspectionTemplateLine> list) {
        orm().batchLoadProps(list, Collections.singleton("template"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaInspectionTemplateLine entity : list) {
            result.add(entity.getTemplate() != null ? entity.getTemplate().getCode() : null);
        }
        return result;
    }
}
