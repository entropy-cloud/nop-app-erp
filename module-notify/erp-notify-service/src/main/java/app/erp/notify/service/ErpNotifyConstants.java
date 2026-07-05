package app.erp.notify.service;

/**
 * 通知派发子系统常量。字典码值权威：`erp-notify-meta/.../dict/*.dict.yaml` + `module-notify/model/app-erp-notify.orm.xml`。
 *
 * <p>权威：`docs/architecture/notification-strategy.md`、
 * `docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md`。
 */
public interface ErpNotifyConstants {

    // 通知渠道（erp-notify/notification-channel）
    String CHANNEL_IN_APP = "IN_APP";
    String CHANNEL_EMAIL = "EMAIL";
    String CHANNEL_SMS = "SMS";

    // 通知实例状态（erp-notify/notification-status）
    String STATUS_PENDING = "PENDING";
    String STATUS_SENT = "SENT";
    String STATUS_MERGED = "MERGED";
    String STATUS_FAILED = "FAILED";

    // 接收人解析器（erp-notify/recipient-resolver）
    String RESOLVER_ROLE = "ROLE";
    String RESOLVER_ORG = "ORG";
    String RESOLVER_PARTNER = "PARTNER";
    String RESOLVER_USER_LIST = "USER_LIST";

    // 频控合并策略（erp-notify/merge-strategy）
    String MERGE_NONE = "NONE";
    String MERGE_BY_USER_TYPE = "MERGE_BY_USER_TYPE";

    // 模板状态（erp-notify/template-status）
    String TEMPLATE_DRAFT = "DRAFT";
    String TEMPLATE_ACTIVE = "ACTIVE";

    // 合并组 key 分隔符（notificationType + "#" + recipientUserId）
    String MERGE_GROUP_SEP = "#";

    // 默认合并窗口（秒）：业务提醒 5 分钟
    int DEFAULT_BUSINESS_REMIND_WINDOW_SECONDS = 300;
    // 异常告警 1 分钟
    int DEFAULT_ALERT_WINDOW_SECONDS = 60;
}
