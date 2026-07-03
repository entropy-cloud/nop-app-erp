package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.config.AppConfig;
import io.nop.core.context.IServiceContext;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * 业务域→质量域强制质检触发助手（business→quality 同步 I*Biz 写触发，DAG 无环）。
 *
 * <p>业务域 Processor/BizModel 在 confirm/approve/reportCompletion 等流转节点调用 {@link #enforceGate}：
 * <ul>
 *   <li>billType 不在 {@code erp-qua.mandatory-inspection-bill-types}（默认空）→ 直接返回 CLEARED，无副作用。</li>
 *   <li>属强制类型且无关联质检单 → 经 {@code IErpQaInspectionBiz.createForBusinessBill} 生成 PENDING 质检单，返回 BLOCKED（阻塞本次流转，待质检结论）。</li>
 *   <li>属强制类型且关联质检单未合格/让步（PENDING 或 REJECTED）→ 返回 BLOCKED。</li>
 *   <li>关联质检单 ACCEPTED/CONDITIONAL → 返回 CLEARED（放行）。</li>
 * </ul>
 *
 * <p>配置默认空 → 现有业务流转行为不变（不触发质量域）。权威：计划 Phase 2 Task Route Decision。
 */
public final class InspectionTrigger {

    public static final int CLEARED = 0;
    public static final int BLOCKED = 1;

    private InspectionTrigger() {
    }

    /**
     * @return {@link #CLEARED} 放行；{@link #BLOCKED} 阻塞（已创建或已有未决质检单）。
     */
    public static int enforceGate(IErpQaInspectionBiz inspectionBiz,
                                  String billType, String billCode, Long materialId, Integer inspectionType,
                                  BigDecimal lotQuantity, Long supplierId, Long warehouseId, String batchNo,
                                  IServiceContext context) {
        if (!isMandatoryBillType(billType)) {
            return CLEARED;
        }
        List<ErpQaInspection> existing = inspectionBiz.findByRelatedBill(billType, billCode, context);
        if (existing.isEmpty()) {
            // 首次流转：生成 PENDING 质检单并阻塞本次（待质检结论后再次流转放行）
            inspectionBiz.createForBusinessBill(billType, billCode, materialId, inspectionType,
                    lotQuantity, supplierId, warehouseId, batchNo, context);
            return BLOCKED;
        }
        return inspectionBiz.isInspectionCleared(billType, billCode, context) ? CLEARED : BLOCKED;
    }

    /** billType 是否属强制质检类型（{@code erp-qua.mandatory-inspection-bill-types}，逗号分隔；空=不强制）。 */
    public static boolean isMandatoryBillType(String billType) {
        if (billType == null || billType.isEmpty()) {
            return false;
        }
        String raw = AppConfig.var("erp-qua.mandatory-inspection-bill-types", "");
        if (raw == null || raw.trim().isEmpty()) {
            return false;
        }
        return Arrays.asList(raw.split(",")).stream().anyMatch(s -> s.trim().equals(billType));
    }
}
