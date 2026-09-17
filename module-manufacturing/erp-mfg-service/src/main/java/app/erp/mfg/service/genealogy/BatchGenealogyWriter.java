package app.erp.mfg.service.genealogy;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.notify.biz.IErpSysNotificationBiz;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpInvBatch、ErpMfgBatchGenealogy、ErpMfgMaterialIssue、ErpMfgMaterialIssueLine、ErpMfgWorkOrderLine）=跨域批量聚合（inv），只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 完工入库时写入生产批次基因链（{@code ErpMfgBatchGenealogy}）。
 *
 * <p>plan 2026-07-07-0305-3 §Phase 1「写入时机/产出批次获取/失败语义」三 Decision 裁定：
 * <ul>
 *   <li>写入时机：在 {@code ErpMfgWorkOrderProcessor.reportCompletion} 完工入库成功后一次性按本次完工消耗写入，
 *       而非领料时 progressive 累积（与 2237-1 完工聚合点一致、避免领料-完工时序耦合）。</li>
 *   <li>产出批次获取：完工时自动创建 {@link ErpInvBatch} 产出批次（batchNo 由工单 code 派生，
 *       状态 OPEN）——完工入库 {@code generateCompletionMove} 当前未设 batchNo，故在此自动建批。</li>
 *   <li>失败语义：best-effort——基因链写入失败仅记 ERROR 日志、不回滚完工入库
 *       （由 {@code erp-mfg.genealogy-write-enabled} 总开关控制）。</li>
 * </ul>
 *
 * <p>各 step 为 {@code protected} 方法，下游派生 Writer 可逐个覆盖（产品化可定制性）。
 *
 * <p>权威：{@code docs/design/manufacturing/batch-genealogy.md}、{@code docs/plans/2026-07-07-0305-3.md}。
 */
public class BatchGenealogyWriter {

    private static final Logger LOG = LoggerFactory.getLogger(BatchGenealogyWriter.class);

    @Inject
    IDaoProvider daoProvider;

    @Inject
    IErpSysNotificationBiz notificationBiz;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setNotificationBiz(IErpSysNotificationBiz notificationBiz) {
        this.notificationBiz = notificationBiz;
    }

    /**
     * 完工入库后写入基因链。config-gated（{@code erp-mfg.genealogy-write-enabled}）。
     * best-effort：内部任何异常仅记日志 + 派发失败告警、不抛出（不阻断完工入库主流程）。
     */
    public void writeOnCompletion(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
        if (!isWriteEnabled()) {
            return;
        }
        if (completedQty == null || completedQty.signum() <= 0) {
            return;
        }
        try {
            doWrite(wo, completedQty, context);
        } catch (Exception e) {
            LOG.error("Failed to write batch genealogy on completion for work order {} (best-effort, non-blocking for completion receipt)", wo.getCode(), e);
            dispatchGenealogyWriteFailureAlert(wo, e);
        }
    }

