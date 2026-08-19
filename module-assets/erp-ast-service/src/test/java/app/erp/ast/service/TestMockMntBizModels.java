package app.erp.ast.service;

import app.erp.mnt.biz.IErpMntEquipmentBiz;
import app.erp.mnt.dao.entity.ErpMntEquipment;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

/**
 * ast 测试容器专用 mnt 设备 BizModel mock（RC-R1.77 资产处置→设备 DECOMMISSIONED 联动接线）。
 *
 * <p>不引入 mnt-service（其全量 beans 需跨域服务闭包），以 {@link CrudBizModel} 实现
 * {@link IErpMntEquipmentBiz}。ast 处置 Processor 仅消费 {@code changeStatusForAssetDisposal} /
 * {@code restoreFromAssetDisposal} 两个联动入口——mock 记录调用参数供断言；
 * {@code failLink} 旗标驱动「联动失败异常传播回滚处置」路径（UC-MAIN-08 L1 硬断言）。
 * 经 testBeansFile 显式加载（仅本 mock 测试类容器生效，其余 ast 测试容器无 mnt bean →
 * {@code @Nullable} 注入为 null 跳过联动，即 D1 容错路径的存量回归证明）。
 * 生产路径 app-erp-all 经 mnt-service 提供真实实现。
 */
public final class TestMockMntBizModels {

    private TestMockMntBizModels() {
    }

    @BizModel("ErpMntEquipment")
    public static class MockErpMntEquipmentBiz extends CrudBizModel<ErpMntEquipment>
            implements IErpMntEquipmentBiz {

        public volatile boolean failLink;
        public volatile Long lastDecommissionAssetId;
        public volatile String lastDecommissionCode;
        public volatile Long lastRestoreAssetId;
        public volatile String lastRestoreCode;

        public MockErpMntEquipmentBiz() {
            setEntityName(ErpMntEquipment.class.getName());
        }

        @Override
        public ErpMntEquipment changeStatus(Long equipmentId, String newStatus, IServiceContext context) {
            throw new UnsupportedOperationException("mock: changeStatus not consumed by ast tests");
        }

        @Override
        public int changeStatusForAssetDisposal(Long assetId, String disposalCode, IServiceContext context) {
            if (failLink) {
                throw new RuntimeException("simulated-mnt-disposal-linkage-failure");
            }
            lastDecommissionAssetId = assetId;
            lastDecommissionCode = disposalCode;
            return 1;
        }

        @Override
        public int restoreFromAssetDisposal(Long assetId, String disposalCode, IServiceContext context) {
            if (failLink) {
                throw new RuntimeException("simulated-mnt-disposal-linkage-failure");
            }
            lastRestoreAssetId = assetId;
            lastRestoreCode = disposalCode;
            return 1;
        }
    }
}
