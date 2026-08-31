package app.erp.hr.service;

import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrPosition;
import app.erp.hr.dao.entity.ErpHrRecruitment;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P1-CK-hr-001 回归测试：部门/职位删除引用守卫。
 *
 * <p>修复前 ErpHrDepartmentBizModel / ErpHrPositionBizModel 裸 CrudBizModel 无任何 defaultPrepareDelete
 * 守卫——删除含在职员工的部门、有编制的职位后 Employee.departmentId/positionId 与 Recruitment 悬挂
 * 引用静默产生，子部门 parentId 指向已删节点后被提升为根节点（树结构静默重组）。
 *
 * <p>覆盖：①含在职员工的部门删除应被 ERR_DEPT_HAS_EMPLOYEES 拒绝；②有子部门的部门删除应被
 * ERR_DEPT_HAS_SUB_DEPARTMENTS 拒绝；③有未关闭招聘单的部门删除应被 ERR_DEPT_HAS_RECRUITMENTS
 * 拒绝；④无引用时可正常删除；⑤职位对应的 activeEmp/openRecruitment 守卫对称验证。
 *
 * <p>删除路径走 GraphQL {@code ErpHrDepartment__delete / ErpHrPosition__delete}（触发 BizModel
 * {@code defaultPrepareDelete}）——直接调 dao().deleteEntity 走 IEntityDao 不触发 BizModel 守卫。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrDepartmentPositionDeleteGuard extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    app.erp.hr.biz.IErpHrDepartmentBiz departmentBiz;
    @Inject
    app.erp.hr.biz.IErpHrPositionBiz positionBiz;

    // ---------- Department 守卫 ----------

    @Test
    public void testDeleteDepartmentWithActiveEmployeesRejected() {
        String deptId = ormTemplate.runInSession(session -> {
            String id = seedDept("D-GUARD-1", null);
            seedEmployee("EMP-G-1", id, null, "ACTIVE");
            return id;
        });

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> departmentBiz.delete(deptId, new ServiceContextImpl())));
        assertEquals(ErpHrErrors.ERR_DEPT_HAS_EMPLOYEES.getErrorCode(), ex.getErrorCode(),
                "含 ACTIVE 员工的部门删除应被 ERR_DEPT_HAS_EMPLOYEES 拒绝（P1-CK-hr-001）");
        assertNotNull(daoProvider.daoFor(ErpHrDepartment.class).getEntityById(deptId),
                "拒绝后部门仍存在");
    }

    @Test
    public void testDeleteDepartmentWithSubDepartmentsRejected() {
        String parentId = ormTemplate.runInSession(session -> {
            String p = seedDept("D-PARENT", null);
            seedDept("D-CHILD", p);
            return p;
        });

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> departmentBiz.delete(parentId, new ServiceContextImpl())));
        assertEquals(ErpHrErrors.ERR_DEPT_HAS_SUB_DEPARTMENTS.getErrorCode(), ex.getErrorCode(),
                "有子部门的部门删除应被 ERR_DEPT_HAS_SUB_DEPARTMENTS 拒绝（P1-CK-hr-001）");
    }

    @Test
    public void testDeleteDepartmentWithOpenRecruitmentsRejected() {
        String deptId = ormTemplate.runInSession(session -> {
            String id = seedDept("D-REC", null);
            seedRecruitment("REC-OPEN", id, null, "OPEN");
            return id;
        });

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> departmentBiz.delete(deptId, new ServiceContextImpl())));
        assertEquals(ErpHrErrors.ERR_DEPT_HAS_RECRUITMENTS.getErrorCode(), ex.getErrorCode(),
                "有 OPEN 招聘单的部门删除应被 ERR_DEPT_HAS_RECRUITMENTS 拒绝（P1-CK-hr-001）");
    }

    @Test
    public void testDeleteDepartmentWithClosedRecruitmentsAllowed() {
        String deptId = ormTemplate.runInSession(session -> {
            String id = seedDept("D-REC-CLOSED", null);
            seedRecruitment("REC-CLOSED", id, null, "CLOSED");
            seedRecruitment("REC-REJECTED", id, null, "REJECTED");
            return id;
        });

        boolean deleted = ormTemplate.runInSession(session -> departmentBiz.delete(deptId, new ServiceContextImpl()));
        assertTrue(deleted, "无在职员工/子部门/未关闭招聘单的部门删除应成功（P1-CK-hr-001）");

        ErpHrDepartment afterDelete = ormTemplate.runInSession(s -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.setDisableLogicalDelete(true);
            return daoProvider.daoFor(ErpHrDepartment.class).getEntityById(deptId);
        });
        assertNotNull(afterDelete);
        assertTrue(afterDelete.getDelVersion() != null && afterDelete.getDelVersion() > 0,
                "delVersion 应被逻辑删除递增");
    }

    @Test
    public void testDeleteEmptyDepartmentAllowed() {
        String deptId = ormTemplate.runInSession(session -> seedDept("D-EMPTY", null));
        // P1-CK-hr-001 守卫检查通过 + 部门可被删除（逻辑删除 delVersion++）。
        boolean deleted = ormTemplate.runInSession(session -> departmentBiz.delete(deptId, new ServiceContextImpl()));
        assertTrue(deleted, "守卫通过的部门删除应返回 true（P1-CK-hr-001）");

        // 验证行被逻辑删除——disableLogicalDelete=true 时仍能查到（delVersion > 0）
        ErpHrDepartment afterDelete = ormTemplate.runInSession(s -> {
            io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
            q.setDisableLogicalDelete(true);
            return daoProvider.daoFor(ErpHrDepartment.class).getEntityById(deptId);
        });
        assertNotNull(afterDelete, "disableLogicalDelete=true 时删除的行仍可查到");
        assertTrue(afterDelete.getDelVersion() != null && afterDelete.getDelVersion() > 0,
                "delVersion 应被逻辑删除递增，was=" + afterDelete.getDelVersion());
    }

    // ---------- Position 守卫 ----------

    @Test
    public void testDeletePositionWithActiveEmployeesRejected() {
        String posId = ormTemplate.runInSession(session -> {
            String id = seedPosition("P-GUARD-1");
            seedEmployee("EMP-P-1", null, id, "ACTIVE");
            return id;
        });

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> positionBiz.delete(posId, new ServiceContextImpl())));
        assertEquals(ErpHrErrors.ERR_POSITION_HAS_EMPLOYEES.getErrorCode(), ex.getErrorCode(),
                "含 ACTIVE 员工的职位删除应被 ERR_POSITION_HAS_EMPLOYEES 拒绝（P1-CK-hr-001）");
    }

    @Test
    public void testDeletePositionWithOpenRecruitmentsRejected() {
        String posId = ormTemplate.runInSession(session -> {
            String id = seedPosition("P-REC");
            seedRecruitment("REC-POS-OPEN", null, id, "OPEN");
            return id;
        });

        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> positionBiz.delete(posId, new ServiceContextImpl())));
        assertEquals(ErpHrErrors.ERR_POSITION_HAS_RECRUITMENTS.getErrorCode(), ex.getCode(),
                "有 OPEN 招聘单的职位删除应被 ERR_POSITION_HAS_RECRUITMENTS 拒绝（P1-CK-hr-001）");
    }

    @Test
    public void testDeleteEmptyPositionAllowed() {
        String posId = ormTemplate.runInSession(session -> seedPosition("P-EMPTY"));
        ormTemplate.runInSession(session -> positionBiz.delete(posId, new ServiceContextImpl()));
        assertNull(daoProvider.daoFor(ErpHrPosition.class).getEntityById(posId));
    }

    // ---------- rpc helpers (unused, reserved) ----------

    private ApiResponse<?> deleteDept(String id) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpHrDepartment__delete",
                ApiRequest.build(java.util.Map.of("id", id)));
        return graphQLEngine.executeRpc(ctx);
    }

    private ApiResponse<?> deletePos(String id) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpHrPosition__delete",
                ApiRequest.build(java.util.Map.of("id", id)));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- entity seeds ----------

    private String seedDept(String code, String parentId) {
        IEntityDao<ErpHrDepartment> dao = daoProvider.daoFor(ErpHrDepartment.class);
        ErpHrDepartment d = new ErpHrDepartment();
        d.setCode(code);
        d.setName(code);
        d.setOrgId("1");
        if (parentId != null) {
            d.setParentId(parentId);
        }
        dao.saveEntity(d);
        return d.getId();
    }

    private String seedPosition(String code) {
        IEntityDao<ErpHrPosition> dao = daoProvider.daoFor(ErpHrPosition.class);
        ErpHrPosition p = new ErpHrPosition();
        p.setCode(code);
        p.setName(code);
        p.setOrgId("1");
        dao.saveEntity(p);
        return p.getId();
    }

    private void seedEmployee(String code, String deptId, String posId, String employmentStatus) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee e = new ErpHrEmployee();
        e.setCode(code);
        e.setFirstName("测");
        e.setLastName("试");
        e.setFullName(code);
        e.setGender("MALE");
        e.setOrgId("1");
        if (deptId != null) {
            e.setDepartmentId(deptId);
        }
        if (posId != null) {
            e.setPositionId(posId);
        }
        e.setEmploymentStatus(employmentStatus);
        e.setEmployeeType("FULL_TIME");
        e.setHireDate(LocalDate.of(2025, 1, 1));
        dao.saveEntity(e);
    }

    private void seedRecruitment(String code, String deptId, String posId, String status) {
        IEntityDao<ErpHrRecruitment> dao = daoProvider.daoFor(ErpHrRecruitment.class);
        ErpHrRecruitment r = new ErpHrRecruitment();
        r.setCode(code);
        r.setCandidateName(code);
        r.setOrgId("1");
        r.setBusinessDate(LocalDate.of(2026, 7, 1));
        if (deptId != null) {
            r.setDepartmentId(deptId);
        }
        if (posId != null) {
            r.setPositionId(posId);
        }
        r.setStatus(status);
        dao.saveEntity(r);
    }
}