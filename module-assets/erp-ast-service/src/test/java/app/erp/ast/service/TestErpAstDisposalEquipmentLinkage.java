package app.erp.ast.service;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstDisposal;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * 资产处置→设备 DECOMMISSIONED 联动接线测试（RC-R1.77 / P1-RC-070 / UC-MAIN-08-A，
 * plan 2026-08-19-0445-3 Phase 1 Proof 接线侧；mnt Facade 真实实现见 mnt-service
 * {@code TestErpMntAssetDisposalLinkage}）。
 *
 * <p>mock 双层范式（对齐 R1.68 cs mock 先例）：ast 测试容器无 mnt bean，经
 * {@code test-mock-mnt.beans.xml} 注册 {@code TestMockMntBizModels.MockErpMntEquipmentBiz}。
 * 覆盖：
 * <ul>
 *   <li>① 处置 approve 后置调用 {@code changeStatusForAssetDisposal(assetId, disposalCode)}</li>
 *   <li>② reverseApprove posted 分支对称调用 {@code restoreFromAssetDisposal(assetId, disposalCode)}</li>
 *   <li>③ 联动失败异常传播回滚处置（approve 失败，处置单与资产状态均回滚）</li>
 *   <li>④ mnt bean 缺失容错（@Nullable null 跳过）由其余 ast 处置测试在无 mock 容器中零回归证明</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/ast/beans/test-mock-mnt.beans.xml")
