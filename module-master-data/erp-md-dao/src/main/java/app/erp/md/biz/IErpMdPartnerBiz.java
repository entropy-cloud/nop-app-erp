
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdPartner;

public interface IErpMdPartnerBiz extends ICrudBiz<ErpMdPartner>{

    /**
     * 按主键加载往来单位，不存在返回 null。供业务域做供应商/客户启用校验等只读跨域访问
     * （经 I*Biz 管道，对齐 data-dependency-matrix §3 / service-layer 跨实体访问规则）。
     */
    @BizAction
    ErpMdPartner findById(@Name("id") Long id, IServiceContext context);
}
