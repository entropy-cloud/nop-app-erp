package app.erp.pur.service;

import app.erp.md.dao.entity.ErpMdSupplierApproval;
import app.erp.pur.dao.entity.ErpPurQuotation;
import app.erp.pur.dao.entity.ErpPurSupplierScorecard;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardCriteria;
import app.erp.pur.dao.entity.ErpPurSupplierScorecardVariable;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
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
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 供应商评分卡 standing → AVL/RFQ 联动端到端测试（{@code docs/plans/2026-07-03-1707-2} Phase 3）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>评分 finalize standing=RED → AVL SUSPENDED 跨域联动（purchase→master-data I*Biz，单事务）</li>
 *   <li>报价单创建对 SUSPENDED/REJECTED 供应商 prevent（抛 ERR_SUPPLIER_NOT_APPROVED）</li>
 *   <li>standing=YELLOW → warn 不阻止（报价单正常保存）</li>
 *   <li>standing=GREEN / 无评分记录 → 正常</li>
 *   <li>config prevent-on-red=false 时 standing=RED → 报价不 prevent（走 hold 审批）</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurScorecardLinkage extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testScorecardRedSuspendsAvl() {
        Long partnerId = 9001L;
        seedApprovedApproval(partnerId);
        Long scorecardId = seedDraftScorecard(partnerId, 50); // 50 < 60 → RED

        ApiResponse<?> resp = finalizeScorecard(scorecardId);
        assertEquals(0, resp.getStatus(), "finalize 应成功");

        ErpMdSupplierApproval approval = findApprovalByPartner(partnerId);
        assertNotNull(approval, "应存在 AVL 记录");
        assertEquals(ErpPurConstants.APPROVAL_STATUS_SUSPENDED, approval.getStatus(), "standing=RED → AVL status=SUSPENDED(40) 联动生效");
    }

    @Test
    public void testSuspendedSupplierCannotQuote() {
        Long partnerId = 9002L;
        seedPartner(partnerId);
        seedApproval(partnerId, ErpPurConstants.APPROVAL_STATUS_SUSPENDED); // SUSPENDED

        ApiResponse<?> resp = saveQuotation(partnerId);
        assertEquals(ErpPurErrors.ERR_SUPPLIER_NOT_APPROVED.getErrorCode(), resp.getCode(),
                "SUSPENDED 供应商不可创建报价 → prevent");
    }

    @Test
    public void testRejectedSupplierCannotQuote() {
        Long partnerId = 9003L;
        seedPartner(partnerId);
        seedApproval(partnerId, ErpPurConstants.APPROVAL_STATUS_REJECTED); // REJECTED

        ApiResponse<?> resp = saveQuotation(partnerId);
        assertEquals(ErpPurErrors.ERR_SUPPLIER_NOT_APPROVED.getErrorCode(), resp.getCode(),
                "REJECTED 供应商不可创建报价 → prevent");
    }

    @Test
    public void testNoApprovalPreventsQuote() {
        Long partnerId = 9004L;
        seedPartner(partnerId); // 供应商主数据存在，但无 AVL 准入资格

        ApiResponse<?> resp = saveQuotation(partnerId);
        assertEquals(ErpPurErrors.ERR_SUPPLIER_NOT_APPROVED.getErrorCode(), resp.getCode(),
                "无准入资格的供应商不可创建报价 → prevent");
    }

    @Test
    public void testYellowStandingWarnsButAllowsQuote() {
        Long partnerId = 9005L;
        seedApprovedApproval(partnerId);
        Long scId = seedDraftScorecard(partnerId, 70); // 70 ∈ [60,80) → YELLOW
        assertEquals(0, finalizeScorecard(scId).getStatus());

        // YELLOW → warn 不阻止，报价单正常保存
        ApiResponse<?> resp = saveQuotation(partnerId);
        assertEquals(0, resp.getStatus(), "YELLOW standing 仅 warn，报价单应正常保存");
    }

    @Test
    public void testGreenStandingAllowsQuote() {
        Long partnerId = 9006L;
        seedApprovedApproval(partnerId);
        Long scId = seedDraftScorecard(partnerId, 95); // 95 ≥ 80 → GREEN
        assertEquals(0, finalizeScorecard(scId).getStatus());

        ApiResponse<?> resp = saveQuotation(partnerId);
        assertEquals(0, resp.getStatus(), "GREEN standing 正常询价");
    }

    @Test
    public void testRedStandingPreventOnRedConfig() {
        Long partnerId = 9007L;
        seedApprovedApproval(partnerId);
        Long scId = seedDraftScorecard(partnerId, 50); // RED
        assertEquals(0, finalizeScorecard(scId).getStatus());

        // 默认 prevent-on-red=true → RED 不可报价（AVL 已被联动 SUSPENDED，双重 prevent）
        ApiResponse<?> resp = saveQuotation(partnerId);
        assertEquals(ErpPurErrors.ERR_SUPPLIER_NOT_APPROVED.getErrorCode(), resp.getCode(),
                "prevent-on-red=true 时 RED → prevent");
    }

    @Test
    public void testRedStandingHoldWhenPreventOnRedFalse() {
        // 独立 partner + 独立 AVL，避免 AVL SUSPENDED 联动干扰 prevent-on-red 断言
        Long partnerId = 9008L;
        // finalize 前临时关闭 prevent-on-red，使 RED 不触发 prevent；
        // 但 finalize 仍会联动 AVL SUSPENDED → 报价时 AVL 已 SUSPENDED 仍 prevent。
        // 为干净验证 prevent-on-red=false 单独语义，此处不 finalize（不触发 AVL 联动），
        // 仅手工置 RED standing + APPROVED AVL，断言 config=false 时 checker 放行。
        seedApprovedApproval(partnerId);
        seedFinalizedScorecard(partnerId, 50); // 直接写入 FINALIZED + RED，不经 finalize（不触发 AVL 联动）

        AppConfig.getConfigProvider().assignConfigValue(
                ErpPurConstants.CONFIG_SCORECARD_PREVENT_ON_RED, false);
        try {
            ApiResponse<?> resp = saveQuotation(partnerId);
            assertEquals(0, resp.getStatus(),
                    "prevent-on-red=false 时 RED（AVL 仍 APPROVED）→ hold 放行不 prevent");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpPurConstants.CONFIG_SCORECARD_PREVENT_ON_RED, true);
        }
    }

    // ---------- helpers ----------

    private ApiResponse<?> finalizeScorecard(Long id) {
        return executeRpc(mutation, "ErpPurSupplierScorecard__finalizeScorecard",
                ApiRequest.build(Map.of("scorecardId", id)));
    }

    private ApiResponse<?> saveQuotation(Long supplierId) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("code", "QT-LINK-" + supplierId);
        data.put("supplierId", supplierId);
        data.put("currencyId", 6101L);
        data.put("businessDate", "2026-07-03");
        data.put("docStatus", ErpPurConstants.DOC_STATUS_DRAFT);
        data.put("approveStatus", ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        return executeRpc(mutation, "ErpPurQuotation__save", ApiRequest.build(Map.of("data", data)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedApprovedApproval(Long partnerId) {
        seedPartner(partnerId);
        seedApproval(partnerId, "APPROVED"); // APPROVED
    }

    private void seedApproval(Long partnerId, String status) {
        ErpMdSupplierApproval approval = new ErpMdSupplierApproval();
        approval.setPartnerId(partnerId);
        approval.setApprovalType("NEW");
        approval.setMaterialCategoryId(7101L);
        approval.setValidFrom(LocalDate.of(2026, 1, 1));
        approval.setValidTo(LocalDate.of(2027, 1, 1));
        approval.setQualificationDoc("ISO9001");
        approval.setStatus(status);
        ormTemplate.runInSession(() -> approvalDao().saveEntity(approval));
    }

    private void seedPartner(Long partnerId) {
        ormTemplate.runInSession(() -> {
            app.erp.md.dao.entity.ErpMdPartner partner = new app.erp.md.dao.entity.ErpMdPartner();
            partner.setId(partnerId);
            partner.setCode("SUP-" + partnerId);
            partner.setName("供应商" + partnerId);
            partner.setPartnerType("SUPPLIER");
            partner.setStatus(ErpPurConstants.PARTNER_STATUS_ACTIVE);
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdPartner.class).saveEntity(partner);

            // 报价单 FK 校验需要 currency 存在（仅首次创建）
            IEntityDao<app.erp.md.dao.entity.ErpMdCurrency> curDao =
                    daoProvider.daoFor(app.erp.md.dao.entity.ErpMdCurrency.class);
            if (curDao.getEntityById(6101L) == null) {
                app.erp.md.dao.entity.ErpMdCurrency cur = new app.erp.md.dao.entity.ErpMdCurrency();
                cur.setId(6101L);
                cur.setCode("CNY");
                cur.setName("人民币");
                curDao.saveEntity(cur);
            }
        });
    }

    private ErpMdSupplierApproval findApprovalByPartner(Long partnerId) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("partnerId", partnerId));
        q.setLimit(1);
        var list = approvalDao().findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedDraftScorecard(Long partnerId, int passRate) {
        ErpPurSupplierScorecard sc = new ErpPurSupplierScorecard();
        sc.setPartnerId(partnerId);
        sc.setPeriodFrom(LocalDate.of(2026, 1, 1));
        sc.setPeriodTo(LocalDate.of(2026, 3, 31));
        sc.setWarnThreshold(new BigDecimal("80"));
        sc.setHoldThreshold(new BigDecimal("60"));
        sc.setPreventThreshold(new BigDecimal("60"));
        sc.setStatus(ErpPurConstants.SCORECARD_STATUS_DRAFT);

        ErpPurSupplierScorecardCriteria c = new ErpPurSupplierScorecardCriteria();
        c.setCriteriaName("质量");
        c.setWeight(new BigDecimal("100"));
        c.setFormula("pass_rate");

        ErpPurSupplierScorecardVariable v = new ErpPurSupplierScorecardVariable();
        v.setVariableName("pass_rate");
        v.setPath("pass_rate");
        v.setValue(new BigDecimal(passRate));

        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            c.setScorecardId(sc.getId());
            criteriaDao().saveEntity(c);
            v.setCriteriaId(c.getId());
            variableDao().saveEntity(v);
        });
        return sc.getId();
    }

    private void seedFinalizedScorecard(Long partnerId, int passRate) {
        // 直接构建 FINALIZED+RED 评分卡，不经 finalize（避免触发 AVL SUSPENDED 联动干扰 config 断言）
        ErpPurSupplierScorecard sc = new ErpPurSupplierScorecard();
        sc.setPartnerId(partnerId);
        sc.setPeriodFrom(LocalDate.of(2026, 1, 1));
        sc.setPeriodTo(LocalDate.of(2026, 3, 31));
        sc.setWarnThreshold(new BigDecimal("80"));
        sc.setHoldThreshold(new BigDecimal("60"));
        sc.setPreventThreshold(new BigDecimal("60"));
        sc.setTotalScore(new BigDecimal(passRate));
        sc.setStanding(ErpPurConstants.STANDING_RED);
        sc.setStatus(ErpPurConstants.SCORECARD_STATUS_FINALIZED);

        ErpPurSupplierScorecardCriteria c = new ErpPurSupplierScorecardCriteria();
        c.setCriteriaName("质量");
        c.setWeight(new BigDecimal("100"));
        c.setFormula("pass_rate");
        c.setScore(new BigDecimal(passRate));
        c.setWeightedScore(new BigDecimal(passRate));

        ErpPurSupplierScorecardVariable v = new ErpPurSupplierScorecardVariable();
        v.setVariableName("pass_rate");
        v.setPath("pass_rate");
        v.setValue(new BigDecimal(passRate));

        ormTemplate.runInSession(() -> {
            scorecardDao().saveEntity(sc);
            c.setScorecardId(sc.getId());
            criteriaDao().saveEntity(c);
            v.setCriteriaId(c.getId());
            variableDao().saveEntity(v);
        });
    }

    private IEntityDao<ErpMdSupplierApproval> approvalDao() {
        return daoProvider.daoFor(ErpMdSupplierApproval.class);
    }

    private IEntityDao<ErpPurSupplierScorecard> scorecardDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecard.class);
    }

    private IEntityDao<ErpPurSupplierScorecardCriteria> criteriaDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecardCriteria.class);
    }

    private IEntityDao<ErpPurSupplierScorecardVariable> variableDao() {
        return daoProvider.daoFor(ErpPurSupplierScorecardVariable.class);
    }
}
