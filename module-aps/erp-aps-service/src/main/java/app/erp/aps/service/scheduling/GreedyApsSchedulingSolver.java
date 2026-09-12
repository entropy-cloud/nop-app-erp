package app.erp.aps.service.scheduling;

import app.erp.aps.biz.SchedulingResult;
import app.erp.aps.dao.entity.ErpApsConstraint;
import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.dao.entity.ErpApsOpRouting;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 默认贪心求解器（E3.4 求解器分离：既有 {@link ErpApsSchedulingEngine} 的策略适配器，行为不变）。
 *
 * <p>FORWARD/BACKWARD 与既有调用形状逐参一致（含 frozenPlanned/routings 重载归并）；TOC 委托引擎
 * {@code scheduleToc}（瓶颈中心优先 + 前/后向兜底）。纯算法委托，无 ORM/DB 依赖。
 */
public class GreedyApsSchedulingSolver implements IApsSchedulingSolver {

    @Override
    public String getName() {
        return SOLVER_GREEDY;
    }

    @Override
    public SchedulingResult solve(ApsSchedulingRequest request) {
        ErpApsSchedulingEngine engine = new ErpApsSchedulingEngine(request.getBufferMinutes(),
                request.getHorizonStart(), request.getHorizonEnd(), request.getRoutingEffectiveDate());
        if (MODE_BACKWARD.equals(request.getMode())) {
            return engine.scheduleBackward(request.getOrders(), request.getMaintenanceConstraints(),
                    request.getRoutings(), request.getFrozenPlanned(), request.getDefaultEarliestStart());
        }
        if (MODE_TOC.equals(request.getMode())) {
            Set<String> bottlenecks = resolveBottlenecks(request);
            SchedulingResult result = engine.scheduleToc(request.getOrders(), request.getMaintenanceConstraints(),
                    request.getRoutings(), request.getFrozenPlanned(), bottlenecks, request.getDefaultEarliestStart());
            result.setMachineLoadRates(request.getMachineLoadRates() == null
                    ? Map.of() : request.getMachineLoadRates());
            result.getBottleneckMachineIds().addAll(bottlenecks);
            return result;
        }
        return engine.scheduleForward(request.getOrders(), request.getMaintenanceConstraints(),
                request.getFrozenPlanned(), request.getRoutings(), request.getDefaultEarliestStart());
    }

    /** 瓶颈中心集合 = 负荷率超过阈值的机器（阈值语义对齐 CRP overload：严格大于）。 */
    private Set<String> resolveBottlenecks(ApsSchedulingRequest request) {
        Set<String> bottlenecks = new LinkedHashSet<>();
        Map<String, BigDecimal> rates = request.getMachineLoadRates();
        if (rates == null) {
            return bottlenecks;
        }
        BigDecimal threshold = BigDecimal.valueOf(request.getBottleneckThreshold());
        for (Map.Entry<String, BigDecimal> e : rates.entrySet()) {
            if (e.getValue() != null && e.getValue().compareTo(threshold) > 0) {
                bottlenecks.add(e.getKey());
            }
        }
        return bottlenecks;
    }
}
