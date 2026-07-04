package app.erp.fin.service.posting;

import io.nop.core.context.IServiceContext;

/**
 * 凭证红冲监听者 SPI（业财闭环方向二：财务侧红冲→业务单据回退）。
 *
 * <p>各业务域（purchase/sales/inventory）可实现本接口并注册为 Bean，由
 * {@link ErpFinReversalListenerRegistry} 启动期经 {@code @Inject List} 聚合所有实现
 * （**镜像 {@code ErpFinAcctDocRegistry} 收集 {@code IErpFinAcctDocProvider} 的范式**）。
 * {@link ErpFinPostingProcessor#reverseProcess} 红字凭证过账成功后构造 {@link VoucherReversedEvent}
 * 并按配置派发——默认 SYNC 同事务同步通知；ASYNC 经 {@code txn().afterCommit} post-commit。
 *
 * <p>监听者职责：经 {@code ErpFinVoucherBillR} 反查源单（billType+billCode），按各域状态机
 * 回退自身 {@code posted}+{@code docStatus}（设计 {@code posting.md §冲销机制方向二 §实现策略 裁决4}）。
 *
 * <p>失败隔离：派发循环对每个监听者 try/catch 包裹——单个监听者抛 {@code NopException}
 * 不中断其他监听者、不回滚已过账红字凭证（凭证法律效力）；失败记录落入 5.1 异常工作台
 * （{@code ErpFinPostingException}，postingType=REVERSAL，failedStage=notify-reversal-listener）。
 */
public interface IErpFinVoucherReversedListener {

    /**
     * 凭证红冲事件回调。监听者据此回退自身域源单状态。
     *
     * @param event   红冲事件（含 voucherId/reversalOfVoucherId/billHeadCode/businessType/billType/traceId）
     * @param context 服务上下文（承接调用方上下文，跨域回退保留用户身份/数据权限）
     */
    void onVoucherReversed(VoucherReversedEvent event, IServiceContext context);
}
