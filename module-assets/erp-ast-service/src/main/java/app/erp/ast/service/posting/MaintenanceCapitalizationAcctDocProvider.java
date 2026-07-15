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
 * 资产维修资本化业财过账 Provider（assets 域，UC-AST-10）。
 *
 * <p>支持单一业务类型 {@link ErpFinBusinessType#MAINTENANCE_CAPITALIZATION}。资本化维修 = 既有资产原值增量
 * （区别于新建 CAPITALIZATION(80) 建卡）。借固定资产（原值增量）；贷在建工程/银行存款/存货（费用来源）。
 *
 * <p>科目编码由派发器按资产类别配置解析后经 billData 传入；未配置时回退标准科目编码。
 */
public class MaintenanceCapitalizationAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_FIXED_ASSET = "1601";   // 固定资产
    static final String SUBJECT_BANK = "1002";          // 银行存款
    static final String SUBJECT_INVENTORY = "1403";     // 存货

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.MAINTENANCE_CAPITALIZATION);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readDecimal(event, ErpAstConstants.BILL_DATA_MAINTENANCE_CAPITALIZED_AMOUNT);

        String fixedAssetSubject = readCode(event, ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                SUBJECT_FIXED_ASSET);
        String bankSubject = readCode(event, ErpAstConstants.BILL_DATA_MAINTENANCE_BANK_SUBJECT_CODE, SUBJECT_BANK);

        List<VoucherFact> facts = new ArrayList<>(2);
        facts.add(fact(fixedAssetSubject, "固定资产", DC_DEBIT, amount, event));
        facts.add(fact(bankSubject, "银行存款", DC_CREDIT, amount, event));
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
