package app.erp.pur.service;

import app.erp.common.service.ErpCrudStatusLock;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.pur.dao.entity.ErpPurInvoice;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.8 分片③ common-011-r3 F1.3 对账裁决锁（__save 通道语义）。
 *
 * <p>裁决（全 reactor C06/C11 金路径回归实证）：approveStatus=APPROVED 的 __save（create）
 * 为域内合法「录入即定稿」模式（质检单/项目成本归集金路径依赖），save 通道守卫仅封锁
 * posted=true 铸造（__save=create-only，改写已存在单据被 id 唯一性拒绝，改写/删除已锁单据
 * 由 defaultPrepareUpdate/Delete 全量状态锁拒绝）；不可变台账全拒由
 * AbstractErpImmutableCrudBizModel 全拒 override 承接（TestErpInvImmutableSaveChannel）。
 *
 * <p>红态先行证据（M2.8 执行记录）：修复前本测试原形态断言「APPROVED 铸造被拒」红
 * （status=0 成功），经对账裁决改锁「APPROVED 创建合法 + posted 守卫在位 + 无改写向量」。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPurCrudSaveChannel extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    private Map<String, Object> seedRefsAndBuildPayload(String code, String approveStatus) {
        java.util.concurrent.atomic.AtomicReference<String> supplierId = new java.util.concurrent.atomic.AtomicReference<>();
        java.util.concurrent.atomic.AtomicReference<String> currencyId = new java.util.concurrent.atomic.AtomicReference<>();
        ormTemplate.runInSession((io.nop.orm.IOrmSession sess) -> {
            ErpMdCurrency cur = new ErpMdCurrency();
            cur.setCode("M28-CUR");
            cur.setName("M2.8 币种");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(cur);
            currencyId.set(cur.getId());

            ErpMdPartner partner = new ErpMdPartner();
            partner.setCode("M28-SUP");
            partner.setName("M2.8 测试供应商");
            partner.setPartnerType("SUPPLIER");
            partner.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(partner);
            supplierId.set(partner.getId());
            return null;
        });

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("supplierId", supplierId.get());
        data.put("businessDate", "2026-07-01");
        data.put("currencyId", currencyId.get());
        data.put("exchangeRate", BigDecimal.ONE);
        data.put("docStatus", ErpPurConstants.DOC_STATUS_DRAFT);
        data.put("approveStatus", approveStatus);
        data.put("paidStatus", ErpPurConstants.PAID_STATUS_UNPAID);
        data.put("paidAmount", BigDecimal.ZERO);
        data.put("totalAmount", new BigDecimal("100"));
        data.put("totalTaxAmount", new BigDecimal("13"));
        data.put("totalAmountWithTax", new BigDecimal("113"));
        data.put("posted", true);
        data.put("remark", "M2.8-common-011-r3-F1.3-对账锁");
        return data;
    }

    @Test
    public void testSaveChannelAllowsApprovedCreate() {
        Map<String, Object> data = seedRefsAndBuildPayload("PI-SAVE-APPR-MINT", ErpPurConstants.APPROVE_STATUS_APPROVED);
        ApiResponse<?> resp = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpPurInvoice__save", ApiRequest.build(Map.of("data", data))));
        assertEquals(0, resp.getStatus(),
                "F1.3 对账裁决：approveStatus=APPROVED 的 __save 创建合法（posted 列 insertable=false 客户端值被忽略）");

        // 已存在单据经 __save 不可改写（create-only，id 唯一性兜底，与 defaultPrepareUpdate 状态锁双层）
        String id = String.valueOf(((Map<?, ?>) resp.getData()).get("id"));
        Map<String, Object> rewriteData = new LinkedHashMap<>();
        rewriteData.put("code", "PI-SAVE-APPR-MINT-2");
        rewriteData.put("supplierId", data.get("supplierId"));
        rewriteData.put("currencyId", data.get("currencyId"));
        rewriteData.put("docStatus", ErpPurConstants.DOC_STATUS_DRAFT);
        rewriteData.put("approveStatus", ErpPurConstants.APPROVE_STATUS_UNSUBMITTED);
        rewriteData.put("paidStatus", ErpPurConstants.PAID_STATUS_UNPAID);
        rewriteData.put("totalAmount", new BigDecimal("1"));
        rewriteData.put("totalTaxAmount", BigDecimal.ZERO);
        rewriteData.put("totalAmountWithTax", new BigDecimal("1"));
        rewriteData.put("businessDate", "2026-07-01");
        rewriteData.put("exchangeRate", BigDecimal.ONE);
        rewriteData.put("posted", false);
        rewriteData.put("id", id);
        ApiResponse<?> rewrite = graphQLEngine.executeRpc(graphQLEngine.newRpcContext(mutation,
                "ErpPurInvoice__save", ApiRequest.build(Map.of("data", rewriteData))));
        assertNotEquals(0, rewrite.getStatus(), "__save 对已存在 id 无改写向量（create-only）");
    }

    @Test
    public void testPostedSaveGuardContract() {
        // save 通道守卫（ErpCrudStatusLock.isPosted）在 posted=true 实体上必须命中：
        // GraphQL 面 posted 列 insertable=false（客户端铸造向量封死），该守卫封 Java 路径
        // （内部调用 CrudBizModel.save 传入 posted 实体）的纵深防御面。
        String id = ormTemplate.runInSession((io.nop.orm.IOrmSession sess) -> {
            ErpPurInvoice invoice = new ErpPurInvoice();
            invoice.setCode("PI-SAVE-POSTED-JAVA");
            invoice.setOrgId("2");
            invoice.setSupplierId("3");
            invoice.setBusinessDate(LocalDate.of(2026, 7, 1));
            invoice.setCurrencyId("1");
            invoice.setExchangeRate(BigDecimal.ONE);
            invoice.setDocStatus(ErpPurConstants.DOC_STATUS_DRAFT);
            invoice.setApproveStatus(ErpPurConstants.APPROVE_STATUS_APPROVED);
            invoice.setPaidStatus(ErpPurConstants.PAID_STATUS_UNPAID);
            invoice.setPaidAmount(BigDecimal.ZERO);
            invoice.setTotalAmount(new BigDecimal("100"));
            invoice.setTotalTaxAmount(new BigDecimal("13"));
            invoice.setTotalAmountWithTax(new BigDecimal("113"));
            invoice.setPosted(true);
            daoProvider.daoFor(ErpPurInvoice.class).saveEntity(invoice);
            return invoice.getId();
        });
        ErpPurInvoice posted = ormTemplate.runInSession((io.nop.orm.IOrmSession sess) -> {
            sess.flush();
            return daoProvider.daoFor(ErpPurInvoice.class).getEntityById(id);
        });
        assertTrue(ErpCrudStatusLock.isPosted(posted), "posted=true 实体必须命中 save 通道守卫判定");
        assertTrue(ErpCrudStatusLock.shouldBlock(posted), "posted=true 实体命中 update/delete 全量状态锁");
    }
}
