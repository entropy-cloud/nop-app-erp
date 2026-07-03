package app.erp.qa.service.entity;

import java.util.Collections;
import java.util.List;

/** 模板匹配结果：模板 ID + 复制到质检单行的规格列表。 */
public final class TemplateMatchResult {
    private final Long templateId;
    private final List<TemplateLineSpec> lines;

    public TemplateMatchResult(Long templateId, List<TemplateLineSpec> lines) {
        this.templateId = templateId;
        this.lines = lines == null ? Collections.emptyList() : lines;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public List<TemplateLineSpec> getLines() {
        return lines;
    }
}
