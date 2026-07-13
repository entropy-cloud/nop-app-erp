
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaInspectionLineBiz;
import app.erp.qa.dao.entity.ErpQaInspectionLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpQaInspectionLine")
public class ErpQaInspectionLineBizModel extends CrudBizModel<ErpQaInspectionLine> implements IErpQaInspectionLineBiz{
    public ErpQaInspectionLineBizModel(){
        setEntityName(ErpQaInspectionLine.class.getName());
    }

    @BizLoader(forType = ErpQaInspectionLine.class)
    public List<String> inspectionCode(@ContextSource List<ErpQaInspectionLine> list) {
        orm().batchLoadProps(list, Collections.singleton("inspection"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaInspectionLine entity : list) {
            result.add(entity.getInspection() != null ? entity.getInspection().getCode() : null);
        }
        return result;
    }
}
