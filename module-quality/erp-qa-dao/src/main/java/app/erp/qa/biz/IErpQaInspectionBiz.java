package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * 质检单业务接口。除标准 CRUD 外，定义质检单 4 态状态机
 * （{@code docs/design/quality/state-machine.md §适用对象一`}）+ 业务触发 + 结果反查契约。
 *
 * <p>标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由 {@link IApprovableBiz}
 * 声明，运行时由平台 {@code approval-support.xbiz} 标准 source 提供。
 *
 * <p>状态机/业务方法（{@link BizMutation}/{@link BizQuery}，自动事务包装）：
 * <ul>
 *   <li>{@link #recordResult}：PENDING→ACCEPTED / CONDITIONAL / REJECTED（行级评测 + 汇总 + posted=true）。</li>
 *   <li>{@link #findByRelatedBill}：按关联业务单据反查质检单 + result（业务域查结论，跨域只读经 I*Biz）。</li>
 *   <li>{@link #createForBusinessBill}：业务触发生成质检单（采购入库→INCOMING / 销售出库→OUTGOING / 工单完工→FINAL）。</li>
 *   <li>{@link #isInspectionCleared}：强制质检门控（业务域 confirm/DONE 前查，未合格/让步则拒绝）。</li>
 * </ul>
 *
 * <p>非法迁移抛 {@code ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION}。权威状态机见
 * {@code docs/design/quality/state-machine.md}；计划见
 * {@code docs/plans/2026-07-02-2237-3-quality-inspection-trigger-ncr-capa.md}。
 */
public interface IErpQaInspectionBiz extends ICrudBiz<ErpQaInspection> {

    @BizMutation
    ErpQaInspection recordResult(@Name("inspectionId") Long inspectionId,
                                 @Name("lineResults") List<InspectionLineResultInput> lineResults,
                                 @Name("allowConcession") Boolean allowConcession,
                                 IServiceContext context);

    @BizQuery
    List<ErpQaInspection> findByRelatedBill(@Name("billType") String billType,
                                            @Name("billCode") String billCode,
                                            IServiceContext context);

    @BizMutation
    ErpQaInspection createForBusinessBill(@Name("billType") String billType,
                                          @Name("billCode") String billCode,
                                          @Name("materialId") Long materialId,
                                          @Name("inspectionType") String inspectionType,
                                          @Name("lotQuantity") BigDecimal lotQuantity,
                                          @Name("supplierId") Long supplierId,
                                          @Name("warehouseId") Long warehouseId,
                                          @Name("batchNo") String batchNo,
                                          IServiceContext context);

    @BizQuery
    boolean isInspectionCleared(@Name("billType") String billType,
                                @Name("billCode") String billCode,
                                IServiceContext context);

    /**
     * 简单合格判定：PENDING→ACCEPTED（行级结果保留自动评测；不录入行级 measuredValue）。
     * 守卫 {@code result==PENDING} 单一源态 + 设 posted=true（与 {@link #recordResult} 行为对齐）；
     * 终态直接调用抛 {@code ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION}。
     */
    @BizMutation
    ErpQaInspection passInspection(@Name("inspectionId") Long inspectionId, IServiceContext context);

    /**
     * 简单不合格判定：PENDING→REJECTED + 设 posted=true + 触发 {@code autoCreateNcrFromInspection}（REJECTED 自动建 NCR）。
     * 守卫 {@code result==PENDING} 单一源态；终态直接调用抛 {@code ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION}。
     */
    @BizMutation
    ErpQaInspection failInspection(@Name("inspectionId") Long inspectionId, IServiceContext context);

    /**
     * F11 批量判定合格（plan 2026-07-22-0444-2 Phase 1）：循环调单条 {@link #passInspection}，
     * 逐行执行（模式 b：行级失败不阻塞其他行），返回 {@link BatchOperationResult} 含成功数 + 失败明细。
     *
     * <p>仅当行 {@code result=PENDING} 才会被推进到 ACCEPTED；其他状态记入 failures。
     */
    @BizMutation
    BatchOperationResult batchPassInspection(@Name("ids") Collection<String> ids, IServiceContext context);

    /**
     * 业务单据作废联动取消质检（UC-QA-08，P1-RC-041 / RC-R1.59）：按 relatedBillType+relatedBillCode
     * 精确查询关联质检单，仅 {@code result=PENDING} 的软删取消（CANCELLED 语义，useLogicalDelete 置 delVersion），
     * 已 ACCEPTED/CONDITIONAL/REJECTED 不动（历史完整，L1 use-cases.md:141）；无匹配零副作用。
     *
     * <p>config-gated（{@code erp-qua.business-cancel-linkage-enabled}，默认 true）：关闭时零副作用返回 0。
     * 幂等：二次调用查无 PENDING 零副作用。返回实际取消的质检单数。
     *
     * <p>软删后 {@link #findByRelatedBill} 经平台逻辑删除过滤（delVersion=0）自动不可见，
     * 门控/反查语义天然闭合。业务域 cancel Processor 后置调用，失败以 LOG.warn 降级不阻断作废主流程。
     */
    @BizMutation
    int cancelForBusinessBill(@Name("billType") String billType,
                              @Name("billCode") String billCode,
                              IServiceContext context);
}
