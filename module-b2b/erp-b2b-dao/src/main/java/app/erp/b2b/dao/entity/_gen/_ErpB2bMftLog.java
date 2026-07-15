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

import app.erp.b2b.dao.entity.ErpB2bMftLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  MFT 传输日志: erp_b2b_mft_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bMftLog extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* MFT配置: CONFIG_ID BIGINT */
    public static final String PROP_NAME_configId = "configId";
    public static final int PROP_ID_configId = 3;
    
    /* 关联单据类型: RELATED_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_relatedBillType = "relatedBillType";
    public static final int PROP_ID_relatedBillType = 4;
    
    /* 关联单据号: RELATED_BILL_CODE VARCHAR */
    public static final String PROP_NAME_relatedBillCode = "relatedBillCode";
    public static final int PROP_ID_relatedBillCode = 5;
    
    /* 方向: DIRECTION VARCHAR */
    public static final String PROP_NAME_direction = "direction";
    public static final int PROP_ID_direction = 6;
    
    /* 文件名: FILE_NAME VARCHAR */
    public static final String PROP_NAME_fileName = "fileName";
    public static final int PROP_ID_fileName = 7;
    
    /* 文件大小(字节): FILE_SIZE BIGINT */
    public static final String PROP_NAME_fileSize = "fileSize";
    public static final int PROP_ID_fileSize = 8;
    
    /* 文件哈希: FILE_HASH VARCHAR */
    public static final String PROP_NAME_fileHash = "fileHash";
    public static final int PROP_ID_fileHash = 9;
    
    /* AS2 Message-ID: MESSAGE_ID VARCHAR */
    public static final String PROP_NAME_messageId = "messageId";
    public static final int PROP_ID_messageId = 10;
    
    /* MDN状态: MDN_STATUS VARCHAR */
    public static final String PROP_NAME_mdnStatus = "mdnStatus";
    public static final int PROP_ID_mdnStatus = 11;
    
    /* 传输协议: PROTOCOL VARCHAR */
    public static final String PROP_NAME_protocol = "protocol";
    public static final int PROP_ID_protocol = 12;
    
    /* 传输状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 13;
    
    /* 开始时间: START_TIME TIMESTAMP */
    public static final String PROP_NAME_startTime = "startTime";
    public static final int PROP_ID_startTime = 14;
    
    /* 结束时间: END_TIME TIMESTAMP */
    public static final String PROP_NAME_endTime = "endTime";
    public static final int PROP_ID_endTime = 15;
    
    /* 耗时(毫秒): DURATION_MS BIGINT */
    public static final String PROP_NAME_durationMs = "durationMs";
    public static final int PROP_ID_durationMs = 16;
    
    /* 错误码: ERROR_CODE VARCHAR */
    public static final String PROP_NAME_errorCode = "errorCode";
    public static final int PROP_ID_errorCode = 17;
    
    /* 错误消息: ERROR_MSG VARCHAR */
    public static final String PROP_NAME_errorMsg = "errorMsg";
    public static final int PROP_ID_errorMsg = 18;
    
    /* 重试次数: RETRY_COUNT INTEGER */
    public static final String PROP_NAME_retryCount = "retryCount";
    public static final int PROP_ID_retryCount = 19;
    
    /* 压缩: IS_COMPRESSED BOOLEAN */
    public static final String PROP_NAME_isCompressed = "isCompressed";
    public static final int PROP_ID_isCompressed = 20;
    
    /* 加密: IS_ENCRYPTED BOOLEAN */
    public static final String PROP_NAME_isEncrypted = "isEncrypted";
    public static final int PROP_ID_isEncrypted = 21;
    
    /* 签名: IS_SIGNED BOOLEAN */
    public static final String PROP_NAME_isSigned = "isSigned";
    public static final int PROP_ID_isSigned = 22;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 23;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 24;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 25;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 26;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 27;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 28;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 29;
    

    private static int _PROP_ID_BOUND = 30;

    
    /* relation:  */
    public static final String PROP_NAME_config = "config";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[30];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_configId] = PROP_NAME_configId;
          PROP_NAME_TO_ID.put(PROP_NAME_configId, PROP_ID_configId);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillType] = PROP_NAME_relatedBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillType, PROP_ID_relatedBillType);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillCode] = PROP_NAME_relatedBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillCode, PROP_ID_relatedBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_direction] = PROP_NAME_direction;
          PROP_NAME_TO_ID.put(PROP_NAME_direction, PROP_ID_direction);
      
          PROP_ID_TO_NAME[PROP_ID_fileName] = PROP_NAME_fileName;
          PROP_NAME_TO_ID.put(PROP_NAME_fileName, PROP_ID_fileName);
      
          PROP_ID_TO_NAME[PROP_ID_fileSize] = PROP_NAME_fileSize;
          PROP_NAME_TO_ID.put(PROP_NAME_fileSize, PROP_ID_fileSize);
      
          PROP_ID_TO_NAME[PROP_ID_fileHash] = PROP_NAME_fileHash;
          PROP_NAME_TO_ID.put(PROP_NAME_fileHash, PROP_ID_fileHash);
      
          PROP_ID_TO_NAME[PROP_ID_messageId] = PROP_NAME_messageId;
          PROP_NAME_TO_ID.put(PROP_NAME_messageId, PROP_ID_messageId);
      
          PROP_ID_TO_NAME[PROP_ID_mdnStatus] = PROP_NAME_mdnStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_mdnStatus, PROP_ID_mdnStatus);
      
          PROP_ID_TO_NAME[PROP_ID_protocol] = PROP_NAME_protocol;
          PROP_NAME_TO_ID.put(PROP_NAME_protocol, PROP_ID_protocol);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_startTime] = PROP_NAME_startTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startTime, PROP_ID_startTime);
      
          PROP_ID_TO_NAME[PROP_ID_endTime] = PROP_NAME_endTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endTime, PROP_ID_endTime);
      
          PROP_ID_TO_NAME[PROP_ID_durationMs] = PROP_NAME_durationMs;
          PROP_NAME_TO_ID.put(PROP_NAME_durationMs, PROP_ID_durationMs);
      
          PROP_ID_TO_NAME[PROP_ID_errorCode] = PROP_NAME_errorCode;
          PROP_NAME_TO_ID.put(PROP_NAME_errorCode, PROP_ID_errorCode);
      
          PROP_ID_TO_NAME[PROP_ID_errorMsg] = PROP_NAME_errorMsg;
          PROP_NAME_TO_ID.put(PROP_NAME_errorMsg, PROP_ID_errorMsg);
      
          PROP_ID_TO_NAME[PROP_ID_retryCount] = PROP_NAME_retryCount;
          PROP_NAME_TO_ID.put(PROP_NAME_retryCount, PROP_ID_retryCount);
      
          PROP_ID_TO_NAME[PROP_ID_isCompressed] = PROP_NAME_isCompressed;
          PROP_NAME_TO_ID.put(PROP_NAME_isCompressed, PROP_ID_isCompressed);
      
          PROP_ID_TO_NAME[PROP_ID_isEncrypted] = PROP_NAME_isEncrypted;
          PROP_NAME_TO_ID.put(PROP_NAME_isEncrypted, PROP_ID_isEncrypted);
      
          PROP_ID_TO_NAME[PROP_ID_isSigned] = PROP_NAME_isSigned;
          PROP_NAME_TO_ID.put(PROP_NAME_isSigned, PROP_ID_isSigned);
      
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
    
    /* MFT配置: CONFIG_ID */
    private java.lang.Long _configId;
    
    /* 关联单据类型: RELATED_BILL_TYPE */
    private java.lang.String _relatedBillType;
    
    /* 关联单据号: RELATED_BILL_CODE */
    private java.lang.String _relatedBillCode;
    
    /* 方向: DIRECTION */
    private java.lang.String _direction;
    
    /* 文件名: FILE_NAME */
    private java.lang.String _fileName;
    
    /* 文件大小(字节): FILE_SIZE */
    private java.lang.Long _fileSize;
    
    /* 文件哈希: FILE_HASH */
    private java.lang.String _fileHash;
    
    /* AS2 Message-ID: MESSAGE_ID */
    private java.lang.String _messageId;
    
    /* MDN状态: MDN_STATUS */
    private java.lang.String _mdnStatus;
    
    /* 传输协议: PROTOCOL */
    private java.lang.String _protocol;
    
    /* 传输状态: STATUS */
    private java.lang.String _status;
    
    /* 开始时间: START_TIME */
    private java.sql.Timestamp _startTime;
    
    /* 结束时间: END_TIME */
    private java.sql.Timestamp _endTime;
    
    /* 耗时(毫秒): DURATION_MS */
    private java.lang.Long _durationMs;
    
    /* 错误码: ERROR_CODE */
    private java.lang.String _errorCode;
    
    /* 错误消息: ERROR_MSG */
    private java.lang.String _errorMsg;
    
    /* 重试次数: RETRY_COUNT */
    private java.lang.Integer _retryCount;
    
    /* 压缩: IS_COMPRESSED */
    private java.lang.Boolean _isCompressed;
    
    /* 加密: IS_ENCRYPTED */
    private java.lang.Boolean _isEncrypted;
    
    /* 签名: IS_SIGNED */
    private java.lang.Boolean _isSigned;
    
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
    

    public _ErpB2bMftLog(){
        // for debug
    }

    protected ErpB2bMftLog newInstance(){
        ErpB2bMftLog entity = new ErpB2bMftLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bMftLog cloneInstance() {
        ErpB2bMftLog entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bMftLog";
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
        
            case PROP_ID_configId:
               return getConfigId();
        
            case PROP_ID_relatedBillType:
               return getRelatedBillType();
        
            case PROP_ID_relatedBillCode:
               return getRelatedBillCode();
        
            case PROP_ID_direction:
               return getDirection();
        
            case PROP_ID_fileName:
               return getFileName();
        
            case PROP_ID_fileSize:
               return getFileSize();
        
            case PROP_ID_fileHash:
               return getFileHash();
        
            case PROP_ID_messageId:
               return getMessageId();
        
            case PROP_ID_mdnStatus:
               return getMdnStatus();
        
            case PROP_ID_protocol:
               return getProtocol();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_startTime:
               return getStartTime();
        
            case PROP_ID_endTime:
               return getEndTime();
        
            case PROP_ID_durationMs:
               return getDurationMs();
        
            case PROP_ID_errorCode:
               return getErrorCode();
        
            case PROP_ID_errorMsg:
               return getErrorMsg();
        
            case PROP_ID_retryCount:
               return getRetryCount();
        
            case PROP_ID_isCompressed:
               return getIsCompressed();
        
            case PROP_ID_isEncrypted:
               return getIsEncrypted();
        
            case PROP_ID_isSigned:
               return getIsSigned();
        
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
        
            case PROP_ID_configId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_configId));
               }
               setConfigId(typedValue);
               break;
            }
        
            case PROP_ID_relatedBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_relatedBillType));
               }
               setRelatedBillType(typedValue);
               break;
            }
        
            case PROP_ID_relatedBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_relatedBillCode));
               }
               setRelatedBillCode(typedValue);
               break;
            }
        
            case PROP_ID_direction:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_direction));
               }
               setDirection(typedValue);
               break;
            }
        
            case PROP_ID_fileName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fileName));
               }
               setFileName(typedValue);
               break;
            }
        
            case PROP_ID_fileSize:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fileSize));
               }
               setFileSize(typedValue);
               break;
            }
        
            case PROP_ID_fileHash:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fileHash));
               }
               setFileHash(typedValue);
               break;
            }
        
            case PROP_ID_messageId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_messageId));
               }
               setMessageId(typedValue);
               break;
            }
        
            case PROP_ID_mdnStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_mdnStatus));
               }
               setMdnStatus(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_startTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_startTime));
               }
               setStartTime(typedValue);
               break;
            }
        
            case PROP_ID_endTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_endTime));
               }
               setEndTime(typedValue);
               break;
            }
        
            case PROP_ID_durationMs:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_durationMs));
               }
               setDurationMs(typedValue);
               break;
            }
        
            case PROP_ID_errorCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_errorCode));
               }
               setErrorCode(typedValue);
               break;
            }
        
            case PROP_ID_errorMsg:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_errorMsg));
               }
               setErrorMsg(typedValue);
               break;
            }
        
            case PROP_ID_retryCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_retryCount));
               }
               setRetryCount(typedValue);
               break;
            }
        
            case PROP_ID_isCompressed:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isCompressed));
               }
               setIsCompressed(typedValue);
               break;
            }
        
            case PROP_ID_isEncrypted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isEncrypted));
               }
               setIsEncrypted(typedValue);
               break;
            }
        
            case PROP_ID_isSigned:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isSigned));
               }
               setIsSigned(typedValue);
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
        
            case PROP_ID_configId:{
               onInitProp(propId);
               this._configId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_relatedBillType:{
               onInitProp(propId);
               this._relatedBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_relatedBillCode:{
               onInitProp(propId);
               this._relatedBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_direction:{
               onInitProp(propId);
               this._direction = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fileName:{
               onInitProp(propId);
               this._fileName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fileSize:{
               onInitProp(propId);
               this._fileSize = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fileHash:{
               onInitProp(propId);
               this._fileHash = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_messageId:{
               onInitProp(propId);
               this._messageId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mdnStatus:{
               onInitProp(propId);
               this._mdnStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_protocol:{
               onInitProp(propId);
               this._protocol = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_startTime:{
               onInitProp(propId);
               this._startTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_endTime:{
               onInitProp(propId);
               this._endTime = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_durationMs:{
               onInitProp(propId);
               this._durationMs = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_errorCode:{
               onInitProp(propId);
               this._errorCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_errorMsg:{
               onInitProp(propId);
               this._errorMsg = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_retryCount:{
               onInitProp(propId);
               this._retryCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_isCompressed:{
               onInitProp(propId);
               this._isCompressed = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isEncrypted:{
               onInitProp(propId);
               this._isEncrypted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isSigned:{
               onInitProp(propId);
               this._isSigned = (java.lang.Boolean)value;
               
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
     * MFT配置: CONFIG_ID
     */
    public final java.lang.Long getConfigId(){
         onPropGet(PROP_ID_configId);
         return _configId;
    }

    /**
     * MFT配置: CONFIG_ID
     */
    public final void setConfigId(java.lang.Long value){
        if(onPropSet(PROP_ID_configId,value)){
            this._configId = value;
            internalClearRefs(PROP_ID_configId);
            
        }
    }
    
    /**
     * 关联单据类型: RELATED_BILL_TYPE
     */
    public final java.lang.String getRelatedBillType(){
         onPropGet(PROP_ID_relatedBillType);
         return _relatedBillType;
    }

    /**
     * 关联单据类型: RELATED_BILL_TYPE
     */
    public final void setRelatedBillType(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillType,value)){
            this._relatedBillType = value;
            internalClearRefs(PROP_ID_relatedBillType);
            
        }
    }
    
    /**
     * 关联单据号: RELATED_BILL_CODE
     */
    public final java.lang.String getRelatedBillCode(){
         onPropGet(PROP_ID_relatedBillCode);
         return _relatedBillCode;
    }

    /**
     * 关联单据号: RELATED_BILL_CODE
     */
    public final void setRelatedBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_relatedBillCode,value)){
            this._relatedBillCode = value;
            internalClearRefs(PROP_ID_relatedBillCode);
            
        }
    }
    
    /**
     * 方向: DIRECTION
     */
    public final java.lang.String getDirection(){
         onPropGet(PROP_ID_direction);
         return _direction;
    }

    /**
     * 方向: DIRECTION
     */
    public final void setDirection(java.lang.String value){
        if(onPropSet(PROP_ID_direction,value)){
            this._direction = value;
            internalClearRefs(PROP_ID_direction);
            
        }
    }
    
    /**
     * 文件名: FILE_NAME
     */
    public final java.lang.String getFileName(){
         onPropGet(PROP_ID_fileName);
         return _fileName;
    }

    /**
     * 文件名: FILE_NAME
     */
    public final void setFileName(java.lang.String value){
        if(onPropSet(PROP_ID_fileName,value)){
            this._fileName = value;
            internalClearRefs(PROP_ID_fileName);
            
        }
    }
    
    /**
     * 文件大小(字节): FILE_SIZE
     */
    public final java.lang.Long getFileSize(){
         onPropGet(PROP_ID_fileSize);
         return _fileSize;
    }

    /**
     * 文件大小(字节): FILE_SIZE
     */
    public final void setFileSize(java.lang.Long value){
        if(onPropSet(PROP_ID_fileSize,value)){
            this._fileSize = value;
            internalClearRefs(PROP_ID_fileSize);
            
        }
    }
    
    /**
     * 文件哈希: FILE_HASH
     */
    public final java.lang.String getFileHash(){
         onPropGet(PROP_ID_fileHash);
         return _fileHash;
    }

    /**
     * 文件哈希: FILE_HASH
     */
    public final void setFileHash(java.lang.String value){
        if(onPropSet(PROP_ID_fileHash,value)){
            this._fileHash = value;
            internalClearRefs(PROP_ID_fileHash);
            
        }
    }
    
    /**
     * AS2 Message-ID: MESSAGE_ID
     */
    public final java.lang.String getMessageId(){
         onPropGet(PROP_ID_messageId);
         return _messageId;
    }

    /**
     * AS2 Message-ID: MESSAGE_ID
     */
    public final void setMessageId(java.lang.String value){
        if(onPropSet(PROP_ID_messageId,value)){
            this._messageId = value;
            internalClearRefs(PROP_ID_messageId);
            
        }
    }
    
    /**
     * MDN状态: MDN_STATUS
     */
    public final java.lang.String getMdnStatus(){
         onPropGet(PROP_ID_mdnStatus);
         return _mdnStatus;
    }

    /**
     * MDN状态: MDN_STATUS
     */
    public final void setMdnStatus(java.lang.String value){
        if(onPropSet(PROP_ID_mdnStatus,value)){
            this._mdnStatus = value;
            internalClearRefs(PROP_ID_mdnStatus);
            
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
     * 传输状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 传输状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 开始时间: START_TIME
     */
    public final java.sql.Timestamp getStartTime(){
         onPropGet(PROP_ID_startTime);
         return _startTime;
    }

    /**
     * 开始时间: START_TIME
     */
    public final void setStartTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_startTime,value)){
            this._startTime = value;
            internalClearRefs(PROP_ID_startTime);
            
        }
    }
    
    /**
     * 结束时间: END_TIME
     */
    public final java.sql.Timestamp getEndTime(){
         onPropGet(PROP_ID_endTime);
         return _endTime;
    }

    /**
     * 结束时间: END_TIME
     */
    public final void setEndTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_endTime,value)){
            this._endTime = value;
            internalClearRefs(PROP_ID_endTime);
            
        }
    }
    
    /**
     * 耗时(毫秒): DURATION_MS
     */
    public final java.lang.Long getDurationMs(){
         onPropGet(PROP_ID_durationMs);
         return _durationMs;
    }

    /**
     * 耗时(毫秒): DURATION_MS
     */
    public final void setDurationMs(java.lang.Long value){
        if(onPropSet(PROP_ID_durationMs,value)){
            this._durationMs = value;
            internalClearRefs(PROP_ID_durationMs);
            
        }
    }
    
    /**
     * 错误码: ERROR_CODE
     */
    public final java.lang.String getErrorCode(){
         onPropGet(PROP_ID_errorCode);
         return _errorCode;
    }

    /**
     * 错误码: ERROR_CODE
     */
    public final void setErrorCode(java.lang.String value){
        if(onPropSet(PROP_ID_errorCode,value)){
            this._errorCode = value;
            internalClearRefs(PROP_ID_errorCode);
            
        }
    }
    
    /**
     * 错误消息: ERROR_MSG
     */
    public final java.lang.String getErrorMsg(){
         onPropGet(PROP_ID_errorMsg);
         return _errorMsg;
    }

    /**
     * 错误消息: ERROR_MSG
     */
    public final void setErrorMsg(java.lang.String value){
        if(onPropSet(PROP_ID_errorMsg,value)){
            this._errorMsg = value;
            internalClearRefs(PROP_ID_errorMsg);
            
        }
    }
    
    /**
     * 重试次数: RETRY_COUNT
     */
    public final java.lang.Integer getRetryCount(){
         onPropGet(PROP_ID_retryCount);
         return _retryCount;
    }

    /**
     * 重试次数: RETRY_COUNT
     */
    public final void setRetryCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_retryCount,value)){
            this._retryCount = value;
            internalClearRefs(PROP_ID_retryCount);
            
        }
    }
    
    /**
     * 压缩: IS_COMPRESSED
     */
    public final java.lang.Boolean getIsCompressed(){
         onPropGet(PROP_ID_isCompressed);
         return _isCompressed;
    }

    /**
     * 压缩: IS_COMPRESSED
     */
    public final void setIsCompressed(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isCompressed,value)){
            this._isCompressed = value;
            internalClearRefs(PROP_ID_isCompressed);
            
        }
    }
    
    /**
     * 加密: IS_ENCRYPTED
     */
    public final java.lang.Boolean getIsEncrypted(){
         onPropGet(PROP_ID_isEncrypted);
         return _isEncrypted;
    }

    /**
     * 加密: IS_ENCRYPTED
     */
    public final void setIsEncrypted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isEncrypted,value)){
            this._isEncrypted = value;
            internalClearRefs(PROP_ID_isEncrypted);
            
        }
    }
    
    /**
     * 签名: IS_SIGNED
     */
    public final java.lang.Boolean getIsSigned(){
         onPropGet(PROP_ID_isSigned);
         return _isSigned;
    }

    /**
     * 签名: IS_SIGNED
     */
    public final void setIsSigned(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isSigned,value)){
            this._isSigned = value;
            internalClearRefs(PROP_ID_isSigned);
            
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
    public final app.erp.b2b.dao.entity.ErpB2bMftConfig getConfig(){
       return (app.erp.b2b.dao.entity.ErpB2bMftConfig)internalGetRefEntity(PROP_NAME_config);
    }

    public final void setConfig(app.erp.b2b.dao.entity.ErpB2bMftConfig refEntity){
   
           if(refEntity == null){
           
                   this.setConfigId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_config, refEntity,()->{
           
                           this.setConfigId(refEntity.getId());
                       
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
       
}
// resume CPD analysis - CPD-ON
