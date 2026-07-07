package app.erp.fin.service.posting;

import io.nop.core.context.IServiceContext;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 凭证红冲监听者注册中心。启动期收集所有 {@link IErpFinVoucherReversedListener} Bean
 * （**镜像 {@code ErpFinAcctDocRegistry} 收集 {@code IErpFinAcctDocProvider} 的范式**），
 * 提供 {@link #dispatch} 统一派发入口。
 *
 * <p>本类为非 BizModel 服务（无聚合根 xmeta），通过 IoC 注册为 Bean：{@code listeners}
 * 由容器按类型收集后经 setter 注入。
 *
 * <p>失败隔离（设计 {@code posting.md §冲销机制方向二 §实现策略 裁决3}）：{@link #dispatch}
 * 对每个监听者 try/catch 包裹——单个监听者抛 {@code NopException} 不中断其他监听者、不回滚
 * 已过账红字凭证（凭证法律效力）；失败收集后由调用方（{@link ErpFinPostingProcessor}）落入
 * 5.1 异常工作台 PENDING 队列供人工处置。
 */
public class ErpFinReversalListenerRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(ErpFinReversalListenerRegistry.class);

    /** 监听者派发失败记录（落入 5.1 异常工作台的载体）。 */
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

    private List<IErpFinVoucherReversedListener> listeners = Collections.emptyList();

    public void setListeners(List<IErpFinVoucherReversedListener> listeners) {
        this.listeners = listeners == null ? Collections.emptyList() : listeners;
    }

    /** 测试/编程式注册用。生产经 IoC {@code @Inject List} 收集。 */
    public void addListener(IErpFinVoucherReversedListener listener) {
        List<IErpFinVoucherReversedListener> all = new ArrayList<>(listeners);
        all.add(listener);
        listeners = all;
    }

    @PostConstruct
    public void init() {
        // O-19：启动期校验——若未注册任何红冲监听者，红冲反写闭环（业务单据回退）不可用，属配置缺陷。
        // 单域测试（仅 finance）无监听者实现时不阻断启动（降级为 warn），但生产聚合 app（app-erp-all）
        // 必须注册至少一个监听者——此 warn 在生产日志中会暴露配置遗漏，避免运行期静默跳过监听者派发
        // 导致业务单据状态与凭证不一致。
        if (listeners.isEmpty()) {
            LOG.warn("凭证红冲监听者注册中心启动时未发现任何 IErpFinVoucherReversedListener 实现（errorCode={}）；"
                    + "红冲反写闭环不可用。生产环境（app-erp-all）必须注册至少一个监听者。",
                    ErpFinPostingErrors.ERR_POSTING_NO_LISTENERS_REGISTERED.getErrorCode());
        }
        listeners = Collections.unmodifiableList(listeners);
    }

    public List<IErpFinVoucherReversedListener> getListeners() {
        return listeners;
    }

    /**
     * 同步遍历所有监听者派发红冲事件，对每个监听者 try/catch 包裹实现失败隔离。
     *
     * @param event   红冲事件
     * @param context 服务上下文
     * @return 单个监听者抛错收集到的失败列表（空列表表示全部成功）；调用方据此落 5.1 异常工作台
     */
    public List<ListenerFailure> dispatch(VoucherReversedEvent event, IServiceContext context) {
        if (listeners.isEmpty()) {
            return Collections.emptyList();
        }
        List<ListenerFailure> failures = null;
        for (IErpFinVoucherReversedListener listener : listeners) {
            try {
                listener.onVoucherReversed(event, context);
            } catch (RuntimeException e) {
                if (failures == null) {
                    failures = new ArrayList<>();
                }
                String errorCode = e instanceof io.nop.api.core.exceptions.NopException
                        ? ((io.nop.api.core.exceptions.NopException) e).getErrorCode() : null;
                String errorMsg = e.getMessage();
                String listenerName = listener.getClass().getName();
                failures.add(new ListenerFailure(listenerName, errorCode, errorMsg));
                LOG.warn("凭证红冲监听者回退失败（隔离，不阻断其他监听者/不回滚红字凭证）：traceId={}, listener={}, billHeadCode={}, businessType={}, errorCode={}, errorMsg={}",
                        event.getTraceId(), listenerName, event.getBillHeadCode(),
                        event.getBusinessType(), errorCode, errorMsg);
            }
        }
        return failures == null ? Collections.emptyList() : failures;
    }
}
