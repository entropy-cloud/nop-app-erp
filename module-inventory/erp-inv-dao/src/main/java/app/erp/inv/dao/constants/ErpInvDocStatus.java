package app.erp.inv.dao.constants;

/**
 * 库存域单据状态常量（dao 层）。仅包含实体状态判断方法所需的稳定值。
 *
 * <p>权威值来自 {@code module-inventory/model/app-erp-inventory.orm.xml} 关联字典
 * {@code erp-inv/move-status}（doc-status）与 ErpInvCostAdjust 审批轴。
 * 本接口为 dao 层引用接口。service 层的 {@code app.erp.inv.service.ErpInvConstants}
 * 通过 {@code extends ErpInvDocStatus} 保持向后兼容（doc-status 复用，approve-status 为新增集中定义）。
 */
public interface ErpInvDocStatus {

    // 审批轴 approve-status（ErpInvCostAdjust DIRECT 审批状态机使用）
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // 单据生命周期轴 doc-status
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_CONFIRMED = "CONFIRMED";
    String DOC_STATUS_DONE = "DONE";
    String DOC_STATUS_CANCELLED = "CANCELLED";
}
