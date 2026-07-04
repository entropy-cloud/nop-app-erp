package app.erp.fin.dao.dto;

/**
 * 银行对账单自动勾对结果报告（{@code IErpFinBankStatementLineBiz.autoMatch} 的返回值）。
 */
public class BankStatementMatchResult {
    private int matched;
    private int unmatched;
    private int suspense;

    public int getMatched() {
        return matched;
    }

    public void setMatched(int matched) {
        this.matched = matched;
    }

    public int getUnmatched() {
        return unmatched;
    }

    public void setUnmatched(int unmatched) {
        this.unmatched = unmatched;
    }

    public int getSuspense() {
        return suspense;
    }

    public void setSuspense(int suspense) {
        this.suspense = suspense;
    }
}
