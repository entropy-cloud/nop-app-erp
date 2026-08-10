package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.common.service.MaskHelper;
import app.erp.hr.biz.IErpHrSalarySimulationItemAdjustmentBiz;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@BizModel("ErpHrSalarySimulationItemAdjustment")
public class ErpHrSalarySimulationItemAdjustmentBizModel extends CrudBizModel<ErpHrSalarySimulationItemAdjustment> implements IErpHrSalarySimulationItemAdjustmentBiz{
    public ErpHrSalarySimulationItemAdjustmentBizModel(){
        setEntityName(ErpHrSalarySimulationItemAdjustment.class.getName());
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 薪酬审批人；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> SALARY_MASK_ROLES = Set.of(MaskHelper.ROLE_SALARY_APPROVER);

    @BizLoader("originalAmount")
    public BigDecimal originalAmountMask(@ContextSource ErpHrSalarySimulationItemAdjustment entity) {
        return MaskHelper.maskDecimal(entity.getOriginalAmount(), SALARY_MASK_ROLES, entity, "originalAmount");
    }

    @BizLoader("adjustedAmount")
    public BigDecimal adjustedAmountMask(@ContextSource ErpHrSalarySimulationItemAdjustment entity) {
        return MaskHelper.maskDecimal(entity.getAdjustedAmount(), SALARY_MASK_ROLES, entity, "adjustedAmount");
    }

}
