package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 3 测试：领料确认→出库移动单 DONE→余额扣减→WorkOrderLine.actualQuantity 回写→WorkOrder.materialCost 汇总。
 *
 * <p>覆盖 {@code docs/design/manufacturing/state-machine.md}（IN_PROCESS 领料出库）+
 * `docs/design/inventory/cross-domain.md` 的 {@code generateMove} 调用方契约 + 幂等。
 *
 * <p>先经库存域 generateMove(INCOMING) 注入 M1 期初库存（10@5=移动加权 avgCost=5），再调
 * {@code ErpMfgMaterialIssue__confirm} 触发 OUTGOING 出库移动单（业务联动自动 DONE）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgMaterialIssue extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long WAREHOUSE_ID = 3301L;
    static final Long UOM_ID = 5301L;
    static final Long CURRENCY_ID = 6301L;
    static final Long P = 1001L;     // 产成品
    static final Long M1 = 1002L;    // 子件
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String MOVE_TYPE_OUTGOING = "OUTGOING";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testConfirmIssuesOutAndAggregatesMaterialCost() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");   // 移动加权平均
        seedBom(9001L, P, M1, bd("2"));
        // 期初库存：入库 10@5 → balance M1 = 10, avgCost = 5
        generateIncoming(M1, "PR-MI-001", bd("10"), bd("5"));
        ErpInvStockBalance before = findBalance(M1);
        assertEquals(0, before.getTotalQuantity().compareTo(bd("10")), "期初 M1=10");

        Long woId = seedWorkOrder("WO-MI");
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"));
        Long issueId = seedIssue("MI-001", woId);
        seedIssueLine(9201L, issueId, M1, bd("2"), wolId);

        // 确认领料 → 出库移动单 DONE
        ApiResponse<?> resp = rpc(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));
        assertEquals(0, resp.getStatus(), "confirm 应成功: " + resp);

        // issue-status → DONE
        ErpMfgMaterialIssue issue = daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(issueId);
        assertEquals(ErpMfgConstants.ISSUE_STATUS_DONE, issue.getDocStatus(), "领料确认 → DONE");

        // 出库移动单生成且 DONE
        ErpInvStockMove move = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE, "MI-001");
        assertNotNull(move, "应生成出库移动单");
        assertEquals("DONE", move.getDocStatus(), "业务联动移动单 DONE");
        assertEquals(MOVE_TYPE_OUTGOING, move.getMoveType(), "领料出库用 OUTGOING");

        // 余额扣减：10 - 2 = 8
        ErpInvStockBalance after = findBalance(M1);
        assertEquals(0, after.getTotalQuantity().compareTo(bd("8")), "出库后 M1=8");

        // WorkOrderLine.actualQuantity 回写 = 2
        ErpMfgWorkOrderLine wol = daoProvider.daoFor(ErpMfgWorkOrderLine.class).getEntityById(wolId);
        assertEquals(0, wol.getActualQuantity().compareTo(bd("2")), "工单投入行 actualQuantity=2");

        // WorkOrder.materialCost = 2 × avgCost(5) = 10
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(0, wo.getMaterialCost().compareTo(bd("10")), "材料成本汇总 = 2×5 = 10");
    }

    @Test
    public void testConfirmIdempotent() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9002L, P, M1, bd("1"));
        generateIncoming(M1, "PR-MI-IDEM", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-MI-IDEM");
        Long wolId = seedWorkOrderLine(woId, M1, bd("1"));
        Long issueId = seedIssue("MI-IDEM", woId);
        seedIssueLine(9202L, issueId, M1, bd("1"), wolId);

        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));
        ErpInvStockMove first = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE, "MI-IDEM");

        // 重复确认 → 幂等，不产生第二张移动单，不双扣
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));
        ErpInvStockMove second = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE, "MI-IDEM");
        assertEquals(first.getId(), second.getId(), "幂等：同一移动单");

        ErpInvStockBalance after = findBalance(M1);
        assertEquals(0, after.getTotalQuantity().compareTo(bd("9")), "幂等：仅扣减一次 10-1=9");
    }

    // ---------- seed helpers ----------

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
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

    private void seedMaterial(Long id, String costMethod) {
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

    private void seedBom(Long bomId, Long productId, Long componentId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", bomId);
            bom.setCode("BOM-" + bomId);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
            IEntityDao<ErpMfgBomLine> ldao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", bomId + 50000);
            line.setBomId(bomId);
            line.setLineNo(10);
            line.setMaterialId(componentId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
        });
    }

    private Long seedWorkOrder(String code) {
        Long id = 8100L + (long) Math.abs(code.hashCode() % 800);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(9001L);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
            dao.saveEntity(wo);
        });
        return id;
    }

    private Long seedWorkOrderLine(Long woId, Long materialId, BigDecimal plannedQty) {
        Long id = 9100L + (long) Math.abs((woId + "" + materialId).hashCode() % 800);
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

    private Long seedIssue(String code, Long woId) {
        Long id = 8200L + (long) Math.abs(code.hashCode() % 800);
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

    private void seedIssueLine(Long id, Long issueId, Long materialId, BigDecimal qty, Long wolId) {
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

    // ---------- query helpers ----------

    private ErpInvStockBalance findBalance(Long materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
