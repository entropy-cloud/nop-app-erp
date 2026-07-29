package app.erp.ast.service.posting;

import app.erp.ast.dao.entity.ErpAstAsset;
import app.erp.ast.dao.entity.ErpAstAssetCategory;
import app.erp.ast.dao.entity.ErpAstDepreciationSchedule;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.notify.dao.entity.ErpSysNotification;
import io.nop.core.context.IServiceContext;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * G4 错误传播分级 Proof（plan 2026-07-30-0341-2 Phase 3 P1-MA4-013）：
 * 折旧过账失败时 DepreciationPostingDispatcher 派发 IErpSysNotificationBiz 告警（ast.depreciation-posting-failure）。
 *
 * <p>单元测试直接构造 dispatcher + Proxy 桩 notificationBiz，验证告警派发而非端到端 GL 故障（与 mfg
 * TestErpMfgVarianceAlert 的端到端范式互补——这里聚焦 catch-swallow→告警 闭环的 dispatchFailureAlert 路径）。
 */
public class TestDepreciationPostingFailureAlert {

    @SuppressWarnings("unchecked")
    private IErpSysNotificationBiz recordingNotify(String[] capturedEventType) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                if ("notify".equals(method.getName()) && args.length == 3) {
                    capturedEventType[0] = (String) args[0];
                }
                return java.util.Collections.emptyList();
            }
        };
        return (IErpSysNotificationBiz) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class[]{IErpSysNotificationBiz.class}, handler);
    }

    @Test
    public void testFailureDispatchesAlert() {
        DepreciationPostingDispatcher dispatcher = new DepreciationPostingDispatcher();
        String[] captured = new String[1];
        dispatcher.notificationBiz = recordingNotify(captured);

        ErpAstAsset asset = new ErpAstAsset();
        asset.setCode("AST-FAIL-001");
        ErpAstDepreciationSchedule schedule = new ErpAstDepreciationSchedule();
        schedule.setPeriod("2026-07");

        dispatcher.dispatchFailureAlert(asset, schedule, new RuntimeException("GL engine down"));

        assertEquals(DepreciationPostingDispatcher.NOTIFY_EVENT_DEPRECIATION_FAILURE, captured[0],
                "折旧过账失败应派发 ast.depreciation-posting-failure 告警");
    }

    @Test
    public void testNullNotificationBizSkipsGracefully() {
        DepreciationPostingDispatcher dispatcher = new DepreciationPostingDispatcher();
        // notificationBiz 为 null（单域无 notify-service）应静默跳过，不抛异常。
        ErpAstAsset asset = new ErpAstAsset();
        asset.setCode("AST-NULL-001");
        ErpAstDepreciationSchedule schedule = new ErpAstDepreciationSchedule();
        schedule.setPeriod("2026-07");
        dispatcher.dispatchFailureAlert(asset, schedule, new RuntimeException("down"));
        assertTrue(true, "null notificationBiz 不抛异常");
    }
}
