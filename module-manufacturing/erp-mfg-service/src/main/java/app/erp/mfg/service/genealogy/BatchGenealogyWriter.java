package app.erp.mfg.service.genealogy;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
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
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

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

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 完工入库后写入基因链。config-gated（{@code erp-mfg.genealogy-write-enabled}）。
     * best-effort：内部任何异常仅记日志、不抛出（不阻断完工入库主流程）。
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
            LOG.error("工单 {} 完工写入批次基因链失败（best-effort，不阻断完工入库）", wo.getCode(), e);
        }
    }

    // ---------- step：写入主流程（protected，下游可逐个覆盖） ----------

    protected void doWrite(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
        ErpMfgWorkOrderLine outputLine = findOutputLine(wo.getId());
        if (outputLine == null) {
            return;
        }
        Long productId = wo.getProductId();
        if (productId == null) {
            productId = outputLine.getMaterialId();
        }
        Long uomId = outputLine.getUoMId();
        Long warehouseId = outputLine.getDestWarehouseId();
        if (warehouseId == null) {
            return;
        }

        List<ErpMfgMaterialIssueLine> issueLines = findIssueLinesWithBatch(wo.getId());
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
        Set<Long> usedInputLots = new HashSet<>();
        for (ErpMfgMaterialIssueLine issueLine : issueLines) {
            ErpInvBatch inputLot = resolveInputLot(issueLine, warehouseId);
            if (inputLot == null) {
                continue;
            }
            if (!usedInputLots.add(inputLot.getId())) {
                continue;
            }
            BigDecimal inputQty = nz(issueLine.getIssuedQuantity()).multiply(ratio)
                    .setScale(4, RoundingMode.HALF_UP);
            if (inputQty.signum() <= 0) {
                continue;
            }
            ErpMfgBatchGenealogy row = newEntity();
            row.setWorkOrderId(wo.getId());
            row.setInputLotId(inputLot.getId());
            row.setInputMaterialId(issueLine.getMaterialId());
            row.setInputQty(inputQty);
            row.setInputUoMId(issueLine.getUoMId());
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

    protected ErpInvBatch ensureOutputLot(ErpMfgWorkOrder wo, Long productId, Long warehouseId,
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

    protected ErpInvBatch resolveInputLot(ErpMfgMaterialIssueLine issueLine, Long warehouseId) {
        String batchNo = issueLine.getBatchNo();
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return null;
        }
        return findBatchByNo(batchNo, issueLine.getMaterialId(), warehouseId);
    }

    // ---------- step：查询辅助（protected，供派生复用与覆盖） ----------

    protected List<ErpMfgMaterialIssueLine> findIssueLinesWithBatch(Long workOrderId) {
        IEntityDao<ErpMfgMaterialIssue> issueDao = daoProvider.daoFor(ErpMfgMaterialIssue.class);
        QueryBean iq = new QueryBean();
        iq.addFilter(eq("workOrderId", workOrderId));
        List<ErpMfgMaterialIssue> issues = issueDao.findAllByQuery(iq);
        if (issues.isEmpty()) {
            return new ArrayList<>();
        }
        IEntityDao<ErpMfgMaterialIssueLine> lineDao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
        List<ErpMfgMaterialIssueLine> result = new ArrayList<>();
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
                    result.add(line);
                }
            }
        }
        return result;
    }

    protected boolean isIssueConsumed(ErpMfgMaterialIssue issue) {
        String status = issue.getDocStatus();
        return Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_CONFIRMED)
                || Objects.equals(status, ErpMfgConstants.ISSUE_STATUS_DONE);
    }

    protected ErpMfgWorkOrderLine findOutputLine(Long workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.addFilter(eq("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_OUTPUT));
        q.setLimit(1);
        IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
        List<ErpMfgWorkOrderLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected ErpInvBatch findBatchByNo(String batchNo, Long materialId, Long warehouseId) {
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
}
