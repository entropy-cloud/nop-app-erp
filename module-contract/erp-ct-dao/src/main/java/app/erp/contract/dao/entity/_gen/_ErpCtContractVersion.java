package app.erp.contract.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.contract.dao.entity.ErpCtContractVersion;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  合同版本: erp_ct_contract_version
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpCtContractVersion extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 合同ID: CONTRACT_ID BIGINT */
    public static final String PROP_NAME_contractId = "contractId";
    public static final int PROP_ID_contractId = 2;
    
    /* 版本号: VERSION_NO INTEGER */
    public static final String PROP_NAME_versionNo = "versionNo";
    public static final int PROP_ID_versionNo = 3;
    
    /* 版本日期: VERSION_DATE DATE */
    public static final String PROP_NAME_versionDate = "versionDate";
    public static final int PROP_ID_versionDate = 4;
    
    /* 版本内容: CONTENT VARCHAR */
    public static final String PROP_NAME_content = "content";
    public static final int PROP_ID_content = 5;
    
    /* 版本附件: ATTACHMENT_FILE_ID VARCHAR */
    public static final String PROP_NAME_attachmentFileId = "attachmentFileId";
    public static final int PROP_ID_attachmentFileId = 6;
    
    /* 是否当前版本: IS_CURRENT BOOLEAN */
    public static final String PROP_NAME_isCurrent = "isCurrent";
    public static final int PROP_ID_isCurrent = 7;
    
    /* 版本状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 8;
    
    /* 批准人: APPROVED_BY VARCHAR */
    public static final String PROP_NAME_approvedBy = "approvedBy";
    public static final int PROP_ID_approvedBy = 9;
    
    /* 批准时间: APPROVED_AT DATETIME */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 10;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 11;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 12;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 13;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 14;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 15;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 16;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 17;
    

    private static int _PROP_ID_BOUND = 18;

    
    /* relation:  */
    public static final String PROP_NAME_contract = "contract";
    
    /* component:  */
    public static final String PROP_NAME_attachmentFileIdComponent = "attachmentFileIdComponent";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_contractId] = PROP_NAME_contractId;
          PROP_NAME_TO_ID.put(PROP_NAME_contractId, PROP_ID_contractId);
      
          PROP_ID_TO_NAME[PROP_ID_versionNo] = PROP_NAME_versionNo;
          PROP_NAME_TO_ID.put(PROP_NAME_versionNo, PROP_ID_versionNo);
      
          PROP_ID_TO_NAME[PROP_ID_versionDate] = PROP_NAME_versionDate;
          PROP_NAME_TO_ID.put(PROP_NAME_versionDate, PROP_ID_versionDate);
      
          PROP_ID_TO_NAME[PROP_ID_content] = PROP_NAME_content;
          PROP_NAME_TO_ID.put(PROP_NAME_content, PROP_ID_content);
      
          PROP_ID_TO_NAME[PROP_ID_attachmentFileId] = PROP_NAME_attachmentFileId;
          PROP_NAME_TO_ID.put(PROP_NAME_attachmentFileId, PROP_ID_attachmentFileId);
      
          PROP_ID_TO_NAME[PROP_ID_isCurrent] = PROP_NAME_isCurrent;
          PROP_NAME_TO_ID.put(PROP_NAME_isCurrent, PROP_ID_isCurrent);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_approvedBy] = PROP_NAME_approvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedBy, PROP_ID_approvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
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
    
    /* 合同ID: CONTRACT_ID */
    private java.lang.Long _contractId;
    
    /* 版本号: VERSION_NO */
    private java.lang.Integer _versionNo;
    
    /* 版本日期: VERSION_DATE */
    private java.time.LocalDate _versionDate;
    
    /* 版本内容: CONTENT */
    private java.lang.String _content;
    
    /* 版本附件: ATTACHMENT_FILE_ID */
    private java.lang.String _attachmentFileId;
    
    /* 是否当前版本: IS_CURRENT */
    private java.lang.Boolean _isCurrent;
    
    /* 版本状态: STATUS */
    private java.lang.String _status;
    
    /* 批准人: APPROVED_BY */
    private java.lang.String _approvedBy;
    
    /* 批准时间: APPROVED_AT */
    private java.time.LocalDateTime _approvedAt;
    
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
    

    public _ErpCtContractVersion(){
        // for debug
    }

    protected ErpCtContractVersion newInstance(){
        ErpCtContractVersion entity = new ErpCtContractVersion();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpCtContractVersion cloneInstance() {
        ErpCtContractVersion entity = newInstance();
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
      return "app.erp.contract.dao.entity.ErpCtContractVersion";
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
        
            case PROP_ID_contractId:
               return getContractId();
        
            case PROP_ID_versionNo:
               return getVersionNo();
        
            case PROP_ID_versionDate:
               return getVersionDate();
        
            case PROP_ID_content:
               return getContent();
        
            case PROP_ID_attachmentFileId:
               return getAttachmentFileId();
        
            case PROP_ID_isCurrent:
               return getIsCurrent();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_approvedBy:
               return getApprovedBy();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
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
        
            case PROP_ID_contractId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_contractId));
               }
               setContractId(typedValue);
               break;
            }
        
            case PROP_ID_versionNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_versionNo));
               }
               setVersionNo(typedValue);
               break;
            }
        
            case PROP_ID_versionDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_versionDate));
               }
               setVersionDate(typedValue);
               break;
            }
        
            case PROP_ID_content:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_content));
               }
               setContent(typedValue);
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
        
            case PROP_ID_isCurrent:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isCurrent));
               }
               setIsCurrent(typedValue);
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
        
            case PROP_ID_approvedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approvedBy));
               }
               setApprovedBy(typedValue);
               break;
            }
        
            case PROP_ID_approvedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_approvedAt));
               }
               setApprovedAt(typedValue);
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
        
            case PROP_ID_contractId:{
               onInitProp(propId);
               this._contractId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_versionNo:{
               onInitProp(propId);
               this._versionNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_versionDate:{
               onInitProp(propId);
               this._versionDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_content:{
               onInitProp(propId);
               this._content = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_attachmentFileId:{
               onInitProp(propId);
               this._attachmentFileId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isCurrent:{
               onInitProp(propId);
               this._isCurrent = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedBy:{
               onInitProp(propId);
               this._approvedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.time.LocalDateTime)value;
               
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
     * 合同ID: CONTRACT_ID
     */
    public final java.lang.Long getContractId(){
         onPropGet(PROP_ID_contractId);
         return _contractId;
    }

    /**
     * 合同ID: CONTRACT_ID
     */
    public final void setContractId(java.lang.Long value){
        if(onPropSet(PROP_ID_contractId,value)){
            this._contractId = value;
            internalClearRefs(PROP_ID_contractId);
            
        }
    }
    
    /**
     * 版本号: VERSION_NO
     */
    public final java.lang.Integer getVersionNo(){
         onPropGet(PROP_ID_versionNo);
         return _versionNo;
    }

    /**
     * 版本号: VERSION_NO
     */
    public final void setVersionNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_versionNo,value)){
            this._versionNo = value;
            internalClearRefs(PROP_ID_versionNo);
            
        }
    }
    
    /**
     * 版本日期: VERSION_DATE
     */
    public final java.time.LocalDate getVersionDate(){
         onPropGet(PROP_ID_versionDate);
         return _versionDate;
    }

    /**
     * 版本日期: VERSION_DATE
     */
    public final void setVersionDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_versionDate,value)){
            this._versionDate = value;
            internalClearRefs(PROP_ID_versionDate);
            
        }
    }
    
    /**
     * 版本内容: CONTENT
     */
    public final java.lang.String getContent(){
         onPropGet(PROP_ID_content);
         return _content;
    }

    /**
     * 版本内容: CONTENT
     */
    public final void setContent(java.lang.String value){
        if(onPropSet(PROP_ID_content,value)){
            this._content = value;
            internalClearRefs(PROP_ID_content);
            
        }
    }
    
    /**
     * 版本附件: ATTACHMENT_FILE_ID
     */
    public final java.lang.String getAttachmentFileId(){
         onPropGet(PROP_ID_attachmentFileId);
         return _attachmentFileId;
    }

    /**
     * 版本附件: ATTACHMENT_FILE_ID
     */
    public final void setAttachmentFileId(java.lang.String value){
        if(onPropSet(PROP_ID_attachmentFileId,value)){
            this._attachmentFileId = value;
            internalClearRefs(PROP_ID_attachmentFileId);
            
        }
    }
    
    /**
     * 是否当前版本: IS_CURRENT
     */
    public final java.lang.Boolean getIsCurrent(){
         onPropGet(PROP_ID_isCurrent);
         return _isCurrent;
    }

    /**
     * 是否当前版本: IS_CURRENT
     */
    public final void setIsCurrent(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isCurrent,value)){
            this._isCurrent = value;
            internalClearRefs(PROP_ID_isCurrent);
            
        }
    }
    
    /**
     * 版本状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 版本状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 批准人: APPROVED_BY
     */
    public final java.lang.String getApprovedBy(){
         onPropGet(PROP_ID_approvedBy);
         return _approvedBy;
    }

    /**
     * 批准人: APPROVED_BY
     */
    public final void setApprovedBy(java.lang.String value){
        if(onPropSet(PROP_ID_approvedBy,value)){
            this._approvedBy = value;
            internalClearRefs(PROP_ID_approvedBy);
            
        }
    }
    
    /**
     * 批准时间: APPROVED_AT
     */
    public final java.time.LocalDateTime getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 批准时间: APPROVED_AT
     */
    public final void setApprovedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
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
    public final app.erp.contract.dao.entity.ErpCtContract getContract(){
       return (app.erp.contract.dao.entity.ErpCtContract)internalGetRefEntity(PROP_NAME_contract);
    }

    public final void setContract(app.erp.contract.dao.entity.ErpCtContract refEntity){
   
           if(refEntity == null){
           
                   this.setContractId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_contract, refEntity,()->{
           
                           this.setContractId(refEntity.getId());
                       
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
