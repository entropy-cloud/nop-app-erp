
package app.erp.ast.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.ast.dao.entity.ErpAstAsset;

public interface IErpAstAssetBiz extends ICrudBiz<ErpAstAsset>{

    /**
     * 资产闲置（RC-R1.54，L1 UC-AST-03 ①）：IN_SERVICE→IDLE，闲置期间停提折旧。
     * 暂停时点经 remark「闲置自 {date}」强制记录（闲置时长派生的时间基准，idleSince 列不落 ORM）。
     * 非法来源态拒绝（须 IN_SERVICE）。
     */
    @BizMutation
    ErpAstAsset suspend(@Name("assetId") String assetId, IServiceContext context);

    /**
     * 资产恢复使用（RC-R1.54，L1 UC-AST-03 ②）：IDLE→IN_SERVICE，恢复计提折旧
     * （PENDING 计划保留，后续执行自然恢复——Phase 3 Decision A）。
     * 非法来源态拒绝（须 IDLE）。
     */
    @BizMutation
    ErpAstAsset resume(@Name("assetId") String assetId, IServiceContext context);

}
