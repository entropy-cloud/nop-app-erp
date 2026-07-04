package app.erp.aps.service;

/**
 * APS 域配置键（{@code scheduling.md §十 配置}），经 {@code AppConfig.var(..., defaultValue)} 读取。
 * 默认值与设计文档一致：贪心启发式、capacity=1、MAINTENANCE 单约束。
 */
public interface ErpApsConfigs {

    /** 默认排产模式：FORWARD / BACKWARD，默认 FORWARD。 */
    String CONFIG_DEFAULT_SCHEDULING_MODE = "erp-aps.default-scheduling-mode";
    String DEFAULT_SCHEDULING_MODE = "FORWARD";

    /** 排序规则：PRIORITY / EDD / CR / SPT，默认 PRIORITY。 */
    String CONFIG_PRIORITY_RULE = "erp-aps.priority-rule";
    String DEFAULT_PRIORITY_RULE = "PRIORITY";

    /** 排产时间槽粒度（分钟），默认 15。 */
    String CONFIG_TIME_BUCKET_MINUTES = "erp-aps.time-bucket-minutes";
    int DEFAULT_TIME_BUCKET_MINUTES = 15;

    /** 插单时是否自动区间重排，默认 true。 */
    String CONFIG_AUTO_RESCHEDULE_ON_INSERT = "erp-aps.auto-reschedule-on-insert";
    boolean DEFAULT_AUTO_RESCHEDULE_ON_INSERT = true;

    /** 区间重排最大展望期（天），默认 30。 */
    String CONFIG_MAX_RESCHEDULE_WINDOW_DAYS = "erp-aps.max-reschedule-window-days";
    int DEFAULT_MAX_RESCHEDULE_WINDOW_DAYS = 30;

    /** 相邻工序间缓冲时间（分钟），默认 5。 */
    String CONFIG_BUFFER_MINUTES_BETWEEN_OPS = "erp-aps.buffer-minutes-between-ops";
    int DEFAULT_BUFFER_MINUTES_BETWEEN_OPS = 5;
}
