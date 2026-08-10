package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.common.service.MaskHelper;
import app.erp.hr.biz.IErpHrSocialInsuranceBaseBiz;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@BizModel("ErpHrSocialInsuranceBase")
public class ErpHrSocialInsuranceBaseBizModel extends CrudBizModel<ErpHrSocialInsuranceBase> implements IErpHrSocialInsuranceBaseBiz{
    public ErpHrSocialInsuranceBaseBizModel(){
        setEntityName(ErpHrSocialInsuranceBase.class.getName());
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 薪酬审批人；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> SALARY_MASK_ROLES = Set.of(MaskHelper.ROLE_SALARY_APPROVER);

    @BizLoader("socialInsuranceBase")
    public BigDecimal socialInsuranceBaseMask(@ContextSource ErpHrSocialInsuranceBase entity) {
        return MaskHelper.maskDecimal(entity.getSocialInsuranceBase(), SALARY_MASK_ROLES, entity, "socialInsuranceBase");
    }

    @BizLoader("housingFundBase")
    public BigDecimal housingFundBaseMask(@ContextSource ErpHrSocialInsuranceBase entity) {
        return MaskHelper.maskDecimal(entity.getHousingFundBase(), SALARY_MASK_ROLES, entity, "housingFundBase");
    }

}
