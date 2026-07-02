
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.ast.dao.entity.ErpAstAssetCapitalization;

public interface IErpAstAssetCapitalizationBiz extends ICrudBiz<ErpAstAssetCapitalization> {

    @BizMutation
    ErpAstAssetCapitalization submit(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstAssetCapitalization approve(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstAssetCapitalization reject(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpAstAssetCapitalization reverseApprove(@Name("id") Long id, IServiceContext context);
}
