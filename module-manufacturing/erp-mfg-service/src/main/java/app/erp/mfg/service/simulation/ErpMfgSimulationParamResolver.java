package app.erp.mfg.service.simulation;

import app.erp.mfg.dao.entity.ErpMfgMrpScenarioParam;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * MRP 仿真参数变体覆盖解析器实现（plan 2026-07-22-1000-2 §参数变体解析）。
 *
 * <p>进程内缓存按 scenarioId 隔离；按调用时按需加载（场景参数量小，不做全局预加载）。
 * 缓存不主动失效——场景版本一旦生成即不可变，参数变更须新建版本（Decision B 反模式 AP-06）。
 *
 * <p>解析键 = (materialId, paramType)；查询顺序：精确 materialId → 全局（materialId=null）→ null。
 */
public class ErpMfgSimulationParamResolver implements IErpMfgSimulationParamResolver {

    @Inject
    IDaoProvider daoProvider;

    private final Map<Long, List<ErpMfgMrpScenarioParam>> cache = new HashMap<>();

    public void setDaoProvider(IDaoProvider daoProvider) {
        this.daoProvider = daoProvider;
    }

    @Override
    public BigDecimal resolveOverride(Long scenarioId, Long materialId, String paramType) {
        if (scenarioId == null || paramType == null) {
            return null;
        }
        List<ErpMfgMrpScenarioParam> params = loadParams(scenarioId);

        // 1. 精确 materialId 匹配
        if (materialId != null) {
            for (ErpMfgMrpScenarioParam p : params) {
                if (Objects.equals(materialId, p.getMaterialId())
                        && Objects.equals(paramType, p.getParamType())) {
                    return p.getParamValue();
                }
            }
        }
        // 2. 全局覆盖（materialId=null）
        for (ErpMfgMrpScenarioParam p : params) {
            if (p.getMaterialId() == null && Objects.equals(paramType, p.getParamType())) {
                return p.getParamValue();
            }
        }
        // 3. 未覆盖
        return null;
    }

    @Override
    public List<ErpMfgMrpScenarioParam> loadParams(Long scenarioId) {
        if (scenarioId == null) {
            return java.util.Collections.emptyList();
        }
        synchronized (cache) {
            return cache.computeIfAbsent(scenarioId, this::doLoadParams);
        }
    }

    /** 清除缓存（测试用途；正常流程不调用）。 */
    public void invalidateCache() {
        synchronized (cache) {
            cache.clear();
        }
    }

    private List<ErpMfgMrpScenarioParam> doLoadParams(Long scenarioId) {
        IEntityDao<ErpMfgMrpScenarioParam> dao = daoProvider.daoFor(ErpMfgMrpScenarioParam.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scenarioId", scenarioId));
        return dao.findAllByQuery(q);
    }
}
