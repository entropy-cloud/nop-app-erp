package io.nop.app.all.it;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.dao.entity.ErpPrjTimesheet;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B6 C12：项目工时过账与结算损益（按 {@code docs/design/integration-testing.md §6 C12} 规格）。
 *
 * <p>全链（自包含项目类型/项目/任务/工时单 + 引用 seed 主数据：员工（md 域 EMP-001 id=1）+ 项目 PRJ-2026-001
 * （id=1，结算对象）+ 组织 2 + 币种 1 + 2026-07 OPEN 期间 + 科目 5101/2211/1601/1603/4103/6001 + 账套 1）：
 * 自包含项目类型（{@code ErpPrjProjectType__save}，defaultSubjectId=5101）→ 自包含项目
 * （{@code ErpPrjProject__save}，OPEN + budget 100000）→ 自包含任务（IN_PROGRESS）→ 工时单 save
 * （引用 seed md 员工 EMP-001，hours=8 × costRate=500 → costAmount=4000）→ {@code ErpPrjTimesheet__submit}
 * （SUBMITTED + costAmount 计算）→ {@code ErpPrjTimesheet__approve}（APPROVED + posted=true +
 * PROJECT_COST_COLLECTION 凭证 Dr 5101/Cr 2211=4000 + 归集行/actualCost 回写）→ 结算 CLOSE
 * （{@code ErpPrjProjectSettlement__createSettlement}(projectId=1, CLOSE) → submit → approve：
 * 转固建卡 + PROJECT_SETTLEMENT 凭证 Dr 1601/Cr 1603=30000）→ 损益结转 = 结算 FINAL
 * （{@code ErpPrjProjectSettlement__createSettlement}(projectId=1, FINAL) → submit → approve：
 * PROJECT_SETTLEMENT 凭证 Dr 5101=30000 / Dr 4103=20000 / Cr 6001=50000）。
 *
 * <p>Phase 2 Decision（三项裁决，落盘设计文档 §6 C12 勘误）：
 * <ol>
 *   <li><b>工时科目 config</b>：`erp-prj.default-payroll-subject-id` 默认空串（实仓 {@code ErpPrjConfigs}，
 *       {@code TimesheetPostingDispatcher} 空科目报 {@code ERR_PAYROLL_SUBJECT_NOT_CONFIGURED}）→ 类级
 *       {@code @NopTestProperty(name="erp-prj.default-payroll-subject-id", value="2211")}（对齐 C06/C08/C10
 *       同型处理）；残留风险：门控默认关闭态下凭证不存在，不作断言源。</li>
 *   <li><b>工时借记科目与员工引用漂移</b>：① 实仓 {@code TimesheetPostingDispatcher} 借方科目 = 项目类型
 *       {@code defaultSubjectId}（缺失抛 {@code ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED}）——seed 项目类型
 *       PRJ-TYPE-IT 无 defaultSubjectId，seed 项目 PRJ-2026-001 上工时过账必然失败 → 工时单落于自包含项目
 *       （项目类型 defaultSubjectId=5101），B4/B5 自包含建数先例，未触发 seed 修正授权；② 设计文档 §6 C12
 *       前置「seed 员工 HR-EMP-001/002」为漂移——工时单 {@code userId} 实仓关系 = {@code ErpMdEmployee}
 *       （md 域 seed EMP-001/002，id 1/2），非 hr 域员工 → 用例引用 md 员工 EMP-001（id=1）。</li>
 *   <li><b>结算/损益结转动作面与 seed PNL 行交互</b>：实仓 {@code ErpPrjProjectSettlementBizModel} 动作 =
 *       {@code createSettlement}/{@code submit}（内部委托 submitForApprovalProcessor，无 GraphQL
 *       {@code submitForApproval}）/{@code approve}/…；CLOSE 结算 approve 转固建卡 + PROJECT_SETTLEMENT 凭证
 *       （Dr 1601/Cr 1603）；损益结转非独立动作 = FINAL 结算 approve 驱动（Dr 5101/Dr 4103/Cr 6001，
 *       {@code ProjectSettlementAcctDocProvider}）——设计文档 §6 C12 步骤 2/3 动作面漂移勘误登记。
 *       <b>seed PNL 行交互裁决</b>：本用例不调用 {@code ErpPrjProjectPnl__refreshPnl} → seed 行
 *       PRJ-PNL-2026-001（CALCULATED，revenue 50000/cost 30000/profit 20000）零改写零消费；
 *       {@code createSettlement} 以该 seed 快照行为快照源（{@code pnlBiz.getProjectPnl}），断言锚点 =
 *       结算单 finalRevenue/finalCost/finalProfit 派生一致 + 凭证金额 + seed 行 reload 原值不变。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（工时审批凭证借贷平衡 + hours × rate、结算 CLOSE 转固建卡 +
 * 凭证、损益结转凭证、seed PNL 行零改写）；层 2 = 每步 response 快照；层 3 = output/tables 变更行
 * （project_type + project + task + timesheet + cost_collection_line + settlement + asset + voucher +
 * voucher_line + voucher_bill_r）。RECORDING→CHECKING 往返已按 M0.2 试点范式完成。
 * 冻结时钟 2026-07-17（{@code C11C12FrozenClockExtension}）保证 workDate/businessDate 确定性；
 * 结算单/资产卡 code 含 currentTimeMillis 后缀（实仓生成逻辑），快照层以 {@code *} 通配处置（B4 先例）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-prj.default-payroll-subject-id", value = "2211")
