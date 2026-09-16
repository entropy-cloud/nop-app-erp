package app.erp.fin.service.entity;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F3.8 批（plan 2026-09-17-0330-1）finance 侧 Proof 测试：
 * <ul>
 *   <li>fin-015：{@code reverseVoucher} 对业务/红字回链凭证拒绝单边标记（ERP_REVERSE_VOUCHER_BILL_LINKED），
 *       手工凭证（无 BillR 回链）保留标记式行为；</li>
 *   <li>fin-009/010/011/013/014 的 Proof 测试见同批 TestErpFinP38Proofs（posting 包）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpFinP38Batch extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    io.nop.orm.IOrmTemplate ormTemplate;

    @Inject
    IGraphQLEngine graphQLEngine;

    private Map<String, ErpMdSubject> subjects;
    private String periodId;

    private void seedPeriod() {
        IEntityDao<app.erp.fin.dao.entity.ErpFinAccountingPeriod> dao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinAccountingPeriod.class);
        app.erp.fin.dao.entity.ErpFinAccountingPeriod p = dao.newEntity();
        p.setCode("2026-07");
        p.setName("2026-07");
        p.setYear(2026);
        p.setMonth(7);
        p.setStartDate(LocalDate.of(2026, 7, 1));
        p.setEndDate(LocalDate.of(2026, 7, 31));
        p.setStatus("OPEN");
        dao.saveEntity(p);
        periodId = p.getId();
    }

    private Map<String, ErpMdSubject> seedSubjects() {
        Map<String, ErpMdSubject> map = new HashMap<>();
        map.put("1001", seedSubject("1001", "库存现金"));
        map.put("6001", seedSubject("6001", "主营业务收入"));
        return map;
    }

    private ErpMdSubject seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = dao.newEntity();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("ASSET");
        s.setDirection("DEBIT");
        s.setStatus("ACTIVE");
        dao.saveEntity(s);
        return s;
    }

    /** 手工凭证（无 ErpFinVoucherBillR 回链）：POSTED 双行。 */
    private String seedPostedManualVoucher(String code) {
        seedPeriod();
        IEntityDao<ErpFinVoucher> vDao = daoProvider.daoFor(ErpFinVoucher.class);
        ErpFinVoucher v = vDao.newEntity();
        v.setCode(code);
        v.setVoucherType("TRANSFER");
        v.setVoucherDate(LocalDate.of(2026, 7, 10));
        v.setOrgId("1");
        v.setAcctSchemaId("1");
        v.setPeriodId(periodId);
        v.setTotalDebit(new BigDecimal("50"));
        v.setTotalCredit(new BigDecimal("50"));
        v.setIsReversed(false);
        v.setDocStatus(ErpFinConstants.VOUCHER_STATUS_POSTED);
        vDao.saveEntity(v);
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherLine> lDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherLine.class);
        String[][] lines = {{"1001", "库存现金", ErpFinConstants.DC_DEBIT, "50"},
                {"6001", "主营业务收入", ErpFinConstants.DC_CREDIT, "50"}};
        int lineNo = 1;
        for (String[] l : lines) {
            app.erp.fin.dao.entity.ErpFinVoucherLine line = lDao.newEntity();
            line.setVoucherId(v.getId());
            line.setLineNo(lineNo++);
            line.setSubjectId(subjects.get(l[0]).getId());
            line.setSubjectCode(l[0]);
            line.setSubjectName(l[1]);
            line.setDcDirection(l[2]);
            line.setDebitAmount(ErpFinConstants.DC_DEBIT.equals(l[2]) ? new BigDecimal(l[3]) : BigDecimal.ZERO);
            line.setCreditAmount(ErpFinConstants.DC_CREDIT.equals(l[2]) ? new BigDecimal(l[3]) : BigDecimal.ZERO);
            line.setCurrencyId("1");
            line.setExchangeRate(BigDecimal.ONE);
            line.setAmountSource(new BigDecimal(l[3]));
            line.setAmountFunctional(new BigDecimal(l[3]));
            line.setAcctSchemaId("1");
            lDao.saveEntity(line);
        }
        return v.getId();
    }

    /** 业务凭证：POSTED 手工凭证 + 人工植入 ErpFinVoucherBillR 业财回链（模拟引擎 persistVoucher 产物）。 */
    private String seedPostedBusinessVoucher(String code) {
        return seedPostedBusinessVoucher(code, null);
    }

    /** 可选 postingType=REVERSAL 变体（红字凭证同样带 BillR 回链）。 */
    private String seedPostedBusinessVoucher(String code, String postingType) {
        String voucherId = seedPostedManualVoucher(code);
        if (postingType != null) {
            ormTemplate.runInSession(session -> {
                ErpFinVoucher v = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
                v.setPostingType(postingType);
                daoProvider.daoFor(ErpFinVoucher.class).saveOrUpdateEntity(v);
                return null;
            });
        }
        IEntityDao<app.erp.fin.dao.entity.ErpFinVoucherBillR> rDao =
                daoProvider.daoFor(app.erp.fin.dao.entity.ErpFinVoucherBillR.class);
        app.erp.fin.dao.entity.ErpFinVoucherBillR link = rDao.newEntity();
        link.setVoucherId(voucherId);
        link.setBillType("AP_INVOICE");
        link.setBillCode("BI-" + code);
        link.setBusinessType("AP_INVOICE");
        rDao.saveEntity(link);
        return voucherId;
    }

    // ---------- fin-015：reverseVoucher 业务/红字回链凭证拒绝 ----------

    @Test
    public void testReverseVoucherBusinessVoucherRejected() {
        subjects = seedSubjects();
        String voucherId = seedPostedBusinessVoucher("V-P38-BIZ-001");

        ApiResponse<?> resp = rpc(mutation, "ErpFinVoucher__reverseVoucher",
                ApiRequest.build(Map.of("voucherId", voucherId)));
        assertEquals(ErpFinErrors.ERR_REVERSE_VOUCHER_BILL_LINKED.getErrorCode(), resp.getCode(),
                "业务回链凭证 reverseVoucher 应拒绝（修复前单边标记致源单反审核永久阻断）");
        ErpFinVoucher after = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertEquals(Boolean.FALSE, after.getIsReversed(), "拒绝路径 isReversed 保持 false");
    }

    /**
     * fin-015 第三场景（结束审计整改）：红字凭证（postingType=REVERSAL，带 BillR）同样被拒——
     * 防「对红字凭证置 isReversed 致 GL 排除式汇总翻符号」。守卫类型无关，本用例固化该行为。
     */
    @Test
    public void testReverseVoucherReversalVoucherRejected() {
        subjects = seedSubjects();
        String voucherId = seedPostedBusinessVoucher("V-P38-REV-001");
        ormTemplate.runInSession(() -> {
            app.erp.fin.dao.entity.ErpFinVoucher v =
                    daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
            v.setPostingType("REVERSAL");
            daoProvider.daoFor(ErpFinVoucher.class).updateEntity(v);
        });

        ApiResponse<?> resp = rpc(mutation, "ErpFinVoucher__reverseVoucher",
                ApiRequest.build(Map.of("voucherId", voucherId)));
        assertEquals(ErpFinErrors.ERR_REVERSE_VOUCHER_BILL_LINKED.getErrorCode(), resp.getCode(),
                "红字凭证（带 BillR）reverseVoucher 同被拒（防 GL 排除式汇总翻符号）");
    }

    @Test
    public void testReverseVoucherManualVoucherStillWorks() {
        subjects = seedSubjects();
        String voucherId = seedPostedManualVoucher("V-P38-MAN-001");

        ApiResponse<?> resp = rpc(mutation, "ErpFinVoucher__reverseVoucher",
                ApiRequest.build(Map.of("voucherId", voucherId)));
        assertEquals(0, resp.getStatus(), "无回链手工凭证标记式行为保留（state-machine.md L41 简化裁决范围）");
        ErpFinVoucher after = daoProvider.daoFor(ErpFinVoucher.class).getEntityById(voucherId);
        assertEquals(Boolean.TRUE, after.getIsReversed(), "手工凭证 isReversed 置 true");
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
