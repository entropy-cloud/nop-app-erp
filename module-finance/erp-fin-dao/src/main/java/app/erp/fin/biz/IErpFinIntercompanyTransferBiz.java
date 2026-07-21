package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;

/**
 * 跨法人内部交易凭证生成跨域 SPI（plan 2026-07-22-1000-1 A3，multi-company.md §跨公司交易生命周期状态机）。
 *
 * <p>严格对齐 multi-company.md §Decision A/B：跨法人调拨 {@code ErpInvTransferOrder.confirm} 后置经本 SPI
 * 触发转移定价解析 + 配对凭证生成；同法人调拨不经本 SPI（inventory 域调用方仅传 fromWarehouseId/toWarehouseId，
 * 跨法人判定 + 定价 + 凭证生成全部在 finance 域）。
 *
 * <p>实现：{@code ErpFinIntercompanyTransferBizModel}（finance-service），config-gated
 * （{@code erp-fin.intercompany-posting-enabled} 默认 false，保护既有 inventory 测试零回归）。
 *
 * <p>事务边界：SYNC 同事务（与 A2 {@code IErpFinBudgetCommitmentBiz} 同范式，避免事务跨域复杂度）。
 *
 * <p>本接口位于 finance-dao（跨层契约面），供 inventory 域注入。
 */
public interface IErpFinIntercompanyTransferBiz {

    /**
     * 跨法人调拨后置触发：识别 from/to 仓库所属法人根，若跨法人则经转移定价规则生成配对内部销售/采购凭证。
     *
     * <p>调用点：{@code ErpInvTransferOrderBizModel.confirm} 后置 hook。
     *
     * @param transferOrderId 调拨单 ID
     * @param fromWarehouseId 调出仓库 ID
     * @param toWarehouseId   调入仓库 ID
     * @param businessDate    业务日期（用于转移定价有效期匹配）
     * @param context         服务上下文
     * @return 配对凭证 ID 列表（AR 凭证 + AP 凭证）；config-gated 关闭或同法人或无定价规则时返回空列表
     */
    @BizMutation
    java.util.List<Long> onTransferConfirmed(@Name("transferOrderId") Long transferOrderId,
                                             @Name("fromWarehouseId") Long fromWarehouseId,
                                             @Name("toWarehouseId") Long toWarehouseId,
                                             @Name("businessDate") java.time.LocalDate businessDate,
                                             IServiceContext context);
}
