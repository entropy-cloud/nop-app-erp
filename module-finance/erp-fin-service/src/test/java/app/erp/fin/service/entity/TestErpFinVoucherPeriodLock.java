package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IEntityDao;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CLOSED_FINAL 期间凭证锁定守卫测试（P1-MA2-021，R1.11）。
 * 验证 postVoucher/reverseVoucher 在所属期间 CLOSED/CLOSED_FINAL 时抛 {@code ERR_FIN_VOUCHER_PERIOD_LOCKED}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:period-close-end-to-end-test.yaml")
public class TestErpFinVoucherPeriodLock extends PeriodCloseTestSupport {

    @Inject
    IErpFinVoucherBiz voucherBiz;

    @Test
    public void testPostVoucherBlockedWhenPeriodClosedFinal() {
        Long periodId = seedPeriodWithVouchers("2025-07");
        ErpFinAccountingPeriod period = setPeriodStatus(periodId, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);

        ErpFinVoucher draftVoucher = findDraftVoucher(periodId);
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> voucherBiz.postVoucher(draftVoucher.getId(), CTX)));
        assertEquals(ErpFinErrors.ERR_FIN_VOUCHER_PERIOD_LOCKED.getErrorCode(), ex.getErrorCode());
        assertTrue(ex.getMessage().contains("CLOSED_FINAL"),
                "错误消息应包含期间状态 CLOSED_FINAL");
    }

    @Test
    public void testPostVoucherBlockedWhenPeriodClosed() {
        Long periodId = seedPeriodWithVouchers("2025-08");
        setPeriodStatus(periodId, ErpFinConstants.PERIOD_STATUS_CLOSED);

        ErpFinVoucher draftVoucher = findDraftVoucher(periodId);
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> voucherBiz.postVoucher(draftVoucher.getId(), CTX)));
        assertEquals(ErpFinErrors.ERR_FIN_VOUCHER_PERIOD_LOCKED.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testReverseVoucherBlockedWhenPeriodClosedFinal() {
        Long periodId = seedPeriodWithVouchers("2025-09");
        setPeriodStatus(periodId, ErpFinConstants.PERIOD_STATUS_CLOSED_FINAL);

        ErpFinVoucher postedVoucher = findPostedVoucher(periodId);
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> voucherBiz.reverseVoucher(postedVoucher.getId(), CTX)));
        assertEquals(ErpFinErrors.ERR_FIN_VOUCHER_PERIOD_LOCKED.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testPostVoucherAllowedWhenPeriodOpen() {
        Long periodId = seedPeriodWithVouchers("2025-10");
        setPeriodStatus(periodId, ErpFinConstants.PERIOD_STATUS_OPEN);

        ErpFinVoucher draftVoucher = findDraftVoucher(periodId);
        ErpFinVoucher result = ormTemplate.runInSession(session ->
                voucherBiz.postVoucher(draftVoucher.getId(), CTX));
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, result.getDocStatus(),
                "OPEN 期间凭证可正常过账");
    }

    private Long seedPeriodWithVouchers(String code) {
        return ormTemplate.runInSession(session -> {
            int year = Integer.parseInt(code.substring(0, 4));
            int month = Integer.parseInt(code.substring(5));
            Long pid = seedOpenPeriod(code, year, month);
            Map<String, ErpMdSubject> subjects = new HashMap<>();
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6001", seedSubject("6001", "主营业务收入", ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            // 已过账凭证（reverseVoucher 测试用）
            seedPostedVoucher("V-" + code + "-POSTED", pid, LocalDate.of(year, month, 10), subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("50")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("50")});
            // 草稿凭证（postVoucher 测试用）
            seedDraftVoucher("V-" + code + "-DRAFT", pid, LocalDate.of(year, month, 15), subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("30")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("30")});
            return pid;
        });
    }

    private void seedDraftVoucher(String vcode, Long periodId, LocalDate date,
                                   Map<String, ErpMdSubject> subjects, Object[]... lines) {
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] l : lines) {
            total = total.add((BigDecimal) l[3]);
        }
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(vcode);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(date);
        v.setOrgId(1L);
        v.setAcctSchemaId(1L);
        v.setPeriodId(periodId);
        v.setTotalDebit(total);
        v.setTotalCredit(total);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_DRAFT);
        vDao.saveEntity(v);
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> lDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        int lineNo = 1;
        for (Object[] l : lines) {
            ErpMdSubject subj = subjects.get((String) l[0]);
            String dc = (String) l[2];
            BigDecimal amt = (BigDecimal) l[3];
            app.erp.fin.dao.entity.ErpFinVoucherLine line = new app.erp.fin.dao.entity.ErpFinVoucherLine();
            line.setVoucherId(v.getId());
            line.setLineNo(lineNo++);
            line.setSubjectId(subj.getId());
            line.setSubjectCode((String) l[0]);
            line.setSubjectName((String) l[1]);
            line.setDcDirection(dc);
            line.setDebitAmount(ErpFinConstants.DC_DEBIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCreditAmount(ErpFinConstants.DC_CREDIT.equals(dc) ? amt : BigDecimal.ZERO);
            line.setCurrencyId(1L);
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountSource(amt);
            line.setAmountFunctional(amt);
            line.setAcctSchemaId(1L);
            lDao.saveEntity(line);
        }
    }

    private ErpFinAccountingPeriod setPeriodStatus(Long periodId, String status) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod p = dao.getEntityById(periodId);
            p.setStatus(status);
            dao.updateEntity(p);
            return p;
        });
    }

    private ErpFinVoucher findDraftVoucher(Long periodId) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("periodId", periodId));
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("docStatus", ErpFinConstants.VOUCHER_STATUS_DRAFT));
            return dao.findAllByQuery(q).get(0);
        });
    }

    private ErpFinVoucher findPostedVoucher(Long periodId) {
        return ormTemplate.runInSession(session -> {
            IEntityDao<ErpFinVoucher> dao = daoProvider.daoFor(ErpFinVoucher.class);
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("periodId", periodId));
            q.addFilter(io.nop.api.core.beans.FilterBeans.eq("docStatus", ErpFinConstants.VOUCHER_STATUS_POSTED));
            return dao.findAllByQuery(q).get(0);
        });
    }
}
