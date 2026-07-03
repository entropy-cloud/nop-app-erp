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
 * 资产处置业财过账 Provider（assets 域，非默认 Provider）。
 *
 * <p>支持单一业务类型 {@link ErpFinBusinessType#DISPOSAL}，按 disposalType 内部分支科目分解
 * （depreciation-and-posting.md §3.1）。统一复式平衡：结转原值（贷固定资产）与累计折旧（借），
 * 按处置收入（借银行存款，>0 时）与清理损益（按 gainLoss 正负 借损失/贷收益）配平：
 * <ul>
 *   <li>SCRAPPED（报废，收入常为 0）：借累计折旧 / 借清理损失(账面净值) / 贷固定资产。</li>
 *   <li>SOLD（出售）：借累计折旧 / 借银行存款(处置收入) / [借清理损失 | 贷清理收益] / 贷固定资产。</li>
 * </ul>
 * 清理损益科目按类别 disposalGainLossSubjectId，gainLoss 正负定借贷方向（正=营业外收入贷，负=营业外支出借）。
 */
public class DisposalAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_FIXED_ASSET = "1601";       // 固定资产
    static final String SUBJECT_ACCUM_DEPRE = "1602";       // 累计折旧
    static final String SUBJECT_BANK_DEPOSIT = "1002";      // 银行存款
    static final String SUBJECT_DISPOSAL_LOSS = "6711";     // 营业外支出（清理损失）
    static final String SUBJECT_DISPOSAL_GAIN = "6301";     // 营业外收入（清理收益）

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.DISPOSAL);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal accumDep = readDecimal(event, ErpAstConstants.BILL_DATA_ACCUMULATED_DEPRECIATION);
        BigDecimal original = readDecimal(event, ErpAstConstants.BILL_DATA_ORIGINAL_VALUE);
        BigDecimal disposalAmount = readDecimal(event, ErpAstConstants.BILL_DATA_DISPOSAL_AMOUNT);
        BigDecimal gainLoss = readDecimal(event, ErpAstConstants.BILL_DATA_GAIN_LOSS);

        String fixedAssetSubject = readCode(event, ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                SUBJECT_FIXED_ASSET);
        String accumSubject = readCode(event, ErpAstConstants.BILL_DATA_ACCUM_DEPRE_SUBJECT_CODE,
                SUBJECT_ACCUM_DEPRE);
        String gainLossSubject = readCode(event, ErpAstConstants.BILL_DATA_DISPOSAL_GAINLOSS_SUBJECT_CODE,
                SUBJECT_DISPOSAL_LOSS);

        List<VoucherFact> facts = new ArrayList<>(4);
        // 借：累计折旧（结转）
        if (accumDep.signum() != 0) {
            facts.add(fact(accumSubject, "累计折旧", DC_DEBIT, accumDep, event));
        }
        // 借：银行存款（处置收入，>0 时）
        if (disposalAmount.signum() > 0) {
            facts.add(fact(SUBJECT_BANK_DEPOSIT, "银行存款", DC_DEBIT, disposalAmount, event));
        }
        // 清理损益（gainLoss 正=收益贷，负=损失借）
        if (gainLoss.signum() > 0) {
            facts.add(fact(gainLossSubject, "营业外收入", DC_CREDIT, gainLoss, event));
        } else if (gainLoss.signum() < 0) {
            facts.add(fact(gainLossSubject, "营业外支出", DC_DEBIT, gainLoss.negate(), event));
        }
        // 贷：固定资产（结转原值）
        facts.add(fact(fixedAssetSubject, "固定资产", DC_CREDIT, original, event));
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
