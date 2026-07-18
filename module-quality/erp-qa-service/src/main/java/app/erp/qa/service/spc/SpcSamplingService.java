package app.erp.qa.service.spc;

import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.IErpQaInspectionLineBiz;
import app.erp.qa.biz.IErpQaSpcSampleBiz;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.lt;
import static io.nop.api.core.beans.FilterBeans.in;

/**
 * SPC 样本采集引擎（{@code docs/design/quality/spc.md §关键流程 1}，plan 2026-07-07-0305-2 Phase 2）。
 *
 * <p>扫描 APPROVED 的 {@link ErpQaInspectionLine}（命中 {@code chart.parameterId}），按
 * {@code chart.subgroupSize} 聚合成 {@link ErpQaSpcSample}（mean/range/stdDev 计算字段 +
 * sourceBillType 三元组反查，不重复存原始值）。幂等：同 (chartId, subgroupNo) 不重建，incremental 补缺。
 *
 * <p>{@code ErpQaInspectionLine.measuredValue} 为 {@code VARCHAR(100)}（非 DECIMAL），
 * 采样引擎经字符串→数值解析，非数值/空值经 {@link ErpQaErrors#ERR_QA_SPC_MEASURED_VALUE_INVALID}
 * 跳过并告警（日志 WARN，不阻断采样）。
 */
public class SpcSamplingService {

