package app.erp.notify.service.dispatch;

import io.nop.integration.api.sms.ISmsSender;
import io.nop.integration.api.sms.SmsMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 短信发送 no-op 默认实现。bootstrap 无真实短信供应商时作为 {@link ISmsSender} 默认 bean 注入，
 * 满足 IoC 依赖解析；不实际发送，仅记录 DEBUG 日志。生产部署注册真实实现（如 TencentSmsSender）时覆盖此默认。
 *
 * <p>config-gated：{@code erp-notify.sms-enabled=false}（默认）时派发引擎不调用本实现。
 */
public class NoopSmsSender implements ISmsSender {
    private static final Logger LOG = LoggerFactory.getLogger(NoopSmsSender.class);

    @Override
    public void sendMessage(SmsMessage message) {
        LOG.debug("notify.noop-sms-sender: 跳过实际发送 mobile={}", message.getMobile());
    }

    @Override
    public void sendMultiMessage(List<SmsMessage> messages) {
        for (SmsMessage message : messages) {
            sendMessage(message);
        }
    }
}
