
package app.erp.qa.service.entity;

import app.erp.qa.biz.BatchOperationResult;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionLineResultInput;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.processor.ErpQaInspectionBatchPassInspectionProcessor;
import app.erp.qa.service.processor.ErpQaInspectionCancelForBusinessBillProcessor;
import app.erp.qa.service.processor.ErpQaInspectionCreateForBusinessBillProcessor;
import app.erp.qa.service.processor.ErpQaInspectionFailInspectionProcessor;
import app.erp.qa.service.processor.ErpQaInspectionPassInspectionProcessor;
import app.erp.qa.service.processor.ErpQaInspectionRecordResultProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 质检单 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。在 {@link CrudBizModel} 标准 CRUD 之上
 * 实现质检单 4 态状态机（{@code docs/design/quality/state-machine.md §适用对象一`}）。多步 mutation 编排委托
 * per-mutation Processor（行级评测 + 结果汇总 + posted + 业务反查），下游可经 Delta beans.xml 同名 bean id 覆盖。
 *
 * <p>结果反查 {@link #findByRelatedBill} 供业务域查质检结论（business→quality 只读，DAG 无环）；
 * 强制质检门控 {@link #isInspectionCleared} 供业务域 confirm/DONE 前校验。
 */
@BizModel("ErpQaInspection")
public class ErpQaInspectionBizModel extends CrudBizModel<ErpQaInspection> implements IErpQaInspectionBiz {

    @Inject
    ErpQaInspectionRecordResultProcessor recordResultProcessor;
    @Inject
    ErpQaInspectionCreateForBusinessBillProcessor createForBusinessBillProcessor;
    @Inject
    ErpQaInspectionPassInspectionProcessor passInspectionProcessor;
    @Inject
    ErpQaInspectionFailInspectionProcessor failInspectionProcessor;
    @Inject
    ErpQaInspectionBatchPassInspectionProcessor batchPassInspectionProcessor;
    @Inject
    ErpQaInspectionCancelForBusinessBillProcessor cancelForBusinessBillProcessor;

    public ErpQaInspectionBizModel() {
        setEntityName(ErpQaInspection.class.getName());
    }

    @Override
    @BizMutation
    public ErpQaInspection recordResult(@Name("inspectionId") Long inspectionId,
                                        @Name("lineResults") List<InspectionLineResultInput> lineResults,
                                        @Name("allowConcession") Boolean allowConcession,
                                        IServiceContext context) {
        return recordResultProcessor.recordResult(inspectionId, lineResults, allowConcession, context);
    }

    @Override
    @BizQuery
    public List<ErpQaInspection> findByRelatedBill(@Name("billType") String billType,
                                                   @Name("billCode") String billCode,
                                                   IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        q.addOrderField("id", false);
        return findList(q, null, context);
    }

    @Override
    @BizQuery
    public boolean isInspectionCleared(@Name("billType") String billType,
                                       @Name("billCode") String billCode,
                                       IServiceContext context) {
        List<ErpQaInspection> inspections = findByRelatedBill(billType, billCode, context);
        for (ErpQaInspection ins : inspections) {
            String result = ins.getResult();
            if (result == null || Objects.equals(result, ErpQaConstants.INSPECTION_RESULT_PENDING)) {
                return false;
            }
            // ACCEPTED / CONDITIONAL 放行；REJECTED 阻塞（业务域应触发退货/返工/NCR 处置）
            if (Objects.equals(result, ErpQaConstants.INSPECTION_RESULT_REJECTED)) {
                return false;
            }
        }
        return true;
    }

    @Override
    @BizMutation
    public ErpQaInspection createForBusinessBill(@Name("billType") String billType,
                                                 @Name("billCode") String billCode,
                                                 @Name("materialId") Long materialId,
                                                 @Name("inspectionType") String inspectionType,
                                                 @Name("lotQuantity") BigDecimal lotQuantity,
                                                 @Name("supplierId") Long supplierId,
                                                 @Name("warehouseId") Long warehouseId,
                                                 @Name("batchNo") String batchNo,
                                                 IServiceContext context) {
        return createForBusinessBillProcessor.createForBusinessBill(billType, billCode, materialId, inspectionType,
                lotQuantity, supplierId, warehouseId, batchNo, context);
    }

    @Override
    @BizMutation
    public ErpQaInspection passInspection(@Name("inspectionId") Long inspectionId, IServiceContext context) {
        return passInspectionProcessor.passInspection(inspectionId, context);
    }

    @Override
    @BizMutation
    public ErpQaInspection failInspection(@Name("inspectionId") Long inspectionId, IServiceContext context) {
        return failInspectionProcessor.failInspection(inspectionId, context);
    }

    /**
     * F11 批量判定合格（plan 2026-07-22-0444-2 Phase 1）。逐行调 {@link ErpQaInspectionPassInspectionProcessor#passInspection}；
     * 行级失败（result 非 PENDING 等）记入 {@link BatchOperationResult#getFailures()}，不阻塞其他行。
     *
     * <p>注：{@code reInspect} 已废弃（P0-MA2-017：终态不可直接恢复）。复检走
     * {@link #createForBusinessBill} 新建关联质检单（owner doc state-machine.md §3）。
     */
    @Override
    @BizMutation
    public BatchOperationResult batchPassInspection(@Name("ids") Collection<String> ids, IServiceContext context) {
        return batchPassInspectionProcessor.batchPassInspection(ids, context);
    }

    @Override
    @BizMutation
    public int cancelForBusinessBill(@Name("billType") String billType,
                                     @Name("billCode") String billCode,
                                     IServiceContext context) {
        return cancelForBusinessBillProcessor.cancelForBusinessBill(billType, billCode, context);
    }
}
