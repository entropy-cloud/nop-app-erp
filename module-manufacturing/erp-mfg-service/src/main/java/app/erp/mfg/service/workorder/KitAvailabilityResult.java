package app.erp.mfg.service.workorder;

import app.erp.mfg.service.ErpMfgConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 齐套校验结果（{@link KitAvailabilityChecker#check}）。
 *
 * <p>全齐 → {@link #fullyAvailable}=true、{@link #shortages}=空、{@link #resultingStatus}={@link ErpMfgConstants#WORK_ORDER_STATUS_STOCK_RESERVED}。
 * 部分齐套 → {@link #fullyAvailable}=false、{@link #shortages}含缺料明细、{@link #resultingStatus}={@link ErpMfgConstants#WORK_ORDER_STATUS_STOCK_PARTIAL}。
 *
 * <p>权威：`docs/design/manufacturing/state-machine.md §迁移完整性`、
 * `docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md` Phase 2。
 */
public class KitAvailabilityResult {

    private boolean fullyAvailable;
    private int resultingStatus;
    private final List<KitShortage> shortages = new ArrayList<>();

    public static KitAvailabilityResult reserved() {
        KitAvailabilityResult r = new KitAvailabilityResult();
        r.fullyAvailable = true;
        r.resultingStatus = ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED;
        return r;
    }

    public static KitAvailabilityResult partial() {
        KitAvailabilityResult r = new KitAvailabilityResult();
        r.fullyAvailable = false;
        r.resultingStatus = ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL;
        return r;
    }

    public boolean isFullyAvailable() {
        return fullyAvailable;
    }

    public int getResultingStatus() {
        return resultingStatus;
    }

    public List<KitShortage> getShortages() {
        return shortages;
    }

    /**
     * 缺料明细行：物料 × 应需量 × 可用量（应需 − 可用 = 缺口）。
     */
    public static class KitShortage {
        private final Long materialId;
        private final BigDecimal requiredQty;
        private final BigDecimal availableQty;

        public KitShortage(Long materialId, BigDecimal requiredQty, BigDecimal availableQty) {
            this.materialId = materialId;
            this.requiredQty = requiredQty;
            this.availableQty = availableQty;
        }

        public Long getMaterialId() {
            return materialId;
        }

        public BigDecimal getRequiredQty() {
            return requiredQty;
        }

        public BigDecimal getAvailableQty() {
            return availableQty;
        }
    }
}
