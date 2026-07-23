package app.erp.md.service;

import app.erp.md.biz.IErpMdOrganizationBiz;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdWarehouse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F7 §3 {@code ErpMdOrganizationBizModel.countReferences} 后端测试（plan 2026-07-23-1145-2 Phase 3）。
 *
 * <p>生产 SPI 实现 {@code ErpMdOrganizationReferenceChecker}（master-data 自有，注册于 app-service.beans.xml）
 * 扫描 ErpMdEmployee.orgId + ErpMdWarehouse.orgId，返回真实计数。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdOrganizationBiz extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpMdOrganizationBiz organizationBiz;

    @Test
    public void testCountReferencesReturnsRealData() {
        long orgId = 5301L;
        long emptyOrgId = 5302L;
        ormTemplate.runInSession(() -> {
            seedOrganization(orgId, "O-REF-1", "引用组织1");
            seedOrganization(emptyOrgId, "O-REF-EMPTY", "无引用组织");
            seedEmployee(6301L, "E-REF-1", orgId);
            seedEmployee(6302L, "E-REF-2", orgId);
            seedWarehouse(7301L, "W-REF-1", orgId);
        });

        // 有引用组织：employee=2, warehouse=1
        @SuppressWarnings("unchecked")
        Map<String, Long> refs = (Map<String, Long>) organizationBiz.countReferences(orgId, CTX);
        assertTrue(refs != null && !refs.isEmpty(), "有引用组织应返回非空 Map");
        assertEquals(2L, refs.get("employee"), "应统计 2 个归属员工");
        assertEquals(1L, refs.get("warehouse"), "应统计 1 个归属仓库");

        // 无引用组织：返回全 0 计数（SPI 仍返回键，值为 0）
        @SuppressWarnings("unchecked")
        Map<String, Long> emptyRefs = (Map<String, Long>) organizationBiz.countReferences(emptyOrgId, CTX);
        assertEquals(0L, emptyRefs.get("employee"), "无引用组织 employee 计数应为 0");
        assertEquals(0L, emptyRefs.get("warehouse"), "无引用组织 warehouse 计数应为 0");
    }

    // ---------- helpers ----------

    private void seedOrganization(long id, String code, String name) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode(code);
        o.setName(name);
        o.setStatus("ACTIVE");
        o.setOrgType("COMPANY");
        dao.saveEntity(o);
    }

    private void seedEmployee(long id, String code, long orgId) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee e = dao.newEntity();
        e.orm_propValue(1, id);
        e.setCode(code);
        e.setName("员工-" + code);
        e.setStatus("ACTIVE");
        e.setOrgId(orgId);
        dao.saveEntity(e);
    }

    private void seedWarehouse(long id, String code, long orgId) {
        IEntityDao<ErpMdWarehouse> dao = daoProvider.daoFor(ErpMdWarehouse.class);
        ErpMdWarehouse w = dao.newEntity();
        w.orm_propValue(1, id);
        w.setCode(code);
        w.setName("仓库-" + code);
        w.setStatus("ACTIVE");
        w.setOrgId(orgId);
        dao.saveEntity(w);
    }
}
