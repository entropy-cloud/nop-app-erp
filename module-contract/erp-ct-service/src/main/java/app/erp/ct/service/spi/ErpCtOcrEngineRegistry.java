package app.erp.ct.service.spi;

import app.erp.ct.service.ErpCtErrors;
import io.nop.api.core.exceptions.NopException;
import jakarta.annotation.PostConstruct;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OCR 引擎注册中心。启动时收集所有 {@link IErpCtOcrEngine} Bean，
 * 按 {@link IErpCtOcrEngine#getEngineCode()} 建立 engineCode→Engine 查找表，
 * {@link #getEngine(String)} 按编码派发，未注册抛
 * {@link ErpCtErrors#ERR_CT_OCR_ENGINE_NOT_REGISTERED}。
 *
 * <p>注入范式镜像 {@code ErpCtSignatureProviderRegistry}（List 注入 + 内部建图，
 * collect-beans 注册于 app-service.beans.xml）；引擎选型经 config
 * {@code erp-ct.ocr-engine}（默认 manual）。
 */
public class ErpCtOcrEngineRegistry {

    private List<IErpCtOcrEngine> engines = Collections.emptyList();
    private final Map<String, IErpCtOcrEngine> engineMap = new LinkedHashMap<>();

    public void setEngines(List<IErpCtOcrEngine> engines) {
        this.engines = engines == null ? Collections.emptyList() : engines;
    }

    @PostConstruct
    public void init() {
        Map<String, IErpCtOcrEngine> map = new LinkedHashMap<>();
        for (IErpCtOcrEngine engine : engines) {
            IErpCtOcrEngine existing = map.put(engine.getEngineCode(), engine);
            if (existing != null && existing != engine) {
                throw new NopException(ErpCtErrors.ERR_CT_OCR_ENGINE_NOT_REGISTERED)
                        .param("engineCode", engine.getEngineCode());
            }
        }
        engineMap.clear();
        engineMap.putAll(map);
    }

    /**
     * 按 engineCode 派发。未注册抛 {@link ErpCtErrors#ERR_CT_OCR_ENGINE_NOT_REGISTERED}。
     */
    public IErpCtOcrEngine getEngine(String engineCode) {
        IErpCtOcrEngine engine = engineMap.get(engineCode);
        if (engine == null) {
            throw new NopException(ErpCtErrors.ERR_CT_OCR_ENGINE_NOT_REGISTERED)
                    .param("engineCode", engineCode);
        }
        return engine;
    }

    /** 暴露已注册的 engineCode 集合（测试/诊断用）。 */
    public List<String> getRegisteredEngineCodes() {
        return new ArrayList<>(engineMap.keySet());
    }
}
