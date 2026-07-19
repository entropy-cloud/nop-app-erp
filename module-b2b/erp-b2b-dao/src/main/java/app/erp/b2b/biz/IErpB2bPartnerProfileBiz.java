
package app.erp.b2b.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;

public interface IErpB2bPartnerProfileBiz extends ICrudBiz<ErpB2bPartnerProfile>{

    /**
     * 上线：CERTIFIED→PRODUCTION。
     */
    @BizMutation
    ErpB2bPartnerProfile activate(@Name("profileId") Long profileId, IServiceContext context);

    /**
     * 暂停：PRODUCTION→SUSPENDED。
     */
    @BizMutation
    ErpB2bPartnerProfile suspend(@Name("profileId") Long profileId, IServiceContext context);

    /**
     * 终止：任意→TERMINATED。
     */
    @BizMutation
    ErpB2bPartnerProfile deactivate(@Name("profileId") Long profileId, IServiceContext context);
}
