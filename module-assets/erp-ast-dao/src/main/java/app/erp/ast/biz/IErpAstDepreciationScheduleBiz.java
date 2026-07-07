
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;

public interface IErpAstDepreciationScheduleBiz extends ICrudBiz<ErpAstDepreciationSchedule> {

    /**
     * 单资产按期间计提折旧：校验期间未结账 + 资产使用中，按折旧方法计算本期折旧（残值约束），
     * 更新计划条目与资产卡片汇总列，触发 DEPRECIATION(70) 业财过账。同期间重复执行先红冲再重生成（幂等）。
     */
    @BizMutation
    ErpAstDepreciationSchedule executeDepreciation(@Name("assetId") Long assetId, @Name("period") String period,
                                                   IServiceContext context);

    /**
     * 批量折旧：对目标期间所有使用中资产逐个计提折旧（单资产失败错误隔离，不影响他资产，§5.3）。
     * 期末结账（1000-3）经 I*Biz 跨模块调用——声明于 dao 层 IBiz，重新 codegen 后 Api 契约传播。
     *
     * @return 成功计提的资产数量
     */
    @BizMutation
    int executeBatchDepreciation(@Name("period") String period, IServiceContext context);

    /**
     * 红字冲销指定资产期间的已执行折旧：冲销 DEPRECIATION 凭证 + 回滚资产卡片累计折旧/净值 +
     * 计划条目置 REVERSED。供反审核/调整场景调用。
     */
    @BizMutation
    ErpAstDepreciationSchedule reverseDepreciation(@Name("assetId") Long assetId, @Name("period") String period,
                                                   IServiceContext context);

    /**
     * 资本化维修折旧计划重算（加性扩展，非破坏性）。删除未执行（PENDING）折旧计划条目，
     * 按剩余使用年限重新摊销（原值+增量 − 已计提累计折旧 − 残值）/ 剩余月数，残值约束保留。
     * 供资产维修资本化路径（UC-AST-10）调用。config-gated {@code erp-ast.maintenance-cap-adjust-depreciation-base}。
     *
     * @param assetId  资产卡片 ID
     * @param increment 资本化增量金额（资产原值已 += increment）
     */
    @BizMutation
    int recalculateForCapitalizationMaintenance(@Name("assetId") Long assetId,
                                                @Name("increment") java.math.BigDecimal increment,
                                                IServiceContext context);
}
