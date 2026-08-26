package app.erp.fin.service.entity;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.dao.AcctSchemaResolver;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.ServiceContextImpl;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * F2.1：①（P1-CK-fin-002）postVoucher 借贷平衡校验——不平衡拒（保持 DRAFT）、平衡/红字风格过、
 * 头合计重算；②（P1-CK-fin-004）AcctSchemaResolver ACTIVE 过滤——仅 INACTIVE → null。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinVoucherBalanceAndSchemaFilter extends PeriodCloseTestSupport {

    @Inject
    app.erp.fin.biz.IErpFinVoucherBiz voucherBiz;

    private String seedPeriodAndSubjects() {
        return ormTemplate.runInSession(session -> {
            String pid = seedOpenPeriod("2026-09", 2026, 9);
            Map<String, app.erp.md.dao.entity.ErpMdSubject> subjects = new HashMap<>();
            subjects.put("1001", seedSubject("1001", "库存现金", "ASSET", ErpFinConstants.DC_DEBIT));
            subjects.put("6001", seedSubject("6001", "主营业务收入",
                    ErpFinConstants.SUBJECT_CLASS_INCOME, ErpFinConstants.DC_CREDIT));
            // 不平衡凭证：借 100 / 贷 60
            seedDraftVoucher("V-BAL-UNBALANCED", pid, LocalDate.of(2026, 9, 10), subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("100")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("60")});
            // 平衡凭证：借 80 / 贷 80（头合计故意 stale=999 验证重算）
            seedDraftVoucher("V-BAL-OK", pid, LocalDate.of(2026, 9, 11), subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("80")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("80")});
            // 红字风格平衡凭证（同向取负对称）
            seedDraftVoucher("V-BAL-NEG", pid, LocalDate.of(2026, 9, 12), subjects,
                    new Object[]{"1001", "库存现金", ErpFinConstants.DC_DEBIT, new BigDecimal("-50")},
                    new Object[]{"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, new BigDecimal("-50")});
            return pid;
        });
    }

    private String voucherIdByCode(String code) {
        return ormTemplate.runInSession(sess -> {
            for (ErpFinVoucher v : daoProvider.daoFor(ErpFinVoucher.class).findAllByQuery(
                    new io.nop.api.core.beans.query.QueryBean())) {
                if (code.equals(v.getCode())) {
                    return v.getId();
                }
            }
            return null;
        });
    }

    @Test
    public void testUnbalancedVoucherRejected() {
        seedPeriodAndSubjects();
        String id = voucherIdByCode("V-BAL-UNBALANCED");
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> voucherBiz.postVoucher(id, CTX)),
                "F2.1：不平衡凭证过账应被拒（修复前无校验直接 POSTED）");
        assertEquals("erp.err.fin.posting.unbalanced", ex.getErrorCode(),
                "错误码 = 借贷不平衡");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_DRAFT,
                ormTemplate.runInSession(sess -> daoProvider.daoFor(ErpFinVoucher.class)
                        .getEntityById(id).getDocStatus()),
                "凭证保持 DRAFT");
    }

    @Test
    public void testBalancedVoucherPostsAndHeaderRecalculated() {
        seedPeriodAndSubjects();
        String id = voucherIdByCode("V-BAL-OK");
        // 头合计故意置 stale 值，验证过账时重算
        ormTemplate.runInSession(sess -> {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(id);
            v.setTotalDebit(new BigDecimal("999"));
            v.setTotalCredit(new BigDecimal("999"));
            daoProvider.daoFor(ErpFinVoucher.class).saveOrUpdateEntity(v);
            return null;
        });
        ErpFinVoucher posted = ormTemplate.runInSession(session -> voucherBiz.postVoucher(id, CTX));
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, posted.getDocStatus(), "平衡凭证过账成功");
        assertEquals(0, posted.getTotalDebit().compareTo(new BigDecimal("80")), "头合计重算 Σdebit=80");
        assertEquals(0, posted.getTotalCredit().compareTo(new BigDecimal("80")), "头合计重算 Σcredit=80");
    }

    @Test
    public void testNegativeRedStyleBalancedPasses() {
        seedPeriodAndSubjects();
        String id = voucherIdByCode("V-BAL-NEG");
        ErpFinVoucher posted = ormTemplate.runInSession(session -> voucherBiz.postVoucher(id, CTX));
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, posted.getDocStatus(),
                "红字风格平衡凭证（-50/-50）不误伤");
    }

    @Test
    public void testResolverOnlyReturnsActiveSchema() {
        ormTemplate.runInSession(sess -> {
            app.erp.md.dao.entity.ErpMdOrganization org = new app.erp.md.dao.entity.ErpMdOrganization();
            org.setCode("ORG-F21");
            org.setName("F2.1测试组织");
            org.setOrgType("COMPANY");
            org.setStatus("ACTIVE");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdOrganization.class).saveEntity(org);

            app.erp.md.dao.entity.ErpMdAcctSchema active = new app.erp.md.dao.entity.ErpMdAcctSchema();
            active.setCode("SCH-F21-ACT");
            active.setName("F2.1启用账套");
            active.setOrgId(org.getId());
            active.setNature("TAX");
            active.setStatus("ACTIVE");
            active.setFunctionalCurrencyId("1");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class).saveEntity(active);

            app.erp.md.dao.entity.ErpMdAcctSchema inactive = new app.erp.md.dao.entity.ErpMdAcctSchema();
            inactive.setCode("SCH-F21-INACT");
            inactive.setName("F2.1停用账套");
            inactive.setOrgId(org.getId());
            inactive.setNature("FINANCIAL");
            inactive.setStatus("INACTIVE");
            inactive.setFunctionalCurrencyId("1");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class).saveEntity(inactive);
            return org.getId();
        }).toString();

        String orgId = ormTemplate.runInSession(sess -> {
            for (app.erp.md.dao.entity.ErpMdOrganization o : daoProvider
                    .daoFor(app.erp.md.dao.entity.ErpMdOrganization.class).findAllByQuery(
                            new io.nop.api.core.beans.query.QueryBean())) {
                if ("ORG-F21".equals(o.getCode())) {
                    return o.getId();
                }
            }
            return null;
        });
        assertNotNull(orgId);

        // 混合：仅返回 ACTIVE（即使 INACTIVE 的 nature=FINANCIAL 优先级更高）
        String resolved = AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
        assertNotNull(resolved, "有 ACTIVE 账套时应解析成功");
        app.erp.md.dao.entity.ErpMdAcctSchema picked = ormTemplate.runInSession(sess ->
                daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class).getEntityById(resolved));
        assertEquals("ACTIVE", picked.getStatus(), "F2.1：解析结果必须是 ACTIVE 账套（修复前 INACTIVE FINANCIAL 可能胜出）");

        // 仅 INACTIVE → null（→ F1.4 引擎 fail-closed 闭环）
        String activeId = resolved;
        ormTemplate.runInSession(sess -> {
            app.erp.md.dao.entity.ErpMdAcctSchema s = daoProvider
                    .daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class).getEntityById(activeId);
            s.setStatus("INACTIVE");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdAcctSchema.class).saveOrUpdateEntity(s);
            return null;
        });
        assertNull(AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId),
                "F2.1：仅 INACTIVE 账套时返回 null（修复前返回 INACTIVE 账套）");
    }

    private void seedDraftVoucher(String vcode, String periodId, LocalDate date,
                                   Map<String, app.erp.md.dao.entity.ErpMdSubject> subjects, Object[]... lines) {
        io.nop.dao.api.IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        BigDecimal total = BigDecimal.ZERO;
        for (Object[] l : lines) {
            total = total.add((BigDecimal) l[3]);
        }
        ErpFinVoucher v = new ErpFinVoucher();
        v.setCode(vcode);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(date);
        v.setOrgId("1");
        v.setAcctSchemaId("1");
        v.setPeriodId(periodId);
        v.setTotalDebit(total);
        v.setTotalCredit(total);
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_DRAFT);
        vDao.saveEntity(v);
        io.nop.dao.api.IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> lDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        int lineNo = 1;
        for (Object[] l : lines) {
            app.erp.md.dao.entity.ErpMdSubject subj = subjects.get((String) l[0]);
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
            line.setCurrencyId("1");
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountSource(amt);
            line.setAmountFunctional(amt);
            line.setAcctSchemaId("1");
            lDao.saveEntity(line);
        }
    }
}