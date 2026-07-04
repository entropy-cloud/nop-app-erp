package app.erp.b2b.service.spi.transport;

import app.erp.b2b.dao.entity.ErpB2bMftConfig;
import app.erp.b2b.service.spi.transport.model.InboundFile;
import app.erp.b2b.service.spi.transport.model.TransportResult;

import java.util.List;

/**
 * MFT 传输适配器 SPI。每个传输协议（AS2/SFTP/FTPS/HTTP/HTTPS）实现本接口，
 * 封装与该协议的文件传输通信细节。
 *
 * <p>对应 {@code managed-file-transfer.md §协议支持}。注册为 Bean 后由
 * {@link ErpB2bMftTransportRegistry} 按 {@link #getSupportedProtocol()} 聚合。
 */
public interface IErpB2bTransportAdapter {

    /** 支持的协议标识（AS2/SFTP/FTPS/HTTP/HTTPS），对应 {@code ErpB2bMftConfig.protocol}。 */
    String getSupportedProtocol();

    /**
     * 发送文件（出站）。
     *
     * @param config  MFT 配置（含端点、证书引用等）
     * @param payload 报文内容
     * @param fileName 文件名
     * @return 传输结果（messageId/fileHash/success 等）
     */
    TransportResult send(ErpB2bMftConfig config, String payload, String fileName);

    /**
     * 拉取入站文件（SFTP 轮询备用路径）。
     *
     * @param config MFT 配置
     * @return 入站文件列表（webhook 为主路径，本期 Mock 返回空列表）
     */
    List<InboundFile> pullInbound(ErpB2bMftConfig config);
}
