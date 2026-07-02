
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.ast.dao.entity.ErpAstDisposal;

public interface IErpAstDisposalBiz extends ICrudBiz<ErpAstDisposal> {

    @BizMutation
    ErpAstDisposal submit(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstDisposal approve(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstDisposal reject(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstDisposal reverseApprove(@Name("id") Long id, IServiceContext context);
}
