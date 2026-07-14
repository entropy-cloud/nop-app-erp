package app.erp.ast.service;

import app.erp.ast.biz.IErpAstValueAdjustmentBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstValueAdjustment;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产价值调整（减值/重估增值/重估减值）审批→过账→净值联动→反向红冲 端到端单测。
 *
 * <p>覆盖：三种调整类型分录 + 净值联动 + 三轴状态机非法迁移 + 反向红冲回退 + 强制审批。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstValueAdjustment extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstValueAdjustmentBiz adjustmentBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testImpairmentDecreasesNetValue() {
        long[] assetIdHolder = new long[1];
        Long adjustmentId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithSubjects("CAT-IMP", "减值类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-IMP", "减值资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            return seedAdjustment("ADJ-IMP-001", assetId, ErpAstConstants.ADJUSTMENT_TYPE_IMPAIRMENT,
                    new BigDecimal("3000"), LocalDate.of(2026, 7, 15));
        });

        assertEquals(0, submitForApproval(adjustmentId).getStatus(), "提交成功");
        assertEquals(0, approve(adjustmentId).getStatus(), "审核成功");
        ErpAstValueAdjustment adjustment = daoProvider.daoFor(ErpAstValueAdjustment.class).getEntityById(adjustmentId);

        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, adjustment.getApproveStatus());
        assertEquals(ErpAstConstants.DOC_STATUS_ACTIVE, adjustment.getDocStatus());
        assertTrue(Boolean.TRUE.equals(adjustment.getPosted()), "减值过账 posted=true");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        // 净值 12000 - 3000 = 9000
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("9000")), "减值后净值=9000");

        assertTrue(!findBillLinks("ADJ-IMP-001", "VALUE_ADJUSTMENT").isEmpty(), "VALUE_ADJUSTMENT 凭证回链已落库");
    }

    @Test
    public void testRevaluationUpIncreasesNetValue() {
        long[] assetIdHolder = new long[1];
        Long adjustmentId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithSubjects("CAT-REV-UP", "重估增值类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-REV-UP", "重估增值资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            return seedAdjustment("ADJ-REV-UP-001", assetId, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_UP,
                    new BigDecimal("5000"), LocalDate.of(2026, 7, 15));
        });

        assertEquals(0, submitForApproval(adjustmentId).getStatus(), "提交成功");
        assertEquals(0, approve(adjustmentId).getStatus(), "审核成功");
        ErpAstValueAdjustment adjustment = daoProvider.daoFor(ErpAstValueAdjustment.class).getEntityById(adjustmentId);

        assertTrue(Boolean.TRUE.equals(adjustment.getPosted()), "重估增值过账 posted=true");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        // 净值 12000 + 5000 = 17000
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("17000")), "重估增值后净值=17000");
    }

    @Test
    public void testRevaluationDownDecreasesNetValue() {
        long[] assetIdHolder = new long[1];
        Long adjustmentId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithSubjects("CAT-REV-DOWN", "重估减值类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-REV-DOWN", "重估减值资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            return seedAdjustment("ADJ-REV-DOWN-001", assetId, ErpAstConstants.ADJUSTMENT_TYPE_REVALUATION_DOWN,
                    new BigDecimal("2000"), LocalDate.of(2026, 7, 15));
        });

        assertEquals(0, submitForApproval(adjustmentId).getStatus(), "提交成功");
        assertEquals(0, approve(adjustmentId).getStatus(), "审核成功");
        ErpAstValueAdjustment adjustment = daoProvider.daoFor(ErpAstValueAdjustment.class).getEntityById(adjustmentId);

        assertTrue(Boolean.TRUE.equals(adjustment.getPosted()), "重估减值过账 posted=true");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        // 净值 12000 - 2000 = 10000
        assertEquals(0, nz(asset.getNetBookValue()).compareTo(new BigDecimal("10000")), "重估减值后净值=10000");
    }

    @Test
    public void testIllegalTransitionApproveBeforeSubmit() {
        Long adjustmentId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithSubjects("CAT-ILL", "非法迁移类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-ILL", "非法迁移资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return seedAdjustment("ADJ-ILL-001", assetId, ErpAstConstants.ADJUSTMENT_TYPE_IMPAIRMENT,
                    new BigDecimal("1000"), LocalDate.of(2026, 7, 15));
        });

        ApiResponse<?> bad = approve(adjustmentId);
        assertTrue(bad.getStatus() != 0, "未提交直接审核应被拒绝");
    }

    @Test
    public void testCancelDraftAdjustment() {
        Long adjustmentId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithSubjects("CAT-CANC", "作废类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-CANC", "作废资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return seedAdjustment("ADJ-CANC-001", assetId, ErpAstConstants.ADJUSTMENT_TYPE_IMPAIRMENT,
                    new BigDecimal("1000"), LocalDate.of(2026, 7, 15));
        });

        ErpAstValueAdjustment cancelled = ormTemplate.runInSession(session -> adjustmentBiz.cancel(adjustmentId, CTX));
        assertEquals(ErpAstConstants.DOC_STATUS_CANCELLED, cancelled.getDocStatus(), "作废后 docStatus=CANCELLED");
    }

    @Test
    public void testReverseRollsBackNetValue() {
        long[] assetIdHolder = new long[1];
        Long adjustmentId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategoryWithSubjects("CAT-REV-IMP", "红冲减值类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-REV-IMP", "红冲减值资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            return seedAdjustment("ADJ-REV-IMP-001", assetId, ErpAstConstants.ADJUSTMENT_TYPE_IMPAIRMENT,
                    new BigDecimal("3000"), LocalDate.of(2026, 7, 15));
        });

        assertEquals(0, submitForApproval(adjustmentId).getStatus(), "提交成功");
        assertEquals(0, approve(adjustmentId).getStatus(), "审核成功");
        ErpAstAsset afterApprove = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(0, nz(afterApprove.getNetBookValue()).compareTo(new BigDecimal("9000")), "减值后净值=9000");

        assertEquals(0, reverseApprove(adjustmentId).getStatus(), "反审核成功");
        ErpAstValueAdjustment reversed = daoProvider.daoFor(ErpAstValueAdjustment.class).getEntityById(adjustmentId);
        assertFalse(Boolean.TRUE.equals(reversed.getPosted()), "红冲后 posted=false");
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, reversed.getApproveStatus());

        ErpAstAsset afterReverse = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(0, nz(afterReverse.getNetBookValue()).compareTo(new BigDecimal("12000")), "红冲回退净值=12000");

        assertTrue(isAllVouchersReversed("ADJ-REV-IMP-001", "VALUE_ADJUSTMENT"), "VALUE_ADJUSTMENT 凭证已红字冲销");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submitForApproval(Long id) {
        return executeRpc("ErpAstValueAdjustment__submitForApproval", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc("ErpAstValueAdjustment__approve", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc("ErpAstValueAdjustment__reverseApprove", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> executeRpc(String action, Map<String, Object> data) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, action, ApiRequest.build(data));
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- helpers ----------

    private void seedCoreBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
        AstTestSupport.seedSubject(daoProvider, "6702", "资产减值损失");
        AstTestSupport.seedSubject(daoProvider, "1604", "固定资产减值准备");
        AstTestSupport.seedSubject(daoProvider, "4002", "资本公积");
    }

    private Long seedCategoryWithSubjects(String code, String name) {
        return AstTestSupport.seedCategory(daoProvider, code, name,
                ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
    }

    private Long seedAdjustment(String code, Long assetId, String adjustmentType, BigDecimal amount,
                                LocalDate businessDate) {
        IEntityDao<ErpAstValueAdjustment> dao = daoProvider.daoFor(ErpAstValueAdjustment.class);
        ErpAstValueAdjustment adj = new ErpAstValueAdjustment();
        adj.setCode(code);
        adj.setOrgId(1L);
        adj.setAssetId(assetId);
        adj.setAdjustmentType(adjustmentType);
        adj.setAdjustmentAmount(amount);
        adj.setCurrencyId(1L);
        adj.setExchangeRate(BigDecimal.ONE);
        adj.setBusinessDate(businessDate);
        adj.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        adj.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(adj);
        return adj.getId();
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
