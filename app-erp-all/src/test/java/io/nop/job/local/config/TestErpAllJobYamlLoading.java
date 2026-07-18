package io.nop.job.local.config;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.core.lang.json.JsonTool;
import io.nop.core.resource.IResource;
import io.nop.core.resource.VirtualFileSystem;
import io.nop.job.api.config.LocalJobConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 运行时验证（plan 2026-07-18-1600-1 Phase 5）：确认 {@code /nop/job/conf/} 下全部 19 个
 * {@code .job.yaml} 文件经 VFS 可见并按 {@link LocalJobConfigLoader} 相同的解析路径
 * （{@link JsonTool#loadDeltaBeanFromResource}）成功反序列化为 {@link LocalJobConfig}。
 */
@NopTestConfig(initDatabaseSchema = OptionalBoolean.TRUE)
public class TestErpAllJobYamlLoading extends JunitBaseTestCase {

    @Test
    public void testAllJobYamlFilesLoad() {
        Collection<? extends IResource> resources = VirtualFileSystem.instance()
                .getAllResources("/nop/job/conf", ".job.yaml");
        assertEquals(19, resources.size(),
                "VFS 应在 /nop/job/conf 下看到 19 个 .job.yaml（实际: " + resources.size() + "）");

        List<LocalJobConfig> configs = new ArrayList<>();
        for (IResource resource : resources) {
            assertTrue(resource.exists(), "资源应存在: " + resource.getStdPath());
            try {
                LocalJobConfig jc = JsonTool.loadDeltaBeanFromResource(resource, LocalJobConfig.class);
                configs.add(jc);
            } catch (Exception e) {
                throw new AssertionError("解析 .job.yaml 失败: " + resource.getStdPath(), e);
            }
        }

        assertEquals(19, configs.size(),
                "全部 19 个 .job.yaml 应被解析为 LocalJobConfig（实际: " + configs.size() + "）");
        for (LocalJobConfig c : configs) {
            assertTrue(c.getJobName() != null && !c.getJobName().isEmpty(),
                    "每个 job.yaml 必须有 jobName");
            assertTrue(c.getTrigger() != null && c.getTrigger().getCronExpr() != null,
                    "每个 job.yaml 必须有 trigger.cronExpr: " + c.getJobName());
            assertTrue(c.getInvoker() != null && c.getInvoker().getBean() != null,
                    "每个 job.yaml 必须有 invoker.bean: " + c.getJobName());
        }
    }
}
