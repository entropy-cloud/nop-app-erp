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
 * 资产盘点差异业财过账 Provider（assets 域，UC-AST-09）。
 *
 * <p>支持单一业务类型 {@link ErpFinBusinessType#ASSET_INVENTORY_ADJUSTMENT}，按差异分支科目分解
 * （inventory.md §三）：
 * <ul>
 *   <li>盘盈（surplus &gt; 0）：借 固定资产 / 贷 营业外收入。</li>
 *   <li>盘亏（shortage &gt; 0）：借 营业外支出 / 贷 固定资产。</li>
 * </ul>
 *
 * <p>科目编码由派发器按资产类别配置解析后经 billData 传入；未配置时回退标准科目编码。
 * 镜像 {@code ValueAdjustmentAcctDocProvider} 范式。
 */
public class AssetInventoryAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_FIXED_ASSET = "1601";           // 固定资产
    static final String SUBJECT_NON_OPERATING_INCOME = "6301";  // 营业外收入
    static final String SUBJECT_NON_OPERATING_EXPENSE = "6711"; // 营业外支出

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.ASSET_INVENTORY_ADJUSTMENT);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal surplus = readDecimal(event, ErpAstConstants.BILL_DATA_INVENTORY_SURPLUS_AMOUNT);
        BigDecimal shortage = readDecimal(event, ErpAstConstants.BILL_DATA_INVENTORY_SHORTAGE_AMOUNT);

        String fixedAssetSubject = readCode(event, ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                SUBJECT_FIXED_ASSET);
        String nonOpIncomeSubject = readCode(event,
                ErpAstConstants.BILL_DATA_NON_OPERATING_INCOME_SUBJECT_CODE, SUBJECT_NON_OPERATING_INCOME);
        String nonOpExpenseSubject = readCode(event,
                ErpAstConstants.BILL_DATA_NON_OPERATING_EXPENSE_SUBJECT_CODE, SUBJECT_NON_OPERATING_EXPENSE);

        List<VoucherFact> facts = new ArrayList<>(4);
        if (surplus.signum() > 0) {
            facts.add(fact(fixedAssetSubject, "固定资产", DC_DEBIT, surplus, event));
            facts.add(fact(nonOpIncomeSubject, "营业外收入", DC_CREDIT, surplus, event));
        }
        if (shortage.signum() > 0) {
            facts.add(fact(nonOpExpenseSubject, "营业外支出", DC_DEBIT, shortage, event));
            facts.add(fact(fixedAssetSubject, "固定资产", DC_CREDIT, shortage, event));
        }
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
        fact.setMemo(event.getBillHeadCode());
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
