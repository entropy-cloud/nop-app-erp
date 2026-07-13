
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinVoucherLineBiz;
import app.erp.fin.dao.entity.ErpFinVoucherLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpFinVoucherLine")
public class ErpFinVoucherLineBizModel extends CrudBizModel<ErpFinVoucherLine> implements IErpFinVoucherLineBiz{
    public ErpFinVoucherLineBizModel(){
        setEntityName(ErpFinVoucherLine.class.getName());
    }

    // ---------- 高价值外键名称解析（机制 D：xmeta 派生字段 + BizLoader 批量加载防 N+1）----------
    // subjectCode/subjectName 为凭证行已持久化的冗余列，无需派生。

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> voucherCode(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("voucher"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getVoucher() != null ? line.getVoucher().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> currencyName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getCurrency() != null ? line.getCurrency().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> acctSchemaCode(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("acctSchema"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getAcctSchema() != null ? line.getAcctSchema().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> orgName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("org"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getOrg() != null ? line.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> partnerName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("partner"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getPartner() != null ? line.getPartner().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> departmentName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("department"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getDepartment() != null ? line.getDepartment().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> projectName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("project"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getProject() != null ? line.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> warehouseName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getWarehouse() != null ? line.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> materialName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpFinVoucherLine.class)
    public List<String> costCenterName(@ContextSource List<ErpFinVoucherLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("costCenter"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpFinVoucherLine line : lines) {
            result.add(line.getCostCenter() != null ? line.getCostCenter().getName() : null);
        }
        return result;
    }
}
