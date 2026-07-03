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

import app.erp.b2b.dao.entity.ErpB2bPartnerCredential;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  伙伴凭证: erp_b2b_partner_credential
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpB2bPartnerCredential extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 伙伴档案: PARTNER_PROFILE_ID BIGINT */
    public static final String PROP_NAME_partnerProfileId = "partnerProfileId";
    public static final int PROP_ID_partnerProfileId = 2;
    
    /* 凭证类型: CREDENTIAL_TYPE VARCHAR */
    public static final String PROP_NAME_credentialType = "credentialType";
    public static final int PROP_ID_credentialType = 3;
    
    /* 凭证标识: CREDENTIAL_KEY VARCHAR */
    public static final String PROP_NAME_credentialKey = "credentialKey";
    public static final int PROP_ID_credentialKey = 4;
    
    /* 凭证值: CREDENTIAL_VALUE VARCHAR */
    public static final String PROP_NAME_credentialValue = "credentialValue";
    public static final int PROP_ID_credentialValue = 5;
    
    /* 颁发日期: ISSUED_AT DATE */
    public static final String PROP_NAME_issuedAt = "issuedAt";
    public static final int PROP_ID_issuedAt = 6;
    
    /* 过期日期: EXPIRES_AT DATE */
    public static final String PROP_NAME_expiresAt = "expiresAt";
    public static final int PROP_ID_expiresAt = 7;
    
    /* 启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 8;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 9;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 10;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 11;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 12;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 13;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 14;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 15;
    

    private static int _PROP_ID_BOUND = 16;

    
    /* relation:  */
    public static final String PROP_NAME_partnerProfile = "partnerProfile";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[16];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_partnerProfileId] = PROP_NAME_partnerProfileId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerProfileId, PROP_ID_partnerProfileId);
      
          PROP_ID_TO_NAME[PROP_ID_credentialType] = PROP_NAME_credentialType;
          PROP_NAME_TO_ID.put(PROP_NAME_credentialType, PROP_ID_credentialType);
      
          PROP_ID_TO_NAME[PROP_ID_credentialKey] = PROP_NAME_credentialKey;
          PROP_NAME_TO_ID.put(PROP_NAME_credentialKey, PROP_ID_credentialKey);
      
          PROP_ID_TO_NAME[PROP_ID_credentialValue] = PROP_NAME_credentialValue;
          PROP_NAME_TO_ID.put(PROP_NAME_credentialValue, PROP_ID_credentialValue);
      
          PROP_ID_TO_NAME[PROP_ID_issuedAt] = PROP_NAME_issuedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_issuedAt, PROP_ID_issuedAt);
      
          PROP_ID_TO_NAME[PROP_ID_expiresAt] = PROP_NAME_expiresAt;
          PROP_NAME_TO_ID.put(PROP_NAME_expiresAt, PROP_ID_expiresAt);
      
          PROP_ID_TO_NAME[PROP_ID_isActive] = PROP_NAME_isActive;
          PROP_NAME_TO_ID.put(PROP_NAME_isActive, PROP_ID_isActive);
      
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
    
    /* 凭证类型: CREDENTIAL_TYPE */
    private java.lang.String _credentialType;
    
    /* 凭证标识: CREDENTIAL_KEY */
    private java.lang.String _credentialKey;
    
    /* 凭证值: CREDENTIAL_VALUE */
    private java.lang.String _credentialValue;
    
    /* 颁发日期: ISSUED_AT */
    private java.time.LocalDate _issuedAt;
    
    /* 过期日期: EXPIRES_AT */
    private java.time.LocalDate _expiresAt;
    
    /* 启用: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
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
    

    public _ErpB2bPartnerCredential(){
        // for debug
    }

    protected ErpB2bPartnerCredential newInstance(){
        ErpB2bPartnerCredential entity = new ErpB2bPartnerCredential();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpB2bPartnerCredential cloneInstance() {
        ErpB2bPartnerCredential entity = newInstance();
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
      return "app.erp.b2b.dao.entity.ErpB2bPartnerCredential";
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
        
            case PROP_ID_credentialType:
               return getCredentialType();
        
            case PROP_ID_credentialKey:
               return getCredentialKey();
        
            case PROP_ID_credentialValue:
               return getCredentialValue();
        
            case PROP_ID_issuedAt:
               return getIssuedAt();
        
            case PROP_ID_expiresAt:
               return getExpiresAt();
        
            case PROP_ID_isActive:
               return getIsActive();
        
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
        
            case PROP_ID_credentialType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_credentialType));
               }
               setCredentialType(typedValue);
               break;
            }
        
            case PROP_ID_credentialKey:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_credentialKey));
               }
               setCredentialKey(typedValue);
               break;
            }
        
            case PROP_ID_credentialValue:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_credentialValue));
               }
               setCredentialValue(typedValue);
               break;
            }
        
            case PROP_ID_issuedAt:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_issuedAt));
               }
               setIssuedAt(typedValue);
               break;
            }
        
            case PROP_ID_expiresAt:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_expiresAt));
               }
               setExpiresAt(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isActive));
               }
               setIsActive(typedValue);
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
        
            case PROP_ID_credentialType:{
               onInitProp(propId);
               this._credentialType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_credentialKey:{
               onInitProp(propId);
               this._credentialKey = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_credentialValue:{
               onInitProp(propId);
               this._credentialValue = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_issuedAt:{
               onInitProp(propId);
               this._issuedAt = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_expiresAt:{
               onInitProp(propId);
               this._expiresAt = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
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
     * 凭证类型: CREDENTIAL_TYPE
     */
    public final java.lang.String getCredentialType(){
         onPropGet(PROP_ID_credentialType);
         return _credentialType;
    }

    /**
     * 凭证类型: CREDENTIAL_TYPE
     */
    public final void setCredentialType(java.lang.String value){
        if(onPropSet(PROP_ID_credentialType,value)){
            this._credentialType = value;
            internalClearRefs(PROP_ID_credentialType);
            
        }
    }
    
    /**
     * 凭证标识: CREDENTIAL_KEY
     */
    public final java.lang.String getCredentialKey(){
         onPropGet(PROP_ID_credentialKey);
         return _credentialKey;
    }

    /**
     * 凭证标识: CREDENTIAL_KEY
     */
    public final void setCredentialKey(java.lang.String value){
        if(onPropSet(PROP_ID_credentialKey,value)){
            this._credentialKey = value;
            internalClearRefs(PROP_ID_credentialKey);
            
        }
    }
    
    /**
     * 凭证值: CREDENTIAL_VALUE
     */
    public final java.lang.String getCredentialValue(){
         onPropGet(PROP_ID_credentialValue);
         return _credentialValue;
    }

    /**
     * 凭证值: CREDENTIAL_VALUE
     */
    public final void setCredentialValue(java.lang.String value){
        if(onPropSet(PROP_ID_credentialValue,value)){
            this._credentialValue = value;
            internalClearRefs(PROP_ID_credentialValue);
            
        }
    }
    
    /**
     * 颁发日期: ISSUED_AT
     */
    public final java.time.LocalDate getIssuedAt(){
         onPropGet(PROP_ID_issuedAt);
         return _issuedAt;
    }

    /**
     * 颁发日期: ISSUED_AT
     */
    public final void setIssuedAt(java.time.LocalDate value){
        if(onPropSet(PROP_ID_issuedAt,value)){
            this._issuedAt = value;
            internalClearRefs(PROP_ID_issuedAt);
            
        }
    }
    
    /**
     * 过期日期: EXPIRES_AT
     */
    public final java.time.LocalDate getExpiresAt(){
         onPropGet(PROP_ID_expiresAt);
         return _expiresAt;
    }

    /**
     * 过期日期: EXPIRES_AT
     */
    public final void setExpiresAt(java.time.LocalDate value){
        if(onPropSet(PROP_ID_expiresAt,value)){
            this._expiresAt = value;
            internalClearRefs(PROP_ID_expiresAt);
            
        }
    }
    
    /**
     * 启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isActive,value)){
            this._isActive = value;
            internalClearRefs(PROP_ID_isActive);
            
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
