package app.erp.fin.dao;

/**
 * 业财过账运行监控指标快照（{@code posting-log.md §运行监控指标 / §裁决3}）。由
 * {@code IErpFinPostingExceptionBiz.getRuntimeMetrics} 产出，呈现四指标当前/窗口值与阈值门控判定。
 *
 * <p>本类型位于 finance-dao（跨层契约面），供 BizModel 返回给调用方/前端。
 *
 * <p>指标定义与残留风险见 owner doc；时延为进程内窗口采样（重启清零），闭环成功率为代理值（SYNC 强一致假设）。
 */
public class ErpFinPostingMetricsSnapshot {

    private MetricValue autoPostingRate;
    private MetricValue latencyP99Millis;
    private MetricValue exceptionRate;
    private MetricValue loopbackSuccessRate;

    /** 聚合窗口（小时）。 */
    private int windowHours;
    /** 窗口内过账成功凭证数（分母基础）。 */
    private long voucherCount;
    /** 窗口内过账异常记录数。 */
    private long exceptionCount;
    /** 窗口内手工补录（resolution=MANUAL）异常数。 */
    private long manualResolutionCount;
    /** 时延内存采样样本数。 */
    private int latencySampleCount;
    /** 闭环成功率是否为代理值（true=SYNC 强一致假设，非源域实测）。 */
    private boolean loopbackProxyMode;

    public static class MetricValue {
        private double value;
        private double threshold;
        /** 是否达标（rate 类指标 value≥threshold 为达标；exceptionRate/latency 类 value<threshold 为达标）。 */
        private boolean healthy;
        /** 指标方向：higher_better（达标=value≥threshold）/ lower_better（达标=value<threshold）。 */
        private String direction;

        public MetricValue() {
        }

        public MetricValue(double value, double threshold, boolean healthy, String direction) {
            this.value = value;
            this.threshold = threshold;
            this.healthy = healthy;
            this.direction = direction;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public double getThreshold() {
            return threshold;
        }

        public void setThreshold(double threshold) {
            this.threshold = threshold;
        }

        public boolean isHealthy() {
            return healthy;
        }

        public void setHealthy(boolean healthy) {
            this.healthy = healthy;
        }

        public String getDirection() {
            return direction;
        }

        public void setDirection(String direction) {
            this.direction = direction;
        }
    }

    public MetricValue getAutoPostingRate() {
        return autoPostingRate;
    }

    public void setAutoPostingRate(MetricValue autoPostingRate) {
        this.autoPostingRate = autoPostingRate;
    }

    public MetricValue getLatencyP99Millis() {
        return latencyP99Millis;
    }

    public void setLatencyP99Millis(MetricValue latencyP99Millis) {
        this.latencyP99Millis = latencyP99Millis;
    }

    public MetricValue getExceptionRate() {
        return exceptionRate;
    }

    public void setExceptionRate(MetricValue exceptionRate) {
        this.exceptionRate = exceptionRate;
    }

    public MetricValue getLoopbackSuccessRate() {
        return loopbackSuccessRate;
    }

    public void setLoopbackSuccessRate(MetricValue loopbackSuccessRate) {
        this.loopbackSuccessRate = loopbackSuccessRate;
    }

    public int getWindowHours() {
        return windowHours;
    }

    public void setWindowHours(int windowHours) {
        this.windowHours = windowHours;
    }

    public long getVoucherCount() {
        return voucherCount;
    }

    public void setVoucherCount(long voucherCount) {
        this.voucherCount = voucherCount;
    }

    public long getExceptionCount() {
        return exceptionCount;
    }

    public void setExceptionCount(long exceptionCount) {
        this.exceptionCount = exceptionCount;
    }

    public long getManualResolutionCount() {
        return manualResolutionCount;
    }

    public void setManualResolutionCount(long manualResolutionCount) {
        this.manualResolutionCount = manualResolutionCount;
    }

    public int getLatencySampleCount() {
        return latencySampleCount;
    }

    public void setLatencySampleCount(int latencySampleCount) {
        this.latencySampleCount = latencySampleCount;
    }

    public boolean isLoopbackProxyMode() {
        return loopbackProxyMode;
    }

    public void setLoopbackProxyMode(boolean loopbackProxyMode) {
        this.loopbackProxyMode = loopbackProxyMode;
    }
}
