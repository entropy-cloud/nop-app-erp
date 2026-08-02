package app.erp.common.test;

import app.erp.fin.biz.IErpFinVoucherBiz;
import app.erp.notify.biz.IErpSysNotificationBiz;

import io.nop.api.core.exceptions.NopException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Collections;

/**
 * 故障注入测试通用 harness（MQ Q4，设计文档 §5.1）。
 *
 * <p>封装两类桩机制（设计文档 §3.2 路径 A）：
 * <ol>
 *   <li><b>Proxy 桩 Facade 接口</b>——经 {@link Proxy} + {@link InvocationHandler}，
 *       在指定方法名抛异常或录制调用。覆盖 {@link IErpFinVoucherBiz}（过账入口）
 *       与 {@link IErpSysNotificationBiz}（告警闭环）。</li>
 *   <li><b>子类 override 具体类</b>——各域 {@code *PostingExecutor} 由调用方内联匿名子类 override
 *       {@code postEvent}/{@code reverse} 抛异常（每域 executor 类不同，无法在共享 harness 泛化；
 *       但可经 {@link #throwingVoucherBiz()} 注入 executor 的 {@code voucherBiz} field 达到同等效果）。</li>
 * </ol>
 *
 * <p>无 Mockito（对齐 R1.16 范式）。全部桩为测试内局部实例，不全局替换 IoC bean / 不改全局静态状态
 *（设计文档 §6 验收 4，与 Q6 thread-local clock 协同）。
 *
 * <p><b>field 注入可见性（设计文档 §3.5 R1）</b>：各域 dispatcher / executor 的
 * {@code @Inject} field 均为 package-private（Nop IoC 规则禁止 private {@code @Inject}）。
 * 测试须置于 dispatcher 同包，经直接 field 赋值注入桩（如 {@code dispatcher.notificationBiz = stub}）。
 * 仅 {@code NcrPostingDispatcher} / {@code MaintenanceLaborPostingDispatcher} /
 * {@code MaintenanceIssuePostingDispatcher} 暴露 public setter，其余经 package-private field。
 */
public final class FaultInjectionStubs {

    private FaultInjectionStubs() {
    }

    /**
     * Proxy 桩 {@link IErpFinVoucherBiz}：{@code post} 方法抛 {@link NopException} 模拟财务过账引擎宕机
     *（G1/G2 故障），其它方法返回 primitive-safe 默认值。
     */
    public static IErpFinVoucherBiz throwingVoucherBiz() {
        return throwingVoucherBiz("post", testFault("test.voucher-posting-engine-down"));
    }

    /**
     * Proxy 桩 {@link IErpFinVoucherBiz}：指定方法名抛给定异常，其它方法返回默认值。
     *
     * @param methodName 触发异常的方法名（如 {@code "post"} / {@code "reverse"}）
     * @param toThrow    方法被调用时抛出的异常
     */
    public static IErpFinVoucherBiz throwingVoucherBiz(String methodName, RuntimeException toThrow) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (methodName.equals(method.getName())) {
                throw toThrow;
            }
            return defaultReturn(method.getReturnType());
        };
        return (IErpFinVoucherBiz) Proxy.newProxyInstance(
                IErpFinVoucherBiz.class.getClassLoader(),
                new Class[]{IErpFinVoucherBiz.class}, handler);
    }

    /**
     * Proxy 桩 {@link IErpSysNotificationBiz}：录制 {@code notify} 调用的 eventType 到
     * {@code capturedEventType[0]}（G4 告警闭环断言），其它方法返回默认值。
     *
     * @param capturedEventType 单元素 String 数组，运行后 {@code [0]} = 实际派发的 eventType
     */
    public static IErpSysNotificationBiz recordingNotificationBiz(String[] capturedEventType) {
        InvocationHandler handler = (proxy, method, args) -> {
            if ("notify".equals(method.getName()) && args != null && args.length >= 1) {
                capturedEventType[0] = (String) args[0];
                return Collections.emptyList();
            }
            return defaultReturn(method.getReturnType());
        };
        return (IErpSysNotificationBiz) Proxy.newProxyInstance(
                IErpSysNotificationBiz.class.getClassLoader(),
                new Class[]{IErpSysNotificationBiz.class}, handler);
    }

    /**
     * 通用 Proxy 桩：任意接口的指定方法名抛异常，其余方法返回默认值。
     */
    @SuppressWarnings("unchecked")
    public static <T> T throwingProxy(Class<T> iface, String methodName, RuntimeException toThrow) {
        InvocationHandler handler = (proxy, method, args) -> {
            if (methodName.equals(method.getName())) {
                throw toThrow;
            }
            return defaultReturn(method.getReturnType());
        };
        return (T) Proxy.newProxyInstance(iface.getClassLoader(), new Class[]{iface}, handler);
    }

    /**
     * primitive-safe 默认返回值（对齐既有先例 {@code TestErpInvPostingDispatcherFailureHangs.defaultReturn}）。
     * Proxy 桩的非目标方法（如 {@code ICrudBiz} 继承方法）须返回类型兼容的默认值，否则反射调用抛 NPE。
     */
    public static Object defaultReturn(Class<?> type) {
        if (type == void.class || !type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }

    /**
     * 构造测试用 {@link NopException}（对齐既有先例范式 {@code new NopException(code, null, true, true)}）。
     */
    public static NopException testFault(String errorCode) {
        return new NopException(errorCode, null, true, true);
    }
}
