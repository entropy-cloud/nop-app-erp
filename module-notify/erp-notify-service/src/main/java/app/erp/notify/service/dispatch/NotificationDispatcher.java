package app.erp.notify.service.dispatch;

import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConfigs;
import app.erp.notify.service.ErpNotifyConstants;
import app.erp.notify.service.ErpNotifyErrors;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.commons.util.StringHelper;
import io.nop.core.lang.json.JsonTool;
import io.nop.dao.api.IDaoProvider;
import io.nop.integration.api.email.EmailMessage;
import io.nop.integration.api.email.IEmailSender;
import io.nop.integration.api.sms.ISmsSender;
import io.nop.integration.api.sms.SmsMessage;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通知派发引擎。职责：模板渲染 + 频控合并 + 站内落库 + 经 nop-integration 派发邮件/短信（config-gated）。
 *
 * <p>平台默认路线对齐：外发走 {@code nop-integration}（{@link IEmailSender}/{@link ISmsSender}），
 * 站内消息为本子系统。通知失败不回滚业务事实（{@code reporting-and-notification-integration.md} 默认无影响）。
 *
 * <p>模板渲染：支持 {@code ${varName}} 占位符插值（从 context 取值），无占位符则原样返回。
 * 与平台 XLang 表达式语义兼容（TemplateStringExpression 风格），复杂表达式求值归后继。
 */
