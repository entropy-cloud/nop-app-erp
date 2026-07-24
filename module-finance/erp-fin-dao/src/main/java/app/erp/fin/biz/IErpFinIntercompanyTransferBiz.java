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

    /**
     * 跨公司贸易单据（采购订单/销售订单）approve 后置触发：识别执行组织所属法人根 + 经转移定价规则表反向查找对手方法人根，
     * 若跨法人则以订单金额生成配对内部销售/采购凭证（plan 2026-07-24-1351-2，multi-company.md §跨公司 PO/SO 触发路径）。
     *
     * <p>跨法人判定全在 finance 域（AP-7 合规）：执行方 = {@code resolveLegalEntityRoot(executingOrgId)}；
     * 对手方 = 转移定价规则表反向查找（PO 查 toOrgId=执行方取 fromOrgId；SO 查 fromOrgId=执行方取 toOrgId）。
     * 同法人 / config-gate 关闭 / 无定价规则 → 返回空列表（既有行为完全不变）。
     *
     * @param docType         单据类型（{@code ErpFinConstants.INTERCOMPANY_DOC_TYPE_PURCHASE_ORDER} / {@code ..._SALES_ORDER}）
     * @param docId           单据 ID（审计/追踪）
     * @param docCode         单据编码（业财回链 billCode，红冲按此反查）
     * @param executingOrgId  执行组织 ID（订单头 orgId）
     * @param amount          交易金额（本位币，订单 totalAmountWithTax）
     * @param businessDate    业务日期（用于定价规则有效期匹配）
     * @param context         服务上下文
     * @return 配对凭证 ID 列表（AR 凭证 + AP 凭证）；config-gated 关闭或同法人或无定价规则时返回空列表
     */
    @BizMutation
    java.util.List<Long> onTradeDocumentApproved(@Name("docType") String docType,
                                                  @Name("docId") Long docId,
                                                  @Name("docCode") String docCode,
                                                  @Name("executingOrgId") Long executingOrgId,
                                                  @Name("amount") java.math.BigDecimal amount,
                                                  @Name("businessDate") java.time.LocalDate businessDate,
                                                  IServiceContext context);

    /**
     * 跨公司贸易单据 reverseApprove 前置触发：按 {@code docCode} 反查 approve 时生成的配对 intercompany 凭证，
     * 逐张生成红字冲销凭证（借贷互换、{@code isReversed=true}、{@code reversalOfVoucherId} 回链原凭证）。
     *
     * <p>config-gated 关闭或无原凭证时返回空列表（容错路径，不阻塞业务流，对齐 commitment release 范式）。
     *
     * @param docType  单据类型（审计用）
     * @param docId    单据 ID（审计/追踪）
     * @param docCode  单据编码（业财回链 billCode，按此反查原配对凭证）
     * @param context  服务上下文
     * @return 红冲凭证 ID 列表（空列表表示无原凭证可红冲）
     */
    @BizMutation
    java.util.List<Long> onTradeDocumentReversed(@Name("docType") String docType,
                                                  @Name("docId") Long docId,
                                                  @Name("docCode") String docCode,
                                                  IServiceContext context);
}
