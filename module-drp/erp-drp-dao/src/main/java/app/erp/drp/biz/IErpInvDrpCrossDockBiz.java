package app.erp.drp.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.drp.dao.entity.ErpInvDrpCrossDock;

import java.util.List;

/**
 * 越库执行记录 BizModel 契约（RC-R1.81 / P1-RC-081，UC-DRP-07）。
 *
 * <p>状态机 mutation 族（{@code docs/design/drp/cross-dock.md §越库状态机}）：
 * {@code receiveMark}（收货识别→STAGING）/ {@code match}（三策略→MATCHED，支持直连 PENDING→MATCHED）/
 * {@code load}（生成出站移动→LOADED）/ {@code complete}（出库确认→COMPLETED）/ {@code cancel}（→CANCELLED）。
 *
 * <p>{@link #markReceivedFromPurchase} 为 purchase 域收货审批后置 Facade（D1 裁决选项 A，
 * 镜像 RC-R1.61 collectProjectMaterialCost 接线方向；pur-service → drp-dao Java 层边，
 * 见 data-dependency-matrix §2.4）。
 */
public interface IErpInvDrpCrossDockBiz extends ICrudBiz<ErpInvDrpCrossDock> {

    @BizMutation
    ErpInvDrpCrossDock receiveMark(@Name("id") Long id, @Name("inboundMoveId") Long inboundMoveId,
                                   IServiceContext context);

    @BizMutation
    ErpInvDrpCrossDock match(@Name("id") Long id, @Optional @Name("targetBillType") String targetBillType,
                             @Optional @Name("targetBillCode") String targetBillCode, IServiceContext context);

    @BizMutation
    ErpInvDrpCrossDock load(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpInvDrpCrossDock complete(@Name("id") Long id, IServiceContext context);

    @BizMutation
    ErpInvDrpCrossDock cancel(@Name("id") Long id, IServiceContext context);

    /**
     * purchase 收货审批后置：按采购单号 + 收货行物料标记 PENDING 越库记录为 STAGING 并回写 inboundMoveId。
     * config {@code erp-inv.drp-xdock-enabled} 关闭时返回 0（零副作用）；同记录重复调用幂等（仅 PENDING 可迁移）。
     *
     * @return 实际标记为 STAGING 的记录数
     */
    @BizMutation
    int markReceivedFromPurchase(@Name("purchaseOrderCode") String purchaseOrderCode,
                                 @Name("inboundMoveId") Long inboundMoveId,
                                 @Name("materialIds") List<Long> materialIds, IServiceContext context);
}
