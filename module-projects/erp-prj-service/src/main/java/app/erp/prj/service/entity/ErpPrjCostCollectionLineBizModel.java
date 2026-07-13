
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjCostCollectionLineBiz;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjCostCollectionLine")
public class ErpPrjCostCollectionLineBizModel extends CrudBizModel<ErpPrjCostCollectionLine> implements IErpPrjCostCollectionLineBiz{
    public ErpPrjCostCollectionLineBizModel(){
        setEntityName(ErpPrjCostCollectionLine.class.getName());
    }

    @BizLoader(forType = ErpPrjCostCollectionLine.class)
    public List<String> costCollectionCode(@ContextSource List<ErpPrjCostCollectionLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("costCollection"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjCostCollectionLine line : lines) {
            result.add(line.getCostCollection() != null ? line.getCostCollection().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjCostCollectionLine.class)
    public List<String> subjectName(@ContextSource List<ErpPrjCostCollectionLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjCostCollectionLine line : lines) {
            result.add(line.getSubject() != null ? line.getSubject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjCostCollectionLine.class)
    public List<String> taskName(@ContextSource List<ErpPrjCostCollectionLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("task"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjCostCollectionLine line : lines) {
            result.add(line.getTask() != null ? line.getTask().getTitle() : null);
        }
        return result;
    }
}
