package app.erp.common.org;

import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M1.3 ErpOrgContext String 语义单元测试（plan 2026-08-21-1045-2 Phase 2）。
 *
 * <p>纯逻辑测试（无 DB / 无 IoC，同 {@code TestMaskAuditRecorder} 范式）：
 * 覆盖 context attr → {@code currentOrgId} 的过渡期宽容转换矩阵（String 合法直返、Number 归一、
 * 非法/空白/null 静默返 null 的校验式契约）、{@code setCurrentOrgId(String)} 写读回、
 * {@code isActive} 的 config-gated 行为（默认关闭恒 false）。
 */
public class TestErpOrgContext {

    private static final String ATTR = ErpOrgIsolationConstants.CONTEXT_ATTR_CURRENT_ORG_ID;
    private static final String CONFIG = ErpOrgIsolationConstants.CONFIG_ORG_ISOLATION_ENABLED;

    private IServiceContext context;
    private Object prevConfig;

    @BeforeEach
    void setUp() {
        context = new ServiceContextImpl();
        prevConfig = AppConfig.var(CONFIG, Boolean.FALSE);
        AppConfig.getConfigProvider().assignConfigValue(CONFIG, Boolean.FALSE);
    }

    @AfterEach
    void tearDown() {
        AppConfig.getConfigProvider().assignConfigValue(CONFIG, prevConfig);
        ContextProvider.setContextAttr(ATTR, null);
    }

    private void setAttr(Object value) {
        context.setAttribute(ATTR, value);
    }

    // ---- (1) 转换矩阵：过渡期宽容 + 校验式契约 ----

    @Test
    public void stringValueReturnedAsIs() {
        setAttr("2");
        assertEquals("2", ErpOrgContext.currentOrgId(context));
    }

    @Test
    public void legalLeadingZeroStringKept() {
        setAttr("007");
        assertEquals("007", ErpOrgContext.currentOrgId(context), "合法数字串直接返回，不做数值归一");
    }

    @Test
    public void longValueNormalizedToString() {
        setAttr(2L);
        assertEquals("2", ErpOrgContext.currentOrgId(context), "过渡宽容：未迁移写入方放 Long 仍可读通");
    }

    @Test
    public void otherNumberNormalizedToString() {
        setAttr(7);
        assertEquals("7", ErpOrgContext.currentOrgId(context));
    }

    @Test
    public void illegalStringReturnsNull() {
        setAttr("abc");
        assertNull(ErpOrgContext.currentOrgId(context), "非法输入 no-op（校验式契约）");
    }

    @Test
    public void nullAttrReturnsNull() {
        context.removeAttribute(ATTR);
        assertNull(ErpOrgContext.currentOrgId(context), "attr 未设置 = null");
    }

    @Test
    public void blankStringReturnsNull() {
        setAttr("   ");
        assertNull(ErpOrgContext.currentOrgId(context));
    }

    // ---- (2) setCurrentOrgId(String) 写入后可读回 ----

    @Test
    public void setCurrentOrgIdReadBack() {
        ErpOrgContext.setCurrentOrgId(context, "3");
        assertEquals("3", ErpOrgContext.currentOrgId(context));
    }

    // ---- (3) readFromProvider 路径（context == null） ----

    @Test
    public void nullContextReadsFromProvider() {
        ContextProvider.setContextAttr(ATTR, 2L);
        assertEquals("2", ErpOrgContext.currentOrgId(null), "context 为空经 ContextProvider 读取并归一");
    }

    @Test
    public void nullContextNoAttrReturnsNull() {
        ContextProvider.setContextAttr(ATTR, null);
        assertNull(ErpOrgContext.currentOrgId(null));
    }

    // ---- (4) isActive：config-gated 默认关闭行为锁定 ----

    @Test
    public void isActiveFalseWhenConfigOff() {
        ErpOrgContext.setCurrentOrgId(context, "2");
        assertFalse(ErpOrgContext.isActive(context), "隔离 config 关闭时恒 false（默认关闭基线）");
    }

    @Test
    public void isActiveTrueOnlyWhenConfigOnAndOrgIdSet() {
        AppConfig.getConfigProvider().assignConfigValue(CONFIG, Boolean.TRUE);
        ErpOrgContext.setCurrentOrgId(context, "2");
        assertTrue(ErpOrgContext.isActive(context));

        context.removeAttribute(ATTR);
        assertFalse(ErpOrgContext.isActive(context), "config 开但未设置 orgId = false");
    }
}
