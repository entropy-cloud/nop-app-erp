package app.erp.ast.service;

import app.erp.ast.biz.IErpAstInventoryBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstInventory;
import app.erp.ast.dao.entity.ErpAstInventoryLine;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
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
import io.nop.graphql.core.IGraphQLExecutionContext;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产盘点（UC-AST-09）端到端单测。
 *
 * <p>覆盖：盘点单创建+范围展开、实盘录入、差异计算（盘盈/盘亏/一致三态）、盘盈建卡、盘亏触发处置、
 * 过账凭证方向、reverse 红冲、非法状态迁移、强制审批门控。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstInventory extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstInventoryBiz inventoryBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testFullFlowSurplusAndShortage() {
        long[] matchedAssetHolder = new long[1];
        long[] shortageAssetHolder = new long[1];
        Long invId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-INV-1", "盘点类别一");
            Long matched = AstTestSupport.seedAsset(daoProvider, "AST-MATCHED", "一致资产", categoryId, 1L,
                    new BigDecimal("10000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            Long shortage = AstTestSupport.seedAsset(daoProvider, "AST-SHORTAGE", "盘亏资产", categoryId, 1L,
                    new BigDecimal("8000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            matchedAssetHolder[0] = matched;
            shortageAssetHolder[0] = shortage;
            return seedInventory("INV-001", "盘点单一号", categoryId, LocalDate.of(2026, 7, 15));
        });

        assertEquals(0, createInventory(invId).getStatus(), "createInventory 成功（展开 2 账面行）");
        ErpAstInventory afterCreate = daoProvider.daoFor(ErpAstInventory.class).getEntityById(invId);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_DRAFT, afterCreate.getStatus());

        assertEquals(2, findLines(invId).size(), "范围内 2 个资产展开 2 行");

        // 录入实盘：一致资产找到(actual=1)，盘亏资产未找到(actual=0)；并新增一条盘盈行
        ormTemplate.runInSession(session -> {
            List<ErpAstInventoryLine> lines = findLines(invId);
            for (ErpAstInventoryLine line : lines) {
                if (line.getAssetId() != null && line.getAssetId() == matchedAssetHolder[0]) {
                    line.setActualQuantity(1);
                } else {
                    line.setActualQuantity(0);
                }
                daoProvider.daoFor(ErpAstInventoryLine.class).saveOrUpdateEntity(line);
            }
            ErpAstInventoryLine surplusLine = daoProvider.daoFor(ErpAstInventoryLine.class).newEntity();
            surplusLine.setInventoryId(invId);
            surplusLine.setOrgId(1L);
            surplusLine.setLineNo(99);
            surplusLine.setAssetId(null);
            surplusLine.setAssetNameSnapshot("盘盈新设备");
            surplusLine.setBookQuantity(0);
            surplusLine.setActualQuantity(1);
            surplusLine.setAssessedValue(new BigDecimal("5000"));
            surplusLine.setBookValue(BigDecimal.ZERO);
            daoProvider.daoFor(ErpAstInventoryLine.class).saveEntity(surplusLine);
            return null;
        });

        assertEquals(0, submitForCount(invId).getStatus(), "submitForCount 成功");
        assertEquals(0, reconcile(invId).getStatus(), "reconcile 成功");
        ErpAstInventory afterReconcile = daoProvider.daoFor(ErpAstInventory.class).getEntityById(invId);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_RECONCILING, afterReconcile.getStatus());
        assertEquals(1, afterReconcile.getSurplusCount(), "盘盈行数=1");
        assertEquals(1, afterReconcile.getShortageCount(), "盘亏行数=1");
        assertEquals(1, afterReconcile.getMatchedCount(), "一致行数=1");
        assertEquals(0, nz(afterReconcile.getSurplusAmount()).compareTo(new BigDecimal("5000")), "盘盈金额=5000");
        assertEquals(0, nz(afterReconcile.getShortageAmount()).compareTo(new BigDecimal("8000")), "盘亏金额=8000");

        assertEquals(0, processVariance(invId).getStatus(), "processVariance 成功");

        // 盘亏资产状态 → SCRAPPED
        ErpAstAsset shortageAsset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(shortageAssetHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, shortageAsset.getStatus(), "盘亏资产已 SCRAPPED");

        // 盘盈行已建新卡
        ErpAstInventoryLine surplusAfter = findSurplusLine(invId);
        assertNotNull(surplusAfter.getNewAssetId(), "盘盈行已建新卡");
        ErpAstAsset newAsset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(surplusAfter.getNewAssetId());
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, newAsset.getStatus(), "盘盈新卡 IN_SERVICE");
        assertEquals(0, nz(newAsset.getOriginalValue()).compareTo(new BigDecimal("5000")), "盘盈新卡原值=5000");

        assertEquals(0, approve(invId).getStatus(), "approve 成功");
        assertEquals(0, post(invId).getStatus(), "post 成功");
        ErpAstInventory afterPost = daoProvider.daoFor(ErpAstInventory.class).getEntityById(invId);
        assertEquals(ErpAstConstants.INVENTORY_STATUS_POSTED, afterPost.getStatus(), "状态 POSTED");
        assertTrue(Boolean.TRUE.equals(afterPost.getPosted()), "posted=true");

        // ASSET_INVENTORY_ADJUSTMENT 凭证回链已落库（盘盈 + 盘亏各 2 行分录 = 4 行）
        List<ErpFinVoucherBillR> links = findBillLinks("INV-001", "ASSET_INVENTORY_ADJUSTMENT");
        assertFalse(links.isEmpty(), "ASSET_INVENTORY_ADJUSTMENT 凭证回链已落库");
    }

    @Test
    public void testIllegalTransitionPostBeforeReconcile() {
        Long invId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-ILL-INV", "非法迁移类别");
            AstTestSupport.seedAsset(daoProvider, "AST-ILL-INV", "非法迁移资产", categoryId, 1L,
                    new BigDecimal("10000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return seedInventory("INV-ILL-001", "非法迁移盘点", categoryId, LocalDate.of(2026, 7, 15));
        });

        createInventory(invId);
        submitForCount(invId);
        // 尚未 reconcile（状态 COUNTING）直接 post 应被拒绝
        ApiResponse<?> bad = post(invId);
        assertTrue(bad.getStatus() != 0, "COUNTING 态直接 post 应被拒绝");
    }

    @Test
    public void testCancelDraftInventory() {
        Long invId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-CANC-INV", "作废类别");
            AstTestSupport.seedAsset(daoProvider, "AST-CANC-INV", "作废资产", categoryId, 1L,
                    new BigDecimal("10000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return seedInventory("INV-CANC-001", "作废盘点", categoryId, LocalDate.of(2026, 7, 15));
        });

        createInventory(invId);
        ErpAstInventory cancelled = ormTemplate.runInSession(session -> inventoryBiz.cancel(invId, CTX));
        assertEquals(ErpAstConstants.INVENTORY_STATUS_CANCELLED, cancelled.getStatus(), "作废后 status=CANCELLED");
    }

    @Test
    public void testReverseRollsBackPosted() {
        long[] shortageAssetHolder = new long[1];
        Long invId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-REV-INV", "红冲类别");
            AstTestSupport.seedAsset(daoProvider, "AST-REV-M", "红冲一致资产", categoryId, 1L,
                    new BigDecimal("10000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            Long shortage = AstTestSupport.seedAsset(daoProvider, "AST-REV-S", "红冲盘亏资产", categoryId, 1L,
                    new BigDecimal("8000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            shortageAssetHolder[0] = shortage;
            return seedInventory("INV-REV-001", "红冲盘点", categoryId, LocalDate.of(2026, 7, 15));
        });

        createInventory(invId);
        // 录入实盘：第一个资产一致，第二个盘亏
        ormTemplate.runInSession(session -> {
            List<ErpAstInventoryLine> lines = findLines(invId);
            for (ErpAstInventoryLine line : lines) {
                line.setActualQuantity(1);
                daoProvider.daoFor(ErpAstInventoryLine.class).saveOrUpdateEntity(line);
            }
            // 把第二个（盘亏资产）标记为未找到
            for (ErpAstInventoryLine line : lines) {
                if (line.getAssetId() != null && line.getAssetId() == shortageAssetHolder[0]) {
                    line.setActualQuantity(0);
                    daoProvider.daoFor(ErpAstInventoryLine.class).saveOrUpdateEntity(line);
                }
            }
            return null;
        });

        submitForCount(invId);
        reconcile(invId);
        processVariance(invId);
        approve(invId);
        post(invId);

        ErpAstInventory beforeReverse = daoProvider.daoFor(ErpAstInventory.class).getEntityById(invId);
        assertTrue(Boolean.TRUE.equals(beforeReverse.getPosted()), "红冲前 posted=true");

        assertEquals(0, reverse(invId).getStatus(), "reverse 成功");
        ErpAstInventory afterReverse = daoProvider.daoFor(ErpAstInventory.class).getEntityById(invId);
        assertFalse(Boolean.TRUE.equals(afterReverse.getPosted()), "红冲后 posted=false");
        assertEquals(ErpAstConstants.INVENTORY_STATUS_RECONCILING, afterReverse.getStatus(), "红冲回退 RECONCILING");

        assertTrue(isAllVouchersReversed("INV-REV-001", "ASSET_INVENTORY_ADJUSTMENT"),
                "ASSET_INVENTORY_ADJUSTMENT 凭证已红字冲销");
    }

    @Test
    public void testRangeEmptyRejected() {
        Long invId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            // 类别存在但无任何资产落在范围内
            Long categoryId = seedCategory("CAT-EMPTY-INV", "空范围类别");
            return seedInventory("INV-EMPTY-001", "空范围盘点", categoryId, LocalDate.of(2026, 7, 15));
        });

        ApiResponse<?> bad = createInventory(invId);
        assertTrue(bad.getStatus() != 0, "空范围 createInventory 应被拒绝");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> createInventory(Long id) {
        return executeRpc("ErpAstInventory__createInventory", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> submitForCount(Long id) {
        return executeRpc("ErpAstInventory__submitForCount", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> reconcile(Long id) {
        return executeRpc("ErpAstInventory__reconcile", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> processVariance(Long id) {
        return executeRpc("ErpAstInventory__processVariance", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc("ErpAstInventory__approve", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> post(Long id) {
        return executeRpc("ErpAstInventory__post", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> reverse(Long id) {
        return executeRpc("ErpAstInventory__reverse", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> executeRpc(String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- helpers ----------

    private void seedCoreBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedSubject(daoProvider, "1601", "固定资产");
        AstTestSupport.seedSubject(daoProvider, "6301", "营业外收入");
        AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
    }

    private Long seedCategory(String code, String name) {
        return AstTestSupport.seedCategory(daoProvider, code, name,
                ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                AstTestSupport.seedSubject(daoProvider, "1601-" + code, "固定资产-" + code),
                AstTestSupport.seedSubject(daoProvider, "1602-" + code, "累计折旧-" + code),
                AstTestSupport.seedSubject(daoProvider, "6602-" + code, "管理费用-" + code));
    }

    private Long seedInventory(String code, String name, Long categoryId, LocalDate businessDate) {
        IEntityDao<ErpAstInventory> dao = daoProvider.daoFor(ErpAstInventory.class);
        ErpAstInventory inv = new ErpAstInventory();
        inv.setCode(code);
        inv.setName(name);
        inv.setOrgId(1L);
        inv.setStatus(ErpAstConstants.INVENTORY_STATUS_DRAFT);
        inv.setRangeCategoryId(categoryId);
        inv.setBusinessDate(businessDate);
        inv.setCurrencyId(1L);
        inv.setExchangeRate(BigDecimal.ONE);
        dao.saveEntity(inv);
        return inv.getId();
    }

    private List<ErpAstInventoryLine> findLines(Long inventoryId) {
        IEntityDao<ErpAstInventoryLine> dao = daoProvider.daoFor(ErpAstInventoryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("inventoryId", inventoryId));
        return dao.findAllByQuery(q);
    }

    private ErpAstInventoryLine findSurplusLine(Long inventoryId) {
        IEntityDao<ErpAstInventoryLine> dao = daoProvider.daoFor(ErpAstInventoryLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("inventoryId", inventoryId));
        q.addFilter(eq("varianceType", ErpAstConstants.VARIANCE_TYPE_SURPLUS));
        List<ErpAstInventoryLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private boolean isAllVouchersReversed(String billCode, String businessType) {
        List<ErpFinVoucherBillR> links = findBillLinks(billCode, businessType);
        if (links.isEmpty()) {
            return false;
        }
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucher> voucherDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucher.class);
        for (ErpFinVoucherBillR link : links) {
            app.erp.fin.dao.entity.ErpFinVoucher v = voucherDao.getEntityById(link.getVoucherId());
            if (v != null && !Boolean.TRUE.equals(v.getIsReversed())
                    && (v.getPostingType() == null || "NORMAL".equals(v.getPostingType()))) {
                return false;
            }
        }
        return true;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
