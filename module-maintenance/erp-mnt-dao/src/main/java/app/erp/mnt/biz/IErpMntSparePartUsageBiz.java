package app.erp.mnt.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.mnt.dao.entity.ErpMntSparePartUsage;

/**
 * 备件消耗单业务接口。除标准 CRUD 外，定义确认出库状态机：
 * confirm 推进头 docStatus(DRAFT→ACTIVE)/approveStatus 后按行调 {@code IErpInvStockMoveBiz.generateMove}
 * 出库（relatedBillType 非空自动 DONE 扣余额）+ posted=true + totalAmount 聚合。
 */
public interface IErpMntSparePartUsageBiz extends ICrudBiz<ErpMntSparePartUsage> {

    @BizMutation
    ErpMntSparePartUsage confirm(@Name("usageId") Long usageId, IServiceContext context);
}
