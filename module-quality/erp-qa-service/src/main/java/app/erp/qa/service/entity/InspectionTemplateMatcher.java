package app.erp.qa.service.entity;

import app.erp.qa.dao.entity.ErpQaInspectionTemplate;
import app.erp.qa.dao.entity.ErpQaInspectionTemplateLine;
import app.erp.qa.service.ErpQaConfigs;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;

import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 质检模板匹配器。权威：计划 Task Route Decision（模板匹配）+
 * {@code docs/design/quality/state-machine.md §异常路径}。
 *
 * <p>匹配顺序：(1) materialId × inspectionType 匹配 active 模板；(2) 无匹配回落到
 * {@code erp-qua.default-inspection-template} 全局默认模板；(3) 仍无则返回 null（质检单无行，人工补录）。
 *
 * <p>模板行 → 质检单行复制：{@code parameterName/specMin/specMax/unit}（模板行无 parameterId 列，
 * 质检单行 parameterId 留空，见 baseline + Draft Review iter-1 B1）。
 */
public final class InspectionTemplateMatcher {

    private InspectionTemplateMatcher() {
    }

    public static TemplateMatchResult match(IDaoProvider daoProvider, Long materialId, Integer inspectionType) {
        ErpQaInspectionTemplate template = findActiveByMaterialAndType(daoProvider, materialId, inspectionType);
        if (template == null) {
            Long defaultId = ErpQaConfigs.getDefaultInspectionTemplateId();
            if (defaultId != null) {
                template = daoProvider.daoFor(ErpQaInspectionTemplate.class).getEntityById(defaultId);
            }
        }
        if (template == null) {
            return null;
        }
        List<ErpQaInspectionTemplateLine> templateLines = loadTemplateLines(daoProvider, template.getId());
        List<TemplateLineSpec> specs = new ArrayList<>();
        for (ErpQaInspectionTemplateLine tl : templateLines) {
            specs.add(new TemplateLineSpec(tl.getParameterName(), tl.getSpecMin(), tl.getSpecMax(), tl.getUnit()));
        }
        return new TemplateMatchResult(template.getId(), specs);
    }

    private static ErpQaInspectionTemplate findActiveByMaterialAndType(IDaoProvider daoProvider,
                                                                       Long materialId, Integer inspectionType) {
        IEntityDao<ErpQaInspectionTemplate> dao = daoProvider.daoFor(ErpQaInspectionTemplate.class);
        QueryBean q = new QueryBean();
        if (materialId != null) {
            q.addFilter(eq("materialId", materialId));
        }
        if (inspectionType != null) {
            q.addFilter(eq("inspectionType", inspectionType));
        }
        q.addFilter(eq("isActive", 1));
        q.addOrderField("id", false);
        q.setLimit(1);
        List<ErpQaInspectionTemplate> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private static List<ErpQaInspectionTemplateLine> loadTemplateLines(IDaoProvider daoProvider, Long templateId) {
        IEntityDao<ErpQaInspectionTemplateLine> dao = daoProvider.daoFor(ErpQaInspectionTemplateLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("templateId", templateId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }
}
