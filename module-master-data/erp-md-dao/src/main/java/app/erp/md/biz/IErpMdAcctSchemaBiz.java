
package app.erp.md.biz;

import io.nop.api.core.annotations.biz.BizAction;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.md.dao.entity.ErpMdAcctSchema;

public interface IErpMdAcctSchemaBiz extends ICrudBiz<ErpMdAcctSchema>{

    /**
     * 按组织解析核算账套（取首条），不存在返回 null。供业务域解析存货估值过账所需 acctSchemaId。
     */
    @BizAction
    ErpMdAcctSchema findFirstByOrg(@Name("orgId") Long orgId, IServiceContext context);
}
