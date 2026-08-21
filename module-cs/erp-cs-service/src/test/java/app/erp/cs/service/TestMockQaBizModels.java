package app.erp.cs.service;

import app.erp.qa.biz.IErpQaNonConformanceBiz;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaRecall;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.beans.FieldSelectionBean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import java.util.List;
import java.util.Map;

/**
 * cs 测试容器专用 qa NCR BizModel mock（RC-R1.68 质量事件联动跨域创建 NCR）。
 *
 * <p>不引入 qa-service（其全量 beans 需跨域服务闭包），以 {@link CrudBizModel} 实现
 * {@link IErpQaNonConformanceBiz}。cs 侧仅消费 {@code save(data map)}（R1.31 save-map 先例）与
 * {@code findList} 只读反查——mock 覆写为 dao 直查 + dao 直存（绕过 BizObject action 派发：
 * 测试容器无 qa 生成的 xbiz/服务 bean；生产路径 app-erp-all 经 qa-service 走标准管道，
 * 不受本测试覆写影响）。{@code lastSaveData} 捕获 data map 供全字段断言；
 * {@code failSave} 旗标驱动「quality 域服务不可用」异常路径（UC-CS-06 异常条款）。
 * 经 test resources 的 app-test-mock-qa.beans.xml 自动合并（app- 前缀约定），仅测试容器生效。
 *
 * <p>bridge-test-117: qa 未迁移（M2.3），mock 桩保持 qa Long 实体/IBiz 签名（cs 侧适配消费，退役 owner M2.3）。
 */
public final class TestMockQaBizModels {

    private TestMockQaBizModels() {
    }

    @BizModel("ErpQaNonConformance")
    public static class MockErpQaNonConformanceBiz extends CrudBizModel<ErpQaNonConformance>
            implements IErpQaNonConformanceBiz {

        public volatile boolean failSave;
        public volatile Map<String, Object> lastSaveData;

        public MockErpQaNonConformanceBiz() {
            setEntityName(ErpQaNonConformance.class.getName());
        }

        @Override
        public ErpQaNonConformance save(Map<String, Object> data, IServiceContext context) {
            if (failSave) {
                throw new RuntimeException("simulated-quality-service-unavailable");
            }
            lastSaveData = data;
            ErpQaNonConformance ncr = dao().newEntity();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                if (entry.getValue() != null) {
                    ncr.orm_propValueByName(entry.getKey(), entry.getValue());
                }
            }
            dao().saveEntity(ncr);
            return ncr;
        }

        @Override
        public List<ErpQaNonConformance> findList(QueryBean query, FieldSelectionBean selection,
                                                  IServiceContext context) {
            return dao().findAllByQuery(query);
        }

        // ---- 以下 NCR 状态机方法 cs 侧不消费（仅接口实现占位，调用即失败暴露误用） ----

        @Override
        public ErpQaNonConformance submitReview(Long ncrId, IServiceContext context) {
            throw new UnsupportedOperationException("mock: submitReview not consumed by cs tests");
        }

        @Override
        public ErpQaNonConformance resolve(Long ncrId, String resolution, String noCapaReason,
                                           IServiceContext context) {
            throw new UnsupportedOperationException("mock: resolve not consumed by cs tests");
        }

        @Override
        public ErpQaNonConformance escalateToRecall(Long ncrId, IServiceContext context) {
            throw new UnsupportedOperationException("mock: escalateToRecall not consumed by cs tests");
        }

        @Override
        public ErpQaRecall upgradeToRecall(Long ncrId, IServiceContext context) {
            throw new UnsupportedOperationException("mock: upgradeToRecall not consumed by cs tests");
        }

        @Override
        public ErpQaNonConformance cancel(Long ncrId, IServiceContext context) {
            throw new UnsupportedOperationException("mock: cancel not consumed by cs tests");
        }

        @Override
        public ErpQaNonConformance postNcr(Long ncrId, IServiceContext context) {
            throw new UnsupportedOperationException("mock: postNcr not consumed by cs tests");
        }

        @Override
        public ErpQaNonConformance reverseNcr(Long ncrId, IServiceContext context) {
            throw new UnsupportedOperationException("mock: reverseNcr not consumed by cs tests");
        }
    }
}
