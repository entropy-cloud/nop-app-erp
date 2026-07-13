
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtApprovalMatrixBiz;
import app.erp.contract.dao.entity.ErpCtApprovalMatrix;

@BizModel("ErpCtApprovalMatrix")
public class ErpCtApprovalMatrixBizModel extends CrudBizModel<ErpCtApprovalMatrix> implements IErpCtApprovalMatrixBiz{
    public ErpCtApprovalMatrixBizModel(){
        setEntityName(ErpCtApprovalMatrix.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtApprovalMatrix.class)
    public List<String> orgName(@ContextSource List<ErpCtApprovalMatrix> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtApprovalMatrix row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
