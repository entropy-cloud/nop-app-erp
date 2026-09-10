package app.erp.aps.service.job;

import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.service.ErpApsConfigs;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * 自动派工扫描 Job Bean（RC-R1.88 / P1-RC-090，UC-APS-07；R1.38 简单 job bean 范式）。
 *
 * <p>由 nop-job-local 的 {@code erp-aps-auto-dispatch.job.yaml} 经 BeanMethodJobInvoker 反射调用
 * {@link #execute()}。双层门控：job enabled 默认 false + 全局开关 {@code erp-aps.auto-dispatch-enabled}
 * （默认 false，auto-dispatch.md §5.2 紧急一键停用）+ {@code erp-aps.auto-dispatch-cron} 空值跳过。
 *
 * <p>扫描经 {@code runInSession} 包裹（对齐 ErpHrLeaveApproverTimeoutJob 范式）：biz 代理直调无请求级
 * ORM session，派工的实体更新须在打开的 session 内完成。
 */
public class ErpApsAutoDispatchJob {

    static final Logger LOG = LoggerFactory.getLogger(ErpApsAutoDispatchJob.class);

    @Inject
    IErpApsOperationOrderBiz operationOrderBiz;

    @Inject
    IOrmTemplate ormTemplate;

    public void setOperationOrderBiz(IErpApsOperationOrderBiz operationOrderBiz) {
        this.operationOrderBiz = operationOrderBiz;
    }

    public void setOrmTemplate(IOrmTemplate ormTemplate) {
        this.ormTemplate = ormTemplate;
    }

    /** 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。全局开关 + cron 空值跳过。 */
    public void execute() {
        if (!AppConfig.var(ErpApsConfigs.CONFIG_AUTO_DISPATCH_ENABLED,
                ErpApsConfigs.DEFAULT_AUTO_DISPATCH_ENABLED)) {
            LOG.info("erp-aps-auto-dispatch-skipped: global switch off (erp-aps.auto-dispatch-enabled)");
            return;
        }
        String cron = AppConfig.var(ErpApsConfigs.CONFIG_AUTO_DISPATCH_CRON,
                ErpApsConfigs.DEFAULT_AUTO_DISPATCH_CRON);
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-aps-auto-dispatch-skipped: cron config empty (erp-aps.auto-dispatch-cron)");
            return;
        }
        IServiceContext ctx = serviceContext();
        try {
            Integer dispatched = ormTemplate.runInSession(session -> operationOrderBiz.scanAutoDispatch(ctx));
            LOG.info("erp-aps-auto-dispatch-done: dispatched={}", dispatched);
        } catch (Exception e) {
            LOG.error("erp-aps-auto-dispatch-failed", e);
        }
    }

    /** 当前服务上下文；无绑定（job 入口/直接 Java 调用）时兜底新建——M2.8 分片③ common-015-r3 族回填，
     * 镜像 ExpenseCostAggregator 兜底范式：优先继承调用方绑定上下文（身份/数据权限），仅无绑定时构造新上下文。 */
    private static IServiceContext serviceContext() {
        IServiceContext context = IServiceContext.getCtx();
        return context != null ? context : new ServiceContextImpl();
    }
}
