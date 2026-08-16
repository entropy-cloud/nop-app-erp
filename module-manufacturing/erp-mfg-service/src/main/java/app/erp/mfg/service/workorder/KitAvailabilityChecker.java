package app.erp.mfg.service.workorder;

import app.erp.mfg.biz.BomExplosionNode;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomSnapshot;
import app.erp.mfg.service.ErpMfgConfigs;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.bom.BomExpander;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.in;

/**
 * 齐套校验器：展开工单 BOM 子件 × plannedQuantity，对照 inventory 余额可用量（availableQuantity = onHand − reserved）。
 *
 * <p>全齐 → {@link KitAvailabilityResult#reserved()}（→ WORK_ORDER_STATUS_STOCK_RESERVED）；
 * 部分齐套 → {@link KitAvailabilityResult#partial()}（→ WORK_ORDER_STATUS_STOCK_PARTIAL，附缺料明细）。
 *
 * <p>只读校验，不写预留（实际预留由工单开工后的领料出库移动单扣减，对齐
 * `docs/design/inventory/cross-domain.md §余量校验规则`）。本类为非 BizModel 服务助手（对齐 {@link BomExpander}
 * 范式），直接用 {@link IDaoProvider} 只读查询库存余额（跨域只读聚合，无权限管道语义）。
 *
 * <p><b>BOM 快照读侧（plan 2026-08-16-0904-1 RC-R1.49，UC-MFG-10 断言④⑤⑥）</b>：已快照工单
 * （{@code snapshotBomId/snapshotBomVersion 非空}）恒读快照内容展开（LOCK_AT_CREATION 默认）；无快照
 * （DRAFT/未提交/旧数据）恒走实时 BOM 零回归；AUTO_UPGRADE 策略下 re-resolve 默认 BOM 实时展开（不写回快照）。
 *
 * <p>权威：`docs/design/manufacturing/state-machine.md §迁移完整性`、`docs/design/inventory/cross-domain.md §余量校验规则`、
 * `docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md` Phase 2。
 */
public class KitAvailabilityChecker {

    static final BigDecimal ZERO = BigDecimal.ZERO;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    BomExpander bomExpander;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    public void setBomExpander(BomExpander bomExpander) {
        this.bomExpander = bomExpander;
    }

