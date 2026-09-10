package app.erp.qa.service.spc;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.QaFrozenClockExtension;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.batch.dsl.runner.IBatchTaskRunner;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-CK-qa-030-r3 负路径回归：spc-sampling 批任务 collectSamples 无 per-chart 容错。
 *
 * <p>缺陷面：{@code spc-sampling.batch.xml} processor 段直接调 collectSamples——坏 chart
 * （parameterId 空 / subgroupSize&lt;2）抛错毒化整个 chunk（失败隔离仅 chunk 级），同 chunk
 * 其余 chart 采样结果丢失。evaluate 段 per-sample catch（{@code SpcRuleEngine} 级联 WARN 隔离）
 * 为对照范式。
 *
 * <p>断言契约（索引修复方向）：坏 chart 经 per-chart try/catch WARN 隔离后，同 chunk 其余 chart
 * 采样结果完整产出；全合法输入控制组行为不变。
 *
 * <p>parameterId 空变体说明：ORM 中 {@code ErpQaSpcChart.parameterId} mandatory=true（NOT NULL），
 * 空值 chart 不可持久化，该分支为防御路径；本测试以可持久化的 subgroupSize&lt;2 坏 chart 构造毒化面。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpQaSpcSamplingBatchFaultIsolation extends JunitAutoTestCase {

    @RegisterExtension
    static QaFrozenClockExtension frozenClock = new QaFrozenClockExtension();

    static final String MATERIAL_ID = "7601";
    static final String INSPECTOR_ID = "7701";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IBatchTaskRunner batchTaskRunner;

    /** 负路径：坏 chart（subgroupSize=1）不得毒化同 chunk 其余 chart 的采样结果。 */
    @Test
    public void badChartDoesNotPoisonChunkSampling() {
        String goodChartId = seedChart(93001L, "CHART-ISO-GOOD", "99101", ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE);
        for (int i = 0; i < 5; i++) {
            seedApprovedInspectionLine(94101L + i, "INS-ISO" + i, "99101", BigDecimal.valueOf(10 + i * 10));
        }
        String badChartId = seedChart(93002L, "CHART-ISO-BAD-SUB1", "99102", 1);

        assertDoesNotThrow(() -> batchTaskRunner.execute("/nop/batch-task/qa/spc-sampling.batch.xml"),
                "坏 chart 应被 per-chart 隔离，不毒化 chunk");

        assertEquals(1, findSamples(goodChartId).size(),
                "坏 chart 被隔离后同 chunk 其余 chart 采样结果完整产出");
        assertTrue(findSamples(badChartId).isEmpty(), "坏 chart 自身不产出样本");
    }

    /** 控制组：全合法输入各 chart 采样结果正常产出。 */
    @Test
    public void allLegalChartsStillSampled() {
        String chartA = seedChart(93011L, "CHART-ISO-CTL-A", "99111", ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE);
        for (int i = 0; i < 5; i++) {
            seedApprovedInspectionLine(94201L + i, "INS-ISO-CTL-A" + i, "99111", BigDecimal.valueOf(10 + i));
        }
        String chartB = seedChart(93012L, "CHART-ISO-CTL-B", "99112", ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE);
        for (int i = 0; i < 5; i++) {
            seedApprovedInspectionLine(94301L + i, "INS-ISO-CTL-B" + i, "99112", BigDecimal.valueOf(20 + i));
        }

        assertDoesNotThrow(() -> batchTaskRunner.execute("/nop/batch-task/qa/spc-sampling.batch.xml"));

        assertEquals(1, findSamples(chartA).size(), "控制组 chart A 正常采样");
        assertEquals(1, findSamples(chartB).size(), "控制组 chart B 正常采样");
    }

    // ---------- helpers ----------

    private String seedChart(long id, String code, String parameterId, Integer subgroupSize) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaSpcChart> dao = daoProvider.daoFor(ErpQaSpcChart.class);
            ErpQaSpcChart chart = dao.newEntity();
            chart.orm_propValueByName("id", String.valueOf(id));
            chart.setCode(code);
            chart.setName("SPC-" + code);
            chart.setChartType(ErpQaConstants.SPC_CHART_TYPE_X_BAR_R);
            chart.setParameterId(parameterId);
            chart.setMaterialId(MATERIAL_ID);
            chart.setSubgroupSize(subgroupSize);
            chart.setClCenterType(ErpQaConstants.SPC_CL_CENTER_AUTO_FROM_DATA);
            chart.setRuleSet(ErpQaConstants.DEFAULT_RULE_SET);
            chart.setCalcStatus(ErpQaConstants.SPC_CALC_STATUS_PENDING);
            chart.setIsActive(Boolean.TRUE);
            chart.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            chart.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(chart);
        });
        return String.valueOf(id);
    }

    private void seedApprovedInspectionLine(long id, String insCode, String parameterId, BigDecimal measuredValue) {
        String insId = String.valueOf(id * 10);
        String lineId = String.valueOf(id * 10 + 1);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpQaInspection> insDao = daoProvider.daoFor(ErpQaInspection.class);
            ErpQaInspection ins = insDao.newEntity();
            ins.orm_propValueByName("id", insId);
            ins.setCode(insCode);
            ins.setInspectionType(ErpQaConstants.INSPECTION_TYPE_INCOMING);
            ins.setMaterialId(MATERIAL_ID);
            ins.setResult(ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            ins.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
            ins.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
            ins.setPosted(Boolean.FALSE);
            ins.setInspectionDate(CoreMetrics.currentDate());
            ins.setBusinessDate(CoreMetrics.currentDate());
            ins.setInspectorId(INSPECTOR_ID);
            insDao.saveEntity(ins);

            IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
            ErpQaInspectionLine line = lineDao.newEntity();
            line.orm_propValueByName("id", lineId);
            line.setInspectionId(insId);
            line.setLineNo(1);
            line.setParameterId(parameterId);
            line.setParameterName("尺寸");
            line.setMeasuredValue(measuredValue.toPlainString());
            line.setResult(ErpQaConstants.INSPECTION_RESULT_ACCEPTED);
            lineDao.saveEntity(line);
        });
    }

    private List<ErpQaSpcSample> findSamples(String chartId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        return daoProvider.daoFor(ErpQaSpcSample.class).findAllByQuery(q);
    }
}
