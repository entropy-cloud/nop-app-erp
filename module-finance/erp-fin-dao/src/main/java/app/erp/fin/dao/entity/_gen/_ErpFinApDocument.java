package app.erp.fin.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.fin.dao.entity.ErpFinApDocument;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  AP 摄取文档: erp_fin_ap_document
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinApDocument extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 文件名: FILE_NAME VARCHAR */
    public static final String PROP_NAME_fileName = "fileName";
    public static final int PROP_ID_fileName = 3;
    
    /* 文件扩展名: FILE_EXT VARCHAR */
    public static final String PROP_NAME_fileExt = "fileExt";
    public static final int PROP_ID_fileExt = 4;
    
    /* MIME 类型: MIME_TYPE VARCHAR */
    public static final String PROP_NAME_mimeType = "mimeType";
    public static final int PROP_ID_mimeType = 5;
    
    /* 文件大小(字节): FILE_LENGTH BIGINT */
    public static final String PROP_NAME_fileLength = "fileLength";
    public static final int PROP_ID_fileLength = 6;
    
    /* 文件引用(nop-file): FILE_ID VARCHAR */
    public static final String PROP_NAME_fileId = "fileId";
    public static final int PROP_ID_fileId = 7;
    
    /* 来源: SOURCE_TYPE VARCHAR */
    public static final String PROP_NAME_sourceType = "sourceType";
    public static final int PROP_ID_sourceType = 8;
    
    /* 文档状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 9;
    
    /* 单据类型: DOC_TYPE VARCHAR */
    public static final String PROP_NAME_docType = "docType";
    public static final int PROP_ID_docType = 10;
    
    /* 对应方(供应商): PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 11;
    
    /* 分类置信度: CONFIDENCE DECIMAL */
    public static final String PROP_NAME_confidence = "confidence";
    public static final int PROP_ID_confidence = 12;
    
    /* 解析结果: PARSE_RESULT VARCHAR */
    public static final String PROP_NAME_parseResult = "parseResult";
    public static final int PROP_ID_parseResult = 13;
    
    /* 草稿发票回链: INVOICE_ID BIGINT */
    public static final String PROP_NAME_invoiceId = "invoiceId";
    public static final int PROP_ID_invoiceId = 14;
    
    /* 错误信息: ERROR_MSG VARCHAR */
    public static final String PROP_NAME_errorMsg = "errorMsg";
    public static final int PROP_ID_errorMsg = 15;
    
    /* 重试次数: RETRY_COUNT INTEGER */
    public static final String PROP_NAME_retryCount = "retryCount";
    public static final int PROP_ID_retryCount = 16;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 17;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 18;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 19;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 20;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 21;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 22;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 23;
    

    private static int _PROP_ID_BOUND = 24;

    
    /* component:  */
    public static final String PROP_NAME_parseResultComponent = "parseResultComponent";
    
    /* component:  */
    public static final String PROP_NAME_fileIdComponent = "fileIdComponent";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[24];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_fileName] = PROP_NAME_fileName;
          PROP_NAME_TO_ID.put(PROP_NAME_fileName, PROP_ID_fileName);
      
          PROP_ID_TO_NAME[PROP_ID_fileExt] = PROP_NAME_fileExt;
          PROP_NAME_TO_ID.put(PROP_NAME_fileExt, PROP_ID_fileExt);
      
          PROP_ID_TO_NAME[PROP_ID_mimeType] = PROP_NAME_mimeType;
          PROP_NAME_TO_ID.put(PROP_NAME_mimeType, PROP_ID_mimeType);
      
          PROP_ID_TO_NAME[PROP_ID_fileLength] = PROP_NAME_fileLength;
          PROP_NAME_TO_ID.put(PROP_NAME_fileLength, PROP_ID_fileLength);
      
          PROP_ID_TO_NAME[PROP_ID_fileId] = PROP_NAME_fileId;
          PROP_NAME_TO_ID.put(PROP_NAME_fileId, PROP_ID_fileId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceType] = PROP_NAME_sourceType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceType, PROP_ID_sourceType);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_docType] = PROP_NAME_docType;
          PROP_NAME_TO_ID.put(PROP_NAME_docType, PROP_ID_docType);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_confidence] = PROP_NAME_confidence;
          PROP_NAME_TO_ID.put(PROP_NAME_confidence, PROP_ID_confidence);
      
          PROP_ID_TO_NAME[PROP_ID_parseResult] = PROP_NAME_parseResult;
          PROP_NAME_TO_ID.put(PROP_NAME_parseResult, PROP_ID_parseResult);
      
          PROP_ID_TO_NAME[PROP_ID_invoiceId] = PROP_NAME_invoiceId;
          PROP_NAME_TO_ID.put(PROP_NAME_invoiceId, PROP_ID_invoiceId);
      
          PROP_ID_TO_NAME[PROP_ID_errorMsg] = PROP_NAME_errorMsg;
          PROP_NAME_TO_ID.put(PROP_NAME_errorMsg, PROP_ID_errorMsg);
      
          PROP_ID_TO_NAME[PROP_ID_retryCount] = PROP_NAME_retryCount;
          PROP_NAME_TO_ID.put(PROP_NAME_retryCount, PROP_ID_retryCount);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_remark] = PROP_NAME_remark;
          PROP_NAME_TO_ID.put(PROP_NAME_remark, PROP_ID_remark);
      
    }

    
    /* ID: ID */
    private java.lang.String _id;
    
    /* 业务组织: ORG_ID */
    private java.lang.String _orgId;
    
    /* 文件名: FILE_NAME */
    private java.lang.String _fileName;
    
    /* 文件扩展名: FILE_EXT */
    private java.lang.String _fileExt;
    
    /* MIME 类型: MIME_TYPE */
    private java.lang.String _mimeType;
    
    /* 文件大小(字节): FILE_LENGTH */
    private java.lang.Long _fileLength;
    
    /* 文件引用(nop-file): FILE_ID */
    private java.lang.String _fileId;
    
    /* 来源: SOURCE_TYPE */
    private java.lang.String _sourceType;
    
    /* 文档状态: STATUS */
    private java.lang.String _status;
    
    /* 单据类型: DOC_TYPE */
    private java.lang.String _docType;
    
    /* 对应方(供应商): PARTNER_ID */
    private java.lang.String _partnerId;
    
    /* 分类置信度: CONFIDENCE */
    private java.math.BigDecimal _confidence;
    
    /* 解析结果: PARSE_RESULT */
    private java.lang.String _parseResult;
    
    /* 草稿发票回链: INVOICE_ID */
    private java.lang.String _invoiceId;
    
    /* 错误信息: ERROR_MSG */
    private java.lang.String _errorMsg;
    
    /* 重试次数: RETRY_COUNT */
    private java.lang.Integer _retryCount;
    
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
    
    /* 备注: REMARK */
    private java.lang.String _remark;
    

    public _ErpFinApDocument(){
        // for debug
    }

    protected ErpFinApDocument newInstance(){
        ErpFinApDocument entity = new ErpFinApDocument();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinApDocument cloneInstance() {
        ErpFinApDocument entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinApDocument";
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
        
            case PROP_ID_fileName:
               return getFileName();
        
            case PROP_ID_fileExt:
               return getFileExt();
        
            case PROP_ID_mimeType:
               return getMimeType();
        
            case PROP_ID_fileLength:
               return getFileLength();
        
            case PROP_ID_fileId:
               return getFileId();
        
            case PROP_ID_sourceType:
               return getSourceType();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_docType:
               return getDocType();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_confidence:
               return getConfidence();
        
            case PROP_ID_parseResult:
               return getParseResult();
        
            case PROP_ID_invoiceId:
               return getInvoiceId();
        
            case PROP_ID_errorMsg:
               return getErrorMsg();
        
            case PROP_ID_retryCount:
               return getRetryCount();
        
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
        
            case PROP_ID_remark:
               return getRemark();
        
           default:
              return super.orm_propValue(propId);
        }
    }

    

    @Override
    public void orm_propValue(int propId, Object value){
        switch(propId){
        
            case PROP_ID_id:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_id));
               }
               setId(typedValue);
               break;
            }
        
            case PROP_ID_orgId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
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
        
            case PROP_ID_fileExt:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fileExt));
               }
               setFileExt(typedValue);
               break;
            }
        
            case PROP_ID_mimeType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_mimeType));
               }
               setMimeType(typedValue);
               break;
            }
        
            case PROP_ID_fileLength:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fileLength));
               }
               setFileLength(typedValue);
               break;
            }
        
            case PROP_ID_fileId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_fileId));
               }
               setFileId(typedValue);
               break;
            }
        
            case PROP_ID_sourceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceType));
               }
               setSourceType(typedValue);
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
        
            case PROP_ID_docType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docType));
               }
               setDocType(typedValue);
               break;
            }
        
            case PROP_ID_partnerId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_partnerId));
               }
               setPartnerId(typedValue);
               break;
            }
        
            case PROP_ID_confidence:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_confidence));
               }
               setConfidence(typedValue);
               break;
            }
        
            case PROP_ID_parseResult:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_parseResult));
               }
               setParseResult(typedValue);
               break;
            }
        
            case PROP_ID_invoiceId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_invoiceId));
               }
               setInvoiceId(typedValue);
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
        
            case PROP_ID_remark:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_remark));
               }
               setRemark(typedValue);
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
               this._id = (java.lang.String)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fileName:{
               onInitProp(propId);
               this._fileName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fileExt:{
               onInitProp(propId);
               this._fileExt = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_mimeType:{
               onInitProp(propId);
               this._mimeType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_fileLength:{
               onInitProp(propId);
               this._fileLength = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_fileId:{
               onInitProp(propId);
               this._fileId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceType:{
               onInitProp(propId);
               this._sourceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_docType:{
               onInitProp(propId);
               this._docType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_confidence:{
               onInitProp(propId);
               this._confidence = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_parseResult:{
               onInitProp(propId);
               this._parseResult = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_invoiceId:{
               onInitProp(propId);
               this._invoiceId = (java.lang.String)value;
               
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
        
            case PROP_ID_remark:{
               onInitProp(propId);
               this._remark = (java.lang.String)value;
               
               break;
            }
        
           default:
              super.orm_internalSet(propId,value);
        }
    }

    
    /**
     * ID: ID
     */
    public final java.lang.String getId(){
         onPropGet(PROP_ID_id);
         return _id;
    }

    /**
     * ID: ID
     */
    public final void setId(java.lang.String value){
        if(onPropSet(PROP_ID_id,value)){
            this._id = value;
            internalClearRefs(PROP_ID_id);
            orm_id();
        }
    }
    
    /**
     * 业务组织: ORG_ID
     */
    public final java.lang.String getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 业务组织: ORG_ID
     */
    public final void setOrgId(java.lang.String value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
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
     * 文件扩展名: FILE_EXT
     */
    public final java.lang.String getFileExt(){
         onPropGet(PROP_ID_fileExt);
         return _fileExt;
    }

    /**
     * 文件扩展名: FILE_EXT
     */
    public final void setFileExt(java.lang.String value){
        if(onPropSet(PROP_ID_fileExt,value)){
            this._fileExt = value;
            internalClearRefs(PROP_ID_fileExt);
            
        }
    }
    
    /**
     * MIME 类型: MIME_TYPE
     */
    public final java.lang.String getMimeType(){
         onPropGet(PROP_ID_mimeType);
         return _mimeType;
    }

    /**
     * MIME 类型: MIME_TYPE
     */
    public final void setMimeType(java.lang.String value){
        if(onPropSet(PROP_ID_mimeType,value)){
            this._mimeType = value;
            internalClearRefs(PROP_ID_mimeType);
            
        }
    }
    
    /**
     * 文件大小(字节): FILE_LENGTH
     */
    public final java.lang.Long getFileLength(){
         onPropGet(PROP_ID_fileLength);
         return _fileLength;
    }

    /**
     * 文件大小(字节): FILE_LENGTH
     */
    public final void setFileLength(java.lang.Long value){
        if(onPropSet(PROP_ID_fileLength,value)){
            this._fileLength = value;
            internalClearRefs(PROP_ID_fileLength);
            
        }
    }
    
    /**
     * 文件引用(nop-file): FILE_ID
     */
    public final java.lang.String getFileId(){
         onPropGet(PROP_ID_fileId);
         return _fileId;
    }

    /**
     * 文件引用(nop-file): FILE_ID
     */
    public final void setFileId(java.lang.String value){
        if(onPropSet(PROP_ID_fileId,value)){
            this._fileId = value;
            internalClearRefs(PROP_ID_fileId);
            
        }
    }
    
    /**
     * 来源: SOURCE_TYPE
     */
    public final java.lang.String getSourceType(){
         onPropGet(PROP_ID_sourceType);
         return _sourceType;
    }

    /**
     * 来源: SOURCE_TYPE
     */
    public final void setSourceType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceType,value)){
            this._sourceType = value;
            internalClearRefs(PROP_ID_sourceType);
            
        }
    }
    
    /**
     * 文档状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 文档状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 单据类型: DOC_TYPE
     */
    public final java.lang.String getDocType(){
         onPropGet(PROP_ID_docType);
         return _docType;
    }

    /**
     * 单据类型: DOC_TYPE
     */
    public final void setDocType(java.lang.String value){
        if(onPropSet(PROP_ID_docType,value)){
            this._docType = value;
            internalClearRefs(PROP_ID_docType);
            
        }
    }
    
    /**
     * 对应方(供应商): PARTNER_ID
     */
    public final java.lang.String getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 对应方(供应商): PARTNER_ID
     */
    public final void setPartnerId(java.lang.String value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 分类置信度: CONFIDENCE
     */
    public final java.math.BigDecimal getConfidence(){
         onPropGet(PROP_ID_confidence);
         return _confidence;
    }

    /**
     * 分类置信度: CONFIDENCE
     */
    public final void setConfidence(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_confidence,value)){
            this._confidence = value;
            internalClearRefs(PROP_ID_confidence);
            
        }
    }
    
    /**
     * 解析结果: PARSE_RESULT
     */
    public final java.lang.String getParseResult(){
         onPropGet(PROP_ID_parseResult);
         return _parseResult;
    }

    /**
     * 解析结果: PARSE_RESULT
     */
    public final void setParseResult(java.lang.String value){
        if(onPropSet(PROP_ID_parseResult,value)){
            this._parseResult = value;
            internalClearRefs(PROP_ID_parseResult);
            
        }
    }
    
    /**
     * 草稿发票回链: INVOICE_ID
     */
    public final java.lang.String getInvoiceId(){
         onPropGet(PROP_ID_invoiceId);
         return _invoiceId;
    }

    /**
     * 草稿发票回链: INVOICE_ID
     */
    public final void setInvoiceId(java.lang.String value){
        if(onPropSet(PROP_ID_invoiceId,value)){
            this._invoiceId = value;
            internalClearRefs(PROP_ID_invoiceId);
            
        }
    }
    
    /**
     * 错误信息: ERROR_MSG
     */
    public final java.lang.String getErrorMsg(){
         onPropGet(PROP_ID_errorMsg);
         return _errorMsg;
    }

    /**
     * 错误信息: ERROR_MSG
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
    
   private io.nop.orm.component.JsonOrmComponent _parseResultComponent;

   private static Map<String,Integer> COMPONENT_PROP_ID_MAP_parseResultComponent = new HashMap<>();
   static{
      
         COMPONENT_PROP_ID_MAP_parseResultComponent.put(io.nop.orm.component.JsonOrmComponent.PROP_NAME__jsonText,PROP_ID_parseResult);
      
   }

   public final io.nop.orm.component.JsonOrmComponent getParseResultComponent(){
      if(_parseResultComponent == null){
          _parseResultComponent = new io.nop.orm.component.JsonOrmComponent();
          _parseResultComponent.bindToEntity(this, COMPONENT_PROP_ID_MAP_parseResultComponent);
      }
      return _parseResultComponent;
   }

   private io.nop.orm.component.OrmFileComponent _fileIdComponent;

   private static Map<String,Integer> COMPONENT_PROP_ID_MAP_fileIdComponent = new HashMap<>();
   static{
      
         COMPONENT_PROP_ID_MAP_fileIdComponent.put(io.nop.orm.component.OrmFileComponent.PROP_NAME_filePath,PROP_ID_fileId);
      
   }

   public final io.nop.orm.component.OrmFileComponent getFileIdComponent(){
      if(_fileIdComponent == null){
          _fileIdComponent = new io.nop.orm.component.OrmFileComponent();
          _fileIdComponent.bindToEntity(this, COMPONENT_PROP_ID_MAP_fileIdComponent);
      }
      return _fileIdComponent;
   }

}
// resume CPD analysis - CPD-ON
