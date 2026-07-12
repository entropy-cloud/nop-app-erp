
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurReturnLineBiz;
import app.erp.pur.dao.entity.ErpPurReturnLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPurReturnLine")
public class ErpPurReturnLineBizModel extends CrudBizModel<ErpPurReturnLine> implements IErpPurReturnLineBiz{
    public ErpPurReturnLineBizModel(){
        setEntityName(ErpPurReturnLine.class.getName());
    }

    @BizLoader(forType = ErpPurReturnLine.class)
    public List<String> materialName(@ContextSource List<ErpPurReturnLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPurReturnLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
