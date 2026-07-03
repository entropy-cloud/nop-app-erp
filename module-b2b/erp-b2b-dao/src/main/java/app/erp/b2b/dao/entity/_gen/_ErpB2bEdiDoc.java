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

import app.erp.b2b.dao.entity.ErpB2bEdiDoc;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  EDI 事务: erp_b2b_edi_doc
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bEdiDoc extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 事务编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* EDI 格式: FORMAT_ID BIGINT */
    public static final String PROP_NAME_formatId = "formatId";
    public static final int PROP_ID_formatId = 4;
    
    /* 关联单据类型: RELATED_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_relatedBillType = "relatedBillType";
    public static final int PROP_ID_relatedBillType = 5;
    
    /* 关联单据号: RELATED_BILL_CODE VARCHAR */
    public static final String PROP_NAME_relatedBillCode = "relatedBillCode";
    public static final int PROP_ID_relatedBillCode = 6;
    
    /* 状态: STATE VARCHAR */
    public static final String PROP_NAME_state = "state";
    public static final int PROP_ID_state = 7;
    
    /* 阻断级别: BLOCKING_LEVEL VARCHAR */
    public static final String PROP_NAME_blockingLevel = "blockingLevel";
    public static final int PROP_ID_blockingLevel = 8;
    
    /* 错误信息: ERROR VARCHAR */
    public static final String PROP_NAME_error = "error";
    public static final int PROP_ID_error = 9;
    
    /* 重试次数: RETRY_COUNT INTEGER */
    public static final String PROP_NAME_retryCount = "retryCount";
    public static final int PROP_ID_retryCount = 10;
    
    /* 附件（EDI 报文文件）: ATTACHMENT_FILE_ID VARCHAR */
    public static final String PROP_NAME_attachmentFileId = "attachmentFileId";
    public static final int PROP_ID_attachmentFileId = 11;
    
    /* 发送时间: SENT_AT DATETIME */
    public static final String PROP_NAME_sentAt = "sentAt";
    public static final int PROP_ID_sentAt = 12;
    
    /* 确认时间: ACKNOWLEDGED_AT DATETIME */
    public static final String PROP_NAME_acknowledgedAt = "acknowledgedAt";
    public static final int PROP_ID_acknowledgedAt = 13;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 14;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 15;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 16;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 17;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 18;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 19;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 20;
    

    private static int _PROP_ID_BOUND = 21;

    
    /* relation:  */
    public static final String PROP_NAME_format = "format";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* component:  */
    public static final String PROP_NAME_attachmentFileIdComponent = "attachmentFileIdComponent";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_formatId] = PROP_NAME_formatId;
          PROP_NAME_TO_ID.put(PROP_NAME_formatId, PROP_ID_formatId);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillType] = PROP_NAME_relatedBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillType, PROP_ID_relatedBillType);
      
          PROP_ID_TO_NAME[PROP_ID_relatedBillCode] = PROP_NAME_relatedBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_relatedBillCode, PROP_ID_relatedBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_state] = PROP_NAME_state;
          PROP_NAME_TO_ID.put(PROP_NAME_state, PROP_ID_state);
      
          PROP_ID_TO_NAME[PROP_ID_blockingLevel] = PROP_NAME_blockingLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_blockingLevel, PROP_ID_blockingLevel);
      
          PROP_ID_TO_NAME[PROP_ID_error] = PROP_NAME_error;
          PROP_NAME_TO_ID.put(PROP_NAME_error, PROP_ID_error);
      
          PROP_ID_TO_NAME[PROP_ID_retryCount] = PROP_NAME_retryCount;
          PROP_NAME_TO_ID.put(PROP_NAME_retryCount, PROP_ID_retryCount);
      
          PROP_ID_TO_NAME[PROP_ID_attachmentFileId] = PROP_NAME_attachmentFileId;
          PROP_NAME_TO_ID.put(PROP_NAME_attachmentFileId, PROP_ID_attachmentFileId);
      
          PROP_ID_TO_NAME[PROP_ID_sentAt] = PROP_NAME_sentAt;
          PROP_NAME_TO_ID.put(PROP_NAME_sentAt, PROP_ID_sentAt);
      
          PROP_ID_TO_NAME[PROP_ID_acknowledgedAt] = PROP_NAME_acknowledgedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_acknowledgedAt, PROP_ID_acknowledgedAt);
      
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
    
    /* 事务编码: CODE */
    private java.lang.String _code;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* EDI 格式: FORMAT_ID */
    private java.lang.Long _formatId;
    
    /* 关联单据类型: RELATED_BILL_TYPE */
    private java.lang.String _relatedBillType;
    
    /* 关联单据号: RELATED_BILL_CODE */
    private java.lang.String _relatedBillCode;
    
    /* 状态: STATE */
    private java.lang.String _state;
    
    /* 阻断级别: BLOCKING_LEVEL */
    private java.lang.String _blockingLevel;
    
    /* 错误信息: ERROR */
    private java.lang.String _error;
    
    /* 重试次数: RETRY_COUNT */
    private java.lang.Integer _retryCount;
    
    /* 附件（EDI 报文文件）: ATTACHMENT_FILE_ID */
    private java.lang.String _attachmentFileId;
    
    /* 发送时间: SENT_AT */
    private java.time.LocalDateTime _sentAt;
    
    /* 确认时间: ACKNOWLEDGED_AT */
    private java.time.LocalDateTime _acknowledgedAt;
    
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
    

    public _ErpB2bEdiDoc(){
        // for debug
    }

    protected ErpB2bEdiDoc newInstance(){
        ErpB2bEdiDoc entity = new ErpB2bEdiDoc();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bEdiDoc cloneInstance() {
        ErpB2bEdiDoc entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bEdiDoc";
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
        
            case PROP_ID_code:
               return getCode();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_formatId:
               return getFormatId();
        
            case PROP_ID_relatedBillType:
               return getRelatedBillType();
        
            case PROP_ID_relatedBillCode:
               return getRelatedBillCode();
        
            case PROP_ID_state:
               return getState();
        
            case PROP_ID_blockingLevel:
               return getBlockingLevel();
        
            case PROP_ID_error:
               return getError();
        
            case PROP_ID_retryCount:
               return getRetryCount();
        
            case PROP_ID_attachmentFileId:
               return getAttachmentFileId();
        
            case PROP_ID_sentAt:
               return getSentAt();
        
            case PROP_ID_acknowledgedAt:
               return getAcknowledgedAt();
        
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
        
            case PROP_ID_code:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_code));
               }
               setCode(typedValue);
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
        
            case PROP_ID_formatId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_formatId));
               }
               setFormatId(typedValue);
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
        
            case PROP_ID_state:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_state));
               }
               setState(typedValue);
               break;
            }
        
            case PROP_ID_blockingLevel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_blockingLevel));
               }
               setBlockingLevel(typedValue);
               break;
            }
        
            case PROP_ID_error:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_error));
               }
               setError(typedValue);
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
        
            case PROP_ID_attachmentFileId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_attachmentFileId));
               }
               setAttachmentFileId(typedValue);
               break;
            }
        
            case PROP_ID_sentAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_sentAt));
               }
               setSentAt(typedValue);
               break;
            }
        
            case PROP_ID_acknowledgedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_acknowledgedAt));
               }
               setAcknowledgedAt(typedValue);
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
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_formatId:{
               onInitProp(propId);
               this._formatId = (java.lang.Long)value;
               
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
        
            case PROP_ID_state:{
               onInitProp(propId);
               this._state = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_blockingLevel:{
               onInitProp(propId);
               this._blockingLevel = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_error:{
               onInitProp(propId);
               this._error = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_retryCount:{
               onInitProp(propId);
               this._retryCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_attachmentFileId:{
               onInitProp(propId);
               this._attachmentFileId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sentAt:{
               onInitProp(propId);
               this._sentAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_acknowledgedAt:{
               onInitProp(propId);
               this._acknowledgedAt = (java.time.LocalDateTime)value;
               
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
     * 事务编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 事务编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
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
     * EDI 格式: FORMAT_ID
     */
    public final java.lang.Long getFormatId(){
         onPropGet(PROP_ID_formatId);
         return _formatId;
    }

    /**
     * EDI 格式: FORMAT_ID
     */
    public final void setFormatId(java.lang.Long value){
        if(onPropSet(PROP_ID_formatId,value)){
            this._formatId = value;
            internalClearRefs(PROP_ID_formatId);
            
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
     * 状态: STATE
     */
    public final java.lang.String getState(){
         onPropGet(PROP_ID_state);
         return _state;
    }

    /**
     * 状态: STATE
     */
    public final void setState(java.lang.String value){
        if(onPropSet(PROP_ID_state,value)){
            this._state = value;
            internalClearRefs(PROP_ID_state);
            
        }
    }
    
    /**
     * 阻断级别: BLOCKING_LEVEL
     */
    public final java.lang.String getBlockingLevel(){
         onPropGet(PROP_ID_blockingLevel);
         return _blockingLevel;
    }

    /**
     * 阻断级别: BLOCKING_LEVEL
     */
    public final void setBlockingLevel(java.lang.String value){
        if(onPropSet(PROP_ID_blockingLevel,value)){
            this._blockingLevel = value;
            internalClearRefs(PROP_ID_blockingLevel);
            
        }
    }
    
    /**
     * 错误信息: ERROR
     */
    public final java.lang.String getError(){
         onPropGet(PROP_ID_error);
         return _error;
    }

    /**
     * 错误信息: ERROR
     */
    public final void setError(java.lang.String value){
        if(onPropSet(PROP_ID_error,value)){
            this._error = value;
            internalClearRefs(PROP_ID_error);
            
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
     * 附件（EDI 报文文件）: ATTACHMENT_FILE_ID
     */
    public final java.lang.String getAttachmentFileId(){
         onPropGet(PROP_ID_attachmentFileId);
         return _attachmentFileId;
    }

    /**
     * 附件（EDI 报文文件）: ATTACHMENT_FILE_ID
     */
    public final void setAttachmentFileId(java.lang.String value){
        if(onPropSet(PROP_ID_attachmentFileId,value)){
            this._attachmentFileId = value;
            internalClearRefs(PROP_ID_attachmentFileId);
            
        }
    }
    
    /**
     * 发送时间: SENT_AT
     */
    public final java.time.LocalDateTime getSentAt(){
         onPropGet(PROP_ID_sentAt);
         return _sentAt;
    }

    /**
     * 发送时间: SENT_AT
     */
    public final void setSentAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_sentAt,value)){
            this._sentAt = value;
            internalClearRefs(PROP_ID_sentAt);
            
        }
    }
    
    /**
     * 确认时间: ACKNOWLEDGED_AT
     */
    public final java.time.LocalDateTime getAcknowledgedAt(){
         onPropGet(PROP_ID_acknowledgedAt);
         return _acknowledgedAt;
    }

    /**
     * 确认时间: ACKNOWLEDGED_AT
     */
    public final void setAcknowledgedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_acknowledgedAt,value)){
            this._acknowledgedAt = value;
            internalClearRefs(PROP_ID_acknowledgedAt);
            
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
    public final app.erp.b2b.dao.entity.ErpB2bEdiFormat getFormat(){
       return (app.erp.b2b.dao.entity.ErpB2bEdiFormat)internalGetRefEntity(PROP_NAME_format);
    }

    public final void setFormat(app.erp.b2b.dao.entity.ErpB2bEdiFormat refEntity){
   
           if(refEntity == null){
           
                   this.setFormatId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_format, refEntity,()->{
           
                           this.setFormatId(refEntity.getId());
                       
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
       
   private io.nop.orm.component.OrmFileComponent _attachmentFileIdComponent;

   private static Map<String,Integer> COMPONENT_PROP_ID_MAP_attachmentFileIdComponent = new HashMap<>();
   static{
      
         COMPONENT_PROP_ID_MAP_attachmentFileIdComponent.put(io.nop.orm.component.OrmFileComponent.PROP_NAME_filePath,PROP_ID_attachmentFileId);
      
   }

   public final io.nop.orm.component.OrmFileComponent getAttachmentFileIdComponent(){
      if(_attachmentFileIdComponent == null){
          _attachmentFileIdComponent = new io.nop.orm.component.OrmFileComponent();
          _attachmentFileIdComponent.bindToEntity(this, COMPONENT_PROP_ID_MAP_attachmentFileIdComponent);
      }
      return _attachmentFileIdComponent;
   }

}
// resume CPD analysis - CPD-ON
