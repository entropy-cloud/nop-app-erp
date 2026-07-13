
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjProjectSettlementLineBiz;
import app.erp.prj.dao.entity.ErpPrjProjectSettlementLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjProjectSettlementLine")
public class ErpPrjProjectSettlementLineBizModel extends CrudBizModel<ErpPrjProjectSettlementLine> implements IErpPrjProjectSettlementLineBiz{
    public ErpPrjProjectSettlementLineBizModel(){
        setEntityName(ErpPrjProjectSettlementLine.class.getName());
    }

    @BizLoader(forType = ErpPrjProjectSettlementLine.class)
    public List<String> settlementCode(@ContextSource List<ErpPrjProjectSettlementLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("settlement"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjProjectSettlementLine line : lines) {
            result.add(line.getSettlement() != null ? line.getSettlement().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjProjectSettlementLine.class)
    public List<String> subjectName(@ContextSource List<ErpPrjProjectSettlementLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjProjectSettlementLine line : lines) {
            result.add(line.getSubject() != null ? line.getSubject().getName() : null);
        }
        return result;
    }
}
