package app.erp.fin.service.posting;

import io.nop.core.context.IServiceContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * F2.1（P1-CK-fin-001）：正向过账监听者注册中心。**镜像 {@link ErpFinReversalListenerRegistry} 范式**
 * （IoC collect-beans by-type 聚合全部 {@link IErpFinVoucherPostedListener}），提供 {@link #dispatch}
 * 统一派发入口。
 *
 * <p>派发通道（仅「调用方不在场」的两重试通道，引擎 process() 路径不派发——
 * {@code posting.md §反写契约}域自治 + 硬规则 6 原子性）：
 * <ul>
 *   <li>{@code ErpFinDeferredPostingRetryHelper#doRetry}——deferred-posting sweep 批任务</li>
 *   <li>{@code ErpFinPostingExceptionRetryProcessor#retry}——异常工作台手动重试</li>
 * </ul>
 *
 * <p>失败隔离（镜像 {@link ErpFinReversalListenerRegistry#dispatch} 裁决 3）：{@link #dispatch} 对每个
 * 监听者 try/catch 包裹——单个监听者抛错不中断其他监听者、不回滚 RETRIED（凭证法律效力不回滚）；
 * 失败收集后由调用方经 {@code ErpFinPostingExceptionRecorder} 落入异常工作台 PENDING 队列
 * （eventData 透传，下轮 sweep 幂等命中再派发形成自愈）。
 */
public class ErpFinPostedListenerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinPostedListenerRegistry.class);

    /** 监听者派发失败记录（落入异常工作台的载体；镜像反向 registry 的嵌套类，不共享提取）。 */
    public static final class ListenerFailure {
        private final String listenerName;
        private final String errorCode;
        private final String errorMessage;

        public ListenerFailure(String listenerName, String errorCode, String errorMessage) {
            this.listenerName = listenerName;
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }

        public String getListenerName() {
            return listenerName;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private List<IErpFinVoucherPostedListener> listeners = Collections.emptyList();

    public void setListeners(List<IErpFinVoucherPostedListener> listeners) {
        this.listeners = listeners == null ? Collections.emptyList() : listeners;
    }

    /** 测试/编程式注册用。生产经 IoC collect-beans by-type 收集后 setter 注入。 */
    public void addListener(IErpFinVoucherPostedListener listener) {
        List<IErpFinVoucherPostedListener> all = new ArrayList<>(listeners);
        all.add(listener);
        listeners = all;
    }

    @PostConstruct
    public void init() {
        // O-19 镜像：启动期校验——分期部署下部分域未上线属预期（降级 warn）；全量部署应至少有 FinPostedListener。
        if (listeners.isEmpty()) {
            LOG.warn("no IErpFinVoucherPostedListener implementation found on posted-listener registry startup"
                    + " (expected under staged deployment with some domains not yet live; a full deployment should register at least FinPostedListener)");
        }
        listeners = Collections.unmodifiableList(listeners);
    }

    public List<IErpFinVoucherPostedListener> getListeners() {
        return listeners;
    }

    /**
     * 同步遍历所有监听者派发过账事件，对每个监听者 try/catch 包裹实现失败隔离。
     *
     * @param event   过账事件
     * @param context 服务上下文（承接重试通道上下文，跨域回写保留用户身份/数据权限）
     * @return 单个监听者抛错收集到的失败列表（空列表表示全部成功）；调用方据此落异常工作台（自愈闭环）
     */
    public List<ListenerFailure> dispatch(VoucherPostedEvent event, IServiceContext context) {
        if (listeners.isEmpty()) {
            return Collections.emptyList();
        }
        List<ListenerFailure> failures = null;
        for (IErpFinVoucherPostedListener listener : listeners) {
            try {
                listener.onVoucherPosted(event, context);
            } catch (RuntimeException e) {
                if (failures == null) {
                    failures = new ArrayList<>();
                }
                String errorCode = e instanceof io.nop.api.core.exceptions.NopException
                        ? ((io.nop.api.core.exceptions.NopException) e).getErrorCode() : null;
                String errorMsg = e instanceof io.nop.api.core.exceptions.NopException
                        ? ((io.nop.api.core.exceptions.NopException) e).getDescription() : e.getMessage();
                String listenerName = listener.getClass().getName();
                failures.add(new ListenerFailure(listenerName, errorCode, errorMsg));
                LOG.warn("posted-listener write-back failed (isolated; does not block other listeners or roll back RETRIED): traceId={}, listener={}, billHeadCode={}, businessType={}, errorCode={}, errorMsg={}",
                        event.getTraceId(), listenerName, event.getBillHeadCode(),
                        event.getBusinessType(), errorCode, errorMsg);
            }
        }
        return failures == null ? Collections.emptyList() : failures;
    }
}
