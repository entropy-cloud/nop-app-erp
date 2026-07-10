package app.erp.mfg.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 2 测试：完工入库 GL 过账（plan 2026-07-10-1100-5）。
 *
 * <p>覆盖 MANUFACTURING_RECEIPT 凭证生成（Dr: 产成品存货 1401 / Cr: WIP 在制品 1411），
 * 三种计价方法（MOVING_AVERAGE / STANDARD / FIFO）均正确过账，完工入库移动单 posted=true。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgCompletionPosting extends JunitAutoTestCase {

    static final Long ORG_ID = 1501L;
    static final Long WAREHOUSE_ID = 3501L;
    static final Long UOM_ID = 5501L;
    static final Long CURRENCY_ID = 6501L;
    static final Long P = 1301L;
    static final Long M1 = 1302L;
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String VOUCHER_STATUS_POSTED = "POSTED";
    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_WIP = "1411";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testMovingAverageCompletionPosting() {
        seedPeriodAndSubjects();
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9401L, P, M1, bd("2"));
        generateIncoming(M1, "PR-CMP-MA", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-CMP-MA", 9401L);
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpInvStockMove move = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-CMP-MA");
        assertNotNull(move, "应生成完工入库移动单");
        assertEquals(ErpMfgConstants.MOVE_TYPE_MANUFACTURING, move.getMoveType());
        assertEquals(true, move.getPosted(), "完工入库 DONE 应过账 posted=true");

        ErpFinVoucher voucher = findVoucherByMoveCode(move.getCode());
        assertNotNull(voucher, "应生成 MANUFACTURING_RECEIPT 凭证");
        assertEquals(VOUCHER_STATUS_POSTED, voucher.getDocStatus());

        ErpFinVoucherLine drLine = findVoucherLine(voucher.getId(), SUBJECT_INVENTORY);
        assertNotNull(drLine, "借方 产成品存货 1401 行存在");
        assertEquals("DEBIT", drLine.getDcDirection());
        assertTrue(drLine.getDebitAmount().signum() > 0, "借方金额 > 0");

        ErpFinVoucherLine crLine = findVoucherLine(voucher.getId(), SUBJECT_WIP);
        assertNotNull(crLine, "贷方 WIP 在制品 1411 行存在");
        assertEquals("CREDIT", crLine.getDcDirection());
        assertEquals(0, drLine.getDebitAmount().compareTo(crLine.getCreditAmount()),
                "借贷平衡");
    }

    @Test
    public void testStandardCostCompletionPosting() {
        seedPeriodAndSubjects();
        seedMaterial(P, "STANDARD");
        seedMaterial(M1, "STANDARD");
        seedBom(9402L, P, M1, bd("2"));
        seedStandardCost(P, bd("40"));
        seedStandardCost(M1, bd("5"));
        generateIncoming(M1, "PR-CMP-STD", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-CMP-STD", 9402L);
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpInvStockMove move = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-CMP-STD");
        assertNotNull(move, "应生成完工入库移动单");
        assertEquals(true, move.getPosted(), "STANDARD 完工入库 posted=true");

        ErpFinVoucher voucher = findVoucherByMoveCode(move.getCode());
        assertNotNull(voucher, "STANDARD 凭证应生成");
        assertEquals(2, countVoucherLines(voucher.getId()), "2 行（Dr Inventory / Cr WIP）");
    }

    @Test
    public void testFifoCompletionPosting() {
        seedPeriodAndSubjects();
        seedMaterial(P, null);
        seedMaterial(M1, "FIFO");
        seedBom(9403L, P, M1, bd("1"));
        generateIncoming(M1, "PR-CMP-FIFO-1", bd("10"), bd("3"));
        generateIncoming(M1, "PR-CMP-FIFO-2", bd("5"), bd("7"));

        Long woId = seedWorkOrder("WO-CMP-FIFO", 9403L);
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpInvStockMove move = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-CMP-FIFO");
        assertNotNull(move, "应生成完工入库移动单");
        assertEquals(true, move.getPosted(), "FIFO 完工入库 posted=true");

        ErpFinVoucher voucher = findVoucherByMoveCode(move.getCode());
        assertNotNull(voucher, "FIFO 凭证应生成");
    }

    // ---------- seed helpers ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpFinAccountingPeriod> pdao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
            ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
            period.setCode("2026-07");
            period.setName("2026-07");
            period.setOrgId(ORG_ID);
            period.orm_propValueByName("year", 2026);
            period.orm_propValueByName("month", 7);
            period.setStartDate(LocalDate.of(2026, 7, 1));
            period.setEndDate(LocalDate.of(2026, 7, 31));
            period.orm_propValueByName("status", "OPEN");
            pdao.saveEntity(period);

            seedSubject(SUBJECT_INVENTORY, "库存商品", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP, "在制品-WIP", "ASSET", "DEBIT");
            seedSubject("2202", "应付账款-暂估", "LIABILITY", "CREDIT");
        });
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
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

    private void seedStandardCost(Long productId, BigDecimal unitCost) {
        ormTemplate.runInSession(() -> {
            Long headerId = productId * 10000 + 90;
            IEntityDao<app.erp.mfg.dao.entity.ErpMfgCostRollup> hDao =
                    daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgCostRollup.class);
            app.erp.mfg.dao.entity.ErpMfgCostRollup header = new app.erp.mfg.dao.entity.ErpMfgCostRollup();
            header.orm_propValueByName("id", headerId);
            header.setCode("ROLLUP-STD-" + productId);
            header.setOrgId(ORG_ID);
            header.setBusinessDate(LocalDate.of(2026, 6, 1));
            header.orm_propValueByName("status", ErpMfgConstants.COST_ROLLUP_STATUS_FIRMED);
            hDao.saveEntity(header);

            IEntityDao<app.erp.mfg.dao.entity.ErpMfgCostRollupLine> lDao =
                    daoProvider.daoFor(app.erp.mfg.dao.entity.ErpMfgCostRollupLine.class);
            app.erp.mfg.dao.entity.ErpMfgCostRollupLine line = new app.erp.mfg.dao.entity.ErpMfgCostRollupLine();
            line.orm_propValueByName("id", productId * 10000 + 91);
            line.setCostRollupId(headerId);
            line.setLineNo(10);
            line.setMaterialId(productId);
            line.setUoMId(UOM_ID);
            line.setMaterialCost(unitCost);
            line.setUnitCost(unitCost);
            line.setTotalCost(unitCost);
            line.setCurrencyId(CURRENCY_ID);
            lDao.saveEntity(line);
        });
    }

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

    private Long seedWorkOrder(String code, Long bomId) {
        Long id = 8600L + (long) Math.abs(code.hashCode() % 600);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
    }

    private Long seedWorkOrderLine(Long woId, Long materialId, BigDecimal plannedQty, String lineType,
                                   Long destWarehouseId) {
        long raw = (woId + "" + materialId + lineType).hashCode();
        Long id = 9400L + (long) Math.abs(raw % 600);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(materialId.intValue());
            wol.orm_propValueByName("lineType", lineType);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            wol.setDestWarehouseId(destWarehouseId);
            dao.saveEntity(wol);
        });
        return id;
    }

    // ---------- query helpers ----------

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucher findVoucherByMoveCode(String moveCode) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("billCode", moveCode));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    private ErpFinVoucherLine findVoucherLine(Long voucherId, String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        List<ErpFinVoucherLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private long countVoucherLines(Long voucherId) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return dao.findAllByQuery(q).size();
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
