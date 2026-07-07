package app.erp.notify.service;

import io.nop.api.core.exceptions.ErrorCode;

/**
 * 通知派发子系统错误码。描述为中文，框架经 i18n 翻译；公共/GraphQL 面向错误统一 {@link ErrorCode} + {@code NopException}。
 *
 * <p>权威：`docs/architecture/notification-strategy.md`、
 * `docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md`。
 */
public interface ErpNotifyErrors {

    String ARG_NOTIFICATION_TYPE = "notificationType";
    String ARG_TEMPLATE_ID = "templateId";
    String ARG_RECIPIENT_USER_ID = "recipientUserId";
    String ARG_CHANNEL = "channel";
    String ARG_RESOLVER = "resolver";
    String ARG_REASON = "reason";
    // O-11 扩展参数键
    String ARG_NOTIFICATION_ID = "notificationId";
    String ARG_SUBSCRIBER_ID = "subscriberId";
    String ARG_EVENT_NAME = "eventName";
    String ARG_PROVIDER = "provider";
    String ARG_LOCALE = "locale";
    String ARG_RETRY_COUNT = "retryCount";

    ErrorCode ERR_NOTIFY_TEMPLATE_NOT_ACTIVE = ErrorCode.define(
            "erp.err.notify.template.not-active",
            "通知类型[{notificationType}]无启用(ACTIVE)模板，config-gated 静默跳过",
            ARG_NOTIFICATION_TYPE);

    ErrorCode ERR_NOTIFY_RECIPIENT_RESOLVE_FAILED = ErrorCode.define(
            "erp.err.notify.recipient.resolve-failed",
            "通知类型[{notificationType}]接收人解析失败：解析器[{resolver}]，原因[{reason}]",
            ARG_NOTIFICATION_TYPE, ARG_RESOLVER, ARG_REASON);

    ErrorCode ERR_NOTIFY_CHANNEL_DISABLED = ErrorCode.define(
            "erp.err.notify.channel.disabled",
            "通知渠道[{channel}]未启用(erp-notify.{channel}-enabled=false)，跳过派发",
            ARG_CHANNEL);

    ErrorCode ERR_NOTIFY_RENDER_FAILED = ErrorCode.define(
            "erp.err.notify.render.failed",
            "通知模板[{templateId}]渲染失败：原因[{reason}]",
            ARG_TEMPLATE_ID, ARG_REASON);

    // ---------- O-11 扩展：通知派发/订阅/跨域通知细粒度错误码 ----------

    ErrorCode ERR_NOTIFY_TEMPLATE_NOT_FOUND = ErrorCode.define(
            "erp.err.notify.template.not-found",
            "通知模板[{templateId}]不存在", ARG_TEMPLATE_ID);

    ErrorCode ERR_NOTIFY_TEMPLATE_DUPLICATE_TYPE = ErrorCode.define(
            "erp.err.notify.template.duplicate-type",
            "通知类型[{notificationType}]已存在启用模板，不允许重复注册 ACTIVE 模板",
            ARG_NOTIFICATION_TYPE);

    ErrorCode ERR_NOTIFY_SUBSCRIPTION_NOT_FOUND = ErrorCode.define(
            "erp.err.notify.subscription.not-found",
            "订阅[{subscriberId}]不存在", ARG_SUBSCRIBER_ID);

    ErrorCode ERR_NOTIFY_SUBSCRIPTION_DUPLICATE = ErrorCode.define(
            "erp.err.notify.subscription.duplicate",
            "用户[{recipientUserId}]已订阅事件[{eventName}]，不允许重复订阅",
            ARG_RECIPIENT_USER_ID, ARG_EVENT_NAME);

    ErrorCode ERR_NOTIFY_INSTANCE_NOT_FOUND = ErrorCode.define(
            "erp.err.notify.instance.not-found",
            "通知实例[{notificationId}]不存在", ARG_NOTIFICATION_ID);

    ErrorCode ERR_NOTIFY_CHANNEL_PROVIDER_FAILED = ErrorCode.define(
            "erp.err.notify.channel.provider-failed",
            "通知渠道[{channel}]外部 Provider[{provider}]调用失败：原因[{reason}]",
            ARG_CHANNEL, ARG_PROVIDER, ARG_REASON);

    ErrorCode ERR_NOTIFY_LOCALE_NOT_SUPPORTED = ErrorCode.define(
            "erp.err.notify.locale.not-supported",
            "通知模板[{templateId}]不支持语言[{locale}]",
            ARG_TEMPLATE_ID, ARG_LOCALE);

    ErrorCode ERR_NOTIFY_DISPATCH_RETRY_EXHAUSTED = ErrorCode.define(
            "erp.err.notify.dispatch.retry-exhausted",
            "通知类型[{notificationType}]派发重试 {retryCount} 次后仍失败：原因[{reason}]",
            ARG_NOTIFICATION_TYPE, ARG_RETRY_COUNT, ARG_REASON);

    ErrorCode ERR_NOTIFY_RECIPIENT_EMPTY = ErrorCode.define(
            "erp.err.notify.recipient.empty",
            "通知类型[{notificationType}]解析后接收人为空，config-gated 静默跳过",
            ARG_NOTIFICATION_TYPE);

    ErrorCode ERR_NOTIFY_EVENT_TYPE_INVALID = ErrorCode.define(
            "erp.err.notify.event-type.invalid",
            "通知事件类型[{eventName}]格式非法（含路径注入字符或不合规段）",
            ARG_EVENT_NAME);
}
