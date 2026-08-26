package app.erp.fin.service.posting;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F2.1（P1-CK-fin-001）：FinPostedListener 正向过账回写单测（finance 本域）。
 * EXPENSE_CLAIM/EMPLOYEE_ADVANCE posted 翻转 + 已 posted 跳过 + 合成码 miss no-op。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestFinPostedListenerWriteback extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testExpenseClaimPostedFlipsTrue() {
        String code = "EC-F21-LIS-001";
        ormTemplate.runInSession(session -> {
            seedClaim(code, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            dispatch("EXPENSE_CLAIM", code);
            session.flush();
            return null;
        });

        ErpFinExpenseClaim after = findClaim(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "悬挂回写：报销单 posted false→true");
        assertNotNull(after.getPostedAt(), "postedAt 回写");
    }

    @Test
    public void testEmployeeAdvancePostedFlipsTrue() {
        String code = "EA-F21-LIS-001";
        ormTemplate.runInSession(session -> {
            seedAdvance(code, false);
            return null;
        });

        ormTemplate.runInSession(session -> {
            dispatch("EMPLOYEE_ADVANCE", code);
            session.flush();
            return null;
        });

        assertEquals(Boolean.TRUE, findAdvance(code).getPosted(), "悬挂回写：借款单 posted false→true");
    }

    @Test
    public void testAlreadyPostedSkipsUpdate() {
        String code = "EC-F21-LIS-002";
        ormTemplate.runInSession(session -> {
            seedClaim(code, true);
            return null;
        });
        int versionBefore = findClaim(code).getVersion();

        ormTemplate.runInSession(session -> {
            dispatch("EXPENSE_CLAIM", code);
            session.flush();
            return null;
        });

        ErpFinExpenseClaim after = findClaim(code);
        assertEquals(Boolean.TRUE, after.getPosted(), "已 posted 保持 true");
        assertEquals(versionBefore, after.getVersion(), "已 true 跳过：version 无谓递增被防住");
    }

    @Test
    public void testSettleSyntheticCodeMissIsNoOp() {
        ormTemplate.runInSession(session -> {
            dispatch("EMPLOYEE_ADVANCE_SETTLE",
                    "EA-CASH-REPAY-EA-F21-LIS-001-1787757000000");
            return null;
        });
        assertTrue(true, "EA-CASH-REPAY-* 合成码无独立源单 → findByCode miss no-op 不抛异常");
    }

    // ---------- helpers ----------

    private void dispatch(String businessType, String billHeadCode) {
        FinPostedListener listener = new FinPostedListener();
        listener.daoProvider = daoProvider;
        VoucherPostedEvent event = new VoucherPostedEvent();
        event.setBusinessType(businessType);
        event.setBillHeadCode(billHeadCode);
        listener.onVoucherPosted(event, CTX);
    }

    private ErpFinExpenseClaim findClaim(String code) {
        return ormTemplate.runInSession(session -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            q.setLimit(1);
            java.util.List<ErpFinExpenseClaim> list =
                    daoProvider.daoFor(ErpFinExpenseClaim.class).findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private ErpFinEmployeeAdvance findAdvance(String code) {
        return ormTemplate.runInSession(session -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("code", code));
            q.setLimit(1);
            java.util.List<ErpFinEmployeeAdvance> list =
                    daoProvider.daoFor(ErpFinEmployeeAdvance.class).findAllByQuery(q);
            return list.isEmpty() ? null : list.get(0);
        });
    }

    private void seedClaim(String code, boolean posted) {
        ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
        claim.setCode(code);
        claim.setOrgId("1");
        claim.setBusinessDate(LocalDate.of(2026, 8, 10));
        claim.setCurrencyId("1");
        claim.setExchangeRate(BigDecimal.ONE);
        claim.setAmountSource(new BigDecimal("200"));
        claim.setAmountFunctional(new BigDecimal("200"));
        claim.setPaymentMode(app.erp.fin.service.ErpFinConstants.PAYMENT_MODE_OWN_ACCOUNT);
        claim.setClaimantId("1");
        claim.setDocStatus("APPROVED");
        claim.setApproveStatus("APPROVED");
        claim.setPosted(posted);
        if (posted) {
            claim.setPostedAt(CoreMetrics.currentTimestamp());
            claim.setPostedBy("poster");
        }
        daoProvider.daoFor(ErpFinExpenseClaim.class).saveEntity(claim);
    }

    private void seedAdvance(String code, boolean posted) {
        ErpFinEmployeeAdvance advance = new ErpFinEmployeeAdvance();
        advance.setCode(code);
        advance.setOrgId("1");
        advance.setEmployeeId("1");
        advance.setAdvanceType("EXPENSE_ADVANCE");
        advance.setBusinessDate(LocalDate.of(2026, 8, 10));
        advance.setCurrencyId("1");
        advance.setExchangeRate(BigDecimal.ONE);
        advance.setAmountSource(new BigDecimal("500"));
        advance.setAmountFunctional(new BigDecimal("500"));
        advance.setDocStatus("APPROVED");
        advance.setApproveStatus("APPROVED");
        advance.setPosted(posted);
        if (posted) {
            advance.setPostedAt(CoreMetrics.currentTimestamp());
            advance.setPostedBy("poster");
        }
        daoProvider.daoFor(ErpFinEmployeeAdvance.class).saveEntity(advance);
    }
}
