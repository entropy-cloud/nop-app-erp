package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.provider.CreditFacilityInterestAcctDocProvider;
import app.erp.fin.service.posting.provider.EmployeeAdvanceAcctDocProvider;
import app.erp.fin.service.posting.provider.ExpenseClaimAcctDocProvider;
import app.erp.fin.service.posting.provider.NotesPayableAcctDocProvider;
import app.erp.fin.service.posting.provider.NotesReceivableAcctDocProvider;
import io.nop.core.unittest.BaseTestCase;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * finance 域 5 Provider GL 映射 accountKey 接入单元测试（plan 2026-07-24-1351-1 Phase 4）。
 * BankReconAdjAcctDocProvider 已在 A1 接入（4 键），本测试仅复核新增 5 Provider。
 * 命中/fallback/strict 三路径由 resolver 承担（{@code TestErpFinGlMappingResolver}）。
 */
public class TestErpFinAcctDocProviderAccountKey extends BaseTestCase {

    private static final BigDecimal AMT = new BigDecimal("100");

    @Test
    public void testEmployeeAdvance() {
        EmployeeAdvanceAcctDocProvider p = new EmployeeAdvanceAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.EMPLOYEE_ADVANCE);
        e.getBillData().put("TOTAL", AMT);
        e.getBillData().put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, 1L);
        assertKeys(p.createFacts(e, null), "EMPLOYEE_ADVANCE_RECEIVABLE", "BANK_DEPOSIT");
    }

    @Test
    public void testEmployeeAdvanceSettleCash() {
        EmployeeAdvanceAcctDocProvider p = new EmployeeAdvanceAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE);
        e.getBillData().put("TOTAL", AMT);
        e.getBillData().put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, 1L);
        e.getBillData().put(ErpFinConstants.BILL_DATA_SETTLE_TYPE, ErpFinConstants.SETTLE_TYPE_CASH);
        assertKeys(p.createFacts(e, null), "BANK_DEPOSIT", "EMPLOYEE_ADVANCE_RECEIVABLE");
    }

    @Test
    public void testEmployeeAdvanceSettleOffset() {
        EmployeeAdvanceAcctDocProvider p = new EmployeeAdvanceAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.EMPLOYEE_ADVANCE_SETTLE);
        e.getBillData().put("TOTAL", AMT);
        e.getBillData().put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, 1L);
        e.getBillData().put(ErpFinConstants.BILL_DATA_SETTLE_TYPE, "OFFSET");
        assertKeys(p.createFacts(e, null), "EMPLOYEE_PAYABLE", "EMPLOYEE_ADVANCE_RECEIVABLE");
    }

    @Test
    public void testExpenseClaimOwnAccount() {
        ExpenseClaimAcctDocProvider p = new ExpenseClaimAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.EXPENSE_CLAIM);
        e.getBillData().put(ErpFinConstants.BILL_DATA_TOTAL_AMOUNT, new BigDecimal("87"));
        e.getBillData().put(ErpFinConstants.BILL_DATA_TOTAL_TAX_AMOUNT, new BigDecimal("13"));
        e.getBillData().put(ErpFinConstants.BILL_DATA_TOTAL_AMOUNT_WITH_TAX, AMT);
        e.getBillData().put(ErpFinConstants.BILL_DATA_PAYMENT_MODE, ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        e.getBillData().put(ErpFinConstants.BILL_DATA_EMPLOYEE_ID, 1L);
        assertKeys(p.createFacts(e, null), "ADMIN_EXPENSE", "INPUT_VAT", "EMPLOYEE_PAYABLE");
    }

    @Test
    public void testNotesPayableIssued() {
        NotesPayableAcctDocProvider p = new NotesPayableAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.NOTES_PAYABLE_ISSUED);
        e.getBillData().put(ErpFinConstants.BILL_DATA_FACE_AMOUNT, AMT);
        e.getBillData().put(ErpFinConstants.BILL_DATA_PARTNER_ID, 1L);
        assertKeys(p.createFacts(e, null), "ACCOUNTS_PAYABLE", "NOTES_PAYABLE");
    }

    @Test
    public void testNotesPayableHonored() {
        NotesPayableAcctDocProvider p = new NotesPayableAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.NOTES_PAYABLE_HONORED);
        e.getBillData().put(ErpFinConstants.BILL_DATA_FACE_AMOUNT, AMT);
        assertKeys(p.createFacts(e, null), "NOTES_PAYABLE", "BANK_DEPOSIT");
    }

    @Test
    public void testCreditFacilityInterest() {
        CreditFacilityInterestAcctDocProvider p = new CreditFacilityInterestAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.CREDIT_FACILITY_INTEREST);
        e.getBillData().put("TOTAL", AMT);
        assertKeys(p.createFacts(e, null), "FINANCIAL_EXPENSE", "BANK_DEPOSIT");
    }

    @Test
    public void testNotesReceivableReceived() {
        NotesReceivableAcctDocProvider p = new NotesReceivableAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.NOTES_RECEIVABLE_RECEIVED);
        e.getBillData().put(ErpFinConstants.BILL_DATA_FACE_AMOUNT, AMT);
        e.getBillData().put(ErpFinConstants.BILL_DATA_PARTNER_ID, 1L);
        assertKeys(p.createFacts(e, null), "NOTES_RECEIVABLE", "AR");
    }

    @Test
    public void testNotesReceivableCollection() {
        NotesReceivableAcctDocProvider p = new NotesReceivableAcctDocProvider();
        PostingEvent e = event(ErpFinBusinessType.NOTES_RECEIVABLE_COLLECTION);
        e.getBillData().put(ErpFinConstants.BILL_DATA_FACE_AMOUNT, AMT);
        assertKeys(p.createFacts(e, null), "BANK_DEPOSIT", "NOTES_RECEIVABLE");
    }

    private PostingEvent event(ErpFinBusinessType type) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(type);
        return event;
    }

    private void assertKeys(List<VoucherFact> facts, String... expectedKeys) {
        for (VoucherFact fact : facts) {
            assertNotNull(fact.getAccountKey(), "accountKey 必须非空");
            assertFalse(fact.getAccountKey().trim().isEmpty(), "accountKey 不能为空白");
        }
        assertEquals(expectedKeys.length, facts.size(), "fact 行数不匹配");
        for (int i = 0; i < expectedKeys.length; i++) {
            assertEquals(expectedKeys[i], facts.get(i).getAccountKey(),
                    "第 " + i + " 条 fact accountKey 语义不匹配");
        }
    }
}
