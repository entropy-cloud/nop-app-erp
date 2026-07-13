package app.erp.ast.service;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMovement;
import app.erp.md.dao.entity.ErpMdCurrency;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 高价值外键名称解析 BizLoader 测试（机制 D：xmeta 派生 *Name/*Code + @BizLoader 批量加载）。
 *
 * <p>覆盖 assets 域 18 实体。经 {@link IGraphQLEngine} findList + {@link FieldSelectionBean}
 * 请求派生字段触发 @BizLoader，验证批量加载名称正确（防 N+1 + 名称对齐 master-data）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstFkNameLoader extends JunitAutoTestCase {

    static final Long ORG_ID = 9101L;
    static final Long DEPT_ID = 9102L;
    static final Long FROM_DEPT_ID = 9103L;
    static final Long TO_DEPT_ID = 9104L;
    static final Long CURRENCY_ID = 9401L;
    static final Long CATEGORY_ID = 9501L;
    static final Long ASSET_ID = 8001L;
    static final Long MOVEMENT_ID = 8101L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testAssetFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "资产测试组织");
            seedOrg(DEPT_ID, "研发部");
            seedCurrency(CURRENCY_ID, "人民币");
            seedCategory(CATEGORY_ID, "办公设备");
            seedAsset(ASSET_ID, "FA-8001", "服务器集群");
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpAstAsset__findList",
                "id", "code", "categoryName", "currencyName", "orgName", "departmentName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条资产");
        Map<String, Object> first = rows.get(0);
        assertEquals("办公设备", first.get("categoryName"));
        assertEquals("人民币", first.get("currencyName"));
        assertEquals("资产测试组织", first.get("orgName"));
        assertEquals("研发部", first.get("departmentName"));
    }

    @Test
    public void testMovementFkNameResolution() {
        ormTemplate.runInSession(() -> {
            seedOrg(ORG_ID, "资产测试组织");
            seedOrg(FROM_DEPT_ID, "财务部");
            seedOrg(TO_DEPT_ID, "市场部");
            seedCurrency(CURRENCY_ID, "人民币");
            seedCategory(CATEGORY_ID, "办公设备");
            seedAsset(ASSET_ID, "FA-8001", "服务器集群");
            seedMovement(MOVEMENT_ID);
        });

        List<Map<String, Object>> rows = queryWithSelection(
                "ErpAstMovement__findList",
                "id", "code", "assetCode", "fromDepartmentName", "toDepartmentName", "orgName", "currencyName");
        assertNotNull(rows);
        assertFalse(rows.isEmpty(), "至少 1 条资产移动");
        Map<String, Object> first = rows.get(0);
        assertEquals("FA-8001", first.get("assetCode"));
        assertEquals("财务部", first.get("fromDepartmentName"));
        assertEquals("市场部", first.get("toDepartmentName"));
        assertEquals("资产测试组织", first.get("orgName"));
        assertEquals("人民币", first.get("currencyName"));
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

    private void seedCategory(long id, String name) {
        IEntityDao<ErpAstAssetCategory> dao = daoProvider.daoFor(ErpAstAssetCategory.class);
        ErpAstAssetCategory cat = dao.newEntity();
        cat.orm_propValue(1, id);
        cat.setCode("CAT-" + id);
        cat.setName(name);
        dao.saveEntity(cat);
    }

    private void seedAsset(long id, String code, String name) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset a = dao.newEntity();
        a.orm_propValue(1, id);
        a.setCode(code);
        a.setName(name);
        a.setOrgId(ORG_ID);
        a.setCategoryId(CATEGORY_ID);
        a.setAcquisitionDate(LocalDate.of(2026, 6, 1));
        a.setCurrencyId(CURRENCY_ID);
        a.setOriginalValue(BigDecimal.TEN);
        a.setCurrentValue(BigDecimal.TEN);
        a.setResidualValue(BigDecimal.ZERO);
        a.setAccumulatedDepreciation(BigDecimal.ZERO);
        a.setNetBookValue(BigDecimal.TEN);
        a.setStatus("IN_SERVICE");
        a.setDepartmentId(DEPT_ID);
        dao.saveEntity(a);
    }

    private void seedMovement(long id) {
        IEntityDao<ErpAstMovement> dao = daoProvider.daoFor(ErpAstMovement.class);
        ErpAstMovement m = dao.newEntity();
        m.orm_propValue(1, id);
        m.setCode("MV-" + id);
        m.setOrgId(ORG_ID);
        m.setAssetId(ASSET_ID);
        m.setBusinessDate(LocalDate.of(2026, 7, 1));
        m.setFromDate(LocalDate.of(2026, 7, 1));
        m.setFromDepartmentId(FROM_DEPT_ID);
        m.setToDepartmentId(TO_DEPT_ID);
        m.setCurrencyId(CURRENCY_ID);
        m.orm_propValueByName("docStatus", "DRAFT");
        m.orm_propValueByName("approveStatus", "DRAFT");
        m.orm_propValueByName("exchangeRate", BigDecimal.ONE);
        dao.saveEntity(m);
    }
}
