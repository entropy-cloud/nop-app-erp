package app.erp.prj.service;

import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.dao.entity.ErpPrjTask;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name/*Code + @BizLoader 批量加载）。
 *
 * <p>覆盖 projects 域代表实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long CURRENCY_ID = 9401L;
    static final Long SUBJECT_ID = 9501L;
    static final Long MANAGER_ID = 9601L;
    static final Long CUSTOMER_ID = 9201L;
    static final Long ASSIGNEE_ID = 9202L;
    static final Long PROJECT_TYPE_ID = 9301L;
    static final Long PROJECT_ID = 9001L;
    static final Long TASK1_ID = 9002L;
    static final Long TASK2_ID = 9003L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testProjectFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "项目测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedSubject(SUBJECT_ID, "5001", "在建工程");
            seedEmployee(MANAGER_ID, "张项目经理");
            seedPartner(CUSTOMER_ID, "客户Alpha");
            seedProjectType(PROJECT_TYPE_ID, "项目类型A");
            seedProject(PROJECT_ID, "PRJ-001", "ERP实施项目");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpPrjProject__findList",
                "id", "code", "name", "orgName", "projectTypeName",
                "customerName", "currencyName", "managerName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条项目");
        Map<String, Object> first = rows.get(0);
        assertEquals("项目测试组织", first.get("orgName"));
        assertEquals("项目类型A", first.get("projectTypeName"));
        assertEquals("客户Alpha", first.get("customerName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("张项目经理", first.get("managerName"));
    }

    @Test
    public void testTaskFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "项目测试组织");
            seedCurrency(CURRENCY_ID, "人民币");
            seedEmployee(MANAGER_ID, "张项目经理");
            seedPartner(CUSTOMER_ID, "客户Alpha");
            seedPartner(ASSIGNEE_ID, "负责人Beta");
            seedProjectType(PROJECT_TYPE_ID, "项目类型A");
            seedProject(PROJECT_ID, "PRJ-001", "ERP实施项目");
            seedTask(TASK1_ID, PROJECT_ID, "需求调研", ASSIGNEE_ID, null);
            seedTask(TASK2_ID, PROJECT_ID, "方案设计", ASSIGNEE_ID, TASK1_ID);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpPrjTask__findList",
                "id", "title", "projectName", "assigneeName", "dependsOnTaskName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条任务");
        Map<String, Object> task2 = rows.stream()
                .filter(r -> "方案设计".equals(r.get("title")))
                .findFirst()
                .orElse(null);
        assertNotNull(task2, "存在方案设计任务");
        assertEquals("ERP实施项目", task2.get("projectName"));
        assertEquals("负责人Beta", task2.get("assigneeName"));
        assertEquals("需求调研", task2.get("dependsOnTaskName"));
    }

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

    private void seedCurrency(long id, String name) {
        IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
        ErpMdCurrency c = dao.newEntity();
        c.orm_propValue(1, id);
        c.setCode("CNY");
        c.setName(name);
        dao.saveEntity(c);
    }

    private void seedSubject(long id, String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.orm_propValue(1, id);
        s.setCode(code);
        s.setName(name);
        s.orm_propValueByName("subjectClass", "ASSET");
        s.orm_propValueByName("direction", "DEBIT");
        s.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(s);
    }

    private void seedEmployee(long id, String name) {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee e = dao.newEntity();
        e.orm_propValue(1, id);
        e.setCode("EMP-" + id);
        e.setName(name);
        e.setStatus("ACTIVE");
        dao.saveEntity(e);
    }

    private void seedPartner(long id, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode("CUS-" + id);
        p.setName(name);
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        p.setReceivableBalance(BigDecimal.ZERO);
        p.setPayableBalance(BigDecimal.ZERO);
        dao.saveEntity(p);
    }

    private void seedProjectType(long id, String name) {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        ErpPrjProjectType t = dao.newEntity();
        t.orm_propValue(1, id);
        t.setCode("PT-" + id);
        t.setName(name);
        dao.saveEntity(t);
    }

    private void seedProject(long id, String code, String name) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = dao.newEntity();
        p.orm_propValue(1, id);
        p.setCode(code);
        p.setName(name);
        p.setOrgId(ORG_ID);
        p.setProjectTypeId(PROJECT_TYPE_ID);
        p.setCustomerId(CUSTOMER_ID);
        p.setCurrencyId(CURRENCY_ID);
        p.setManagerId(MANAGER_ID);
        p.setStatus("OPEN");
        dao.saveEntity(p);
    }

    private void seedTask(long id, long projectId, String title, long assigneeId, Long dependsOnId) {
        IEntityDao<ErpPrjTask> dao = daoProvider.daoFor(ErpPrjTask.class);
        ErpPrjTask t = dao.newEntity();
        t.orm_propValue(1, id);
        t.setProjectId(projectId);
        t.setTitle(title);
        t.setAssigneeId(assigneeId);
        if (dependsOnId != null) {
            t.setDependsOnId(dependsOnId);
        }
        t.setStatus("IN_PROGRESS");
        dao.saveEntity(t);
    }
}
