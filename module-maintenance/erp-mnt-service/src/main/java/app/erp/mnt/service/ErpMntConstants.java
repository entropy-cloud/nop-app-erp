package app.erp.mnt.service;

/**
 * 维护域服务层常量。状态码权威值来自 {@code app.erp.mnt.dao._ErpMntDaoConstants}（生成）。
 *
 * <p>本接口补充跨域联动标识、库存作业类型调用方副本、配置键等。
 *
 * <p>审核轴常量（{@code wf/approve-status} 四态）由本接口显式声明——维护域 orm.xml 不再
 * 定义 {@code erp-mnt/approve-status} 域字典（plan 2026-07-07-1915-1 M-1 删除冗余字典），
 * 故 {@code _ErpMntDaoConstants} 不再生成这些常量。
 */
public interface ErpMntConstants {

    // 审核轴 wf/approve-status（四态，与 nop-wf 平台字典对齐；显式声明避免依赖生成的 DaoConstants）
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

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

    boolean DEFAULT_AUTO_GENERATE_DUE_VISITS = true;
    boolean DEFAULT_EQUIPMENT_STATUS_LINK_ENABLED = true;

    // ---- 看板预警阈值配置项（dashboards.md §实现约定 §5，经 AppConfig.var 读取，NopSysVariable 可运行时覆盖）----
    /** 维护逾期预警窗口天数（Schedule.nextDueDate 早于 today-minus-overdueDays 触发预警）；默认 0=直接 < today 比对。 */
    String CONFIG_DASH_MNT_MAINTENANCE_OVERDUE_DAYS = "erp-dash.mnt-maintenance-overdue-days";
    int DEFAULT_DASH_MNT_MAINTENANCE_OVERDUE_DAYS = 0;
}
