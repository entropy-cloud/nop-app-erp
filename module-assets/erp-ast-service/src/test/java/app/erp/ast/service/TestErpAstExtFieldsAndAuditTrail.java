package app.erp.ast.service;

import app.erp.ast.biz.IErpAstAssetBiz;
import app.erp.ast.biz.IErpAstAssetModelBiz;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdLocation;
import app.erp.md.dao.entity.ErpMdOrganization;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetActionLog;
import app.erp.ast.dao.entity.ErpAstAssetModel;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * E3.3 型号级 ext 字段集 + E3.8 审计轨迹 Proof（`audit-trail-and-custom-fieldsets.md` §1/§2）：
 * <ul>
 *   <li>ext 字段按型号声明校验负路径（未指定型号携带值 / 非法键 / 缺必填 / 类型不匹配）+ 正路径；</li>
 *   <li>审计事件覆盖：CREATE（save 钩子）/ STATUS_CHANGE（update diff + suspend/resume）/
 *       TRANSFER（归属字段 diff）/ VALUATION（currentValue diff）/ UPDATE（信息字段 diff）；</li>
 *   <li>{@code getAssetAuditTrail} 时间轴倒序 + from/to 快照 + GraphQL 可查。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstExtFieldsAndAuditTrail extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstAssetBiz assetBiz;
    @Inject
    IErpAstAssetModelBiz modelBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- E3.3 ext 字段集校验 ----------

    @Test
    public void testExtFieldValidationHappyPath() {
        String modelId = seedModel("MDL-EXT-1", "[{\"key\":\"cpu\",\"label\":\"CPU\",\"type\":\"string\",\"required\":true},"
                + "{\"key\":\"ramGb\",\"label\":\"内存GB\",\"type\":\"number\",\"required\":true},"
                + "{\"key\":\"ssd\",\"label\":\"SSD\",\"type\":\"boolean\",\"required\":false}]");
        ErpAstAsset asset = seedAsset("AST-EXT-1", modelId,
                "{\"cpu\":\"i7\",\"ramGb\":32,\"ssd\":true}");

        // 无校验错误即通过；再验证负路径改值被拒
        ApiResponse<?> bad = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "extFieldValues", "{\"cpu\":\"i7\",\"unknownKey\":1}")))));
        assertNotEquals(0, bad.getStatus(), "非法键应被拒绝");
        assertEquals(ErpAstErrors.ERR_AST_EXT_FIELD_NOT_DECLARED.getErrorCode(), bad.getCode());
    }

    @Test
    public void testExtFieldWithoutModelRejected() {
        ErpAstAsset asset = seedAsset("AST-EXT-2", null, null);
        ApiResponse<?> bad = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "extFieldValues", "{\"any\":\"value\"}")))));
        assertNotEquals(0, bad.getStatus(), "未指定型号不允许携带扩展字段值");
        assertEquals(ErpAstErrors.ERR_AST_EXT_FIELD_WITHOUT_MODEL.getErrorCode(), bad.getCode());
    }

    @Test
    public void testExtFieldRequiredMissingRejected() {
        String modelId = seedModel("MDL-EXT-2", "[{\"key\":\"cpu\",\"label\":\"CPU\",\"type\":\"string\",\"required\":true}]");
        ErpAstAsset asset = seedAsset("AST-EXT-3", modelId, "{\"cpu\":\"i5\"}");

        ApiResponse<?> bad = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "extFieldValues", "{}")))));
        assertNotEquals(0, bad.getStatus(), "缺必填扩展字段应被拒绝");
        assertEquals(ErpAstErrors.ERR_AST_EXT_FIELD_REQUIRED_MISSING.getErrorCode(), bad.getCode());
    }

    @Test
    public void testExtFieldTypeMismatchRejected() {
        String modelId = seedModel("MDL-EXT-3", "[{\"key\":\"ramGb\",\"label\":\"内存GB\",\"type\":\"number\",\"required\":true}]");
        ErpAstAsset asset = seedAsset("AST-EXT-4", modelId, "{\"ramGb\":32}");

        ApiResponse<?> bad = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "extFieldValues", "{\"ramGb\":\"thirty-two\"}")))));
        assertNotEquals(0, bad.getStatus(), "类型不匹配应被拒绝");
        assertEquals(ErpAstErrors.ERR_AST_EXT_FIELD_TYPE_MISMATCH.getErrorCode(), bad.getCode());
    }

    // ---------- E3.8 审计轨迹 ----------

    @Test
    public void testAuditTrailCoversLifecycleEvents() {
        seedOrgRefs();
        ErpAstAsset asset = seedAsset("AST-AUD-1", null, null);

        // STATUS_CHANGE：update diff（DRAFT→IN_SERVICE）
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "status", "IN_SERVICE"))))).getStatus());

        // TRANSFER：归属字段 diff（部门变更）
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "departmentId", "9001"))))).getStatus());

        // VALUATION：currentValue diff
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "currentValue", new BigDecimal("9000")))))).getStatus());

        // UPDATE：信息字段 diff（name）
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "name", "改名服务器"))))).getStatus());

        // STATUS_CHANGE：suspend/resume（processor 路径；对齐 TestErpAstIdleStateMachine 的 session 包裹范式）
        ormTemplate.runInSession(sess -> assetBiz.suspend(asset.getId(), CTX));
        ormTemplate.runInSession(sess -> assetBiz.resume(asset.getId(), CTX));

        List<Map<String, Object>> trail = assetBiz.getAssetAuditTrail(asset.getId(), CTX);
        assertEquals(7, trail.size(), "events=" + trail.stream().map(r -> String.valueOf(r.get("eventType")) + "@" + r.get("createTime") + "#" + r.get("id")).collect(java.util.stream.Collectors.toList()));

        // 倒序：最新事件在前（resume）
        assertEquals("STATUS_CHANGE", trail.get(0).get("eventType"));
        assertEquals("IN_SERVICE", trail.get(0).get("toStatus"));
        assertEquals("IDLE", trail.get(0).get("fromStatus"));

        // 正序检查各事件存在
        java.util.Set<String> types = new java.util.HashSet<>();
        for (Map<String, Object> row : trail) {
            types.add(String.valueOf(row.get("eventType")));
        }
        assertTrue(types.contains("CREATE"));
        assertTrue(types.contains("STATUS_CHANGE"));
        assertTrue(types.contains("TRANSFER"));
        assertTrue(types.contains("VALUATION"));
        assertTrue(types.contains("UPDATE"));

        // CREATE 无 from 快照
        Map<String, Object> create = trail.stream()
                .filter(r -> "CREATE".equals(r.get("eventType"))).findFirst().orElseThrow();
        assertEquals(null, create.get("fromStatus"));
        assertEquals("DRAFT", create.get("toStatus"));

        // GraphQL 可查
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(query,
                "ErpAstAsset__getAssetAuditTrail", ApiRequest.build(Map.of("assetId", asset.getId()))));
        assertEquals(0, resp.getStatus());
        assertNotNull(resp.getData());
    }

    @Test
    public void testAuditRowsPersistedWithFromToSnapshots() {
        seedOrgRefs();
        ErpAstAsset asset = seedAsset("AST-AUD-2", null, null);
        ormTemplate.flushSession();
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "status", "IN_SERVICE",
                        "locationId", "9101", "employeeId", "9201"))))).getStatus());

        IEntityDao<ErpAstAssetActionLog> dao = daoProvider.daoFor(ErpAstAssetActionLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", asset.getId()));
        List<ErpAstAssetActionLog> logs = dao.findAllByQuery(q);
        assertTrue(logs.size() >= 3, "CREATE + STATUS_CHANGE + TRANSFER 均落审计行");

        ErpAstAssetActionLog transfer = logs.stream()
                .filter(l -> "TRANSFER".equals(l.getEventType())).findFirst().orElseThrow();
        assertEquals("9101", transfer.getToLocationId());
        assertEquals("9201", transfer.getToStaffId());
        assertEquals(null, transfer.getFromLocationId());
    }

    // ---------- seeds ----------

    private void seedOrgRefs() {
        ormTemplate.runInSession(sess -> {
            IEntityDao<ErpMdOrganization> orgDao = daoProvider.daoFor(ErpMdOrganization.class);
            if (orgDao.getEntityById("9001") == null) {
                ErpMdOrganization o = orgDao.newEntity();
                o.orm_propValue(1, "9001");
                o.setCode("ORG-9001");
                o.setName("审计测试部门");
                o.setOrgType("COMPANY");
                o.setStatus("ACTIVE");
                orgDao.saveEntity(o);
            }
            IEntityDao<ErpMdLocation> locDao = daoProvider.daoFor(ErpMdLocation.class);
            if (locDao.getEntityById("9101") == null) {
                ErpMdLocation l = locDao.newEntity();
                l.orm_propValue(1, "9101");
                l.setWarehouseId("1");
                l.setCode("LOC-9101");
                l.setName("审计测试地点");
                locDao.saveEntity(l);
            }
            IEntityDao<ErpMdEmployee> empDao = daoProvider.daoFor(ErpMdEmployee.class);
            if (empDao.getEntityById("9201") == null) {
                ErpMdEmployee e = empDao.newEntity();
                e.orm_propValue(1, "9201");
                e.setCode("EMP-9201");
                e.setName("审计测试使用人");
                e.setOrgId("9001");
                e.setStatus("1");
                empDao.saveEntity(e);
            }
            return null;
        });
    }

    private void seedCurrency() {
        ormTemplate.runInSession(sess -> {
            IEntityDao<ErpMdCurrency> dao = daoProvider.daoFor(ErpMdCurrency.class);
            if (dao.getEntityById("1") == null) {
                ErpMdCurrency c = dao.newEntity();
                c.orm_propValue(1, "1");
                c.setCode("CNY");
                c.setName("人民币");
                dao.saveEntity(c);
            }
            return null;
        });
    }

    private String seedModel(String code, String extFieldDefs) {
        return ormTemplate.runInSession(sess -> {
            ErpAstAssetModel m = modelBiz.newEntity();
            m.setCode(code);
            m.setName(code + "-name");
            m.setExtFieldDefs(extFieldDefs);
            modelBiz.saveEntity(m, null, CTX);
            return m.getId();
        });
    }

    private ErpAstAsset seedAsset(String code, String modelId, String extFieldValues) {
        seedCurrency();
        return ormTemplate.runInSession(sess -> {
            Map<String, Object> data = new HashMap<>();
            data.put("code", code);
            data.put("name", code + "-name");
            data.put("acquisitionDate", LocalDate.of(2026, 7, 1).toString());
            data.put("originalValue", new BigDecimal("10000"));
            data.put("currentValue", new BigDecimal("10000"));
            data.put("status", "DRAFT");
            data.put("currencyId", "1");
            if (modelId != null) {
                data.put("modelId", modelId);
            }
            if (extFieldValues != null) {
                data.put("extFieldValues", extFieldValues);
            }
            ErpAstAsset saved = assetBiz.save(data, CTX);
            return saved;
        });
    }
}
