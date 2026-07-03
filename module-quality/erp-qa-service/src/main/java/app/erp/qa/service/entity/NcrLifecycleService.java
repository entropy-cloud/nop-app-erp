package app.erp.qa.service.entity;

import app.erp.qa.dao.entity.ErpQaInspection;
import app.erp.qa.dao.entity.ErpQaInspectionLine;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConfigs;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * NCR 生命周期服务：自动生成 + 5 态状态机 + resolve 门控（CAPA 全 COMPLETED + 效果验证）。
 *
 * <p>权威：{@code docs/design/quality/state-machine.md §适用对象二`} + 计划 Task Route Decision
 * （NCR resolve 门控 + NCR 自动生成）。
 *
 * <p>本类为非 BizModel 服务助手（对齐 KitAvailabilityChecker 范式），直接用 {@link IDaoProvider}
 * 操作 NCR/Action 实体。resolve 门控经 {@link #allActionsCompletedAndVerified(Long)} 校验。
 */
public class NcrLifecycleService {

    @Inject
    IDaoProvider daoProvider;

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    /**
     * 质检单 REJECTED 时自动生成 NCR（sourceType=INSPECTION / sourceCode=质检单号 /
     * status=OPEN / dispositionType 待 NCR 评审裁决）。配置 {@code erp-qua.auto-create-ncr-on-reject} 门控（默认 true）。
     */
    public void autoCreateNcrFromInspection(ErpQaInspection inspection, List<ErpQaInspectionLine> lines,
                                            IServiceContext context) {
        if (!ErpQaConfigs.isAutoCreateNcrOnReject()) {
            return;
        }
        IEntityDao<ErpQaNonConformance> dao = daoProvider.daoFor(ErpQaNonConformance.class);
        ErpQaNonConformance ncr = dao.newEntity();
        ncr.setCode("NCR-" + inspection.getCode());
        ncr.setNcrDate(LocalDate.now());
        ncr.setSourceType(ErpQaConstants.NCR_SOURCE_TYPE_INSPECTION);
        ncr.setSourceCode(inspection.getCode());
        ncr.setInspectionId(inspection.getId());
        ncr.setMaterialId(inspection.getMaterialId());
        ncr.setQuantity(resolveRejectQuantity(inspection, lines));
        // severity 字段 mandatory：默认 NORMAL(20)，评审时再改；dispositionType 留待评审裁决（非 mandatory）
        ncr.orm_propValueByName("severity", 20);
        ncr.setStatus(ErpQaConstants.NCR_STATUS_OPEN);
        ncr.setDescription(buildRejectDescription(lines));
        dao.saveEntity(ncr);
    }

    private BigDecimal resolveRejectQuantity(ErpQaInspection inspection, List<ErpQaInspectionLine> lines) {
        if (inspection.getLotQuantity() != null) {
            return inspection.getLotQuantity();
        }
        if (inspection.getSampleQuantity() != null) {
            return inspection.getSampleQuantity();
        }
        return BigDecimal.ONE;
    }

    private String buildRejectDescription(List<ErpQaInspectionLine> lines) {
        if (lines == null || lines.isEmpty()) {
            return "质检不合格";
        }
        StringBuilder sb = new StringBuilder("不合格项: ");
        boolean first = true;
        for (ErpQaInspectionLine line : lines) {
            if (line.getResult() != null && line.getResult() == ErpQaConstants.INSPECTION_RESULT_REJECTED) {
                if (!first) {
                    sb.append("; ");
                }
                sb.append(line.getParameterName()).append("=").append(line.getMeasuredValue());
                first = false;
            }
        }
        return first ? "质检不合格" : sb.toString();
    }

    boolean allActionsCompletedAndVerified(Long ncrId) {
        List<ErpQaActionImpl> actions = loadActions(ncrId);
        if (actions.isEmpty()) {
            // 无 CAPA 措施：允许 resolve（NCR 可无措施直接关闭，如误开评审后作废场景由 cancel 走）
            // 但 resolve 语义要求 CAPA 闭环——若确有不合格应有措施。这里允许无措施 resolve，
            // 由评审人保证；有措施时必须全完成+验证。
            return true;
        }
        for (ErpQaActionImpl a : actions) {
            if (a.status == null || a.status != ErpQaConstants.ACTION_STATUS_COMPLETED) {
                return false;
            }
            if (a.verificationPerson == null || a.verificationDate == null) {
                return false;
            }
        }
        return true;
    }

    private List<ErpQaActionImpl> loadActions(Long ncrId) {
        IEntityDao<app.erp.qa.dao.entity.ErpQaAction> dao = daoProvider.daoFor(app.erp.qa.dao.entity.ErpQaAction.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("ncrId", ncrId));
        List<app.erp.qa.dao.entity.ErpQaAction> list = dao.findAllByQuery(q);
        java.util.List<ErpQaActionImpl> result = new java.util.ArrayList<>();
        for (app.erp.qa.dao.entity.ErpQaAction a : list) {
            result.add(new ErpQaActionImpl(a.getId(), a.getStatus(),
                    a.orm_propValueByName("verificationPerson") == null ? null
                            : ((Number) a.orm_propValueByName("verificationPerson")).longValue(),
                    a.getVerificationDate()));
        }
        return result;
    }

    /** 校验 NCR resolve 门控失败时抛异常。 */
    public void requireResolveGate(Long ncrId, String ncrCode) {
        if (!allActionsCompletedAndVerified(ncrId)) {
            throw new NopException(ErpQaErrors.ERR_NCR_RESOLVE_CAPA_NOT_COMPLETED)
                    .param(ErpQaErrors.ARG_NCR_CODE, ncrCode);
        }
    }

    private static final class ErpQaActionImpl {
        final Long id;
        final Integer status;
        final Long verificationPerson;
        final LocalDate verificationDate;

        ErpQaActionImpl(Long id, Integer status, Long verificationPerson, LocalDate verificationDate) {
            this.id = id;
            this.status = status;
            this.verificationPerson = verificationPerson;
            this.verificationDate = verificationDate;
        }
    }
}
