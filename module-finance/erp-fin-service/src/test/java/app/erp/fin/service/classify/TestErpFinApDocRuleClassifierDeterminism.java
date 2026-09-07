package app.erp.fin.service.classify;

import app.erp.fin.dao.entity.ErpFinApDocument;
import app.erp.fin.service.ErpFinConfigs;
import app.erp.md.dao.entity.ErpMdPartner;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-6（plan 2026-08-28-0219-1 Phase 1）：默认规则分类引擎确定性单测。
 *
 * <p>各用例使用互不包含的独占名称 token（H2 跨方法共享，防伙伴种子串扰）。
 *
 * <ul>
 *   <li>同输入两次分类首匹配确定一致（候选按 id 升序，无排序时 DB 返回序不定）；</li>
 *   <li>双向包含匹配保留（伙伴名是供应商名子串仍命中——名称过滤不下推的理由证明）；</li>
 *   <li>SUPPLIER 优先于 BOTH 的既有语义在排序后保持（fallback 扫描不因排序破坏）；</li>
 *   <li>解析要素 null 不再产生字面 "null" 幻影（docType/匹配路径收口）；</li>
 *   <li>超 {@code erp-fin.ap-doc-partner-match-limit} 截断 WARN 可观测（行为保持落人工门）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinApDocRuleClassifierDeterminism extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpFinApDocRuleClassifier classifier;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger classifierLogger;

    @BeforeEach
    void attachLogAppender() {
        classifierLogger = (Logger) LoggerFactory.getLogger(ErpFinApDocRuleClassifier.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        classifierLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppenderAndResetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConfigs.CONFIG_AP_DOC_PARTNER_MATCH_LIMIT,
                String.valueOf(ErpFinConfigs.DEFAULT_AP_DOC_PARTNER_MATCH_LIMIT));
        if (classifierLogger != null && logAppender != null) {
            classifierLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    /** 同输入两次分类：首匹配确定一致（id 升序，低 id 命中稳定）；双向包含保留。 */
    @Test
    public void testSameInputClassifiedDeterministically() {
        // 先插短名（反向包含：供应商输入含短伙伴名，id 更低），后插长名（正向包含）
        String shortId = seedPartner("P-DET-SHORT", "华东甲", "SUPPLIER");
        seedPartner("P-DET-FULL", "华东甲长名供应商", "SUPPLIER");
        Map<String, Object> parseResult = parseResult("华东甲长名供应商集团有限公司的发票", "华东甲长名供应商集团有限公司");

        ApDocClassification first = classifier.classify(newDoc(), parseResult, CTX);
        ApDocClassification second = classifier.classify(newDoc(), parseResult, CTX);
        assertNotNull(first.getPartnerId(), "应命中伙伴");
        assertEquals(first.getPartnerId(), second.getPartnerId(), "同输入两次分类首匹配确定一致");
        assertEquals(shortId, first.getPartnerId(), "id 升序 → 低 id 短名伙伴稳定命中（双向包含保留证明）");
    }

    /** SUPPLIER 优先于 BOTH 的既有语义在确定性排序后保持（BOTH 低 id 只作 fallback）。 */
    @Test
    public void testSupplierPriorityPreservedUnderOrdering() {
        seedPartner("P-DET-BOTH", "华南乙", "BOTH");
        String supplierId = seedPartner("P-DET-SUP", "华南乙供应商", "SUPPLIER");

        ApDocClassification c = classifier.classify(newDoc(),
                parseResult("华南乙供应商开出的发票", "华南乙供应商"), CTX);
        assertEquals(supplierId, c.getPartnerId(), "SUPPLIER 命中优先于 BOTH（低 id BOTH 仅 fallback）");
    }

    /** 解析要素缺失（null）：无字面 "null" 幻影——docType null、未识别供应商名、零分。 */
    @Test
    public void testNullParseElementsProduceNoPhantomValues() {
        Map<String, Object> parseResult = new HashMap<>();
        parseResult.put("excerpt", null);
        parseResult.put("supplierName", null);

        ApDocClassification c = classifier.classify(newDoc(), parseResult, CTX);

        assertNull(c.getDocType(), "excerpt null → docType null（非 \"null\" 字面路径）");
        assertNull(c.getPartnerId(), "supplierName null → 不匹配伙伴（\"null\" 守卫对齐）");
        assertEquals(0, BigDecimal.ZERO.compareTo(c.getConfidence()), "零要素 → 置信度 0");
        assertTrue(c.getReason().contains("supplier name not recognized"), "reason 不应含字面 null: " + c.getReason());
    }

    /** 超上限截断显式 WARN（可观测）；行为保持未匹配落人工门。 */
    @Test
    public void testPartnerCandidatesTruncationWarns() {
        AppConfig.getConfigProvider().assignConfigValue(ErpFinConfigs.CONFIG_AP_DOC_PARTNER_MATCH_LIMIT, "3");
        seedPartner("P-DET-T1", "西北丙无关公司一", "SUPPLIER");
        seedPartner("P-DET-T2", "西北丙无关公司二", "SUPPLIER");
        seedPartner("P-DET-T3", "西北丙无关公司三", "SUPPLIER");
        // 目标命中者 id 最高：limit=3 截断后不可见
        seedPartner("P-DET-T4", "西北丙目标供应商", "SUPPLIER");

        ApDocClassification c = classifier.classify(newDoc(),
                parseResult("发票正文", "西北丙目标供应商"), CTX);
        assertNull(c.getPartnerId(), "截断后唯一命中者不可见 → 未匹配（人工门语义不变）");
        ILoggingEvent warn = findLog("erp-fin-ap-doc-partner-candidates-truncated");
        assertNotNull(warn, "超上限截断应记录 WARN 日志");
        assertTrue(warn.getFormattedMessage().contains("limit=3"), "WARN 应含上限: " + warn.getFormattedMessage());
        assertTrue(warn.getFormattedMessage().contains("hit=3"), "WARN 应含命中数: " + warn.getFormattedMessage());
    }

    // ---------- helpers ----------

    private Map<String, Object> parseResult(String excerpt, String supplierName) {
        Map<String, Object> parseResult = new HashMap<>();
        parseResult.put("excerpt", excerpt);
        parseResult.put("supplierName", supplierName);
        return parseResult;
    }

    private ErpFinApDocument newDoc() {
        return ormTemplate.runInSession(sess -> {
            IEntityDao<ErpFinApDocument> dao = daoProvider.daoFor(ErpFinApDocument.class);
            return dao.newEntity();
        });
    }

    private String seedPartner(String code, String name, String partnerType) {
        return ormTemplate.runInSession(sess -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", code));
            List<ErpMdPartner> existing = dao.findAllByQuery(q);
            if (!existing.isEmpty()) {
                return existing.get(0).getId();
            }
            ErpMdPartner p = dao.newEntity();
            p.setCode(code);
            p.setName(name);
            p.setPartnerType(partnerType);
            p.setStatus("ACTIVE");
            dao.saveEntity(p);
            return p.getId();
        });
    }

    private ILoggingEvent findLog(String marker) {
        for (ILoggingEvent event : logAppender.list) {
            if (event.getFormattedMessage().contains(marker)) {
                return event;
            }
        }
        return null;
    }
}
