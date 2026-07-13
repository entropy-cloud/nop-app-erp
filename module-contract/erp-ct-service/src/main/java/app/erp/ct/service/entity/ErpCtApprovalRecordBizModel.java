
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtApprovalRecordBiz;
import app.erp.contract.dao.entity.ErpCtApprovalRecord;

@BizModel("ErpCtApprovalRecord")
public class ErpCtApprovalRecordBizModel extends CrudBizModel<ErpCtApprovalRecord> implements IErpCtApprovalRecordBiz{
    public ErpCtApprovalRecordBizModel(){
        setEntityName(ErpCtApprovalRecord.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtApprovalRecord.class)
    public List<String> contractName(@ContextSource List<ErpCtApprovalRecord> rows) {
        orm().batchLoadProps(rows, Collections.singleton("contract"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtApprovalRecord row : rows) {
            result.add(row.orm_attached() && row.getContract() != null ? row.getContract().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtApprovalRecord.class)
    public List<String> orgName(@ContextSource List<ErpCtApprovalRecord> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtApprovalRecord row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtApprovalRecord.class)
    public List<String> approvalMatrixName(@ContextSource List<ErpCtApprovalRecord> rows) {
        orm().batchLoadProps(rows, Collections.singleton("approvalMatrix"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtApprovalRecord row : rows) {
            result.add(row.orm_attached() && row.getApprovalMatrix() != null ? row.getApprovalMatrix().getCode() : null);
        }
        return result;
    }

}
