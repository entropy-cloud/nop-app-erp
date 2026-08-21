package app.erp.fin.dao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 凭证红字冲销预览（{@code IErpFinVoucherBiz.previewReverseVoucher} 返回值，plan 2026-07-23-1145-2 Phase 2）。
 *
 * <p>只读预览，不执行实际冲销。反映 {@code reverseVoucher(voucherId)} 的真实副作用：
 * 原凭证 {@code isReversed} 将置 true（{@code reverseVoucher} 仅标记状态，不生成红字凭证、不回退域单据——
 * 域单据级红冲经 {@code reverse(billHeadCode, businessType)} 入口）。
 *
 * <p>含红字金额预估（原借/贷同向取负）与关联业财回链（{@code ErpFinVoucherBillR}）信息上下文，
 * 供用户在执行冲销前评估影响。
 */
public class VoucherReversePreview {
    private String voucherId;
    private String voucherCode;
    private String voucherType;
    private LocalDate voucherDate;
    private BigDecimal totalDebit;
    private BigDecimal totalCredit;
    /** 红字预估借方（原 totalDebit 取负）。 */
    private BigDecimal reversedDebit;
    /** 红字预估贷方（原 totalCredit 取负）。 */
    private BigDecimal reversedCredit;
    private int lineCount;
    /** reverseVoucher 将把原凭证 isReversed 置为 true。 */
    private boolean willSetReversed;
    /** 关联业财回链（凭证由哪些域单据生成），信息上下文。 */
    private List<BillLinkInfo> billLinks = new ArrayList<>();

    public String getVoucherId() { return voucherId; }
    public void setVoucherId(String voucherId) { this.voucherId = voucherId; }
    public String getVoucherCode() { return voucherCode; }
    public void setVoucherCode(String voucherCode) { this.voucherCode = voucherCode; }
    public String getVoucherType() { return voucherType; }
    public void setVoucherType(String voucherType) { this.voucherType = voucherType; }
    public LocalDate getVoucherDate() { return voucherDate; }
    public void setVoucherDate(LocalDate voucherDate) { this.voucherDate = voucherDate; }
    public BigDecimal getTotalDebit() { return totalDebit; }
    public void setTotalDebit(BigDecimal totalDebit) { this.totalDebit = totalDebit; }
    public BigDecimal getTotalCredit() { return totalCredit; }
    public void setTotalCredit(BigDecimal totalCredit) { this.totalCredit = totalCredit; }
    public BigDecimal getReversedDebit() { return reversedDebit; }
    public void setReversedDebit(BigDecimal reversedDebit) { this.reversedDebit = reversedDebit; }
    public BigDecimal getReversedCredit() { return reversedCredit; }
    public void setReversedCredit(BigDecimal reversedCredit) { this.reversedCredit = reversedCredit; }
    public int getLineCount() { return lineCount; }
    public void setLineCount(int lineCount) { this.lineCount = lineCount; }
    public boolean isWillSetReversed() { return willSetReversed; }
    public void setWillSetReversed(boolean willSetReversed) { this.willSetReversed = willSetReversed; }
    public List<BillLinkInfo> getBillLinks() { return billLinks; }
    public void setBillLinks(List<BillLinkInfo> billLinks) { this.billLinks = billLinks; }

    public static class BillLinkInfo {
        private String billType;
        private String billCode;
        private String businessType;

        public String getBillType() { return billType; }
        public void setBillType(String billType) { this.billType = billType; }
        public String getBillCode() { return billCode; }
        public void setBillCode(String billCode) { this.billCode = billCode; }
        public String getBusinessType() { return businessType; }
        public void setBusinessType(String businessType) { this.businessType = businessType; }
    }
}
