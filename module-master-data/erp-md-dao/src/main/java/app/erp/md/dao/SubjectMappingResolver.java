package app.erp.md.dao;

import app.erp.md.dao.entity.ErpMdSubjectMapping;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 跨账套科目映射解析器。在多套账传播时，将源账套的科目翻译为目标账套的科目。
 *
 * <p>映射规则：按 (sourceSubjectId, targetAcctSchemaId) 查找 {@link ErpMdSubjectMapping}。
 * 无映射记录时回退源科目本身（即所有账套共享同一科目表，适用于多数场景）。
 *
 * <p>权威：{@code docs/design/finance/multiple-accounting-schemas.md §科目映射规则}。
 */
public class SubjectMappingResolver {

    @Inject
    IDaoProvider daoProvider;

    /**
     * 批量解析目标账套的科目映射。
     *
     * @param sourceSubjectIds  源科目 ID 集合
     * @param targetAcctSchemaId 目标账套 ID
     * @return 映射表：sourceSubjectId → targetSubjectId（无映射时 targetSubjectId = sourceSubjectId）
     */
    public Map<Long, Long> resolveMappings(List<Long> sourceSubjectIds, Long targetAcctSchemaId) {
        Map<Long, Long> result = new HashMap<>();
        if (sourceSubjectIds == null || sourceSubjectIds.isEmpty() || targetAcctSchemaId == null) {
            return result;
        }
        for (Long sid : sourceSubjectIds) {
            result.put(sid, sid);
        }
        IEntityDao<ErpMdSubjectMapping> dao = daoProvider.daoFor(ErpMdSubjectMapping.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("targetAcctSchemaId", targetAcctSchemaId));
        List<ErpMdSubjectMapping> mappings = dao.findAllByQuery(q);
        for (ErpMdSubjectMapping m : mappings) {
            if (m.getSourceSubjectId() != null && m.getTargetSubjectId() != null) {
                result.put(m.getSourceSubjectId(), m.getTargetSubjectId());
            }
        }
        return result;
    }
}
