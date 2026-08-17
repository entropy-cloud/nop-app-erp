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
    static final String SUBJECT_RETENTION_RECEIVABLE = "1122";  // 应收账款-质保金（留存借方 / 返还贷方，RC-R1.63 D2）
    static final String SUBJECT_RETENTION_PAYABLE = "2241";     // 其他应付款-质保金（留存贷方 / 返还借方，RC-R1.63 D2）

    /**
     * A1 GL 映射键（plan 2026-07-24-1351-1）：FIXED_ASSET/REVENUE 通用键复用；CIP/PROJECT_COST/PROFIT_LOSS 域专用键。
     * RETENTION_RECEIVABLE/RETENTION_PAYABLE 为质保金域专用键（RC-R1.63 / P1-RC-052，D2 选项 A：
     * GL mapping 规则可覆盖 subjectCode，空匹配回退 Provider 默认编码——部署须预置 1122/2241 科目）。
     */
    static final String ACCOUNT_KEY_FIXED_ASSET = "FIXED_ASSET";
    static final String ACCOUNT_KEY_CIP = "CIP";
    static final String ACCOUNT_KEY_PROJECT_COST = "PROJECT_COST";
    static final String ACCOUNT_KEY_PROFIT_LOSS = "PROFIT_LOSS";
    static final String ACCOUNT_KEY_REVENUE = "REVENUE";
    static final String ACCOUNT_KEY_RETENTION_RECEIVABLE = "RETENTION_RECEIVABLE";
    static final String ACCOUNT_KEY_RETENTION_PAYABLE = "RETENTION_PAYABLE";

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
        BigDecimal retentionAmount = readDecimal(event, ErpPrjConstants.BILL_DATA_RETENTION_AMOUNT);
        boolean retentionReturn = readBoolean(event, ErpPrjConstants.BILL_DATA_RETENTION_RETURN);
        String memo = null;

        List<VoucherFact> facts = new ArrayList<>();

        if (ErpPrjConstants.SETTLEMENT_TYPE_CLOSE.equals(settlementType) && transferToAsset) {
            // CLOSE 转固：借固定资产（资本化最终成本）/ 贷在建工程（项目成本结转）。借贷平衡（finalCost）。
            VoucherFact debit = fact(SUBJECT_FIXED_ASSET, "固定资产", DC_DEBIT, finalCost, event, memo,
                    ACCOUNT_KEY_FIXED_ASSET);
            debit.setProjectId(projectId);
            facts.add(debit);

            VoucherFact credit = fact(SUBJECT_CIP, "在建工程", DC_CREDIT, finalCost, event, memo, ACCOUNT_KEY_CIP);
            credit.setProjectId(projectId);
            facts.add(credit);
        } else if (!retentionReturn) {
            // FINAL/INTERIM：借项目成本（结转）+ 本年利润（损益平衡）/ 贷项目收入（结转）。
            // 平衡：finalCost + profitLoss = finalRevenue。
            // 返还凭证（RETENTION_RETURN=true）跳过主结算腿——仅生成质保金镜像对冲腿，避免重复结转。
            BigDecimal profitLoss = finalRevenue.subtract(finalCost);
            VoucherFact debitCost = fact(SUBJECT_PROJECT_COST, "项目成本", DC_DEBIT, finalCost, event, memo,
                    ACCOUNT_KEY_PROJECT_COST);
            debitCost.setProjectId(projectId);
            facts.add(debitCost);

            if (profitLoss.signum() != 0) {
                String plSubject = SUBJECT_PROFIT_LOSS;
                String plName = "本年利润";
                String plDirection = profitLoss.signum() > 0 ? DC_DEBIT : DC_CREDIT;
                BigDecimal plAmount = profitLoss.abs();
                VoucherFact pl = fact(plSubject, plName, plDirection, plAmount, event, memo, ACCOUNT_KEY_PROFIT_LOSS);
                pl.setProjectId(projectId);
                facts.add(pl);
            }

            VoucherFact creditRevenue = fact(SUBJECT_PROJECT_REVENUE, "项目收入", DC_CREDIT, finalRevenue, event, memo,
                    ACCOUNT_KEY_REVENUE);
            creditRevenue.setProjectId(projectId);
            facts.add(creditRevenue);
        }

        // RC-R1.63 / P1-RC-052（D2 选项 A）：质保金平衡腿（金额=retentionAmount，标 projectId 辅助核算）。
        // 留存（主结算凭证内）：借 1122 应收账款-质保金 / 贷 2241 其他应付款-质保金。
        // 返还（billHeadCode=结算单号#RETURN 独立凭证）：镜像腿 借 2241 / 贷 1122 对冲清零。
        if (retentionAmount.signum() != 0) {
            if (retentionReturn) {
                VoucherFact dr = fact(SUBJECT_RETENTION_PAYABLE, "其他应付款-质保金", DC_DEBIT, retentionAmount, event,
                        memo, ACCOUNT_KEY_RETENTION_PAYABLE);
                dr.setProjectId(projectId);
                facts.add(dr);
                VoucherFact cr = fact(SUBJECT_RETENTION_RECEIVABLE, "应收账款-质保金", DC_CREDIT, retentionAmount, event,
                        memo, ACCOUNT_KEY_RETENTION_RECEIVABLE);
                cr.setProjectId(projectId);
                facts.add(cr);
            } else {
                VoucherFact dr = fact(SUBJECT_RETENTION_RECEIVABLE, "应收账款-质保金", DC_DEBIT, retentionAmount, event,
                        memo, ACCOUNT_KEY_RETENTION_RECEIVABLE);
                dr.setProjectId(projectId);
                facts.add(dr);
                VoucherFact cr = fact(SUBJECT_RETENTION_PAYABLE, "其他应付款-质保金", DC_CREDIT, retentionAmount, event,
                        memo, ACCOUNT_KEY_RETENTION_PAYABLE);
                cr.setProjectId(projectId);
                facts.add(cr);
            }
        }
        return facts;
    }

    private VoucherFact fact(String subjectCode, String subjectName, String dcDirection, BigDecimal amount,
                             PostingEvent event, String memo, String accountKey) {
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
