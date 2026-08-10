package app.erp.pur.service.entity;

import java.math.BigDecimal;
import java.util.Set;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.common.service.MaskHelper;
import app.erp.pur.biz.IErpPurSupplierPriceListBiz;
import app.erp.pur.dao.entity.ErpPurSupplierPriceList;

@BizModel("ErpPurSupplierPriceList")
public class ErpPurSupplierPriceListBizModel extends CrudBizModel<ErpPurSupplierPriceList> implements IErpPurSupplierPriceListBiz{
    public ErpPurSupplierPriceListBizModel(){
        setEntityName(ErpPurSupplierPriceList.class.getName());
    }

    // ---------- E3.1 后端响应层脱敏（@BizLoader，plan 2026-08-10-2059-2）----------
    // 授权 = 采购员/管理员；非授权 = null。委托 MaskHelper（fail-closed）。
    private static final Set<String> PRICE_ROLES = Set.of(MaskHelper.ROLE_PURCHASER, MaskHelper.ROLE_BIZ_ADMIN);

    @BizLoader("unitPrice")
    public BigDecimal unitPriceMask(@ContextSource ErpPurSupplierPriceList entity) {
        return MaskHelper.maskDecimal(entity.getUnitPrice(), PRICE_ROLES);
    }
}
