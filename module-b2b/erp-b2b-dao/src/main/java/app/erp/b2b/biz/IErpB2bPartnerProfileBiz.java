
package app.erp.b2b.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;

public interface IErpB2bPartnerProfileBiz extends ICrudBiz<ErpB2bPartnerProfile>{

    /**
     * 推进至测试：REGISTERED→TESTING。前置：基本配置完整（partnerId/protocol/authMethod/transportEndpoint/allowedFormats）。
     */
    @BizMutation
    ErpB2bPartnerProfile promoteToTesting(@Name("profileId") String profileId, IServiceContext context);

    /**
     * 推进至认证：TESTING→CERTIFIED。前置：测试通过率≥config 门槛（默认 0.9）+ 关键用例 TC-001/TC-004 必过
     * + 认证清单必检项全部通过。
     */
    @BizMutation
    ErpB2bPartnerProfile promoteToCertified(@Name("profileId") String profileId, IServiceContext context);

    /**
     * 上线：CERTIFIED→PRODUCTION，并设置 goLiveDate=now。
     */
    @BizMutation
    ErpB2bPartnerProfile activate(@Name("profileId") String profileId, IServiceContext context);

    /**
     * 暂停：REGISTERED/TESTING/CERTIFIED/PRODUCTION→SUSPENDED。
     */
    @BizMutation
    ErpB2bPartnerProfile suspend(@Name("profileId") String profileId, IServiceContext context);

    /**
     * 终止：任意非终态→TERMINATED，并设置 archivedAt=now。
     */
    @BizMutation
    ErpB2bPartnerProfile deactivate(@Name("profileId") String profileId, IServiceContext context);
}
