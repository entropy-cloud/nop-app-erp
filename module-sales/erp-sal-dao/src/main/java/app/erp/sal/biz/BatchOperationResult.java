package app.erp.sal.biz;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量操作结果（F11 批量审批等场景）。
 *
 * <p>逐行执行策略（Phase 0 决策 = 模式 b）：每行独立 try-catch，行级失败不阻塞其他行；
 * 调用方据 {@link #failures} 列表展示失败明细（如部分行状态不满足迁移条件）。
 *
 * <p>本类为 sales 域内部 DTO，不进 api 模块。其他域（purchase/quality/aps）按域隔离各自维护同名 DTO。
 */
public class BatchOperationResult {

    private int totalCount;
    private int successCount;
    private int failedCount;
    private List<BatchItemFailure> failures = new ArrayList<>();

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(int successCount) {
        this.successCount = successCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(int failedCount) {
        this.failedCount = failedCount;
    }

    public List<BatchItemFailure> getFailures() {
        return failures;
    }

    public void setFailures(List<BatchItemFailure> failures) {
        this.failures = failures == null ? new ArrayList<>() : failures;
    }

    public void recordSuccess() {
        successCount += 1;
    }

    public void recordFailure(String id, String code, String message) {
        failures.add(new BatchItemFailure(id, code, message));
        failedCount += 1;
    }

    public static BatchOperationResult forTotal(int total) {
        BatchOperationResult r = new BatchOperationResult();
        r.setTotalCount(total);
        return r;
    }

    public static class BatchItemFailure {
        private String id;
        private String code;
        private String message;

        public BatchItemFailure() {
        }

        public BatchItemFailure(String id, String code, String message) {
            this.id = id;
            this.code = code;
            this.message = message;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
