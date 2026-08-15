package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdPartner;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 合同 terminate 状态机守卫测试（P1-MA2-072，plan 2026-07-30-0631-2-r1-22；
 * RC-R1.34 两段化改造——terminate 发起 + approveTermination 通过后 TERMINATED）。
 *
 * <p>覆盖 terminate 守卫扩展：接受 ACTIVE（生效合同提前终止）+ NEGOTIATION（谈判破裂放弃）两类源态，
 * 其余状态（DRAFT/SUSPENDED/EXPIRED/TERMINATED）拒绝。对齐 {@code docs/design/contract/state-machine.md} §2/§3。
 *
 * <p>RC-R1.34（P1-RC-076）语义更新：terminate 为两段化发起（生成 PENDING 法务记录 + 合同保持原状态），
 * 断言 TERMINATED 的路径须经 approveTermination（本测试环境无角色种子 → approverId null →
 * 任意操作员可批）。非法源态拒绝断言不变（守卫仍在 terminate mutation 上）。
 *
 * <p>沿用 Phase 1 样板（-service 模块、JunitAutoTestCase、@NopTestConfig）；直接经 DAO 校验状态落库，
 * 不依赖快照断言（与 TestErpCtContractPosting 同范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtContractTerminate extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testTerminateFromActiveSucceeds() {
        long contractId = setupActiveContract();
        long vid = contractId;

        ApiResponse<?> resp = terminate(contractId);
        assertEquals(0, resp.getStatus(), "ACTIVE 合同 terminate 发起应成功: " + resp);
        ErpCtContract pending = daoProvider.daoFor(ErpCtContract.class).getEntityById(vid);
        assertEquals("ACTIVE", pending.getStatus(), "发起终止申请合同保持 ACTIVE");
        long recordId = pendingTerminationRecordId(contractId);
        assertNotNull(recordId, "应生成 PENDING 法务记录");

        ApiResponse<?> approve = executeRpc(mutation, "ErpCtContract__approveTermination",
                ApiRequest.build(Map.of("recordId", recordId)));
        assertEquals(0, approve.getStatus(), "法务通过后终止执行: " + approve);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(vid);
        assertEquals("TERMINATED", contract.getStatus(), "ACTIVE→TERMINATED 两段后落地");
    }

    @Test
    public void testTerminateFromNegotiationSucceeds() {
        long partnerId = createPartner();
        long currencyId = createCurrency();
        long contractId = createContract(partnerId, currencyId, "NEGOTIATION");

        ApiResponse<?> resp = terminate(contractId);
        assertEquals(0, resp.getStatus(), "NEGOTIATION 合同 terminate 发起应成功（谈判破裂出口）: " + resp);
        long recordId = pendingTerminationRecordId(contractId);
        assertNotNull(recordId, "应生成 PENDING 法务记录");
        ApiResponse<?> approve = executeRpc(mutation, "ErpCtContract__approveTermination",
                ApiRequest.build(Map.of("recordId", recordId)));
        assertEquals(0, approve.getStatus(), "法务通过后终止执行: " + approve);

        ErpCtContract contract = daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId);
        assertEquals("TERMINATED", contract.getStatus(), "NEGOTIATION→TERMINATED 应落地");
    }

    @Test
    public void testTerminateRejectedForDRAFT() {
        assertTerminateRejected("DRAFT");
    }

    @Test
    public void testTerminateRejectedForSUSPENDED() {
        assertTerminateRejected("SUSPENDED");
    }

    @Test
    public void testTerminateRejectedForEXPIRED() {
        assertTerminateRejected("EXPIRED");
    }

    @Test
    public void testTerminateRejectedForTERMINATED() {
        assertTerminateRejected("TERMINATED");
    }

    private void assertTerminateRejected(String illegalStatus) {
        long contractId = setupContractInStatus(illegalStatus);
        ApiResponse<?> resp = terminate(contractId);
        assertNotEquals(0, resp.getStatus(),
                illegalStatus + " 合同 terminate 应被拒绝（ERR_CT_ILLEGAL_STATUS_TRANSITION）: " + resp);
    }

    private long pendingTerminationRecordId(long contractId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("contractId", contractId));
        q.addFilter(eq("approvalMatrixId", null));
        List<ErpCtApprovalRecord> records = daoProvider.daoFor(ErpCtApprovalRecord.class).findAllByQuery(q);
        if (records == null) {
            return 0L;
        }
        for (ErpCtApprovalRecord r : records) {
            if (ErpCtConstants.APPROVAL_STATUS_PENDING.equals(r.getApprovalStatus())) {
                return r.getId();
            }
        }
        return 0L;
    }

    private long setupContractInStatus(String status) {
        long partnerId = createPartner();
        long currencyId = createCurrency();
        switch (status) {
            case "DRAFT":
                return createContract(partnerId, currencyId, "DRAFT");
            case "NEGOTIATION":
                return createContract(partnerId, currencyId, "NEGOTIATION");
            case "ACTIVE": {
                long id = createContract(partnerId, currencyId, "NEGOTIATION");
                createVersion(id, 1, true, "FINALIZED");
                executeRpc(mutation, "ErpCtContract__activate", ApiRequest.build(Map.of("contractId", id)));
                return id;
            }
            case "SUSPENDED": {
                long id = setupContractInStatus("ACTIVE");
                executeRpc(mutation, "ErpCtContract__suspend", ApiRequest.build(Map.of("contractId", id)));
                return id;
            }
            case "EXPIRED": {
                long id = setupContractInStatus("ACTIVE");
                executeRpc(mutation, "ErpCtContract__expire", ApiRequest.build(Map.of("contractId", id)));
                return id;
            }
            case "TERMINATED": {
                long id = setupContractInStatus("ACTIVE");
                executeRpc(mutation, "ErpCtContract__terminate", ApiRequest.build(Map.of("contractId", id)));
                executeRpc(mutation, "ErpCtContract__approveTermination",
                        ApiRequest.build(Map.of("recordId", pendingTerminationRecordId(id))));
                return id;
            }
            default:
                throw new IllegalArgumentException("unsupported status: " + status);
        }
    }

    private long setupActiveContract() {
        return setupContractInStatus("ACTIVE");
    }

    private long createPartner() {
        long[] holder = new long[1];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-TRM-PARTNER-" + System.nanoTime());
            p.setName("终止测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            holder[0] = p.getId();
            return null;
        });
        return holder[0];
    }

    private long createCurrency() {
        long[] holder = new long[1];
        ormTemplate.runInSession(session -> {
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            // CODE precision=10，使用短固定码（JunitAutoTestCase localDb=true 每测试方法隔离 DB）
            c.setCode("CNY");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            holder[0] = c.getId();
            return null;
        });
        return holder[0];
    }

    private long createContract(long partnerId, long currencyId, String status) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CT-TRM-" + System.nanoTime());
        data.put("contractName", "终止状态机测试合同");
        data.put("contractType", "PURCHASE");
        data.put("contractDirection", "INBOUND");
        data.put("partnerId", partnerId);
        data.put("currencyId", currencyId);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("status", status);
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContract__save 应成功: " + resp);
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

    private ApiResponse<?> terminate(long contractId) {
        return executeRpc(mutation, "ErpCtContract__terminate",
                ApiRequest.build(Map.of("contractId", contractId)));
    }

    private long toLongId(Map<?, ?> r) {
        Object id = r.get("id");
        if (id instanceof Number) {
            return ((Number) id).longValue();
        }
        return Long.parseLong(String.valueOf(id));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
