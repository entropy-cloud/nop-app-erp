package app.erp.aps.service.job;

import app.erp.aps.biz.IErpApsOperationOrderBiz;
import app.erp.aps.service.ErpApsConfigs;
import io.nop.api.core.config.AppConfig;
import io.nop.commons.util.StringHelper;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * WorkOrder 下达拉取扫描 Job Bean（RC-R1.86 / P1-RC-088，D1 裁决选项 B；R1.38 简单 job bean 范式）。
 *
 * <p>由 nop-job-local 的 {@code erp-aps-workorder-scan.job.yaml} 经 BeanMethodJobInvoker 反射调用
 * {@link #execute()}。双层门控：job enabled 默认 false + {@code erp-aps.workorder-scan-cron} 配置为空时跳过
 * （「不调度」语义，镜像 R1.4/R1.38 范式）。非空时扫描已下达工单批量创建 DRAFT OperationOrder。
 */
public class ErpApsWorkOrderScanJob {

    static final Logger LOG = LoggerFactory.getLogger(ErpApsWorkOrderScanJob.class);

    @Inject
    IErpApsOperationOrderBiz operationOrderBiz;

    public void setOperationOrderBiz(IErpApsOperationOrderBiz operationOrderBiz) {
        this.operationOrderBiz = operationOrderBiz;
    }

    /** 定时触发入口（无参方法，BeanMethodJobInvoker 反射调用）。cron 空值跳过。 */
    public void execute() {
        String cron = AppConfig.var(ErpApsConfigs.CONFIG_WORKORDER_SCAN_CRON,
                ErpApsConfigs.DEFAULT_WORKORDER_SCAN_CRON);
        if (StringHelper.isEmpty(cron)) {
            LOG.info("erp-aps-workorder-scan-skipped: cron config empty (erp-aps.workorder-scan-cron)");
            return;
        }
        IServiceContext ctx = new ServiceContextImpl();
        try {
            Integer created = operationOrderBiz.scanReleasedWorkOrders(ctx);
            LOG.info("erp-aps-workorder-scan-done: created={}", created);
        } catch (Exception e) {
            LOG.error("erp-aps-workorder-scan-failed", e);
        }
    }
}
