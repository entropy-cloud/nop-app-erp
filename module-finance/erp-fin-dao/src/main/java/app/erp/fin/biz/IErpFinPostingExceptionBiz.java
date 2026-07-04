
package app.erp.fin.biz;

import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import app.erp.fin.dao.ErpFinPostingMetricsSnapshot;
import app.erp.fin.dao.entity.ErpFinPostingException;

/**
 * 过账异常工作台契约（{@code posting-log.md §过账异常处置}）。CRUD 之外承载三个处置动作：
 * <ul>
 *   <li>{@link #retry(Long, IServiceContext)} —— 重试：重新触发过账，成功翻 RETRIED。</li>
 *   <li>{@link #ignore(Long, String, IServiceContext)} —— 忽略：标记 IGNORED + 原因必填。</li>
 *   <li>{@link #manualEntry(Long, Long, String, IServiceContext)} —— 手工补录：关联源单已过账凭证。</li>
 * </ul>
 *
 * <p>处置状态机经 ErrorCode 守门（仅 PENDING 可处置；忽略须原因；补录须关联凭证）。
 * 期末结账前置检查扫描未处置（PENDING/RETRYING）记录阻止结账（见 {@code IErpFinAccountingPeriodBiz.preCheck}）。
 *
 * <p>运行监控指标查询（{@code posting-log.md §裁决3}）：{@link #getRuntimeMetrics} 聚合四指标
 * （自动化记账率/时延 P99/异常率/闭环成功率）经 SQL + 进程内时延采样呈现，阈值 config-gated。
 */
public interface IErpFinPostingExceptionBiz extends ICrudBiz<ErpFinPostingException> {

    /**
     * 重试过账。将状态翻为 RETRYING 后重新触发对应业务事件的过账；成功后翻 RETRIED 并关联新生成的凭证。
     *
     * @return 处置后的异常记录（成功为 RETRIED，失败为 RETRYING 并记 retryCount）
     */
    @BizMutation
    ErpFinPostingException retry(@Name("exceptionId") Long exceptionId, IServiceContext context);

    /**
     * 忽略异常。标记 IGNORED，须填写处置说明。
     */
    @BizMutation
    ErpFinPostingException ignore(@Name("exceptionId") Long exceptionId,
                                  @Name("resolutionNote") String resolutionNote,
                                  IServiceContext context);

    /**
     * 手工补录。关联源单已过账凭证（voucherId 必填），标记 MANUAL。
     */
    @BizMutation
    ErpFinPostingException manualEntry(@Name("exceptionId") Long exceptionId,
                                       @Name("voucherId") Long voucherId,
                                       @Name("resolutionNote") String resolutionNote,
                                       IServiceContext context);

    /**
     * 查询未处置（PENDING/RETRYING）异常记录数量。供期末结账前置检查调用。
     */
    @BizQuery
    long countUnresolved(IServiceContext context);

    /**
     * 查询过账运行监控四指标快照（{@code posting-log.md §运行监控指标}）。
     *
     * <p>聚合窗口由 {@code erp-fin.metric.window-hours} 配置（默认 24h）。四指标：
     * 自动化记账率（≥95% 达标）、凭证生成时延 P99（&lt;30s 达标，进程内窗口采样）、
     * 过账异常率（&lt;1% 达标）、业财闭环成功率（≥99.5% 达标，SYNC 默认下为代理值）。
     *
     * @param windowHours 聚合窗口（小时）；null 则用配置默认值
     */
    @BizQuery
    ErpFinPostingMetricsSnapshot getRuntimeMetrics(@Name("windowHours") Integer windowHours,
                                                    IServiceContext context);
}

