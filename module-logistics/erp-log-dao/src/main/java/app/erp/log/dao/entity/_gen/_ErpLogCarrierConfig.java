package app.erp.log.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.log.dao.entity.ErpLogCarrierConfig;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  承运商配置: erp_log_carrier_config
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpLogCarrierConfig extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 承运商ID: CARRIER_ID BIGINT */
    public static final String PROP_NAME_carrierId = "carrierId";
    public static final int PROP_ID_carrierId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 配置编码: CONFIG_CODE VARCHAR */
    public static final String PROP_NAME_configCode = "configCode";
    public static final int PROP_ID_configCode = 4;
    
    /* 服务类型: SERVICE_TYPE VARCHAR */
    public static final String PROP_NAME_serviceType = "serviceType";
    public static final int PROP_ID_serviceType = 5;
    
    /* 接口地址: API_ENDPOINT VARCHAR */
    public static final String PROP_NAME_apiEndpoint = "apiEndpoint";
    public static final int PROP_ID_apiEndpoint = 6;
    
    /* API密钥: API_KEY VARCHAR */
    public static final String PROP_NAME_apiKey = "apiKey";
    public static final int PROP_ID_apiKey = 7;
    
    /* API密钥: API_SECRET VARCHAR */
    public static final String PROP_NAME_apiSecret = "apiSecret";
    public static final int PROP_ID_apiSecret = 8;
    
    /* 完整凭证: CREDENTIALS VARCHAR */
    public static final String PROP_NAME_credentials = "credentials";
    public static final int PROP_ID_credentials = 9;
    
    /* 追踪URL模板: TRACKING_URL_TEMPLATE VARCHAR */
    public static final String PROP_NAME_trackingUrlTemplate = "trackingUrlTemplate";
    public static final int PROP_ID_trackingUrlTemplate = 10;
    
    /* 面单打印格式: PRINT_FORMAT VARCHAR */
    public static final String PROP_NAME_printFormat = "printFormat";
    public static final int PROP_ID_printFormat = 11;
    
    /* 扩展参数: ADDITIONAL_PROPERTIES VARCHAR */
    public static final String PROP_NAME_additionalProperties = "additionalProperties";
    public static final int PROP_ID_additionalProperties = 12;
    
    /* 是否启用: IS_ACTIVE INTEGER */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 13;
    
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
    public static final String PROP_NAME_carrier = "carrier";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[21];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_carrierId] = PROP_NAME_carrierId;
          PROP_NAME_TO_ID.put(PROP_NAME_carrierId, PROP_ID_carrierId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_configCode] = PROP_NAME_configCode;
          PROP_NAME_TO_ID.put(PROP_NAME_configCode, PROP_ID_configCode);
      
          PROP_ID_TO_NAME[PROP_ID_serviceType] = PROP_NAME_serviceType;
          PROP_NAME_TO_ID.put(PROP_NAME_serviceType, PROP_ID_serviceType);
      
          PROP_ID_TO_NAME[PROP_ID_apiEndpoint] = PROP_NAME_apiEndpoint;
          PROP_NAME_TO_ID.put(PROP_NAME_apiEndpoint, PROP_ID_apiEndpoint);
      
          PROP_ID_TO_NAME[PROP_ID_apiKey] = PROP_NAME_apiKey;
          PROP_NAME_TO_ID.put(PROP_NAME_apiKey, PROP_ID_apiKey);
      
          PROP_ID_TO_NAME[PROP_ID_apiSecret] = PROP_NAME_apiSecret;
          PROP_NAME_TO_ID.put(PROP_NAME_apiSecret, PROP_ID_apiSecret);
      
          PROP_ID_TO_NAME[PROP_ID_credentials] = PROP_NAME_credentials;
          PROP_NAME_TO_ID.put(PROP_NAME_credentials, PROP_ID_credentials);
      
          PROP_ID_TO_NAME[PROP_ID_trackingUrlTemplate] = PROP_NAME_trackingUrlTemplate;
          PROP_NAME_TO_ID.put(PROP_NAME_trackingUrlTemplate, PROP_ID_trackingUrlTemplate);
      
          PROP_ID_TO_NAME[PROP_ID_printFormat] = PROP_NAME_printFormat;
          PROP_NAME_TO_ID.put(PROP_NAME_printFormat, PROP_ID_printFormat);
      
          PROP_ID_TO_NAME[PROP_ID_additionalProperties] = PROP_NAME_additionalProperties;
          PROP_NAME_TO_ID.put(PROP_NAME_additionalProperties, PROP_ID_additionalProperties);
      
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
    
    /* 承运商ID: CARRIER_ID */
    private java.lang.Long _carrierId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 配置编码: CONFIG_CODE */
    private java.lang.String _configCode;
    
    /* 服务类型: SERVICE_TYPE */
    private java.lang.String _serviceType;
    
    /* 接口地址: API_ENDPOINT */
    private java.lang.String _apiEndpoint;
    
    /* API密钥: API_KEY */
    private java.lang.String _apiKey;
    
    /* API密钥: API_SECRET */
    private java.lang.String _apiSecret;
    
    /* 完整凭证: CREDENTIALS */
    private java.lang.String _credentials;
    
    /* 追踪URL模板: TRACKING_URL_TEMPLATE */
    private java.lang.String _trackingUrlTemplate;
    
    /* 面单打印格式: PRINT_FORMAT */
    private java.lang.String _printFormat;
    
    /* 扩展参数: ADDITIONAL_PROPERTIES */
    private java.lang.String _additionalProperties;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Integer _isActive;
    
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
    

    public _ErpLogCarrierConfig(){
        // for debug
    }

    protected ErpLogCarrierConfig newInstance(){
        ErpLogCarrierConfig entity = new ErpLogCarrierConfig();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpLogCarrierConfig cloneInstance() {
        ErpLogCarrierConfig entity = newInstance();
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
      return "app.erp.log.dao.entity.ErpLogCarrierConfig";
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
        
            case PROP_ID_carrierId:
               return getCarrierId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_configCode:
               return getConfigCode();
        
            case PROP_ID_serviceType:
               return getServiceType();
        
            case PROP_ID_apiEndpoint:
               return getApiEndpoint();
        
            case PROP_ID_apiKey:
               return getApiKey();
        
            case PROP_ID_apiSecret:
               return getApiSecret();
        
            case PROP_ID_credentials:
               return getCredentials();
        
            case PROP_ID_trackingUrlTemplate:
               return getTrackingUrlTemplate();
        
            case PROP_ID_printFormat:
               return getPrintFormat();
        
            case PROP_ID_additionalProperties:
               return getAdditionalProperties();
        
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
        
            case PROP_ID_carrierId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_carrierId));
               }
               setCarrierId(typedValue);
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
        
            case PROP_ID_configCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_configCode));
               }
               setConfigCode(typedValue);
               break;
            }
        
            case PROP_ID_serviceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_serviceType));
               }
               setServiceType(typedValue);
               break;
            }
        
            case PROP_ID_apiEndpoint:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_apiEndpoint));
               }
               setApiEndpoint(typedValue);
               break;
            }
        
            case PROP_ID_apiKey:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_apiKey));
               }
               setApiKey(typedValue);
               break;
            }
        
            case PROP_ID_apiSecret:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_apiSecret));
               }
               setApiSecret(typedValue);
               break;
            }
        
            case PROP_ID_credentials:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_credentials));
               }
               setCredentials(typedValue);
               break;
            }
        
            case PROP_ID_trackingUrlTemplate:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_trackingUrlTemplate));
               }
               setTrackingUrlTemplate(typedValue);
               break;
            }
        
            case PROP_ID_printFormat:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_printFormat));
               }
               setPrintFormat(typedValue);
               break;
            }
        
            case PROP_ID_additionalProperties:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_additionalProperties));
               }
               setAdditionalProperties(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
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
        
            case PROP_ID_carrierId:{
               onInitProp(propId);
               this._carrierId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_configCode:{
               onInitProp(propId);
               this._configCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_serviceType:{
               onInitProp(propId);
               this._serviceType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_apiEndpoint:{
               onInitProp(propId);
               this._apiEndpoint = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_apiKey:{
               onInitProp(propId);
               this._apiKey = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_apiSecret:{
               onInitProp(propId);
               this._apiSecret = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_credentials:{
               onInitProp(propId);
               this._credentials = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_trackingUrlTemplate:{
               onInitProp(propId);
               this._trackingUrlTemplate = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_printFormat:{
               onInitProp(propId);
               this._printFormat = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_additionalProperties:{
               onInitProp(propId);
               this._additionalProperties = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Integer)value;
               
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
     * 承运商ID: CARRIER_ID
     */
    public final java.lang.Long getCarrierId(){
         onPropGet(PROP_ID_carrierId);
         return _carrierId;
    }

    /**
     * 承运商ID: CARRIER_ID
     */
    public final void setCarrierId(java.lang.Long value){
        if(onPropSet(PROP_ID_carrierId,value)){
            this._carrierId = value;
            internalClearRefs(PROP_ID_carrierId);
            
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
     * 配置编码: CONFIG_CODE
     */
    public final java.lang.String getConfigCode(){
         onPropGet(PROP_ID_configCode);
         return _configCode;
    }

    /**
     * 配置编码: CONFIG_CODE
     */
    public final void setConfigCode(java.lang.String value){
        if(onPropSet(PROP_ID_configCode,value)){
            this._configCode = value;
            internalClearRefs(PROP_ID_configCode);
            
        }
    }
    
    /**
     * 服务类型: SERVICE_TYPE
     */
    public final java.lang.String getServiceType(){
         onPropGet(PROP_ID_serviceType);
         return _serviceType;
    }

    /**
     * 服务类型: SERVICE_TYPE
     */
    public final void setServiceType(java.lang.String value){
        if(onPropSet(PROP_ID_serviceType,value)){
            this._serviceType = value;
            internalClearRefs(PROP_ID_serviceType);
            
        }
    }
    
    /**
     * 接口地址: API_ENDPOINT
     */
    public final java.lang.String getApiEndpoint(){
         onPropGet(PROP_ID_apiEndpoint);
         return _apiEndpoint;
    }

    /**
     * 接口地址: API_ENDPOINT
     */
    public final void setApiEndpoint(java.lang.String value){
        if(onPropSet(PROP_ID_apiEndpoint,value)){
            this._apiEndpoint = value;
            internalClearRefs(PROP_ID_apiEndpoint);
            
        }
    }
    
    /**
     * API密钥: API_KEY
     */
    public final java.lang.String getApiKey(){
         onPropGet(PROP_ID_apiKey);
         return _apiKey;
    }

    /**
     * API密钥: API_KEY
     */
    public final void setApiKey(java.lang.String value){
        if(onPropSet(PROP_ID_apiKey,value)){
            this._apiKey = value;
            internalClearRefs(PROP_ID_apiKey);
            
        }
    }
    
    /**
     * API密钥: API_SECRET
     */
    public final java.lang.String getApiSecret(){
         onPropGet(PROP_ID_apiSecret);
         return _apiSecret;
    }

    /**
     * API密钥: API_SECRET
     */
    public final void setApiSecret(java.lang.String value){
        if(onPropSet(PROP_ID_apiSecret,value)){
            this._apiSecret = value;
            internalClearRefs(PROP_ID_apiSecret);
            
        }
    }
    
    /**
     * 完整凭证: CREDENTIALS
     */
    public final java.lang.String getCredentials(){
         onPropGet(PROP_ID_credentials);
         return _credentials;
    }

    /**
     * 完整凭证: CREDENTIALS
     */
    public final void setCredentials(java.lang.String value){
        if(onPropSet(PROP_ID_credentials,value)){
            this._credentials = value;
            internalClearRefs(PROP_ID_credentials);
            
        }
    }
    
    /**
     * 追踪URL模板: TRACKING_URL_TEMPLATE
     */
    public final java.lang.String getTrackingUrlTemplate(){
         onPropGet(PROP_ID_trackingUrlTemplate);
         return _trackingUrlTemplate;
    }

    /**
     * 追踪URL模板: TRACKING_URL_TEMPLATE
     */
    public final void setTrackingUrlTemplate(java.lang.String value){
        if(onPropSet(PROP_ID_trackingUrlTemplate,value)){
            this._trackingUrlTemplate = value;
            internalClearRefs(PROP_ID_trackingUrlTemplate);
            
        }
    }
    
    /**
     * 面单打印格式: PRINT_FORMAT
     */
    public final java.lang.String getPrintFormat(){
         onPropGet(PROP_ID_printFormat);
         return _printFormat;
    }

    /**
     * 面单打印格式: PRINT_FORMAT
     */
    public final void setPrintFormat(java.lang.String value){
        if(onPropSet(PROP_ID_printFormat,value)){
            this._printFormat = value;
            internalClearRefs(PROP_ID_printFormat);
            
        }
    }
    
    /**
     * 扩展参数: ADDITIONAL_PROPERTIES
     */
    public final java.lang.String getAdditionalProperties(){
         onPropGet(PROP_ID_additionalProperties);
         return _additionalProperties;
    }

    /**
     * 扩展参数: ADDITIONAL_PROPERTIES
     */
    public final void setAdditionalProperties(java.lang.String value){
        if(onPropSet(PROP_ID_additionalProperties,value)){
            this._additionalProperties = value;
            internalClearRefs(PROP_ID_additionalProperties);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Integer getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Integer value){
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
    public final app.erp.log.dao.entity.ErpLogCarrier getCarrier(){
       return (app.erp.log.dao.entity.ErpLogCarrier)internalGetRefEntity(PROP_NAME_carrier);
    }

    public final void setCarrier(app.erp.log.dao.entity.ErpLogCarrier refEntity){
   
           if(refEntity == null){
           
                   this.setCarrierId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_carrier, refEntity,()->{
           
                           this.setCarrierId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
