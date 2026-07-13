package app.erp.hr.service;

import app.erp.hr.dao.entity.ErpHrDepartment;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrPosition;
import app.erp.hr.dao.entity.ErpHrRecruitment;
import app.erp.hr.dao.entity.ErpHrShift;
import app.erp.hr.dao.entity.ErpHrShiftAssignment;
import app.erp.md.dao.entity.ErpMdCostCenter;
import app.erp.md.dao.entity.ErpMdOrganization;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.FieldSelectionBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name/*Code + @BizLoader 批量加载）。
 *
 * <p>覆盖 HR 域核心实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 7101L;
    static final Long DEPT_ID = 7201L;
    static final Long POS_ID = 7301L;
    static final Long COST_CENTER_ID = 7401L;
    static final Long EMPLOYEE_ID = 7501L;
    static final Long SUPERIOR_ID = 7502L;
    static final Long INTERVIEWER_ID = 7503L;
    static final Long SHIFT_ID = 7601L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testEmployeeFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "HR测试组织");
            seedCostCenter(COST_CENTER_ID, "研发成本中心");
            seedDepartment(DEPT_ID, "研发部");
            seedPosition(POS_ID, "高级工程师");
            seedEmployee(SUPERIOR_ID, "张", "主管", "张主管");
            seedEmployee(EMPLOYEE_ID, "李", "员工", "李员工");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpHrEmployee__findList",
                "id", "departmentName", "positionName", "superiorDisplayName", "costCenterName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条员工");
        Map<String, Object> target = rows.stream()
                .filter(r -> EMPLOYEE_ID.equals(r.get("id")))
                .findFirst()
                .orElse(rows.get(0));
        assertEquals("研发部", target.get("departmentName"));
        assertEquals("高级工程师", target.get("positionName"));
        assertEquals("张主管", target.get("superiorDisplayName"));
        assertEquals("研发成本中心", target.get("costCenterName"));
    }

    @Test
    public void testRecruitmentFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "HR测试组织");
            seedDepartment(DEPT_ID, "研发部");
            seedPosition(POS_ID, "高级工程师");
            seedEmployee(INTERVIEWER_ID, "王", "面试官", "王面试官");
            seedRecruitment(7701L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpHrRecruitment__findList",
                "id", "positionName", "departmentName", "interviewerDisplayName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条招聘");
        Map<String, Object> first = rows.get(0);
        assertEquals("高级工程师", first.get("positionName"));
        assertEquals("研发部", first.get("departmentName"));
        assertEquals("王面试官", first.get("interviewerDisplayName"));
    }

    @Test
    public void testShiftAssignmentFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "HR测试组织");
            seedEmployee(EMPLOYEE_ID, "李", "员工", "李员工");
            seedShift(SHIFT_ID, "早班");
            seedShiftAssignment(7801L);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpHrShiftAssignment__findList",
                "id", "shiftName", "employeeDisplayName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条排班");
        Map<String, Object> first = rows.get(0);
        assertEquals("早班", first.get("shiftName"));
        assertEquals("李员工", first.get("employeeDisplayName"));
    }

    // ---------- query helper ----------

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> queryWithSelection(String action, String... fields) {
        FieldSelectionBean selection = new FieldSelectionBean();
        for (String f : fields) {
            selection.addField(f);
        }
        ApiRequest<?> request = ApiRequest.build(Map.of());
        request.setSelection(selection);
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(
                GraphQLOperationType.query, action, request);
        ApiResponse<?> resp = graphQLEngine.executeRpc(ctx);
        assertEquals(0, resp.getStatus(), action + " 查询成功");
        Object data = resp.getData();
        if (data instanceof List) {
            return (List<Map<String, Object>>) data;
        }
        return (List<Map<String, Object>>) ((Map<?, ?>) data).get("items");
    }

    // ---------- seed helpers ----------

    private void seedOrg(long id, String name) {
        IEntityDao<ErpMdOrganization> dao = daoProvider.daoFor(ErpMdOrganization.class);
        ErpMdOrganization o = dao.newEntity();
        o.orm_propValue(1, id);
        o.setCode("ORG-" + id);
        o.setName(name);
        o.setOrgType("COMPANY");
        o.setStatus("ACTIVE");
        dao.saveEntity(o);
    }

    private void seedCostCenter(long id, String name) {
        IEntityDao<ErpMdCostCenter> dao = daoProvider.daoFor(ErpMdCostCenter.class);
        ErpMdCostCenter c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CC-" + id);
        c.setName(name);
        c.setOrgId(ORG_ID);
        c.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(c);
    }

    private void seedDepartment(long id, String name) {
        IEntityDao<ErpHrDepartment> dao = daoProvider.daoFor(ErpHrDepartment.class);
        ErpHrDepartment d = dao.newEntity();
        d.orm_propValue(1, id);
        d.setCode("DEPT-" + id);
        d.setName(name);
        dao.saveEntity(d);
    }

    private void seedPosition(long id, String name) {
        IEntityDao<ErpHrPosition> dao = daoProvider.daoFor(ErpHrPosition.class);
        ErpHrPosition p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("POS-" + id);
        p.setName(name);
        dao.saveEntity(p);
    }

    private void seedEmployee(long id, String firstName, String lastName, String fullName) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee e = dao.newEntity();
        e.orm_propValue(1, id);
        e.setCode("EMP-" + id);
        e.setFirstName(firstName);
        e.setLastName(lastName);
        e.setFullName(fullName);
        e.orm_propValueByName("gender", "MALE");
        e.setHireDate(LocalDate.of(2026, 1, 1));
        e.orm_propValueByName("employmentStatus", "ACTIVE");
        e.orm_propValueByName("employeeType", "FULL_TIME");
        e.setOrgId(ORG_ID);
        e.setDepartmentId(DEPT_ID);
        e.setPositionId(POS_ID);
        e.setCostCenterId(COST_CENTER_ID);
        if (id == EMPLOYEE_ID) {
            e.setSuperiorId(SUPERIOR_ID);
        }
        dao.saveEntity(e);
    }

    private void seedRecruitment(long id) {
        IEntityDao<ErpHrRecruitment> dao = daoProvider.daoFor(ErpHrRecruitment.class);
        ErpHrRecruitment r = dao.newEntity();
        r.orm_propValue(1, id);
        r.setCode("REC-" + id);
        r.orm_propValueByName("candidateName", "候选人甲");
        r.orm_propValueByName("status", "OPEN");
        r.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        r.setPositionId(POS_ID);
        r.setDepartmentId(DEPT_ID);
        r.setInterviewerId(INTERVIEWER_ID);
        r.setOrgId(ORG_ID);
        dao.saveEntity(r);
    }

    private void seedShift(long id, String name) {
        IEntityDao<ErpHrShift> dao = daoProvider.daoFor(ErpHrShift.class);
        ErpHrShift s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode("SHIFT-" + id);
        s.setName(name);
        s.orm_propValueByName("shiftType", "FIXED");
        s.orm_propValueByName("startTime", "09:00");
        s.orm_propValueByName("endTime", "18:00");
        s.setOrgId(ORG_ID);
        dao.saveEntity(s);
    }

    private void seedShiftAssignment(long id) {
        IEntityDao<ErpHrShiftAssignment> dao = daoProvider.daoFor(ErpHrShiftAssignment.class);
        ErpHrShiftAssignment a = dao.newEntity();
        a.orm_propValue(1, id);
        a.setOrgId(ORG_ID);
        a.setEmployeeId(EMPLOYEE_ID);
        a.setShiftId(SHIFT_ID);
        a.orm_propValueByName("assignmentDate", LocalDate.of(2026, 7, 1));
        a.orm_propValueByName("status", "SCHEDULED");
        a.orm_propValueByName("businessDate", LocalDate.of(2026, 7, 1));
        dao.saveEntity(a);
    }
}
