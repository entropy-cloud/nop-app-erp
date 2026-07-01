package app.erp.b2b.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.b2b.dao.entity.ErpB2bMftConfig;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  MFT 配置: erp_b2b_mft_config
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bMftConfig extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 合作伙伴: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 3;
    
    /* 传输协议: PROTOCOL VARCHAR */
    public static final String PROP_NAME_protocol = "protocol";
    public static final int PROP_ID_protocol = 4;
    
    /* 传输端点: TRANSPORT_ENDPOINT VARCHAR */
    public static final String PROP_NAME_transportEndpoint = "transportEndpoint";
    public static final int PROP_ID_transportEndpoint = 5;
    
    /* 本地AS2 ID: LOCAL_AS2_ID VARCHAR */
    public static final String PROP_NAME_localAs2Id = "localAs2Id";
    public static final int PROP_ID_localAs2Id = 6;
    
    /* 对方AS2 ID: REMOTE_AS2_ID VARCHAR */
    public static final String PROP_NAME_remoteAs2Id = "remoteAs2Id";
    public static final int PROP_ID_remoteAs2Id = 7;
    
    /* SFTP用户名: SFTP_USERNAME VARCHAR */
    public static final String PROP_NAME_sftpUsername = "sftpUsername";
    public static final int PROP_ID_sftpUsername = 8;
    
    /* SFTP端口: SFTP_PORT INTEGER */
    public static final String PROP_NAME_sftpPort = "sftpPort";
    public static final int PROP_ID_sftpPort = 9;
    
    /* FTPS端口: FTPS_PORT INTEGER */
    public static final String PROP_NAME_ftpsPort = "ftpsPort";
    public static final int PROP_ID_ftpsPort = 10;
    
    /* 隐式TLS: FTPS_IMPLICIT_TLS BOOLEAN */
    public static final String PROP_NAME_ftpsImplicitTls = "ftpsImplicitTls";
    public static final int PROP_ID_ftpsImplicitTls = 11;
    
    /* 启用压缩: COMPRESSION BOOLEAN */
    public static final String PROP_NAME_compression = "compression";
    public static final int PROP_ID_compression = 12;
    
    /* 启用加密: ENCRYPTION BOOLEAN */
    public static final String PROP_NAME_encryption = "encryption";
    public static final int PROP_ID_encryption = 13;
    
    /* 加密算法: ENCRYPTION_ALGO VARCHAR */
    public static final String PROP_NAME_encryptionAlgo = "encryptionAlgo";
    public static final int PROP_ID_encryptionAlgo = 14;
    
    /* 启用签名: SIGNATURE BOOLEAN */
    public static final String PROP_NAME_signature = "signature";
    public static final int PROP_ID_signature = 15;
    
    /* 签名算法: SIGNATURE_ALGO VARCHAR */
    public static final String PROP_NAME_signatureAlgo = "signatureAlgo";
    public static final int PROP_ID_signatureAlgo = 16;
    
    /* 关联证书: CERT_ID BIGINT */
    public static final String PROP_NAME_certId = "certId";
    public static final int PROP_ID_certId = 17;
    
    /* 启用: ACTIVE BOOLEAN */
    public static final String PROP_NAME_active = "active";
    public static final int PROP_ID_active = 18;
    
    /* 最大重试次数: MAX_RETRIES INTEGER */
    public static final String PROP_NAME_maxRetries = "maxRetries";
    public static final int PROP_ID_maxRetries = 19;
    
    /* 重试间隔(分钟): RETRY_INTERVAL_MIN INTEGER */
    public static final String PROP_NAME_retryIntervalMin = "retryIntervalMin";
    public static final int PROP_ID_retryIntervalMin = 20;
    
    /* 启用死信队列: DEAD_LETTER_ENABLED BOOLEAN */
    public static final String PROP_NAME_deadLetterEnabled = "deadLetterEnabled";
    public static final int PROP_ID_deadLetterEnabled = 21;
    
    /* 监控目录: MONITOR_DIRECTORY VARCHAR */
    public static final String PROP_NAME_monitorDirectory = "monitorDirectory";
    public static final int PROP_ID_monitorDirectory = 22;
    
    /* 监控间隔(秒): MONITOR_INTERVAL_SEC INTEGER */
    public static final String PROP_NAME_monitorIntervalSec = "monitorIntervalSec";
    public static final int PROP_ID_monitorIntervalSec = 23;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 24;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 25;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 26;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 27;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 28;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 29;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 30;
    

    private static int _PROP_ID_BOUND = 31;

    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_cert = "cert";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[31];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_protocol] = PROP_NAME_protocol;
          PROP_NAME_TO_ID.put(PROP_NAME_protocol, PROP_ID_protocol);
      
          PROP_ID_TO_NAME[PROP_ID_transportEndpoint] = PROP_NAME_transportEndpoint;
          PROP_NAME_TO_ID.put(PROP_NAME_transportEndpoint, PROP_ID_transportEndpoint);
      
          PROP_ID_TO_NAME[PROP_ID_localAs2Id] = PROP_NAME_localAs2Id;
          PROP_NAME_TO_ID.put(PROP_NAME_localAs2Id, PROP_ID_localAs2Id);
      
          PROP_ID_TO_NAME[PROP_ID_remoteAs2Id] = PROP_NAME_remoteAs2Id;
          PROP_NAME_TO_ID.put(PROP_NAME_remoteAs2Id, PROP_ID_remoteAs2Id);
      
          PROP_ID_TO_NAME[PROP_ID_sftpUsername] = PROP_NAME_sftpUsername;
          PROP_NAME_TO_ID.put(PROP_NAME_sftpUsername, PROP_ID_sftpUsername);
      
          PROP_ID_TO_NAME[PROP_ID_sftpPort] = PROP_NAME_sftpPort;
          PROP_NAME_TO_ID.put(PROP_NAME_sftpPort, PROP_ID_sftpPort);
      
          PROP_ID_TO_NAME[PROP_ID_ftpsPort] = PROP_NAME_ftpsPort;
          PROP_NAME_TO_ID.put(PROP_NAME_ftpsPort, PROP_ID_ftpsPort);
      
          PROP_ID_TO_NAME[PROP_ID_ftpsImplicitTls] = PROP_NAME_ftpsImplicitTls;
          PROP_NAME_TO_ID.put(PROP_NAME_ftpsImplicitTls, PROP_ID_ftpsImplicitTls);
      
          PROP_ID_TO_NAME[PROP_ID_compression] = PROP_NAME_compression;
          PROP_NAME_TO_ID.put(PROP_NAME_compression, PROP_ID_compression);
      
          PROP_ID_TO_NAME[PROP_ID_encryption] = PROP_NAME_encryption;
          PROP_NAME_TO_ID.put(PROP_NAME_encryption, PROP_ID_encryption);
      
          PROP_ID_TO_NAME[PROP_ID_encryptionAlgo] = PROP_NAME_encryptionAlgo;
          PROP_NAME_TO_ID.put(PROP_NAME_encryptionAlgo, PROP_ID_encryptionAlgo);
      
          PROP_ID_TO_NAME[PROP_ID_signature] = PROP_NAME_signature;
          PROP_NAME_TO_ID.put(PROP_NAME_signature, PROP_ID_signature);
      
          PROP_ID_TO_NAME[PROP_ID_signatureAlgo] = PROP_NAME_signatureAlgo;
          PROP_NAME_TO_ID.put(PROP_NAME_signatureAlgo, PROP_ID_signatureAlgo);
      
          PROP_ID_TO_NAME[PROP_ID_certId] = PROP_NAME_certId;
          PROP_NAME_TO_ID.put(PROP_NAME_certId, PROP_ID_certId);
      
          PROP_ID_TO_NAME[PROP_ID_active] = PROP_NAME_active;
          PROP_NAME_TO_ID.put(PROP_NAME_active, PROP_ID_active);
      
          PROP_ID_TO_NAME[PROP_ID_maxRetries] = PROP_NAME_maxRetries;
          PROP_NAME_TO_ID.put(PROP_NAME_maxRetries, PROP_ID_maxRetries);
      
          PROP_ID_TO_NAME[PROP_ID_retryIntervalMin] = PROP_NAME_retryIntervalMin;
          PROP_NAME_TO_ID.put(PROP_NAME_retryIntervalMin, PROP_ID_retryIntervalMin);
      
          PROP_ID_TO_NAME[PROP_ID_deadLetterEnabled] = PROP_NAME_deadLetterEnabled;
          PROP_NAME_TO_ID.put(PROP_NAME_deadLetterEnabled, PROP_ID_deadLetterEnabled);
      
          PROP_ID_TO_NAME[PROP_ID_monitorDirectory] = PROP_NAME_monitorDirectory;
          PROP_NAME_TO_ID.put(PROP_NAME_monitorDirectory, PROP_ID_monitorDirectory);
      
          PROP_ID_TO_NAME[PROP_ID_monitorIntervalSec] = PROP_NAME_monitorIntervalSec;
          PROP_NAME_TO_ID.put(PROP_NAME_monitorIntervalSec, PROP_ID_monitorIntervalSec);
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
          PROP_ID_TO_NAME[PROP_ID_delVersion] = PROP_NAME_delVersion;
          PROP_NAME_TO_ID.put(PROP_NAME_delVersion, PROP_ID_delVersion);
      
          PROP_ID_TO_NAME[PROP_ID_version] = PROP_NAME_version;
          PROP_NAME_TO_ID.put(PROP_NAME_version, PROP_ID_version);
      
          PROP_ID_TO_NAME[PROP_ID_createdBy] = PROP_NAME_createdBy;
          PROP_NAME_TO_ID.put(PROP_NAME_createdBy, PROP_ID_createdBy);
      
          PROP_ID_TO_NAME[PROP_ID_createTime] = PROP_NAME_createTime;
          PROP_NAME_TO_ID.put(PROP_NAME_createTime, PROP_ID_createTime);
      
          PROP_ID_TO_NAME[PROP_ID_updatedBy] = PROP_NAME_updatedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_updatedBy, PROP_ID_updatedBy);
      
          PROP_ID_TO_NAME[PROP_ID_updateTime] = PROP_NAME_updateTime;
          PROP_NAME_TO_ID.put(PROP_NAME_updateTime, PROP_ID_updateTime);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 合作伙伴: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 传输协议: PROTOCOL */
    private java.lang.String _protocol;
    
    /* 传输端点: TRANSPORT_ENDPOINT */
    private java.lang.String _transportEndpoint;
    
    /* 本地AS2 ID: LOCAL_AS2_ID */
    private java.lang.String _localAs2Id;
    
    /* 对方AS2 ID: REMOTE_AS2_ID */
    private java.lang.String _remoteAs2Id;
    
    /* SFTP用户名: SFTP_USERNAME */
    private java.lang.String _sftpUsername;
    
    /* SFTP端口: SFTP_PORT */
    private java.lang.Integer _sftpPort;
    
    /* FTPS端口: FTPS_PORT */
    private java.lang.Integer _ftpsPort;
    
    /* 隐式TLS: FTPS_IMPLICIT_TLS */
    private java.lang.Boolean _ftpsImplicitTls;
    
    /* 启用压缩: COMPRESSION */
    private java.lang.Boolean _compression;
    
    /* 启用加密: ENCRYPTION */
    private java.lang.Boolean _encryption;
    
    /* 加密算法: ENCRYPTION_ALGO */
    private java.lang.String _encryptionAlgo;
    
    /* 启用签名: SIGNATURE */
    private java.lang.Boolean _signature;
    
    /* 签名算法: SIGNATURE_ALGO */
    private java.lang.String _signatureAlgo;
    
    /* 关联证书: CERT_ID */
    private java.lang.Long _certId;
    
    /* 启用: ACTIVE */
    private java.lang.Boolean _active;
    
    /* 最大重试次数: MAX_RETRIES */
    private java.lang.Integer _maxRetries;
    
    /* 重试间隔(分钟): RETRY_INTERVAL_MIN */
    private java.lang.Integer _retryIntervalMin;
    
    /* 启用死信队列: DEAD_LETTER_ENABLED */
    private java.lang.Boolean _deadLetterEnabled;
    
    /* 监控目录: MONITOR_DIRECTORY */
    private java.lang.String _monitorDirectory;
    
    /* 监控间隔(秒): MONITOR_INTERVAL_SEC */
    private java.lang.Integer _monitorIntervalSec;
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    
    /* 逻辑删除版本: DEL_VERSION */
    private java.lang.Long _delVersion;
    
    /* 数据版本: VERSION */
    private java.lang.Integer _version;
    
    /* 创建人: CREATED_BY */
    private java.lang.String _createdBy;
    
    /* 创建时间: CREATE_TIME */
    private java.sql.Timestamp _createTime;
    
    /* 修改人: UPDATED_BY */
    private java.lang.String _updatedBy;
    
    /* 修改时间: UPDATE_TIME */
    private java.sql.Timestamp _updateTime;
    

    public _ErpB2bMftConfig(){
        // for debug
    }

    protected ErpB2bMftConfig newInstance(){
        ErpB2bMftConfig entity = new ErpB2bMftConfig();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bMftConfig cloneInstance() {
        ErpB2bMftConfig entity = newInstance();
        orm_forEachInitedProp((value, propId) -> {
            entity.orm_propValue(propId,value);
        });
        return entity;
    }

    @Override
    public String orm_entityName() {
      // 如果存在实体模型对象，则以模型对象上的设置为准
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getName();
      return "app.erp.b2b.dao.entity.ErpB2bMftConfig";
    }

    @Override
    public int orm_propIdBound(){
      IEntityModel entityModel = orm_entityModel();
      if(entityModel != null)
          return entityModel.getPropIdBound();
      return _PROP_ID_BOUND;
    }

    @Override
    public Object orm_id() {
    
        return buildSimpleId(PROP_ID_id);
     
    }

    @Override
    public boolean orm_isPrimary(int propId) {
        
            return propId == PROP_ID_id;
          
    }

    @Override
    public String orm_propName(int propId) {
        if(propId >= PROP_ID_TO_NAME.length)
            return super.orm_propName(propId);
        String propName = PROP_ID_TO_NAME[propId];
        if(propName == null)
           return super.orm_propName(propId);
        return propName;
    }

    @Override
    public int orm_propId(String propName) {
        Integer propId = PROP_NAME_TO_ID.get(propName);
        if(propId == null)
            return super.orm_propId(propName);
        return propId;
    }

    @Override
    public Object orm_propValue(int propId) {
        switch(propId){
        
            case PROP_ID_id:
               return getId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_protocol:
               return getProtocol();
        
            case PROP_ID_transportEndpoint:
               return getTransportEndpoint();
        
            case PROP_ID_localAs2Id:
               return getLocalAs2Id();
        
            case PROP_ID_remoteAs2Id:
               return getRemoteAs2Id();
        
            case PROP_ID_sftpUsername:
               return getSftpUsername();
        
            case PROP_ID_sftpPort:
               return getSftpPort();
        
            case PROP_ID_ftpsPort:
               return getFtpsPort();
        
            case PROP_ID_ftpsImplicitTls:
               return getFtpsImplicitTls();
        
            case PROP_ID_compression:
               return getCompression();
        
            case PROP_ID_encryption:
               return getEncryption();
        
            case PROP_ID_encryptionAlgo:
               return getEncryptionAlgo();
        
            case PROP_ID_signature:
               return getSignature();
        
            case PROP_ID_signatureAlgo:
               return getSignatureAlgo();
        
            case PROP_ID_certId:
               return getCertId();
        
            case PROP_ID_active:
               return getActive();
        
            case PROP_ID_maxRetries:
               return getMaxRetries();
        
            case PROP_ID_retryIntervalMin:
               return getRetryIntervalMin();
        
            case PROP_ID_deadLetterEnabled:
               return getDeadLetterEnabled();
        
            case PROP_ID_monitorDirectory:
               return getMonitorDirectory();
        
            case PROP_ID_monitorIntervalSec:
               return getMonitorIntervalSec();
        
            case PROP_ID_remark:
               return getRemark();
        
            case PROP_ID_delVersion:
               return getDelVersion();
        
            case PROP_ID_version:
               return getVersion();
        
            case PROP_ID_createdBy:
               return getCreatedBy();
        
            case PROP_ID_createTime:
               return getCreateTime();
        
            case PROP_ID_updatedBy:
               return getUpdatedBy();
        
            case PROP_ID_updateTime:
               return getUpdateTime();
        
           default:
              return super.orm_propValue(propId);
        }
    }

    

    @Override
    public void orm_propValue(int propId, Object value){
        switch(propId){
        
            case PROP_ID_id:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_partnerId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_protocol:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_protocol));
               }
               setProtocol(typedValue);
               break;
            }
        
            case PROP_ID_transportEndpoint:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_transportEndpoint));
               }
               setTransportEndpoint(typedValue);
               break;
            }
        
            case PROP_ID_localAs2Id:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_localAs2Id));
               }
               setLocalAs2Id(typedValue);
               break;
            }
        
            case PROP_ID_remoteAs2Id:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remoteAs2Id));
               }
               setRemoteAs2Id(typedValue);
               break;
            }
        
            case PROP_ID_sftpUsername:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sftpUsername));
               }
               setSftpUsername(typedValue);
               break;
            }
        
            case PROP_ID_sftpPort:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sftpPort));
               }
               setSftpPort(typedValue);
               break;
            }
        
            case PROP_ID_ftpsPort:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_ftpsPort));
               }
               setFtpsPort(typedValue);
               break;
            }
        
            case PROP_ID_ftpsImplicitTls:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_ftpsImplicitTls));
               }
               setFtpsImplicitTls(typedValue);
               break;
            }
        
            case PROP_ID_compression:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_compression));
               }
               setCompression(typedValue);
               break;
            }
        
            case PROP_ID_encryption:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_encryption));
               }
               setEncryption(typedValue);
               break;
            }
        
            case PROP_ID_encryptionAlgo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_encryptionAlgo));
               }
               setEncryptionAlgo(typedValue);
               break;
            }
        
            case PROP_ID_signature:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_signature));
               }
               setSignature(typedValue);
               break;
            }
        
            case PROP_ID_signatureAlgo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_signatureAlgo));
               }
               setSignatureAlgo(typedValue);
               break;
            }
        
            case PROP_ID_certId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_certId));
               }
               setCertId(typedValue);
               break;
            }
        
            case PROP_ID_active:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_active));
               }
               setActive(typedValue);
               break;
            }
        
            case PROP_ID_maxRetries:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxRetries));
               }
               setMaxRetries(typedValue);
               break;
            }
        
            case PROP_ID_retryIntervalMin:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_retryIntervalMin));
               }
               setRetryIntervalMin(typedValue);
               break;
            }
        
            case PROP_ID_deadLetterEnabled:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_deadLetterEnabled));
               }
               setDeadLetterEnabled(typedValue);
               break;
            }
        
            case PROP_ID_monitorDirectory:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_monitorDirectory));
               }
               setMonitorDirectory(typedValue);
               break;
            }
        
            case PROP_ID_monitorIntervalSec:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_monitorIntervalSec));
               }
               setMonitorIntervalSec(typedValue);
               break;
            }
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
               break;
            }
        
            case PROP_ID_delVersion:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_delVersion));
               }
               setDelVersion(typedValue);
               break;
            }
        
            case PROP_ID_version:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_version));
               }
               setVersion(typedValue);
               break;
            }
        
            case PROP_ID_createdBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_createdBy));
               }
               setCreatedBy(typedValue);
               break;
            }
        
            case PROP_ID_createTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_createTime));
               }
               setCreateTime(typedValue);
               break;
            }
        
            case PROP_ID_updatedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_updatedBy));
               }
               setUpdatedBy(typedValue);
               break;
            }
        
            case PROP_ID_updateTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_updateTime));
               }
               setUpdateTime(typedValue);
               break;
            }
        
           default:
              super.orm_propValue(propId,value);
        }
    }

    @Override
    public void orm_internalSet(int propId, Object value) {
        switch(propId){
        
            case PROP_ID_id:{
               onInitProp(propId);
               this._id = (java.lang.Long)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_protocol:{
               onInitProp(propId);
               this._protocol = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_transportEndpoint:{
               onInitProp(propId);
               this._transportEndpoint = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_localAs2Id:{
               onInitProp(propId);
               this._localAs2Id = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_remoteAs2Id:{
               onInitProp(propId);
               this._remoteAs2Id = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sftpUsername:{
               onInitProp(propId);
               this._sftpUsername = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sftpPort:{
               onInitProp(propId);
               this._sftpPort = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_ftpsPort:{
               onInitProp(propId);
               this._ftpsPort = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_ftpsImplicitTls:{
               onInitProp(propId);
               this._ftpsImplicitTls = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_compression:{
               onInitProp(propId);
               this._compression = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_encryption:{
               onInitProp(propId);
               this._encryption = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_encryptionAlgo:{
               onInitProp(propId);
               this._encryptionAlgo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_signature:{
               onInitProp(propId);
               this._signature = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_signatureAlgo:{
               onInitProp(propId);
               this._signatureAlgo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_certId:{
               onInitProp(propId);
               this._certId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_active:{
               onInitProp(propId);
               this._active = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_maxRetries:{
               onInitProp(propId);
               this._maxRetries = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_retryIntervalMin:{
               onInitProp(propId);
               this._retryIntervalMin = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_deadLetterEnabled:{
               onInitProp(propId);
               this._deadLetterEnabled = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_monitorDirectory:{
               onInitProp(propId);
               this._monitorDirectory = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_monitorIntervalSec:{
               onInitProp(propId);
               this._monitorIntervalSec = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_delVersion:{
               onInitProp(propId);
               this._delVersion = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_version:{
               onInitProp(propId);
               this._version = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_createdBy:{
               onInitProp(propId);
               this._createdBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_createTime:{
               onInitProp(propId);
               this._createTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_updatedBy:{
               onInitProp(propId);
               this._updatedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_updateTime:{
               onInitProp(propId);
               this._updateTime = (java.sql.Timestamp)value;
               
               break;
            }
        
           default:
              super.orm_internalSet(propId,value);
        }
    }

    
    /**
     * ID: ID
     */
    public final java.lang.Long getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.Long value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 业务组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 合作伙伴: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 合作伙伴: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 传输协议: PROTOCOL
     */
    public final java.lang.String getProtocol(){
         onPropGet(PROP_ID_protocol);
         return _protocol;
    }

    /**
     * 传输协议: PROTOCOL
     */
    public final void setProtocol(java.lang.String value){
        if(onPropSet(PROP_ID_protocol,value)){
            this._protocol = value;
            internalClearRefs(PROP_ID_protocol);
            
        }
    }
    
    /**
     * 传输端点: TRANSPORT_ENDPOINT
     */
    public final java.lang.String getTransportEndpoint(){
         onPropGet(PROP_ID_transportEndpoint);
         return _transportEndpoint;
    }

    /**
     * 传输端点: TRANSPORT_ENDPOINT
     */
    public final void setTransportEndpoint(java.lang.String value){
        if(onPropSet(PROP_ID_transportEndpoint,value)){
            this._transportEndpoint = value;
            internalClearRefs(PROP_ID_transportEndpoint);
            
        }
    }
    
    /**
     * 本地AS2 ID: LOCAL_AS2_ID
     */
    public final java.lang.String getLocalAs2Id(){
         onPropGet(PROP_ID_localAs2Id);
         return _localAs2Id;
    }

    /**
     * 本地AS2 ID: LOCAL_AS2_ID
     */
    public final void setLocalAs2Id(java.lang.String value){
        if(onPropSet(PROP_ID_localAs2Id,value)){
            this._localAs2Id = value;
            internalClearRefs(PROP_ID_localAs2Id);
            
        }
    }
    
    /**
     * 对方AS2 ID: REMOTE_AS2_ID
     */
    public final java.lang.String getRemoteAs2Id(){
         onPropGet(PROP_ID_remoteAs2Id);
         return _remoteAs2Id;
    }

    /**
     * 对方AS2 ID: REMOTE_AS2_ID
     */
    public final void setRemoteAs2Id(java.lang.String value){
        if(onPropSet(PROP_ID_remoteAs2Id,value)){
            this._remoteAs2Id = value;
            internalClearRefs(PROP_ID_remoteAs2Id);
            
        }
    }
    
    /**
     * SFTP用户名: SFTP_USERNAME
     */
    public final java.lang.String getSftpUsername(){
         onPropGet(PROP_ID_sftpUsername);
         return _sftpUsername;
    }

    /**
     * SFTP用户名: SFTP_USERNAME
     */
    public final void setSftpUsername(java.lang.String value){
        if(onPropSet(PROP_ID_sftpUsername,value)){
            this._sftpUsername = value;
            internalClearRefs(PROP_ID_sftpUsername);
            
        }
    }
    
    /**
     * SFTP端口: SFTP_PORT
     */
    public final java.lang.Integer getSftpPort(){
         onPropGet(PROP_ID_sftpPort);
         return _sftpPort;
    }

    /**
     * SFTP端口: SFTP_PORT
     */
    public final void setSftpPort(java.lang.Integer value){
        if(onPropSet(PROP_ID_sftpPort,value)){
            this._sftpPort = value;
            internalClearRefs(PROP_ID_sftpPort);
            
        }
    }
    
    /**
     * FTPS端口: FTPS_PORT
     */
    public final java.lang.Integer getFtpsPort(){
         onPropGet(PROP_ID_ftpsPort);
         return _ftpsPort;
    }

    /**
     * FTPS端口: FTPS_PORT
     */
    public final void setFtpsPort(java.lang.Integer value){
        if(onPropSet(PROP_ID_ftpsPort,value)){
            this._ftpsPort = value;
            internalClearRefs(PROP_ID_ftpsPort);
            
        }
    }
    
    /**
     * 隐式TLS: FTPS_IMPLICIT_TLS
     */
    public final java.lang.Boolean getFtpsImplicitTls(){
         onPropGet(PROP_ID_ftpsImplicitTls);
         return _ftpsImplicitTls;
    }

    /**
     * 隐式TLS: FTPS_IMPLICIT_TLS
     */
    public final void setFtpsImplicitTls(java.lang.Boolean value){
        if(onPropSet(PROP_ID_ftpsImplicitTls,value)){
            this._ftpsImplicitTls = value;
            internalClearRefs(PROP_ID_ftpsImplicitTls);
            
        }
    }
    
    /**
     * 启用压缩: COMPRESSION
     */
    public final java.lang.Boolean getCompression(){
         onPropGet(PROP_ID_compression);
         return _compression;
    }

    /**
     * 启用压缩: COMPRESSION
     */
    public final void setCompression(java.lang.Boolean value){
        if(onPropSet(PROP_ID_compression,value)){
            this._compression = value;
            internalClearRefs(PROP_ID_compression);
            
        }
    }
    
    /**
     * 启用加密: ENCRYPTION
     */
    public final java.lang.Boolean getEncryption(){
         onPropGet(PROP_ID_encryption);
         return _encryption;
    }

    /**
     * 启用加密: ENCRYPTION
     */
    public final void setEncryption(java.lang.Boolean value){
        if(onPropSet(PROP_ID_encryption,value)){
            this._encryption = value;
            internalClearRefs(PROP_ID_encryption);
            
        }
    }
    
    /**
     * 加密算法: ENCRYPTION_ALGO
     */
    public final java.lang.String getEncryptionAlgo(){
         onPropGet(PROP_ID_encryptionAlgo);
         return _encryptionAlgo;
    }

    /**
     * 加密算法: ENCRYPTION_ALGO
     */
    public final void setEncryptionAlgo(java.lang.String value){
        if(onPropSet(PROP_ID_encryptionAlgo,value)){
            this._encryptionAlgo = value;
            internalClearRefs(PROP_ID_encryptionAlgo);
            
        }
    }
    
    /**
     * 启用签名: SIGNATURE
     */
    public final java.lang.Boolean getSignature(){
         onPropGet(PROP_ID_signature);
         return _signature;
    }

    /**
     * 启用签名: SIGNATURE
     */
    public final void setSignature(java.lang.Boolean value){
        if(onPropSet(PROP_ID_signature,value)){
            this._signature = value;
            internalClearRefs(PROP_ID_signature);
            
        }
    }
    
    /**
     * 签名算法: SIGNATURE_ALGO
     */
    public final java.lang.String getSignatureAlgo(){
         onPropGet(PROP_ID_signatureAlgo);
         return _signatureAlgo;
    }

    /**
     * 签名算法: SIGNATURE_ALGO
     */
    public final void setSignatureAlgo(java.lang.String value){
        if(onPropSet(PROP_ID_signatureAlgo,value)){
            this._signatureAlgo = value;
            internalClearRefs(PROP_ID_signatureAlgo);
            
        }
    }
    
    /**
     * 关联证书: CERT_ID
     */
    public final java.lang.Long getCertId(){
         onPropGet(PROP_ID_certId);
         return _certId;
    }

    /**
     * 关联证书: CERT_ID
     */
    public final void setCertId(java.lang.Long value){
        if(onPropSet(PROP_ID_certId,value)){
            this._certId = value;
            internalClearRefs(PROP_ID_certId);
            
        }
    }
    
    /**
     * 启用: ACTIVE
     */
    public final java.lang.Boolean getActive(){
         onPropGet(PROP_ID_active);
         return _active;
    }

    /**
     * 启用: ACTIVE
     */
    public final void setActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_active,value)){
            this._active = value;
            internalClearRefs(PROP_ID_active);
            
        }
    }
    
    /**
     * 最大重试次数: MAX_RETRIES
     */
    public final java.lang.Integer getMaxRetries(){
         onPropGet(PROP_ID_maxRetries);
         return _maxRetries;
    }

    /**
     * 最大重试次数: MAX_RETRIES
     */
    public final void setMaxRetries(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxRetries,value)){
            this._maxRetries = value;
            internalClearRefs(PROP_ID_maxRetries);
            
        }
    }
    
    /**
     * 重试间隔(分钟): RETRY_INTERVAL_MIN
     */
    public final java.lang.Integer getRetryIntervalMin(){
         onPropGet(PROP_ID_retryIntervalMin);
         return _retryIntervalMin;
    }

    /**
     * 重试间隔(分钟): RETRY_INTERVAL_MIN
     */
    public final void setRetryIntervalMin(java.lang.Integer value){
        if(onPropSet(PROP_ID_retryIntervalMin,value)){
            this._retryIntervalMin = value;
            internalClearRefs(PROP_ID_retryIntervalMin);
            
        }
    }
    
    /**
     * 启用死信队列: DEAD_LETTER_ENABLED
     */
    public final java.lang.Boolean getDeadLetterEnabled(){
         onPropGet(PROP_ID_deadLetterEnabled);
         return _deadLetterEnabled;
    }

    /**
     * 启用死信队列: DEAD_LETTER_ENABLED
     */
    public final void setDeadLetterEnabled(java.lang.Boolean value){
        if(onPropSet(PROP_ID_deadLetterEnabled,value)){
            this._deadLetterEnabled = value;
            internalClearRefs(PROP_ID_deadLetterEnabled);
            
        }
    }
    
    /**
     * 监控目录: MONITOR_DIRECTORY
     */
    public final java.lang.String getMonitorDirectory(){
         onPropGet(PROP_ID_monitorDirectory);
         return _monitorDirectory;
    }

    /**
     * 监控目录: MONITOR_DIRECTORY
     */
    public final void setMonitorDirectory(java.lang.String value){
        if(onPropSet(PROP_ID_monitorDirectory,value)){
            this._monitorDirectory = value;
            internalClearRefs(PROP_ID_monitorDirectory);
            
        }
    }
    
    /**
     * 监控间隔(秒): MONITOR_INTERVAL_SEC
     */
    public final java.lang.Integer getMonitorIntervalSec(){
         onPropGet(PROP_ID_monitorIntervalSec);
         return _monitorIntervalSec;
    }

    /**
     * 监控间隔(秒): MONITOR_INTERVAL_SEC
     */
    public final void setMonitorIntervalSec(java.lang.Integer value){
        if(onPropSet(PROP_ID_monitorIntervalSec,value)){
            this._monitorIntervalSec = value;
            internalClearRefs(PROP_ID_monitorIntervalSec);
            
        }
    }
    
    /**
     * 备注: REMARK
     */
    public final java.lang.String getRemark(){
         onPropGet(PROP_ID_remark);
         return _remark;
    }

    /**
     * 备注: REMARK
     */
    public final void setRemark(java.lang.String value){
        if(onPropSet(PROP_ID_remark,value)){
            this._remark = value;
            internalClearRefs(PROP_ID_remark);
            
        }
    }
    
    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final java.lang.Long getDelVersion(){
         onPropGet(PROP_ID_delVersion);
         return _delVersion;
    }

    /**
     * 逻辑删除版本: DEL_VERSION
     */
    public final void setDelVersion(java.lang.Long value){
        if(onPropSet(PROP_ID_delVersion,value)){
            this._delVersion = value;
            internalClearRefs(PROP_ID_delVersion);
            
        }
    }
    
    /**
     * 数据版本: VERSION
     */
    public final java.lang.Integer getVersion(){
         onPropGet(PROP_ID_version);
         return _version;
    }

    /**
     * 数据版本: VERSION
     */
    public final void setVersion(java.lang.Integer value){
        if(onPropSet(PROP_ID_version,value)){
            this._version = value;
            internalClearRefs(PROP_ID_version);
            
        }
    }
    
    /**
     * 创建人: CREATED_BY
     */
    public final java.lang.String getCreatedBy(){
         onPropGet(PROP_ID_createdBy);
         return _createdBy;
    }

    /**
     * 创建人: CREATED_BY
     */
    public final void setCreatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_createdBy,value)){
            this._createdBy = value;
            internalClearRefs(PROP_ID_createdBy);
            
        }
    }
    
    /**
     * 创建时间: CREATE_TIME
     */
    public final java.sql.Timestamp getCreateTime(){
         onPropGet(PROP_ID_createTime);
         return _createTime;
    }

    /**
     * 创建时间: CREATE_TIME
     */
    public final void setCreateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_createTime,value)){
            this._createTime = value;
            internalClearRefs(PROP_ID_createTime);
            
        }
    }
    
    /**
     * 修改人: UPDATED_BY
     */
    public final java.lang.String getUpdatedBy(){
         onPropGet(PROP_ID_updatedBy);
         return _updatedBy;
    }

    /**
     * 修改人: UPDATED_BY
     */
    public final void setUpdatedBy(java.lang.String value){
        if(onPropSet(PROP_ID_updatedBy,value)){
            this._updatedBy = value;
            internalClearRefs(PROP_ID_updatedBy);
            
        }
    }
    
    /**
     * 修改时间: UPDATE_TIME
     */
    public final java.sql.Timestamp getUpdateTime(){
         onPropGet(PROP_ID_updateTime);
         return _updateTime;
    }

    /**
     * 修改时间: UPDATE_TIME
     */
    public final void setUpdateTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_updateTime,value)){
            this._updateTime = value;
            internalClearRefs(PROP_ID_updateTime);
            
        }
    }
    
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdPartner getPartner(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_partner);
    }

    public final void setPartner(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partner, refEntity,()->{
           
                           this.setPartnerId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdOrganization getOrg(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_org);
    }

    public final void setOrg(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setOrgId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_org, refEntity,()->{
           
                           this.setOrgId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.b2b.dao.entity.ErpB2bMftCertificate getCert(){
       return (app.erp.b2b.dao.entity.ErpB2bMftCertificate)internalGetRefEntity(PROP_NAME_cert);
    }

    public final void setCert(app.erp.b2b.dao.entity.ErpB2bMftCertificate refEntity){
   
           if(refEntity == null){
           
                   this.setCertId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_cert, refEntity,()->{
           
                           this.setCertId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
