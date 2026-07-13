
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtRebateAccrualBiz;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;

@BizModel("ErpCtRebateAccrual")
public class ErpCtRebateAccrualBizModel extends CrudBizModel<ErpCtRebateAccrual> implements IErpCtRebateAccrualBiz{
    public ErpCtRebateAccrualBizModel(){
        setEntityName(ErpCtRebateAccrual.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCtRebateAccrual.class)
    public List<String> rebateAgreementName(@ContextSource List<ErpCtRebateAccrual> rows) {
        orm().batchLoadProps(rows, Collections.singleton("rebateAgreement"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtRebateAccrual row : rows) {
            result.add(row.orm_attached() && row.getRebateAgreement() != null ? row.getRebateAgreement().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCtRebateAccrual.class)
    public List<String> orgName(@ContextSource List<ErpCtRebateAccrual> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCtRebateAccrual row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

}
