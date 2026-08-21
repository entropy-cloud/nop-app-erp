package app.erp.md.service.spi;

import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdWarehouse;
import app.erp.md.spi.IErpMdOrganizationReferenceChecker;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoEntity;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 组织业务单据引用计数 SPI 生产实现（plan 2026-07-23-1145-2 Phase 1）。
 *
 * <p>仅扫描 master-data 自有实体（{@link ErpMdEmployee}.orgId / {@link ErpMdWarehouse}.orgId），
 * 不反向依赖下游域，避免 reactor 环。{@code orgId} 作为审计维度的海量噪声按
 * {@code IErpMdOrganizationReferenceChecker} javadoc 裁决排除，仅计语义归属引用。
 *
 * <p>下游域（如 ErpHrDepartment.orgId）的引用计数由各自域追加实现或经 successor 补齐，
 * 本实现提供最高价值子集（master-data 内部归属）。
 */
public class ErpMdOrganizationReferenceChecker implements IErpMdOrganizationReferenceChecker {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public Map<String, Long> countReferences(String organizationId) {
        Map<String, Long> refs = new LinkedHashMap<>();
        if (organizationId == null) {
            return refs;
        }
        refs.put("employee", countByOrg(ErpMdEmployee.class, organizationId));
        refs.put("warehouse", countByOrg(ErpMdWarehouse.class, organizationId));
        return refs;
    }

    private <T extends IDaoEntity> long countByOrg(Class<T> entityClass, String organizationId) {
        IEntityDao<T> dao = daoProvider.daoFor(entityClass);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", organizationId));
        return dao.findAllByQuery(q).size();
    }
}
