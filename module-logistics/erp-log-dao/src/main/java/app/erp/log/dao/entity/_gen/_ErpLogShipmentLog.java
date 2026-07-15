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

import app.erp.log.dao.entity.ErpLogShipmentLog;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  网关交互日志: erp_log_shipment_log
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpLogShipmentLog extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 发运单ID: SHIPMENT_ID BIGINT */
    public static final String PROP_NAME_shipmentId = "shipmentId";
    public static final int PROP_ID_shipmentId = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 网关标识: GATEWAY_ID VARCHAR */
    public static final String PROP_NAME_gatewayId = "gatewayId";
    public static final int PROP_ID_gatewayId = 4;
    
    /* 操作类型: ACTION_TYPE VARCHAR */
    public static final String PROP_NAME_actionType = "actionType";
    public static final int PROP_ID_actionType = 5;
    
    /* 请求报文: REQUEST_BODY VARCHAR */
    public static final String PROP_NAME_requestBody = "requestBody";
    public static final int PROP_ID_requestBody = 6;
    
    /* 响应报文: RESPONSE_BODY VARCHAR */
    public static final String PROP_NAME_responseBody = "responseBody";
    public static final int PROP_ID_responseBody = 7;
    
    /* HTTP状态码: HTTP_STATUS INTEGER */
    public static final String PROP_NAME_httpStatus = "httpStatus";
    public static final int PROP_ID_httpStatus = 8;
    
    /* 错误码: ERROR_CODE VARCHAR */
    public static final String PROP_NAME_errorCode = "errorCode";
    public static final int PROP_ID_errorCode = 9;
    
    /* 错误信息: ERROR_MESSAGE VARCHAR */
    public static final String PROP_NAME_errorMessage = "errorMessage";
    public static final int PROP_ID_errorMessage = 10;
    
    /* 是否成功: IS_SUCCESS BOOLEAN */
    public static final String PROP_NAME_isSuccess = "isSuccess";
    public static final int PROP_ID_isSuccess = 11;
    
    /* 执行时间: EXECUTED_AT TIMESTAMP */
    public static final String PROP_NAME_executedAt = "executedAt";
    public static final int PROP_ID_executedAt = 12;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 13;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 14;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 15;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 16;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 17;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 18;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 19;
    

    private static int _PROP_ID_BOUND = 20;

    
    /* relation:  */
    public static final String PROP_NAME_shipment = "shipment";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_shipmentId] = PROP_NAME_shipmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_shipmentId, PROP_ID_shipmentId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_gatewayId] = PROP_NAME_gatewayId;
          PROP_NAME_TO_ID.put(PROP_NAME_gatewayId, PROP_ID_gatewayId);
      
          PROP_ID_TO_NAME[PROP_ID_actionType] = PROP_NAME_actionType;
          PROP_NAME_TO_ID.put(PROP_NAME_actionType, PROP_ID_actionType);
      
          PROP_ID_TO_NAME[PROP_ID_requestBody] = PROP_NAME_requestBody;
          PROP_NAME_TO_ID.put(PROP_NAME_requestBody, PROP_ID_requestBody);
      
          PROP_ID_TO_NAME[PROP_ID_responseBody] = PROP_NAME_responseBody;
          PROP_NAME_TO_ID.put(PROP_NAME_responseBody, PROP_ID_responseBody);
      
          PROP_ID_TO_NAME[PROP_ID_httpStatus] = PROP_NAME_httpStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_httpStatus, PROP_ID_httpStatus);
      
          PROP_ID_TO_NAME[PROP_ID_errorCode] = PROP_NAME_errorCode;
          PROP_NAME_TO_ID.put(PROP_NAME_errorCode, PROP_ID_errorCode);
      
          PROP_ID_TO_NAME[PROP_ID_errorMessage] = PROP_NAME_errorMessage;
          PROP_NAME_TO_ID.put(PROP_NAME_errorMessage, PROP_ID_errorMessage);
      
          PROP_ID_TO_NAME[PROP_ID_isSuccess] = PROP_NAME_isSuccess;
          PROP_NAME_TO_ID.put(PROP_NAME_isSuccess, PROP_ID_isSuccess);
      
          PROP_ID_TO_NAME[PROP_ID_executedAt] = PROP_NAME_executedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_executedAt, PROP_ID_executedAt);
      
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
    
    /* 发运单ID: SHIPMENT_ID */
    private java.lang.Long _shipmentId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 网关标识: GATEWAY_ID */
    private java.lang.String _gatewayId;
    
    /* 操作类型: ACTION_TYPE */
    private java.lang.String _actionType;
    
    /* 请求报文: REQUEST_BODY */
    private java.lang.String _requestBody;
    
    /* 响应报文: RESPONSE_BODY */
    private java.lang.String _responseBody;
    
    /* HTTP状态码: HTTP_STATUS */
    private java.lang.Integer _httpStatus;
    
    /* 错误码: ERROR_CODE */
    private java.lang.String _errorCode;
    
    /* 错误信息: ERROR_MESSAGE */
    private java.lang.String _errorMessage;
    
    /* 是否成功: IS_SUCCESS */
    private java.lang.Boolean _isSuccess;
    
    /* 执行时间: EXECUTED_AT */
    private java.sql.Timestamp _executedAt;
    
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
    

    public _ErpLogShipmentLog(){
        // for debug
    }

    protected ErpLogShipmentLog newInstance(){
        ErpLogShipmentLog entity = new ErpLogShipmentLog();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpLogShipmentLog cloneInstance() {
        ErpLogShipmentLog entity = newInstance();
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
      return "app.erp.log.dao.entity.ErpLogShipmentLog";
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
        
            case PROP_ID_shipmentId:
               return getShipmentId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_gatewayId:
               return getGatewayId();
        
            case PROP_ID_actionType:
               return getActionType();
        
            case PROP_ID_requestBody:
               return getRequestBody();
        
            case PROP_ID_responseBody:
               return getResponseBody();
        
            case PROP_ID_httpStatus:
               return getHttpStatus();
        
            case PROP_ID_errorCode:
               return getErrorCode();
        
            case PROP_ID_errorMessage:
               return getErrorMessage();
        
            case PROP_ID_isSuccess:
               return getIsSuccess();
        
            case PROP_ID_executedAt:
               return getExecutedAt();
        
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
        
            case PROP_ID_shipmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_shipmentId));
               }
               setShipmentId(typedValue);
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
        
            case PROP_ID_gatewayId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_gatewayId));
               }
               setGatewayId(typedValue);
               break;
            }
        
            case PROP_ID_actionType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_actionType));
               }
               setActionType(typedValue);
               break;
            }
        
            case PROP_ID_requestBody:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_requestBody));
               }
               setRequestBody(typedValue);
               break;
            }
        
            case PROP_ID_responseBody:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_responseBody));
               }
               setResponseBody(typedValue);
               break;
            }
        
            case PROP_ID_httpStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_httpStatus));
               }
               setHttpStatus(typedValue);
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
        
            case PROP_ID_errorMessage:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_errorMessage));
               }
               setErrorMessage(typedValue);
               break;
            }
        
            case PROP_ID_isSuccess:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isSuccess));
               }
               setIsSuccess(typedValue);
               break;
            }
        
            case PROP_ID_executedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_executedAt));
               }
               setExecutedAt(typedValue);
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
        
            case PROP_ID_shipmentId:{
               onInitProp(propId);
               this._shipmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_gatewayId:{
               onInitProp(propId);
               this._gatewayId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_actionType:{
               onInitProp(propId);
               this._actionType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_requestBody:{
               onInitProp(propId);
               this._requestBody = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_responseBody:{
               onInitProp(propId);
               this._responseBody = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_httpStatus:{
               onInitProp(propId);
               this._httpStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_errorCode:{
               onInitProp(propId);
               this._errorCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_errorMessage:{
               onInitProp(propId);
               this._errorMessage = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_isSuccess:{
               onInitProp(propId);
               this._isSuccess = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_executedAt:{
               onInitProp(propId);
               this._executedAt = (java.sql.Timestamp)value;
               
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
     * 发运单ID: SHIPMENT_ID
     */
    public final java.lang.Long getShipmentId(){
         onPropGet(PROP_ID_shipmentId);
         return _shipmentId;
    }

    /**
     * 发运单ID: SHIPMENT_ID
     */
    public final void setShipmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_shipmentId,value)){
            this._shipmentId = value;
            internalClearRefs(PROP_ID_shipmentId);
            
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
     * 网关标识: GATEWAY_ID
     */
    public final java.lang.String getGatewayId(){
         onPropGet(PROP_ID_gatewayId);
         return _gatewayId;
    }

    /**
     * 网关标识: GATEWAY_ID
     */
    public final void setGatewayId(java.lang.String value){
        if(onPropSet(PROP_ID_gatewayId,value)){
            this._gatewayId = value;
            internalClearRefs(PROP_ID_gatewayId);
            
        }
    }
    
    /**
     * 操作类型: ACTION_TYPE
     */
    public final java.lang.String getActionType(){
         onPropGet(PROP_ID_actionType);
         return _actionType;
    }

    /**
     * 操作类型: ACTION_TYPE
     */
    public final void setActionType(java.lang.String value){
        if(onPropSet(PROP_ID_actionType,value)){
            this._actionType = value;
            internalClearRefs(PROP_ID_actionType);
            
        }
    }
    
    /**
     * 请求报文: REQUEST_BODY
     */
    public final java.lang.String getRequestBody(){
         onPropGet(PROP_ID_requestBody);
         return _requestBody;
    }

    /**
     * 请求报文: REQUEST_BODY
     */
    public final void setRequestBody(java.lang.String value){
        if(onPropSet(PROP_ID_requestBody,value)){
            this._requestBody = value;
            internalClearRefs(PROP_ID_requestBody);
            
        }
    }
    
    /**
     * 响应报文: RESPONSE_BODY
     */
    public final java.lang.String getResponseBody(){
         onPropGet(PROP_ID_responseBody);
         return _responseBody;
    }

    /**
     * 响应报文: RESPONSE_BODY
     */
    public final void setResponseBody(java.lang.String value){
        if(onPropSet(PROP_ID_responseBody,value)){
            this._responseBody = value;
            internalClearRefs(PROP_ID_responseBody);
            
        }
    }
    
    /**
     * HTTP状态码: HTTP_STATUS
     */
    public final java.lang.Integer getHttpStatus(){
         onPropGet(PROP_ID_httpStatus);
         return _httpStatus;
    }

    /**
     * HTTP状态码: HTTP_STATUS
     */
    public final void setHttpStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_httpStatus,value)){
            this._httpStatus = value;
            internalClearRefs(PROP_ID_httpStatus);
            
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
     * 错误信息: ERROR_MESSAGE
     */
    public final java.lang.String getErrorMessage(){
         onPropGet(PROP_ID_errorMessage);
         return _errorMessage;
    }

    /**
     * 错误信息: ERROR_MESSAGE
     */
    public final void setErrorMessage(java.lang.String value){
        if(onPropSet(PROP_ID_errorMessage,value)){
            this._errorMessage = value;
            internalClearRefs(PROP_ID_errorMessage);
            
        }
    }
    
    /**
     * 是否成功: IS_SUCCESS
     */
    public final java.lang.Boolean getIsSuccess(){
         onPropGet(PROP_ID_isSuccess);
         return _isSuccess;
    }

    /**
     * 是否成功: IS_SUCCESS
     */
    public final void setIsSuccess(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isSuccess,value)){
            this._isSuccess = value;
            internalClearRefs(PROP_ID_isSuccess);
            
        }
    }
    
    /**
     * 执行时间: EXECUTED_AT
     */
    public final java.sql.Timestamp getExecutedAt(){
         onPropGet(PROP_ID_executedAt);
         return _executedAt;
    }

    /**
     * 执行时间: EXECUTED_AT
     */
    public final void setExecutedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_executedAt,value)){
            this._executedAt = value;
            internalClearRefs(PROP_ID_executedAt);
            
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
    public final app.erp.log.dao.entity.ErpLogShipment getShipment(){
       return (app.erp.log.dao.entity.ErpLogShipment)internalGetRefEntity(PROP_NAME_shipment);
    }

    public final void setShipment(app.erp.log.dao.entity.ErpLogShipment refEntity){
   
           if(refEntity == null){
           
                   this.setShipmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_shipment, refEntity,()->{
           
                           this.setShipmentId(refEntity.getId());
                       
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
