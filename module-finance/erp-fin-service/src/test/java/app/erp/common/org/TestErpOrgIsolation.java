package app.erp.common.org;

import app.erp.fin.biz.IErpFinArApItemBiz;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多公司 orgId 隔离负向测试（plan 2026-07-30-0841-3-r1-29，P1-MA2-093/094 Phase 2 Proof）。
 *
 * <p>验证 config-gated 隔离双路径：
 * <ul>
 *   <li>{@code enabled=true} + 上下文 orgId=2：CrudBizModel 查询隔离 orgId=3 数据（读路径 093）+
 *       实体保存 stamp 覆盖客户端传入 orgId（写路径 094）；</li>
 *   <li>{@code enabled=false}（默认）：读/写回归零变化。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpOrgIsolation extends JunitAutoTestCase {

    private static final String CONFIG_KEY = ErpOrgIsolationConstants.CONFIG_ORG_ISOLATION_ENABLED;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinArApItemBiz arApItemBiz;

    @AfterEach
    void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(CONFIG_KEY, "false");
        ContextProvider.setContextAttr(ErpOrgIsolationConstants.CONTEXT_ATTR_CURRENT_ORG_ID, null);
    }

    /** 读路径隔离：orgId=2 上下文查询 orgId=3 数据断言空；关闭后回归可见。 */
    @Test
    public void testReadIsolationFiltersOtherOrg() {
        // 隔离关闭态 seed orgId=3 数据（interceptor no-op）
        ormTemplate.runInSession(() -> seedArApItem(6001L, 3L, "READ-ISO-3"));

        IServiceContext ctx = new ServiceContextImpl();
        // 默认关闭：可见 orgId=3 数据
        List<ErpFinArApItem> all = queryByCode(ctx, "READ-ISO-3");
        assertEquals(1, all.size(), "隔离关闭：orgId=3 数据可见");

        // 开启隔离 + ctx orgId=2
        AppConfig.getConfigProvider().assignConfigValue(CONFIG_KEY, "true");
        ErpOrgContext.setCurrentOrgId(ctx, 2L);
        List<ErpFinArApItem> isolated = queryByCode(ctx, "READ-ISO-3");
        assertTrue(isolated.isEmpty(), "隔离开启 + ctx orgId=2：orgId=3 数据被过滤");

        // 关闭回归
        AppConfig.getConfigProvider().assignConfigValue(CONFIG_KEY, "false");
        List<ErpFinArApItem> regression = queryByCode(ctx, "READ-ISO-3");
        assertEquals(1, regression.size(), "关闭隔离后回归：orgId=3 数据恢复可见");
    }

    /** 写路径 stamp：开启隔离 + 线程上下文 orgId=2，seed orgId=3 被覆盖为 orgId=2。 */
    @Test
    public void testWriteStampOverridesClientOrgId() {
        AppConfig.getConfigProvider().assignConfigValue(CONFIG_KEY, "true");
        // 写路径 interceptor 经 ContextProvider（线程上下文）解析，非 IServiceContext 参数
        ContextProvider.setContextAttr(ErpOrgIsolationConstants.CONTEXT_ATTR_CURRENT_ORG_ID, 2L);

        ormTemplate.runInSession(() -> seedArApItem(6002L, 3L, "WRITE-STAMP-3"));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", "WRITE-STAMP-3"));
            q.setLimit(1);
            ErpFinArApItem saved = dao.findAllByQuery(q).get(0);
            // 客户端传入 orgId=3 被 stamp 覆盖为上下文 orgId=2
            assertEquals(2L, saved.getOrgId(), "写路径 stamp 覆盖客户端 orgId=3 → 2");
        });

        // 关闭隔离：写入保留客户端 orgId
        AppConfig.getConfigProvider().assignConfigValue(CONFIG_KEY, "false");
        ContextProvider.setContextAttr(ErpOrgIsolationConstants.CONTEXT_ATTR_CURRENT_ORG_ID, null);
        ormTemplate.runInSession(() -> seedArApItem(6003L, 3L, "WRITE-NOSTAMP-3"));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("code", "WRITE-NOSTAMP-3"));
            q.setLimit(1);
            ErpFinArApItem saved = dao.findAllByQuery(q).get(0);
            assertEquals(3L, saved.getOrgId(), "隔离关闭：客户端 orgId=3 保留");
        });
    }

    private List<ErpFinArApItem> queryByCode(IServiceContext ctx, String code) {
        QueryBean query = new QueryBean();
        query.addFilter(eq("code", code));
        return arApItemBiz.findList(query, null, ctx);
    }

    private void seedArApItem(long id, long orgId, String code) {
        IEntityDao<ErpFinArApItem> dao = daoProvider.daoFor(ErpFinArApItem.class);
        ErpFinArApItem it = dao.newEntity();
        it.orm_propValue(1, id);
        it.setCode(code);
        it.setOrgId(orgId);
        it.setAcctSchemaId(1L);
        it.setDirection(ErpFinConstants.DIRECTION_RECEIVABLE);
        it.setPartnerId(900L);
        it.setSourceBillType(ErpFinConstants.SOURCE_BILL_AR_INVOICE);
        it.setSourceBillCode("BILL-" + id);
        it.setBusinessDate(LocalDate.of(2026, 7, 1));
        it.setDueDate(LocalDate.of(2026, 7, 30));
        it.setCurrencyId(1L);
        it.setExchangeRate(BigDecimal.ONE);
        it.setAmountSource(BigDecimal.ZERO);
        it.setAmountFunctional(BigDecimal.ZERO);
        it.setSettledAmountSource(BigDecimal.ZERO);
        it.setSettledAmountFunctional(BigDecimal.ZERO);
        it.setOpenAmountSource(BigDecimal.ZERO);
        it.setOpenAmountFunctional(BigDecimal.ZERO);
        it.setStatus(ErpFinConstants.AR_AP_STATUS_OPEN);
        dao.saveEntity(it);
    }
}
