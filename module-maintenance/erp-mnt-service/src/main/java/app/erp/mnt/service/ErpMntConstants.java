package app.erp.mnt.service;

/**
 * 维护域服务层常量。状态码权威值来自 {@code app.erp.mnt.dao._ErpMntDaoConstants}（生成）。
 *
 * <p>本接口补充跨域联动标识、库存作业类型调用方副本、配置键等。
 */
public interface ErpMntConstants {

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

    boolean DEFAULT_AUTO_GENERATE_DUE_VISITS = true;
    boolean DEFAULT_EQUIPMENT_STATUS_LINK_ENABLED = true;
}
