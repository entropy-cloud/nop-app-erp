package app.erp.sal.dao.constants;

/**
 * 销售域单据状态常量（dao 层）。仅包含实体状态判断方法所需的稳定值。
 *
 * <p>权威值来自 {@code module-sales/model/app-erp-sales.orm.xml} 关联字典
 * {@code erp-sal/approve-status} 与 {@code erp-sal/doc-status}。本接口为 dao 层引用接口，
 * service 层的 {@code app.erp.sal.service.ErpSalConstants} 通过 {@code extends ErpSalDocStatus}
 * 保持向后兼容，避免常量漂移。
 *
 * <p>三轴状态分离见 {@code docs/design/sales/state-machine.md}。
 */
public interface ErpSalDocStatus {

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
