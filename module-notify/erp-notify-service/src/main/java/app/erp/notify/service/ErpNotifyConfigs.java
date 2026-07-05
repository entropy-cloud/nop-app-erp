package app.erp.notify.service;

/**
 * 通知派发子系统配置项。命名空间 {@code erp-notify.*}。
 *
 * <p>所有配置项经 {@code AppConfig.var(..., defaultValue)} 读取。bootstrap 默认仅站内消息通道；
 * 邮件/短信经配置启用，无真实供应商时跳过并 WARN（不阻断业务）。
 *
 * <p>权威：`docs/architecture/notification-strategy.md`、
 * `docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md`。
 */
public interface ErpNotifyConfigs {

    // 通知总开关（默认 true）
    String CONFIG_NOTIFY_ENABLED = "erp-notify.enabled";
    boolean DEFAULT_NOTIFY_ENABLED = true;

    // 邮件通道是否启用（默认 false，无供应商时不调用 IEmailSender）
    String CONFIG_NOTIFY_EMAIL_ENABLED = "erp-notify.email-enabled";
    boolean DEFAULT_NOTIFY_EMAIL_ENABLED = false;

    // 短信通道是否启用（默认 false，无供应商时不调用 ISmsSender）
    String CONFIG_NOTIFY_SMS_ENABLED = "erp-notify.sms-enabled";
    boolean DEFAULT_NOTIFY_SMS_ENABLED = false;

    // 频控合并是否启用（默认 true）
    String CONFIG_NOTIFY_MERGE_ENABLED = "erp-notify.merge-enabled";
    boolean DEFAULT_NOTIFY_MERGE_ENABLED = true;
}
