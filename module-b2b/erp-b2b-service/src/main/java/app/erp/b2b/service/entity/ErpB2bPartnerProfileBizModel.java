
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.b2b.biz.IErpB2bPartnerProfileBiz;
import app.erp.b2b.dao.entity.ErpB2bPartnerProfile;

@BizModel("ErpB2bPartnerProfile")
public class ErpB2bPartnerProfileBizModel extends CrudBizModel<ErpB2bPartnerProfile> implements IErpB2bPartnerProfileBiz {
    public ErpB2bPartnerProfileBizModel(){
        setEntityName(ErpB2bPartnerProfile.class.getName());
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile activate(@Name("profileId") Long profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(String.valueOf(profileId), null, context);
        profile.setStatus("PRODUCTION");
        updateEntity(profile, null, context);
        return profile;
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile suspend(@Name("profileId") Long profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(String.valueOf(profileId), null, context);
        profile.setStatus("SUSPENDED");
        updateEntity(profile, null, context);
        return profile;
    }

    @Override
    @BizMutation
    public ErpB2bPartnerProfile deactivate(@Name("profileId") Long profileId, IServiceContext context) {
        ErpB2bPartnerProfile profile = requireEntity(String.valueOf(profileId), null, context);
        profile.setStatus("TERMINATED");
        updateEntity(profile, null, context);
        return profile;
    }

}
