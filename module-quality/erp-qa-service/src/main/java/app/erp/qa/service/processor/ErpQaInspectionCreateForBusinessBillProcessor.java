package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.entity.InspectionTemplateMatcher;
import app.erp.qa.service.entity.TemplateLineSpec;
import app.erp.qa.service.entity.TemplateMatchResult;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import java.math.BigDecimal;

/**
 * ErpQaInspection createForBusinessBill per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含业务单据触发的质检单创建编排：模板匹配 → 建质检单 → 复制模板行（{@code docs/design/quality/state-machine.md §异常路径`}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaInspectionProcessor}。
 */
public class ErpQaInspectionCreateForBusinessBillProcessor extends AbstractErpQaInspectionProcessor {

    public ErpQaInspection createForBusinessBill(String billType, String billCode, Long materialId, String inspectionType,
                                                 BigDecimal lotQuantity, Long supplierId, Long warehouseId,
                                                 String batchNo, IServiceContext context) {
        // 模板匹配：materialId × inspectionType → active 模板；无匹配走全局默认；仍无则无行（人工补录）
        TemplateMatchResult match = InspectionTemplateMatcher.match(daoProvider, materialId, inspectionType);

        ErpQaInspection inspection = inspectionDao().newEntity();
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
        inspection.setInspectionDate(CoreMetrics.today());
        inspection.setBusinessDate(CoreMetrics.today());
        if (match != null) {
            inspection.setTemplateId(match.getTemplateId());
        }
        inspectionDao().saveEntity(inspection);

        // 模板行复制到质检单行（模板行无 parameterId，质检单行 parameterId 留空）
        if (match != null) {
            copyTemplateLinesToInspection(inspection.getId(), match);
        }
        return inspection;
    }

    private void copyTemplateLinesToInspection(Long inspectionId, TemplateMatchResult match) {
        IEntityDao<ErpQaInspectionLine> lineDao = lineDao();
        int lineNo = 1;
        for (TemplateLineSpec spec : match.getLines()) {
            ErpQaInspectionLine line = lineDao.newEntity();
            line.setInspectionId(inspectionId);
            line.setLineNo(lineNo++);
            line.setParameterName(spec.getParameterName());
            line.setSpecMin(spec.getSpecMin());
            line.setSpecMax(spec.getSpecMax());
            line.setUnit(spec.getUnit());
            line.setIsCritical(spec.getIsCritical());
            line.setResult(ErpQaConstants.INSPECTION_RESULT_PENDING);
            lineDao.saveEntity(line);
        }
    }

    private String generateCode(String billType, String billCode) {
        return "INS-" + billType + "-" + CoreMetrics.currentTimeMillis();
    }
}
