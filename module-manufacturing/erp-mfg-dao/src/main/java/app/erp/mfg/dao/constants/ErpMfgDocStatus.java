package app.erp.mfg.dao.constants;

/**
 * 制造域单据状态常量（dao 层）。仅包含实体状态判断方法所需的稳定值。
 *
 * <p>权威值来自 {@code module-manufacturing/model/app-erp-manufacturing.orm.xml} 关联字典
 * {@code wf/approve-status}。制造域 {@code docStatus} 列绑定域专属状态字典（work-order-status /
 * issue-status / subcontract-status），无统一 doc-status 字典，故本接口仅承载审核轴；
 * 单据生命周期常量保留在 {@code app.erp.mfg.service.ErpMfgConstants}（WORK_ORDER_STATUS_* 等）。
 * service 层的 {@code app.erp.mfg.service.ErpMfgConstants} 通过 {@code extends ErpMfgDocStatus}
 * 保持向后兼容，避免审核轴常量漂移。
 */
public interface ErpMfgDocStatus {

    // 审核轴 approve-status（三轴审批的审核轴）
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";
}
