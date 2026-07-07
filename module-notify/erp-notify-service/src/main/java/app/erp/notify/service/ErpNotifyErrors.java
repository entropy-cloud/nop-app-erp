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
}
