
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bPartnerCredentialBiz;
import app.erp.b2b.dao.entity.ErpB2bPartnerCredential;

@BizModel("ErpB2bPartnerCredential")
public class ErpB2bPartnerCredentialBizModel extends CrudBizModel<ErpB2bPartnerCredential> implements IErpB2bPartnerCredentialBiz{
    public ErpB2bPartnerCredentialBizModel(){
        setEntityName(ErpB2bPartnerCredential.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpB2bPartnerCredential.class)
    public List<String> partnerProfileName(@ContextSource List<ErpB2bPartnerCredential> rows) {
        orm().batchLoadProps(rows, Collections.singleton("partnerProfile"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpB2bPartnerCredential row : rows) {
            result.add(row.orm_attached() && row.getPartnerProfile() != null ? row.getPartnerProfile().getCode() : null);
        }
        return result;
    }

}
