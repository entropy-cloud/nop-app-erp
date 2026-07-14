package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 折旧集成门控单测（Phase 3）。默认配置 {@code erp-ast.auto-depreciation-on-close=true}：closePeriod 经
 * IBizObjectManager 解析 assets 折旧能力；finance 单域测试无 ast-service 时解析失败→配置门控告警跳过，
 * 不阻断结账（AST 模块仍关账，§ Non-Goal 配置门控）。
 *
 * <p>assets 域 impl 在 app-erp-all 聚合时注入后，同路径将真实调起批量折旧。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinDepreciationIntegration extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpFinAccountingPeriodBiz periodBiz;

    @Test
    public void testDepreciationGateNonBlocking() {
        // 默认 auto-depreciation-on-close=true：折旧集成尝试执行；finance 单域无 ast-service → 告警跳过，不阻断。
        Long periodId = seedReturn(() -> seedOpenPeriod("2024-09", 2024, 9));

        ErpFinAccountingPeriod period = ormTemplate.runInSession(session -> periodBiz.closePeriod(periodId, CTX));

        assertEquals(ErpFinConstants.PERIOD_STATUS_CLOSED, period.getStatus(), "折旧不可用不阻断结账");

        ErpFinAccountingPeriodStatus status = loadStatus(periodId);
        assertEquals(ErpFinConstants.MODULE_CLOSE_CLOSED, status.getAssetStatus(),
                "AST 模块仍关账（折旧门控跳过后推进）");
    }

    // ---------- helpers ----------

    private <T> T seedReturn(java.util.function.Supplier<T> action) {
        return ormTemplate.runInSession(session -> action.get());
    }

    private Long seedOpenPeriod(String code, int year, int month) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod p = new ErpFinAccountingPeriod();
        p.setCode(code);
        p.setName(code);
        p.setOrgId(1L);
        p.setYear(year);
        p.setMonth(month);
        p.setStartDate(LocalDate.of(year, month, 1));
        p.setEndDate(LocalDate.of(year, month, 28));
        p.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(p);
        return p.getId();
    }

    private ErpFinAccountingPeriodStatus loadStatus(Long periodId) {
        IEntityDao<ErpFinAccountingPeriodStatus> dao = daoProvider.daoFor(ErpFinAccountingPeriodStatus.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodId", periodId));
        return dao.findAllByQuery(q).get(0);
    }
}
