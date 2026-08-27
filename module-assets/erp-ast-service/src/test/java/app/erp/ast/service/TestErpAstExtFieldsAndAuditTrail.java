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
import io.nop.api.core.exceptions.NopException;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    // ---------- P2-4 财务敏感字段 UPDATE 审计 + P2-5 逻辑删除型号双路径守卫（plan 2026-08-28-0219-2） ----------

    @Test
    public void testFinancialFieldChangeAuditedAsUpdateWithFieldNames() {
        ErpAstAsset asset = seedAsset("AST-AUD-FIN", null, null);

        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(),
                        "depreciationMethod", "DECLINING",
                        "residualValue", new BigDecimal("500"),
                        "acquisitionDate", "2026-07-15"))))).getStatus(),
                "财务敏感字段变更应保存成功");

        IEntityDao<ErpAstAssetActionLog> dao = daoProvider.daoFor(ErpAstAssetActionLog.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", asset.getId()));
        q.addFilter(eq("eventType", "UPDATE"));
        List<ErpAstAssetActionLog> updates = dao.findAllByQuery(q);
        assertEquals(1, updates.size(), "财务字段变更产生 1 条 UPDATE 审计事件");
        String summary = updates.get(0).getSummary();
        assertTrue(summary.contains("depreciationMethod"), "remark 带变更字段名 depreciationMethod: " + summary);
        assertTrue(summary.contains("residualValue"), "remark 带变更字段名 residualValue: " + summary);
        assertTrue(summary.contains("acquisitionDate"), "remark 带变更字段名 acquisitionDate: " + summary);
    }

    @Test
    public void testNewBindingToDeletedModelRejected() {
        // 双层守卫（执行发现登记，见计划执行注记）：平台 ObjMetaBasedValidator 对 ext:relation FK 的
        // deleted-ref 预检先于 BizModel 钩子——标准 Map/GraphQL 入口以平台通用码拒绝已删型号绑定
        // （审计 P2-5 前半句「可被绑定」对标准入口实测不成立）；钩子层 ERR_AST_ASSET_MODEL_DELETED
        // 为域内语义兜底（存量豁免同方法落地，防 meta 漂移移除 ext:relation 预检或内部构造路径）。
        String modelId = seedModel("MDL-DEL-1", "[{\"key\":\"cpu\",\"label\":\"CPU\",\"type\":\"string\",\"required\":true}]");
        deleteModel(modelId);

        // 新绑定路径 ①：save 携带已删 modelId → 拒绝（错误码断言；biz 直调与 GraphQL 共用同一校验管道）
        seedCurrency();
        Map<String, Object> data = new HashMap<>();
        data.put("code", "AST-DEL-M1");
        data.put("name", "AST-DEL-M1-name");
        data.put("acquisitionDate", LocalDate.of(2026, 7, 1).toString());
        data.put("originalValue", new BigDecimal("10000"));
        data.put("currentValue", new BigDecimal("10000"));
        data.put("status", "DRAFT");
        data.put("currencyId", "1");
        data.put("modelId", modelId);
        NopException saveEx = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(sess -> assetBiz.save(new HashMap<>(data), CTX)),
                "新资产绑定已删型号应被拒绝");
        assertEquals("nop.err.dao.unknown-entity", saveEx.getErrorCode(),
                "标准入口的已删 ref 拒绝码 = 平台 deleted-ref 预检层");

        // 新绑定路径 ②：存量无型号资产 update 变更 modelId 至已删型号 → 拒绝
        ErpAstAsset asset = seedAsset("AST-DEL-M2", null, null);
        NopException updateEx = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(sess -> assetBiz.update(
                        Map.of("id", asset.getId(), "modelId", modelId), CTX)),
                "变更绑定至已删型号应被拒绝");
        assertEquals("nop.err.dao.unknown-entity", updateEx.getErrorCode());
    }

    @Test
    public void testStockAssetWithDeletedModelExemptedFromValidation() {
        // 存量路径：资产绑定型号并携带合规 extFieldValues 后型号被逻辑删除 →
        // 后续保存不按已删 defs 强制（原必填键缺失场景通过），且不报 ERR_AST_EXT_FIELD_WITHOUT_MODEL
        String modelId = seedModel("MDL-DEL-2", "[{\"key\":\"cpu\",\"label\":\"CPU\",\"type\":\"string\",\"required\":true}]");
        ErpAstAsset asset = seedAsset("AST-DEL-M3", modelId, "{\"cpu\":\"i7\"}");
        deleteModel(modelId);

        // 场景 ①：普通字段保存通过（modelId 未变更 → 存量豁免）
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "name", "改名后服务器"))))).getStatus(),
                "已删型号存量资产普通保存应通过");

        // 场景 ②：原必填键缺失（cpu 不在值集）→ 值不再按已删 defs 校验，通过
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "extFieldValues", "{\"ramGb\":32}"))))).getStatus(),
                "已删型号 defs 不再强制，原必填键缺失不拒绝");

        // 场景 ③：清空值同样通过（豁免对既有值零强制）
        assertEquals(0, graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpAstAsset__update", ApiRequest.build(Map.of("data", Map.of(
                        "id", asset.getId(), "extFieldValues", "{}"))))).getStatus(),
                "已删型号存量资产清空值保存应通过");
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

    /** 逻辑删除型号（useLogicalDelete → deleteEntity 即 UPDATE delVersion，对齐生产删除路径）。 */
    private void deleteModel(String modelId) {
        ormTemplate.runInSession(sess -> {
            IEntityDao<ErpAstAssetModel> dao = daoProvider.daoFor(ErpAstAssetModel.class);
            ErpAstAssetModel m = dao.getEntityById(modelId);
            dao.deleteEntity(m);
            return null;
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