public class TestErpAstDisposalEquipmentLinkage extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    /** 测试容器唯一 IErpMntEquipmentBiz bean 即 mock（test-mock-mnt.beans.xml 注册，ioc:type 按接口注册）。 */
    @Inject
    app.erp.mnt.biz.IErpMntEquipmentBiz mntEquipmentBiz;

    private TestMockMntBizModels.MockErpMntEquipmentBiz mockMnt() {
        return (TestMockMntBizModels.MockErpMntEquipmentBiz) mntEquipmentBiz;
    }

    static final String DISPOSAL_CODE = "DISP-MNT-LINK-001";

    @BeforeEach
    public void resetMock() {
        mockMnt().failLink = false;
        mockMnt().lastDecommissionAssetId = null;
        mockMnt().lastDecommissionCode = null;
        mockMnt().lastRestoreAssetId = null;
        mockMnt().lastRestoreCode = null;
    }

    @Test
    public void testApproveCallsDecommissionFacade() {
        setUser();
        Long[] holder = new Long[1];
        Long disposalId = seedSubmittedDisposal(holder);
        Long assetId = holder[0];

        assertEquals(0, approve(disposalId).getStatus(), "处置 approve 应成功");

        assertNotNull(mockMnt().lastDecommissionAssetId, "应调用 mnt 处置联动 Facade");
        assertEquals(assetId, mockMnt().lastDecommissionAssetId, "联动 assetId=处置资产");
        assertEquals(DISPOSAL_CODE, mockMnt().lastDecommissionCode, "联动 sourceBillCode=处置单编码");
        assertNull(mockMnt().lastRestoreAssetId, "approve 不触发恢复");
    }

    @Test
    public void testReverseApproveCallsRestoreFacade() {
        setUser();
        Long[] holder = new Long[1];
        Long disposalId = seedSubmittedDisposal(holder);
        Long assetId = holder[0];
        assertEquals(0, approve(disposalId).getStatus(), "前置：approve 成功");
        ErpAstDisposal approved = daoProvider.daoFor(ErpAstDisposal.class).getEntityById(disposalId);
        assertEquals(Boolean.TRUE, approved.getPosted(), "前置：过账 posted=true（restore 钉 posted 分支）");

        ApiResponse<?> reverse = executeRpc(mutation, "ErpAstDisposal__reverseApprove",
                ApiRequest.build(Map.of("id", String.valueOf(disposalId))));
        assertEquals(0, reverse.getStatus(), "reverseApprove 应成功: " + reverse);

        assertEquals(assetId, mockMnt().lastRestoreAssetId, "posted 分支应对称调用恢复 Facade");
        assertEquals(DISPOSAL_CODE, mockMnt().lastRestoreCode);
    }

    @Test
    public void testLinkageFailureRollsBackApprove() {
        setUser();
        Long[] holder = new Long[1];
        Long disposalId = seedSubmittedDisposal(holder);
        Long assetId = holder[0];
        mockMnt().failLink = true;
        try {
            ApiResponse<?> resp = approve(disposalId);
            assertNotEquals(0, resp.getStatus(), "联动失败应异常传播使 approve 失败（L1 硬断言强一致）");

            ErpAstDisposal disposal = daoProvider.daoFor(ErpAstDisposal.class).getEntityById(disposalId);
            assertNotEquals(ErpAstConstants.APPROVE_STATUS_APPROVED, disposal.getApproveStatus(),
                    "处置单审批状态回滚（非 APPROVED）");
            ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
            assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, asset.getStatus(),
                    "资产状态回滚（设备停用是 L1 硬断言，处置成功但设备未停用 = 契约破坏）");
        } finally {
            mockMnt().failLink = false;
        }
    }

    // ---------- helpers ----------

    /** 完整处置种子（镜像 TestErpAstDisposal.testScrapLossAndTerminalStatus：SCRAPPED + 过账；提交后待 approve）。 */
    private Long seedSubmittedDisposal(Long[] assetIdHolder) {
        Long disposalId = seedDisposalRow(assetIdHolder);
        assertEquals(0, executeRpc(mutation, "ErpAstDisposal__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(disposalId)))).getStatus(), "提交成功");
        return disposalId;
    }

    private Long seedDisposalRow(Long[] assetIdHolder) {
        return ormTemplate.runInSession(session -> {
            AstTestSupport.seedAcctSchema(daoProvider, 1L);
            AstTestSupport.seedSubject(daoProvider, "1002", "银行存款");
            AstTestSupport.seedSubject(daoProvider, "1606", "固定资产清理");
            AstTestSupport.seedPeriod(daoProvider, "2026-08", 2026, 8, ErpAstConstants.PERIOD_STATUS_OPEN);
            Long gainLossSubjectId = AstTestSupport.seedSubject(daoProvider, "6711", "营业外支出");
            Long categoryId = AstTestSupport.seedCategory(daoProvider, "CAT-MNT-LINK", "联动测试类别",
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    AstTestSupport.seedSubject(daoProvider, "1601", "固定资产"),
                    AstTestSupport.seedSubject(daoProvider, "1602", "累计折旧"),
                    AstTestSupport.seedSubject(daoProvider, "6602", "管理费用"));
            daoProvider.daoFor(app.erp.ast.dao.entity.ErpAstAssetCategory.class).getEntityById(categoryId)
                    .setDisposalGainLossSubjectId(gainLossSubjectId);
            Long assetId = AstTestSupport.seedAsset(daoProvider, "AST-MNT-LINK", "联动资产", categoryId, 1L,
                    new BigDecimal("12000"), BigDecimal.ZERO,
                    ErpAstConstants.DEPRECIATION_METHOD_STRAIGHT_LINE, 12,
                    ErpAstConstants.ASSET_STATUS_IN_SERVICE);
            assetIdHolder[0] = assetId;

            IEntityDao<ErpAstDisposal> dao = daoProvider.daoFor(ErpAstDisposal.class);
            ErpAstDisposal disposal = new ErpAstDisposal();
            disposal.setCode(DISPOSAL_CODE);
            disposal.setOrgId(1L);
            disposal.setAssetId(assetId);
            disposal.setDisposalType(ErpAstConstants.DISPOSAL_TYPE_SCRAPPED);
            disposal.setDisposalAmount(BigDecimal.ZERO);
            disposal.setCurrencyId(1L);
            disposal.setExchangeRate(BigDecimal.ONE);
            disposal.setBusinessDate(LocalDate.of(2026, 8, 15));
            disposal.setDocStatus(ErpAstConstants.DOC_STATUS_DRAFT);
            disposal.setApproveStatus(ErpAstConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(disposal);
            return disposal.getId();
        });
    }

    private ApiResponse<?> approve(Long id) {
        return executeRpc(mutation, "ErpAstDisposal__approve", ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    // WORKFLOW 模式下 submit 会启动 wf 实例，wf 引擎校验 caller 需 resolved 用户（镜像 TestErpAstDisposal）。
    private void setUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                                      ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
