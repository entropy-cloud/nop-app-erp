package app.erp.qa.dao.constants;

/**
 * 质量域单据状态常量（dao 层）。仅包含实体状态判断方法所需的稳定值。
 *
 * <p>权威值来自 {@code module-quality/model/app-erp-quality.orm.xml} 关联字典
 * {@code wf/approve-status} 与 {@code erp-qa/doc-status}。本接口为 dao 层引用接口，
 * service 层的 {@code app.erp.qa.service.ErpQaConstants} 通过 {@code extends ErpQaDocStatus}
 * 保持向后兼容，避免常量漂移。
 */
public interface ErpQaDocStatus {

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
