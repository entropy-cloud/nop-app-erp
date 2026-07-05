package app.erp.ast.service;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDisposal;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import io.nop.wf.core.IWorkflow;
import io.nop.wf.core.IWorkflowManager;
import io.nop.wf.core.IWorkflowStep;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 资产处置 WORKFLOW 多级审批端到端测试（approval-framework.md WORKFLOW）。
 * 验证 submitForApproval 启动 wf → manager-approval agree → wf 结束回调 Processor.approve → APPROVED+过账+资产终态，
 * disagree → wf 结束回调 Processor.reject → REJECTED，REJECTED 后可重提。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstDisposalWorkflowApproval extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IWorkflowManager workflowManager;

    @Test
    public void testSubmitAgreeThenApproved() {
        setUser();
        long[] assetIdHolder = new long[1];
        Long disposalId = ormTemplate.runInSession(session -> {
            seedBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-WF-S", "报废类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-WF-SCRAP", "报废资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;
            return seedDisposal("DISP-WF-001", assetId, ErpAstConstants.DISPOSAL_TYPE_SCRAPPED,
                    BigDecimal.ZERO, LocalDate.of(2026, 7, 15));
        });

        assertEquals(0, submit(disposalId).getStatus(), "submitForApproval 应成功");
        ErpAstDisposal reloaded = reload(disposalId);
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, reloaded.getApproveStatus());
        assertNotNull(reloaded.getNopFlowId(), "WORKFLOW 模式应启动 wf 实例并回写 nopFlowId");

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(disposalId).getNopFlowId());
            invokeStep(wf, "manager-approval", "agree", ctx);
        });

        reloaded = reload(disposalId);
        assertEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus(),
                "manager-approval agree 后 wf 结束回调 approve → APPROVED");
        assertTrue(Boolean.TRUE.equals(reloaded.getPosted()), "处置过账 posted=true");

        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetIdHolder[0]);
        assertEquals(ErpAstConstants.ASSET_STATUS_SCRAPPED, asset.getStatus(), "资产终态=SCRAPPED");
    }

    @Test
    public void testDisagreeThenRejected() {
        setUser();
        Long disposalId = ormTemplate.runInSession(session -> {
            seedBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-WF-S2", "报废类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-WF-SCRAP2", "报废资产2", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return seedDisposal("DISP-WF-002", assetId, ErpAstConstants.DISPOSAL_TYPE_SCRAPPED,
                    BigDecimal.ZERO, LocalDate.of(2026, 7, 15));
        });
        assertEquals(0, submit(disposalId).getStatus());

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(disposalId).getNopFlowId());
            invokeStep(wf, "manager-approval", "disagree", ctx);
        });

        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, reload(disposalId).getApproveStatus(),
                "manager-approval disagree → wf 结束回调 reject → REJECTED");
    }

    @Test
    public void testResubmitAfterRejected() {
        setUser();
        Long disposalId = ormTemplate.runInSession(session -> {
            seedBasics();
            AstTestSupport.seedPeriod(daoProvider, "2026-07", 2026, 7, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-WF-S3", "报废类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-WF-SCRAP3", "报废资产3", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            return seedDisposal("DISP-WF-003", assetId, ErpAstConstants.DISPOSAL_TYPE_SCRAPPED,
                    BigDecimal.ZERO, LocalDate.of(2026, 7, 15));
        });
        submit(disposalId);
        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(disposalId).getNopFlowId());
            invokeStep(wf, "manager-approval", "disagree", ctx);
        });
        assertEquals(ErpAstConstants.APPROVE_STATUS_REJECTED, reload(disposalId).getApproveStatus());

        assertEquals(0, submit(disposalId).getStatus(), "REJECTED 后 submitForApproval 应可重提");
        ErpAstDisposal reloaded = reload(disposalId);
        assertEquals(ErpAstConstants.APPROVE_STATUS_SUBMITTED, reloaded.getApproveStatus());
        assertNotNull(reloaded.getNopFlowId(), "重提应启动新的 wf 实例");
    }

    // ---------- helpers ----------

    private IServiceContext newContext() {
        IServiceContext ctx = new ServiceContextImpl();
        ctx.getContext().setUserId("0");
        ctx.getContext().setUserName("SYS");
        return ctx;
    }

    private void setUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    private void invokeStep(IWorkflow wf, String stepName, String action, IServiceContext ctx) {
        List<? extends IWorkflowStep> steps = wf.getActivatedSteps();
        for (IWorkflowStep step : steps) {
            if (step.getStepName().equals(stepName)) {
                step.invokeAction(action, null, ctx);
                wf.runAutoTransitions(ctx);
                return;
            }
        }
        throw new IllegalStateException("步骤未激活: " + stepName + "，当前激活=" + steps);
    }

    private ApiResponse<?> submit(Long id) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(mutation, "ErpAstDisposal__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpAstDisposal reload(Long id) {
        return daoProvider.daoFor(ErpAstDisposal.class).getEntityById(id);
    }

    private void seedBasics() {
        AstTestSupport.seedAcctSchema(daoProvider, 1L);
        AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
    }

    private Long seedDisposal(String code, Long assetId, String disposalType, BigDecimal disposalAmount,
                              LocalDate businessDate) {
        IEntityDao<ErpAstDisposal> dao = daoProvider.daoFor(ErpAstDisposal.class);
        ErpAstDisposal disposal = new ErpAstDisposal();
        disposal.setCode(code);
        disposal.setOrgId(1L);
        disposal.setAssetId(assetId);
        disposal.setDisposalType(disposalType);
        disposal.setDisposalAmount(disposalAmount);
        disposal.setCurrencyId(1L);
        disposal.setExchangeRate(BigDecimal.ONE);
        disposal.setBusinessDate(businessDate);
        disposal.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
        disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
        dao.saveEntity(disposal);
        return disposal.getId();
    }
}
