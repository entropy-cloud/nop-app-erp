package app.erp.ast.service.posting;

import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 折旧业财过账 Provider（assets 域，非默认 Provider）。
 *
 * <p>支持业务类型 {@link ErpFinBusinessType#DEPRECIATION}：借折旧费用（类别 expenseSubjectId）/ 贷累计折旧
 * （类别 depreciationSubjectId），金额取自 PostingEvent.billData.DEPRECIATION_AMOUNT（depreciation-and-posting.md §1.1）。
 *
 * <p>科目编码由派发器按资产类别配置解析后经 billData 传入（EXPENSE_SUBJECT_CODE / ACCUM_DEPRE_SUBJECT_CODE），
 * 未配置时回退标准科目编码，引擎按 code 解析为主数据科目。
 */
public class DepreciationAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_EXPENSE = "6602";       // 折旧费用（管理费用）
    static final String SUBJECT_ACCUM_DEPRE = "1602";   // 累计折旧

    /**
     * A1 GL 映射键（plan 2026-07-24-1351-1）：DEPRECIATION_EXPENSE/ACCUMULATED_DEPRECIATION 通用键复用。
     */
    static final String ACCOUNT_KEY_DEPRECIATION_EXPENSE = "DEPRECIATION_EXPENSE";
    static final String ACCOUNT_KEY_ACCUMULATED_DEPRECIATION = "ACCUMULATED_DEPRECIATION";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.DEPRECIATION);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readDecimal(event, ErpAstConstants.BILL_DATA_DEPRECIATION_AMOUNT);
        String expenseSubject = readCode(event, ErpAstConstants.BILL_DATA_EXPENSE_SUBJECT_CODE, SUBJECT_EXPENSE);
        String accumSubject = readCode(event, ErpAstConstants.BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE, SUBJECT_ACCUM_DEPRE);
        // RC-R1.52 方式B 补提标注（L1 UC-AST-07「补提凭证标注所属期间(审计)」）：CATCHUP_PERIODS 键存在时
        // 凭证行 memo 记「补提 {periods}」（VoucherFact.memo → ErpFinVoucherLine.memo，审计可追溯）。
        String catchUpPeriods = readCode(event, ErpAstConstants.BILL_DATA_CATCHUP_PERIODS, null);
        String memo = catchUpPeriods != null ? "补提折旧(" + catchUpPeriods + ")" : null;

        List<VoucherFact> facts = new ArrayList<>(2);
        facts.add(fact(expenseSubject, "折旧费用", DC_DEBIT, amount, event, ACCOUNT_KEY_DEPRECIATION_EXPENSE, memo));
        facts.add(fact(accumSubject, "累计折旧", DC_CREDIT, amount, event, ACCOUNT_KEY_ACCUMULATED_DEPRECIATION, memo));
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event, String accountKey, String memo) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setAccountKey(accountKey);
        fact.setBusinessType(event.getBusinessType().name());
        fact.setMemo(memo);
        return fact;
    }

    private BigDecimal readDecimal(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(value.toString().trim());
    }

    private String readCode(PostingEvent event, String key, String defaultValue) {
        Object value = event.getBillData().get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return value.toString().trim();
    }
}
