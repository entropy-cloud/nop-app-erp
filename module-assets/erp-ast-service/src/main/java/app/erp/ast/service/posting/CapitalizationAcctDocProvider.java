package app.erp.ast.service.posting;

import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import java.util.Objects;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 资产资本化业财过账 Provider（assets 域，非默认 Provider）。
 *
 * <p>支持业务类型 {@link ErpFinBusinessType#CAPITALIZATION}：借固定资产 / 贷方按 sourceType 分解
 * （depreciation-and-posting.md §2.1）：
 * <ul>
 *   <li>DIRECT_PURCHASE：贷银行存款（直接购置）。</li>
 *   <li>CIP：贷在建工程（在建工程转固）。</li>
 * </ul>
 *
 * <p>科目编码（subjectCode）由派发器按资产类别配置解析后经 billData 传入；未配置时回退标准科目编码，
 * 引擎 {@code resolveSubjects} 按 code 解析为主数据科目。
 */
public class CapitalizationAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_FIXED_ASSET = "1601";       // 固定资产
    static final String SUBJECT_CIP = "1603";               // 在建工程
    static final String SUBJECT_BANK_DEPOSIT = "1002";      // 银行存款（直接购置贷方）

    /**
     * A1 GL 映射键（plan 2026-07-24-1351-1）：FIXED_ASSET/BANK_DEPOSIT 通用键复用；CIP(在建工程) 域专用键。
     */
    static final String ACCOUNT_KEY_FIXED_ASSET = "FIXED_ASSET";
    static final String ACCOUNT_KEY_CIP = "CIP";
    static final String ACCOUNT_KEY_BANK_DEPOSIT = "BANK_DEPOSIT";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.CAPITALIZATION);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readDecimal(event, ErpAstConstants.BILL_DATA_ORIGINAL_VALUE);
        String sourceType = readCode(event, ErpAstConstants.BILL_DATA_SOURCE_TYPE, ErpAstConstants.SOURCE_TYPE_DIRECT_PURCHASE);

        String fixedAssetSubject = readCode(event, ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                SUBJECT_FIXED_ASSET);
        String creditSubject;
        String creditName;
        String creditAccountKey;
        if (Objects.equals(sourceType, ErpAstConstants.SOURCE_TYPE_CIP)) {
            creditSubject = readCode(event, ErpAstConstants.BILL_DATA_CREDIT_SUBJECT_CODE, SUBJECT_CIP);
            creditName = "在建工程";
            creditAccountKey = ACCOUNT_KEY_CIP;
        } else {
            // DIRECT_PURCHASE → 银行存款/应付账款（基线取银行存款）
            creditSubject = readCode(event, ErpAstConstants.BILL_DATA_CREDIT_SUBJECT_CODE, SUBJECT_BANK_DEPOSIT);
            creditName = "银行存款";
            creditAccountKey = ACCOUNT_KEY_BANK_DEPOSIT;
        }

        List<VoucherFact> facts = new ArrayList<>(2);
        facts.add(fact(fixedAssetSubject, "固定资产", DC_DEBIT, amount, event, ACCOUNT_KEY_FIXED_ASSET));
        facts.add(fact(creditSubject, creditName, DC_CREDIT, amount, event, creditAccountKey));
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event, String accountKey) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
        fact.setAccountKey(accountKey);
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

    private int readInt(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value == null) {
            return 0;
        }
        return Integer.parseInt(value.toString().trim());
    }

    private String readCode(PostingEvent event, String key, String defaultValue) {
        Object value = event.getBillData().get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return value.toString().trim();
    }
}
