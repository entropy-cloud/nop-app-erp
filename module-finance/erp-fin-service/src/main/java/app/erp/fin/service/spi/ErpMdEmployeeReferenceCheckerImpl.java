package app.erp.fin.service.spi;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.md.spi.IErpMdEmployeeReferenceChecker;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ne;

/**
 * 职员业务单据引用计数 SPI 生产实现（plan 2026-07-23-1145-2 Phase 1）。
 *
 * <p>{@code IErpMdEmployeeReferenceChecker} 端口声明在 master-data（基础域不可反向依赖下游域），
 * 本实现落在 finance-service（finance → master-data 为合法 DAG 边）：统计员工借款单
 * ({@link ErpFinEmployeeAdvance}) 对该职员的引用，仅计未取消的开放借款单。
 *
 * <p>注册为 finance bean 后，在 app-erp-all 聚合运行时被 master-data 的
 * {@code ErpPartyBizModel.employeeReferenceChecker} 经 {@code @Nullable @Inject} 收集，
 * 使 {@code ErpParty__findReferences(EMPLOYEE)} 返回真实数据。master-data 单域测试
 * （finance 不在 classpath）时返回空 Map，符合 SPI 默认行为。
 *
 * <p>其余下游域（assets ErpAstAsset.employeeId / maintenance ErpMntMaintenanceTeamMember.employeeId）
 * 的引用计数按 successor 触发条件逐域补齐（{@code IErpMdEmployeeReferenceChecker} 单实例注入，
 * 多域聚合需引入 List 收集器，归 Deferred）。
 */
public class ErpMdEmployeeReferenceCheckerImpl implements IErpMdEmployeeReferenceChecker {

    @Inject
    IDaoProvider daoProvider;

    @Override
    public Map<String, Long> countReferences(Long employeeId) {
        Map<String, Long> refs = new LinkedHashMap<>();
        if (employeeId == null) {
            return refs;
        }
        refs.put("employeeAdvance", countAdvances(employeeId));
        return refs;
    }

    private long countAdvances(Long employeeId) {
        IEntityDao<ErpFinEmployeeAdvance> dao = daoProvider.daoFor(ErpFinEmployeeAdvance.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        // 仅计未取消的借款单（取消单不构成删除阻断）
        q.addFilter(ne("docStatus", "CANCELLED"));
        return dao.findAllByQuery(q).size();
    }
}
