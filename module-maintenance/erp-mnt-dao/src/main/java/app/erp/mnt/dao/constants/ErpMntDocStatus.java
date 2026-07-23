package app.erp.mnt.dao.constants;

/**
 * 维护域单据状态常量（dao 层）。仅包含实体状态判断方法所需的稳定值。
 *
 * <p>权威值来自 {@code module-maintenance/model/app-erp-maintenance.orm.xml} 关联字典
 * {@code wf/approve-status} 与 {@code erp-mnt/doc-status}。本接口为 dao 层引用接口，
 * service 层的 {@code app.erp.mnt.service.ErpMntConstants} 通过 {@code extends ErpMntDocStatus}
 * 保持向后兼容，避免常量漂移。
 */
public interface ErpMntDocStatus {

    // 审核轴 approve-status
    String APPROVE_STATUS_UNSUBMITTED = "UNSUBMITTED";
    String APPROVE_STATUS_SUBMITTED = "SUBMITTED";
    String APPROVE_STATUS_APPROVED = "APPROVED";
    String APPROVE_STATUS_REJECTED = "REJECTED";

    // 单据生命周期轴 doc-status
    String DOC_STATUS_DRAFT = "DRAFT";
    String DOC_STATUS_ACTIVE = "ACTIVE";
    String DOC_STATUS_CANCELLED = "CANCELLED";
}
