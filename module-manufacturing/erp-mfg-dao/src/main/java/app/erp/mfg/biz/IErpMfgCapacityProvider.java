package app.erp.mfg.biz;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 工作中心可用产能查询 SPI（E3.4 TOC 瓶颈识别试点，`constraint-based-planning.md` §2 / AP-3）。
 *
 * <p>声明于 mfg-dao（消费方接口模式，同 {@link IErpApsLoadSourceProvider} 反向）：aps-service 经
 * {@code ioc:collect-beans} 跨模块收集实现，复用 {@code CrpLoadCalculator} 的产能派生链
 * （WorkcenterCalendar 出勤时段 × WorkcenterCapacity.efficiencyFactor），避免 APS 侧重复实现日历换算。
 *
 * <p>mfg-service 实现缺失（模块未聚合）时收集为空 list，调用方按兜底产能降级（行为不变）。
 */
public interface IErpMfgCapacityProvider {

    /**
     * 区间内各工作中心可用产能小时合计（逐日 capacityHours 求和，派生链同 CrpLoadCalculator.getLoadReport）。
     *
     * @param workcenterIds 工作中心 id 列表（null/空 = 按 {@code CrpLoadCalculator} 默认范围解析）
     * @return workcenterId → 产能小时；区间无日历配置的工作中心不出现在结果中
     */
    Map<String, BigDecimal> getCapacityHours(LocalDate periodFrom, LocalDate periodTo, List<String> workcenterIds);
}
