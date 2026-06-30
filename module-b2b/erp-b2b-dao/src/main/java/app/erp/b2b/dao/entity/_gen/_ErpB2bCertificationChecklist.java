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

import app.erp.b2b.dao.entity.ErpB2bCertificationChecklist;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  认证检查清单: erp_b2b_certification_checklist
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bCertificationChecklist extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 伙伴档案: PARTNER_PROFILE_ID BIGINT */
    public static final String PROP_NAME_partnerProfileId = "partnerProfileId";
    public static final int PROP_ID_partnerProfileId = 2;
    
    /* 检查项: CHECKLIST_ITEM VARCHAR */
    public static final String PROP_NAME_checklistItem = "checklistItem";
    public static final int PROP_ID_checklistItem = 3;
    
    /* 对应文档类型: REQUIRED_DOC_TYPE VARCHAR */
    public static final String PROP_NAME_requiredDocType = "requiredDocType";
    public static final int PROP_ID_requiredDocType = 4;
    
    /* 必检: IS_MANDATORY BOOLEAN */
    public static final String PROP_NAME_isMandatory = "isMandatory";
    public static final int PROP_ID_isMandatory = 5;
    
    /* 通过: IS_PASSED BOOLEAN */
    public static final String PROP_NAME_isPassed = "isPassed";
    public static final int PROP_ID_isPassed = 6;
    
    /* 检查人: CHECKED_BY VARCHAR */
    public static final String PROP_NAME_checkedBy = "checkedBy";
    public static final int PROP_ID_checkedBy = 7;
    
    /* 检查时间: CHECKED_AT DATETIME */
    public static final String PROP_NAME_checkedAt = "checkedAt";
    public static final int PROP_ID_checkedAt = 8;
    
    /* 检查证据: EVIDENCE VARCHAR */
    public static final String PROP_NAME_evidence = "evidence";
    public static final int PROP_ID_evidence = 9;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 10;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 11;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 12;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 13;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 14;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 15;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 16;
    

    private static int _PROP_ID_BOUND = 17;

    
    /* relation:  */
    public static final String PROP_NAME_partnerProfile = "partnerProfile";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[17];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_partnerProfileId] = PROP_NAME_partnerProfileId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerProfileId, PROP_ID_partnerProfileId);
      
          PROP_ID_TO_NAME[PROP_ID_checklistItem] = PROP_NAME_checklistItem;
          PROP_NAME_TO_ID.put(PROP_NAME_checklistItem, PROP_ID_checklistItem);
      
          PROP_ID_TO_NAME[PROP_ID_requiredDocType] = PROP_NAME_requiredDocType;
          PROP_NAME_TO_ID.put(PROP_NAME_requiredDocType, PROP_ID_requiredDocType);
      
          PROP_ID_TO_NAME[PROP_ID_isMandatory] = PROP_NAME_isMandatory;
          PROP_NAME_TO_ID.put(PROP_NAME_isMandatory, PROP_ID_isMandatory);
      
          PROP_ID_TO_NAME[PROP_ID_isPassed] = PROP_NAME_isPassed;
          PROP_NAME_TO_ID.put(PROP_NAME_isPassed, PROP_ID_isPassed);
      
          PROP_ID_TO_NAME[PROP_ID_checkedBy] = PROP_NAME_checkedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_checkedBy, PROP_ID_checkedBy);
      
          PROP_ID_TO_NAME[PROP_ID_checkedAt] = PROP_NAME_checkedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_checkedAt, PROP_ID_checkedAt);
      
          PROP_ID_TO_NAME[PROP_ID_evidence] = PROP_NAME_evidence;
          PROP_NAME_TO_ID.put(PROP_NAME_evidence, PROP_ID_evidence);
      
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
    
    /* 伙伴档案: PARTNER_PROFILE_ID */
    private java.lang.Long _partnerProfileId;
    
    /* 检查项: CHECKLIST_ITEM */
    private java.lang.String _checklistItem;
    
    /* 对应文档类型: REQUIRED_DOC_TYPE */
    private java.lang.String _requiredDocType;
    
    /* 必检: IS_MANDATORY */
    private java.lang.Boolean _isMandatory;
    
    /* 通过: IS_PASSED */
    private java.lang.Boolean _isPassed;
    
    /* 检查人: CHECKED_BY */
    private java.lang.String _checkedBy;
    
    /* 检查时间: CHECKED_AT */
    private java.time.LocalDateTime _checkedAt;
    
    /* 检查证据: EVIDENCE */
    private java.lang.String _evidence;
    
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
    

    public _ErpB2bCertificationChecklist(){
        // for debug
    }

    protected ErpB2bCertificationChecklist newInstance(){
        ErpB2bCertificationChecklist entity = new ErpB2bCertificationChecklist();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bCertificationChecklist cloneInstance() {
        ErpB2bCertificationChecklist entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bCertificationChecklist";
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
        
            case PROP_ID_partnerProfileId:
               return getPartnerProfileId();
        
            case PROP_ID_checklistItem:
               return getChecklistItem();
        
            case PROP_ID_requiredDocType:
               return getRequiredDocType();
        
            case PROP_ID_isMandatory:
               return getIsMandatory();
        
            case PROP_ID_isPassed:
               return getIsPassed();
        
            case PROP_ID_checkedBy:
               return getCheckedBy();
        
            case PROP_ID_checkedAt:
               return getCheckedAt();
        
            case PROP_ID_evidence:
               return getEvidence();
        
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
        
            case PROP_ID_partnerProfileId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_partnerProfileId));
               }
               setPartnerProfileId(typedValue);
               break;
            }
        
            case PROP_ID_checklistItem:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_checklistItem));
               }
               setChecklistItem(typedValue);
               break;
            }
        
            case PROP_ID_requiredDocType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_requiredDocType));
               }
               setRequiredDocType(typedValue);
               break;
            }
        
            case PROP_ID_isMandatory:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isMandatory));
               }
               setIsMandatory(typedValue);
               break;
            }
        
            case PROP_ID_isPassed:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isPassed));
               }
               setIsPassed(typedValue);
               break;
            }
        
            case PROP_ID_checkedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_checkedBy));
               }
               setCheckedBy(typedValue);
               break;
            }
        
            case PROP_ID_checkedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_checkedAt));
               }
               setCheckedAt(typedValue);
               break;
            }
        
            case PROP_ID_evidence:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_evidence));
               }
               setEvidence(typedValue);
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
        
            case PROP_ID_partnerProfileId:{
               onInitProp(propId);
               this._partnerProfileId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_checklistItem:{
               onInitProp(propId);
               this._checklistItem = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_requiredDocType:{
               onInitProp(propId);
               this._requiredDocType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isMandatory:{
               onInitProp(propId);
               this._isMandatory = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_isPassed:{
               onInitProp(propId);
               this._isPassed = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_checkedBy:{
               onInitProp(propId);
               this._checkedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_checkedAt:{
               onInitProp(propId);
               this._checkedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_evidence:{
               onInitProp(propId);
               this._evidence = (java.lang.String)value;
               
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
     * 伙伴档案: PARTNER_PROFILE_ID
     */
    public final java.lang.Long getPartnerProfileId(){
         onPropGet(PROP_ID_partnerProfileId);
         return _partnerProfileId;
    }

    /**
     * 伙伴档案: PARTNER_PROFILE_ID
     */
    public final void setPartnerProfileId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerProfileId,value)){
            this._partnerProfileId = value;
            internalClearRefs(PROP_ID_partnerProfileId);
            
        }
    }
    
    /**
     * 检查项: CHECKLIST_ITEM
     */
    public final java.lang.String getChecklistItem(){
         onPropGet(PROP_ID_checklistItem);
         return _checklistItem;
    }

    /**
     * 检查项: CHECKLIST_ITEM
     */
    public final void setChecklistItem(java.lang.String value){
        if(onPropSet(PROP_ID_checklistItem,value)){
            this._checklistItem = value;
            internalClearRefs(PROP_ID_checklistItem);
            
        }
    }
    
    /**
     * 对应文档类型: REQUIRED_DOC_TYPE
     */
    public final java.lang.String getRequiredDocType(){
         onPropGet(PROP_ID_requiredDocType);
         return _requiredDocType;
    }

    /**
     * 对应文档类型: REQUIRED_DOC_TYPE
     */
    public final void setRequiredDocType(java.lang.String value){
        if(onPropSet(PROP_ID_requiredDocType,value)){
            this._requiredDocType = value;
            internalClearRefs(PROP_ID_requiredDocType);
            
        }
    }
    
    /**
     * 必检: IS_MANDATORY
     */
    public final java.lang.Boolean getIsMandatory(){
         onPropGet(PROP_ID_isMandatory);
         return _isMandatory;
    }

    /**
     * 必检: IS_MANDATORY
     */
    public final void setIsMandatory(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isMandatory,value)){
            this._isMandatory = value;
            internalClearRefs(PROP_ID_isMandatory);
            
        }
    }
    
    /**
     * 通过: IS_PASSED
     */
    public final java.lang.Boolean getIsPassed(){
         onPropGet(PROP_ID_isPassed);
         return _isPassed;
    }

    /**
     * 通过: IS_PASSED
     */
    public final void setIsPassed(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isPassed,value)){
            this._isPassed = value;
            internalClearRefs(PROP_ID_isPassed);
            
        }
    }
    
    /**
     * 检查人: CHECKED_BY
     */
    public final java.lang.String getCheckedBy(){
         onPropGet(PROP_ID_checkedBy);
         return _checkedBy;
    }

    /**
     * 检查人: CHECKED_BY
     */
    public final void setCheckedBy(java.lang.String value){
        if(onPropSet(PROP_ID_checkedBy,value)){
            this._checkedBy = value;
            internalClearRefs(PROP_ID_checkedBy);
            
        }
    }
    
    /**
     * 检查时间: CHECKED_AT
     */
    public final java.time.LocalDateTime getCheckedAt(){
         onPropGet(PROP_ID_checkedAt);
         return _checkedAt;
    }

    /**
     * 检查时间: CHECKED_AT
     */
    public final void setCheckedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_checkedAt,value)){
            this._checkedAt = value;
            internalClearRefs(PROP_ID_checkedAt);
            
        }
    }
    
    /**
     * 检查证据: EVIDENCE
     */
    public final java.lang.String getEvidence(){
         onPropGet(PROP_ID_evidence);
         return _evidence;
    }

    /**
     * 检查证据: EVIDENCE
     */
    public final void setEvidence(java.lang.String value){
        if(onPropSet(PROP_ID_evidence,value)){
            this._evidence = value;
            internalClearRefs(PROP_ID_evidence);
            
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
    public final app.erp.b2b.dao.entity.ErpB2bPartnerProfile getPartnerProfile(){
       return (app.erp.b2b.dao.entity.ErpB2bPartnerProfile)internalGetRefEntity(PROP_NAME_partnerProfile);
    }

    public final void setPartnerProfile(app.erp.b2b.dao.entity.ErpB2bPartnerProfile refEntity){
   
           if(refEntity == null){
           
                   this.setPartnerProfileId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_partnerProfile, refEntity,()->{
           
                           this.setPartnerProfileId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
