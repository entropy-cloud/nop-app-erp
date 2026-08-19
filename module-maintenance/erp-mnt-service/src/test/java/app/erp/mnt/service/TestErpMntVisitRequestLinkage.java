package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.api.core.annotations.autotest.EnableSnapshot;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * UC-MAIN-05 visit↔request 双向联动测试组（RC-R1.75 / plan 2026-08-19-0445-2 Phase 3）。
 *
 * <p>覆盖：①accept 生成访问 requestId 回填（D5）②visit 完整链 start→complete → request=COMPLETED
 * （IN_PROGRESS 输入，D6 经既有 complete 边）③ACCEPTED 输入合成迁移（startRepair+complete 两条既有合法边）
 * ④request 终态（REJECTED/CANCELLED）no-op 访问正常完成 ⑤PLANNED 访问（requestId null）零影响回归
 * ⑥request 侧 COMPLETED 终态 no-op 幂等 + `_cases/` 快照。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntVisitRequestLinkage extends JunitAutoTestCase {

    @RegisterExtension
    static MntFrozenClockExtension frozenClock = new MntFrozenClockExtension();

    static final Long EQUIPMENT_ID = 301L;
    static final Long ASSIGNEE_ID = 401L;
    static final Long SNAP_EQUIPMENT_ID = 21101L;
    static final Long SNAP_REQUEST_ID = 22101L;

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(200000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    // ---------- Proof ①：accept 生成侧回填（D5） ----------

    @Test
    public void testAcceptBackfillsRequestIdOnVisit() {
        Long requestId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN);
            return null;
        });

        assertEquals(0, accept(requestId).getStatus(), "OPEN→ACCEPTED");
        ErpMntVisit visit = findVisitByCode("VST-REQ-" + requestId);
        assertNotNull(visit, "受理生成响应式访问");
        assertEquals(requestId, visit.getRequestId(), "D5：生成访问显式回填 requestId（code 命名约定保留）");
    }

    // ---------- Proof ②：IN_PROGRESS 输入 → complete 边写回 ----------

    @Test
    public void testVisitCompleteFromInProgressWritesBackCompleted() {
        Long requestId = nextId();
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS);
            seedLinkedVisit(visitId, EQUIPMENT_ID, requestId, ErpMntDaoConstants.VISIT_STATUS_DRAFT);
            return null;
        });

        assertEquals(0, schedule(visitId).getStatus(), "DRAFT→SCHEDULED");
        assertEquals(0, start(visitId).getStatus(), "SCHEDULED→IN_PROGRESS");
        assertEquals(0, complete(visitId).getStatus(), "IN_PROGRESS→COMPLETED");

        ErpMntRequest request = loadRequest(requestId);
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED, request.getStatus(),
                "D6：visit complete 经既有 complete 边写回 request COMPLETED");
        assertNotNull(request.getCompletedAt(), "completedAt 已设置");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, loadVisit(visitId).getStatus());
    }

    // ---------- Proof ③：ACCEPTED 输入 → 合成迁移写回 ----------

    @Test
    public void testVisitCompleteFromAcceptedSynthesizesMigration() {
        Long requestId = nextId();
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED);
            seedLinkedVisit(visitId, EQUIPMENT_ID, requestId, ErpMntDaoConstants.VISIT_STATUS_DRAFT);
            return null;
        });

        assertEquals(0, schedule(visitId).getStatus());
        assertEquals(0, start(visitId).getStatus());
        assertEquals(0, complete(visitId).getStatus(), "访问完成");

        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED,
                loadRequest(requestId).getStatus(),
                "D6：ACCEPTED 输入经 startRepair+complete 两条既有合法边合成迁移至 COMPLETED");
    }

    // ---------- Proof ④：request 终态 no-op，访问正常完成 ----------

    @Test
    public void testTerminalRequestNoOpVisitStillCompletes() {
        Long rejectedId = nextId();
        Long rejectedVisitId = nextId();
        Long cancelledId = nextId();
        Long cancelledVisitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(rejectedId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_REJECTED);
            seedLinkedVisit(rejectedVisitId, EQUIPMENT_ID, rejectedId, ErpMntDaoConstants.VISIT_STATUS_DRAFT);
            seedRequest(cancelledId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_CANCELLED);
            seedLinkedVisit(cancelledVisitId, EQUIPMENT_ID, cancelledId, ErpMntDaoConstants.VISIT_STATUS_DRAFT);
            return null;
        });

        for (Long visitId : new Long[]{rejectedVisitId, cancelledVisitId}) {
            assertEquals(0, schedule(visitId).getStatus());
            assertEquals(0, start(visitId).getStatus());
            assertEquals(0, complete(visitId).getStatus(), "终态请求 no-op 不阻断访问完成");
            assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, loadVisit(visitId).getStatus());
        }
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_REJECTED, loadRequest(rejectedId).getStatus(),
                "REJECTED 保持不变");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_CANCELLED, loadRequest(cancelledId).getStatus(),
                "CANCELLED 保持不变");
    }

    // ---------- Proof ⑤：PLANNED 访问（requestId null）零影响 ----------

    @Test
    public void testPlannedVisitWithoutRequestZeroImpact() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            ErpMntVisit visit = daoProvider.daoFor(ErpMntVisit.class).newEntity();
            visit.setId(visitId);
            visit.setCode("VST-PLN-NOREQ-" + visitId);
            visit.setEquipmentId(EQUIPMENT_ID);
            visit.setVisitDate(LocalDate.of(2026, 7, 1));
            visit.setStatus(ErpMntDaoConstants.VISIT_STATUS_DRAFT);
            visit.setVisitType(ErpMntDaoConstants.VISIT_TYPE_PLANNED);
            visit.setAssignedTo(ASSIGNEE_ID);
            daoProvider.daoFor(ErpMntVisit.class).saveEntity(visit);
            return null;
        });

        assertEquals(0, schedule(visitId).getStatus());
        assertEquals(0, start(visitId).getStatus());
        assertEquals(0, complete(visitId).getStatus(), "PLANNED 访问零影响（requestId null 短路）");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, loadVisit(visitId).getStatus());
        assertNull(loadVisit(visitId).getRequestId());
    }

    // ---------- Proof ⑥：request 侧 COMPLETED 终态 no-op 幂等 ----------

    @Test
    public void testCompletedRequestNoOpIdempotent() {
        Long requestId = nextId();
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_COMPLETED);
            seedLinkedVisit(visitId, EQUIPMENT_ID, requestId, ErpMntDaoConstants.VISIT_STATUS_DRAFT);
            return null;
        });

        assertEquals(0, schedule(visitId).getStatus());
        assertEquals(0, start(visitId).getStatus());
        assertEquals(0, complete(visitId).getStatus(), "COMPLETED 请求 no-op，visit 侧完成幂等");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED, loadRequest(requestId).getStatus(),
                "request 保持 COMPLETED（no-op 不重复写回）");
    }

    // ---------- 快照 ----------

    @EnableSnapshot
    @Test
    public void testAcceptBackfillSnapshot() {
        ormTemplate.runInSession(session -> {
            seedEquipment(SNAP_EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(SNAP_REQUEST_ID, SNAP_EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN);
            return null;
        });
        ApiResponse<?> resp = executeRpc(mutation, "ErpMntRequest__accept",
                request("request.json5", Map.class));
        output("response.json5", resp);
        assertEquals(0, resp.getStatus(), "快照路径 accept 成功");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> schedule(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__schedule", ApiRequest.build(Map.of("visitId", visitId)));
    }

    private ApiResponse<?> start(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__start", ApiRequest.build(Map.of("visitId", visitId)));
    }

    private ApiResponse<?> complete(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__complete", ApiRequest.build(Map.of("visitId", visitId)));
    }

    private ApiResponse<?> accept(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__accept", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedEquipment(Long id, String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = dao.newEntity();
        equipment.setId(id);
        equipment.setCode("EQ-" + id);
        equipment.setName("设备" + id);
        equipment.setStatus(status);
        dao.saveEntity(equipment);
    }

    private void seedRequest(Long id, Long equipmentId, String status) {
        IEntityDao<ErpMntRequest> dao = daoProvider.daoFor(ErpMntRequest.class);
        ErpMntRequest request = dao.newEntity();
        request.setId(id);
        request.setCode("REQ-LNK-" + id);
        request.setEquipmentId(equipmentId);
        request.setRequestDate(LocalDate.of(2026, 7, 1));
        request.setDescription("联动测试报修" + id);
        request.setPriority(ErpMntDaoConstants.PRIORITY_NORMAL);
        request.setStatus(status);
        request.setRequestedBy(ASSIGNEE_ID);
        request.setAssignedTo(ASSIGNEE_ID);
        dao.saveEntity(request);
    }

    private void seedLinkedVisit(Long id, Long equipmentId, Long requestId, String status) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        ErpMntVisit visit = dao.newEntity();
        visit.setId(id);
        visit.setCode("VST-LNK-" + id);
        visit.setEquipmentId(equipmentId);
        visit.setVisitDate(LocalDate.of(2026, 7, 1));
        visit.setStatus(status);
        visit.setVisitType(ErpMntDaoConstants.VISIT_TYPE_RESPONSIVE);
        visit.setAssignedTo(ASSIGNEE_ID);
        visit.setRequestId(requestId);
        dao.saveEntity(visit);
    }

    // ---------- query helpers ----------

    private ErpMntVisit loadVisit(Long visitId) {
        return daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId);
    }

    private ErpMntRequest loadRequest(Long requestId) {
        return daoProvider.daoFor(ErpMntRequest.class).getEntityById(requestId);
    }

    private ErpMntVisit findVisitByCode(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return daoProvider.daoFor(ErpMntVisit.class).findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
