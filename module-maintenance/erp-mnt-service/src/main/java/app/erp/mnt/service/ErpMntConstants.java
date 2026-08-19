package app.erp.mnt.service;

import app.erp.mnt.dao.constants.ErpMntDocStatus;

/**
 * 维护域服务层常量。状态码权威值来自 {@code app.erp.mnt.dao._ErpMntDaoConstants}（生成）。
 *
 * <p>本接口补充跨域联动标识、库存作业类型调用方副本、配置键等。
 *
 * <p>{@code extends ErpMntDocStatus} 复用 dao 层常量定义，保持 approve-status / doc-status 单一真相源；
 * 本接口仅追加 service 层独有的派生状态与配置项。维护域 orm.xml 不再定义 per-domain approve-status
 * 域字典（plan 2026-07-07-1915-1 M-1 删除冗余字典，其 per-domain dict 文件亦于 plan 2026-07-24-0930-2 移除），
 * 故 {@code _ErpMntDaoConstants} 不再生成这些常量。
 */
public interface ErpMntConstants extends ErpMntDocStatus {

    // approve-status / doc-status 常量继承自 ErpMntDocStatus（dao 层单一真相源）

    // 库存作业类型（对齐 erp-inv/operation-type，调用方侧副本避免 main 代码依赖 inventory-service）
    String MOVE_TYPE_OUTGOING = "OUTGOING";

    // 库存移动单业务态（对齐 erp-inv/doc-status DONE，调用方侧副本）：DONE 代表库存已出库。
    String STOCK_MOVE_DOC_STATUS_DONE = "DONE";

    // 备件领料出库联动标识（自由字符串，inventory 侧无字典约束）
    String RELATED_BILL_TYPE_MNT_SPARE_PART = "ERP_MNT_SPARE_PART";

    // 配置项（经 AppConfig.var 读取，缺失走默认，无 .env）
    String CONFIG_AUTO_GENERATE_DUE_VISITS = "erp-mnt.auto-generate-due-visits";
    String CONFIG_EQUIPMENT_STATUS_LINK_ENABLED = "erp-mnt.equipment-status-link-enabled";
    /** 定时到期访问生成 cron（空=不调度；plan 2026-07-05-0306-1 §配置点）。 */
    String CONFIG_DUE_VISIT_CRON = "erp-mnt.due-visit-cron";

    // ---- 备件消耗 GL 过账配置项（plan 2026-07-10-1100-6）----
    /** 备件消耗 GL 过账总开关，默认 false（向后兼容：仅库存出库，不生成凭证）。 */
    String CONFIG_SPARE_PART_POSTING_ENABLED = "erp-mnt.spare-part-posting-enabled";
    /** 维修费用科目编码（借方），默认 6602。 */
    String CONFIG_EXPENSE_SUBJECT_CODE = "erp-mnt.expense-subject-code";
    /** 存货科目编码（贷方默认），默认 1403（与物料类别 inventorySubject 同科目源族）。 */
    String CONFIG_INVENTORY_SUBJECT_CODE = "erp-mnt.inventory-subject-code";

    boolean DEFAULT_SPARE_PART_POSTING_ENABLED = false;
    String DEFAULT_EXPENSE_SUBJECT_CODE = "6602";
    String DEFAULT_INVENTORY_SUBJECT_CODE = "1403";

    // ---- 维修工时费用化 GL 过账配置项（plan 2026-07-18-0949-1）----
    /** 工时费用化 GL 过账总开关，默认 false（向后兼容：仅 complete 终态，不生成凭证）。 */
    String CONFIG_LABOR_POSTING_ENABLED = "erp-mnt.labor-posting-enabled";
    /** 默认工时小时费率（元/小时），默认 0=未配置（跳过过账，不抛错）。 */
    String CONFIG_DEFAULT_LABOR_HOURLY_RATE = "erp-mnt.default-labor-hourly-rate";
    /** 应付职工薪酬科目编码（贷方），默认 2211（种子 0742-2 已加性追加）。 */
    String CONFIG_LABOR_PAYABLE_SUBJECT_CODE = "erp-mnt.labor-payable-subject-code";

    boolean DEFAULT_LABOR_POSTING_ENABLED = false;
    String DEFAULT_LABOR_HOURLY_RATE_VALUE = "0";
    String DEFAULT_LABOR_PAYABLE_SUBJECT_CODE = "2211";

    boolean DEFAULT_AUTO_GENERATE_DUE_VISITS = true;
    boolean DEFAULT_EQUIPMENT_STATUS_LINK_ENABLED = true;

    // ---- 跨域联动配置项（RC-R1.76/77，B 类预授权）----
    /** 资产处置→设备 DECOMMISSIONED 联动门控（镜像 equipment-status-link-enabled 先例），默认 true。 */
    String CONFIG_DISPOSAL_LINK_ENABLED = "erp-mnt.disposal-link-enabled";
    /** 停机 record/complete 双向计划员通知门控（notify 辅助语义），默认 true。 */
    String CONFIG_DOWNTIME_NOTIFY_ENABLED = "erp-mnt.downtime-notify-enabled";

    boolean DEFAULT_DISPOSAL_LINK_ENABLED = true;
    boolean DEFAULT_DOWNTIME_NOTIFY_ENABLED = true;

    // ---- 状态日志来源（RC-R1.77 处置链路）----
    /**
     * 处置联动状态日志来源。注意：dict erp-mnt/status-log-source 值集当前为 VISIT/DOWNTIME/MANUAL
     * （零 ORM 约束下不追加 dict 选项）；Java 写路径（EquipmentStatusLogWriter.append）不做 dict 校验，
     * UI 展示回落原值码。dict 选项加性追加归 successor（ORM 变更）。
     */
    String STATUS_LOG_SOURCE_DISPOSAL = "DISPOSAL";

    // ---- notify 事件类型（RC-R1.76 停机双事件，模板种子 7208/7209）----
    String NOTIFY_EVENT_EQUIPMENT_DOWNTIME = "mnt.equipment-downtime";
    String NOTIFY_EVENT_EQUIPMENT_RECOVERED = "mnt.equipment-recovered";

    // ---- 看板预警阈值配置项（dashboards.md §实现约定 §5，经 AppConfig.var 读取，NopSysVariable 可运行时覆盖）----
    /** 维护逾期预警窗口天数（Schedule.nextDueDate 早于 today-minus-overdueDays 触发预警）；默认 0=直接 < today 比对。 */
    String CONFIG_DASH_MNT_MAINTENANCE_OVERDUE_DAYS = "erp-dash.mnt-maintenance-overdue-days";
    int DEFAULT_DASH_MNT_MAINTENANCE_OVERDUE_DAYS = 0;
}
