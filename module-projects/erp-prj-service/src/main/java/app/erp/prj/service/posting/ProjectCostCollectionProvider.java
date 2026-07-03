package app.erp.prj.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.AcctDocContext;
import app.erp.fin.service.posting.IErpFinAcctDocProvider;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 项目成本归集业财过账 Provider（projects 域，非默认 Provider——Registry 中优先于默认 fallback）。
 *
 * <p>支持业务类型 {@link ErpFinBusinessType#PROJECT_COST_COLLECTION}。按 {@code sourceBillType} 区分借方科目：
 * <ul>
 *   <li>{@code TIMESHEET}（工时成本）：借项目成本科目（项目类型 defaultSubjectId，由派发器解析后填入
 *       {@code DEBIT_SUBJECT_CODE}），贷应付职工薪酬科目（{@code erp-prj.default-payroll-subject-id}，
 *       为空时派发器已抛 {@link ErpPrjErrors#ERR_PAYROLL_SUBJECT_NOT_CONFIGURED}）。</li>
 *   <li>{@code EXPENSE}（费用报销归集）：贷方科目由费用来源决定（暂由调用方在 CREDIT_SUBJECT_CODE 指定）。</li>
 * </ul>
 *
 * <p>对应 {@code cost-collection.md §八 凭证注册}；设计文档写作 {@code PROJECT_LABOR_COST} 为命名偏差，
 * 实际复用既有 {@code PROJECT_COST_COLLECTION(110)} 枚举（保持「不新增 finance 契约」边界）。
 * 所有分录行标 {@code projectId} 辅助核算维度（cost-collection.md §1.1）。
 */
public class ProjectCostCollectionProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    /** 工时成本默认借方科目（项目类型 defaultSubjectId 缺失时回退）。 */
    static final String SUBJECT_PROJECT_COST_DEFAULT = "5101";
    /** 应付职工薪酬默认贷方科目（工时成本，配置缺失时回退）。 */
    static final String SUBJECT_PAYROLL_DEFAULT = "2211";

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return Collections.singleton(ErpFinBusinessType.PROJECT_COST_COLLECTION);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        String sourceBillType = readString(event, ErpPrjConstants.BILL_DATA_SOURCE_BILL_TYPE);
        BigDecimal amount = readDecimal(event, ErpPrjConstants.BILL_DATA_COST_AMOUNT);
        Long projectId = readLong(event, ErpPrjConstants.BILL_DATA_PROJECT_ID);

        String debitSubjectCode = readString(event, ErpPrjConstants.BILL_DATA_DEBIT_SUBJECT_CODE);
        if (debitSubjectCode == null || debitSubjectCode.isEmpty()) {
            debitSubjectCode = SUBJECT_PROJECT_COST_DEFAULT;
        }
        String creditSubjectCode = readString(event, ErpPrjConstants.BILL_DATA_CREDIT_SUBJECT_CODE);
        if (creditSubjectCode == null || creditSubjectCode.isEmpty()) {
            creditSubjectCode = SUBJECT_PAYROLL_DEFAULT;
        }

        String memo = buildMemo(event, sourceBillType);

        List<VoucherFact> facts = new ArrayList<>(2);
        VoucherFact debit = fact(debitSubjectCode, "项目成本", DC_DEBIT, amount, event, memo);
        debit.setProjectId(projectId);
        facts.add(debit);

        VoucherFact credit = fact(creditSubjectCode, creditSubjectName(sourceBillType), DC_CREDIT, amount, event, memo);
        credit.setProjectId(projectId);
        facts.add(credit);

        return facts;
    }

    private String creditSubjectName(String sourceBillType) {
        if (ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET.equals(sourceBillType)) {
            return "应付职工薪酬";
        }
        return "项目费用贷方";
    }

    private String buildMemo(PostingEvent event, String sourceBillType) {
        return (sourceBillType == null ? "" : sourceBillType) + ":" + event.getBillHeadCode();
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
        if (value == null) {
            return null;
        }
        String s = value.toString().trim();
        return s.isEmpty() ? null : s;
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
}
