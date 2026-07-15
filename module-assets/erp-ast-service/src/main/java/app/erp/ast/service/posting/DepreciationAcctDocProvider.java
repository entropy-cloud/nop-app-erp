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

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.DEPRECIATION);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readDecimal(event, ErpAstConstants.BILL_DATA_DEPRECIATION_AMOUNT);
        String expenseSubject = readCode(event, ErpAstConstants.BILL_DATA_EXPENSE_SUBJECT_CODE, SUBJECT_EXPENSE);
        String accumSubject = readCode(event, ErpAstConstants.BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE, SUBJECT_ACCUM_DEPRE);

        List<VoucherFact> facts = new ArrayList<>(2);
        facts.add(fact(expenseSubject, "折旧费用", DC_DEBIT, amount, event));
        facts.add(fact(accumSubject, "累计折旧", DC_CREDIT, amount, event));
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setBusinessType(event.getBusinessType().name());
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
