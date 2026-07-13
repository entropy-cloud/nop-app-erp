
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherBillRBiz;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpFinVoucherBillR")
public class ErpFinVoucherBillRBizModel extends CrudBizModel<ErpFinVoucherBillR> implements IErpFinVoucherBillRBiz{
    public ErpFinVoucherBillRBizModel(){
        setEntityName(ErpFinVoucherBillR.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D）----------

    @BizLoader(forType = ErpFinVoucherBillR.class)
    public List<String> voucherCode(@ContextSource List<ErpFinVoucherBillR> bills) {
        orm().batchLoadProps(bills, Collections.singleton("voucher"));
        List<String> result = new ArrayList<>(bills.size());
        for (ErpFinVoucherBillR bill : bills) {
            result.add(bill.getVoucher() != null ? bill.getVoucher().getCode() : null);
        }
        return result;
    }
}
