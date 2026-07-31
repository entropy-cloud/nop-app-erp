package app.erp.qa.service.processor;

import app.erp.qa.biz.BatchOperationResult;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Collection;

/**
 * ErpQaInspection batchPassInspection per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含批量判定合格编排：逐行调 {@link ErpQaInspectionPassInspectionProcessor#passInspection}；
 * 行级失败记入 {@link BatchOperationResult#getFailures()}，不阻塞其他行。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpQaInspectionBatchPassInspectionProcessor {

    @Inject
    ErpQaInspectionPassInspectionProcessor passInspectionProcessor;

    public BatchOperationResult batchPassInspection(Collection<String> ids, IServiceContext context) {
        BatchOperationResult result = BatchOperationResult.forTotal(ids == null ? 0 : ids.size());
        if (ids == null || ids.isEmpty()) {
            return result;
        }
        for (String id : ids) {
            try {
                passInspectionProcessor.passInspection(Long.valueOf(id), context);
                result.recordSuccess();
            } catch (NopException e) {
                result.recordFailure(id, e.getErrorCode(), e.getDescription());
            } catch (NumberFormatException e) {
                result.recordFailure(id, "INVALID_ID", "非数字 ID：" + id);
            }
        }
        return result;
    }
}
