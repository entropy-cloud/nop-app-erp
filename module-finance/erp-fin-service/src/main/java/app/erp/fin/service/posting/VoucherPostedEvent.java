package app.erp.fin.service.posting;

/**
 * 凭证正向过账成功事件（业财闭环方向一补全：财务侧过账成功→业务单据 posted 回写）。
 *
 * <p>悬挂场景（REQUIRES_NEW 凭证已提交 + 调用方主事务回滚 → 源单 posted=false + 凭证存在 + PENDING 异常）
 * 由两「调用方不在场」重试通道在凭证落账后构造本事件并经 {@link ErpFinPostedListenerRegistry} 派发给所有
 * {@link IErpFinVoucherPostedListener} 监听者（各域自治实现，回写自身 {@code posted} 三字段）：
 * <ul>
 *   <li>{@code ErpFinDeferredPostingRetryHelper#doRetry}——deferred-posting sweep 批任务</li>
 *   <li>{@code ErpFinPostingExceptionRetryProcessor#retry}——异常工作台手动重试</li>
 * </ul>
 * 引擎 {@code process()} 路径不派发（反写契约域自治 + 硬规则 6 原子性，见
 * {@code posting.md §反写契约 §VoucherPostedEvent 契约}）。
 *
 * <p>引擎只持有事件快照不持有源业务实体的 ORM 引用（反写契约见 {@code posting.md §反写契约}）。
 */
public class VoucherPostedEvent {

    /** 已过账凭证 ID */
    private String voucherId;
    /** 关联的业务单据号（经业财回链反查） */
    private String billHeadCode;
    /** 业务类型（路由回退逻辑用，对应 {@link app.erp.fin.dao.ErpFinBusinessType} 枚举名） */
    private String businessType;
    /** 源单类型（对应 ORM 实体，见 data-dependency-matrix.md §5.2；同 businessType，留作域监听器分流） */
    private String billType;
    /** 端到端追踪 ID（见 posting-log.md） */
    private String traceId;

    public String getVoucherId() {
        return voucherId;
    }

    public void setVoucherId(String voucherId) {
        this.voucherId = voucherId;
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
