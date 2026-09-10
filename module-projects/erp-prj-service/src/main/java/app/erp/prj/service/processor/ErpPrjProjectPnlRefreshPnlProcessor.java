package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.service.pnl.ProjectPnlCalculator;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.time.LocalDate;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpPrjProjectPnl refreshPnl per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含损益汇总计算（聚合 Billing 收入 + CostCollection 四类成本）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjProjectPnlRefreshPnlProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ProjectPnlCalculator pnlCalculator;

    public ErpPrjProjectPnl refreshPnl(String projectId, LocalDate periodFrom, LocalDate periodTo,
                                       IServiceContext context) {
        return pnlCalculator.refreshPnl(projectId, periodFrom, periodTo);
    }
}
