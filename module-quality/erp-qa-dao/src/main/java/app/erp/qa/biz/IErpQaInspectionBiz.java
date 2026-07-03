package app.erp.qa.biz;

import app.erp.qa.dao.entity.ErpQaInspection;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.math.BigDecimal;
import java.util.List;

/**
 * 质检单业务接口。除标准 CRUD 外，定义质检单 4 态状态机
 * （{@code docs/design/quality/state-machine.md §适用对象一`}）+ 业务触发 + 结果反查契约。
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
                                          @Name("inspectionType") Integer inspectionType,
                                          @Name("lotQuantity") BigDecimal lotQuantity,
                                          @Name("supplierId") Long supplierId,
                                          @Name("warehouseId") Long warehouseId,
                                          @Name("batchNo") String batchNo,
                                          IServiceContext context);

    @BizQuery
    boolean isInspectionCleared(@Name("billType") String billType,
                                @Name("billCode") String billCode,
                                IServiceContext context);
}
