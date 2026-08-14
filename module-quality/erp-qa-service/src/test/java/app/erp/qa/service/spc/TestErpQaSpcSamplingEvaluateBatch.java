package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaAction;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.QaFrozenClockExtension;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.batch.dsl.runner.IBatchTaskRunner;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SPC 采样批任务 evaluate 接线端到端测试（plan 2026-08-14-2304-2 Phase 3，P1-RC-042）。
 *
 * <p>批任务级执行 {@code /nop/batch-task/qa/spc-sampling.batch.xml}（经
 * {@link IBatchTaskRunner#execute}，nop-batch-dsl 执行入口，镜像 {@code TestErpCrmLeadScoringRecalcJob} 范式）：
 * <ul>
 *   <li>① 失控样本（均值超 UCL → 规则 1）经 batch 全链（collectSamples → recalculate → evaluate）
 *       自动标记 isOutOfControl=true + violatedRules 非空；</li>
 *   <li>② NCR(sourceType=SPC) + CAPA Action 经 afterCommit（batch chunk 事务提交触发）创建；
 *       {@code erp-qa.spc-auto-ncr-enabled} config-gated（默认 true）；</li>
 *   <li>③ 受控样本 → 零 NCR 零 CAPA；</li>
 *   <li>④ config 关闭 → 仅标记不建 NCR；</li>
 *   <li>⑤ 幂等：同 chart 二次执行不重复 NCR。</li>
 * </ul>
 *
 * <p>seed 确定性配方（plan M1 裁决固化）：控制图 seed < 20 样本（recalculate no-op 不重写
 * cl/ucl/lcl）+ 显式 cl/ucl/lcl + parameterId 非空 + <b>不 seed 匹配该 parameterId 的 APPROVED 检验行</b>
 * （批内 collectSamples 零追加样本，总量保持 < 20，控制限不被数据重算覆盖——镜像
 * {@code TestErpQaSpcSampling.recalculateKeepsPendingWhenLessThan20} 实证）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaSpcSamplingEvaluateBatch extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final Long MATERIAL_ID = 7851L;
    static final Long PARAMETER_ID = 9102L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IBatchTaskRunner batchTaskRunner;

    @AfterEach
    void clearConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_SPC_AUTO_NCR_ENABLED, "");
    }

    /** ① + ② batch 全链：失控样本（mean=20 &gt; UCL=13 → 规则 1）自动标记 + afterCommit 建 NCR/CAPA。 */
    @Test
    public void batchEvaluateMarksOutOfControlAndCreatesNcrCapa() {
        Long chartId = seedChart("CHART-BATCH-OOC", new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("7"));
        Long outOfControlSampleId = seedSample(chartId, 1, new BigDecimal("20"));
        seedSample(chartId, 2, new BigDecimal("10"));
        seedSample(chartId, 3, new BigDecimal("10"));

        batchTaskRunner.execute("/nop/batch-task/qa/spc-sampling.batch.xml");

        ErpQaSpcSample sample = daoProvider.daoFor(ErpQaSpcSample.class).getEntityById(outOfControlSampleId);
        assertEquals(Boolean.TRUE, sample.getIsOutOfControl(), "batch evaluate 后失控样本自动标记");
        assertNotNull(sample.getViolatedRules(), "violatedRules 非空");
        assertTrue(sample.getViolatedRules().contains("1"), "应命中规则 1，实际: " + sample.getViolatedRules());

        ErpQaSpcChart chart = daoProvider.daoFor(ErpQaSpcChart.class).getEntityById(chartId);
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        q.addFilter(eq("sourceCode", chart.getCode() + "#" + sample.getSubgroupNo()));
        List<ErpQaNonConformance> ncrs = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        assertEquals(1, ncrs.size(), "afterCommit 触发建 1 NCR(sourceType=SPC)");
        ErpQaNonConformance ncr = ncrs.get(0);
        assertEquals(ErpQaConstants.NCR_STATUS_OPEN, ncr.getStatus());
        assertEquals(MATERIAL_ID, ncr.getMaterialId());
        assertEquals(ErpQaConstants.NCR_SOURCE_TYPE_SPC, ncr.getSourceType());

        QueryBean actionQ = new QueryBean();
        actionQ.addFilter(eq("ncrId", ncr.getId()));
        List<ErpQaAction> actions = daoProvider.daoFor(ErpQaAction.class).findAllByQuery(actionQ);
        assertEquals(1, actions.size(), "建 1 CAPA Action");
        assertEquals("CAPA", actions.get(0).getActionType());
        assertEquals(ErpQaConstants.ACTION_STATUS_PENDING, actions.get(0).getStatus());
    }

    /** ③ 受控样本：batch 全链零违规 → 零 NCR 零 CAPA。 */
    @Test
    public void batchEvaluateControlledSamplesNoNcr() {
        Long chartId = seedChart("CHART-BATCH-CTL", new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("7"));
        seedSample(chartId, 1, new BigDecimal("10"));
        seedSample(chartId, 2, new BigDecimal("9"));
        seedSample(chartId, 3, new BigDecimal("11"));

        batchTaskRunner.execute("/nop/batch-task/qa/spc-sampling.batch.xml");

        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        List<ErpQaSpcSample> samples = daoProvider.daoFor(ErpQaSpcSample.class).findAllByQuery(q);
        for (ErpQaSpcSample s : samples) {
            assertNull(s.getViolatedRules(), "受控样本无违规");
            assertEquals(Boolean.FALSE, s.getIsOutOfControl());
        }

        QueryBean ncrQ = new QueryBean();
        ncrQ.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        assertTrue(daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(ncrQ).isEmpty(),
                "受控样本零 NCR");
        assertTrue(daoProvider.daoFor(ErpQaAction.class).findAllByQuery(new QueryBean()).isEmpty(),
                "受控样本零 CAPA Action");
    }

    /** ④ config 关闭（erp-qa.spc-auto-ncr-enabled=false）→ 仅标记 isOutOfControl，不建 NCR。 */
    @Test
    public void batchEvaluateAutoNcrDisabledMarksOnly() {
        AppConfig.getConfigProvider().assignConfigValue(ErpQaConstants.CONFIG_SPC_AUTO_NCR_ENABLED, "false");

        Long chartId = seedChart("CHART-BATCH-OFF", new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("7"));
        Long outOfControlSampleId = seedSample(chartId, 1, new BigDecimal("20"));

        batchTaskRunner.execute("/nop/batch-task/qa/spc-sampling.batch.xml");

        ErpQaSpcSample sample = daoProvider.daoFor(ErpQaSpcSample.class).getEntityById(outOfControlSampleId);
        assertEquals(Boolean.TRUE, sample.getIsOutOfControl(), "config 关闭仍标记 isOutOfControl");

        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        assertTrue(daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q).isEmpty(),
                "config 关闭不建 NCR");
    }

    /** ⑤ 幂等：同 chart 二次 batch 执行不重复 NCR/CAPA。 */
    @Test
    public void batchEvaluateIdempotentNoDuplicateNcr() {
        Long chartId = seedChart("CHART-BATCH-IDEM", new BigDecimal("10"), new BigDecimal("13"), new BigDecimal("7"));
        seedSample(chartId, 1, new BigDecimal("20"));

        batchTaskRunner.execute("/nop/batch-task/qa/spc-sampling.batch.xml");
        batchTaskRunner.execute("/nop/batch-task/qa/spc-sampling.batch.xml");

        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_SPC));
        List<ErpQaNonConformance> ncrs = daoProvider.daoFor(ErpQaNonConformance.class).findAllByQuery(q);
        assertEquals(1, ncrs.size(), "幂等：二次执行不重复 NCR");
        assertEquals(1, daoProvider.daoFor(ErpQaAction.class).findAllByQuery(new QueryBean()).size(),
                "幂等：二次执行不重复 CAPA");
    }

    // ---------- helpers ----------

    private Long seedChart(String code, BigDecimal cl, BigDecimal ucl, BigDecimal lcl) {
        Long id = 91000L + (long) Math.abs(code.hashCode() % 10000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
            ErpQaSpcChart chart = dao.newEntity();
            chart.orm_propValueByName("id", id);
            chart.setCode(code);
            chart.setName("SPC-" + code);
            chart.setChartType(ErpQaConstants.SPC_CHART_TYPE_X_BAR_R);
            chart.setParameterId(PARAMETER_ID);
            chart.setMaterialId(MATERIAL_ID);
            chart.setSubgroupSize(ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE);
            chart.setClCenterType(ErpQaConstants.SPC_CL_CENTER_AUTO_FROM_DATA);
            chart.setRuleSet(ErpQaConstants.DEFAULT_RULE_SET);
            chart.setCalcStatus(ErpQaConstants.SPC_CALC_STATUS_CALCULATED);
            chart.setCl(cl);
            chart.setUcl(ucl);
            chart.setLcl(lcl);
            chart.setIsActive(Boolean.TRUE);
            chart.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            chart.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(chart);
        });
        return id;
    }

    private Long seedSample(Long chartId, int subgroupNo, BigDecimal mean) {
        Long id = 92000L + chartId * 10 + subgroupNo;
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
            ErpQaSpcSample sample = dao.newEntity();
            sample.orm_propValueByName("id", id);
            sample.setChartId(chartId);
            sample.setSubgroupNo(subgroupNo);
            sample.setSampleTime(CoreMetrics.currentTimestamp());
            sample.setMeasuredValues("[" + mean.toPlainString() + "]");
            sample.setMean(mean);
            sample.setRange(BigDecimal.ZERO);
            sample.setStdDev(BigDecimal.ZERO);
            sample.setIsOutOfControl(false);
            dao.saveEntity(sample);
        });
        return id;
    }
}
