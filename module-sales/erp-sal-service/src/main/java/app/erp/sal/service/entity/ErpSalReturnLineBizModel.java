
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalReturnLineBiz;
import app.erp.sal.dao.entity.ErpSalReturnLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpSalReturnLine")
public class ErpSalReturnLineBizModel extends CrudBizModel<ErpSalReturnLine> implements IErpSalReturnLineBiz{
    public ErpSalReturnLineBizModel(){
        setEntityName(ErpSalReturnLine.class.getName());
    }

    @BizLoader(forType = ErpSalReturnLine.class)
    public List<String> materialName(@ContextSource List<ErpSalReturnLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpSalReturnLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
