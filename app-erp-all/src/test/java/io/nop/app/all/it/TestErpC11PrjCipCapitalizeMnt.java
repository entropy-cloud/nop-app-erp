package io.nop.app.all.it;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCapitalization;
import app.erp.ast.dao.entity.ErpAstCip;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.ast.service.ErpAstConstants;
import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B6 C11：项目成本归集 → CIP → 资产资本化 → 维护设备（按 {@code docs/design/integration-testing.md §6 C11} 规格）。
 *
 * <p>全链（自包含成本归集单/CIP/资产类别/设备卡，引用部署 seed：项目 PRJ-2026-001（id=1，OPEN）+ 组织 2 +
 * 币种 1 + 2026-07 OPEN 期间 + 科目 1601/1603 + 设备分类 MNT-CAT-MACH）：自包含成本归集单 save
 * （{@code ErpPrjCostCollection__save}，引用 seed 项目 PRJ-2026-001，金额 20000）→ 自包含资产类别
 * （{@code ErpAstAssetCategory__save}，直线法 36 月 + 科目 1601/在建工程科目 1603）→ CIP 建单
 * （{@code ErpAstCip__save}，sourceType 由 CIP 转固内部生成，projectId=1 弱引用关联 seed 项目）→
 * {@code ErpAstCip__startConstruction}（DRAFT→IN_CONSTRUCTION）→ {@code ErpAstCip__addCostItem}
 * （PURCHASE 20000，sourceBillType=COST_COLLECTION 回链成本归集单）→ {@code ErpAstCip__transferToAsset}
 * （完工转固：内部走资本化审批链建卡 + CAPITALIZATION 凭证）→ 资产卡片 IN_SERVICE + 原值=20000 +
 * 折旧计划生成 → {@code ErpMntEquipment__save}（设备卡 assetId 跨域引用新资产）。
 *
 * <p>Phase 1 Decision（两裁决，落盘设计文档 §6 C11 勘误）：
 * <ol>
 *   <li><b>CIP→资本化动作面裁决</b>：设计文档 §6 C11 步骤 2/3 原述「save → 完工审批 →
 *       {@code ErpAstAssetCapitalization__save} → submit → approve」为漂移——实仓
 *       {@code ErpAstCipBizModel} 无「完工审批」动作（实仓 = startConstruction/addCostItem/addProgressBilling/
 *       transferToAsset/reverseTransfer）；{@code transferToAsset} 内部经
 *       {@code ErpAstCipTransferToAssetProcessor → ErpAstCipProcessor.doTransfer} 复用既有资本化审批链
 *       （submit → approve 立即建卡 + 出 CAPITALIZATION 凭证），CIP 终态 TRANSFERRED + isCompleted=true +
 *       completedAssetId 回写。本用例按实仓动作面落地，无独立 Capitalization save/submit/approve 步骤。</li>
 *   <li><b>资本化科目/折旧计划裁决</b>：CAPITALIZATION 凭证借贷科目 = 资产类别 subjectId（1601 固定资产）/
 *       cipSubjectId（1603 在建工程），未配置类别科目时回退标准码 1601/1603（{@code CapitalizationPostingDispatcher}
 *       + {@code CapitalizationAcctDocProvider}）；本用例显式自包含类别科目（subjectId=1601 行 id 27、
 *       cipSubjectId=1603 行 id 29）保证断言锚点确定性。资产折旧计划随建卡生成（类别直线法 36 月，断言非空）。
 *       CIP 成本 = Σ CostItem amountFunctional = 资产原值断言锚点成立。</li>
 *   <li><b>数据来源</b>：seed 资产类别（AST-CAT-IT/MACH）无 subjectId/cipSubjectId 且无 seed CIP——
 *       CIP 转固链路前置自包含建数（类别 + CIP），未触发 seed 修正授权（B4/B5 同型裁决）。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（CIP 状态迁移 + 资产卡片 IN_SERVICE + CAPITALIZATION 凭证借贷平衡
 * + CIP 成本 = 资本化原值 + 设备卡 assetId 跨域核对）；层 2 = 每步 response 快照；层 3 = output/tables 变更行
 * （cost_collection + cip + cip_cost_item + asset + capitalization + depreciation_schedule + voucher +
 * voucher_line + voucher_bill_r + equipment）。RECORDING→CHECKING 往返已按 M0.2 试点范式完成。
 * 冻结时钟 2026-07-17（{@code C11C12FrozenClockExtension}）保证 businessDate/transferDate 确定性；
 * 资本化单/资产卡 code 含 currentTimeMillis 后缀（实仓生成逻辑），快照层按 B4 先例以 {@code *} 通配处置。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC11PrjCipCapitalizeMnt extends ErpIntegrationTestCase {

    static final String CC_CODE = "IT-C11-CC-001";
    static final String CIP_CODE = "IT-C11-CIP-001";
    static final String CAT_CODE = "IT-C11-CAT-001";
    static final String EQ_CODE = "IT-C11-EQ-001";
    static final String SEED_PROJECT_ID = "1";      // seed PRJ-2026-001
    static final String ORG_ID = "2";               // seed 组织 2
    static final String CURRENCY_ID = "1";          // seed CNY
    static final String SUBJECT_FIXED_ASSET_ID = "27";  // seed 科目 1601 固定资产
    static final String SUBJECT_CIP_ID = "29";          // seed 科目 1603 在建工程
    static final String MNT_CATEGORY_ID = "1";      // seed MNT-CAT-MACH 设备分类
    static final BigDecimal COST_AMOUNT = new BigDecimal("20000");

    @RegisterExtension
    static C11C12FrozenClockExtension frozenClock = new C11C12FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testPrjCipCapitalizeMntClosedLoop() {
        // ---------- 1. 自包含成本归集单 save（引用 seed 项目 PRJ-2026-001） ----------
        ApiResponse<?> ccSave = rpcMutation("ErpPrjCostCollection__save",
                request("1_cost_collection_save.json5", Map.class));
        output("1_cost_collection_save_response.json5", ccSave);
        assertEquals(0, ccSave.getStatus(), "成本归集单保存应成功");
        String ccId = idOf(ccSave);
        addVar("ccId", ccId);
        addVar("ccCode", CC_CODE);

        // 层 1 锚点：归集单落库 + 金额正确
        ErpPrjCostCollection cc = daoProvider.daoFor(ErpPrjCostCollection.class).getEntityById(ccId);
        assertNotNull(cc, "成本归集单应落库");
        assertEquals(SEED_PROJECT_ID, cc.getProjectId(), "成本归集单引用 seed 项目 PRJ-2026-001");
        assertEquals(0, COST_AMOUNT.compareTo(cc.getTotalAmount()), "归集单金额=20000");

        // ---------- 2. 自包含资产类别（固定资产/在建工程科目）+ CIP 建单 ----------
        ApiResponse<?> catSave = rpcMutation("ErpAstAssetCategory__save",
                request("2_category_save.json5", Map.class));
        output("2_category_save_response.json5", catSave);
        assertEquals(0, catSave.getStatus(), "资产类别保存应成功");
        addVar("catId", idOf(catSave));

        ApiResponse<?> cipSave = rpcMutation("ErpAstCip__save", request("3_cip_save.json5", Map.class));
        output("3_cip_save_response.json5", cipSave);
        assertEquals(0, cipSave.getStatus(), "CIP 建单保存应成功");
        String cipId = idOf(cipSave);
        addVar("cipId", cipId);
        addVar("cipCode", CIP_CODE);

        ErpAstCip cip = reloadCip(cipId);
        assertEquals(ErpAstConstants.CIP_STATUS_DRAFT, cip.getStatus(), "CIP 初始 DRAFT");
        assertEquals(SEED_PROJECT_ID, cip.getProjectId(), "CIP 弱引用 seed 项目 PRJ-2026-001");

        // ---------- 3. startConstruction（DRAFT→IN_CONSTRUCTION） ----------
        ApiResponse<?> start = rpcMutation("ErpAstCip__startConstruction",
                request("4_cip_start.json5", Map.class));
        output("4_cip_start_response.json5", start);
        assertEquals(0, start.getStatus(), "CIP 开工应成功");
        assertEquals(ErpAstConstants.CIP_STATUS_IN_CONSTRUCTION, reloadCip(cipId).getStatus(),
                "开工后 CIP IN_CONSTRUCTION");

        // ---------- 4. addCostItem（成本归集 20000，sourceBill 回链成本归集单） ----------
        ApiResponse<?> addCost = rpcMutation("ErpAstCip__addCostItem",
                request("5_cip_add_cost_item.json5", Map.class));
        output("5_cip_add_cost_item_response.json5", addCost);
        assertEquals(0, addCost.getStatus(), "CIP 成本归集应成功");
        assertEquals(0, COST_AMOUNT.compareTo(reloadCip(cipId).getAccumulatedCost()),
                "CIP 累计归集成本=20000");

        // ---------- 5. transferToAsset（完工转固：内部资本化审批链建卡 + CAPITALIZATION 凭证） ----------
        ApiResponse<?> transfer = rpcMutation("ErpAstCip__transferToAsset",
                request("6_cip_transfer.json5", Map.class));
        output("6_cip_transfer_response.json5", transfer);
        assertEquals(0, transfer.getStatus(), "CIP 完工转固应成功");

        // 层 1 锚点：CIP 终态 TRANSFERRED + isCompleted + completedAssetId 回写
        ErpAstCip transferred = reloadCip(cipId);
        assertEquals(ErpAstConstants.CIP_STATUS_TRANSFERRED, transferred.getStatus(),
                "CIP 终态 TRANSFERRED");
        assertEquals(Boolean.TRUE, transferred.getIsCompleted(), "isCompleted=true");
        String assetId = transferred.getCompletedAssetId();
        assertNotNull(assetId, "completedAssetId 回写");
        addVar("assetId", assetId);

        // 层 1 锚点：资产卡片 IN_SERVICE + 原值 = CIP 累计成本
        ErpAstAsset asset = daoProvider.daoFor(ErpAstAsset.class).getEntityById(assetId);
        assertNotNull(asset, "转固资产卡片存在");
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE, asset.getStatus(), "资产卡片 IN_SERVICE");
        assertEquals(0, COST_AMOUNT.compareTo(asset.getOriginalValue()), "资产原值=资本化原值=20000");

        // 层 1 锚点：资本化单（sourceType=CIP + sourceCode=CIP 单号）posted=true + 折旧计划生成
        ErpAstAssetCapitalization cap = findCapitalizationByCip(CIP_CODE);
        assertNotNull(cap, "资本化单已创建");
        assertEquals(ErpAstConstants.SOURCE_TYPE_CIP, cap.getSourceType(), "资本化单来源类型=CIP");
        assertEquals(Boolean.TRUE, cap.getPosted(), "资本化单 posted=true（CAPITALIZATION 凭证已生成）");
        List<ErpAstDepreciationSchedule> schedules = findSchedulesByAsset(assetId);
        assertFalse(schedules.isEmpty(), "折旧计划已生成");

        // 层 1 锚点：CAPITALIZATION 凭证借贷平衡（Dr 1601 固定资产 / Cr 1603 在建工程 = 20000）
        ErpFinVoucherBillR link = findBillLink(cap.getCode());
        ErpFinVoucher voucher = requireVoucherBalanced(link, COST_AMOUNT, "CAPITALIZATION 凭证");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, voucher.getDocStatus(), "CAPITALIZATION 凭证已过账");
        List<ErpFinVoucherLine> lines = findVoucherLines(voucher.getId());
        ErpFinVoucherLine debit1601 = findLineBySubject(lines, "1601");
        ErpFinVoucherLine credit1603 = findLineBySubject(lines, "1603");
        assertNotNull(debit1601, "CAPITALIZATION 凭证应含借方 1601 固定资产");
        assertNotNull(credit1603, "CAPITALIZATION 凭证应含贷方 1603 在建工程");
        assertEquals(ErpFinConstants.DC_DEBIT, debit1601.getDcDirection(), "固定资产借方方向");
        assertEquals(0, COST_AMOUNT.compareTo(debit1601.getDebitAmount()), "固定资产借方=20000");
        assertEquals(ErpFinConstants.DC_CREDIT, credit1603.getDcDirection(), "在建工程贷方方向");
        assertEquals(0, COST_AMOUNT.compareTo(credit1603.getCreditAmount()), "在建工程贷方=20000");
        assertEquals(2, lines.size(), "CAPITALIZATION 凭证 2 行");

        // ---------- 6. ErpMntEquipment__save（设备卡 assetId 跨域引用新资产） ----------
        ApiResponse<?> eqSave = rpcMutation("ErpMntEquipment__save",
                request("7_equipment_save.json5", Map.class));
        output("7_equipment_save_response.json5", eqSave);
        assertEquals(0, eqSave.getStatus(), "设备卡保存应成功");
        String eqId = idOf(eqSave);
        addVar("eqId", eqId);

        // 层 1 锚点：设备卡 assetId 指向新资产
        ErpMntEquipment equipment = daoProvider.daoFor(ErpMntEquipment.class).getEntityById(eqId);
        assertNotNull(equipment, "设备卡应落库");
        assertEquals(assetId, equipment.getAssetId(), "设备卡 assetId 指向转固新资产");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipment.getStatus(), "设备状态 RUNNING");
    }

    // ---------- helpers ----------

    private ErpAstCip reloadCip(String cipId) {
        return daoProvider.daoFor(ErpAstCip.class).getEntityById(cipId);
    }

    private ErpAstAssetCapitalization findCapitalizationByCip(String cipCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceCode", cipCode));
        q.addFilter(eq("sourceType", ErpAstConstants.SOURCE_TYPE_CIP));
        q.setLimit(1);
        List<ErpAstAssetCapitalization> list = daoProvider.daoFor(ErpAstAssetCapitalization.class)
                .findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpAstDepreciationSchedule> findSchedulesByAsset(String assetId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("assetId", assetId));
        return daoProvider.daoFor(ErpAstDepreciationSchedule.class).findAllByQuery(q);
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