    private static final Logger LOG = LoggerFactory.getLogger(SpcSamplingService.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpQaInspectionLineBiz inspectionLineBiz;
    @Inject
    IErpQaInspectionBiz inspectionBiz;
    @Inject
    IErpQaSpcSampleBiz spcSampleBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    public void setInspectionLineBiz(IErpQaInspectionLineBiz inspectionLineBiz) {
        this.inspectionLineBiz = inspectionLineBiz;
    }

    public void setInspectionBiz(IErpQaInspectionBiz inspectionBiz) {
        this.inspectionBiz = inspectionBiz;
    }

    public void setSpcSampleBiz(IErpQaSpcSampleBiz spcSampleBiz) {
        this.spcSampleBiz = spcSampleBiz;
    }

    /**
     * 对指定 chart 执行增量样本采集。返回新增的样本数。
     *
     * <p>chartType 两分支（plan 2026-07-19-0120-2 增计数型）：
     * <ul>
     *   <li><b>计量型</b>（X_BAR_R/X_BAR_S/X_MR，含 chartType==null 回落）：既有 measuredValues 聚合
     *       —— APPROVED inspection line 的 measuredValue（字符串→BigDecimal）按 subgroupSize 聚合；</li>
     *   <li><b>计数型</b>（P/NP/C/U）：按 chartType 派生数据源——
     *       P/NP 从 ErpQaInspectionLine 按 result=REJECTED 计数 defectCount + line 总数作 inspectedCount；
     *       C/U 从 ErpQaNonConformance 按 sourceType=INSPECTION + inspectionId 反查 quantity 累计 defectCount +
     *       inspection 数作 inspectedCount。每子组（按 inspection 切分，subgroupSize=子组内 inspection 数）1 sample。</li>
     * </ul>
     *
     * @param chartId 控制图 ID
     * @param context 服务上下文
     * @return 本次新采集的样本（子组）数；无新数据返回 0
     */
    public int collectSamples(Long chartId, IServiceContext context) {
        if (chartId == null) {
            throw new NopException(ErpQaErrors.ERR_QA_SPC_CHART_NOT_FOUND)
                    .param(ErpQaErrors.ARG_CHART_ID, chartId);
        }
        IEntityDao<ErpQaSpcChart> chartDao = daoProvider.daoFor(ErpQaSpcChart.class);
        ErpQaSpcChart chart = chartDao.getEntityById(chartId);
        if (chart == null) {
            throw new NopException(ErpQaErrors.ERR_QA_SPC_CHART_NOT_FOUND)
                    .param(ErpQaErrors.ARG_CHART_ID, chartId);
        }
        Long parameterId = chart.getParameterId();
        if (parameterId == null) {
            throw new NopException(ErpQaErrors.ERR_QA_SPC_PARAMETER_NOT_FOUND)
                    .param(ErpQaErrors.ARG_CHART_CODE, chart.getCode())
                    .param(ErpQaErrors.ARG_PARAMETER_ID, null);
        }
        int subgroupSize = chart.getSubgroupSize() == null ? ErpQaConstants.DEFAULT_SPC_SUBGROUP_SIZE : chart.getSubgroupSize();
        if (subgroupSize < 2) {
            throw new NopException(ErpQaErrors.ERR_QA_SPC_SUBGROUP_SIZE_INVALID)
                    .param(ErpQaErrors.ARG_CHART_CODE, chart.getCode());
        }

        // chartType 分支：计数型走 attributes 聚合路径（plan 2026-07-19-0120-2）
        if (SpcControlLimitCalculator.isAttributesChart(chart.getChartType())) {
            return collectAttributesSamples(chart, parameterId, subgroupSize);
        }

        // 1. 候选 InspectionLine：parameterId 命中 + APPROVED 的 inspection
        List<ErpQaInspectionLine> candidates = findApprovedInspectionLines(parameterId);
        if (candidates.isEmpty()) {
            LOG.debug("spc-sampling-no-data: chartId={} parameterId={}", chartId, parameterId);
            return 0;
        }

        // 2. 已采样 measuredValue 集合（经 sourceCode+sourceLineCode 反查），幂等跳过
        //    简化：以 (sourceCode, sourceLineCode, measuredValue) 三元组为幂等键
        List<ErpQaSpcSample> existingSamples = findSamples(chartId);
        java.util.Set<String> sampledKeys = buildSampledKeys(existingSamples);

        // 3. 解析并过滤候选点（数值化、未采样）
        List<SamplePoint> pendingPoints = new ArrayList<>();
        for (ErpQaInspectionLine line : candidates) {
            BigDecimal value = parseMeasuredValue(line, chartId, parameterId);
            if (value == null) {
                continue;
            }
            ErpQaInspection inspection = resolveInspection(line.getInspectionId());
            if (inspection == null) {
                continue;
            }
            String key = inspection.getCode() + "#" + safeLineNo(line) + "#" + value.toPlainString();
            if (sampledKeys.contains(key)) {
                continue;
            }
            pendingPoints.add(new SamplePoint(value, inspection, line));
        }
        if (pendingPoints.isEmpty()) {
            return 0;
        }

        // 4. 按 sampleTime（inspection.createTime 近似）排序后按 subgroupSize 聚合成子组
        pendingPoints.sort((a, b) -> {
            LocalDateTime ta = a.inspection.getCreateTime() == null ? LocalDateTime.MIN : a.inspection.getCreateTime().toLocalDateTime();
            LocalDateTime tb = b.inspection.getCreateTime() == null ? LocalDateTime.MIN : b.inspection.getCreateTime().toLocalDateTime();
            return ta.compareTo(tb);
        });

        // 起始 subgroupNo = max(existing) + 1
        int nextSubgroupNo = nextSubgroupNo(existingSamples);
        IEntityDao<ErpQaSpcSample> sampleDao = daoProvider.daoFor(ErpQaSpcSample.class);

        int total = pendingPoints.size();
        int created = 0;
        for (int offset = 0; offset + subgroupSize <= total; offset += subgroupSize) {
            List<SamplePoint> subgroup = pendingPoints.subList(offset, offset + subgroupSize);
            ErpQaSpcSample sample = sampleDao.newEntity();
            sample.setChartId(chartId);
            sample.setOrgId(chart.getOrgId());
            sample.setSubgroupNo(nextSubgroupNo++);
            sample.setSampleTime(CoreMetrics.currentTimestamp());

            List<BigDecimal> values = new ArrayList<>(subgroup.size());
            SamplePoint first = subgroup.get(0);
            for (SamplePoint p : subgroup) {
                values.add(p.value);
            }
            sample.setMeasuredValues(JsonTool.stringify(values));

            BigDecimal mean = Statistics.mean(values);
            BigDecimal range = Statistics.range(values);
            BigDecimal stdDev = Statistics.stdDev(values);
            sample.setMean(scaleTo(mean));
            sample.setRange(scaleTo(range));
            sample.setStdDev(scaleTo(stdDev));

            sample.setSourceBillType(ErpQaConstants.SPC_SOURCE_BILL_TYPE_INSPECTION);
            sample.setSourceCode(first.inspection.getCode());
            sample.setSourceLineCode(safeLineNo(first.line));
            sample.setInspectorId(first.inspection.getInspectorId());
            sample.setIsOutOfControl(false);
            sampleDao.saveEntity(sample);
            created++;
        }
        return created;
    }

    /**
     * 计数型样本采集（plan 2026-07-19-0120-2 Phase 2）。
     *
     * <p>P/NP 从 ErpQaInspectionLine 按 result=REJECTED 计数 defectCount + line 总数作 inspectedCount；
     * C/U 从 ErpQaNonConformance 按 sourceType=INSPECTION + inspectionId 反查 quantity 累计 defectCount +
     * inspection 数作 inspectedCount。
     *
     * <p>子组按 APPROVED inspection 切分（chart.subgroupSize 语义改为"子组内 inspection 数"），
     * 每子组 1 ErpQaSpcSample，幂等键 = "ATTR#" + inspectionCode（避免与计量型 measuredValue 键冲突）。
     */
    private int collectAttributesSamples(ErpQaSpcChart chart, Long parameterId, int subgroupSize) {
        Long chartId = chart.getId();
        // 找全部 APPROVED inspection（命中 parameterId 的 line 关联）
        List<ErpQaInspectionLine> approvedLines = findApprovedInspectionLines(parameterId);
        if (approvedLines.isEmpty()) {
            LOG.debug("spc-sampling-attrs-no-data: chartId={} parameterId={}", chartId, parameterId);
            return 0;
        }
        // 按 inspectionId 分组行（一个 inspection 多个 line）
        Map<Long, List<ErpQaInspectionLine>> linesByInspection = new LinkedHashMap<>();
        for (ErpQaInspectionLine line : approvedLines) {
            if (line.getInspectionId() == null) continue;
            linesByInspection.computeIfAbsent(line.getInspectionId(), k -> new ArrayList<>()).add(line);
        }
        // 收集有效 inspection（按 createTime 升序）
        List<ErpQaInspection> inspections = new ArrayList<>();
        for (Long insId : linesByInspection.keySet()) {
            ErpQaInspection ins = resolveInspection(insId);
            if (ins != null) inspections.add(ins);
        }
        if (inspections.isEmpty()) {
            return 0;
        }
        inspections.sort((a, b) -> {
            LocalDateTime ta = a.getCreateTime() == null ? LocalDateTime.MIN : a.getCreateTime().toLocalDateTime();
            LocalDateTime tb = b.getCreateTime() == null ? LocalDateTime.MIN : b.getCreateTime().toLocalDateTime();
            return ta.compareTo(tb);
        });

        // 幂等：检查已有 sample 的 sourceCode（计数型以 inspection code 列表 join 作为幂等键）
        List<ErpQaSpcSample> existingSamples = findSamples(chartId);
        java.util.Set<String> sampledKeys = buildAttributesSampledKeys(existingSamples);

        // 按 subgroupSize 切分 inspection 集合
        int nextSubgroupNo = nextSubgroupNo(existingSamples);
        IEntityDao<ErpQaSpcSample> sampleDao = daoProvider.daoFor(ErpQaSpcSample.class);
        boolean isCU = ErpQaConstants.SPC_CHART_TYPE_C.equals(chart.getChartType())
                || ErpQaConstants.SPC_CHART_TYPE_U.equals(chart.getChartType());
        // C/U 预加载 NCR（按 inspectionId 索引）
        Map<Long, List<ErpQaNonConformance>> ncrsByInspection = isCU
                ? loadNcrsByInspectionId(linesByInspection.keySet()) : java.util.Collections.emptyMap();

        int total = inspections.size();
        int created = 0;
        for (int offset = 0; offset + subgroupSize <= total; offset += subgroupSize) {
            List<ErpQaInspection> subgroup = inspections.subList(offset, offset + subgroupSize);
            // 幂等键：以子组首个 inspection code（同一起始 inspection 不重建）
            String firstCode = subgroup.get(0).getCode();
            String key = "ATTR#" + firstCode;
            if (sampledKeys.contains(key)) {
                // 该子组起始 inspection 已被采样过；进一步检查后续子组（增量场景：旧子组保留 + 新数据成新子组）
                continue;
            }

            int defectCount = 0;
            int inspectedCount = 0;
            for (ErpQaInspection ins : subgroup) {
                List<ErpQaInspectionLine> lines = linesByInspection.get(ins.getId());
                if (lines == null) continue;
                if (isCU) {
                    // C/U：defectCount 从 NCR.quantity 累计（每 inspection），inspectedCount = 1 (1 inspection = 1 单位 opportunity)
                    List<ErpQaNonConformance> ncrs = ncrsByInspection.get(ins.getId());
                    if (ncrs != null) {
                        for (ErpQaNonConformance ncr : ncrs) {
                            if (ncr.getQuantity() != null) {
                                defectCount += ncr.getQuantity().intValue();
                            }
                        }
                    }
                    inspectedCount += 1;
                } else {
                    // P/NP：defectCount = REJECTED line 数；inspectedCount = line 总数
                    for (ErpQaInspectionLine line : lines) {
                        if (ErpQaConstants.INSPECTION_RESULT_REJECTED.equals(line.getResult())) {
                            defectCount++;
                        }
                        inspectedCount++;
                    }
                }
            }

            ErpQaSpcSample sample = sampleDao.newEntity();
            sample.setChartId(chartId);
            sample.setOrgId(chart.getOrgId());
            sample.setSubgroupNo(nextSubgroupNo++);
            sample.setSampleTime(CoreMetrics.currentTimestamp());
            sample.setDefectCount(defectCount);
            sample.setInspectedCount(inspectedCount);
            // 派生 mean/range/stdDev：保留计量型字段语义为 defectRate/0/0（兼容既有看板渲染 mean 字段）
            BigDecimal defectRate = inspectedCount > 0
                    ? BigDecimal.valueOf(defectCount).divide(BigDecimal.valueOf(inspectedCount), 6, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            sample.setMean(scaleTo(defectRate));
            sample.setRange(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            sample.setStdDev(BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP));
            sample.setSourceBillType(ErpQaConstants.SPC_SOURCE_BILL_TYPE_INSPECTION);
            sample.setSourceCode(subgroup.get(0).getCode());
            sample.setSourceLineCode(String.valueOf(subgroup.size()));
            sample.setInspectorId(subgroup.get(0).getInspectorId());
            sample.setIsOutOfControl(false);
            sampleDao.saveEntity(sample);
            created++;
        }
        return created;
    }

    private Map<Long, List<ErpQaNonConformance>> loadNcrsByInspectionId(java.util.Set<Long> inspectionIds) {
        if (inspectionIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        QueryBean q = new QueryBean();
        q.addFilter(in("inspectionId", new ArrayList<>(inspectionIds)));
        q.addFilter(eq("sourceType", ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION));
        List<ErpQaNonConformance> ncrs = dao.findAllByQuery(q);
        Map<Long, List<ErpQaNonConformance>> result = new LinkedHashMap<>();
        for (ErpQaNonConformance ncr : ncrs) {
            if (ncr.getInspectionId() == null) continue;
            result.computeIfAbsent(ncr.getInspectionId(), k -> new ArrayList<>()).add(ncr);
        }
        return result;
    }

    private java.util.Set<String> buildAttributesSampledKeys(List<ErpQaSpcSample> existingSamples) {
        // 计数型幂等键：sample.sourceCode（子组首个 inspection code）—— 同一起始 inspection 不重建
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (ErpQaSpcSample s : existingSamples) {
            if (s.getDefectCount() == null && s.getInspectedCount() == null) {
                continue;
            }
            if (s.getSourceCode() != null) {
                keys.add("ATTR#" + s.getSourceCode());
            }
        }
        return keys;
    }

    private ErpQaInspection resolveInspection(Long inspectionId) {
        if (inspectionId == null) {
            return null;
        }
        return daoProvider.daoFor(ErpQaInspection.class).getEntityById(inspectionId);
    }

    private List<ErpQaInspectionLine> findApprovedInspectionLines(Long parameterId) {
        // 两步：先找 parameterId 命中的所有行；再过滤出对应 inspection 已 APPROVED 的
        IEntityDao<ErpQaInspectionLine> lineDao = daoProvider.daoFor(ErpQaInspectionLine.class);
        QueryBean lineQuery = new QueryBean();
        lineQuery.addFilter(eq("parameterId", parameterId));
        List<ErpQaInspectionLine> all = lineDao.findAllByQuery(lineQuery);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }
        // 取 inspectionId 集合查 APPROVED inspection
        java.util.Set<Long> inspectionIds = new java.util.HashSet<>();
        for (ErpQaInspectionLine l : all) {
            if (l.getInspectionId() != null) {
                inspectionIds.add(l.getInspectionId());
            }
        }
        if (inspectionIds.isEmpty()) {
            return Collections.emptyList();
        }
        IEntityDao<ErpQaInspection> insDao = daoProvider.daoFor(ErpQaInspection.class);
        QueryBean insQuery = new QueryBean();
        insQuery.addFilter(in("id", new ArrayList<>(inspectionIds)));
        insQuery.addFilter(eq("approveStatus", ErpQaConstants.APPROVE_STATUS_APPROVED));
        List<ErpQaInspection> approvedIns = insDao.findAllByQuery(insQuery);
        if (approvedIns.isEmpty()) {
            return Collections.emptyList();
        }
        java.util.Set<Long> approvedIds = new java.util.HashSet<>();
        for (ErpQaInspection i : approvedIns) {
            approvedIds.add(i.getId());
        }
        List<ErpQaInspectionLine> result = new ArrayList<>();
        for (ErpQaInspectionLine l : all) {
            if (approvedIds.contains(l.getInspectionId())) {
                result.add(l);
            }
        }
        return result;
    }

    private List<ErpQaSpcSample> findSamples(Long chartId) {
        IEntityDao<ErpQaSpcSample> dao = daoProvider.daoFor(ErpQaSpcSample.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        return dao.findAllByQuery(q);
    }

    private java.util.Set<String> buildSampledKeys(List<ErpQaSpcSample> existingSamples) {
        java.util.Set<String> keys = new java.util.HashSet<>();
        for (ErpQaSpcSample s : existingSamples) {
            String measuredValuesJson = s.getMeasuredValues();
            if (StringHelper.isEmpty(measuredValuesJson)) {
                continue;
            }
            try {
                List<BigDecimal> values = parseMeasuredValuesJson(measuredValuesJson);
                StringBuilder keyBase = new StringBuilder();
                keyBase.append(s.getSourceCode() == null ? "" : s.getSourceCode()).append('#');
                keyBase.append(s.getSourceLineCode() == null ? "" : s.getSourceLineCode()).append('#');
                for (BigDecimal v : values) {
                    keys.add(keyBase.toString() + v.toPlainString());
                }
            } catch (Exception e) {
                LOG.warn("spc-sampling-parse-existing-failed: sampleId={} measuredValues={}",
                        s.getId(), measuredValuesJson, e);
            }
        }
        return keys;
    }

    @SuppressWarnings("unchecked")
    private List<BigDecimal> parseMeasuredValuesJson(String json) {
        List<?> list = JsonTool.parseBeanFromText(json, List.class);
        if (list == null) {
            return Collections.emptyList();
        }
        List<BigDecimal> result = new ArrayList<>(list.size());
        for (Object o : list) {
            if (o == null) {
                continue;
            }
            try {
                result.add(new BigDecimal(o.toString()));
            } catch (NumberFormatException ignored) {
                // 跳过非数值（兼容历史数据）
            }
        }
        return result;
    }

    private int nextSubgroupNo(List<ErpQaSpcSample> existingSamples) {
        int max = 0;
        for (ErpQaSpcSample s : existingSamples) {
            if (s.getSubgroupNo() != null && s.getSubgroupNo() > max) {
                max = s.getSubgroupNo();
            }
        }
        return max + 1;
    }

    private BigDecimal parseMeasuredValue(ErpQaInspectionLine line, Long chartId, Long parameterId) {
        String raw = line.getMeasuredValue();
        if (StringHelper.isEmpty(raw)) {
            LOG.warn("spc-sampling-skip-empty-value: chartId={} parameterId={} lineId={}",
                    chartId, parameterId, line.getId());
            return null;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            LOG.warn("spc-sampling-skip-non-numeric-value: chartId={} parameterId={} lineId={} measuredValue={}",
                    chartId, parameterId, line.getId(), raw);
            return null;
        }
    }

    private String safeLineNo(ErpQaInspectionLine line) {
        return line.getLineNo() == null ? "" : String.valueOf(line.getLineNo());
    }

    private BigDecimal scaleTo(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.setScale(6, RoundingMode.HALF_UP);
    }

    private static final class SamplePoint {
        final BigDecimal value;
        final ErpQaInspection inspection;
        final ErpQaInspectionLine line;

        SamplePoint(BigDecimal value, ErpQaInspection inspection, ErpQaInspectionLine line) {
            this.value = value;
            this.inspection = inspection;
            this.line = line;
        }
    }
}
