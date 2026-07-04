package app.erp.mfg.service;

import app.erp.mfg.biz.ApsLoadSlot;
import app.erp.mfg.biz.IErpApsLoadSourceProvider;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试专用 APS 负荷来源 SPI 桩（plan 2026-07-05-0306-2 Phase 3）。
 *
 * <p>替代真实 aps-service 的 {@code ApsLoadSourceProvider}（需 aps-dao 实体注册），由 {@code test-aps-load-source.beans.xml}
 * 注册并通过 {@code ioc:collect-beans by-type} 注入到 {@code CrpLoadCalculator.apsLoadSourceProviders}。
 *
 * <p>测试方法在 seed 阶段调 {@link #putSlots} 注入工序时段，CRP APS 分支据此分派负荷。
 */
public class TestStubApsLoadSourceProvider implements IErpApsLoadSourceProvider {

    private final Map<Long, List<ApsLoadSlot>> slotsByWorkOrder = new HashMap<>();

    @Override
    public List<ApsLoadSlot> findScheduledSlots(List<Long> workOrderIds, LocalDate periodFrom, LocalDate periodTo) {
        if (workOrderIds == null || workOrderIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ApsLoadSlot> result = new ArrayList<>();
        for (Long woId : workOrderIds) {
            List<ApsLoadSlot> slots = slotsByWorkOrder.get(woId);
            if (slots != null) {
                result.addAll(slots);
            }
        }
        return result;
    }

    public void putSlots(Long workOrderId, List<ApsLoadSlot> slots) {
        slotsByWorkOrder.put(workOrderId, slots);
    }

    public void clear() {
        slotsByWorkOrder.clear();
    }
}
