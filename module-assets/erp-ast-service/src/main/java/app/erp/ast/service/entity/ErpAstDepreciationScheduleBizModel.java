
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.processor.ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor;
import app.erp.ast.service.processor.ErpAstDepreciationScheduleExecuteDepreciationProcessor;
import app.erp.ast.service.processor.ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor;
import app.erp.ast.service.processor.ErpAstDepreciationScheduleReverseDepreciationProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 折旧计划 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 单资产/批量折旧计提 + 反折旧 + 资本化维修折旧重算编排委托对应 per-mutation Processor（R6.3 拆分，
 * protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义见 {@code depreciation-and-posting.md} §1/§5；{@code @BizMutation} 钉事务/会话边界。
 */
@BizModel("ErpAstDepreciationSchedule")
public class ErpAstDepreciationScheduleBizModel extends CrudBizModel<ErpAstDepreciationSchedule>
        implements IErpAstDepreciationScheduleBiz {

    @Inject
    ErpAstDepreciationScheduleExecuteDepreciationProcessor executeDepreciationProcessor;

    @Inject
    ErpAstDepreciationScheduleExecuteBatchDepreciationProcessor executeBatchDepreciationProcessor;

    @Inject
    ErpAstDepreciationScheduleReverseDepreciationProcessor reverseDepreciationProcessor;

    @Inject
    ErpAstDepreciationScheduleRecalculateForCapitalizationMaintenanceProcessor recalculateProcessor;

    public ErpAstDepreciationScheduleBizModel() {
        setEntityName(ErpAstDepreciationSchedule.class.getName());
    }

    @Override
    @BizMutation
    public ErpAstDepreciationSchedule executeDepreciation(@Name("assetId") Long assetId,
                                                           @Name("period") String period,
                                                           IServiceContext context) {
        return executeDepreciationProcessor.executeDepreciation(assetId, period, context);
    }

    @Override
    @BizMutation
    public int executeBatchDepreciation(@Name("period") String period, IServiceContext context) {
        return executeBatchDepreciationProcessor.executeBatchDepreciation(period, context);
    }

    @Override
    @BizMutation
    public ErpAstDepreciationSchedule reverseDepreciation(@Name("assetId") Long assetId,
                                                           @Name("period") String period,
                                                           IServiceContext context) {
        return reverseDepreciationProcessor.reverseDepreciation(assetId, period, context);
    }

    @Override
    @BizMutation
    public int recalculateForCapitalizationMaintenance(@Name("assetId") Long assetId,
                                                       @Name("increment") BigDecimal increment,
                                                       IServiceContext context) {
        return recalculateProcessor.recalculateForCapitalizationMaintenance(assetId, increment, context);
    }
}
