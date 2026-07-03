
package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionLineResultInput;
import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 质检单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现质检单 4 态状态机
 * （{@code docs/design/quality/state-machine.md §适用对象一`}）：行级评测 + 结果汇总 + posted + 业务反查。
 *
 * <p>行级评测经 {@link InspectionResultEvaluator}（specMin/specMax vs measuredValue 数值比较），
 * 汇总遵循 Task Route Decision（全合格→ACCEPTED；含不合格+让步→CONDITIONAL；含不合格未让步→REJECTED）。
 * 终态不可恢复；PENDING→终态由 {@link #recordResult} 触发。
 *
 * <p>结果反查 {@link #findByRelatedBill} 供业务域查质检结论（business→quality 只读，DAG 无环）；
 * 强制质检门控 {@link #isInspectionCleared} 供业务域 confirm/DONE 前校验。
 */
@BizModel("ErpQaInspection")
public class ErpQaInspectionBizModel extends CrudBizModel<ErpQaInspection> implements IErpQaInspectionBiz {

    @Inject
    NcrLifecycleService ncrLifecycleService;

    public ErpQaInspectionBizModel() {
        setEntityName(ErpQaInspection.class.getName());
    }

    public void setNcrLifecycleService(NcrLifecycleService ncrLifecycleService) {
        this.ncrLifecycleService = ncrLifecycleService;
    }

    @Override
    @BizMutation
    public ErpQaInspection recordResult(@Name("inspectionId") Long inspectionId,
                                        @Name("lineResults") List<InspectionLineResultInput> lineResults,
                                        @Name("allowConcession") Boolean allowConcession,
                                        IServiceContext context) {
        ErpQaInspection inspection = requireInspection(inspectionId, context);
        String current = inspection.getResult();
        if (current != null && !Objects.equals(current, ErpQaConstants.INSPECTION_RESULT_PENDING)) {
            throw illegalInspectionTransition(inspection, current, "PENDING（终态不可恢复）");
        }

        List<ErpQaInspectionLine> lines = loadLines(inspectionId);
        if (lines.isEmpty()) {
            throw new NopException(ErpQaErrors.ERR_INSPECTION_LINES_EMPTY)
                    .param(ErpQaErrors.ARG_INSPECTION_CODE, inspection.getCode());
        }

        Set<Long> explicitResultLineIds = applyLineResults(lines, lineResults);
        for (ErpQaInspectionLine line : lines) {
            if (!explicitResultLineIds.contains(line.getId())) {
                line.setResult(InspectionResultEvaluator.evaluateLine(line));
            }
            daoFor(ErpQaInspectionLine.class).updateEntity(line);
        }

        boolean concession = Boolean.TRUE.equals(allowConcession);
        String aggregated = InspectionResultEvaluator.aggregate(lines, concession);
        inspection.setResult(aggregated);
        inspection.setPosted(Boolean.TRUE);
        if (Objects.equals(aggregated, ErpQaConstants.INSPECTION_RESULT_CONDITIONAL)) {
            // 让步接收须经审批：本期以质量主管审核（approveStatus=APPROVED）简化（完整多级审批流 Non-Goal）
            inspection.setApproveStatus(ErpQaConstants.APPROVE_STATUS_APPROVED);
        }
        dao().updateEntity(inspection);

        // Phase 3：REJECTED 自动生成 NCR（经 NcrLifecycleService，配置门控）
        if (Objects.equals(aggregated, ErpQaConstants.INSPECTION_RESULT_REJECTED)) {
            ncrLifecycleService.autoCreateNcrFromInspection(inspection, lines, context);
        }
        return inspection;
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
                                                 @Name("lotQuantity") java.math.BigDecimal lotQuantity,
                                                 @Name("supplierId") Long supplierId,
                                                 @Name("warehouseId") Long warehouseId,
                                                 @Name("batchNo") String batchNo,
                                                 IServiceContext context) {
        return doCreateForBusinessBill(billType, billCode, materialId, inspectionType,
                lotQuantity, supplierId, warehouseId, batchNo, context);
    }

    ErpQaInspection doCreateForBusinessBill(String billType, String billCode, Long materialId, String inspectionType,
                                            java.math.BigDecimal lotQuantity, Long supplierId, Long warehouseId,
                                            String batchNo, IServiceContext context) {
        // 模板匹配：materialId × inspectionType → active 模板；无匹配走全局默认；仍无则无行（人工补录）
        TemplateMatchResult match = InspectionTemplateMatcher.match(daoProvider(), materialId, inspectionType);

        ErpQaInspection inspection = newEntity();
        inspection.setCode(generateCode(billType, billCode));
        inspection.setRelatedBillType(billType);
        inspection.setRelatedBillCode(billCode);
        inspection.setMaterialId(materialId);
        inspection.setInspectionType(inspectionType);
        inspection.setLotQuantity(lotQuantity);
        inspection.setSupplierId(supplierId);
        inspection.setWarehouseId(warehouseId);
        inspection.setBatchNo(batchNo);
        inspection.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
        inspection.setDocStatus(ErpQaConstants.DOC_STATUS_ACTIVE);
        inspection.setApproveStatus(ErpQaConstants.APPROVE_STATUS_UNSUBMITTED);
        inspection.setPosted(Boolean.FALSE);
        inspection.setInspectionDate(java.time.LocalDate.now());
        inspection.setBusinessDate(java.time.LocalDate.now());
        if (match != null) {
            inspection.setTemplateId(match.getTemplateId());
        }
        saveEntity(inspection, null, context);

        // 模板行复制到质检单行（模板行无 parameterId，质检单行 parameterId 留空）
        if (match != null) {
            copyTemplateLinesToInspection(inspection.getId(), match);
        }
        return inspection;
    }

    private void copyTemplateLinesToInspection(Long inspectionId, TemplateMatchResult match) {
        IEntityDao<ErpQaInspectionLine> lineDao = daoFor(ErpQaInspectionLine.class);
        int lineNo = 1;
        for (TemplateLineSpec spec : match.getLines()) {
            ErpQaInspectionLine line = lineDao.newEntity();
            line.setInspectionId(inspectionId);
            line.setLineNo(lineNo++);
            line.setParameterName(spec.getParameterName());
            line.setSpecMin(spec.getSpecMin());
            line.setSpecMax(spec.getSpecMax());
            line.setUnit(spec.getUnit());
            line.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
            lineDao.saveEntity(line);
        }
    }

    private String generateCode(String billType, String billCode) {
        return "INS-" + billType + "-" + io.nop.api.core.time.CoreMetrics.currentTimeMillis();
    }

    // ---------- helpers ----------

    private ErpQaInspection requireInspection(Long inspectionId, IServiceContext context) {
        if (inspectionId == null) {
            throw new NopException(ErpQaErrors.ERR_INSPECTION_NOT_FOUND).param(ErpQaErrors.ARG_INSPECTION_ID, inspectionId);
        }
        return requireEntity(String.valueOf(inspectionId), null, context);
    }

    private List<ErpQaInspectionLine> loadLines(Long inspectionId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("inspectionId", inspectionId));
        q.addOrderField("lineNo", false);
        return daoFor(ErpQaInspectionLine.class).findAllByQuery(q);
    }

    private Set<Long> applyLineResults(List<ErpQaInspectionLine> lines, List<InspectionLineResultInput> inputs) {
        Set<Long> explicitResultLineIds = new java.util.HashSet<>();
        if (inputs == null || inputs.isEmpty()) {
            return explicitResultLineIds;
        }
        Map<Long, InspectionLineResultInput> byId = new HashMap<>();
        Map<Integer, InspectionLineResultInput> byNo = new HashMap<>();
        for (InspectionLineResultInput in : inputs) {
            if (in.getLineId() != null) {
                byId.put(in.getLineId(), in);
            } else if (in.getLineNo() != null) {
                byNo.put(in.getLineNo(), in);
            }
        }
        for (ErpQaInspectionLine line : lines) {
            InspectionLineResultInput in = byId.get(line.getId());
            if (in == null && line.getLineNo() != null) {
                in = byNo.get(line.getLineNo());
            }
            if (in == null) {
                continue;
            }
            if (in.getMeasuredValue() != null) {
                line.setMeasuredValue(in.getMeasuredValue());
            }
            if (in.getResult() != null) {
                line.setResult(in.getResult());
                explicitResultLineIds.add(line.getId());
            }
        }
        return explicitResultLineIds;
    }

    private NopException illegalInspectionTransition(ErpQaInspection ins, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_INSPECTION_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_INSPECTION_CODE, ins.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }
}
