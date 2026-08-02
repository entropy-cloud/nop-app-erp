package app.erp.fin.service.processor;

import app.erp.common.test.ThreadLocalFrozenClock;
import app.erp.fin.dao.entity.ErpFinVoucherTemplate;
import app.erp.fin.dao.entity.ErpFinVoucherTemplateLine;
import app.erp.fin.service.posting.ErpFinPostingErrors;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 凭证模板 {@code findActiveTemplate} 日期敏感路径聚焦测试（plan 2026-08-02-0650-1 Phase 1 Proof）。
 *
 * <p>Q6 successor 闭合：生产侧 {@link ErpFinVoucherTemplateRenderTemplateProcessor#findActiveTemplate}
 * 原读 {@code LocalDate.now()}（墙钟），现改读 {@link CoreMetrics#today()}（可冻结）。本测试冻结时钟到
 * 有效期边界日，断言 {@code validFrom}/{@code validTo} 过滤按<b>冻结日</b>而非墙钟判定：
 * <ul>
 *   <li>{@code validTo = 冻结日-1} 的模板被排除（renderTemplate 抛 ERR_TEMPLATE_NOT_FOUND）；</li>
 *   <li>{@code validFrom = 冻结日+1} 的模板被排除（renderTemplate 抛 ERR_TEMPLATE_NOT_FOUND）——
 *       区分价值：墙钟若已过 {@code 冻结日+1}（如真实今天 2026-08-02 > 2026-07-18）则该模板<b>会被选中</b>，
 *       仅冻结时钟下才被排除，故此断言证明走冻结时钟；</li>
 *   <li>跨冻结日的模板（validFrom ≤ 冻结日 ≤ validTo）被选中（renderTemplate 返回行）。</li>
 * </ul>
 *
 * <p>用 {@link JunitBaseTestCase}（容器+DB，无快照），方法内手动
 * {@link ThreadLocalFrozenClock#ensureRegistered()}/{@link ThreadLocalFrozenClock#install(LocalDate)}/
 * {@link ThreadLocalFrozenClock#clear()}，{@code @AfterEach} 兜底防线程本地泄漏。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinVoucherTemplateActiveByDate extends JunitBaseTestCase {

    private static final LocalDate FROZEN = LocalDate.of(2026, 7, 17);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpFinVoucherTemplateRenderTemplateProcessor processor;

    @AfterEach
    void clearClock() {
        ThreadLocalFrozenClock.clear();
    }

    @Test
    public void testPastTemplateExcludedAtBoundary() {
        freezeAndRun(() -> {
            // validTo = 冻结日-1 → date.isAfter(validTo)=true → 排除
            seedTemplate("DATE-PAST", "2020-01-01", "2026-07-16", null);
            NopException ex = assertThrows(NopException.class, () ->
                    ormTemplate.runInSession(s ->
                            processor.renderTemplate("DATE-PAST", java.util.Collections.emptyMap())));
            assertEquals(ErpFinPostingErrors.ERR_TEMPLATE_NOT_FOUND.getErrorCode(), ex.getErrorCode(),
                    "validTo=冻结日-1 的模板应被排除（按冻结日判定）");
        });
    }

    @Test
    public void testFutureTemplateExcludedAtBoundary() {
        freezeAndRun(() -> {
            // validFrom = 冻结日+1 → date.isBefore(validFrom)=true → 排除
            // 区分价值：若走墙钟且真实今天 > 2026-07-18，则 date.isBefore(7/18)=false → 会被选中；
            // 仅冻结时钟下（today=7/17）才排除。此断言通过即证明读冻结时钟。
            seedTemplate("DATE-FUTURE", "2026-07-18", "2099-12-31", null);
            NopException ex = assertThrows(NopException.class, () ->
                    ormTemplate.runInSession(s ->
                            processor.renderTemplate("DATE-FUTURE", java.util.Collections.emptyMap())));
            assertEquals(ErpFinPostingErrors.ERR_TEMPLATE_NOT_FOUND.getErrorCode(), ex.getErrorCode(),
                    "validFrom=冻结日+1 的模板应被排除（按冻结日判定，证明走冻结时钟而非墙钟）");
        });
    }

    @Test
    public void testActiveTemplateSelectedAtBoundary() {
        freezeAndRun(() -> {
            // 跨冻结日（validFrom ≤ 冻结日 ≤ validTo）→ 选中
            seedTemplate("DATE-ACTIVE", "2020-01-01", "2099-12-31", "SUBJ-ACTIVE");
            List<Map<String, Object>> rows = ormTemplate.runInSession(s -> processor.renderTemplate("DATE-ACTIVE",
                    java.util.Collections.emptyMap()));
            assertEquals(1, rows.size(), "跨冻结日的模板应被选中并返回其行");
            assertEquals("SUBJ-ACTIVE", rows.get(0).get("subjectCode"));
        });
    }

    @Test
    public void testNoFreezeDelegatesToWallClock() {
        // 反向证明：不冻结时 CoreMetrics.today() == LocalDate.now()（生产运行时行为零变更核验）。
        // 不调 install，仅 ensureRegistered 挂 delegating clock（REF_DATE 未 set 委托 defaultClock）。
        ThreadLocalFrozenClock.ensureRegistered();
        LocalDate viaCoreMetrics = CoreMetrics.today();
        LocalDate viaWallClock = LocalDate.now();
        assertEquals(viaWallClock, viaCoreMetrics,
                "非冻结时 CoreMetrics.today() 应等于 LocalDate.now()（生产运行时行为不变）");
        assertTrue(!ThreadLocalFrozenClock.isFrozen(), "未 install 时线程本地应为非冻结");
    }

    private void freezeAndRun(Runnable body) {
        ThreadLocalFrozenClock.ensureRegistered();
        ThreadLocalFrozenClock.install(FROZEN);
        assertEquals(FROZEN, CoreMetrics.today(), "CoreMetrics.today() 应返回冻结日");
        body.run();
    }

    private void seedTemplate(String businessType, String validFrom, String validTo, String subjectCode) {
        ormTemplate.runInSession(session -> {
            IEntityDao<ErpFinVoucherTemplate> headDao = daoProvider.daoFor(ErpFinVoucherTemplate.class);
            ErpFinVoucherTemplate t = headDao.newEntity();
            t.setCode("TPL-" + businessType);
            t.setName("日期边界测试-" + businessType);
            t.setBusinessType(businessType);
            t.setIsActive(Boolean.TRUE);
            t.setValidFrom(LocalDate.parse(validFrom));
            t.setValidTo(LocalDate.parse(validTo));
            headDao.saveEntity(t);

            if (subjectCode != null) {
                IEntityDao<ErpFinVoucherTemplateLine> lineDao = daoProvider.daoFor(ErpFinVoucherTemplateLine.class);
                ErpFinVoucherTemplateLine line = lineDao.newEntity();
                line.setTemplateId(t.getId());
                line.setLineNo(1);
                line.setSubjectCode(subjectCode);
                line.setDcDirection("DEBIT");
                line.setAmountKey("DOC_TOTAL");
                lineDao.saveEntity(line);
            }
            return null;
        });
    }
}
