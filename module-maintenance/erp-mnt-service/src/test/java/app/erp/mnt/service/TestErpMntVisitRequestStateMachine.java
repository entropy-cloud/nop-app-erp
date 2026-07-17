package app.erp.mnt.service;

import app.erp.mnt.dao.ErpMntDaoConstants;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import app.erp.mnt.dao.entity.ErpMntRequest;
import app.erp.mnt.dao.entity.ErpMntVisit;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 服务层集成测试：维护访问 5 态状态机 + 设备状态联动 + 维护请求 6 态状态机（受理生成访问）。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpMntVisit__schedule/start/complete/cancel} 与
 * {@code ErpMntRequest__accept/startRepair/complete/rejectRequest/cancel}，引擎建 session/事务/管道。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntVisitRequestStateMachine extends JunitAutoTestCase {

    @RegisterExtension
    static MntFrozenClockExtension frozenClock = new MntFrozenClockExtension();

    static final Long EQUIPMENT_ID = 101L;
    static final Long ASSIGNEE_ID = 201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    private final AtomicLong idSeq = new AtomicLong(100000L);

    private Long nextId() {
        return idSeq.incrementAndGet();
    }

    @Test
    public void testVisitHappyPathWithEquipmentLink() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_DRAFT, "VST-HAPPY-001");
            return null;
        });

        assertEquals(0, schedule(visitId).getStatus(), "DRAFT→SCHEDULED");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "排程阶段设备状态不变");

        assertEquals(0, start(visitId).getStatus(), "SCHEDULED→IN_PROGRESS");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, visitStatus(visitId));
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, equipmentStatus(),
                "执行中设备置 UNDER_MAINTENANCE");

        assertEquals(0, complete(visitId).getStatus(), "IN_PROGRESS→COMPLETED");
        ErpMntVisit completed = loadVisit(visitId);
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_COMPLETED, completed.getStatus());
        assertNotNull(completed.getCompletedAt(), "completedAt 已设置");
        assertNotNull(completed.getTotalMinutes(), "totalMinutes 已计算");
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "完成恢复设备 RUNNING");
    }

    @Test
    public void testVisitScheduleConflict() {
        Long existing = nextId();
        Long conflict = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedVisit(existing, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, "VST-CONFLICT-001");
            seedVisit(conflict, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_DRAFT, "VST-CONFLICT-002");
            return null;
        });

        ApiResponse<?> resp = schedule(conflict);
        assertNotEquals(0, resp.getStatus(), "同设备同日排程冲突应拒绝");
        assertEquals(ErpMntErrors.ERR_VISIT_SCHEDULE_CONFLICT.getErrorCode(), resp.getCode());
    }

    @Test
    public void testVisitCancelRestoresEquipment() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_SCHEDULED, "VST-CANCEL-001");
            return null;
        });

        start(visitId);
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE, equipmentStatus());

        assertEquals(0, cancel(visitId).getStatus(), "SCHEDULED→CANCELLED");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_CANCELLED, visitStatus(visitId));
        assertEquals(ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING, equipmentStatus(),
                "取消恢复设备 RUNNING");
    }

    @Test
    public void testVisitTerminalCannotTransition() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_COMPLETED, "VST-TERM-001");
            return null;
        });

        ApiResponse<?> bad = cancel(visitId);
        assertNotEquals(0, bad.getStatus(), "终态不可再迁移");
        assertEquals(ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION.getErrorCode(), bad.getCode());

        ApiResponse<?> badStart = start(visitId);
        assertNotEquals(0, badStart.getStatus(), "终态不可 start");
    }

    @Test
    public void testVisitIllegalTransition() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_DRAFT, "VST-ILL-001");
            return null;
        });

        ApiResponse<?> bad = start(visitId);
        assertNotEquals(0, bad.getStatus(), "DRAFT 不可直接 start（须先 schedule）");
        assertEquals(ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION.getErrorCode(), bad.getCode());
    }

    @Test
    public void testRequestAcceptGeneratesResponsiveVisit() {
        Long requestId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "REQ-ACCEPT-001");
            return null;
        });

        assertEquals(0, accept(requestId).getStatus(), "OPEN→ACCEPTED");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, requestStatus(requestId));

        ErpMntVisit visit = findVisitByCode("VST-REQ-" + requestId);
        assertNotNull(visit, "受理应生成维护访问");
        assertEquals(ErpMntDaoConstants.VISIT_TYPE_RESPONSIVE, visit.getVisitType(), "生成访问 visitType=RESPONSIVE");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_DRAFT, visit.getStatus(), "生成访问 DRAFT");
        assertEquals(EQUIPMENT_ID, visit.getEquipmentId());
    }

    @Test
    public void testRequestRejectAndCancel() {
        Long rejectId = nextId();
        Long cancelId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(rejectId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "REQ-REJ-001");
            seedRequest(cancelId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "REQ-CNL-001");
            return null;
        });

        assertEquals(0, reject(rejectId).getStatus(), "OPEN→REJECTED");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_REJECTED, requestStatus(rejectId));

        assertEquals(0, cancelRequest(cancelId).getStatus(), "OPEN→CANCELLED");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_CANCELLED, requestStatus(cancelId));
    }

    @Test
    public void testRequestFullFlow() {
        Long requestId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "REQ-FULL-001");
            return null;
        });

        assertEquals(0, accept(requestId).getStatus());
        assertEquals(0, startRepair(requestId).getStatus(), "ACCEPTED→IN_PROGRESS");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS, requestStatus(requestId));

        assertEquals(0, completeRequest(requestId).getStatus(), "IN_PROGRESS→COMPLETED");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_COMPLETED, requestStatus(requestId));
    }

    @Test
    public void testRequestIllegalTransition() {
        Long requestId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
            seedRequest(requestId, EQUIPMENT_ID, ErpMntDaoConstants.REQUEST_STATUS_OPEN, "REQ-ILL-001");
            return null;
        });

        ApiResponse<?> bad = startRepair(requestId);
        assertNotEquals(0, bad.getStatus(), "OPEN 不可直接 startRepair（须先 accept）");
        assertEquals(ErpMntErrors.ERR_INVALID_REQUEST_STATUS_TRANSITION.getErrorCode(), bad.getCode());
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

    private ApiResponse<?> cancel(Long visitId) {
        return executeRpc(mutation, "ErpMntVisit__cancel", ApiRequest.build(Map.of("visitId", visitId)));
    }

    private ApiResponse<?> accept(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__accept", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> startRepair(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__startRepair", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> reject(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__rejectRequest", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> completeRequest(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__complete", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> cancelRequest(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__cancel", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    // ---------- seed helpers ----------

    private void seedEquipment(Long id, String status) {
        IEntityDao<ErpMntEquipment> dao = daoProvider.daoFor(ErpMntEquipment.class);
        ErpMntEquipment equipment = new ErpMntEquipment();
        equipment.setId(id);
        equipment.setCode("EQ-" + id);
        equipment.setName("设备" + id);
        equipment.setStatus(status);
        dao.saveEntity(equipment);
    }

    private void seedVisit(Long id, Long equipmentId, String status, String code) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        ErpMntVisit visit = new ErpMntVisit();
        visit.setId(id);
        visit.setCode(code);
        visit.setEquipmentId(equipmentId);
        visit.setVisitDate(LocalDate.of(2026, 7, 1));
        visit.setStatus(status);
        visit.setVisitType(ErpMntDaoConstants.VISIT_TYPE_PLANNED);
        visit.setAssignedTo(ASSIGNEE_ID);
        dao.saveEntity(visit);
    }

    private void seedRequest(Long id, Long equipmentId, String status, String code) {
        IEntityDao<ErpMntRequest> dao = daoProvider.daoFor(ErpMntRequest.class);
        ErpMntRequest request = new ErpMntRequest();
        request.setId(id);
        request.setCode(code);
        request.setEquipmentId(equipmentId);
        request.setRequestDate(LocalDate.of(2026, 7, 1));
        request.setDescription("报修" + code);
        request.setPriority(ErpMntDaoConstants.PRIORITY_NORMAL);
        request.setStatus(status);
        request.setRequestedBy(ASSIGNEE_ID);
        request.setAssignedTo(ASSIGNEE_ID);
        dao.saveEntity(request);
    }

    // ---------- query helpers ----------

    private ErpMntVisit loadVisit(Long visitId) {
        return daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId);
    }

    private String visitStatus(Long visitId) {
        return loadVisit(visitId).getStatus();
    }

    private String requestStatus(Long requestId) {
        return daoProvider.daoFor(ErpMntRequest.class).getEntityById(requestId).getStatus();
    }

    private String equipmentStatus() {
        return daoProvider.daoFor(ErpMntEquipment.class).getEntityById(EQUIPMENT_ID).getStatus();
    }

    private ErpMntVisit findVisitByCode(String code) {
        IEntityDao<ErpMntVisit> dao = daoProvider.daoFor(ErpMntVisit.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }
}
