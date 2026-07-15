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

import app.erp.fin.dao.entity.ErpFinPostingException;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  过账异常记录: erp_fin_posting_exception
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinPostingException extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 追踪ID: TRACE_ID VARCHAR */
    public static final String PROP_NAME_traceId = "traceId";
    public static final int PROP_ID_traceId = 2;
    
    /* 单据编号: BILL_HEAD_CODE VARCHAR */
    public static final String PROP_NAME_billHeadCode = "billHeadCode";
    public static final int PROP_ID_billHeadCode = 3;
    
    /* 业务类型: BUSINESS_TYPE VARCHAR */
    public static final String PROP_NAME_businessType = "businessType";
    public static final int PROP_ID_businessType = 4;
    
    /* 过账类型: POSTING_TYPE VARCHAR */
    public static final String PROP_NAME_postingType = "postingType";
    public static final int PROP_ID_postingType = 5;
    
    /* 错误码: ERROR_CODE VARCHAR */
    public static final String PROP_NAME_errorCode = "errorCode";
    public static final int PROP_ID_errorCode = 6;
    
    /* 错误信息: ERROR_MESSAGE VARCHAR */
    public static final String PROP_NAME_errorMessage = "errorMessage";
    public static final int PROP_ID_errorMessage = 7;
    
    /* 失败阶段: FAILED_STAGE VARCHAR */
    public static final String PROP_NAME_failedStage = "failedStage";
    public static final int PROP_ID_failedStage = 8;
    
    /* 凭证日期: VOUCHER_DATE DATE */
    public static final String PROP_NAME_voucherDate = "voucherDate";
    public static final int PROP_ID_voucherDate = 9;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 10;
    
    /* 账套: ACCT_SCHEMA_ID BIGINT */
    public static final String PROP_NAME_acctSchemaId = "acctSchemaId";
    public static final int PROP_ID_acctSchemaId = 11;
    
    /* 处置状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 12;
    
    /* 重试次数: RETRY_COUNT INTEGER */
    public static final String PROP_NAME_retryCount = "retryCount";
    public static final int PROP_ID_retryCount = 13;
    
    /* 处置动作: RESOLUTION VARCHAR */
    public static final String PROP_NAME_resolution = "resolution";
    public static final int PROP_ID_resolution = 14;
    
    /* 处置说明: RESOLUTION_NOTE VARCHAR */
    public static final String PROP_NAME_resolutionNote = "resolutionNote";
    public static final int PROP_ID_resolutionNote = 15;
    
    /* 处置人: RESOLVED_BY VARCHAR */
    public static final String PROP_NAME_resolvedBy = "resolvedBy";
    public static final int PROP_ID_resolvedBy = 16;
    
    /* 处置时间: RESOLVED_AT TIMESTAMP */
    public static final String PROP_NAME_resolvedAt = "resolvedAt";
    public static final int PROP_ID_resolvedAt = 17;
    
    /* 关联凭证ID: VOUCHER_ID BIGINT */
    public static final String PROP_NAME_voucherId = "voucherId";
    public static final int PROP_ID_voucherId = 18;
    
    /* 发生时间: OCCURRENCE_TIME TIMESTAMP */
    public static final String PROP_NAME_occurrenceTime = "occurrenceTime";
    public static final int PROP_ID_occurrenceTime = 19;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 20;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 21;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 22;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 23;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 24;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 25;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 27;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 28;
    
    /* 原始事件数据(JSON): EVENT_DATA VARCHAR */
    public static final String PROP_NAME_eventData = "eventData";
    public static final int PROP_ID_eventData = 29;
    

    private static int _PROP_ID_BOUND = 30;

    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_acctSchema = "acctSchema";
    
    /* relation:  */
    public static final String PROP_NAME_voucher = "voucher";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[30];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_traceId] = PROP_NAME_traceId;
          PROP_NAME_TO_ID.put(PROP_NAME_traceId, PROP_ID_traceId);
      
          PROP_ID_TO_NAME[PROP_ID_billHeadCode] = PROP_NAME_billHeadCode;
          PROP_NAME_TO_ID.put(PROP_NAME_billHeadCode, PROP_ID_billHeadCode);
      
          PROP_ID_TO_NAME[PROP_ID_businessType] = PROP_NAME_businessType;
          PROP_NAME_TO_ID.put(PROP_NAME_businessType, PROP_ID_businessType);
      
          PROP_ID_TO_NAME[PROP_ID_postingType] = PROP_NAME_postingType;
          PROP_NAME_TO_ID.put(PROP_NAME_postingType, PROP_ID_postingType);
      
          PROP_ID_TO_NAME[PROP_ID_errorCode] = PROP_NAME_errorCode;
          PROP_NAME_TO_ID.put(PROP_NAME_errorCode, PROP_ID_errorCode);
      
          PROP_ID_TO_NAME[PROP_ID_errorMessage] = PROP_NAME_errorMessage;
          PROP_NAME_TO_ID.put(PROP_NAME_errorMessage, PROP_ID_errorMessage);
      
          PROP_ID_TO_NAME[PROP_ID_failedStage] = PROP_NAME_failedStage;
          PROP_NAME_TO_ID.put(PROP_NAME_failedStage, PROP_ID_failedStage);
      
          PROP_ID_TO_NAME[PROP_ID_voucherDate] = PROP_NAME_voucherDate;
          PROP_NAME_TO_ID.put(PROP_NAME_voucherDate, PROP_ID_voucherDate);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_acctSchemaId] = PROP_NAME_acctSchemaId;
          PROP_NAME_TO_ID.put(PROP_NAME_acctSchemaId, PROP_ID_acctSchemaId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_retryCount] = PROP_NAME_retryCount;
          PROP_NAME_TO_ID.put(PROP_NAME_retryCount, PROP_ID_retryCount);
      
          PROP_ID_TO_NAME[PROP_ID_resolution] = PROP_NAME_resolution;
          PROP_NAME_TO_ID.put(PROP_NAME_resolution, PROP_ID_resolution);
      
          PROP_ID_TO_NAME[PROP_ID_resolutionNote] = PROP_NAME_resolutionNote;
          PROP_NAME_TO_ID.put(PROP_NAME_resolutionNote, PROP_ID_resolutionNote);
      
          PROP_ID_TO_NAME[PROP_ID_resolvedBy] = PROP_NAME_resolvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_resolvedBy, PROP_ID_resolvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_resolvedAt] = PROP_NAME_resolvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_resolvedAt, PROP_ID_resolvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_voucherId] = PROP_NAME_voucherId;
          PROP_NAME_TO_ID.put(PROP_NAME_voucherId, PROP_ID_voucherId);
      
          PROP_ID_TO_NAME[PROP_ID_occurrenceTime] = PROP_NAME_occurrenceTime;
          PROP_NAME_TO_ID.put(PROP_NAME_occurrenceTime, PROP_ID_occurrenceTime);
      
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
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_eventData] = PROP_NAME_eventData;
          PROP_NAME_TO_ID.put(PROP_NAME_eventData, PROP_ID_eventData);
      
    }

    
    /* ID: ID */
    private java.lang.Long _id;
    
    /* 追踪ID: TRACE_ID */
    private java.lang.String _traceId;
    
    /* 单据编号: BILL_HEAD_CODE */
    private java.lang.String _billHeadCode;
    
    /* 业务类型: BUSINESS_TYPE */
    private java.lang.String _businessType;
    
    /* 过账类型: POSTING_TYPE */
    private java.lang.String _postingType;
    
    /* 错误码: ERROR_CODE */
    private java.lang.String _errorCode;
    
    /* 错误信息: ERROR_MESSAGE */
    private java.lang.String _errorMessage;
    
    /* 失败阶段: FAILED_STAGE */
    private java.lang.String _failedStage;
    
    /* 凭证日期: VOUCHER_DATE */
    private java.time.LocalDate _voucherDate;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 账套: ACCT_SCHEMA_ID */
    private java.lang.Long _acctSchemaId;
    
    /* 处置状态: STATUS */
    private java.lang.String _status;
    
    /* 重试次数: RETRY_COUNT */
    private java.lang.Integer _retryCount;
    
    /* 处置动作: RESOLUTION */
    private java.lang.String _resolution;
    
    /* 处置说明: RESOLUTION_NOTE */
    private java.lang.String _resolutionNote;
    
    /* 处置人: RESOLVED_BY */
    private java.lang.String _resolvedBy;
    
    /* 处置时间: RESOLVED_AT */
    private java.sql.Timestamp _resolvedAt;
    
    /* 关联凭证ID: VOUCHER_ID */
    private java.lang.Long _voucherId;
    
    /* 发生时间: OCCURRENCE_TIME */
    private java.sql.Timestamp _occurrenceTime;
    
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
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 原始事件数据(JSON): EVENT_DATA */
    private java.lang.String _eventData;
    

    public _ErpFinPostingException(){
        // for debug
    }

    protected ErpFinPostingException newInstance(){
        ErpFinPostingException entity = new ErpFinPostingException();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinPostingException cloneInstance() {
        ErpFinPostingException entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinPostingException";
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
        
            case PROP_ID_traceId:
               return getTraceId();
        
            case PROP_ID_billHeadCode:
               return getBillHeadCode();
        
            case PROP_ID_businessType:
               return getBusinessType();
        
            case PROP_ID_postingType:
               return getPostingType();
        
            case PROP_ID_errorCode:
               return getErrorCode();
        
            case PROP_ID_errorMessage:
               return getErrorMessage();
        
            case PROP_ID_failedStage:
               return getFailedStage();
        
            case PROP_ID_voucherDate:
               return getVoucherDate();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_acctSchemaId:
               return getAcctSchemaId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_retryCount:
               return getRetryCount();
        
            case PROP_ID_resolution:
               return getResolution();
        
            case PROP_ID_resolutionNote:
               return getResolutionNote();
        
            case PROP_ID_resolvedBy:
               return getResolvedBy();
        
            case PROP_ID_resolvedAt:
               return getResolvedAt();
        
            case PROP_ID_voucherId:
               return getVoucherId();
        
            case PROP_ID_occurrenceTime:
               return getOccurrenceTime();
        
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
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_eventData:
               return getEventData();
        
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
        
            case PROP_ID_traceId:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_traceId));
               }
               setTraceId(typedValue);
               break;
            }
        
            case PROP_ID_billHeadCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_billHeadCode));
               }
               setBillHeadCode(typedValue);
               break;
            }
        
            case PROP_ID_businessType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_businessType));
               }
               setBusinessType(typedValue);
               break;
            }
        
            case PROP_ID_postingType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postingType));
               }
               setPostingType(typedValue);
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
        
            case PROP_ID_failedStage:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_failedStage));
               }
               setFailedStage(typedValue);
               break;
            }
        
            case PROP_ID_voucherDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_voucherDate));
               }
               setVoucherDate(typedValue);
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
        
            case PROP_ID_acctSchemaId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_acctSchemaId));
               }
               setAcctSchemaId(typedValue);
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
        
            case PROP_ID_retryCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_retryCount));
               }
               setRetryCount(typedValue);
               break;
            }
        
            case PROP_ID_resolution:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_resolution));
               }
               setResolution(typedValue);
               break;
            }
        
            case PROP_ID_resolutionNote:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_resolutionNote));
               }
               setResolutionNote(typedValue);
               break;
            }
        
            case PROP_ID_resolvedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_resolvedBy));
               }
               setResolvedBy(typedValue);
               break;
            }
        
            case PROP_ID_resolvedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_resolvedAt));
               }
               setResolvedAt(typedValue);
               break;
            }
        
            case PROP_ID_voucherId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_voucherId));
               }
               setVoucherId(typedValue);
               break;
            }
        
            case PROP_ID_occurrenceTime:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_occurrenceTime));
               }
               setOccurrenceTime(typedValue);
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
        
            case PROP_ID_currencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_currencyId));
               }
               setCurrencyId(typedValue);
               break;
            }
        
            case PROP_ID_exchangeRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_eventData:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_eventData));
               }
               setEventData(typedValue);
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
        
            case PROP_ID_traceId:{
               onInitProp(propId);
               this._traceId = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_billHeadCode:{
               onInitProp(propId);
               this._billHeadCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_businessType:{
               onInitProp(propId);
               this._businessType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_postingType:{
               onInitProp(propId);
               this._postingType = (java.lang.String)value;
               
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
        
            case PROP_ID_failedStage:{
               onInitProp(propId);
               this._failedStage = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_voucherDate:{
               onInitProp(propId);
               this._voucherDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_acctSchemaId:{
               onInitProp(propId);
               this._acctSchemaId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_retryCount:{
               onInitProp(propId);
               this._retryCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_resolution:{
               onInitProp(propId);
               this._resolution = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_resolutionNote:{
               onInitProp(propId);
               this._resolutionNote = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_resolvedBy:{
               onInitProp(propId);
               this._resolvedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_resolvedAt:{
               onInitProp(propId);
               this._resolvedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_voucherId:{
               onInitProp(propId);
               this._voucherId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_occurrenceTime:{
               onInitProp(propId);
               this._occurrenceTime = (java.sql.Timestamp)value;
               
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
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_eventData:{
               onInitProp(propId);
               this._eventData = (java.lang.String)value;
               
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
     * 追踪ID: TRACE_ID
     */
    public final java.lang.String getTraceId(){
         onPropGet(PROP_ID_traceId);
         return _traceId;
    }

    /**
     * 追踪ID: TRACE_ID
     */
    public final void setTraceId(java.lang.String value){
        if(onPropSet(PROP_ID_traceId,value)){
            this._traceId = value;
            internalClearRefs(PROP_ID_traceId);
            
        }
    }
    
    /**
     * 单据编号: BILL_HEAD_CODE
     */
    public final java.lang.String getBillHeadCode(){
         onPropGet(PROP_ID_billHeadCode);
         return _billHeadCode;
    }

    /**
     * 单据编号: BILL_HEAD_CODE
     */
    public final void setBillHeadCode(java.lang.String value){
        if(onPropSet(PROP_ID_billHeadCode,value)){
            this._billHeadCode = value;
            internalClearRefs(PROP_ID_billHeadCode);
            
        }
    }
    
    /**
     * 业务类型: BUSINESS_TYPE
     */
    public final java.lang.String getBusinessType(){
         onPropGet(PROP_ID_businessType);
         return _businessType;
    }

    /**
     * 业务类型: BUSINESS_TYPE
     */
    public final void setBusinessType(java.lang.String value){
        if(onPropSet(PROP_ID_businessType,value)){
            this._businessType = value;
            internalClearRefs(PROP_ID_businessType);
            
        }
    }
    
    /**
     * 过账类型: POSTING_TYPE
     */
    public final java.lang.String getPostingType(){
         onPropGet(PROP_ID_postingType);
         return _postingType;
    }

    /**
     * 过账类型: POSTING_TYPE
     */
    public final void setPostingType(java.lang.String value){
        if(onPropSet(PROP_ID_postingType,value)){
            this._postingType = value;
            internalClearRefs(PROP_ID_postingType);
            
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
     * 失败阶段: FAILED_STAGE
     */
    public final java.lang.String getFailedStage(){
         onPropGet(PROP_ID_failedStage);
         return _failedStage;
    }

    /**
     * 失败阶段: FAILED_STAGE
     */
    public final void setFailedStage(java.lang.String value){
        if(onPropSet(PROP_ID_failedStage,value)){
            this._failedStage = value;
            internalClearRefs(PROP_ID_failedStage);
            
        }
    }
    
    /**
     * 凭证日期: VOUCHER_DATE
     */
    public final java.time.LocalDate getVoucherDate(){
         onPropGet(PROP_ID_voucherDate);
         return _voucherDate;
    }

    /**
     * 凭证日期: VOUCHER_DATE
     */
    public final void setVoucherDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_voucherDate,value)){
            this._voucherDate = value;
            internalClearRefs(PROP_ID_voucherDate);
            
        }
    }
    
    /**
     * 核算组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 核算组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 账套: ACCT_SCHEMA_ID
     */
    public final java.lang.Long getAcctSchemaId(){
         onPropGet(PROP_ID_acctSchemaId);
         return _acctSchemaId;
    }

    /**
     * 账套: ACCT_SCHEMA_ID
     */
    public final void setAcctSchemaId(java.lang.Long value){
        if(onPropSet(PROP_ID_acctSchemaId,value)){
            this._acctSchemaId = value;
            internalClearRefs(PROP_ID_acctSchemaId);
            
        }
    }
    
    /**
     * 处置状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 处置状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
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
     * 处置动作: RESOLUTION
     */
    public final java.lang.String getResolution(){
         onPropGet(PROP_ID_resolution);
         return _resolution;
    }

    /**
     * 处置动作: RESOLUTION
     */
    public final void setResolution(java.lang.String value){
        if(onPropSet(PROP_ID_resolution,value)){
            this._resolution = value;
            internalClearRefs(PROP_ID_resolution);
            
        }
    }
    
    /**
     * 处置说明: RESOLUTION_NOTE
     */
    public final java.lang.String getResolutionNote(){
         onPropGet(PROP_ID_resolutionNote);
         return _resolutionNote;
    }

    /**
     * 处置说明: RESOLUTION_NOTE
     */
    public final void setResolutionNote(java.lang.String value){
        if(onPropSet(PROP_ID_resolutionNote,value)){
            this._resolutionNote = value;
            internalClearRefs(PROP_ID_resolutionNote);
            
        }
    }
    
    /**
     * 处置人: RESOLVED_BY
     */
    public final java.lang.String getResolvedBy(){
         onPropGet(PROP_ID_resolvedBy);
         return _resolvedBy;
    }

    /**
     * 处置人: RESOLVED_BY
     */
    public final void setResolvedBy(java.lang.String value){
        if(onPropSet(PROP_ID_resolvedBy,value)){
            this._resolvedBy = value;
            internalClearRefs(PROP_ID_resolvedBy);
            
        }
    }
    
    /**
     * 处置时间: RESOLVED_AT
     */
    public final java.sql.Timestamp getResolvedAt(){
         onPropGet(PROP_ID_resolvedAt);
         return _resolvedAt;
    }

    /**
     * 处置时间: RESOLVED_AT
     */
    public final void setResolvedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_resolvedAt,value)){
            this._resolvedAt = value;
            internalClearRefs(PROP_ID_resolvedAt);
            
        }
    }
    
    /**
     * 关联凭证ID: VOUCHER_ID
     */
    public final java.lang.Long getVoucherId(){
         onPropGet(PROP_ID_voucherId);
         return _voucherId;
    }

    /**
     * 关联凭证ID: VOUCHER_ID
     */
    public final void setVoucherId(java.lang.Long value){
        if(onPropSet(PROP_ID_voucherId,value)){
            this._voucherId = value;
            internalClearRefs(PROP_ID_voucherId);
            
        }
    }
    
    /**
     * 发生时间: OCCURRENCE_TIME
     */
    public final java.sql.Timestamp getOccurrenceTime(){
         onPropGet(PROP_ID_occurrenceTime);
         return _occurrenceTime;
    }

    /**
     * 发生时间: OCCURRENCE_TIME
     */
    public final void setOccurrenceTime(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_occurrenceTime,value)){
            this._occurrenceTime = value;
            internalClearRefs(PROP_ID_occurrenceTime);
            
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
     * 币种: CURRENCY_ID
     */
    public final java.lang.Long getCurrencyId(){
         onPropGet(PROP_ID_currencyId);
         return _currencyId;
    }

    /**
     * 币种: CURRENCY_ID
     */
    public final void setCurrencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_currencyId,value)){
            this._currencyId = value;
            internalClearRefs(PROP_ID_currencyId);
            
        }
    }
    
    /**
     * 汇率: EXCHANGE_RATE
     */
    public final java.math.BigDecimal getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 原始事件数据(JSON): EVENT_DATA
     */
    public final java.lang.String getEventData(){
         onPropGet(PROP_ID_eventData);
         return _eventData;
    }

    /**
     * 原始事件数据(JSON): EVENT_DATA
     */
    public final void setEventData(java.lang.String value){
        if(onPropSet(PROP_ID_eventData,value)){
            this._eventData = value;
            internalClearRefs(PROP_ID_eventData);
            
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
    public final app.erp.md.dao.entity.ErpMdAcctSchema getAcctSchema(){
       return (app.erp.md.dao.entity.ErpMdAcctSchema)internalGetRefEntity(PROP_NAME_acctSchema);
    }

    public final void setAcctSchema(app.erp.md.dao.entity.ErpMdAcctSchema refEntity){
   
           if(refEntity == null){
           
                   this.setAcctSchemaId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_acctSchema, refEntity,()->{
           
                           this.setAcctSchemaId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinVoucher getVoucher(){
       return (app.erp.fin.dao.entity.ErpFinVoucher)internalGetRefEntity(PROP_NAME_voucher);
    }

    public final void setVoucher(app.erp.fin.dao.entity.ErpFinVoucher refEntity){
   
           if(refEntity == null){
           
                   this.setVoucherId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_voucher, refEntity,()->{
           
                           this.setVoucherId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdCurrency getCurrency(){
       return (app.erp.md.dao.entity.ErpMdCurrency)internalGetRefEntity(PROP_NAME_currency);
    }

    public final void setCurrency(app.erp.md.dao.entity.ErpMdCurrency refEntity){
   
           if(refEntity == null){
           
                   this.setCurrencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_currency, refEntity,()->{
           
                           this.setCurrencyId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