public class TestErpC12PrjTimesheetSettlement extends ErpIntegrationTestCase {

    static final String PT_CODE = "IT-C12-PT-001";
    static final String PRJ_CODE = "IT-C12-PRJ-001";
    static final String TASK_TITLE = "C12 实施任务";
    static final String TS_CODE = "IT-C12-TS-001";
    static final String SEED_PROJECT_ID = "1";          // seed PRJ-2026-001（结算对象）
    static final String ORG_ID = "2";                   // seed 组织 2
    static final String CURRENCY_ID = "1";              // seed CNY
    static final String SEED_EMPLOYEE_ID = "1";         // seed md 员工 EMP-001（工时 userId 实仓关系 = md 域）
    static final String SUBJECT_PROJECT_COST_ID = "32"; // seed 科目 5101 项目成本
    static final BigDecimal HOURS = new BigDecimal("8");
    static final BigDecimal COST_RATE = new BigDecimal("500");
    static final BigDecimal TS_AMOUNT = new BigDecimal("4000");  // 8 × 500
    static final BigDecimal SETTLE_COST = new BigDecimal("30000");  // seed PNL 快照 totalCost
    static final BigDecimal SETTLE_REVENUE = new BigDecimal("50000"); // seed PNL 快照 revenue
    static final BigDecimal SETTLE_PROFIT = new BigDecimal("20000"); // seed PNL 快照 grossProfit

    @RegisterExtension
    static C11C12FrozenClockExtension frozenClock = new C11C12FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testPrjTimesheetSettlementClosedLoop() {
        // ---------- 1. 自包含项目类型（defaultSubjectId=5101）+ 项目 + 任务 ----------
        ApiResponse<?> ptSave = rpcMutation("ErpPrjProjectType__save",
                request("1_project_type_save.json5", Map.class));
        output("1_project_type_save_response.json5", ptSave);
        assertEquals(0, ptSave.getStatus(), "项目类型保存应成功");
        addVar("ptId", idOf(ptSave));

        ApiResponse<?> prjSave = rpcMutation("ErpPrjProject__save",
                request("2_project_save.json5", Map.class));
        output("2_project_save_response.json5", prjSave);
        assertEquals(0, prjSave.getStatus(), "项目保存应成功");
        String prjId = idOf(prjSave);
        addVar("prjId", prjId);
        addVar("prjCode", PRJ_CODE);

        ApiResponse<?> taskSave = rpcMutation("ErpPrjTask__save",
                request("3_task_save.json5", Map.class));
        output("3_task_save_response.json5", taskSave);
        assertEquals(0, taskSave.getStatus(), "任务保存应成功");
        addVar("taskId", idOf(taskSave));

        // ---------- 2. 自包含工时单（引用 seed md 员工 EMP-001）→ submit → approve ----------
        ApiResponse<?> tsSave = rpcMutation("ErpPrjTimesheet__save",
                request("4_timesheet_save.json5", Map.class));
        output("4_timesheet_save_response.json5", tsSave);
        assertEquals(0, tsSave.getStatus(), "工时单保存应成功");
        String tsId = idOf(tsSave);
        addVar("tsId", tsId);
        addVar("tsCode", TS_CODE);

        ApiResponse<?> tsSubmit = rpcMutation("ErpPrjTimesheet__submit",
                request("5_timesheet_submit.json5", Map.class));
        output("5_timesheet_submit_response.json5", tsSubmit);
        assertEquals(0, tsSubmit.getStatus(), "工时提交应成功");
        ErpPrjTimesheet submitted = reloadTimesheet(tsId);
        assertEquals(ErpPrjConstants.APPROVE_STATUS_SUBMITTED, submitted.getStatus(), "submit 后 SUBMITTED");
        assertEquals(0, TS_AMOUNT.compareTo(submitted.getCostAmount()),
                "costAmount=hours×costRate=8×500=4000");

        ApiResponse<?> tsApprove = rpcMutation("ErpPrjTimesheet__approve",
                request("6_timesheet_approve.json5", Map.class));
        output("6_timesheet_approve_response.json5", tsApprove);
        assertEquals(0, tsApprove.getStatus(), "工时审批应成功");
        ErpPrjTimesheet approved = reloadTimesheet(tsId);
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, approved.getStatus(), "approve 后 APPROVED");
        assertEquals(Boolean.TRUE, approved.getPosted(), "工时过账成功 posted=true");

