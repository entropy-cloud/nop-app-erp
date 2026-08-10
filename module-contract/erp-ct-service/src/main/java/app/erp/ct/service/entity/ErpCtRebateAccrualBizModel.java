package app.erp.ct.service.entity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.common.service.MaskHelper;
import app.erp.ct.biz.IErpCtRebateAccrualBiz;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;

@BizModel("ErpCtRebateAccrual")
public class ErpCtRebateAccrualBizModel extends CrudBizModel<ErpCtRebateAccrual> implements IErpCtRebateAccrualBiz{
    public ErpCtRebateAccrualBizModel(){
        setEntityName(ErpCtRebateAccrual.class.getName());
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 合同审批人/合同专员；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> CT_AMOUNT_ROLES = Set.of(MaskHelper.ROLE_CT_APPROVER, MaskHelper.ROLE_CT_CLERK);

    @BizLoader("billAmountSource")
    public BigDecimal billAmountSourceMask(@ContextSource ErpCtRebateAccrual entity) {
        return MaskHelper.maskDecimal(entity.getBillAmountSource(), CT_AMOUNT_ROLES);
    }

    @BizLoader("accruedRebate")
    public BigDecimal accruedRebateMask(@ContextSource ErpCtRebateAccrual entity) {
        return MaskHelper.maskDecimal(entity.getAccruedRebate(), CT_AMOUNT_ROLES);
    }

}
