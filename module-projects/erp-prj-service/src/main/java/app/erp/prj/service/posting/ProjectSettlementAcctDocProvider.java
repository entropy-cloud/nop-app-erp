package app.erp.prj.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.prj.service.ErpPrjConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 项目结算业财过账 Provider（projects 域，非默认 Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型 {@link ErpFinBusinessType#PROJECT_SETTLEMENT}。按 {@code settlementType} 区分借贷科目
 * （{@code profitability.md §关键流程 2/3}）：
 * <ul>
 *   <li>{@code FINAL}/{@code INTERIM}（收入/成本结转）：借项目成本（结转最终成本），贷项目收入（结转最终收入），
 *       差额经本年利润。基线简化为：借finalCost/贷finalRevenue，毛利差额经本年利润科目（由引擎平衡校验捕获）。</li>
 *   <li>{@code CLOSE}（关闭转固）：借固定资产（最终成本资本化），贷项目成本结转。</li>
 * </ul>
 *
 * <p>科目编码（subjectCode）由派发器经 billData 传入或回退标准编码，引擎 {@code resolveSubjects} 按 code 解析为主数据科目。
 * 所有分录行标 {@code projectId} 辅助核算维度。
 */
public class ProjectSettlementAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_PROJECT_REVENUE = "6001";   // 主营业务收入（结算收入结转）
    static final String SUBJECT_PROJECT_COST = "5101";      // 项目成本（最终成本结转）
    static final String SUBJECT_FIXED_ASSET = "1601";       // 固定资产（CLOSE 资本化借方）
    static final String SUBJECT_CIP = "1603";               // 在建工程（CLOSE 转固贷方）
    static final String SUBJECT_PROFIT_LOSS = "4103";       // 本年利润（FINAL/INTERIM 损益平衡科目）

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.singleton(ErpFinBusinessType.PROJECT_SETTLEMENT);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        String settlementType = readString(event, ErpPrjConstants.BILL_DATA_SETTLEMENT_TYPE);
        BigDecimal finalRevenue = readDecimal(event, ErpPrjConstants.BILL_DATA_FINAL_REVENUE);
        BigDecimal finalCost = readDecimal(event, ErpPrjConstants.BILL_DATA_FINAL_COST);
        Long projectId = readLong(event, ErpPrjConstants.BILL_DATA_PROJECT_ID);
        boolean transferToAsset = readBoolean(event, ErpPrjConstants.BILL_DATA_TRANSFER_TO_ASSET);
        String memo = null;

        List<VoucherFact> facts = new ArrayList<>();

        if (ErpPrjConstants.SETTLEMENT_TYPE_CLOSE.equals(settlementType) && transferToAsset) {
            // CLOSE 转固：借固定资产（资本化最终成本）/ 贷在建工程（项目成本结转）。借贷平衡（finalCost）。
            VoucherFact debit = fact(SUBJECT_FIXED_ASSET, "固定资产", DC_DEBIT, finalCost, event, memo);
            debit.setProjectId(projectId);
            facts.add(debit);

            VoucherFact credit = fact(SUBJECT_CIP, "在建工程", DC_CREDIT, finalCost, event, memo);
            credit.setProjectId(projectId);
            facts.add(credit);
        } else {
            // FINAL/INTERIM：借项目成本（结转）+ 本年利润（损益平衡）/ 贷项目收入（结转）。
            // 平衡：finalCost + profitLoss = finalRevenue。
            BigDecimal profitLoss = finalRevenue.subtract(finalCost);
            VoucherFact debitCost = fact(SUBJECT_PROJECT_COST, "项目成本", DC_DEBIT, finalCost, event, memo);
            debitCost.setProjectId(projectId);
            facts.add(debitCost);

            if (profitLoss.signum() != 0) {
                String plSubject = SUBJECT_PROFIT_LOSS;
                String plName = "本年利润";
                String plDirection = profitLoss.signum() > 0 ? DC_DEBIT : DC_CREDIT;
                BigDecimal plAmount = profitLoss.abs();
                VoucherFact pl = fact(plSubject, plName, plDirection, plAmount, event, memo);
                pl.setProjectId(projectId);
                facts.add(pl);
            }

            VoucherFact creditRevenue = fact(SUBJECT_PROJECT_REVENUE, "项目收入", DC_CREDIT, finalRevenue, event, memo);
            creditRevenue.setProjectId(projectId);
            facts.add(creditRevenue);
        }
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event, String memo) {
        VoucherFact fact = new VoucherFact();
        fact.setSubjectCode(subjectCode);
        fact.setSubjectName(subjectName);
        fact.setDcDirection(dcDirection);
        fact.setAmount(amount);
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

    private String readString(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        return value == null ? null : value.toString().trim();
    }

    private Long readLong(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean readBoolean(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        return Boolean.TRUE.equals(value);
    }
}