public class NotificationDispatcher {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationDispatcher.class);
    private static final Pattern VAR_PATTERN = Pattern.compile("\\$\\{([a-zA-Z_][a-zA-Z0-9_.]*)\\}");

    private final NotificationRecipientResolver recipientResolver;
    private final NotificationMergeCoordinator mergeCoordinator;

    @Inject
    IEmailSender emailSender;
    @Inject
    ISmsSender smsSender;
    @Inject
    IDaoProvider daoProvider;

    public NotificationDispatcher(NotificationRecipientResolver recipientResolver,
                                  NotificationMergeCoordinator mergeCoordinator) {
        this.recipientResolver = recipientResolver;
        this.mergeCoordinator = mergeCoordinator;
    }

    public void setEmailSender(IEmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setSmsSender(ISmsSender smsSender) {
        this.smsSender = smsSender;
    }

    /**
     * 执行派发：渲染模板、解析接收人、频控合并、落站内消息、按 channelSet config-gated 派发外发通道。
     *
     * @param template 已解析的 ACTIVE 模板
     * @param context  业务上下文（模板求值用）
     * @return 派发结果通知实例集合（站内消息；合并命中时为既有实例）
     */
    public List<ErpSysNotification> dispatch(ErpSysNotificationTemplate template, Map<String, Object> context) {
        String subject = renderTemplate(template.getId(), template.getSubjectTpl(), context);
        String body = renderTemplate(template.getId(), template.getBodyTpl(), context);

        Set<String> recipients = recipientResolver.resolve(template, context);
        if (recipients.isEmpty()) {
            LOG.warn("notify.dispatch: 模板[{}] notificationType={} 解析接收人为空，config-gated 跳过",
                    template.getId(), template.getNotificationType());
            return Collections.emptyList();
        }

        Set<String> channels = parseChannelSet(template.getChannelSet());

        List<ErpSysNotification> notifications = new ArrayList<>();
        for (String userId : recipients) {
            ErpSysNotification n = mergeOrPersist(template, userId, subject, body, context, channels);
            notifications.add(n);
        }

        for (ErpSysNotification n : notifications) {
            dispatchExternalChannels(n, channels);
        }
        return notifications;
    }

    private Set<String> parseChannelSet(String channelSet) {
        if (StringHelper.isBlank(channelSet)) {
            return new LinkedHashSet<>(List.of(ErpNotifyConstants.CHANNEL_IN_APP));
        }
        Set<String> ret = new LinkedHashSet<>();
        for (String c : Arrays.asList(channelSet.split(","))) {
            String trim = c.trim();
            if (!trim.isEmpty()) ret.add(trim);
        }
        if (ret.isEmpty()) ret.add(ErpNotifyConstants.CHANNEL_IN_APP);
        return ret;
    }

    private ErpSysNotification mergeOrPersist(ErpSysNotificationTemplate template, String userId,
                                              String subject, String body, Map<String, Object> context,
                                              Set<String> channels) {
        boolean mergeEnabled = AppConfig.var(
                ErpNotifyConfigs.CONFIG_NOTIFY_MERGE_ENABLED, ErpNotifyConfigs.DEFAULT_NOTIFY_MERGE_ENABLED);

        if (mergeEnabled && ErpNotifyConstants.MERGE_BY_USER_TYPE.equals(template.getMergeStrategy())) {
            ErpSysNotification existing = mergeCoordinator.findMergeable(
                    template.getMergeStrategy(),
                    template.getMergeWindowSeconds() == null ? 0 : template.getMergeWindowSeconds(),
                    template.getNotificationType(), userId);
            if (existing != null) {
                int prevCount = existing.getMergeCount() == null ? 1 : existing.getMergeCount();
                String mergedBody = StringHelper.isBlank(body) ? body
                        : body + "\n[合并 +" + prevCount + "]";
                return mergeCoordinator.mergeInto(existing, subject, mergedBody);
            }
        }

        ErpSysNotification n = daoProvider.daoFor(ErpSysNotification.class).newEntity();
        n.setTemplateId(template.getId());
        n.setNotificationType(template.getNotificationType());
        n.setRecipientUserId(userId);
        n.setChannel(channels.contains(ErpNotifyConstants.CHANNEL_IN_APP)
                ? ErpNotifyConstants.CHANNEL_IN_APP : channels.iterator().next());
        n.setSubject(subject);
        n.setBody(body);
        n.setPayloadJson(context == null || context.isEmpty() ? null
                : JsonTool.serialize(new java.util.TreeMap<>(context), false));
        n.setStatus(ErpNotifyConstants.STATUS_SENT);
        n.setMergeGroupId(template.getNotificationType() + ErpNotifyConstants.MERGE_GROUP_SEP + userId);
        n.setMergeCount(1);
        n.setSentAt(CoreMetrics.currentTimestamp());
        return n;
    }

    private void dispatchExternalChannels(ErpSysNotification n, Set<String> channels) {
        if (channels.contains(ErpNotifyConstants.CHANNEL_EMAIL)) {
            if (AppConfig.var(ErpNotifyConfigs.CONFIG_NOTIFY_EMAIL_ENABLED,
                    ErpNotifyConfigs.DEFAULT_NOTIFY_EMAIL_ENABLED)) {
                sendEmailIfPossible(n);
            } else {
                LOG.warn("notify.dispatch: 通知[{}] 渠道 EMAIL 配置未启用(erp-notify.email-enabled=false)，跳过派发",
                        n.getId());
            }
        }
        if (channels.contains(ErpNotifyConstants.CHANNEL_SMS)) {
            if (AppConfig.var(ErpNotifyConfigs.CONFIG_NOTIFY_SMS_ENABLED,
                    ErpNotifyConfigs.DEFAULT_NOTIFY_SMS_ENABLED)) {
                sendSmsIfPossible(n);
            } else {
                LOG.warn("notify.dispatch: 通知[{}] 渠道 SMS 配置未启用(erp-notify.sms-enabled=false)，跳过派发",
                        n.getId());
            }
        }
    }

    private void sendEmailIfPossible(ErpSysNotification n) {
        if (emailSender == null) {
            LOG.warn("notify.dispatch: 通知[{}] 无 IEmailSender 实现，跳过邮件派发", n.getId());
            return;
        }
        try {
            EmailMessage mail = new EmailMessage();
            mail.setSubject(StringHelper.toString(n.getSubject(), ""));
            mail.setText(StringHelper.toString(n.getBody(), ""));
            emailSender.sendEmail(mail);
            LOG.info("notify.dispatch: 通知[{}] 邮件派发成功", n.getId());
        } catch (Exception e) {
            LOG.error("notify.dispatch: 通知[{}] 邮件派发失败（不阻断业务）: {}", n.getId(), e.getMessage(), e);
        }
    }

    private void sendSmsIfPossible(ErpSysNotification n) {
        if (smsSender == null) {
            LOG.warn("notify.dispatch: 通知[{}] 无 ISmsSender 实现，跳过短信派发", n.getId());
            return;
        }
        try {
            SmsMessage sms = new SmsMessage();
            sms.setText(StringHelper.toString(n.getBody(), ""));
            smsSender.sendMessage(sms);
            LOG.info("notify.dispatch: 通知[{}] 短信派发成功", n.getId());
        } catch (Exception e) {
            LOG.error("notify.dispatch: 通知[{}] 短信派发失败（不阻断业务）: {}", n.getId(), e.getMessage(), e);
        }
    }

    /**
     * 模板渲染：{@code ${varName}} 占位符从 context 插值；无占位符原样返回。
     * 渲染失败抛 {@link ErpNotifyErrors#ERR_NOTIFY_RENDER_FAILED}。
     */
    String renderTemplate(Long templateId, String tpl, Map<String, Object> context) {
        if (StringHelper.isBlank(tpl)) {
            return "";
        }
        if (context == null || context.isEmpty() || !tpl.contains("${")) {
            return tpl;
        }
        try {
            Matcher m = VAR_PATTERN.matcher(tpl);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String key = m.group(1);
                Object val = context.containsKey(key) ? context.get(key) : context.get(toCamelKey(key, context));
                String replacement = val == null ? "" : val.toString();
                m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            throw new NopException(ErpNotifyErrors.ERR_NOTIFY_RENDER_FAILED, e)
                    .param(ErpNotifyErrors.ARG_TEMPLATE_ID, templateId)
                    .param(ErpNotifyErrors.ARG_REASON, e.getMessage());
        }
    }

    private String toCamelKey(String dottedOrSnake, Map<String, Object> context) {
        // 兼容 snake_case / dotted key 退化匹配：仅当直接 key 未命中时尝试
        if (dottedOrSnake.contains("_")) {
            return dottedOrSnake.replace("_", "");
        }
        return dottedOrSnake;
    }
}
