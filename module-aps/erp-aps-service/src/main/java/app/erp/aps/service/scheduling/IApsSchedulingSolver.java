package app.erp.aps.service.scheduling;

import app.erp.aps.biz.SchedulingResult;

/**
 * APS 排产求解器策略接口（E3.4 求解器分离，`constraint-based-planning.md` §1：求解策略与约束建模分离，
 * 贪心实现保留为默认，可插拔经 bean 注册 + config 切换，对齐 D3 子计算器注入范式）。
 *
 * <p>实现注册为 Nop IoC bean 后由 {@code ErpApsSchedulingProcessor} 经 {@code ioc:collect-beans}
 * 收集并按 {@code erp-aps.scheduling-solver}（默认 GREEDY）选择；未匹配时回退默认贪心（行为不变）。
 */
public interface IApsSchedulingSolver {

    String SOLVER_GREEDY = "GREEDY";
    String MODE_FORWARD = "FORWARD";
    String MODE_BACKWARD = "BACKWARD";
    String MODE_TOC = "TOC";

    /** 求解器名（config 匹配键）。 */
    String getName();

    /**
     * 求解一次排产。{@code request.mode} ∈ FORWARD / BACKWARD / TOC
     * （TOC = 瓶颈驱动试点：先排瓶颈中心（拉动式），再排非瓶颈（前/后向兜底））。
     */
    SchedulingResult solve(ApsSchedulingRequest request);
}
