package app.erp.notify.service.dispatch;

import io.nop.integration.api.email.EmailMessage;
import io.nop.integration.api.email.IEmailSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * 邮件发送 no-op 默认实现。bootstrap 无真实 SMTP 供应商时作为 {@link IEmailSender} 默认 bean 注入，
 * 满足 IoC 依赖解析；不实际发送，仅记录 DEBUG 日志。生产部署注册真实实现（如 JavaEmailSender）时覆盖此默认。
 *
 * <p>config-gated：{@code erp-notify.email-enabled=false}（默认）时派发引擎不调用本实现。
 */
public class NoopEmailSender implements IEmailSender {
    private static final Logger LOG = LoggerFactory.getLogger(NoopEmailSender.class);

    @Override
    public void sendEmail(EmailMessage mail) {
        LOG.debug("notify.noop-email-sender: 跳过实际发送 to={}", mail.getTo());
    }

    @Override
    public void sendMultiEmail(List<EmailMessage> mails) {
        for (EmailMessage mail : mails) {
            sendEmail(mail);
        }
    }
}