    /**
     * 基因链写失败告警派发（G3；通知失败降级不阻断主流程）。镜像
     * {@code ErpMfgWorkOrderProcessor.dispatchVarianceFailureAlert} 范式（A4.2.4）——消除
     * A4.2.9 residual observability gap（catch 仅 LOG.error 无监控采集通道，运营不可感知）。
     * 无 ACTIVE 模板时 notify config-gated 静默跳过（运营侧配置模板后生效，对齐
     * {@code IErpSysNotificationBiz.notify} 契约）。
     */
    protected void dispatchGenealogyWriteFailureAlert(ErpMfgWorkOrder wo, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("workOrderId", wo.getId());
        ctx.put("workOrderCode", wo.getCode());
        ctx.put("errorCode", cause instanceof NopException
                ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        IServiceContext serviceCtx = serviceContext();
        try {
            notificationBiz.notify(ErpMfgConstants.NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("Genealogy write failure alert dispatch failed (degraded): workOrderCode={}, reason={}",
                    wo.getCode(), notifyErr.getMessage());
        }
    }

    // ---------- step：写入主流程（protected，下游可逐个覆盖） ----------

    protected void doWrite(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
        ErpMfgWorkOrderLine outputLine = findOutputLine(wo.getId());
        if (outputLine == null) {
            return;
        }
        String productId = wo.getProductId();
        if (productId == null) {
            productId = outputLine.getMaterialId();
        }
        String uomId = outputLine.getUoMId();
        String warehouseId = outputLine.getDestWarehouseId();
        if (warehouseId == null) {
            return;
        }

        List<IssueLineCtx> issueLines = findIssueLinesWithBatch(wo.getId());
        if (issueLines.isEmpty()) {
            return;
        }

        ErpInvBatch outputLot = ensureOutputLot(wo, productId, warehouseId, completedQty, context);
        if (outputLot == null) {
            return;
        }

        LocalDate productionDate = wo.getBusinessDate() != null ? wo.getBusinessDate() : CoreMetrics.today();
        Timestamp productionTime = CoreMetrics.currentTimestamp();
        BigDecimal plannedQty = nz(wo.getPlannedQuantity());
        BigDecimal ratio = plannedQty.signum() > 0
                ? completedQty.divide(plannedQty, 6, RoundingMode.HALF_UP)
                : BigDecimal.ONE;

        int lineNo = 10;
        // P2-CK-mfg3-008：同批次多领料行去重丢量——改 Map 累合替代去重跳过
        // (inputLotId → [inputLot, materialId, uoMId, qty])，qty 按 ratio 缩放后累加
        Map<String, Object[]> aggregatedInputs = new LinkedHashMap<>();
        for (IssueLineCtx ctx : issueLines) {
            ErpMfgMaterialIssueLine issueLine = ctx.line;
            ErpInvBatch inputLot = resolveInputLot(issueLine, ctx.issueWarehouseId);
            if (inputLot == null) {
                continue;
            }
            BigDecimal inputQty = nz(issueLine.getIssuedQuantity()).multiply(ratio)
                    .setScale(4, RoundingMode.HALF_UP);
            if (inputQty.signum() <= 0) {
                continue;
            }
            String lotId = inputLot.getId();
            if (aggregatedInputs.containsKey(lotId)) {
                Object[] prev = aggregatedInputs.get(lotId);
                prev[3] = ((BigDecimal) prev[3]).add(inputQty);
            } else {
                aggregatedInputs.put(lotId, new Object[]{inputLot, issueLine.getMaterialId(),
                        issueLine.getUoMId(), inputQty});
            }
        }
        // P2-CK-mfg3-008：从聚合 Map 生成基因链行（同批次多行已累合）
        for (Object[] agg : aggregatedInputs.values()) {
            ErpInvBatch inputLot = (ErpInvBatch) agg[0];
            String materialId = (String) agg[1];
            String lineUoMId = (String) agg[2];
            BigDecimal totalQty = (BigDecimal) agg[3];

            ErpMfgBatchGenealogy row = newEntity();
            row.setWorkOrderId(wo.getId());
            row.setInputLotId(inputLot.getId());
            row.setInputMaterialId(materialId);
            row.setInputQty(totalQty);
            row.setInputUoMId(lineUoMId);
            row.setOutputLotId(outputLot.getId());
            row.setOutputMaterialId(productId);
            row.setOutputQty(completedQty);
            row.setOutputUoMId(uomId);
            row.setProductionDate(productionDate);
            row.setProductionTime(productionTime);
            row.setLineNo(lineNo);
            row.setLotStatus(ErpMfgConstants.LOT_STATUS_RELEASED);
            row.setIsInputConsumed(Boolean.TRUE);
            genealogyDao().saveEntity(row);
            lineNo += 10;
        }
    }

    // ---------- step：产出批次获取（自动建批） ----------

    protected ErpInvBatch ensureOutputLot(ErpMfgWorkOrder wo, String productId, String warehouseId,
                                          BigDecimal completedQty, IServiceContext context) {
        String batchNo = ErpMfgConstants.GENEALOGY_OUTPUT_BATCH_PREFIX + "-" + wo.getCode();
        ErpInvBatch existing = findBatchByNo(batchNo, productId, warehouseId);
        if (existing != null) {
            existing.setTotalQuantity(nz(existing.getTotalQuantity()).add(completedQty));
            existing.setAvailableQuantity(nz(existing.getAvailableQuantity()).add(completedQty));
            batchDao().updateEntity(existing);
            return existing;
        }
        ErpInvBatch batch = newBatchEntity();
        batch.setOrgId(wo.getOrgId());
        batch.setBatchNo(batchNo);
        batch.setMaterialId(productId);
        batch.setWarehouseId(warehouseId);
        batch.setTotalQuantity(completedQty);
        batch.setAvailableQuantity(completedQty);
        batch.setProductionDate(wo.getBusinessDate() != null ? wo.getBusinessDate() : CoreMetrics.today());
        batch.setStatus(ErpMfgConstants.INV_BATCH_STATUS_OPEN);
        batchDao().saveEntity(batch);
        return batch;
    }

    // ---------- step：输入批次解析（batchNo 字符串 → ErpInvBatch） ----------

    protected ErpInvBatch resolveInputLot(ErpMfgMaterialIssueLine issueLine, String warehouseId) {
        String batchNo = issueLine.getBatchNo();
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return null;
        }
        return findBatchByNo(batchNo, issueLine.getMaterialId(), warehouseId);
    }

    // ---------- step：查询辅助（protected，供派生复用与覆盖） ----------

    protected List<IssueLineCtx> findIssueLinesWithBatch(String workOrderId) {
        IEntityDao<ErpMfgMaterialIssue> issueDao = daoProvider.daoFor(ErpMfgMaterialIssue.class);
        QueryBean iq = new QueryBean();
        iq.addFilter(eq("workOrderId", workOrderId));
        List<ErpMfgMaterialIssue> issues = issueDao.findAllByQuery(iq);
        if (issues.isEmpty()) {
            return new ArrayList<>();
        }
        IEntityDao<ErpMfgMaterialIssueLine> lineDao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
        List<IssueLineCtx> result = new ArrayList<>();
        for (ErpMfgMaterialIssue issue : issues) {
            if (!isIssueConsumed(issue)) {
                continue;
            }
            QueryBean lq = new QueryBean();
            lq.addFilter(eq("issueId", issue.getId()));
            List<ErpMfgMaterialIssueLine> lines = lineDao.findAllByQuery(lq);
            for (ErpMfgMaterialIssueLine line : lines) {
                if (line.getBatchNo() != null && !line.getBatchNo().trim().isEmpty()
                        && line.getIssuedQuantity() != null && line.getIssuedQuantity().signum() > 0) {
                    result.add(new IssueLineCtx(line, issue.getWarehouseId()));
                }
            }
        }
        return result;
    }

    /** 领料行 + 领料单头仓库（P1-CK-mfg3-004：输入批次按领料仓而非产成品仓解析）。 */
    protected static class IssueLineCtx {
        final ErpMfgMaterialIssueLine line;
        final String issueWarehouseId;

        IssueLineCtx(ErpMfgMaterialIssueLine line, String issueWarehouseId) {
            this.line = line;
            this.issueWarehouseId = issueWarehouseId;
        }
    }

    protected boolean isIssueConsumed(ErpMfgMaterialIssue issue) {
        String status = issue.getDocStatus();
        return Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_CONFIRMED)
                || Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_DONE);
    }

    protected ErpMfgWorkOrderLine findOutputLine(String workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.addFilter(eq("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_OUTPUT));
        q.setLimit(1);
        IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
        List<ErpMfgWorkOrderLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected ErpInvBatch findBatchByNo(String batchNo, String materialId, String warehouseId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("batchNo", batchNo));
        q.addFilter(eq("materialId", materialId));
        if (warehouseId != null) {
            q.addFilter(eq("warehouseId", warehouseId));
        }
        q.setLimit(1);
        List<ErpInvBatch> list = batchDao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected boolean isWriteEnabled() {
        try {
            String value = AppConfig.var(ErpMfgConstants.CONFIG_GENEALOGY_WRITE_ENABLED, "true");
            if (value == null || value.trim().isEmpty()) {
                return true;
            }
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return true;
        }
    }

    // ---------- misc helpers ----------

    protected ErpMfgBatchGenealogy newEntity() {
        return genealogyDao().newEntity();
    }

    protected ErpInvBatch newBatchEntity() {
        return batchDao().newEntity();
    }

    protected IEntityDao<ErpMfgBatchGenealogy> genealogyDao() {
        return daoProvider.daoFor(ErpMfgBatchGenealogy.class);
    }

    protected IEntityDao<ErpInvBatch> batchDao() {
        return daoProvider.daoFor(ErpInvBatch.class);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    /** 当前服务上下文；无绑定（job 入口/直接 Java 调用）时兜底新建——M2.8 分片③ common-015-r3 族回填，
     * 镜像 ExpenseCostAggregator 兜底范式：优先继承调用方绑定上下文（身份/数据权限），仅无绑定时构造新上下文。 */
    private static IServiceContext serviceContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }
}
