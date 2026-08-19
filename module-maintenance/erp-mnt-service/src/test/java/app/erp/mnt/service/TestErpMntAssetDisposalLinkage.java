package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntEquipmentStatusLog;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 资产处置→设备 DECOMMISSIONED 联动 Facade 真实实现测试（RC-R1.77 / P1-RC-070 / UC-MAIN-08-A，
 * plan 2026-08-19-0445-3 Phase 1 Proof ①-⑤；assets 处置 Processor 接线侧见
 * {@code TestErpAstDisposalEquipmentLinkage}（ast-service mock 双层））。
 *
 * <p>覆盖 {@code IErpMntEquipmentBiz#changeStatusForAssetDisposal}/{@code #restoreFromAssetDisposal}：
 * ① 关联设备置 DECOMMISSIONED + DISPOSAL 状态日志行（sourceBillCode=处置单编码）
 * ② 无关联设备 no-op 返回 0 ③ 已目标态幂等跳过 ④ restore posted 对称恢复 RUNNING + 非 DECOMMISSIONED 幂等
 * ⑤ config {@code erp-mnt.disposal-link-enabled} 关闭跳过。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntAssetDisposalLinkage extends JunitAutoTestCase {

    static final Long ASSET_ID = 4001L;
    static final Long ASSET_ID_NO_EQUIPMENT = 4999L;
    static final Long EQUIPMENT_ID = 401L;
    static final String DISPOSAL_CODE = "DISP-LINK-001";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ① 处置联动置 DECOMMISSIONED + 状态日志行 ----------

    @Test
    public void testDisposalLinkageSetsDecommissionedAndWritesLog() {
        seedEquipment(EQUIPMENT_ID, ASSET_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);

        ApiResponse<?> resp = rpc(mutation, "ErpMntEquipment__changeStatusForAssetDisposal",
                Map.of("assetId", ASSET_ID, "disposalCode", DISPOSAL_CODE));
        assertEquals(0, resp.getStatus(), "联动应成功: " + resp);
        assertEquals(1, ((Number) resp.getData()).intValue(), "联动行数=1");

        ErpMntEquipment equipment = loadEquipment(EQUIPMENT_ID);
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED, equipment.getStatus(),
                "关联设备应置 DECOMMISSIONED");

        ErpMntEquipmentStatusLog log = findLatestLog(EQUIPMENT_ID);
        assertNotNull(log, "应写 DISPOSAL 状态日志行");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, log.getFromStatus());
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED, log.getToStatus());
        assertEquals(ErpMntConstants.STATUS_LOG_SOURCE_DISPOSAL, log.getSource());
        assertEquals(DISPOSAL_CODE, log.getSourceBillCode(), "sourceBillCode=处置单编码");
    }

    // ---------- ② 无关联设备 no-op + ③ 幂等 ----------

    @Test
    public void testNoLinkedEquipmentAndIdempotentSkip() {
        seedEquipment(EQUIPMENT_ID, ASSET_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);

        // ② 无关联设备（§1.2 可选关联）→ no-op 0
        ApiResponse<?> miss = rpc(mutation, "ErpMntEquipment__changeStatusForAssetDisposal",
                Map.of("assetId", ASSET_ID_NO_EQUIPMENT, "disposalCode", DISPOSAL_CODE));
        assertEquals(0, miss.getStatus());
        assertEquals(0, ((Number) miss.getData()).intValue(), "无关联设备 no-op 返回 0");

        // ③ 首次联动后再次调用 → 已目标态幂等跳过
        rpc(mutation, "ErpMntEquipment__changeStatusForAssetDisposal",
                Map.of("assetId", ASSET_ID, "disposalCode", DISPOSAL_CODE));
        ApiResponse<?> again = rpc(mutation, "ErpMntEquipment__changeStatusForAssetDisposal",
                Map.of("assetId", ASSET_ID, "disposalCode", DISPOSAL_CODE));
        assertEquals(0, ((Number) again.getData()).intValue(), "已 DECOMMISSIONED 幂等跳过返回 0");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED, loadEquipment(EQUIPMENT_ID).getStatus());
        assertEquals(1, countLogs(EQUIPMENT_ID), "幂等重调不追加日志行");
    }

    // ---------- ④ reverseApprove 对称恢复 ----------

    @Test
    public void testRestoreFromDisposalRecoversRunning() {
        seedEquipment(EQUIPMENT_ID, ASSET_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED);

        ApiResponse<?> resp = rpc(mutation, "ErpMntEquipment__restoreFromAssetDisposal",
                Map.of("assetId", ASSET_ID, "disposalCode", DISPOSAL_CODE));
        assertEquals(0, resp.getStatus());
        assertEquals(1, ((Number) resp.getData()).intValue());
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, loadEquipment(EQUIPMENT_ID).getStatus(),
                "§1.3 资产恢复分支：设备恢复 RUNNING（字面语义）");

        ErpMntEquipmentStatusLog log = findLatestLog(EQUIPMENT_ID);
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED, log.getFromStatus());
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, log.getToStatus());
        assertEquals(ErpMntConstants.STATUS_LOG_SOURCE_DISPOSAL, log.getSource());

        // 非 DECOMMISSIONED 幂等跳过
        ApiResponse<?> again = rpc(mutation, "ErpMntEquipment__restoreFromAssetDisposal",
                Map.of("assetId", ASSET_ID, "disposalCode", DISPOSAL_CODE));
        assertEquals(0, ((Number) again.getData()).intValue(), "非 DECOMMISSIONED 幂等跳过返回 0");
    }

    // ---------- ⑤ config 关闭跳过 ----------

    @Test
    public void testConfigDisabledSkipsLinkage() {
        AppConfig.getConfigProvider().assignConfigValue(ErpMntConstants.CONFIG_DISPOSAL_LINK_ENABLED, "false");
        try {
            seedEquipment(EQUIPMENT_ID, ASSET_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);

            ApiResponse<?> decommission = rpc(mutation, "ErpMntEquipment__changeStatusForAssetDisposal",
                    Map.of("assetId", ASSET_ID, "disposalCode", DISPOSAL_CODE));
            assertEquals(0, decommission.getStatus());
            assertEquals(0, ((Number) decommission.getData()).intValue(), "config 关闭跳过联动");
            assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, loadEquipment(EQUIPMENT_ID).getStatus(),
                    "设备状态不变");
            assertEquals(0, countLogs(EQUIPMENT_ID), "零状态日志行");

            seedEquipment(EQUIPMENT_ID + 1, ASSET_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED);
            ApiResponse<?> restore = rpc(mutation, "ErpMntEquipment__restoreFromAssetDisposal",
                    Map.of("assetId", ASSET_ID, "disposalCode", DISPOSAL_CODE));
            assertEquals(0, ((Number) restore.getData()).intValue(), "config 关闭跳过恢复");
            assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_DECOMMISSIONED,
                    loadEquipment(EQUIPMENT_ID + 1).getStatus());
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpMntConstants.CONFIG_DISPOSAL_LINK_ENABLED, "true");
        }
    }

    // ---------- helpers ----------

    private void seedEquipment(Long id, Long assetId, String status) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
            ErpMntEquipment equipment = new ErpMntEquipment();
            equipment.setId(id);
            equipment.setCode("EQ-" + id);
            equipment.setName("设备" + id);
            equipment.setAssetId(assetId);
            equipment.setStatus(status);
            dao.saveEntity(equipment);
        });
    }

    private ErpMntEquipment loadEquipment(Long id) {
        return daoProvider.daoFor(ErpMntEquipment.class).getEntityById(id);
    }

    private ErpMntEquipmentStatusLog findLatestLog(Long equipmentId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("equipmentId", equipmentId));
        q.addOrderField("id", true);
        q.setLimit(1);
        List<ErpMntEquipmentStatusLog> logs = daoProvider.daoFor(ErpMntEquipmentStatusLog.class).findAllByQuery(q);
        return logs.isEmpty() ? null : logs.get(0);
    }

    private int countLogs(Long equipmentId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("equipmentId", equipmentId));
        return daoProvider.daoFor(ErpMntEquipmentStatusLog.class).findAllByQuery(q).size();
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                                Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
