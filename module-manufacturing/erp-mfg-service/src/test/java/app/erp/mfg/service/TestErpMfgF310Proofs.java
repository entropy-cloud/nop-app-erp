package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrder;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderBomSnapshot;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.bom.BomExpander;
import app.erp.mfg.service.workorder.KitAvailabilityChecker;
import app.erp.mfg.service.workorder.KitAvailabilityResult;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F3.10 mfg 批 Proof 测试（plan 2026-09-17-0800-1）：批内修复的红→绿证明点。
 *
 * <ul>
 *   <li>P2-CK-mfg-006：BOM 快照实体不可变——通用 update 被拒（RC-R1.49 LOCK_AT_CREATION）。</li>
 *   <li>P2-CK-mfg-007：KitAvailabilityChecker 余额查询 orgId 隔离（null-skip 契约）——A 组织库存不可满足 B 组织工单齐套。</li>
 *   <li>P2-CK-mfg-008：BOM consumption 消费控制分级——STRICT 超工单行计划量确认拒绝（副作用前中止）/ WARNING warn 放行 / FLEXIBLE（默认）放行。</li>
 *   <li>P2-CK-mfg2-010：BOM qty≤0 展开抛 IllegalArgumentException（修复前静默归零）。</li>
 *   <li>P2-CK-mfg3-007：委外收货 destWarehouseId 非空守卫 + 非正数量拒绝（修复前静默替换/放行）。</li>
 *   <li>P2-CK-mfg3-012：委外 reverseApprove docStatus 守卫——仅 APPROVED（未发料）可反审核。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgF310Proofs extends JunitAutoTestCase {

    static final String ORG_ID = "1901";
    static final String ORG_B = "1902";
    static final String WAREHOUSE_ID = "3901";
    static final String UOM_ID = "5901";
    static final String CURRENCY_ID = "6901";
    static final String P = "1903";     // 产成品
    static final String M1 = "1904";    // 子件
    static final String MOVE_TYPE_INCOMING = "INCOMING";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    KitAvailabilityChecker kitAvailabilityChecker;
    @Inject
    BomExpander bomExpander;

    // ===================== P2-CK-mfg-006：快照不可变 =====================

    /**
     * 红绿反转证明：修复前通用 update 放行（快照可被篡改，提交时点锁定语义失效）；
     * 修复后 {@code defaultPrepareUpdate} 抛错，update 拒绝且快照内容不变。
     */
    @Test
    public void testSnapshotUpdateRejected() {
        seedSnapshot("7001", "7002");
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderBomSnapshot> dao = daoProvider.daoFor(ErpMfgWorkOrderBomSnapshot.class);
            ErpMfgWorkOrderBomSnapshot before = dao.getEntityById("7001");
            before.setVersionLabel("V1.0");
            dao.updateEntity(before);
        });

        ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrderBomSnapshot__update",
                Map.of("data", Map.of("id", "7001", "versionLabel", "TAMPERED")));
        assertTrue(resp.getStatus() != 0, "快照通用 update 必须被拒（LOCK_AT_CREATION 不可变语义）: " + resp);

        ErpMfgWorkOrderBomSnapshot after = daoProvider.daoFor(ErpMfgWorkOrderBomSnapshot.class).getEntityById("7001");
        assertEquals("V1.0", after.getVersionLabel(), "拒绝路径快照内容不变");
    }

    // ===================== P2-CK-mfg-007：kit orgId 隔离 =====================

    /**
     * 红绿反转证明：修复前余额查询无 orgId 过滤——A 组织的 M1 库存被误判为 B 组织工单齐套可用；
     * 修复后按工单 orgId 过滤，跨组织余额不可见。
     */
    @Test
    public void testKitCheckIsolatesBalanceByOrgId() {
        seedBom("9901", P, M1, bd("2"), null);
        seedWorkOrder("8601", "WO-F310-KIT", ORG_B, "9901");

        // 仅 A 组织有 M1 余额（10 ≥ 需求 2）；B 组织无任何 M1 余额
        seedBalance(M1, ORG_ID, bd("10"));

        KitAvailabilityResult result = kitAvailabilityChecker.check("8601");
        assertFalse(result.isFullyAvailable(), "B 组织工单不得消费 A 组织余额（orgId 隔离）");
        assertFalse(result.getShortages().isEmpty(), "M1 应报缺料");
        assertEquals(0, result.getShortages().get(0).getAvailableQty().compareTo(bd("0")),
                "B 组织视角 M1 可用量 = 0（修复前误读 A 组织 10）");

        // 控制组：B 组织注入自身余额 → 齐套
        seedBalance(M1, ORG_B, bd("5"));
        KitAvailabilityResult ok = kitAvailabilityChecker.check("8601");
        assertTrue(ok.isFullyAvailable(), "B 组织自身余额满足 → 齐套");
    }

    // ===================== P2-CK-mfg-008：consumption 消费控制 =====================

    /**
     * 红绿反转证明：STRICT BOM 超工单行计划量领料确认拒绝（修复前零消费放行）；
     * 中止发生在任何副作用之前（docStatus 保持 DRAFT、无出库）。
     */
    @Test
    public void testStrictConsumptionRejectsOverIssue() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom("9902", P, M1, bd("1"), "STRICT");
        String woId = seedWorkOrder("8602", "WO-F310-STRICT", ORG_ID, "9902");
        String wolId = seedWorkOrderLine("9701", woId, M1, bd("2"));
        String issueId = seedIssue("MI-F310-S1", woId);
        seedIssueLine("9601", issueId, M1, bd("5"), wolId);

        ApiResponse<?> resp = rpc(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));
        assertTrue(resp.getStatus() != 0, "STRICT 超领（5 > 计划 2）必须拒绝: " + resp);

        ErpMfgMaterialIssue issue = daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(issueId);
        assertEquals(ErpMfgConstants.ISSUE_STATUS_DRAFT, issue.getDocStatus(), "副作用前中止：状态保持 DRAFT");
        assertEquals(0, bd("0").compareTo(nz(daoProvider.daoFor(ErpMfgWorkOrderLine.class)
                        .getEntityById(wolId).getActualQuantity())),
                "副作用前中止：工单行 actualQuantity 未回写");
    }

    /**
     * WARNING：同型超领 warn 放行（有库存通道，确认成功 + 回写 5）。
     */
    @Test
    public void testWarningConsumptionWarnsAndAllows() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom("9903", P, M1, bd("1"), "WARNING");
        generateIncoming(M1, "PR-F310-W1", bd("10"), bd("5"));
        String woId = seedWorkOrder("8603", "WO-F310-WARN", ORG_ID, "9903");
        String wolId = seedWorkOrderLine("9702", woId, M1, bd("2"));
        String issueId = seedIssue("MI-F310-W1", woId);
        seedIssueLine("9602", issueId, M1, bd("5"), wolId);

        ApiResponse<?> resp = rpc(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));
        assertEquals(0, resp.getStatus(), "WARNING 超领 warn 放行: " + resp);
        assertEquals(0, bd("5").compareTo(daoProvider.daoFor(ErpMfgWorkOrderLine.class)
                        .getEntityById(wolId).getActualQuantity()),
                "放行路径正常回写 actualQuantity=5");
    }

    /**
     * FLEXIBLE（列空缺省）：超领放行（对照组，修复前后行为一致零回归）。
     */
    @Test
    public void testFlexibleConsumptionAllowsOverIssue() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom("9904", P, M1, bd("1"), null);
        generateIncoming(M1, "PR-F310-F1", bd("10"), bd("5"));
        String woId = seedWorkOrder("8604", "WO-F310-FLEX", ORG_ID, "9904");
        String wolId = seedWorkOrderLine("9703", woId, M1, bd("2"));
        String issueId = seedIssue("MI-F310-F1", woId);
        seedIssueLine("9603", issueId, M1, bd("5"), wolId);

        assertEquals(0, rpc(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId)).getStatus(),
                "FLEXIBLE（缺省）超领放行");
    }

    // ===================== P2-CK-mfg2-010：BOM qty≤0 抛错 =====================

    /**
     * 红绿反转证明：修复前 BOM qty=0 静默归零（除零保护吞语义，子件需求全灭）；
     * 修复后抛 IllegalArgumentException 快速失败。
     */
    @Test
    public void testBomNonPositiveQtyExplodeRejected() {
        seedMaterial(P, null);
        seedMaterial(M1, null);
        seedBom("9905", P, M1, bd("1"), null);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = dao.getEntityById("9905");
            bom.setQty(bd("0"));
            dao.updateEntity(bom);
        });

        assertThrows(IllegalArgumentException.class,
                () -> bomExpander.explode("9905", bd("1"), false),
                "BOM qty=0 展开必须抛错（修复前静默归零）");
    }

    // ===================== P2-CK-mfg3-007：委外收货入参守卫 =====================

    /**
     * 红绿反转证明：修复前 receivedQty≤0 静默替换为行数量或 ONE、仓库缺失静默放行；
     * 修复后 destWarehouseId 非空守卫先行、非正数量拒绝。
     */
    @Test
    public void testSubcontractReceiveRejectsMissingWarehouseAndNonPositiveQty() {
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        generateIncoming(M1, "PR-F310-SC1", bd("10"), bd("5"));

        String orderId = seedSubcontractOrder("SUB-F310-R1", bd("30"));
        seedSubcontractLine("9501", orderId, M1, bd("2"));
        rpcOk(mutation, "ErpMfgSubcontractOrder__submitForApproval", Map.of("id", orderId));
        rpcOk(mutation, "ErpMfgSubcontractOrder__approve", Map.of("id", orderId));
        rpcOk(mutation, "ErpMfgSubcontractOrder__issueMaterials",
                Map.of("subcontractOrderId", orderId, "sourceWarehouseId", WAREHOUSE_ID));

        // 仓库缺失 → 非空守卫拒绝
        Map<String, Object> noWh = new LinkedHashMap<>();
        noWh.put("subcontractOrderId", orderId);
        noWh.put("receivedQty", bd("1"));
        assertTrue(rpc(mutation, "ErpMfgSubcontractOrder__receiveFinished", noWh).getStatus() != 0,
                "destWarehouseId 缺失必须拒绝（修复前静默放行）");

        // 非正数量 → 拒绝
        assertTrue(rpc(mutation, "ErpMfgSubcontractOrder__receiveFinished",
                        Map.of("subcontractOrderId", orderId, "receivedQty", bd("0"),
                                "destWarehouseId", WAREHOUSE_ID)).getStatus() != 0,
                "receivedQty=0 必须拒绝（修复前静默替换）");
        assertTrue(rpc(mutation, "ErpMfgSubcontractOrder__receiveFinished",
                        Map.of("subcontractOrderId", orderId, "receivedQty", bd("-1"),
                                "destWarehouseId", WAREHOUSE_ID)).getStatus() != 0,
                "receivedQty<0 必须拒绝");

        // 超过订单行数量合计（2）→ 上限守卫拒绝（100% 严格上限，损耗容差未裁决）
        assertTrue(rpc(mutation, "ErpMfgSubcontractOrder__receiveFinished",
                        Map.of("subcontractOrderId", orderId, "receivedQty", bd("3"),
                                "destWarehouseId", WAREHOUSE_ID)).getStatus() != 0,
                "receivedQty 超过订单行合计必须拒绝（修复前无上限）");
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED, statusOf(orderId),
                "拒绝路径状态保持 ISSUED");

        // 控制组：合法收货放行
        rpcOk(mutation, "ErpMfgSubcontractOrder__receiveFinished",
                Map.of("subcontractOrderId", orderId, "receivedQty", bd("1"),
                        "destWarehouseId", WAREHOUSE_ID));
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_RECEIVED, statusOf(orderId));
    }

    // ===================== P2-CK-mfg3-012：reverseApprove docStatus 守卫 =====================

    /**
     * 红绿反转证明：修复前 ISSUED（已发料）单可反审核（发料出库已发生而单据回到未审核）；
     * 修复后仅 APPROVED（未发料）可反审核。
     */
    @Test
    public void testSubcontractReverseApproveOnlyBeforeIssue() {
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        generateIncoming(M1, "PR-F310-SC2", bd("10"), bd("5"));

        // 已发料单：reverseApprove 拒绝
        String issuedId = seedSubcontractOrder("SUB-F310-RA1", bd("30"));
        seedSubcontractLine("9502", issuedId, M1, bd("1"));
        rpcOk(mutation, "ErpMfgSubcontractOrder__submitForApproval", Map.of("id", issuedId));
        rpcOk(mutation, "ErpMfgSubcontractOrder__approve", Map.of("id", issuedId));
        rpcOk(mutation, "ErpMfgSubcontractOrder__issueMaterials",
                Map.of("subcontractOrderId", issuedId, "sourceWarehouseId", WAREHOUSE_ID));
        assertTrue(rpc(mutation, "ErpMfgSubcontractOrder__reverseApprove",
                        Map.of("id", issuedId)).getStatus() != 0,
                "已发料（docStatus=ISSUED）单反审核必须拒绝（P2-CK-mfg3-012）");
        assertEquals(ErpMfgConstants.SUBCONTRACT_STATUS_ISSUED, statusOf(issuedId), "拒绝后状态不变");

        // 控制组：APPROVED（未发料）单：reverseApprove 放行
        String approvedId = seedSubcontractOrder("SUB-F310-RA2", bd("30"));
        seedSubcontractLine("9503", approvedId, M1, bd("1"));
        rpcOk(mutation, "ErpMfgSubcontractOrder__submitForApproval", Map.of("id", approvedId));
        rpcOk(mutation, "ErpMfgSubcontractOrder__approve", Map.of("id", approvedId));
        rpcOk(mutation, "ErpMfgSubcontractOrder__reverseApprove", Map.of("id", approvedId));
    }

    // ===================== seed helpers =====================

    private void seedMaterial(String id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
        });
    }

    private void seedBom(String bomId, String productId, String componentId, BigDecimal qty, String consumption) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", bomId);
            bom.setCode("BOM-" + bomId);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(qty);
            if (consumption != null) {
                bom.orm_propValueByName("consumption", consumption);
            }
            dao.saveEntity(bom);
            IEntityDao<ErpMfgBomLine> ldao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", String.valueOf(Long.parseLong(bomId) + 50000));
            line.setBomId(bomId);
            line.setLineNo(10);
            line.setMaterialId(componentId);
            line.setUoMId(UOM_ID);
            line.setQuantity(bd("1"));
            ldao.saveEntity(line);
        });
    }

    private String seedWorkOrder(String id, String code, String orgId, String bomId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(bomId);
            wo.setOrgId(orgId);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
            dao.saveEntity(wo);
        });
        return id;
    }

    private String seedWorkOrderLine(String id, String woId, String materialId, BigDecimal plannedQty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(10);
            wol.orm_propValueByName("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_INPUT);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            dao.saveEntity(wol);
        });
        return id;
    }

    private String seedIssue(String code, String woId) {
        String id = String.valueOf(8400 + Math.abs(code.hashCode() % 500));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssue> dao = daoProvider.daoFor(ErpMfgMaterialIssue.class);
            ErpMfgMaterialIssue issue = new ErpMfgMaterialIssue();
            issue.orm_propValueByName("id", id);
            issue.setCode(code);
            issue.setWorkOrderId(woId);
            issue.setOrgId(ORG_ID);
            issue.setWarehouseId(WAREHOUSE_ID);
            issue.setBusinessDate(LocalDate.of(2026, 7, 1));
            issue.setCurrencyId(CURRENCY_ID);
            issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DRAFT);
            issue.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(issue);
        });
        return id;
    }

    private void seedIssueLine(String id, String issueId, String materialId, BigDecimal qty, String wolId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssueLine> dao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
            ErpMfgMaterialIssueLine line = new ErpMfgMaterialIssueLine();
            line.orm_propValueByName("id", id);
            line.setIssueId(issueId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setRequiredQuantity(qty);
            line.setIssuedQuantity(qty);
            line.setWorkOrderLineId(wolId);
            dao.saveEntity(line);
        });
    }

    private void seedBalance(String materialId, String orgId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance balance = dao.newEntity();
            balance.orm_propValueByName("id", String.valueOf(6000
                    + Math.abs((materialId + orgId).hashCode() % 3000)));
            balance.setOrgId(orgId);
            balance.setMaterialId(materialId);
            balance.setWarehouseId(WAREHOUSE_ID);
            balance.setTotalQuantity(qty);
            balance.setAvailableQuantity(qty);
            dao.saveEntity(balance);
        });
    }

    private void seedSnapshot(String id, String workOrderId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderBomSnapshot> dao = daoProvider.daoFor(ErpMfgWorkOrderBomSnapshot.class);
            ErpMfgWorkOrderBomSnapshot snapshot = dao.newEntity();
            snapshot.orm_propValueByName("id", id);
            snapshot.orm_propValueByName("workOrderId", workOrderId);
            snapshot.setBomId("9901");
            snapshot.setProductId(P);
            snapshot.setQty(bd("1"));
            dao.saveEntity(snapshot);
        });
    }

    private String seedSubcontractOrder(String code, BigDecimal processingFee) {
        String id = String.valueOf(8700 + Math.abs(code.hashCode() % 500));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrder> dao = daoProvider.daoFor(ErpMfgSubcontractOrder.class);
            ErpMfgSubcontractOrder order = new ErpMfgSubcontractOrder();
            order.orm_propValueByName("id", id);
            order.setCode(code);
            order.setOrgId(ORG_ID);
            order.setSupplierId("4901");
            order.setProductId(P);
            order.setBusinessDate(LocalDate.of(2026, 7, 1));
            order.setCurrencyId(CURRENCY_ID);
            order.setExchangeRate(BigDecimal.ONE);
            order.setProcessingFee(processingFee);
            order.setTotalAmount(processingFee);
            order.setDocStatus(ErpMfgConstants.SUBCONTRACT_STATUS_DRAFT);
            order.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            order.orm_propValueByName("postedStatus", "DRAFT");
            dao.saveEntity(order);
        });
        return id;
    }

    private void seedSubcontractLine(String id, String orderId, String materialId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgSubcontractOrderLine> dao = daoProvider.daoFor(ErpMfgSubcontractOrderLine.class);
            ErpMfgSubcontractOrderLine line = new ErpMfgSubcontractOrderLine();
            line.orm_propValueByName("id", id);
            line.setSubcontractOrderId(orderId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            dao.saveEntity(line);
        });
    }

    private void generateIncoming(String materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));
        rpcOk(mutation, "ErpInvStockMove__generateMove", Map.of("request", req));
    }

    // ---------- query helpers ----------

    private String statusOf(String orderId) {
        return daoProvider.daoFor(ErpMfgSubcontractOrder.class).getEntityById(orderId).getDocStatus();
    }

    private ApiResponse<?> rpc(GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
