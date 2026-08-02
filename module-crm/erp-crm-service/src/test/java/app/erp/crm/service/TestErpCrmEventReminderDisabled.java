package app.erp.crm.service;

import app.erp.crm.dao.entity.ErpCrmEvent;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * findDueReminders config-gated 关闭测试（R1.24 / P1-MA2-076）。
 *
 * <p>覆盖：{@code erp-crm.event-reminder-enabled}=false 时即使存在窗口内 PLANNED 事件也返回空（行为不变）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testConfigFile = "classpath:event-reminder-disabled-test.yaml")
public class TestErpCrmEventReminderDisabled extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testDisabledReturnsEmptyEvenWithDueEvents() {
        LocalDateTime now = CoreMetrics.currentDateTime();
        ormTemplate.runInSession(() -> {
            // 窗口内 PLANNED 事件（若启用本应命中）
            IEntityDao<ErpCrmEvent> dao = daoProvider.daoFor(ErpCrmEvent.class);
            ErpCrmEvent event = new ErpCrmEvent();
            event.setId(4601L);
            event.setCode("EVT-DIS-001");
            event.setOrgId(ORG_ID);
            event.setEventType("CALL");
            event.setSubject("事件-EVT-DIS-001");
            event.setStatus(ErpCrmConstants.EVENT_STATUS_PLANNED);
            event.setPriority("NORMAL");
            event.setStartDateTime(Timestamp.valueOf(now.plusMinutes(5)));
            event.setEndDateTime(Timestamp.valueOf(now.plusMinutes(35)));
            event.setReminderMinutesBefore(60);
            dao.saveEntity(event);
        });
        ApiResponse<?> resp = graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(query, "ErpCrmEvent__findDueReminders",
                        ApiRequest.build(Map.of("windowMinutes", 60))));
        assertTrue(resp.getStatus() == 0, "findDueReminders 应成功");
        List<?> list = (List<?>) resp.getData();
        assertTrue(list == null || list.isEmpty(),
                "event-reminder-enabled=false 时应返回空（即使存在窗口内 PLANNED 事件）");
    }
}
