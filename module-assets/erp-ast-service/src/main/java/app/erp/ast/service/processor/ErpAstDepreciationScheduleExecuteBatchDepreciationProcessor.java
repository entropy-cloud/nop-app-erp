package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * ErpAstDepreciationSchedule executeBatchDepreciation per-mutation Processor（R6.3，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量折旧计提编排（期间控制 + 逐资产委托 {@link ErpAstDepreciationScheduleExecuteDepreciationProcessor} + 错误隔离）；
 * 共享 protected helper 单一真相源在 {@link ErpAstDepreciationScheduleProcessor}（delete-after-extract facade）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor.class);

    @Inject
    ErpAstDepreciationScheduleProcessor facade;

    @Inject
    IDaoProvider daoProvider;

    @Inject
    ErpAstDepreciationScheduleExecuteDepreciationProcessor executeDepreciationProcessor;

    public int executeBatchDepreciation(String period, IServiceContext context) {
        facade.requirePeriodOpen(period, context);

        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE));
        List<ErpAstAsset> assets = dao.findAllByQuery(q);

        int processed = 0;
        for (ErpAstAsset asset : assets) {
            try {
                executeDepreciationProcessor.executeDepreciation(asset.getId(), period, context);
                processed++;
            } catch (Exception e) {
                LOG.warn("批量折旧：资产 {} 期间 {} 计提失败，跳过：{}", asset.getCode(), period, e.getMessage());
            }
        }
        return processed;
    }
}
