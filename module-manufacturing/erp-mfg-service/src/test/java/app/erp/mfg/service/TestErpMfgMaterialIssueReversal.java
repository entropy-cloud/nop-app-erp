package app.erp.mfg.service;

import app.erp.fin.dao.ErpFinBusinessType;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
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
import io.nop.graphql.core.ast.GraphQLOperationType;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 领料 reverseConfirm 红冲测试（plan 2026-07-18-1745-2 Phase 4）。
 *
 * <p>覆盖 {@code ErpMfgMaterialIssueBizModel.reverseConfirm}：confirm 产 MANUFACTURING_ISSUE 凭证 + OUTGOING 移动后，
 * 调 reverseConfirm → 红冲凭证（原 isReversed=true + 红字凭证 postingType=REVERSAL）+ 反向移动单
 * （库存域 {@code IErpInvStockMoveBiz.reverse} 生成 REVERSAL 反向移动单）+ posted=false + docStatus=CANCELLED。
 *
 * <p>镜像 {@code TestErpMntSparePartUsageReversal} 范式（红冲正路径 + 非法态守卫）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgMaterialIssueReversal extends JunitAutoTestCase {

    static final Long ORG_ID = 1851L;
    static final Long WAREHOUSE_ID = 3851L;
    static final Long UOM_ID = 5851L;
    static final Long CURRENCY_ID = 6851L;
    static final Long ACCT_SCHEMA_ID = 7851L;
    static final Long P = 2851L;     // 产成品
    static final Long M1 = 2852L;    // 子件
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String MOVE_TYPE_OUTGOING = "OUTGOING";

    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_WIP = "1411";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- 场景 1：confirm → reverseConfirm 正路径（红冲凭证 + 反向移动单） ----------

    @Test
    public void testReverseConfirmRedReversesVoucherAndMove() {
        seedPeriodAndSubjects();
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9801L, P, M1, bd("2"));
        // 期初库存：入库 10@5 → balance M1 = 10, avgCost = 5
        generateIncoming(M1, "PR-MI-RV", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-MI-RV");
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"));
        Long issueId = seedIssue("MI-RV-001", woId);
        seedIssueLine(9301L, issueId, M1, bd("2"), wolId);

        // confirm 产 MANUFACTURING_ISSUE 凭证 + OUTGOING 移动 + posted=true
        assertEquals(0, confirm(issueId).getStatus(), "confirm 应成功");
        ErpMfgMaterialIssue confirmed = loadIssue(issueId);
        assertTrue(Boolean.TRUE.equals(confirmed.getPosted()), "前置：posted=true");
        assertEquals(ErpMfgConstants.ISSUE_STATUS_DONE, confirmed.getDocStatus());

        ErpFinVoucher original = findVoucher("MI-RV-001-MI", ErpFinBusinessType.MANUFACTURING_ISSUE);
        assertNotNull(original, "前置：应存在 MANUFACTURING_ISSUE 凭证");
        ErpInvStockMove originalMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE, "MI-RV-001");
        assertNotNull(originalMove, "前置：应存在 OUTGOING 移动单");
        assertEquals("DONE", originalMove.getDocStatus());

        // 执行 reverseConfirm
        assertEquals(0, reverseConfirm(issueId).getStatus(), "reverseConfirm 应成功");

        // 1. 状态翻转：posted=false + docStatus=CANCELLED
        ErpMfgMaterialIssue reversed = loadIssue(issueId);
        assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "红冲后 posted=false");
        assertEquals(ErpMfgConstants.ISSUE_STATUS_CANCELLED, reversed.getDocStatus(),
                "红冲后 docStatus=CANCELLED");

        // 2. 原 MANUFACTURING_ISSUE 凭证 isReversed=true
        ErpFinVoucher originalAfter = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(original.getId());
        assertTrue(Boolean.TRUE.equals(originalAfter.getIsReversed()),
                "原 MANUFACTURING_ISSUE 凭证应被标记 isReversed=true");

        // 3. 红字凭证存在（同 billHeadCode，postingType=REVERSAL）
        ErpFinVoucher redVoucher = findReversalVoucher("MI-RV-001-MI", ErpFinBusinessType.MANUFACTURING_ISSUE);
        assertNotNull(redVoucher, "应存在 MANUFACTURING_ISSUE 红字凭证");
        assertEquals("REVERSAL", redVoucher.getPostingType());

        // 4. 反向 OUTGOING 移动单（REVERSAL 移动单，relatedBillType=REVERSAL）
        ErpInvStockMove reversalMove = findReversalMove(originalMove.getCode());
        assertNotNull(reversalMove, "应存在 REVERSAL 反向冲销移动单");
        assertEquals("REVERSAL", reversalMove.getRelatedBillType(),
                "REVERSAL 移动单 relatedBillType=REVERSAL");
        assertEquals(originalMove.getCode(), reversalMove.getRelatedBillCode(),
                "REVERSAL 移动单 relatedBillCode 指向原移动单 code");
    }

    // ---------- 场景 2：未过账守卫（confirm 前 reverseConfirm 抛 ERR_MATERIAL_ISSUE_NOT_POSTED） ----------

    @Test
    public void testReverseConfirmRejectsNotPosted() {
        seedPeriodAndSubjects();
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9802L, P, M1, bd("1"));
        generateIncoming(M1, "PR-MI-REJ", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-MI-REJ");
        Long wolId = seedWorkOrderLine(woId, M1, bd("1"));
        Long issueId = seedIssue("MI-REJ-001", woId);
        seedIssueLine(9302L, issueId, M1, bd("1"), wolId);

        // 未 confirm 直接 reverseConfirm → 守卫拒绝
        ApiResponse<?> resp = reverseConfirm(issueId);
        assertEquals(ErpMfgErrors.ERR_MATERIAL_ISSUE_NOT_POSTED.getErrorCode(), resp.getCode(),
                "未过账领料单 reverseConfirm 应被守卫拒绝");

        // 状态不变（守卫前置，未进入红冲步骤）
        ErpMfgMaterialIssue issue = loadIssue(issueId);
        assertFalse(Boolean.TRUE.equals(issue.getPosted()));
        assertEquals(ErpMfgConstants.ISSUE_STATUS_DRAFT, issue.getDocStatus());
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> confirm(Long issueId) {
        return executeRpc(mutation, "ErpMfgMaterialIssue__confirm",
                ApiRequest.build(Map.of("issueId", issueId)));
    }

    private ApiResponse<?> reverseConfirm(Long issueId) {
        return executeRpc(mutation, "ErpMfgMaterialIssue__reverseConfirm",
                ApiRequest.build(Map.of("issueId", issueId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action,
                                        ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- 移动单生成（建立余额） ----------

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));

        ApiResponse<?> resp = executeRpc(mutation, "ErpInvStockMove__generateMove",
                ApiRequest.build(Map.of("request", req)));
        assertEquals(0, resp.getStatus(), "generateIncoming 应成功");
    }

    // ---------- finance / material seed ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedAcctSchema();
            seedOpenPeriod();
            seedSubject(SUBJECT_INVENTORY, "原材料存货", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP, "在制品", "ASSET", "DEBIT");
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.orm_propValueByName("id", ACCT_SCHEMA_ID);
        schema.setCode("ACCT-" + ORG_ID);
        schema.setName("账套 " + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.orm_propValueByName("nature", "FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode("2026-07-MI-RV");
        period.setName("2026-07-MI-RV");
        period.setOrgId(ORG_ID);
        period.orm_propValueByName("year", 2026);
        period.orm_propValueByName("month", 7);
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.orm_propValueByName("status", "OPEN");
        dao.saveEntity(period);
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

    private Long seedWorkOrder(String code) {
        Long id = 8810L + (long) Math.abs(code.hashCode() % 800);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(9801L);
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
        Long id = 9910L + (long) Math.abs((woId + "" + materialId).hashCode() % 800);
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
        Long id = 8920L + (long) Math.abs(code.hashCode() % 800);
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

    private ErpMfgMaterialIssue loadIssue(Long issueId) {
        return daoProvider.daoFor(ErpMfgMaterialIssue.class).getEntityById(issueId);
    }

    private ErpInvStockBalance findBalance(Long materialId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockMove findReversalMove(String originalMoveCode) {
        IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", "REVERSAL"));
        q.addFilter(eq("relatedBillCode", originalMoveCode));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpFinVoucher findVoucher(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && "NORMAL".equals(v.getPostingType())) {
                return v;
            }
        }
        return null;
    }

    private ErpFinVoucher findReversalVoucher(String billHeadCode, ErpFinBusinessType businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billHeadCode),
                eq("businessType", businessType.name())));
        List<ErpFinVoucherBillR> links = dao.findAllByQuery(q);
        for (ErpFinVoucherBillR link : links) {
            ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
            if (v != null && "REVERSAL".equals(v.getPostingType())) {
                return v;
            }
        }
        return null;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
