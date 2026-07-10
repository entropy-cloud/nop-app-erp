package app.erp.mfg.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 全链路端到端测试：制造业财一体成本流转闭环（plan 2026-07-10-1100-5）。
 *
 * <p>验证完整成本流转链：原材料存货 →（领料）→ WIP →（完工）→ 产成品存货。
 * 领料过账（Dr WIP / Cr Inventory）+ 完工入库过账（Dr Inventory / Cr WIP）双凭证同时存在，
 * WIP 科目净余额 = 0（成本完整流转闭环），且领料出库不再误派 SALES_OUTPUT 凭证。
 *
 * <p>差异过账为 config-gated（默认关），本测试不开启，故差异凭证不参与 WIP 余额。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgCostFlowEndToEnd extends JunitAutoTestCase {

    static final Long ORG_ID = 1701L;
    static final Long WAREHOUSE_ID = 3701L;
    static final Long UOM_ID = 5701L;
    static final Long CURRENCY_ID = 6701L;
    static final Long ACCT_SCHEMA_ID = 7701L;
    static final Long P = 1501L;
    static final Long M1 = 1502L;
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_WIP = "1411";
    static final String SUBJECT_AP = "2202";
    static final String VOUCHER_STATUS_POSTED = "POSTED";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testFullCostFlowIssueToCompletion() {
        seedPeriodAndSubjects();
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        seedBom(9801L, P, M1, bd("2"));
        generateIncoming(M1, "PR-CF-E2E", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-CF-E2E", 9801L);
        Long inputLineId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", String.valueOf(woId)));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        // ---- 领料出库 → MANUFACTURING_ISSUE 凭证 ----
        Long issueId = seedIssue("MI-CF-E2E", woId);
        seedIssueLine(9801L, issueId, M1, bd("2"), inputLineId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        BigDecimal materialCost = bd("10"); // 2 × avgCost 5

        ErpMfgMaterialIssue issue = daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(issueId);
        assertEquals(true, issue.getPosted(), "领料 posted=true");
        ErpInvStockMove issueMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE, "MI-CF-E2E");
        assertNotNull(issueMove, "领料出库移动单存在");

        ErpFinVoucher issueVoucher = findVoucher("MI-CF-E2E-MI", ErpFinBusinessType.MANUFACTURING_ISSUE);
        assertNotNull(issueVoucher, "MANUFACTURING_ISSUE 凭证存在");
        assertEquals(VOUCHER_STATUS_POSTED, issueVoucher.getDocStatus());
        assertVoucherLine(issueVoucher.getId(), SUBJECT_WIP, "DEBIT", materialCost);
        assertVoucherLine(issueVoucher.getId(), SUBJECT_INVENTORY, "CREDIT", materialCost);

        // ---- 领料出库不再误派 SALES_OUTPUT 凭证 ----
        assertNull(findVoucher(issueMove.getCode(), ErpFinBusinessType.SALES_OUTPUT),
                "领料出库不应生成 SALES_OUTPUT 凭证");

        // ---- 完工入库 → MANUFACTURING_RECEIPT 凭证 ----
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpInvStockMove completionMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-CF-E2E");
        assertNotNull(completionMove, "完工入库移动单存在");
        assertEquals(true, completionMove.getPosted(), "完工入库 posted=true");

        BigDecimal completionCost = materialCost; // laborCost=0 → totalCost = materialCost

        ErpFinVoucher completionVoucher = findVoucher(completionMove.getCode(), ErpFinBusinessType.MANUFACTURING_RECEIPT);
        assertNotNull(completionVoucher, "MANUFACTURING_RECEIPT 凭证存在");
        assertEquals(VOUCHER_STATUS_POSTED, completionVoucher.getDocStatus());
        assertVoucherLine(completionVoucher.getId(), SUBJECT_INVENTORY, "DEBIT", completionCost);
        assertVoucherLine(completionVoucher.getId(), SUBJECT_WIP, "CREDIT", completionCost);

        // ---- WIP 科目净余额验证：领料 Dr - 完工 Cr = materialCost - completionCost = 0（成本完整流转闭环）----
        BigDecimal wipNet = sumSubjectBalance(SUBJECT_WIP);
        assertEquals(0, BigDecimal.ZERO.compareTo(wipNet),
                "WIP 净余额 = 0（领料借方 " + materialCost + " - 完工贷方 " + completionCost + "）");

        // ---- 产成品存货科目净余额：完工 Dr - 领料 Cr = completionCost - 0 = completionCost ----
        BigDecimal invNet = sumSubjectBalance(SUBJECT_INVENTORY);
        assertTrue(invNet.signum() > 0, "产成品存货净余额 > 0（完工入库借方）");
    }

    // ---------- seed helpers ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdAcctSchema> asDao = daoProvider.daoFor(ErpMdAcctSchema.class);
            ErpMdAcctSchema acctSchema = new ErpMdAcctSchema();
            acctSchema.orm_propValueByName("id", ACCT_SCHEMA_ID);
            acctSchema.setCode("ACCT-" + ORG_ID);
            acctSchema.setName("账套 " + ORG_ID);
            acctSchema.setOrgId(ORG_ID);
            acctSchema.orm_propValueByName("nature", "FINANCIAL");
            acctSchema.setFunctionalCurrencyId(CURRENCY_ID);
            acctSchema.orm_propValueByName("status", "ACTIVE");
            asDao.saveEntity(acctSchema);

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
            seedSubject(SUBJECT_AP, "应付账款-暂估", "LIABILITY", "CREDIT");
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

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
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
        Long id = 9100L + (long) Math.abs(code.hashCode() % 500);
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
        Long id = 9900L + (long) Math.abs(raw % 500);
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

    private Long seedIssue(String code, Long woId) {
        Long id = 9200L + (long) Math.abs(code.hashCode() % 500);
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

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinVoucher findVoucher(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        if (links.isEmpty()) {
            return null;
        }
        return daoProvider.daoFor(ErpFinVoucher.class).getEntityById(links.get(0).getVoucherId());
    }

    private void assertVoucherLine(Long voucherId, String subjectCode, String dcDirection, BigDecimal amount) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        q.addFilter(eq("subjectCode", subjectCode));
        List<ErpFinVoucherLine> list = dao.findAllByQuery(q);
        assertEquals(1, list.size(), "凭证 " + voucherId + " 科目 " + subjectCode + " 应唯一一行");
        ErpFinVoucherLine line = list.get(0);
        assertEquals(dcDirection, line.getDcDirection(),
                "凭证 " + voucherId + " 科目 " + subjectCode + " 方向");
        if ("DEBIT".equals(dcDirection)) {
            assertEquals(0, amount.compareTo(line.getDebitAmount()),
                    "凭证 " + voucherId + " 科目 " + subjectCode + " 借方金额");
        } else {
            assertEquals(0, amount.compareTo(line.getCreditAmount()),
                    "凭证 " + voucherId + " 科目 " + subjectCode + " 贷方金额");
        }
    }

    private BigDecimal sumSubjectBalance(String subjectCode) {
        IEntityDao<ErpFinVoucherLine> dao = daoProvider.daoFor(ErpFinVoucherLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("subjectCode", subjectCode));
        List<ErpFinVoucherLine> lines = dao.findAllByQuery(q);
        BigDecimal net = BigDecimal.ZERO;
        for (ErpFinVoucherLine l : lines) {
            net = net.add(nz(l.getDebitAmount())).subtract(nz(l.getCreditAmount()));
        }
        return net;
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

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
