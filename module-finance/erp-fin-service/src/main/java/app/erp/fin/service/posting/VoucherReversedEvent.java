package app.erp.fin.service.posting;

/**
 * 凭证红冲事件（业财闭环方向二：财务侧红冲→业务单据回退）。
 *
 * <p>财务员调用 {@code IErpFinVoucherBiz.reverse()} 红冲已过账凭证成功后，由
 * {@link ErpFinPostingProcessor#reverseProcess} 构造本事件并经
 * {@link ErpFinReversalListenerRegistry} 派发给所有 {@link IErpFinVoucherReversedListener}
 * 监听者（purchase/sales/inventory 各域自治实现，回退自身 {@code posted}+{@code docStatus}）。
 *
 * <p>字段契约对齐 {@code docs/design/finance/posting.md §VoucherReversedEvent 契约}。
 * 引擎只持有事件快照不持有源业务实体的 ORM 引用（反写契约见 {@code posting.md §反写契约}）。
 */
public class VoucherReversedEvent {

    /** 红字凭证 ID */
    private Long voucherId;
    /** 被冲销的原凭证 ID */
    private Long reversalOfVoucherId;
    /** 关联的业务单据号（经业财回链反查） */
    private String billHeadCode;
    /** 业务类型（路由回退逻辑用，对应 {@link app.erp.fin.dao.ErpFinBusinessType} 枚举名） */
    private String businessType;
    /** 源单类型（对应 ORM 实体，见 data-dependency-matrix.md §5.2；同 businessType，留作域监听器分流） */
    private String billType;
    /** 端到端追踪 ID（见 posting-log.md） */
    private String traceId;

    public Long getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Long getReversalOfVoucherId() {
        return reversalOfVoucherId;
    }

    public void setReversalOfVoucherId(Long reversalOfVoucherId) {
        this.reversalOfVoucherId = reversalOfVoucherId;
    }

    public String getBillHeadCode() {
        return billHeadCode;
    }

    public void setBillHeadCode(String billHeadCode) {
        this.billHeadCode = billHeadCode;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