    /**
     * 校验工单齐套情况。BOM 取工单 {@code bomId}（缺失则回落到产品的默认 BOM），按 {@code plannedQuantity} 多级展开子件需求。
     * 已快照工单读快照内容（提交时点锁定），无快照实时 BOM（零回归）。
     *
     * @param workOrderId 工单 ID
     * @return 齐套校验结果（全齐 / 部分齐套）
     */
    public KitAvailabilityResult check(Long workOrderId) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId);
        BigDecimal plannedQty = nz(wo.getPlannedQuantity());
        List<BomExplosionNode> nodes = explodeRequirements(wo, plannedQty);

        Map<Long, BigDecimal> requiredByMaterial = aggregateRequirements(nodes);
        if (requiredByMaterial.isEmpty()) {
            return KitAvailabilityResult.reserved();
        }

        Map<Long, BigDecimal> availableByMaterial = loadAvailableByMaterial(requiredByMaterial.keySet());

        KitAvailabilityResult result = KitAvailabilityResult.reserved();
        for (Map.Entry<Long, BigDecimal> e : requiredByMaterial.entrySet()) {
            Long materialId = e.getKey();
            BigDecimal required = e.getValue();
            BigDecimal available = nz(availableByMaterial.get(materialId));
            if (available.compareTo(required) < 0) {
                if (result.isFullyAvailable()) {
                    result = KitAvailabilityResult.partial();
                }
                result.getShortages().add(new KitAvailabilityResult.KitShortage(
                        materialId, required.setScale(4, RoundingMode.HALF_UP), available.setScale(4, RoundingMode.HALF_UP)));
            }
        }
        return result;
    }

    public Map<Long, BigDecimal> aggregateRequirements(List<BomExplosionNode> nodes) {
        Map<Long, BigDecimal> requiredByMaterial = new HashMap<>();
        for (BomExplosionNode node : nodes) {
            if (node.getMaterialId() == null) {
                continue;
            }
            requiredByMaterial.merge(node.getMaterialId(), nz(node.getQuantity()), BigDecimal::add);
        }
        return requiredByMaterial;
    }

    /**
     * 按 BOM 多级展开子件需求（RC-R1.48 物料预留创建复用；对齐 {@link #check} 的 explode 调用范式）。
     */
    public List<BomExplosionNode> explodeRequirements(Long bomId, BigDecimal requestedQty) {
        return bomExpander.explode(bomId, requestedQty, true);
    }

    /**
     * 快照感知展开（plan 2026-08-16-0904-1 RC-R1.49）：已快照工单（LOCK_AT_CREATION）经快照内容展开；
     * AUTO_UPGRADE 时 re-resolve 默认 BOM 实时展开（无默认 BOM 防御回退快照）；无快照走实时 BOM 零回归。
     * approve 预留路径（{@code createReservations}）复用本入口，快照切换自动继承。
     */
    public List<BomExplosionNode> explodeRequirements(ErpMfgWorkOrder wo, BigDecimal requestedQty) {
        if (hasBomSnapshot(wo)) {
            if (ErpMfgConfigs.isBomSnapshotAutoUpgrade()) {
                ErpMfgBom latest = bomExpander.findDefaultBomOrNull(wo.getProductId());
                if (latest != null) {
                    return bomExpander.explode(latest.getId(), requestedQty, true);
                }
                // 无默认 BOM（升级目标缺失）→ 防御回退快照（保持锁定内容语义）
                ErpMfgWorkOrderBomSnapshot snap = loadBomSnapshot(wo);
                if (snap != null) {
                    return bomExpander.explodeFromSnapshot(snap, requestedQty, true);
                }
            } else {
                ErpMfgWorkOrderBomSnapshot snap = loadBomSnapshot(wo);
                if (snap != null) {
                    return bomExpander.explodeFromSnapshot(snap, requestedQty, true);
                }
            }
        }
        Long bomId = resolveBomId(wo);
        return bomExpander.explode(bomId, requestedQty, true);
    }

    /**
     * 工单是否已快照 BOM（snapshotBomId/snapshotBomVersion 任一非空即视为已快照；提交时点复制后回写）。
     */
    public boolean hasBomSnapshot(ErpMfgWorkOrder wo) {
        return wo != null && (wo.getSnapshotBomId() != null || wo.getSnapshotBomVersion() != null);
    }

    private ErpMfgWorkOrderBomSnapshot loadBomSnapshot(ErpMfgWorkOrder wo) {
        io.nop.orm.IOrmEntitySet<ErpMfgWorkOrderBomSnapshot> snaps = wo.getBomSnapshot();
        return snaps == null || snaps.isEmpty() ? null : snaps.iterator().next();
    }

    private Map<Long, BigDecimal> loadAvailableByMaterial(Set<Long> materialIds) {
        Map<Long, BigDecimal> availableByMaterial = new HashMap<>();
        if (materialIds.isEmpty()) {
            return availableByMaterial;
        }
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        // 按物料 IN 查询，余额表多仓/批时聚合 availableQuantity。
        List<ErpInvStockBalance> balances = dao.findAllByQuery(buildBalanceQuery(materialIds));
        for (ErpInvStockBalance b : balances) {
            availableByMaterial.merge(b.getMaterialId(), nz(b.getAvailableQuantity()), BigDecimal::add);
        }
        return availableByMaterial;
    }

    private QueryBean buildBalanceQuery(Set<Long> materialIds) {
        QueryBean q = new QueryBean();
        q.addFilter(in("materialId", new ArrayList<>(materialIds)));
        return q;
    }

    private ErpMfgWorkOrder requireWorkOrder(Long workOrderId) {
        if (workOrderId == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND).param(ErpMfgErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(workOrderId);
        if (wo == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND).param(ErpMfgErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        return wo;
    }

    public Long resolveBomId(ErpMfgWorkOrder wo) {
        if (wo.getBomId() != null) {
            return wo.getBomId();
        }
        ErpMfgBom defaultBom = bomExpander.findDefaultBomOrNull(wo.getProductId());
        if (defaultBom == null) {
            throw new NopException(ErpMfgErrors.ERR_DEFAULT_BOM_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_PRODUCT_ID, wo.getProductId());
        }
        return defaultBom.getId();
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : ZERO;
    }
}
