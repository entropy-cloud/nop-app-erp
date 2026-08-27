package app.erp.mfg.service.crp;

import app.erp.mfg.biz.CrpLoadReportItem;
import app.erp.mfg.biz.IErpMfgCapacityProvider;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link IErpMfgCapacityProvider} mfg 侧实现（E3.4 TOC 瓶颈识别试点，AP-3 复用 {@link CrpLoadCalculator} 派生链）：
 * 包装 {@code getLoadReport} 并按工作中心聚合逐日 capacityHours（日历出勤 × 效率），供 aps 瓶颈识别消费。
 */
public class CrpCapacityProvider implements IErpMfgCapacityProvider {

    @Inject
    CrpLoadCalculator crpLoadCalculator;

    public void setCrpLoadCalculator(CrpLoadCalculator crpLoadCalculator) {
        this.crpLoadCalculator = crpLoadCalculator;
    }

    @Override
    public Map<String, BigDecimal> getCapacityHours(LocalDate periodFrom, LocalDate periodTo, List<String> workcenterIds) {
        Map<String, BigDecimal> result = new HashMap<>();
        if (crpLoadCalculator == null) {
            return result;
        }
        List<CrpLoadReportItem> items = crpLoadCalculator.getLoadReport(periodFrom, periodTo, workcenterIds);
        for (CrpLoadReportItem item : items) {
            if (item.getWorkcenterId() == null || item.getCapacityHours() == null) {
                continue;
            }
            result.merge(item.getWorkcenterId(), item.getCapacityHours(), BigDecimal::add);
        }
        return result;
    }
}
