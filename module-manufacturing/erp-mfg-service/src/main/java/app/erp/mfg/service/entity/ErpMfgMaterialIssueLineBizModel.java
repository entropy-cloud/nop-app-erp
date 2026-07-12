
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgMaterialIssueLineBiz;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgMaterialIssueLine")
public class ErpMfgMaterialIssueLineBizModel extends CrudBizModel<ErpMfgMaterialIssueLine> implements IErpMfgMaterialIssueLineBiz{
    public ErpMfgMaterialIssueLineBizModel(){
        setEntityName(ErpMfgMaterialIssueLine.class.getName());
    }

    @BizLoader(forType = ErpMfgMaterialIssueLine.class)
    public List<String> materialName(@ContextSource List<ErpMfgMaterialIssueLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgMaterialIssueLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
