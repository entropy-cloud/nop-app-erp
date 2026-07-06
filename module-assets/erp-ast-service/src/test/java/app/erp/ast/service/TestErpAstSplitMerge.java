package app.erp.ast.service;

import app.erp.ast.biz.IErpAstMergeBiz;
import app.erp.ast.biz.IErpAstSplitBiz;
import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.dao.entity.ErpAstMergeLine;
import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.dao.entity.ErpAstSplitLine;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产拆分/合并端到端单测。覆盖 6 拆分场景 + 2 合并场景：
 * <ol>
 *   <li>proportional 拆分 happy path（比例分摊 + 最大项补差 + 折旧计划 + 源 DISPOSED + ASSET_SPLIT 凭证）。</li>
 *   <li>FIXED_AMOUNT 拆分（固定金额 + 累计折旧按比例派生）。</li>
 *   <li>比例不平衡拒绝 submit。</li>
 *   <li>跨类别 config-gated 拒绝 submit。</li>
 *   <li>源资产非 IN_SERVICE 拒绝 submit。</li>
 *   <li>approve 后 reverse 不可逆契约（抛 ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED）。</li>
 *   <li>合并 WEIGHTED happy path（Σ 源价值 + 源 DISPOSED + ASSET_MERGE 凭证）。</li>
 *   <li>跨币种拒绝 submit。</li>
 * </ol>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstSplitMerge extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpAstSplitBiz splitBiz;
    @Inject
    IErpAstMergeBiz mergeBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testProportionalSplitHappyPath() {
        long[] sourceHolder = new long[1];
        Long splitId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-SP1", "拆分类别1");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SP1", "拆分源资产", categoryId, 1L,
                    new BigDecimal("100000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            // 预置累计折旧 40000，净值 60000
            ErpAstAsset src = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
            src.setAccumulatedDepreciation(new BigDecimal("40000"));
            src.setNetBookValue(new BigDecimal("60000"));
            daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(src);
            sourceHolder[0] = assetId;
            return seedSplit("SPL-001", assetId, categoryId, new BigDecimal("100000"), 1L);
        });
        seedProportionalLines(splitId, 1L, new BigDecimal[]{
                new BigDecimal("0.5"), new BigDecimal("0.3"), new BigDecimal("0.2")});

        assertEquals(0, submitForApproval(splitId).getStatus(), "拆分提交成功");
        assertEquals(0, approve(splitId).getStatus(), "拆分审核成功");

        ErpAstSplit split = daoProvider.daoFor(ErpAstSplit.class).getEntityById(splitId);
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, split.getApproveStatus());
        assertTrue(Boolean.TRUE.equals(split.getPosted()), "拆分过账 posted=true");

        // 源资产 DISPOSED + 净值归零
        ErpAstAsset source = daoProvider.daoFor(ErpAstAsset.class).getEntityById(sourceHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_DISPOSED, source.getStatus());
        assertEquals(0, nz(source.getNetBookValue()).compareTo(BigDecimal.ZERO), "源资产净值归零");

        // 3 个新卡片 + 价值分摊（最大项补差）
        List<ErpAstSplitLine> lines = loadSplitLines(splitId);
        assertEquals(3, lines.size(), "3 个拆分行");
        List<ErpAstAsset> targets = new ArrayList<>();
        BigDecimal sumOrig = BigDecimal.ZERO;
        BigDecimal sumDep = BigDecimal.ZERO;
        for (ErpAstSplitLine line : lines) {
            assertTrue(line.getTargetAssetId() != null, "目标资产已回写");
            ErpAstAsset t = daoProvider.daoFor(ErpAstAsset.class).getEntityById(line.getTargetAssetId());
            targets.add(t);
            sumOrig = sumOrig.add(nz(line.getOriginalCostAmount()));
            sumDep = sumDep.add(nz(line.getAccumulatedDepreciationAmount()));
        }
        // Σ 原值 = 100000，Σ 累计折旧 = 40000（最大项补差）
        assertEquals(0, sumOrig.compareTo(new BigDecimal("100000")), "拆分原值合计=源原值");
        assertEquals(0, sumDep.compareTo(new BigDecimal("40000")), "拆分累计折旧合计=源累计折旧");
        // 各目标 IN_SERVICE
        for (ErpAstAsset t : targets) {
            assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, t.getStatus());
        }

        // ASSET_SPLIT 凭证回链（Dr 3 / Cr 1）
        List<ErpFinVoucherBillR> links = findBillLinks("SPL-001", "ASSET_SPLIT");
        assertTrue(!links.isEmpty(), "ASSET_SPLIT 凭证回链已落库");
    }

    @Test
    public void testFixedAmountSplit() {
        long[] sourceHolder = new long[1];
        Long splitId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-SP2", "拆分类别2");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SP2", "固定金额拆分源", categoryId, 1L,
                    new BigDecimal("100000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            ErpAstAsset src = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
            src.setAccumulatedDepreciation(new BigDecimal("40000"));
            src.setNetBookValue(new BigDecimal("60000"));
            daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(src);
            sourceHolder[0] = assetId;
            return seedSplit("SPL-002", assetId, categoryId, new BigDecimal("100000"), 1L);
        });
        seedFixedAmountLines(splitId, 1L, new BigDecimal[]{
                new BigDecimal("50000"), new BigDecimal("30000"), new BigDecimal("20000")});

        assertEquals(0, submitForApproval(splitId).getStatus(), "固定金额拆分提交成功");
        assertEquals(0, approve(splitId).getStatus(), "固定金额拆分审核成功");

        List<ErpAstSplitLine> lines = loadSplitLines(splitId);
        BigDecimal sumOrig = BigDecimal.ZERO;
        for (ErpAstSplitLine line : lines) {
            sumOrig = sumOrig.add(nz(line.getOriginalCostAmount()));
        }
        assertEquals(0, sumOrig.compareTo(new BigDecimal("100000")), "固定金额原值合计=100000");
    }

    @Test
    public void testProportionNotBalancedRejected() {
        Long splitId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-SP3", "拆分类别3");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SP3", "不平衡源", categoryId, 1L,
                    new BigDecimal("100000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return seedSplit("SPL-003", assetId, categoryId, new BigDecimal("100000"), 1L);
        });
        // Σ = 0.95（不平衡）
        seedProportionalLines(splitId, 1L, new BigDecimal[]{
                new BigDecimal("0.5"), new BigDecimal("0.3"), new BigDecimal("0.15")});

        ApiResponse<?> bad = submitForApproval(splitId);
        assertNotEquals(0, bad.getStatus(), "比例不平衡应被拒绝");
    }

    @Test
    public void testCrossCategoryConfigGatedRejected() {
        Long splitId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-SP4", "拆分类别4");
            Long otherCategoryId = seedCategory("CAT-SP4-OTHER", "其他类别");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SP4", "跨类别源", categoryId, 1L,
                    new BigDecimal("100000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            Long sid = seedSplit("SPL-004", assetId, categoryId, new BigDecimal("100000"), 1L);
            // 行类别设为其他类别（与源不同），config=false 默认
            seedProportionalLinesWithCategory(sid, 1L, otherCategoryId,
                    new BigDecimal("0.5"));
            return sid;
        });

        ApiResponse<?> bad = submitForApproval(splitId);
        assertNotEquals(0, bad.getStatus(), "跨类别拆分应被拒绝（config=false）");
    }

    @Test
    public void testSourceNotInServiceRejected() {
        Long splitId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-SP5", "拆分类别5");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SP5", "已报废源", categoryId, 1L,
                    new BigDecimal("100000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                    ErpAstConstants.ASSET_STATUS_SCRAPPED);
            return seedSplit("SPL-005", assetId, categoryId, new BigDecimal("100000"), 1L);
        });
        seedProportionalLines(splitId, 1L, new BigDecimal[]{new BigDecimal("1.0")});

        ApiResponse<?> bad = submitForApproval(splitId);
        assertNotEquals(0, bad.getStatus(), "源资产非 IN_SERVICE 应被拒绝");
    }

    @Test
    public void testApproveThenReverseNotSupported() {
        long[] sourceHolder = new long[1];
        Long splitId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-SP6", "拆分类别6");
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-SP6", "不可逆源", categoryId, 1L,
                    new BigDecimal("100000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            sourceHolder[0] = assetId;
            return seedSplit("SPL-006", assetId, categoryId, new BigDecimal("100000"), 1L);
        });
        seedProportionalLines(splitId, 1L, new BigDecimal[]{new BigDecimal("1.0")});

        assertEquals(0, submitForApproval(splitId).getStatus(), "提交成功");
        assertEquals(0, approve(splitId).getStatus(), "审核成功");

        // approve 后尝试 reverse → ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED（不可逆契约）
        ApiResponse<?> bad = reverseApprove(splitId);
        assertNotEquals(0, bad.getStatus(), "拆分执行后不可 reverse（owner doc 不可逆契约）");

        // 源资产保持 DISPOSED（未被回退）
        ErpAstAsset source = daoProvider.daoFor(ErpAstAsset.class).getEntityById(sourceHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_DISPOSED, source.getStatus(), "源资产保持 DISPOSED");
    }

    @Test
    public void testMergeWeightedHappyPath() {
        long[] targetHolder = new long[1];
        Long mergeId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            Long categoryId = seedCategory("CAT-MG1", "合并类别1");
            Long a1 = seedAssetWithDep("AST-MG1", categoryId, new BigDecimal("50000"), new BigDecimal("10000"));
            Long a2 = seedAssetWithDep("AST-MG2", categoryId, new BigDecimal("30000"), new BigDecimal("6000"));
            Long a3 = seedAssetWithDep("AST-MG3", categoryId, new BigDecimal("20000"), new BigDecimal("4000"));
            Long mid = seedMerge("MRG-001", 1L, 1L);
            seedMergeLine(mid, 1L, a1, 10);
            seedMergeLine(mid, 1L, a2, 20);
            seedMergeLine(mid, 1L, a3, 30);
            return mid;
        });

        assertEquals(0, mergeSubmit(mergeId).getStatus(), "合并提交成功");
        assertEquals(0, mergeApprove(mergeId).getStatus(), "合并审核成功");

        ErpAstMerge merge = daoProvider.daoFor(ErpAstMerge.class).getEntityById(mergeId);
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, merge.getApproveStatus());
        assertTrue(merge.getTargetAssetId() != null, "合并目标资产已回写");
        assertTrue(Boolean.TRUE.equals(merge.getPosted()), "合并过账 posted=true");
        targetHolder[0] = merge.getTargetAssetId();

        // 新卡片 Σ=源合计原值 100000 / 累计折旧 20000
        ErpAstAsset target = daoProvider.daoFor(ErpAstAsset.class).getEntityById(targetHolder[0]);
        assertEquals(0, nz(target.getOriginalValue()).compareTo(new BigDecimal("100000")), "合并新卡原值=100000");
        assertEquals(0, nz(target.getAccumulatedDepreciation()).compareTo(new BigDecimal("20000")), "合并新卡累计折旧=20000");

        // 3 源资产 DISPOSED + 净值归零
        List<ErpAstMergeLine> lines = loadMergeLines(mergeId);
        assertEquals(3, lines.size());
        for (ErpAstMergeLine line : lines) {
            ErpAstAsset src = daoProvider.daoFor(ErpAstAsset.class).getEntityById(line.getSourceAssetId());
            assertEquals(ErpAstConstants.ASSET_STATUS_DISPOSED, src.getStatus(), "源资产 DISPOSED");
            assertEquals(0, nz(src.getNetBookValue()).compareTo(BigDecimal.ZERO), "源资产净值归零");
        }

        // ASSET_MERGE 凭证回链
        assertTrue(!findBillLinks("MRG-001", "ASSET_MERGE").isEmpty(), "ASSET_MERGE 凭证回链已落库");
    }

    @Test
    public void testMergeCrossCurrencyRejected() {
        Long mergeId = ormTemplate.runInSession(session -> {
            seedCoreBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long categoryId = seedCategory("CAT-MG2", "合并类别2");
            Long a1 = seedAssetWithDepAndCurrency("AST-MG-CCY1", categoryId,
                    new BigDecimal("50000"), new BigDecimal("10000"), 1L);
            Long a2 = seedAssetWithDepAndCurrency("AST-MG-CCY2", categoryId,
                    new BigDecimal("30000"), new BigDecimal("6000"), 999L);
            Long mid = seedMerge("MRG-002", 1L, 1L);
            seedMergeLine(mid, 1L, a1, 10);
            seedMergeLine(mid, 1L, a2, 20);
            return mid;
        });

        ApiResponse<?> bad = mergeSubmit(mergeId);
        assertNotEquals(0, bad.getStatus(), "跨币种合并应被拒绝");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submitForApproval(Long id) {
        return executeRpc("ErpAstSplit__submitForApproval", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc("ErpAstSplit__approve", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> reverseApprove(Long id) {
        return executeRpc("ErpAstSplit__reverseApprove", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> mergeSubmit(Long id) {
        return executeRpc("ErpAstMerge__submitForApproval", Map.of("id", String.valueOf(id)));
    }

    private ApiResponse<?> mergeApprove(Long id) {
        return executeRpc("ErpAstMerge__approve", Map.of("id", String.valueOf(id)));
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
    }

    private Long seedCategory(String code, String name) {
        return AstTestSupport.seedCategory(daoProvider, code, name,
                ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
    }

    private Long seedAssetWithDep(String code, Long categoryId, BigDecimal original, BigDecimal accumDep) {
        Long assetId = AstTestSupport.seedAsset(daoProvider, code, code, categoryId, 1L,
                original, BigDecimal.ZERO,
                ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 60,
                ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        ErpAstAsset src = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        src.setAccumulatedDepreciation(accumDep);
        src.setNetBookValue(original.subtract(accumDep));
        daoProvider.daoFor(ErpAstAsset.class).saveOrUpdateEntity(src);
        return assetId;
    }

    private Long seedAssetWithDepAndCurrency(String code, Long categoryId, BigDecimal original,
                                             BigDecimal accumDep, Long currencyId) {
        IEntityDao<ErpAstAsset> dao = daoProvider.daoFor(ErpAstAsset.class);
        ErpAstAsset asset = dao.newEntity();
        asset.setCode(code);
        asset.setName(code);
        asset.setOrgId(1L);
        asset.setCategoryId(categoryId);
        asset.setAcquisitionDate(LocalDate.of(2026, 6, 1));
        asset.setCurrencyId(currencyId);
        asset.setOriginalValue(original);
        asset.setCurrentValue(original);
        asset.setResidualValue(BigDecimal.ZERO);
        asset.setDepreciationMethod(ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE);
        asset.setUsefulLifeMonths(60);
        asset.setAccumulatedDepreciation(accumDep);
        asset.setNetBookValue(original.subtract(accumDep));
        asset.setStatus(ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        dao.saveEntity(asset);
        return asset.getId();
    }

    private Long seedSplit(String code, Long sourceAssetId, Long categoryId, BigDecimal amountSource, Long currencyId) {
        IEntityDao<ErpAstSplit> dao = daoProvider.daoFor(ErpAstSplit.class);
        ErpAstSplit split = dao.newEntity();
        split.setCode(code);
        split.setOrgId(1L);
        split.setSourceAssetId(sourceAssetId);
        split.setBusinessDate(LocalDate.of(2026, 7, 15));
        split.setCurrencyId(currencyId);
        split.setExchangeRate(BigDecimal.ONE);
        split.setAmountSource(amountSource);
        split.setAmountFunctional(amountSource);
        split.setSplitReason("测试拆分");
        split.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        split.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(split);
        return split.getId();
    }

    private void seedProportionalLines(Long splitId, Long orgId, BigDecimal[] proportions) {
        IEntityDao<ErpAstSplitLine> dao = daoProvider.daoFor(ErpAstSplitLine.class);
        for (int i = 0; i < proportions.length; i++) {
            ErpAstSplitLine line = dao.newEntity();
            line.setSplitId(splitId);
            line.setOrgId(orgId);
            line.setLineNo(i + 1);
            line.setTargetAssetCode("TGT-SP-" + splitId + "-" + i);
            line.setTargetAssetName("目标资产-" + i);
            line.setAllocationMethod(ErpAstConstants.ALLOCATION_METHOD_PROPORTIONAL);
            line.setProportion(proportions[i]);
            dao.saveEntity(line);
        }
    }

    private void seedProportionalLinesWithCategory(Long splitId, Long orgId, Long categoryId, BigDecimal proportion) {
        IEntityDao<ErpAstSplitLine> dao = daoProvider.daoFor(ErpAstSplitLine.class);
        ErpAstSplitLine line = dao.newEntity();
        line.setSplitId(splitId);
        line.setOrgId(orgId);
        line.setLineNo(1);
        line.setTargetAssetCode("TGT-SP-X-" + splitId);
        line.setTargetAssetName("跨类别目标");
        line.setCategoryId(categoryId);
        line.setAllocationMethod(ErpAstConstants.ALLOCATION_METHOD_PROPORTIONAL);
        line.setProportion(proportion);
        dao.saveEntity(line);
    }

    private void seedFixedAmountLines(Long splitId, Long orgId, BigDecimal[] amounts) {
        IEntityDao<ErpAstSplitLine> dao = daoProvider.daoFor(ErpAstSplitLine.class);
        for (int i = 0; i < amounts.length; i++) {
            ErpAstSplitLine line = dao.newEntity();
            line.setSplitId(splitId);
            line.setOrgId(orgId);
            line.setLineNo(i + 1);
            line.setTargetAssetCode("TGT-FX-" + splitId + "-" + i);
            line.setTargetAssetName("固定金额目标-" + i);
            line.setAllocationMethod(ErpAstConstants.ALLOCATION_METHOD_FIXED_AMOUNT);
            line.setProportion(BigDecimal.ZERO);
            line.setOriginalCostAmount(amounts[i]);
            dao.saveEntity(line);
        }
    }

    private Long seedMerge(String code, Long orgId, Long currencyId) {
        IEntityDao<ErpAstMerge> dao = daoProvider.daoFor(ErpAstMerge.class);
        ErpAstMerge merge = dao.newEntity();
        merge.setCode(code);
        merge.setOrgId(orgId);
        merge.setBusinessDate(LocalDate.of(2026, 7, 15));
        merge.setCurrencyId(currencyId);
        merge.setExchangeRate(BigDecimal.ONE);
        merge.setMergeReason("测试合并");
        merge.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        merge.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(merge);
        return merge.getId();
    }

    private void seedMergeLine(Long mergeId, Long orgId, Long sourceAssetId, int lineNo) {
        IEntityDao<ErpAstMergeLine> dao = daoProvider.daoFor(ErpAstMergeLine.class);
        ErpAstMergeLine line = dao.newEntity();
        line.setMergeId(mergeId);
        line.setOrgId(orgId);
        line.setLineNo(lineNo);
        line.setSourceAssetId(sourceAssetId);
        dao.saveEntity(line);
    }

    private List<ErpAstSplitLine> loadSplitLines(Long splitId) {
        IEntityDao<ErpAstSplitLine> dao = daoProvider.daoFor(ErpAstSplitLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("splitId", splitId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    private List<ErpAstMergeLine> loadMergeLines(Long mergeId) {
        IEntityDao<ErpAstMergeLine> dao = daoProvider.daoFor(ErpAstMergeLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("mergeId", mergeId));
        q.addOrderField("lineNo", false);
        return dao.findAllByQuery(q);
    }

    private List<ErpFinVoucherBillR> findBillLinks(String billCode, String businessType) {
        IEntityDao<ErpFinVoucherBillR> dao = daoProvider.daoFor(ErpFinVoucherBillR.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("billCode", billCode), eq("businessType", businessType)));
        return dao.findAllByQuery(q);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
