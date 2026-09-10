package app.erp.cs.service.entity;

import app.erp.crm.biz.IErpCrmTeamBiz;
import app.erp.crm.biz.IErpCrmTeamMemberBiz;
import app.erp.cs.dao.entity.ErpCsTeam;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P3-CK-cs-025-r3 工单自动分配降级可观测性回归：跨域解析失败降级返空池必须 LOG.warn
 * （降级原因 + teamCode，英文载体），与全域 notify「降级必 WARN」范式对齐；空池降级语义保持。
 */
public class TestTicketAssignResolverDegradationLog {

    private ListAppender<ILoggingEvent> appender;
    private Logger logger;

    @BeforeEach
    void attachAppender() {
        logger = (Logger) LoggerFactory.getLogger(TicketAssignResolver.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
    }

    @Test
    public void testDegradationWritesWarnWithTeamCode() {
        TicketAssignResolver resolver = new TicketAssignResolver();
        String boom = "crm resolver down (fault injection)";
        // 跨域 crm 团队查询失败（故障注入桩）→ 降级返空池
        resolver.crmTeamBiz = (IErpCrmTeamBiz) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IErpCrmTeamBiz.class},
                (p, m, a) -> {
                    if ("findList".equals(m.getName())) {
                        throw new IllegalStateException(boom);
                    }
                    return defaultValue(m.getReturnType());
                });
        resolver.crmTeamMemberBiz = (IErpCrmTeamMemberBiz) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{IErpCrmTeamMemberBiz.class},
                (p, m, a) -> defaultValue(m.getReturnType()));

        ErpCsTeam team = new ErpCsTeam();
        team.setCode("CS-DEGRADE-TEAM");

        IServiceContext ctx = new ServiceContextImpl();
        List<String> pool = resolver.resolveCandidatePool(team, ctx);

        assertTrue(pool.isEmpty(), "降级语义保持：解析失败返空池");
        assertTrue(appender.list.stream()
                        .anyMatch(e -> e.getLevel() == Level.WARN
                                && e.getFormattedMessage().contains("CS-DEGRADE-TEAM")
                                && e.getFormattedMessage().contains(boom)),
                "降级必须写 WARN 日志（含 teamCode + 降级原因），实际事件="
                        + appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList());
    }

    private Object defaultValue(Class<?> type) {
        if (type == null || !type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == int.class) {
            return 0;
        }
        return null;
    }
}
