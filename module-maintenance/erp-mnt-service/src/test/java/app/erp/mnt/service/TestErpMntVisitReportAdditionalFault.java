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

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.31 / P1-RC-069 测试矩阵：{@code ErpMntVisit__reportAdditionalFault} 编排
 * （IN_PROGRESS 守卫 + remark 追加语义 + 建新 OPEN 维护请求 + 不中断本次维护 + 重复上报 code 唯一 + 闭环可操作性）。
 *
 * <p>经 {@link IGraphQLEngine} 调 RPC（GraphQL 冒烟即测试面）；拒绝路径断言零 request 落库；
 * 快照录制于 {@code _cases/}（动态 code 由 autotest 框架 {@code @var} 掩码，CHECKING 回放安全）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMntVisitReportAdditionalFault extends JunitAutoTestCase {

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
    public void testReportAdditionalFaultFromInProgressVisitCreatesOpenRequest() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, "VST-FAULT-001");
            return null;
        });

        ApiResponse<?> resp = reportAdditionalFault(visitId, "液压泵异响，需另开请求处理", null, null);
        assertEquals(0, resp.getStatus(), "IN_PROGRESS visit 上报应成功");

        Long requestId = responseId(resp);
        ErpMntRequest request = loadRequest(requestId);
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_OPEN, request.getStatus(), "新 request 初始 OPEN");
        assertEquals(EQUIPMENT_ID, request.getEquipmentId(), "equipmentId 同 visit");
        assertEquals("液压泵异响，需另开请求处理", request.getDescription(), "description 正确");
        assertEquals(ErpMntDaoConstants.PRIORITY_NORMAL, request.getPriority(), "priority 缺省 NORMAL");
        assertEquals(ASSIGNEE_ID, request.getRequestedBy(), "requestedBy 回退 visit.assignedTo（测试上下文 userId 非数值）");
        assertTrue(request.getCode().startsWith("REQ-VST-" + visitId + "-"), "code 前缀 REQ-VST-{visitId}-: " + request.getCode());
        assertTrue(request.getCode().length() <= 50, "code 长度 ≤ requestCode precision 50: " + request.getCode());

        assertEquals(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, visitStatus(visitId), "visit 保持 IN_PROGRESS（不中断）");
        ErpMntVisit visit = loadVisit(visitId);
        assertNull(visit.getResult(), "E4: 不写 result");
        assertNull(visit.getTotalMinutes(), "E4: 不写 totalMinutes（工时归 complete 流程）");
        assertNotNull(visit.getRemark(), "E3: visit remark 已记录");
        assertTrue(visit.getRemark().contains("[额外故障] 液压泵异响"), "remark 追加内容: " + visit.getRemark());
    }

    @Test
    public void testReportAdditionalFaultRejectedWhenNotInProgress() {
        String[] statuses = new String[]{ErpMntDaoConstants.VISIT_STATUS_DRAFT,
                ErpMntDaoConstants.VISIT_STATUS_SCHEDULED,
                ErpMntDaoConstants.VISIT_STATUS_COMPLETED,
                ErpMntDaoConstants.VISIT_STATUS_CANCELLED};
        for (int i = 0; i < statuses.length; i++) {
            String status = statuses[i];
            Long visitId = nextId();
            Long equipmentId = EQUIPMENT_ID + i;
            ormTemplate.runInSession(session -> {
                seedEquipment(equipmentId, ErpMntDaoConstants.EQUIPMENT_STATUS_RUNNING);
                seedVisit(visitId, equipmentId, status, "VST-FAULT-REJ-" + status);
                return null;
            });

            ApiResponse<?> resp = reportAdditionalFault(visitId, "描述", null, null);
            assertNotEquals(0, resp.getStatus(), status + " 不可上报应拒绝");
            assertEquals(ErpMntErrors.ERR_INVALID_VISIT_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                    status + " 拒绝错误码 E2 复用");
            assertNotNull(resp.getMsg());
            assertTrue(resp.getMsg().contains(status), "错误消息含 currentStatus: " + resp.getMsg());
            assertEquals(0, countRequests(), status + " 拒绝时零 request 落库");
        }
    }

    @Test
    public void testReportAdditionalFaultAppendsVisitRemark() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, "VST-FAULT-RMK-001");
            return null;
        });
        ormTemplate.runInSession(session -> {
            ErpMntVisit visit = loadVisit(visitId);
            visit.setRemark("已完成基础保养");
            daoProvider.daoFor(ErpMntVisit.class).updateEntity(visit);
            return null;
        });

        ApiResponse<?> resp = reportAdditionalFault(visitId, "皮带老化", null, null);
        assertEquals(0, resp.getStatus());
        String remark = loadVisit(visitId).getRemark();
        assertTrue(remark.startsWith("已完成基础保养\n[额外故障] 皮带老化"), "E3 追加语义保留既有记录: " + remark);
    }

    @Test
    public void testReportAdditionalFaultVisitNotFound() {
        ApiResponse<?> resp = reportAdditionalFault(999999L, "描述", null, null);
        assertNotEquals(0, resp.getStatus());
        assertEquals(ErpMntErrors.ERR_VISIT_NOT_FOUND.getErrorCode(), resp.getCode());
        assertEquals(0, countRequests(), "visit 不存在零落库");
    }

    @Test
    public void testRepeatReportAdditionalFaultCreatesSecondRequestWithUniqueCode() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, "VST-FAULT-RPT-001");
            return null;
        });

        ApiResponse<?> r1 = reportAdditionalFault(visitId, "第一故障", null, null);
        assertEquals(0, r1.getStatus());
        ApiResponse<?> r2 = reportAdditionalFault(visitId, "第二故障", null, null);
        assertEquals(0, r2.getStatus(), "visit 保持 IN_PROGRESS 可合法多次上报（L1 多故障语义）");

        Long id1 = responseId(r1);
        Long id2 = responseId(r2);
        assertNotEquals(id1, id2, "两次上报两 request");
        assertNotEquals(loadRequest(id1).getCode(), loadRequest(id2).getCode(), "code 唯一（E1 毫秒时间戳后缀零 UK 冲突）");
        assertEquals(2, countRequests(), "两次上报两 request 落库");
        assertEquals(ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, visitStatus(visitId), "visit 仍不中断");
    }

    @Test
    public void testReportAdditionalFaultRequestCanContinueFullChain() {
        Long visitId = nextId();
        ormTemplate.runInSession(session -> {
            seedEquipment(EQUIPMENT_ID, ErpMntDaoConstants.EQUIPMENT_STATUS_UNDER_MAINTENANCE);
            seedVisit(visitId, EQUIPMENT_ID, ErpMntDaoConstants.VISIT_STATUS_IN_PROGRESS, "VST-FAULT-FLOW-001");
            return null;
        });

        ApiResponse<?> resp = reportAdditionalFault(visitId, "停机类额外故障", ErpMntDaoConstants.PRIORITY_HIGH, "现场已隔离");
        assertEquals(0, resp.getStatus());
        Long requestId = responseId(resp);
        ErpMntRequest request = loadRequest(requestId);
        assertEquals(ErpMntDaoConstants.PRIORITY_HIGH, request.getPriority(), "priority 按入参");
        assertEquals("现场已隔离", request.getRemark(), "remark 入参归新 request.remark（E3/E1）");

        assertEquals(0, accept(requestId).getStatus(), "新 request 可 accept（闭环）");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_ACCEPTED, requestStatus(requestId));
        assertEquals(0, startRepair(requestId).getStatus(), "新 request 可 startRepair（闭环）");
        assertEquals(ErpMntDaoConstants.REQUEST_STATUS_IN_PROGRESS, requestStatus(requestId));
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> reportAdditionalFault(Long visitId, String description, String priority, String remark) {
        Map<String, Object> args = new java.util.LinkedHashMap<>();
        args.put("visitId", visitId);
        args.put("description", description);
        args.put("priority", priority);
        args.put("remark", remark);
        return executeRpc(mutation, "ErpMntVisit__reportAdditionalFault", ApiRequest.build(args));
    }

    private Long responseId(ApiResponse<?> resp) {
        return Long.parseLong(String.valueOf(((Map<?, ?>) resp.getData()).get("id")));
    }

    private ApiResponse<?> accept(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__accept", ApiRequest.build(Map.of("requestId", requestId)));
    }

    private ApiResponse<?> startRepair(Long requestId) {
        return executeRpc(mutation, "ErpMntRequest__startRepair", ApiRequest.build(Map.of("requestId", requestId)));
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

    // ---------- query helpers ----------

    private ErpMntVisit loadVisit(Long visitId) {
        return daoProvider.daoFor(ErpMntVisit.class).getEntityById(visitId);
    }

    private String visitStatus(Long visitId) {
        return loadVisit(visitId).getStatus();
    }

    private ErpMntRequest loadRequest(Long requestId) {
        return daoProvider.daoFor(ErpMntRequest.class).getEntityById(requestId);
    }

    private String requestStatus(Long requestId) {
        return loadRequest(requestId).getStatus();
    }

    private long countRequests() {
        return daoProvider.daoFor(ErpMntRequest.class).findAllByQuery(new QueryBean()).size();
    }
}
