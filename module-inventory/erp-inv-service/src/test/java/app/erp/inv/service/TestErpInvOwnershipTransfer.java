package app.erp.inv.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinArApItem;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.inv.dao.entity.ErpInvOwnershipTransfer;
import app.erp.inv.dao.entity.ErpInvOwnershipTransferLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.md.dao.entity.ErpMdAcctSchema;
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
 * Phase 3 端到端测试：所有权转移单（consignment.md，plan 2026-07-04-0549-1）。
 *
 * <p>覆盖：(a) VMI_CONSUME 余额重分类 + OWNERSHIP_TRANSFER 凭证回链 posted=true + DIRECTION_PAYABLE 辅助账 openAmount>0；
 * (b) ownership-tracking-enabled=false 时 done 抛 ERR_OWNERSHIP_TRACKING_DISABLED；
 * (c) sourceLocId≠destLocId 抛 ERR_OWNERSHIP_TRANSFER_LOC_MISMATCH；
 * (d) 非 VMI_CONSUME 不生成应付。
 *
 * <p>VMI_SUPPLIER 余额经直接余额种子建立（不依赖未实现的 VMI 收货流，见 Non-Goals）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvOwnershipTransfer extends JunitAutoTestCase {

    static final String ORG_ID = "1501";
    static final String WAREHOUSE_ID = "3501";
    static final String LOCATION_ID = "4501";
    static final String PARTNER_ID = "8501";   // 供应商往来单位
    static final String MATERIAL_ID = "2501";
    static final String CURRENCY_ID = "6501";
    static final String ACCT_SCHEMA_ID = "7501";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    /** (a) VMI_CONSUME happy path：余额重分类 + 凭证回链 + DIRECTION_PAYABLE 辅助账。 */
    @Test
    public void testVmiConsumeReclassifyAndPostPayable() {
        seedPeriodAndSubjects();
        enableOwnershipTracking();
        // 直接余额种子建立 VMI_SUPPLIER 余额（不依赖未实现的 VMI 收货流）
        seedBalance(MATERIAL_ID, ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER, PARTNER_ID,
                new BigDecimal("10"), new BigDecimal("5"));

        String transferId = createTransfer(ErpInvConstants.TRANSFER_TYPE_VMI_CONSUME,
                ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER, ErpInvConstants.OWNERSHIP_TYPE_OWNED,
                LOCATION_ID, LOCATION_ID, new BigDecimal("4"), new BigDecimal("5"));

        assertEquals(0, callConfirm(transferId).getStatus(), "DRAFT→CONFIRMED 应成功");
        ApiResponse<?> doneResp = callDone(transferId);
        assertEquals(0, doneResp.getStatus(), "CONFIRMED→DONE 应成功（含调账+过账）");

        ErpInvOwnershipTransfer transfer = daoProvider.daoFor(ErpInvOwnershipTransfer.class)
                .getEntityById(transferId);
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE, transfer.getDocStatus(), "应 DONE");
        assertEquals(true, transfer.getPosted(), "VMI_CONSUME DONE 应过账 posted=true");

        // 余额重分类：VMI_SUPPLIER 减至 6，OWNED 增至 4（数量守恒，物理位置不变）
        ErpInvStockBalance vmi = findBalance(MATERIAL_ID, ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER, PARTNER_ID);
        assertNotNull(vmi, "VMI_SUPPLIER 子余额应存在");
        assertEquals(0, vmi.getTotalQuantity().compareTo(new BigDecimal("6")), "VMI_SUPPLIER 余额 10-4=6");
        ErpInvStockBalance owned = findBalance(MATERIAL_ID, ErpInvConstants.OWNERSHIP_TYPE_OWNED, null);
        assertNotNull(owned, "OWNED 子余额应被重分类创建");
        assertEquals(0, owned.getTotalQuantity().compareTo(new BigDecimal("4")), "OWNED 余额=4");

        // OWNERSHIP_TRANSFER 凭证回链
        ErpFinVoucherBillR link = findBillLink(transfer.getCode());
        assertNotNull(link, "应生成业财回链");
        ErpFinVoucher voucher = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(link.getVoucherId());
        assertNotNull(voucher, "凭证应落库");
        assertEquals("POSTED", voucher.getDocStatus(), "凭证状态=已过账");
        assertTrue(voucher.getTotalDebit().compareTo(new BigDecimal("20")) == 0,
                "借方合计=存货 20 (=4×5)");
        assertTrue(voucher.getTotalCredit().compareTo(new BigDecimal("20")) == 0,
                "贷方合计=应付-供应商 20");

        // DIRECTION_PAYABLE 辅助账 openAmount>0
        ErpFinArApItem apItem = findApItem(transfer.getCode());
        assertNotNull(apItem, "应生成应付辅助账");
        assertEquals("PAYABLE", apItem.getDirection(), "方向=应付");
        assertEquals(ErpFinSourceBill.OWNERSHIP_TRANSFER, apItem.getSourceBillType(),
                "sourceBillType=OWNERSHIP_TRANSFER");
        assertTrue(apItem.getOpenAmountFunctional().compareTo(BigDecimal.ZERO) > 0,
                "openAmountFunctional>0（待供应商采购发票核销）");
    }

    /** (b) ownership-tracking-enabled=false 时 done 抛 ERR_OWNERSHIP_TRACKING_DISABLED。 */
    @Test
    public void testTrackingDisabledDoneThrows() {
        seedPeriodAndSubjects();
        disableOwnershipTracking();
        // disabled 时余额 seed 无 ownerId（键不含 owner）
        seedBalance(MATERIAL_ID, ErpInvConstants.OWNERSHIP_TYPE_OWNED, null,
                new BigDecimal("10"), new BigDecimal("5"));

        String transferId = createTransfer(ErpInvConstants.TRANSFER_TYPE_VMI_CONSUME,
                ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER, ErpInvConstants.OWNERSHIP_TYPE_OWNED,
                LOCATION_ID, LOCATION_ID, new BigDecimal("4"), new BigDecimal("5"));

        callConfirm(transferId);
        ApiResponse<?> doneResp = callDone(transferId);
        assertTrue(doneResp.getStatus() != 0, "tracking-disabled 时 done 应抛错");
        assertTrue(doneResp.getCode() != null
                        && doneResp.getCode().contains("ownership-tracking-disabled"),
                "错误码应为 ERR_OWNERSHIP_TRACKING_DISABLED: " + doneResp.getCode() + " / " + doneResp.getMsg());
    }

    /** (c) sourceLocId≠destLocId 抛 ERR_OWNERSHIP_TRANSFER_LOC_MISMATCH（confirm 阶段校验）。 */
    @Test
    public void testLocationMismatchThrows() {
        seedPeriodAndSubjects();
        enableOwnershipTracking();
        seedBalance(MATERIAL_ID, ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER, PARTNER_ID,
                new BigDecimal("10"), new BigDecimal("5"));

        String transferId = createTransfer(ErpInvConstants.TRANSFER_TYPE_VMI_CONSUME,
                ErpInvConstants.OWNERSHIP_TYPE_VMI_SUPPLIER, ErpInvConstants.OWNERSHIP_TYPE_OWNED,
                LOCATION_ID, "4999", new BigDecimal("4"), new BigDecimal("5"));

        ApiResponse<?> resp = callConfirm(transferId);
        assertTrue(resp.getStatus() != 0, "sourceLocId≠destLocId 应抛错");
        assertTrue(resp.getCode() != null
                        && resp.getCode().contains("ownership-transfer-loc-mismatch"),
                "错误码应为 ERR_OWNERSHIP_TRANSFER_LOC_MISMATCH: " + resp.getCode() + " / " + resp.getMsg());
    }

    /** (d) 非 VMI_CONSUME（OWNERSHIP_TO_CUSTOMER）不生成应付（无供应商结算）。 */
    @Test
    public void testNonVmiConsumeNoPayable() {
        seedPeriodAndSubjects();
        enableOwnershipTracking();
        // OWNED 余额种子（转客户前自有）
        seedBalance(MATERIAL_ID, ErpInvConstants.OWNERSHIP_TYPE_OWNED, null,
                new BigDecimal("10"), new BigDecimal("5"));

        String transferId = createTransfer(ErpInvConstants.TRANSFER_TYPE_OWNERSHIP_TO_CUSTOMER,
                ErpInvConstants.OWNERSHIP_TYPE_OWNED, ErpInvConstants.OWNERSHIP_TYPE_CUSTOMER_PROVIDED,
                LOCATION_ID, LOCATION_ID, new BigDecimal("3"), new BigDecimal("5"));

        callConfirm(transferId);
        ApiResponse<?> doneResp = callDone(transferId);
        assertEquals(0, doneResp.getStatus(), "OWNERSHIP_TO_CUSTOMER DONE 应成功（调账）");

        ErpInvOwnershipTransfer transfer = daoProvider.daoFor(ErpInvOwnershipTransfer.class)
                .getEntityById(transferId);
        assertEquals(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE, transfer.getDocStatus());
        assertEquals(false, transfer.getPosted(), "非 VMI_CONSUME 不生成应付，posted=false");
        assertNull(findApItem(transfer.getCode()), "不应生成应付辅助账");
        assertEquals(0, countBillLinks(transfer.getCode()), "不应生成凭证/回链");
    }

    // ---------- helpers ----------

    private String createTransfer(String transferType, String fromType, String toType,
                                String sourceLocId, String destLocId, BigDecimal qty, BigDecimal unitCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvOwnershipTransfer> headDao = daoProvider.daoFor(ErpInvOwnershipTransfer.class);
            ErpInvOwnershipTransfer head = new ErpInvOwnershipTransfer();
            head.setCode("OT-" + System.nanoTime());
            head.setOrgId(ORG_ID);
            head.setTransferType(transferType);
            head.setPartnerId(PARTNER_ID);
            head.setBusinessDate(LocalDate.of(2026, 7, 4));
            head.setWarehouseId(WAREHOUSE_ID);
            head.setSourceLocId(sourceLocId);
            head.setDestLocId(destLocId);
            head.setFromOwnershipType(fromType);
            head.setToOwnershipType(toType);
            head.setCurrencyId(CURRENCY_ID);
            head.setDocStatus(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT);
            head.setPosted(false);
            headDao.saveEntity(head);

            IEntityDao<ErpInvOwnershipTransferLine> lineDao = daoProvider.daoFor(ErpInvOwnershipTransferLine.class);
            ErpInvOwnershipTransferLine line = new ErpInvOwnershipTransferLine();
            line.setTransferId(head.getId());
            line.setLineNo(10);
            line.setMaterialId(MATERIAL_ID);
            line.setQuantity(qty);
            line.setUnitCost(unitCost);
            line.setTotalCost(qty.multiply(unitCost));
            lineDao.saveEntity(line);
        });
        ormTemplate.flushSession();
        List<ErpInvOwnershipTransfer> list = daoProvider.daoFor(ErpInvOwnershipTransfer.class).findAllByQuery(
                new QueryBean());
        return list.get(list.size() - 1).getId();
    }

    private ApiResponse<?> callConfirm(String transferId) {
        return executeRpc(mutation, "ErpInvOwnershipTransfer__confirm",
                ApiRequest.build(Map.of("transferId", transferId)));
    }

    private ApiResponse<?> callDone(String transferId) {
        return executeRpc(mutation, "ErpInvOwnershipTransfer__done",
                ApiRequest.build(Map.of("transferId", transferId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedBalance(String materialId, String ownershipType, String ownerId,
                             BigDecimal qty, BigDecimal unitCost) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
            ErpInvStockBalance balance = new ErpInvStockBalance();
            balance.setOrgId(ORG_ID);
            balance.setMaterialId(materialId);
            balance.setWarehouseId(WAREHOUSE_ID);
            balance.setLocationId(LOCATION_ID);
            balance.setTotalQuantity(qty);
            balance.setReservedQuantity(BigDecimal.ZERO);
            balance.setLockedQuantity(BigDecimal.ZERO);
            balance.setAvailableQuantity(qty);
            balance.setCostMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
            balance.setAvgCost(unitCost);
            balance.setTotalCost(qty.multiply(unitCost));
            balance.setCurrencyId(CURRENCY_ID);
            balance.setOwnershipType(ownershipType);
            balance.setOwnerId(ownerId);
            dao.saveEntity(balance);
        });
        ormTemplate.flushSession();
    }

    private ErpInvStockBalance findBalance(String materialId, String ownershipType, String ownerId) {
        IEntityDao<ErpInvStockBalance> dao = daoProvider.daoFor(ErpInvStockBalance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("orgId", ORG_ID));
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        q.addFilter(eq("ownershipType", ownershipType));
        if (ownerId != null) {
            q.addFilter(eq("ownerId", ownerId));
        }
        List<ErpInvStockBalance> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpFinArApItem findApItem(String transferCode) {
        return daoProvider.daoFor(ErpFinArApItem.class).findAllByQuery(new QueryBean()).stream()
                .filter(i -> transferCode.equals(i.getSourceBillCode()))
                .findFirst().orElse(null);
    }

    private long countBillLinks(String transferCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> transferCode.equals(l.getBillCode())).count();
    }

    private ErpFinVoucherBillR findBillLink(String transferCode) {
        return daoProvider.daoFor(ErpFinVoucherBillR.class).findAllByQuery(new QueryBean()).stream()
                .filter(l -> transferCode.equals(l.getBillCode())).findFirst().orElse(null);
    }

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07", 2026, 7, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 31), "OPEN");
            seedSubject("1401", "库存商品", "ASSET", "DEBIT");
            seedSubject("2202", "应付账款-供应商", "LIABILITY", "CREDIT");
            // 账套（供 PostingEvent.acctSchemaId 解析，VMI_CONSUME 过账凭证 voucherLine.acctSchemaId 必填）
            seedAcctSchema(ACCT_SCHEMA_ID);
        });
    }

    private void seedAcctSchema(String id) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.orm_propValueByName("id", id);
        schema.setCode("AS-" + id);
        schema.setName("AcctSchema " + id);
        schema.setOrgId(ORG_ID);
        schema.orm_propValueByName("nature", "FINANCIAL");
        schema.orm_propValueByName("functionalCurrencyId", CURRENCY_ID);
        schema.setStatus("ACTIVE");
        schema.setCostingMethod(ErpInvConstants.COST_METHOD_MOVING_AVERAGE);
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code, int year, int month, LocalDate start, LocalDate end, String status) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(ORG_ID);
        period.setYear(year);
        period.setMonth(month);
        period.setStartDate(start);
        period.setEndDate(end);
        period.setStatus(status);
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.setSubjectClass(subjectClass);
        subject.setDirection(direction);
        subject.setStatus("ACTIVE");
        dao.saveEntity(subject);
    }

    private void enableOwnershipTracking() {
        AppConfigProvider.set(ErpInvConstants.CONFIG_OWNERSHIP_TRACKING_ENABLED, "true");
    }

    private void disableOwnershipTracking() {
        AppConfigProvider.set(ErpInvConstants.CONFIG_OWNERSHIP_TRACKING_ENABLED, "false");
    }

    /** AppConfig 写入辅助（隔离对 AppConfig.getConfigProvider() 的直接依赖）。 */
    static final class AppConfigProvider {
        static void set(String key, String value) {
            io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
        }
    }

    /** sourceBillType 常量（与 ErpFinConstants.SOURCE_BILL_OWNERSHIP_TRANSFER 同值，测试断言用）。 */
    static final class ErpFinSourceBill {
        static final String OWNERSHIP_TRANSFER = "OWNERSHIP_TRANSFER";
    }
}
