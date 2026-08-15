package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.contract.dao.entity.ErpCtContractLine;
import app.erp.contract.dao.entity.ErpCtContractVersion;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdUoM;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 合同创建校验 + 版本族测试（RC-R1.32，P1-RC-072 + P1-RC-073）。
 *
 * <p>覆盖（对齐 plan 2026-08-15-0456-2 Phase 3 测试矩阵）：
 * <ol>
 *   <li>创建校验：totalAmount 匹配放行 / 不匹配拒绝（嵌套行）+ 日期倒置拒绝 + 缺失容忍（无行时 totalAmount 可空）；</li>
 *   <li>submit：DRAFT→NEGOTIATION + v1 自动创建（versionNo=1、isCurrent=true、DRAFT）+ 非 DRAFT 拒绝 +
 *       已有版本放行并保留既有 DRAFT current 版本不动（amend 场景）；</li>
 *   <li>全链回归双路径：submit→finalizeVersion(v1)→activate（首提路径）+ amend→submit→finalizeVersion(v2)→activate
 *       （amend 路径，activate 前显式 finalizeVersion 契约）；</li>
 *   <li>rejectAmend：DRAFT→ACTIVE + SIGNED 优先最大 versionNo 恢复 isCurrent + 非 DRAFT 拒绝 +
 *       重复 amend→rejectAmend 周期二次恢复目标正确性 + finalize-then-reject 边界（恢复 SIGNED v1 而非 FINALIZED v2）；</li>
 *   <li>amend 行保留语义（D3 选项 A：同合同模型下行保留可编辑）；</li>
 *   <li>GraphQL RPC 冒烟（submit/rejectAmend 均经 GraphQL 实调断言）。</li>
 * </ol>
 *
 * <p>沿用 {@link TestErpCtContractPosting} 样板（JunitAutoTestCase + @NopTestConfig + 直接 DAO 断言）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtContractCreateValidate extends JunitAutoTestCase {

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    // ---------- ① 创建校验 ----------

    @Test
    public void testSaveAcceptsNoLinesMissingAmounts() {
        long contractId = createContract("DRAFT", null, null, null);
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertNotNull(contract);
        assertEquals("DRAFT", contract.getStatus());
    }

    @Test
    public void testSaveAcceptsMatchingNestedLines() {
        Map<String, Object> data = headData("DRAFT");
        data.put("totalAmount", new BigDecimal("1000"));
        data.put("lines", List.of(lineData(1, new BigDecimal("400")), lineData(2, new BigDecimal("600"))));
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save", ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "totalAmount==Σ行金额 应放行: " + resp);
    }

    @Test
    public void testSaveRejectsAmountMismatchWithNestedLines() {
        Map<String, Object> data = headData("DRAFT");
        data.put("totalAmount", new BigDecimal("1000"));
        data.put("lines", List.of(lineData(1, new BigDecimal("400")), lineData(2, new BigDecimal("300"))));
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save", ApiRequest.build(Map.of("data", data)));
        assertNotEquals(0, resp.getStatus(), "totalAmount≠Σ行金额 应被拒绝（ERR_CT_AMOUNT_MISMATCH）: " + resp);
        assertTrue(String.valueOf(resp).contains("amount-mismatch")
                    || String.valueOf(resp).contains("不一致"),
                "拒绝应携带金额不一致错误码: " + resp);
    }

    @Test
    public void testSaveRejectsNullTotalAmountWithNestedLines() {
        Map<String, Object> data = headData("DRAFT");
        data.put("lines", List.of(lineData(1, new BigDecimal("500"))));
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save", ApiRequest.build(Map.of("data", data)));
        assertNotEquals(0, resp.getStatus(), "有行时 totalAmount 缺失应被拒绝: " + resp);
    }

    @Test
    public void testSaveRejectsDateRangeInvalid() {
        Map<String, Object> data = headData("DRAFT");
        data.put("startDate", "2027-01-01");
        data.put("endDate", "2026-01-01");
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save", ApiRequest.build(Map.of("data", data)));
        assertNotEquals(0, resp.getStatus(), "startDate≥endDate 应被拒绝（ERR_CT_DATE_RANGE_INVALID）: " + resp);
    }

    // ---------- ② submit ----------

    @Test
    public void testSubmitCreatesV1AndMovesToNegotiation() {
        long contractId = createContract("DRAFT", null, null, null);
        ApiResponse<?> resp = submit(contractId);
        assertEquals(0, resp.getStatus(), "DRAFT 合同 submit 应成功: " + resp);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("NEGOTIATION", contract.getStatus(), "submit 后合同状态=NEGOTIATION");

        List<ErpCtContractVersion> versions = findVersions(contractId);
        assertEquals(1, versions.size(), "submit 应自动创建 v1 版本");
        ErpCtContractVersion v1 = versions.get(0);
        assertEquals(1, v1.getVersionNo(), "v1 versionNo=1");
        assertEquals(true, v1.getIsCurrent(), "v1 isCurrent=true");
        assertEquals("DRAFT", v1.getStatus(), "v1 初始状态=DRAFT（D2 裁决）");
    }

    @Test
    public void testSubmitRejectedForNonDraft() {
        long contractId = createContract("NEGOTIATION", null, null, null);
        ApiResponse<?> resp = submit(contractId);
        assertNotEquals(0, resp.getStatus(), "非 DRAFT 合同 submit 应被拒绝: " + resp);
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("NEGOTIATION", contract.getStatus(), "拒绝后状态不变");
    }

    @Test
    public void testSubmitKeepsExistingDraftCurrentVersion() {
        // amend 生命周期场景：v1 SIGNED current → amend（v2 DRAFT current，合同 DRAFT）→ submit
        // → 合同 NEGOTIATION + v2 保持 DRAFT current 不动（MAJOR-1 语义：已有版本放行，不新建 v3）
        long contractId = setupActiveWithSignedV1();
        executeRpc(mutation, "ErpCtContract__amend", ApiRequest.build(Map.of("contractId", contractId)));

        ApiResponse<?> resp = submit(contractId);
        assertEquals(0, resp.getStatus(), "已有版本（amend 生命周期）submit 应放行: " + resp);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("NEGOTIATION", contract.getStatus(), "submit 后合同状态=NEGOTIATION");

        List<ErpCtContractVersion> versions = findVersions(contractId);
        assertEquals(2, versions.size(), "已有版本 submit 不应新建 v3");
        ErpCtContractVersion v1 = findVersionByNo(versions, 1);
        ErpCtContractVersion v2 = findVersionByNo(versions, 2);
        assertEquals(false, v1.getIsCurrent(), "v1（SIGNED）保持非 current");
        assertEquals(true, v2.getIsCurrent(), "v2（DRAFT）保持 current 不动");
        assertEquals("DRAFT", v2.getStatus(), "v2 保持 DRAFT（提交不自动定稿，activate 前须 finalizeVersion）");
    }

    @Test
    public void testSubmitRejectsAmountMismatchViaDaoLines() {
        // 头先存（无行，totalAmount=1000）→ 行后加（amount=500）→ submit 权威门卫拒绝（DAO 查询口径）
        long contractId = createContract("DRAFT", new BigDecimal("1000"), "2026-01-01", "2027-12-31");
        long materialId = createMaterial(createUoM());
        saveLine(contractId, materialId, new BigDecimal("500"));

        ApiResponse<?> resp = submit(contractId);
        assertNotEquals(0, resp.getStatus(), "submit 应拒绝金额不一致（DAO 行汇总口径）: " + resp);
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("DRAFT", contract.getStatus(), "拒绝后状态不变");
    }

    // ---------- ③ 全链回归双路径 ----------

    @Test
    public void testFullChainFirstSubmissionPath() {
        // 零版本首提路径：submit（建 v1 DRAFT）→ finalizeVersion(v1) → activate（级联签署 v1 SIGNED）
        long contractId = createContract("DRAFT", null, null, null);
        assertEquals(0, submit(contractId).getStatus());

        ErpCtContractVersion v1 = findVersions(contractId).get(0);
        assertEquals(0, finalizeVersion(v1.getId()).getStatus(), "finalizeVersion(v1) 应成功");

        ApiResponse<?> act = activate(contractId);
        assertEquals(0, act.getStatus(), "首提路径 activate 应成功: " + act);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("ACTIVE", contract.getStatus());
        ErpCtContractVersion v1After = daoProvider.daoFor(ErpCtContractVersion.class).getEntityById(v1.getId());
        assertEquals("SIGNED", v1After.getStatus(), "activate 级联签署 v1→SIGNED");
        assertEquals(true, v1After.getIsCurrent(), "v1 isCurrent=true");
    }

    @Test
    public void testFullChainAmendPath() {
        // amend 路径：amend（v2 DRAFT current）→ submit → finalizeVersion(v2) → activate（级联签署 v2 SIGNED）
        long contractId = setupActiveWithSignedV1();
        executeRpc(mutation, "ErpCtContract__amend", ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, submit(contractId).getStatus());

        ErpCtContractVersion v2 = findVersionByNo(findVersions(contractId), 2);
        assertEquals(0, finalizeVersion(v2.getId()).getStatus(), "finalizeVersion(v2) 应成功");

        ApiResponse<?> act = activate(contractId);
        assertEquals(0, act.getStatus(), "amend 路径 activate 应成功: " + act);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("ACTIVE", contract.getStatus());
        ErpCtContractVersion v2After = daoProvider.daoFor(ErpCtContractVersion.class).getEntityById(v2.getId());
        assertEquals("SIGNED", v2After.getStatus(), "activate 级联签署 v2→SIGNED");
        assertEquals(true, v2After.getIsCurrent(), "v2 isCurrent=true");
        ErpCtContractVersion v1 = findVersionByNo(findVersions(contractId), 1);
        assertEquals(false, v1.getIsCurrent(), "v1 isCurrent=false");
    }

    // ---------- ④ rejectAmend ----------

    @Test
    public void testRejectAmendRestoresSignedMaxVersion() {
        long contractId = setupActiveWithSignedV1();
        executeRpc(mutation, "ErpCtContract__amend", ApiRequest.build(Map.of("contractId", contractId)));

        ApiResponse<?> resp = rejectAmend(contractId);
        assertEquals(0, resp.getStatus(), "DRAFT 变更单 rejectAmend 应成功: " + resp);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("ACTIVE", contract.getStatus(), "rejectAmend 后合同恢复 ACTIVE");

        List<ErpCtContractVersion> versions = findVersions(contractId);
        ErpCtContractVersion v1 = findVersionByNo(versions, 1);
        ErpCtContractVersion v2 = findVersionByNo(versions, 2);
        assertEquals(true, v1.getIsCurrent(), "SIGNED v1 恢复 isCurrent=true（D5 选项 B：SIGNED 优先最大 versionNo）");
        assertEquals(false, v2.getIsCurrent(), "amend 遗留 DRAFT v2 isCurrent=false");
    }

    @Test
    public void testRejectAmendRejectedForNonDraft() {
        long contractId = createContract("NEGOTIATION", null, null, null);
        ApiResponse<?> resp = rejectAmend(contractId);
        assertNotEquals(0, resp.getStatus(), "非 DRAFT 合同 rejectAmend 应被拒绝: " + resp);
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("NEGOTIATION", contract.getStatus(), "拒绝后状态不变");
    }

    @Test
    public void testRejectAmendFinalizeThenRejectRestoresSigned() {
        // finalize-then-reject 边界（iteration-4）：amend → finalizeVersion(v2) → rejectAmend
        // → 恢复 SIGNED v1 而非 FINALIZED v2（防 ACTIVE + 未签署 current 版本不一致态）
        long contractId = setupActiveWithSignedV1();
        executeRpc(mutation, "ErpCtContract__amend", ApiRequest.build(Map.of("contractId", contractId)));
        ErpCtContractVersion v2 = findVersionByNo(findVersions(contractId), 2);
        assertEquals(0, finalizeVersion(v2.getId()).getStatus(), "finalizeVersion(v2) 应成功（可达路径）");

        ApiResponse<?> resp = rejectAmend(contractId);
        assertEquals(0, resp.getStatus(), "finalize-then-reject rejectAmend 应成功: " + resp);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("ACTIVE", contract.getStatus());
        List<ErpCtContractVersion> versions = findVersions(contractId);
        assertEquals(true, findVersionByNo(versions, 1).getIsCurrent(), "恢复 SIGNED v1 为 current（非 FINALIZED v2）");
        assertEquals(false, findVersionByNo(versions, 2).getIsCurrent(), "FINALIZED v2 保持非 current（未签署不恢复）");
    }

    @Test
    public void testRepeatedAmendRejectCycleRestoresSignedTarget() {
        // 重复 amend→rejectAmend 周期（MAJOR-2 修正）：二次周期后恢复目标仍为 SIGNED v1，
        // 遗留 DRAFT 行（v2/v3）不被误恢复为 current
        long contractId = setupActiveWithSignedV1();
        for (int i = 0; i < 2; i++) {
            executeRpc(mutation, "ErpCtContract__amend", ApiRequest.build(Map.of("contractId", contractId)));
            ApiResponse<?> resp = rejectAmend(contractId);
            assertEquals(0, resp.getStatus(), "第 " + (i + 1) + " 轮 rejectAmend 应成功: " + resp);
        }
        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("ACTIVE", contract.getStatus(), "周期后合同保持 ACTIVE");

        List<ErpCtContractVersion> versions = findVersions(contractId);
        assertEquals(true, findVersionByNo(versions, 1).getIsCurrent(), "SIGNED v1 恒为 current");
        for (ErpCtContractVersion v : versions) {
            if (v.getVersionNo() != null && v.getVersionNo() > 1) {
                assertEquals(false, v.getIsCurrent(), "遗留 DRAFT 版本 v" + v.getVersionNo() + " 不得为 current");
            }
        }
    }

    // ---------- ⑤ amend 行保留语义（D3 选项 A） ----------

    @Test
    public void testAmendKeepsLinesRetained() {
        // 同合同 amend 模型：行留 contractId 下，amend 后行保留且仍可编辑（复制语义 = 行保留）
        long[] setup = setupActiveContractWithLine();
        long contractId = setup[0];
        long lineId = setup[1];

        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__amend",
                ApiRequest.build(Map.of("contractId", contractId)));
        assertEquals(0, resp.getStatus(), "amend 应成功: " + resp);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("DRAFT", contract.getStatus(), "amend 后合同=DRAFT（变更单态）");

        List<ErpCtContractLine> lines = findLines(contractId);
        assertEquals(1, lines.size(), "amend 后行保留（零复制语义：行留在原 contractId 下）");
        ErpCtContractLine line = daoProvider.daoFor(ErpCtContractLine.class).getEntityById(lineId);
        assertEquals(0, new BigDecimal("1000").compareTo(line.getAmount()), "行字段不变（金额 scale 容忍）");

        // 行仍可编辑（变更 DRAFT 态下允许增删改）
        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", lineId);
        upd.put("amount", new BigDecimal("1200"));
        ApiResponse<?> lineUpd = executeRpc(mutation, "ErpCtContractLine__update",
                ApiRequest.build(Map.of("data", upd)));
        assertEquals(0, lineUpd.getStatus(), "amend 后行可编辑: " + lineUpd);
        ErpCtContractLine lineAfter = daoProvider.daoFor(ErpCtContractLine.class).getEntityById(lineId);
        assertEquals(0, new BigDecimal("1200").compareTo(lineAfter.getAmount()), "行编辑生效（金额 scale 容忍）");
    }

    // ---------- helpers ----------

    private long setupActiveWithSignedV1() {
        long partnerId = createPartner();
        long currencyId = createCurrency();
        long contractId = createContract("NEGOTIATION", null, "2026-01-01", "2027-12-31");
        createVersion(contractId, 1, true, "FINALIZED");
        ApiResponse<?> act = activate(contractId);
        assertEquals(0, act.getStatus(), "setup activate 应成功: " + act);
        ErpCtContractVersion v1 = daoProvider.daoFor(ErpCtContractVersion.class)
                .findFirstByQuery(eqQuery("contractId", contractId));
        assertEquals("SIGNED", v1.getStatus(), "setup v1 应已签署");
        return contractId;
    }

    private long[] setupActiveContractWithLine() {
        long partnerId = createPartner();
        long currencyId = createCurrency();
        long materialId = createMaterial(createUoM());
        long contractId = createContract("NEGOTIATION", null, "2026-01-01", "2027-12-31");
        long lineId = saveLine(contractId, materialId, new BigDecimal("1000"));
        createVersion(contractId, 1, true, "FINALIZED");
        ApiResponse<?> act = activate(contractId);
        assertEquals(0, act.getStatus(), "setup activate 应成功: " + act);
        return new long[]{contractId, lineId};
    }

    private long createPartner() {
        ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
        p.setCode("CT-CV-PARTNER-" + System.nanoTime());
        p.setName("创建校验测试伙伴");
        p.setPartnerType("CUSTOMER");
        p.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
        return p.getId();
    }

    private long createCurrency() {
        ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
        c.setCode("CNY-CV");
        c.setName("人民币");
        daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
        return c.getId();
    }

    private long createUoM() {
        ErpMdUoM u = daoProvider.daoFor(ErpMdUoM.class).newEntity();
        u.setCode("PCS-CT-CV");
        u.setName("个");
        daoProvider.daoFor(ErpMdUoM.class).saveEntity(u);
        return u.getId();
    }

    private long createMaterial(long uomId) {
        ErpMdMaterial m = daoProvider.daoFor(ErpMdMaterial.class).newEntity();
        m.setCode("MAT-CT-CV-" + System.nanoTime());
        m.setName("创建校验测试物料");
        m.setMaterialType("GOODS");
        m.setUoMId(uomId);
        m.setStatus("ACTIVE");
        daoProvider.daoFor(ErpMdMaterial.class).saveEntity(m);
        return m.getId();
    }

    private long createContract(String status, BigDecimal totalAmount, String startDate, String endDate) {
        Map<String, Object> data = headData(status);
        if (totalAmount != null) {
            data.put("totalAmount", totalAmount);
        }
        if (startDate != null) {
            data.put("startDate", startDate);
        }
        if (endDate != null) {
            data.put("endDate", endDate);
        }
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContract__save 应成功: " + resp);
        return toLongId((Map<?, ?>) resp.getData());
    }

    private Map<String, Object> headData(String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CT-CV-" + System.nanoTime());
        data.put("contractName", "创建校验测试合同");
        data.put("contractType", "PURCHASE");
        data.put("contractDirection", "INBOUND");
        data.put("partnerId", createPartner());
        data.put("currencyId", createCurrency());
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("status", status);
        return data;
    }

    private Map<String, Object> lineData(int lineNo, BigDecimal amount) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("lineNo", lineNo);
        line.put("amount", amount);
        return line;
    }

    private long saveLine(long contractId, long materialId, BigDecimal amount) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lineNo", 1);
        data.put("contractId", contractId);
        data.put("materialId", materialId);
        data.put("quantity", new BigDecimal("100"));
        data.put("unitPrice", amount.divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP));
        data.put("amount", amount);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractLine__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractLine__save 应成功: " + resp);
        return toLongId((Map<?, ?>) resp.getData());
    }

    private void createVersion(long contractId, int versionNo, boolean isCurrent, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("contractId", contractId);
        data.put("versionNo", versionNo);
        data.put("versionDate", "2026-01-01");
        data.put("isCurrent", isCurrent);
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContractVersion__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContractVersion__save 应成功: " + resp);
    }

    private List<ErpCtContractVersion> findVersions(long contractId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", contractId));
        return daoProvider.daoFor(ErpCtContractVersion.class).findAllByQuery(q);
    }

    private List<ErpCtContractLine> findLines(long contractId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", contractId));
        return daoProvider.daoFor(ErpCtContractLine.class).findAllByQuery(q);
    }

    private ErpCtContractVersion findVersionByNo(List<ErpCtContractVersion> versions, int versionNo) {
        for (ErpCtContractVersion v : versions) {
            if (v.getVersionNo() != null && v.getVersionNo() == versionNo) {
                return v;
            }
        }
        throw new IllegalStateException("versionNo=" + versionNo + " not found");
    }

    private ApiResponse<?> submit(long contractId) {
        return executeRpc(mutation, "ErpCtContract__submit", ApiRequest.build(Map.of("contractId", contractId)));
    }

    private ApiResponse<?> rejectAmend(long contractId) {
        return executeRpc(mutation, "ErpCtContract__rejectAmend", ApiRequest.build(Map.of("contractId", contractId)));
    }

    private ApiResponse<?> finalizeVersion(long versionId) {
        return executeRpc(mutation, "ErpCtContractVersion__finalizeVersion",
                ApiRequest.build(Map.of("versionId", versionId)));
    }

    private ApiResponse<?> activate(long contractId) {
        return executeRpc(mutation, "ErpCtContract__activate", ApiRequest.build(Map.of("contractId", contractId)));
    }

    private long toLongId(Map<?, ?> r) {
        Object id = r.get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return Long.parseLong(String.valueOf(id));
    }

    private QueryBean eqQuery(String field, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(field, value));
        return q;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
