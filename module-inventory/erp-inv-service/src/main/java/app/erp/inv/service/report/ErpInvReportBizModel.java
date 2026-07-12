package app.erp.inv.service.report;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.TraceChainResult;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.dao.entity.ErpInvStockMoveLine;
import app.erp.inv.service.ErpInvErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.commons.concurrent.executor.GlobalExecutors;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.lang.eval.IEvalScope;
import io.nop.core.resource.IResource;
import io.nop.core.resource.ResourceHelper;
import io.nop.core.resource.tpl.ITemplateOutput;
import io.nop.core.resource.tpl.ITextTemplateOutput;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.report.core.engine.IReportEngine;
import io.nop.xlang.api.XLang;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.or;

/**
 * 库存域报表渲染入口。注入平台 {@link IReportEngine}，按报表名解析 VFS 模板路径
 * （{@code /nop/main/report/inv/<name>.xpt.xml}）并渲染 html/xlsx/pdf。
 *
 * <p>镜像 {@code ErpMfgReportBizModel}/{@code ErpFinReportBizModel} 域隔离范式（finance→{@code /fin/}，
 * manufacturing→{@code /mfg/}，inventory→{@code /inv/}）：不自建报表引擎，渲染逻辑全部委托给 {@code nop-report}；
 * 报表名经 {@link StringHelper#isValidVPath} 校验防路径注入。数据集由本类的 {@code buildXxxDataset} 方法
 * 从既有库存域 ORM 实体与追溯链查询（{@link IErpInvStockMoveBiz} 同域 I*Biz）聚合。
 *
 * <p>首张种子报表：库存追溯可视化报表（批次/物料→移动链路汇总，对齐 {@code trace-chain.md §追溯链查询}）。
 */
@BizModel("ErpInvReport")
public class ErpInvReportBizModel {

    /** 库存报表模板 VFS 根路径（与 finance {@code /fin/}、manufacturing {@code /mfg/} 隔离）。 */
    static final String REPORT_PATH_PREFIX = "/nop/main/report/inv/";

    /** 模板内统一引用的数据集变量名（单元格 {@code *=^ds!<field>}）。 */
    static final String DS_VAR = "ds";

    private static final Set<String> ALLOWED_RENDER_TYPES = new HashSet<>(Arrays.asList("html", "xlsx", "pdf"));

    @Inject
    IReportEngine reportEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;

    // ===================== 渲染入口 =====================

