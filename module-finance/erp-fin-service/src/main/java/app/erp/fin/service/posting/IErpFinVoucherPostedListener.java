package app.erp.fin.service.posting;

import io.nop.core.context.IServiceContext;

/**
 * F2.1（P1-CK-fin-001）：凭证正向过账成功监听者 SPI（业财闭环方向一补全：财务侧过账成功→业务单据
 * posted 回写）。镜像 {@link IErpFinVoucherReversedListener} 对偶。
 *
 * <p>派发通道（仅「调用方不在场」的两重试通道，引擎 process() 路径不派发——
 * {@code posting.md §反写契约}域自治 + 硬规则 6 原子性）：
 * <ul>
 *   <li>{@code ErpFinDeferredPostingRetryHelper#doRetry}——deferred-posting sweep 批任务</li>
 *   <li>{@code ErpFinPostingExceptionRetryProcessor#retry}——异常工作台手动重试</li>
 * </ul>
 * 场景：REQUIRES_NEW 凭证已提交 + 主事务回滚 → 源单 posted=false + 凭证存在 + PENDING 异常记录 →
 * sweep/手动重试经引擎幂等命中（F1.1 返回既有 id）→ 派发本事件 → 域回写 posted 三字段。
 *
 * <p>监听者职责：按 (businessType, billHeadCode) 定位源单（各域 findByCode/后缀解码），
 * posted=true + postedAt/postedBy；已 true 跳过（防 version 无谓递增）；定位 miss no-op。
 *
 * <p>失败隔离：与反向 registry 相同——单个监听者抛错不中断其他监听者、不回滚 RETRIED；
 * 失败经 {@code ErpFinPostingExceptionRecorder} 落异常工作台（failedStage=notify-posted-listener，
 * eventData 透传形成下轮 sweep 自愈）。
 */
public interface IErpFinVoucherPostedListener {

    /**
     * 凭证过账成功事件回调。监听者据此回写自身域源单 posted 三字段。
     *
     * @param event   过账事件（含 voucherId/billHeadCode/businessType/billType/traceId）
     * @param context 服务上下文（承接重试通道上下文，跨域回写保留用户身份/数据权限）
     */
    void onVoucherPosted(VoucherPostedEvent event, IServiceContext context);
}
