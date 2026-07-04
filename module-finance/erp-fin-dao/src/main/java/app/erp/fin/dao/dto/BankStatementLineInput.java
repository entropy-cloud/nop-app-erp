package app.erp.fin.dao.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行对账单行导入输入 DTO（{@code IErpFinBankStatementBiz.importStatement} 的行参数）。
 *
 * <p>外部文件解析（MT940/CSV/Excel）属集成层 Non-Goal，本 DTO 承载已解析的银行流水行。
 */
public class BankStatementLineInput {
    private LocalDate transactionDate;
    private String description;
    /** 银行参考号（幂等去重优先键）。 */
    private String refNo;
    /** 借贷方向（{@code erp-fin/dc-direction} DEBIT/CREDIT）。 */
    private String dcDirection;
    private BigDecimal amount;
    private BigDecimal balanceAfter;

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRefNo() {
        return refNo;
    }

    public void setRefNo(String refNo) {
        this.refNo = refNo;
    }

    public String getDcDirection() {
        return dcDirection;
    }

    public void setDcDirection(String dcDirection) {
        this.dcDirection = dcDirection;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(BigDecimal balanceAfter) {
        this.balanceAfter = balanceAfter;
    }
}