    @BizQuery
    public String renderHtml(@Name("reportName") String reportName,
                             @Optional @Name("data") Map<String, Object> data,
                             IServiceContext context) {
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);
        ITextTemplateOutput output = reportEngine.getHtmlRenderer(path);
        return output.generateText(scope);
    }

    @BizQuery
    public WebContentBean download(@Name("reportName") String reportName,
                                   @Name("renderType") String renderType,
                                   @Optional @Name("data") Map<String, Object> data,
                                   IServiceContext context) {
        if (!ALLOWED_RENDER_TYPES.contains(renderType)) {
            throw new NopException(ErpInvErrors.ERR_REPORT_RENDER_TYPE_INVALID)
                    .param(ErpInvErrors.ARG_RENDER_TYPE, renderType);
        }
        String path = resolveReportPath(reportName);
        IEvalScope scope = XLang.newEvalScope();
        Map<String, Object> merged = mergeData(data);
        prepareDataset(reportName, merged, context);
        scope.setLocalValues(merged);

        ITemplateOutput output = reportEngine.getRenderer(path, renderType);
        IResource resource = ResourceHelper.getTempResource("inv-rpt");
        try {
            output.generateToResource(resource, scope);
            String fileName = baseName(reportName) + "." + renderType;
            WebContentBean content = new WebContentBean("application/octet-stream", resource.toFile(), fileName);
            GlobalExecutors.globalTimer().schedule(() -> {
                resource.delete();
                return null;
            }, 5, TimeUnit.MINUTES);
            return content;
        } catch (Exception e) {
            resource.delete();
            throw NopException.adapt(e);
        }
    }

    // ===================== 路径解析与防注入 =====================

    /**
     * 将报表名规范化为完整 VFS 路径，并校验防路径注入。
     *
     * <p>接受 {@code inventory-trace-report}、{@code inventory-trace-report.xpt.xml} 两种形式，统一补全到
     * {@code /nop/main/report/inv/inventory-trace-report.xpt.xml}。
     */
    String resolveReportPath(String reportName) {
        if (StringHelper.isEmpty(reportName) || !StringHelper.isValidVPath(reportName)) {
            throw new NopException(ErpInvErrors.ERR_REPORT_NAME_INVALID)
                    .param(ErpInvErrors.ARG_REPORT_NAME, reportName);
        }
        String name = reportName;
        if (!name.endsWith(".xpt.xml") && !name.endsWith(".xpt.xlsx")) {
            name = name + ".xpt.xml";
        }
        return REPORT_PATH_PREFIX + name;
    }

    /** 取报表名的去后缀短名（用于数据集路由与下载文件名）。 */
    private String baseName(String reportName) {
        String n = reportName;
        int slash = n.lastIndexOf('/');
        if (slash >= 0) n = n.substring(slash + 1);
        if (n.endsWith(".xpt.xml")) n = StringHelper.removeTail(n, ".xpt.xml");
        else if (n.endsWith(".xpt.xlsx")) n = StringHelper.removeTail(n, ".xpt.xlsx");
        return n;
    }

    private Map<String, Object> mergeData(Map<String, Object> data) {
        Map<String, Object> merged = new LinkedHashMap<>();
        if (data != null) merged.putAll(data);
        return merged;
    }

    /** 按报表短名自动装配数据集（调用方未预先提供时）。 */
    private void prepareDataset(String reportName, Map<String, Object> data, IServiceContext context) {
        String key = baseName(reportName);
        if (data.containsKey(DS_VAR)) return;
        switch (key) {
            case "inventory-trace-report":
                data.put(DS_VAR, buildInventoryTraceDataset(asString(data, "batchNo"),
                        asLong(data, "materialId"), asLong(data, "warehouseId"),
                        asLong(data, "moveId"), context));
                break;
            default:
                // 未知报表：不自动装配，模板可经 beforeExpand 自行构造数据集
                break;
        }
    }

    private static String asString(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        return v == null ? null : v.toString();
    }

    private static Long asLong(Map<String, Object> data, String k) {
        if (data == null) return null;
        Object v = data.get(k);
        if (v == null) return null;
        String s = v.toString();
        if (s.trim().isEmpty()) return null;
        return Long.valueOf(s);
    }

    // ===================== 数据集构造（也作 @BizQuery 供前端取原始数据） =====================

    /** 库存追溯可视化数据集：批次/物料/仓库/移动单维度的移动链路汇总，对齐 {@code trace-chain.md §追溯链查询}。 */
    @BizQuery
    public List<Map<String, Object>> inventoryTraceData(@Optional @Name("batchNo") String batchNo,
                                                         @Optional @Name("materialId") Long materialId,
                                                         @Optional @Name("warehouseId") Long warehouseId,
                                                         @Optional @Name("moveId") Long moveId,
                                                         IServiceContext context) {
        return buildInventoryTraceDataset(batchNo, materialId, warehouseId, moveId, context);
    }

    // ===================== 数据集聚合实现 =====================

    /**
     * 库存追溯链数据集聚合。优先级：批次追溯 > 移动单追溯（正向链）> 物料/仓库过滤后正向链。
     *
     * <p>口径对齐 {@code trace-chain.md}：经 {@link IErpInvStockMoveBiz} 同域 I*Biz 的 4 个追溯方法聚合
     * 批次/物料的移动链路（{@code originMoveId}/{@code originReturnedMoveId} 上下游 + 批次号 + 移动方向 +
     * 数量 + 时间）。每行字段：moveId/code/moveType/businessDate/sourceWarehouseId/destWarehouseId/docStatus/
     * originMoveId/originReturnedMoveId/batchNos/quantity/traceType。
     */
    List<Map<String, Object>> buildInventoryTraceDataset(String batchNo, Long materialId, Long warehouseId,
                                                          Long moveId, IServiceContext context) {
        List<ErpInvStockMove> moves;
        String traceType;
        if (StringHelper.isNotEmpty(batchNo)) {
            // 批次追溯：跨移动单行 + 流水聚合（IErpInvStockMoveBiz.batchTrace）
            moves = stockMoveBiz.batchTrace(batchNo, context).getNodes();
            traceType = "BATCH";
        } else if (moveId != null) {
            // 移动单正向追溯：origin→dest 全下游链路
            moves = stockMoveBiz.forwardTrace(moveId, context).getNodes();
            traceType = "FORWARD";
        } else {
            // 物料/仓库过滤：定位候选移动单后正向追溯各自下游
            List<ErpInvStockMove> candidates = findCandidateMoves(materialId, warehouseId);
            if (candidates.isEmpty()) return Collections.emptyList();
            moves = expandForwardChain(candidates, context);
            traceType = "FORWARD";
        }
        return toTraceRows(moves, traceType);
    }

    private List<ErpInvStockMove> findCandidateMoves(Long materialId, Long warehouseId) {
        if (materialId == null && warehouseId == null) {
            return Collections.emptyList();
        }
        Set<Long> moveIdsByMaterial = null;
        if (materialId != null) {
            moveIdsByMaterial = new LinkedHashSet<>();
            QueryBean lineQ = new QueryBean();
            lineQ.addFilter(eq("materialId", materialId));
            for (ErpInvStockMoveLine line : daoProvider.daoFor(ErpInvStockMoveLine.class).findAllByQuery(lineQ)) {
                if (line.getMoveId() != null) moveIdsByMaterial.add(line.getMoveId());
            }
            if (moveIdsByMaterial.isEmpty()) return Collections.emptyList();
        }
        QueryBean q = new QueryBean();
        q.addFilter(eq("delVersion", 0L));
        if (warehouseId != null) {
            q.addFilter(or(eq("sourceWarehouseId", warehouseId), eq("destWarehouseId", warehouseId)));
        }
        if (moveIdsByMaterial != null) {
            q.addFilter(or(moveIdsByMaterial.stream()
                    .map(id -> eq("id", id))
                    .toArray(io.nop.api.core.beans.TreeBean[]::new)));
        }
        return daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
    }

    private List<ErpInvStockMove> expandForwardChain(List<ErpInvStockMove> candidates, IServiceContext context) {
        List<ErpInvStockMove> all = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (ErpInvStockMove m : candidates) {
            for (ErpInvStockMove node : stockMoveBiz.forwardTrace(m.getId(), context).getNodes()) {
                if (seen.add(node.getId())) all.add(node);
            }
        }
        return all;
    }

    private List<Map<String, Object>> toTraceRows(List<ErpInvStockMove> moves, String traceType) {
        if (moves.isEmpty()) return Collections.emptyList();
        IEntityDao<ErpInvStockMoveLine> lineDao = daoProvider.daoFor(ErpInvStockMoveLine.class);
        List<Map<String, Object>> rows = new ArrayList<>(moves.size());
        for (ErpInvStockMove m : moves) {
            List<ErpInvStockMoveLine> lines = loadLinesForMove(lineDao, m.getId());
            rows.add(moveToRow(m, lines, traceType));
        }
        return rows;
    }

    private List<ErpInvStockMoveLine> loadLinesForMove(IEntityDao<ErpInvStockMoveLine> dao, Long moveId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("moveId", moveId));
        return dao.findAllByQuery(q);
    }

    private Map<String, Object> moveToRow(ErpInvStockMove m, List<ErpInvStockMoveLine> lines, String traceType) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("moveId", m.getId());
        r.put("moveCode", m.getCode());
        r.put("moveType", m.getMoveType());
        r.put("businessDate", m.getBusinessDate());
        r.put("sourceWarehouseId", m.getSourceWarehouseId());
        r.put("destWarehouseId", m.getDestWarehouseId());
        r.put("docStatus", m.getDocStatus());
        r.put("originMoveId", m.getOriginMoveId());
        r.put("originReturnedMoveId", m.getOriginReturnedMoveId());
        r.put("isReturn", m.getOriginReturnedMoveId() != null);
        Set<String> batchNos = new LinkedHashSet<>();
        BigDecimal qty = BigDecimal.ZERO;
        Long materialId = null;
        for (ErpInvStockMoveLine line : lines) {
            if (StringHelper.isNotEmpty(line.getBatchNo())) batchNos.add(line.getBatchNo());
            if (line.getQuantity() != null) qty = qty.add(line.getQuantity());
            if (materialId == null) materialId = line.getMaterialId();
        }
        r.put("materialId", materialId);
        r.put("batchNos", String.join(",", batchNos));
        r.put("quantity", qty);
        r.put("traceType", traceType);
        return r;
    }
}
