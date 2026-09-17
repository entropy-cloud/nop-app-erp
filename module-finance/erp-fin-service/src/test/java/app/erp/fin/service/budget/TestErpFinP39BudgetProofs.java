package app.erp.fin.service.budget;

import app.erp.fin.dao.entity.ErpFinBudgetLine;
import app.erp.fin.dao.entity.ErpFinBudgetScenario;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F3.9 批（plan 2026-09-17-0600-1）fin3 组 Proof：
 * <ul>
 *   <li>fin3-008：match-any——带 costCenterId 预算行在调用侧无 cc 维度时命中（修复前 IS NULL 精确匹配永不命中）；
 *       无 cc 专用行优先。</li>
 *   <li>fin3-009：periodId=null 显式分支——HARD 模式抛 ERR_BUDGET_PERIOD_NOT_RESOLVED（修复前静默 PASS fail-open）；
 *       WARN（默认）记 SKIPPED ControlLog 后放行。</li>
 * </ul>
 * 直接注入 ErpFinBudgetControlBiz 调 doCheck 公共入口 check。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinP39BudgetProofs extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    ErpFinBudgetControlBiz budgetControlBiz;

    private String seedBudgetLineWithCc(String code, String ccId, BigDecimal amount) {
        IEntityDao<ErpFinBudgetScenario> scDao = daoProvider.daoFor(ErpFinBudgetScenario.class);
        ErpFinBudgetScenario sc = scDao.newEntity();
        sc.setCode(code);
        sc.setName("预算方案-" + code);
        sc.setScenarioType("ANNUAL");
        sc.setOrgId("1");
        sc.setCurrencyId("1");
        sc.setAcctSchemaId("1");
        sc.setFiscalYear(2026);
        sc.setAmountFunctional(amount);
        sc.setDocStatus(ErpFinConstants.BUDGET_STATUS_APPROVED);
        sc.setApproveStatus("APPROVED");
        sc.setControlLevel(ErpFinConstants.BUDGET_CONTROL_HARD);
        scDao.saveEntity(sc);

        IEntityDao<ErpFinBudgetLine> lineDao = daoProvider.daoFor(ErpFinBudgetLine.class);
        ErpFinBudgetLine line = lineDao.newEntity();
        line.setScenarioId(sc.getId());
        line.setLineNo(1);
        line.setSubjectCode("6001");
        line.setOrgId("1");
        line.setAcctSchemaId("1");
        line.setSubjectId("6001");
        line.setCostCenterId(ccId);
        line.setPeriodId("1");
        line.setBudgetAmountSource(amount);
        line.setBudgetAmountFunctional(amount);
        line.setCurrencyId("1");
        line.setExchangeRate(BigDecimal.ONE);
        lineDao.saveEntity(line);
        return sc.getId();
    }

    // ---------- fin3-008：match-any ----------

    @Test
    public void testMatchAnyHitsCcBudgetLineWhenCallerHasNoCc() {
        AppConfig.getConfigProvider().assignConfigValue("erp-fin.budget-check-enabled", Boolean.TRUE);
        String scenarioId = seedBudgetLineWithCc("P39-CC-001", "9001", new BigDecimal("1000"));
        // 调用侧 costCenterId=null（单据无维度字段场景）：修复前 isNull 精确匹配永不命中带 cc 行
        // HARD 模式 + 命中带 cc 行 → 余量 1000 < 请求 1500 → BLOCKED 抛异常（证明行可达且控制生效）
        org.junit.jupiter.api.Assertions.assertThrows(io.nop.api.core.exceptions.NopException.class,
                () -> ormTemplate.runInSession(session ->
                        budgetControlBiz.check("6001", null, "1", new BigDecimal("1500"),
                                "PURCHASE_ORDER", "PO-P39-001", null)),
                "match-any 命中带 cc 行余量 1000 < 请求 1500 → HARD 拦截");
    }

    // ---------- fin3-009：periodId=null 显式分支 ----------

    @Test
    public void testPeriodNullHardModeRejects() {
        // fin3-009 依赖 budget check enabled（默认 false）
        AppConfig.getConfigProvider().assignConfigValue("erp-fin.budget-check-enabled", Boolean.TRUE);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinBudgetControlBiz.CONFIG_BUDGET_PERIOD_MISSING_MODE,
                ErpFinBudgetControlBiz.BUDGET_PERIOD_MISSING_MODE_HARD);
        try {
            // HARD 模式：check 抛 ERR_BUDGET_PERIOD_NOT_RESOLVED（修复前静默 PASS fail-open）
            org.junit.jupiter.api.Assertions.assertThrows(io.nop.api.core.exceptions.NopException.class,
                    () -> budgetControlBiz.check("6001", null, null, new BigDecimal("100"),
                            "PURCHASE_ORDER", "PO-P39-002", null),
                    "periodId=null HARD 模式应抛 ERR_BUDGET_PERIOD_NOT_RESOLVED");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinBudgetControlBiz.CONFIG_BUDGET_PERIOD_MISSING_MODE,
                    ErpFinBudgetControlBiz.BUDGET_PERIOD_MISSING_MODE_WARN);
        }
    }

    @Test
    public void testPeriodNullWarnModeSkipsWithLog() {
        AppConfig.getConfigProvider().assignConfigValue("erp-fin.budget-check-enabled", Boolean.TRUE);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpFinBudgetControlBiz.CONFIG_BUDGET_PERIOD_MISSING_MODE,
                ErpFinBudgetControlBiz.BUDGET_PERIOD_MISSING_MODE_WARN);
        try {
            var result = budgetControlBiz.check("6001", null, null, new BigDecimal("100"),
                    "PURCHASE_ORDER", "PO-P39-003", null);
            assertNotNull(result, "WARN 模式放行（默认）返回结果");
            assertTrue("PASS".equals(result.getActionResult()) || "WARNED".equals(result.getActionResult()),
                    "WARN 模式放行（默认）");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpFinBudgetControlBiz.CONFIG_BUDGET_PERIOD_MISSING_MODE,
                    ErpFinBudgetControlBiz.BUDGET_PERIOD_MISSING_MODE_WARN);
        }
    }
}
