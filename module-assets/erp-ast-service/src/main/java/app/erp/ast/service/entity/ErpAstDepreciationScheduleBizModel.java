
package app.erp.ast.service.entity;

import app.erp.ast.biz.IErpAstDepreciationScheduleBiz;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.processor.ErpAstDepreciationScheduleProcessor;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 折旧计划 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 单资产/批量折旧计提 + 反折旧 + DEPRECIATION 业财过账编排委托
 * {@link ErpAstDepreciationScheduleProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义见 {@code depreciation-and-posting.md} §1/§5；{@code @BizMutation}+{@code @SingleSession} 钉事务/会话边界。
 */
@BizModel("ErpAstDepreciationSchedule")
public class ErpAstDepreciationScheduleBizModel extends CrudBizModel<ErpAstDepreciationSchedule>
        implements IErpAstDepreciationScheduleBiz {

    @Inject
    ErpAstDepreciationScheduleProcessor depreciationProcessor;

    public ErpAstDepreciationScheduleBizModel() {
        setEntityName(ErpAstDepreciationSchedule.class.getName());
    }

    @BizLoader(forType = ErpAstDepreciationSchedule.class)
    public List<String> assetCode(@ContextSource List<ErpAstDepreciationSchedule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("asset"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstDepreciationSchedule row : rows) {
            result.add(row.getAsset() != null ? row.getAsset().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstDepreciationSchedule.class)
    public List<String> orgName(@ContextSource List<ErpAstDepreciationSchedule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstDepreciationSchedule row : rows) {
            result.add(row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpAstDepreciationSchedule.class)
    public List<String> currencyName(@ContextSource List<ErpAstDepreciationSchedule> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpAstDepreciationSchedule row : rows) {
            result.add(row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDepreciationSchedule executeDepreciation(@Name("assetId") Long assetId,
                                                           @Name("period") String period,
                                                           IServiceContext context) {
        return depreciationProcessor.executeDepreciation(assetId, period, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public int executeBatchDepreciation(@Name("period") String period, IServiceContext context) {
        return depreciationProcessor.executeBatchDepreciation(period, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpAstDepreciationSchedule reverseDepreciation(@Name("assetId") Long assetId,
                                                           @Name("period") String period,
                                                           IServiceContext context) {
        return depreciationProcessor.reverseDepreciation(assetId, period, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public int recalculateForCapitalizationMaintenance(@Name("assetId") Long assetId,
                                                       @Name("increment") BigDecimal increment,
                                                       IServiceContext context) {
        return depreciationProcessor.recalculateForCapitalizationMaintenance(assetId, increment, context);
    }
}
