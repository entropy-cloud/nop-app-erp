
package app.erp.md.service.entity;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.md.biz.IErpMdPartnerBiz;
import app.erp.md.dao.entity.ErpMdPartner;

@BizModel("ErpMdPartner")
public class ErpMdPartnerBizModel extends CrudBizModel<ErpMdPartner> implements IErpMdPartnerBiz{
    public ErpMdPartnerBizModel(){
        setEntityName(ErpMdPartner.class.getName());
    }

    @Override
    @BizAction
    public ErpMdPartner findById(@Name("id") Long id, IServiceContext context) {
        if (id == null) {
            return null;
        }
        // 经 get() 走数据权限 + Meta 管道（回归默认读取行为，对齐审计 D2 裁决）。
        return get(String.valueOf(id), true, context);
    }
}
