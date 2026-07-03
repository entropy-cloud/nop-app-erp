package app.erp.mfg.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;

import java.time.LocalDate;
import java.util.List;

/**
 * CRP（产能需求计划）负荷计算契约。{@code docs/design/manufacturing/crp.md}。
 *
 * <p>由 {@code IErpMfgCrpLoadBiz} 继承，挂在 {@code ErpMfgCrpLoad} 实体上对外暴露为 GraphQL 动作
 * {@code ErpMfgCrpLoad__calculateLoad} / {@code ErpMfgCrpLoad__getLoadReport}。
 *
 * <p>CRP 为只读负荷报表（不写排程）；负荷来源本期取 WorkOrder 计划日期 + RoutingOperation 标准工时
 * （无 APS OperationOrder 排程时间，fallback）。
 */
public interface IErpMfgCrpBiz {

    /**
     * 计算负荷快照：扫描计划日期落在 [periodFrom, periodTo] 且非 CANCELLED 的 WorkOrder，
     * 经 RoutingOperation 分派到工作中心，按 workcenter×loadDate 聚合 loadHours（标准工时）+ setupHours（换模），
     * 重算前清区间内既有 CrpLoad 快照再写新行。
     *
     * @param workcenterIds 可选，限定工作中心范围；null/空 表示全部。
     * @return 写入的 CrpLoad 行数。
     */
    @BizMutation
    Integer calculateLoad(@Name("periodFrom") LocalDate periodFrom,
                          @Name("periodTo") LocalDate periodTo,
                          @Optional @Name("workcenterIds") List<Long> workcenterIds,
                          IServiceContext context);

    /**
     * 负荷报表查询：返回 [periodFrom, periodTo] 内 workcenter×date 聚合
     * （loadHours / capacityHours / loadRate / overloaded）。
     * {@code overloaded = loadRate > erp-mfg.crp-overload-threshold}（默认 1.0）。
     *
     * @param workcenterIds 可选，限定工作中心范围；null/空 表示全部。
     */
    @BizQuery
    List<CrpLoadReportItem> getLoadReport(@Name("periodFrom") LocalDate periodFrom,
                                          @Name("periodTo") LocalDate periodTo,
                                          @Optional @Name("workcenterIds") List<Long> workcenterIds,
                                          IServiceContext context);
}
