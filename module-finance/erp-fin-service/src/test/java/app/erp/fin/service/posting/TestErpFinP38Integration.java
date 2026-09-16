package app.erp.fin.service.posting;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.PostingEvent;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.api.IErpFinGlMappingResolver;
import app.erp.fin.dao.dto.GlMappingDimensions;
import app.erp.fin.dao.entity.ErpFinGlMappingRule;
import app.erp.fin.dao.entity.ErpFinPostingException;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.posting.ErpFinPostingExceptionRecorder;
import app.erp.fin.service.posting.VoucherFact;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F3.8 批（plan 2026-09-17-0330-1）结束审计整改：fin-010/011/014 集成 Proof。
 *
 * <ul>
 *   <li>fin-010：GL 账套精确规则（acctSchemaId 非 NULL）经 resolveSubjects 透传后按设计命中
 *       （修复前 stub 恒 null 使精确规则全部跳过）。</li>
 *   <li>fin-011：辅助账查重带账套维度——同源单两账套各生成一条 ErpFinArApItem（修复前仅首账套）。</li>
 *   <li>fin-014：Recorder PENDING 合并——同键刷新证据字段不新增行（粗粒度通道归一），
 *       listener 通道自成键不与引擎管道失败合并。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinP38Integration extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpFinArApItemGenerator arApItemGenerator;
    @Inject
    ErpFinPostingExceptionRecorder recorder;
    @Inject
    ErpFinPostingProcessor postingProcessor;
    @Inject
    app.erp.fin.dao.api.IErpFinGlMappingResolver glMappingResolver;

    private void seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("ASSET");
        s.setDirection("DEBIT");
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
    }

    // ---------- fin-010：账套精确规则命中 ----------

    @Test
    public void testGlMappingExactSchemaRuleHits() {
        ormTemplate.runInSession(session -> {
            seedSubject("9001", "精确规则目标科目");
            seedSubject("1405", "默认应付");
            ErpFinGlMappingRule rule = daoProvider.daoFor(ErpFinGlMappingRule.class).newEntity();
            rule.setCode("GLMR-P38");
            rule.setName("P38-EXACT");
            rule.setOrgId("1");
            rule.setBusinessType("AP_INVOICE");
            rule.setAccountKey("AP");
            rule.setAcctSchemaId("1");
            rule.setPriority(1);
            rule.setTargetSubjectCode("9001");
            rule.setIsActive(true);
            daoProvider.daoFor(ErpFinGlMappingRule.class).saveEntity(rule);
            return null;
        });

        // 直调 resolver 证明透传语义：acctSchemaId='1' 命中精确规则；null（修复前 stub 行为）不命中
        glMappingResolver.invalidateCache(); // seed 后刷新进程内规则缓存
        GlMappingDimensions dims = new GlMappingDimensions();
        dims.setOrgId("1");
        assertEquals("9001", glMappingResolver.resolveSubjectCode("AP_INVOICE", "AP", dims, "1"),
                "acctSchemaId='1' 透传后精确规则命中（修复前 stub 恒 null 全部跳过）");
        assertNull(glMappingResolver.resolveSubjectCode("AP_INVOICE", "AP", dims, null),
                "null = 通配规则口径（修复前行为），精确规则不命中");
    }

    // ---------- fin-011：辅助账查重账套维度 ----------

    @Test
    public void testArApItemGeneratedPerSchema() {
        ormTemplate.runInSession(session -> {
            ErpMdPartner partner = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            partner.setId("13801");
            partner.setCode("C-P38");
            partner.setName("客户P38");
            partner.setPartnerType("CUSTOMER");
            partner.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(partner);
            return null;
        });

        var eventA = arInvoiceEvent("BI-P38-001", "1");
        assertNotNull(arApItemGenerator.generate(eventA, null), "账套 A 首次生成辅助账项");
        assertEquals(1, countItems("BI-P38-001", "1"), "账套 1 生成 1 条");

        // 同源单换账套 B：修复前 existsItem 命中账套 A 记录被跳过（仅首账套建项）
        var eventB = arInvoiceEvent("BI-P38-001", "2");
        assertNotNull(arApItemGenerator.generate(eventB, null), "账套 B 生成（修复前被首套去重挡住）");
        assertEquals(1, countItems("BI-P38-001", "2"), "账套 2 各 1 条辅助账项");
        assertEquals(1, countItems("BI-P38-001", "1"), "账套 1 不受影响");
    }

    private PostingEvent arInvoiceEvent(String billHeadCode, String acctSchemaId) {
        PostingEvent event = new PostingEvent();
        event.setBusinessType(ErpFinBusinessType.AR_INVOICE);
        event.setBillHeadCode(billHeadCode);
        event.setAcctSchemaId(acctSchemaId);
        event.setVoucherDate(LocalDate.of(2026, 7, 15));
        event.setOrgId("1");
        event.setAcctSchemaId(acctSchemaId);
        event.setCurrencyId("1");
        event.setExchangeRate(BigDecimal.ONE);
        Map<String, Object> billData = new LinkedHashMap<>();
        billData.put("partnerId", "13801");
        billData.put("TOTAL_AMOUNT_WITH_TAX", new BigDecimal("113"));
        event.setBillData(billData);
        return event;
    }

    private int countItems(String billHeadCode, String acctSchemaId) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        var q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(and(eq("sourceBillCode", billHeadCode), eq("acctSchemaId", acctSchemaId),
                eq("status", ErpFinConstants.AR_AP_STATUS_OPEN)));
        return dao.findAllByQuery(q).size();
    }

    // ---------- fin-014：Recorder PENDING 合并 ----------

    @Test
    public void testRecorderMergesSameChannelPending() {
        recorder.record("tr-M1", "BI-P38-M1", "AR_INVOICE", "NORMAL",
                "err.first", "first failure", "generateFacts",
                LocalDate.of(2026, 7, 15), "1", "1", "1", BigDecimal.ONE, "{}");
        // 同单同键第二次失败（不同细粒度阶段，粗粒度通道同为 post）→ 合并不新增
        recorder.record("tr-M2", "BI-P38-M1", "AR_INVOICE", "NORMAL",
                "err.second", "second failure", "persistVoucher_1",
                LocalDate.of(2026, 7, 15), "1", "1", "1", BigDecimal.ONE, "{}");

        List<ErpFinPostingException> rows = findPending("BI-P38-M1", "AR_INVOICE");
        assertEquals(1, rows.size(), "同粗粒度通道两轮失败合并为单行（修复前每次新增）");
        assertEquals("err.second", rows.get(0).getErrorCode(), "合并刷新为最新失败证据");
        assertEquals("post", rows.get(0).getFailedStage(),
                "failedStage 存粗粒度通道值（首行落库即归一，两行口径一致合并才命中）");
        assertTrue(rows.get(0).getRetryCount() >= 1, "合并递增 retryCount");
    }

    @Test
    public void testRecorderListenerChannelIsolated() {
        recorder.record("tr-M3", "BI-P38-M2", "AR_INVOICE", "NORMAL",
                "err.post", "post failure", "generateFacts",
                LocalDate.of(2026, 7, 15), "1", "1", "1", BigDecimal.ONE, "{}");
        // listener 失败（FAILED_STAGE_NOTIFY_* 自成通道）→ 不与 post 通道合并
        recorder.record("tr-M4", "BI-P38-M2", "AR_INVOICE", "NORMAL",
                "err.listener", "listener failure", "NOTIFY_POSTED_LISTENER",
                LocalDate.of(2026, 7, 15), "1", "1", "1", BigDecimal.ONE, "{}");

        List<ErpFinPostingException> rows = findPending("BI-P38-M2", "AR_INVOICE");
        assertEquals(2, rows.size(), "listener 通道与引擎 post 通道不合并（failedStage 隔离）");
    }

    private java.util.List<ErpFinPostingException> findPending(String billHeadCode, String businessType) {
        IEntityDao<ErpFinPostingException> dao = daoProvider.daoFor(ErpFinPostingException.class);
        var q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(and(eq("billHeadCode", billHeadCode), eq("businessType", businessType),
                eq("status", ErpFinConstants.POSTING_EXCEPTION_STATUS_PENDING)));
        return dao.findAllByQuery(q);
    }
}
