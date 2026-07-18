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
 *
 * <p>红冲入口 {@link #reverseConfirm(Long, IServiceContext)}：已过账（posted=true + ACTIVE）的备件消耗单可红冲，
 * 红冲 MAINTENANCE_ISSUE 凭证 + 反向 OUTGOING 移动单 + posted=false + docStatus=CANCELLED
 *（plan 2026-07-18-1745-1）。
 */
public interface IErpMntSparePartUsageBiz extends ICrudBiz<ErpMntSparePartUsage> {

    @BizMutation
    ErpMntSparePartUsage confirm(@Name("usageId") Long usageId, IServiceContext context);

    /**
     * 红冲已确认出库的备件消耗单：红冲 MAINTENANCE_ISSUE 凭证 + 反向 OUTGOING 移动单（库存域
     * {@code IErpInvStockMoveBiz.reverse} 生成 REVERSAL 反向移动单，余额自动回滚）+ posted=false + docStatus=CANCELLED。
     *
     * <p>守卫：未过账（posted=false）抛 {@code ERR_SPARE_PART_USAGE_NOT_POSTED}。
     */
    @BizMutation
    ErpMntSparePartUsage reverseConfirm(@Name("usageId") Long usageId, IServiceContext context);
}
