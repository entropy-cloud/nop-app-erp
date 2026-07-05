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
import java.util.Objects;
import java.util.Set;

/**
 * 资产价值调整业财过账 Provider（assets 域，非默认 Provider）。
 *
 * <p>支持单一业务类型 {@link ErpFinBusinessType#VALUE_ADJUSTMENT}，按 adjustmentType 内部分支科目分解
 * （depreciation-and-posting.md §4.1）：
 * <ul>
 *   <li>IMPAIRMENT（减值）：借资产减值损失 / 贷固定资产减值准备。</li>
 *   <li>REVALUATION_UP（重估增值）：借固定资产 / 贷资本公积-其他资本公积。</li>
 *   <li>REVALUATION_DOWN（重估减值）：借资产减值损失 / 贷固定资产。</li>
 * </ul>
 *
 * <p>科目编码由派发器按资产类别配置解析后经 billData 传入；未配置时回退标准科目编码。
 */
public class ValueAdjustmentAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_FIXED_ASSET = "1601";              // 固定资产
    static final String SUBJECT_IMPAIRMENT_LOSS = "6702";          // 资产减值损失
    static final String SUBJECT_IMPAIRMENT_PROVISION = "1604";     // 固定资产减值准备
    static final String SUBJECT_CAPITAL_RESERVE = "4002";          // 资本公积-其他资本公积

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.VALUE_ADJUSTMENT);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readDecimal(event, ErpAstConstants.BILL_DATA_ADJUSTMENT_AMOUNT);
        String adjustmentType = readCode(event, ErpAstConstants.BILL_DATA_ADJUSTMENT_TYPE, "");

        String fixedAssetSubject = readCode(event, ErpAstConstants.BILL_DATA_FIXED_ASSET_SUBJECT_CODE,
                SUBJECT_FIXED_ASSET);
        String impairmentLossSubject = readCode(event, ErpAstConstants.BILL_DATA_IMPAIRMENT_LOSS_SUBJECT_CODE,
                SUBJECT_IMPAIRMENT_LOSS);
        String impairmentProvisionSubject = readCode(event,
                ErpAstConstants.BILL_DATA_IMPAIRMENT_PROVISION_SUBJECT_CODE, SUBJECT_IMPAIRMENT_PROVISION);
        String capitalReserveSubject = readCode(event, ErpAstConstants.BILL_DATA_CAPITAL_RESERVE_SUBJECT_CODE,
                SUBJECT_CAPITAL_RESERVE);

        List<VoucherFact> facts = new ArrayList<>(2);
        if (Objects.equals(adjustmentType, ErpAstConstants.ADJUSTMENT_TYPE_IMPAIRMENT)) {
            facts.add(fact(impairmentLossSubject, "资产减值损失", DC_DEBIT, amount, event));
            facts.add(fact(impairmentProvisionSubject, "固定资产减值准备", DC_CREDIT, amount, event));
        } else if (Objects.equals(adjustmentType, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_UP)) {
            facts.add(fact(fixedAssetSubject, "固定资产", DC_DEBIT, amount, event));
            facts.add(fact(capitalReserveSubject, "资本公积-其他资本公积", DC_CREDIT, amount, event));
        } else if (Objects.equals(adjustmentType, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_DOWN)) {
            facts.add(fact(impairmentLossSubject, "资产减值损失", DC_DEBIT, amount, event));
            facts.add(fact(fixedAssetSubject, "固定资产", DC_CREDIT, amount, event));
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
