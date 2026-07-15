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
 * 资产维修费用化业财过账 Provider（assets 域，UC-AST-10）。
 *
 * <p>支持单一业务类型 {@link ErpFinBusinessType#MAINTENANCE_EXPENSE}。借维修费用；贷方按
 * {@code maintenanceVisitId} 分支（防双重扣减，maintenance.md §四）：
 * <ul>
 *   <li>关联维护工单（linkedVisit=true）：贷维修中转/清算科目（备件已由 maintenance 域实物出库）。</li>
 *   <li>独立维修（linkedVisit=false）：贷存货（备件消耗）/银行存款（外协/人工）。</li>
 * </ul>
 *
 * <p>科目编码由派发器解析后经 billData 传入；未配置时回退标准科目编码。镜像
 * {@code ValueAdjustmentAcctDocProvider} 范式。
 */
public class MaintenanceExpenseAcctDocProvider implements IErpFinAcctDocProvider {

    static final String DC_DEBIT = ErpFinConstants.DC_DEBIT;
    static final String DC_CREDIT = ErpFinConstants.DC_CREDIT;

    static final String SUBJECT_MAINTENANCE_EXPENSE = "6602";  // 维修费用
    static final String SUBJECT_CLEARING = "2502";             // 维修中转清算科目
    static final String SUBJECT_BANK = "1002";                 // 银行存款
    static final String SUBJECT_INVENTORY = "1403";            // 存货（备件消耗）

    @Override
    public Set<ErpFinBusinessType> getSupportedBusinessTypes() {
        return EnumSet.of(ErpFinBusinessType.MAINTENANCE_EXPENSE);
    }

    @Override
    public List<VoucherFact> createFacts(PostingEvent event, AcctDocContext ctx) {
        BigDecimal amount = readDecimal(event, ErpAstConstants.BILL_DATA_MAINTENANCE_TOTAL_COST);
        boolean linkedVisit = readBoolean(event, ErpAstConstants.BILL_DATA_MAINTENANCE_LINKED_VISIT);

        String expenseSubject = readCode(event, ErpAstConstants.BILL_DATA_MAINTENANCE_EXPENSE_SUBJECT_CODE,
                SUBJECT_MAINTENANCE_EXPENSE);
        String clearingSubject = readCode(event, ErpAstConstants.BILL_DATA_MAINTENANCE_CLEARING_SUBJECT_CODE,
                SUBJECT_CLEARING);
        String bankSubject = readCode(event, ErpAstConstants.BILL_DATA_MAINTENANCE_BANK_SUBJECT_CODE, SUBJECT_BANK);
        String inventorySubject = readCode(event, ErpAstConstants.BILL_DATA_MAINTENANCE_INVENTORY_SUBJECT_CODE,
                SUBJECT_INVENTORY);

        List<VoucherFact> facts = new ArrayList<>(2);
        facts.add(fact(expenseSubject, "维修费用", DC_DEBIT, amount, event));
        if (linkedVisit) {
            facts.add(fact(clearingSubject, "维修中转清算", DC_CREDIT, amount, event));
        } else {
            facts.add(fact(bankSubject, "银行存款", DC_CREDIT, amount, event));
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

    private boolean readBoolean(PostingEvent event, String key) {
        Object value = event.getBillData().get(key);
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.parseBoolean(value.toString().trim());
    }

    private String readCode(PostingEvent event, String key, String defaultValue) {
        Object value = event.getBillData().get(key);
        if (value == null || value.toString().trim().isEmpty()) {
            return defaultValue;
        }
        return value.toString().trim();
    }
}