        // 层 1 锚点：PROJECT_COST_COLLECTION 凭证借贷平衡（Dr 5101 项目成本 / Cr 2211 应付职工薪酬 = 4000）
        ErpFinVoucherBillR tsLink = findBillLink(TS_CODE);
        assertNotNull(tsLink, "工时应生成 PROJECT_COST_COLLECTION 凭证回链");
        assertEquals("PROJECT_COST_COLLECTION", tsLink.getBusinessType(), "工时凭证业务类型");
        ErpFinVoucher tsVoucher = requireVoucherBalanced(tsLink, TS_AMOUNT, "工时凭证");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, tsVoucher.getDocStatus(), "工时凭证已过账");
        List<ErpFinVoucherLine> tsLines = findVoucherLines(tsVoucher.getId());
        ErpFinVoucherLine tsDebit = findLineBySubject(tsLines, "5101");
        ErpFinVoucherLine tsCredit = findLineBySubject(tsLines, "2211");
        assertNotNull(tsDebit, "工时凭证应含借方 5101 项目成本");
        assertNotNull(tsCredit, "工时凭证应含贷方 2211 应付职工薪酬");
        assertEquals(ErpFinConstants.DC_DEBIT, tsDebit.getDcDirection(), "项目成本借方方向");
        assertEquals(0, TS_AMOUNT.compareTo(tsDebit.getDebitAmount()), "项目成本借方=4000");
        assertEquals(ErpFinConstants.DC_CREDIT, tsCredit.getDcDirection(), "应付职工薪酬贷方方向");
        assertEquals(0, TS_AMOUNT.compareTo(tsCredit.getCreditAmount()), "应付职工薪酬贷方=4000");
        assertEquals(2, tsLines.size(), "工时凭证 2 行");

        // 层 1 锚点：归集行生成（TIMESHEET）+ 项目 actualCost 回写
        ErpPrjCostCollectionLine ccLine = findCollectionLine(TS_CODE);
        assertNotNull(ccLine, "工时审批应生成归集行");
        assertEquals(ErpPrjConstants.COST_CATEGORY_LABOR, ccLine.getCostCategory(), "归集行成本类别 LABOR");
        assertEquals(0, TS_AMOUNT.compareTo(ccLine.getAmount()), "归集行金额=4000");
        assertEquals(0, TS_AMOUNT.compareTo(reloadProject(prjId).getActualCost()), "项目 actualCost 回写=4000");

        // ---------- 3. 结算 CLOSE（seed 项目 PRJ-2026-001）→ submit → approve（转固建卡 + 凭证） ----------
        ApiResponse<?> stlCloseCreate = rpcMutation("ErpPrjProjectSettlement__createSettlement",
                request("7_settlement_close_create.json5", Map.class));
        output("7_settlement_close_create_response.json5", stlCloseCreate);
        assertEquals(0, stlCloseCreate.getStatus(), "CLOSE 结算创建应成功");
        String stlCloseId = idOf(stlCloseCreate);
        addVar("stlCloseId", stlCloseId);

        ErpPrjProjectSettlement closeCreated = reloadSettlement(stlCloseId);
        assertEquals(ErpPrjConstants.SETTLEMENT_TYPE_CLOSE, closeCreated.getSettlementType(), "结算类型 CLOSE");
        assertEquals(Boolean.TRUE, closeCreated.getTransferToAsset(), "CLOSE 结算 transferToAsset=true");
        assertEquals(0, SETTLE_COST.compareTo(closeCreated.getFinalCost()), "CLOSE 结算 finalCost 派生自 seed PNL 快照=30000");

        ApiResponse<?> stlCloseSubmit = rpcMutation("ErpPrjProjectSettlement__submit",
                request("8_settlement_close_submit.json5", Map.class));
        output("8_settlement_close_submit_response.json5", stlCloseSubmit);
        assertEquals(0, stlCloseSubmit.getStatus(), "CLOSE 结算提交应成功");
        assertEquals(ErpPrjConstants.APPROVE_STATUS_SUBMITTED, reloadSettlement(stlCloseId).getApproveStatus(),
                "CLOSE 结算提交后 SUBMITTED");

        ApiResponse<?> stlCloseApprove = rpcMutation("ErpPrjProjectSettlement__approve",
                request("9_settlement_close_approve.json5", Map.class));
        output("9_settlement_close_approve_response.json5", stlCloseApprove);
        assertEquals(0, stlCloseApprove.getStatus(), "CLOSE 结算审批应成功");

        // 层 1 锚点：CLOSE 转固建卡（IN_SERVICE + 原值=最终成本）+ PROJECT_SETTLEMENT 凭证 Dr 1601/Cr 1603
        ErpPrjProjectSettlement closeApproved = reloadSettlement(stlCloseId);
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, closeApproved.getApproveStatus(), "CLOSE 结算 APPROVED");
        String assetCardId = closeApproved.getAssetCardId();
        assertNotNull(assetCardId, "CLOSE 转固创建资产卡片");
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetCardId);
        assertNotNull(asset, "资产卡片存在");
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, asset.getStatus(), "资产卡片 IN_SERVICE");
        assertEquals(0, SETTLE_COST.compareTo(asset.getOriginalValue()), "资产原值=最终成本=30000");
        addVar("assetCardId", assetCardId);

        ErpFinVoucherBillR stlCloseLink = findBillLink(closeApproved.getCode());
        assertNotNull(stlCloseLink, "CLOSE 结算应生成 PROJECT_SETTLEMENT 凭证回链");
        assertEquals("PROJECT_SETTLEMENT", stlCloseLink.getBusinessType(), "CLOSE 凭证业务类型");
        ErpFinVoucher stlCloseVoucher = requireVoucherBalanced(stlCloseLink, SETTLE_COST, "CLOSE 结算凭证");
        List<ErpFinVoucherLine> stlCloseLines = findVoucherLines(stlCloseVoucher.getId());
        ErpFinVoucherLine dr1601 = findLineBySubject(stlCloseLines, "1601");
        ErpFinVoucherLine cr1603 = findLineBySubject(stlCloseLines, "1603");
        assertNotNull(dr1601, "CLOSE 凭证应含借方 1601 固定资产");
        assertNotNull(cr1603, "CLOSE 凭证应含贷方 1603 在建工程");
        assertEquals(0, SETTLE_COST.compareTo(dr1601.getDebitAmount()), "固定资产借方=30000");
        assertEquals(0, SETTLE_COST.compareTo(cr1603.getCreditAmount()), "在建工程贷方=30000");

        // ---------- 4. 损益结转 = 结算 FINAL（seed 项目 PRJ-2026-001）→ submit → approve ----------
        ApiResponse<?> stlFinalCreate = rpcMutation("ErpPrjProjectSettlement__createSettlement",
                request("10_settlement_final_create.json5", Map.class));
        output("10_settlement_final_create_response.json5", stlFinalCreate);
        assertEquals(0, stlFinalCreate.getStatus(), "FINAL 结算创建应成功");
        String stlFinalId = idOf(stlFinalCreate);
        addVar("stlFinalId", stlFinalId);

        ErpPrjProjectSettlement finalCreated = reloadSettlement(stlFinalId);
        assertEquals(ErpPrjConstants.SETTLEMENT_TYPE_FINAL, finalCreated.getSettlementType(), "结算类型 FINAL");
        assertEquals(0, SETTLE_REVENUE.compareTo(finalCreated.getFinalRevenue()),
                "FINAL 结算 finalRevenue 派生自 seed PNL 快照=50000");
        assertEquals(0, SETTLE_PROFIT.compareTo(finalCreated.getFinalProfit()),
                "FINAL 结算 finalProfit 派生自 seed PNL 快照=20000");

        ApiResponse<?> stlFinalSubmit = rpcMutation("ErpPrjProjectSettlement__submit",
                request("11_settlement_final_submit.json5", Map.class));
        output("11_settlement_final_submit_response.json5", stlFinalSubmit);
        assertEquals(0, stlFinalSubmit.getStatus(), "FINAL 结算提交应成功");

        ApiResponse<?> stlFinalApprove = rpcMutation("ErpPrjProjectSettlement__approve",
                request("12_settlement_final_approve.json5", Map.class));
        output("12_settlement_final_approve_response.json5", stlFinalApprove);
        assertEquals(0, stlFinalApprove.getStatus(), "FINAL 结算审批应成功");

        // 层 1 锚点：损益结转凭证（Dr 5101 项目成本=30000 / Dr 4103 本年利润=20000 / Cr 6001 项目收入=50000）
        ErpPrjProjectSettlement finalApproved = reloadSettlement(stlFinalId);
        assertEquals(ErpPrjConstants.APPROVE_STATUS_APPROVED, finalApproved.getApproveStatus(), "FINAL 结算 APPROVED");
        ErpFinVoucherBillR stlFinalLink = findBillLink(finalApproved.getCode());
        assertNotNull(stlFinalLink, "FINAL 结算应生成 PROJECT_SETTLEMENT 凭证回链");
        ErpFinVoucher stlFinalVoucher = requireVoucherBalanced(stlFinalLink, SETTLE_REVENUE, "FINAL 结算凭证");
        List<ErpFinVoucherLine> stlFinalLines = findVoucherLines(stlFinalVoucher.getId());
        ErpFinVoucherLine dr5101 = findLineBySubject(stlFinalLines, "5101");
        ErpFinVoucherLine dr4103 = findLineBySubject(stlFinalLines, "4103");
        ErpFinVoucherLine cr6001 = findLineBySubject(stlFinalLines, "6001");
        assertNotNull(dr5101, "FINAL 凭证应含借方 5101 项目成本");
        assertNotNull(dr4103, "FINAL 凭证应含借方 4103 本年利润（损益平衡腿）");
        assertNotNull(cr6001, "FINAL 凭证应含贷方 6001 项目收入");
        assertEquals(ErpFinConstants.DC_DEBIT, dr5101.getDcDirection(), "项目成本借方方向");
        assertEquals(0, SETTLE_COST.compareTo(dr5101.getDebitAmount()), "项目成本借方=30000");
        assertEquals(ErpFinConstants.DC_DEBIT, dr4103.getDcDirection(), "本年利润借方方向（盈利结转）");
        assertEquals(0, SETTLE_PROFIT.compareTo(dr4103.getDebitAmount()), "本年利润借方=20000");
        assertEquals(ErpFinConstants.DC_CREDIT, cr6001.getDcDirection(), "项目收入贷方方向");
        assertEquals(0, SETTLE_REVENUE.compareTo(cr6001.getCreditAmount()), "项目收入贷方=50000");

        // 层 1 锚点（Decision ③）：seed PNL 行零改写零消费——reload 原值不变 + 结算快照派生一致
        ErpPrjProjectPnl seedPnl = daoProvider.daoFor(ErpPrjProjectPnl.class).getEntityById("1");
        assertNotNull(seedPnl, "seed PNL 行存在");
        assertEquals(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED, seedPnl.getCalcStatus(), "seed PNL 行 CALC_STATUS 未变");
        assertEquals(0, SETTLE_REVENUE.compareTo(seedPnl.getRevenueAmount()), "seed PNL 行 revenue 未变=50000");
        assertEquals(0, SETTLE_COST.compareTo(seedPnl.getTotalCost()), "seed PNL 行 totalCost 未变=30000");
        assertEquals(0, SETTLE_PROFIT.compareTo(seedPnl.getGrossProfit()), "seed PNL 行 grossProfit 未变=20000");
    }

    // ---------- helpers ----------

    private ErpPrjTimesheet reloadTimesheet(String tsId) {
        return daoProvider.daoFor(ErpPrjTimesheet.class).getEntityById(tsId);
    }

    private ErpPrjProject reloadProject(String prjId) {
        return daoProvider.daoFor(ErpPrjProject.class).getEntityById(prjId);
    }

    private ErpPrjProjectSettlement reloadSettlement(String stlId) {
        return daoProvider.daoFor(ErpPrjProjectSettlement.class).getEntityById(stlId);
    }

    private ErpPrjCostCollectionLine findCollectionLine(String sourceBillCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", ErpPrjConstants.SOURCE_BILL_TYPE_TIMESHEET));
        q.addFilter(eq("sourceBillCode", sourceBillCode));
        q.setLimit(1);
        List<ErpPrjCostCollectionLine> list = daoProvider.daoFor(ErpPrjCostCollectionLine.class)
                .findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpFinVoucherLine findLineBySubject(List<ErpFinVoucherLine> lines, String subjectCode) {
        for (ErpFinVoucherLine l : lines) {
            if (subjectCode.equals(l.getSubjectCode())) {
                return l;
            }
        }
        return null;
    }
}