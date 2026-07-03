package app.erp.mfg.service;

/**
 * 制造域常量。字典码值权威：`erp-mfg-meta/.../dict/*.dict.yaml` + `module-manufacturing/model/*.orm.xml`。
 *
 * <p>权威：`docs/design/manufacturing/bom-and-routing.md`、`docs/design/manufacturing/state-machine.md`、
 * `docs/plans/2026-07-02-1538-2-manufacturing-bom-routing-rollup.md`、`docs/plans/2026-07-02-2237-1-manufacturing-workorder-jobcard-state-machine.md`。
 */
public interface ErpMfgConstants {

    // BOM 类型（erp-mfg/bom-type）
    int BOM_TYPE_MANUFACTURED = 10;
    int BOM_TYPE_PHANTOM = 20;

    // 成本滚算状态（erp-mfg/cost-rollup-status）
    int COST_ROLLUP_STATUS_DRAFT = 10;
    int COST_ROLLUP_STATUS_CALCULATED = 20;
    int COST_ROLLUP_STATUS_FIRMED = 30;

    // 多级 BOM 展开深度上限 / 环兜底（`erp-mfg.bom-max-depth`）
    String CONFIG_BOM_MAX_DEPTH = "erp-mfg.bom-max-depth";
    int DEFAULT_BOM_MAX_DEPTH = 15;

    // 工单状态（erp-mfg/work-order-status，10 态；权威：state-machine.md §适用对象一）
    int WORK_ORDER_STATUS_DRAFT = 10;
    int WORK_ORDER_STATUS_SUBMITTED = 20;
    int WORK_ORDER_STATUS_NOT_STARTED = 30;
    int WORK_ORDER_STATUS_IN_PROCESS = 40;
    int WORK_ORDER_STATUS_STOCK_RESERVED = 50;
    int WORK_ORDER_STATUS_STOCK_PARTIAL = 60;
    int WORK_ORDER_STATUS_COMPLETED = 70;
    int WORK_ORDER_STATUS_STOPPED = 80;
    int WORK_ORDER_STATUS_CLOSED = 90;
    int WORK_ORDER_STATUS_CANCELLED = 100;

    // 审核状态（erp-mfg/approve-status，三轴审批的审核轴）
    int APPROVE_STATUS_UNSUBMITTED = 10;
    int APPROVE_STATUS_SUBMITTED = 20;
    int APPROVE_STATUS_APPROVED = 30;
    int APPROVE_STATUS_REJECTED = 40;

    // 领料状态（erp-mfg/issue-status）
    int ISSUE_STATUS_DRAFT = 10;
    int ISSUE_STATUS_CONFIRMED = 20;
    int ISSUE_STATUS_DONE = 30;
    int ISSUE_STATUS_CANCELLED = 40;

    // 作业卡状态（erp-mfg/job-card-status，8 态；权威：state-machine.md §适用对象二）
    int JOB_CARD_STATUS_OPEN = 10;
    int JOB_CARD_STATUS_WORK_IN_PROGRESS = 20;
    int JOB_CARD_STATUS_PARTIALLY_TRANSFERRED = 30;
    int JOB_CARD_STATUS_MATERIAL_TRANSFERRED = 40;
    int JOB_CARD_STATUS_ON_HOLD = 50;
    int JOB_CARD_STATUS_SUBMITTED = 60;
    int JOB_CARD_STATUS_COMPLETED = 70;
    int JOB_CARD_STATUS_CANCELLED = 80;

    // 工单行类型（ErpMfgWorkOrderLine.lineType 自由字符串）
    String WORK_ORDER_LINE_TYPE_OUTPUT = "OUTPUT";
    String WORK_ORDER_LINE_TYPE_INPUT = "INPUT";
    String WORK_ORDER_LINE_TYPE_BYPRODUCT = "BYPRODUCT";

    // 业务联动源单类型（manufacturing 发起的库存移动单 relatedBillType 自由字符串）
    String RELATED_BILL_TYPE_MFG_ISSUE = "ERP_MFG_ISSUE";
    String RELATED_BILL_TYPE_MFG_WORK_ORDER = "ERP_MFG_WORK_ORDER";

    // 库存移动单作业类型（对齐 ErpInvConstants 码值，避免跨 service 模块依赖）
    int MOVE_TYPE_OUTGOING_ISSUE = 20;        // 领料出库（ErpInvConstants.MOVE_TYPE_OUTGOING）：库存域 bookCompletion 据此扣减余额
    int MOVE_TYPE_MANUFACTURING = 40;         // 完工入库（ErpInvConstants.MOVE_TYPE_MANUFACTURING）：库存域视为入库，加产成品库存

    // 工单状态机配置项（无 .env/外部服务，经 AppConfig.var 读取）
    String CONFIG_ALLOW_PARTIAL_KIT_START = "erp-mfg.allow-partial-kit-start";
    String CONFIG_INSPECTION_GATE_ENABLED = "erp-mfg.inspection-gate-enabled";
}
