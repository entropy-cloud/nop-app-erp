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

import app.erp.fin.dao.entity.ErpFinArApItem;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  应收应付明细账: erp_fin_ar_ap_item
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinArApItem extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 账套: ACCT_SCHEMA_ID BIGINT */
    public static final String PROP_NAME_acctSchemaId = "acctSchemaId";
    public static final int PROP_ID_acctSchemaId = 4;
    
    /* 应收应付: DIRECTION INTEGER */
    public static final String PROP_NAME_direction = "direction";
    public static final int PROP_ID_direction = 5;
    
    /* 往来单位: PARTNER_ID BIGINT */
    public static final String PROP_NAME_partnerId = "partnerId";
    public static final int PROP_ID_partnerId = 6;
    
    /* 来源单据类型(AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT): SOURCE_BILL_TYPE VARCHAR */
    public static final String PROP_NAME_sourceBillType = "sourceBillType";
    public static final int PROP_ID_sourceBillType = 7;
    
    /* 来源单据号: SOURCE_BILL_CODE VARCHAR */
    public static final String PROP_NAME_sourceBillCode = "sourceBillCode";
    public static final int PROP_ID_sourceBillCode = 8;
    
    /* 业务日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 9;
    
    /* 到期日: DUE_DATE DATE */
    public static final String PROP_NAME_dueDate = "dueDate";
    public static final int PROP_ID_dueDate = 10;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 11;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 12;
    
    /* 源币种金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 13;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 14;
    
    /* 已核销源币金额: SETTLED_AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_settledAmountSource = "settledAmountSource";
    public static final int PROP_ID_settledAmountSource = 15;
    
    /* 已核销本位币金额: SETTLED_AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_settledAmountFunctional = "settledAmountFunctional";
    public static final int PROP_ID_settledAmountFunctional = 16;
    
    /* 未核销源币金额: OPEN_AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_openAmountSource = "openAmountSource";
    public static final int PROP_ID_openAmountSource = 17;
    
    /* 未核销本位币金额: OPEN_AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_openAmountFunctional = "openAmountFunctional";
    public static final int PROP_ID_openAmountFunctional = 18;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 19;
    
    /* 所属期间: PERIOD_ID BIGINT */
    public static final String PROP_NAME_periodId = "periodId";
    public static final int PROP_ID_periodId = 20;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 21;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 22;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 23;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 24;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 25;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 26;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 27;
    

    private static int _PROP_ID_BOUND = 28;

    
    /* relation:  */
    public static final String PROP_NAME_partner = "partner";
    
    /* relation:  */
    public static final String PROP_NAME_acctSchema = "acctSchema";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_period = "period";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[28];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_acctSchemaId] = PROP_NAME_acctSchemaId;
          PROP_NAME_TO_ID.put(PROP_NAME_acctSchemaId, PROP_ID_acctSchemaId);
      
          PROP_ID_TO_NAME[PROP_ID_direction] = PROP_NAME_direction;
          PROP_NAME_TO_ID.put(PROP_NAME_direction, PROP_ID_direction);
      
          PROP_ID_TO_NAME[PROP_ID_partnerId] = PROP_NAME_partnerId;
          PROP_NAME_TO_ID.put(PROP_NAME_partnerId, PROP_ID_partnerId);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillType] = PROP_NAME_sourceBillType;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillType, PROP_ID_sourceBillType);
      
          PROP_ID_TO_NAME[PROP_ID_sourceBillCode] = PROP_NAME_sourceBillCode;
          PROP_NAME_TO_ID.put(PROP_NAME_sourceBillCode, PROP_ID_sourceBillCode);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_dueDate] = PROP_NAME_dueDate;
          PROP_NAME_TO_ID.put(PROP_NAME_dueDate, PROP_ID_dueDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_settledAmountSource] = PROP_NAME_settledAmountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_settledAmountSource, PROP_ID_settledAmountSource);
      
          PROP_ID_TO_NAME[PROP_ID_settledAmountFunctional] = PROP_NAME_settledAmountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_settledAmountFunctional, PROP_ID_settledAmountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_openAmountSource] = PROP_NAME_openAmountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_openAmountSource, PROP_ID_openAmountSource);
      
          PROP_ID_TO_NAME[PROP_ID_openAmountFunctional] = PROP_NAME_openAmountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_openAmountFunctional, PROP_ID_openAmountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_periodId] = PROP_NAME_periodId;
          PROP_NAME_TO_ID.put(PROP_NAME_periodId, PROP_ID_periodId);
      
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
    
    /* 单号: CODE */
    private java.lang.String _code;
    
    /* 组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 账套: ACCT_SCHEMA_ID */
    private java.lang.Long _acctSchemaId;
    
    /* 应收应付: DIRECTION */
    private java.lang.Integer _direction;
    
    /* 往来单位: PARTNER_ID */
    private java.lang.Long _partnerId;
    
    /* 来源单据类型(AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT): SOURCE_BILL_TYPE */
    private java.lang.String _sourceBillType;
    
    /* 来源单据号: SOURCE_BILL_CODE */
    private java.lang.String _sourceBillCode;
    
    /* 业务日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 到期日: DUE_DATE */
    private java.time.LocalDate _dueDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.lang.String _exchangeRate;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.lang.String _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.lang.String _amountFunctional;
    
    /* 已核销源币金额: SETTLED_AMOUNT_SOURCE */
    private java.lang.String _settledAmountSource;
    
    /* 已核销本位币金额: SETTLED_AMOUNT_FUNCTIONAL */
    private java.lang.String _settledAmountFunctional;
    
    /* 未核销源币金额: OPEN_AMOUNT_SOURCE */
    private java.lang.String _openAmountSource;
    
    /* 未核销本位币金额: OPEN_AMOUNT_FUNCTIONAL */
    private java.lang.String _openAmountFunctional;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 所属期间: PERIOD_ID */
    private java.lang.Long _periodId;
    
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
    

    public _ErpFinArApItem(){
        // for debug
    }

    protected ErpFinArApItem newInstance(){
        ErpFinArApItem entity = new ErpFinArApItem();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinArApItem cloneInstance() {
        ErpFinArApItem entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinArApItem";
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
        
            case PROP_ID_acctSchemaId:
               return getAcctSchemaId();
        
            case PROP_ID_direction:
               return getDirection();
        
            case PROP_ID_partnerId:
               return getPartnerId();
        
            case PROP_ID_sourceBillType:
               return getSourceBillType();
        
            case PROP_ID_sourceBillCode:
               return getSourceBillCode();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_dueDate:
               return getDueDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
            case PROP_ID_settledAmountSource:
               return getSettledAmountSource();
        
            case PROP_ID_settledAmountFunctional:
               return getSettledAmountFunctional();
        
            case PROP_ID_openAmountSource:
               return getOpenAmountSource();
        
            case PROP_ID_openAmountFunctional:
               return getOpenAmountFunctional();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_periodId:
               return getPeriodId();
        
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
        
            case PROP_ID_acctSchemaId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_acctSchemaId));
               }
               setAcctSchemaId(typedValue);
               break;
            }
        
            case PROP_ID_direction:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_direction));
               }
               setDirection(typedValue);
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
        
            case PROP_ID_sourceBillType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillType));
               }
               setSourceBillType(typedValue);
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sourceBillCode));
               }
               setSourceBillCode(typedValue);
               break;
            }
        
            case PROP_ID_businessDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_businessDate));
               }
               setBusinessDate(typedValue);
               break;
            }
        
            case PROP_ID_dueDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_dueDate));
               }
               setDueDate(typedValue);
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
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeRate));
               }
               setExchangeRate(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
               break;
            }
        
            case PROP_ID_settledAmountSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_settledAmountSource));
               }
               setSettledAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_settledAmountFunctional:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_settledAmountFunctional));
               }
               setSettledAmountFunctional(typedValue);
               break;
            }
        
            case PROP_ID_openAmountSource:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_openAmountSource));
               }
               setOpenAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_openAmountFunctional:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_openAmountFunctional));
               }
               setOpenAmountFunctional(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_periodId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_periodId));
               }
               setPeriodId(typedValue);
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
        
            case PROP_ID_acctSchemaId:{
               onInitProp(propId);
               this._acctSchemaId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_direction:{
               onInitProp(propId);
               this._direction = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_partnerId:{
               onInitProp(propId);
               this._partnerId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sourceBillType:{
               onInitProp(propId);
               this._sourceBillType = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sourceBillCode:{
               onInitProp(propId);
               this._sourceBillCode = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_dueDate:{
               onInitProp(propId);
               this._dueDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_exchangeRate:{
               onInitProp(propId);
               this._exchangeRate = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_settledAmountSource:{
               onInitProp(propId);
               this._settledAmountSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_settledAmountFunctional:{
               onInitProp(propId);
               this._settledAmountFunctional = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_openAmountSource:{
               onInitProp(propId);
               this._openAmountSource = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_openAmountFunctional:{
               onInitProp(propId);
               this._openAmountFunctional = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_periodId:{
               onInitProp(propId);
               this._periodId = (java.lang.Long)value;
               
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
     * 单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 单号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 组织: ORG_ID
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
     * 应收应付: DIRECTION
     */
    public final java.lang.Integer getDirection(){
         onPropGet(PROP_ID_direction);
         return _direction;
    }

    /**
     * 应收应付: DIRECTION
     */
    public final void setDirection(java.lang.Integer value){
        if(onPropSet(PROP_ID_direction,value)){
            this._direction = value;
            internalClearRefs(PROP_ID_direction);
            
        }
    }
    
    /**
     * 往来单位: PARTNER_ID
     */
    public final java.lang.Long getPartnerId(){
         onPropGet(PROP_ID_partnerId);
         return _partnerId;
    }

    /**
     * 往来单位: PARTNER_ID
     */
    public final void setPartnerId(java.lang.Long value){
        if(onPropSet(PROP_ID_partnerId,value)){
            this._partnerId = value;
            internalClearRefs(PROP_ID_partnerId);
            
        }
    }
    
    /**
     * 来源单据类型(AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT): SOURCE_BILL_TYPE
     */
    public final java.lang.String getSourceBillType(){
         onPropGet(PROP_ID_sourceBillType);
         return _sourceBillType;
    }

    /**
     * 来源单据类型(AP_INVOICE/AR_INVOICE/PAYMENT/RECEIPT): SOURCE_BILL_TYPE
     */
    public final void setSourceBillType(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillType,value)){
            this._sourceBillType = value;
            internalClearRefs(PROP_ID_sourceBillType);
            
        }
    }
    
    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final java.lang.String getSourceBillCode(){
         onPropGet(PROP_ID_sourceBillCode);
         return _sourceBillCode;
    }

    /**
     * 来源单据号: SOURCE_BILL_CODE
     */
    public final void setSourceBillCode(java.lang.String value){
        if(onPropSet(PROP_ID_sourceBillCode,value)){
            this._sourceBillCode = value;
            internalClearRefs(PROP_ID_sourceBillCode);
            
        }
    }
    
    /**
     * 业务日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 业务日期: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
        }
    }
    
    /**
     * 到期日: DUE_DATE
     */
    public final java.time.LocalDate getDueDate(){
         onPropGet(PROP_ID_dueDate);
         return _dueDate;
    }

    /**
     * 到期日: DUE_DATE
     */
    public final void setDueDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_dueDate,value)){
            this._dueDate = value;
            internalClearRefs(PROP_ID_dueDate);
            
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
    public final java.lang.String getExchangeRate(){
         onPropGet(PROP_ID_exchangeRate);
         return _exchangeRate;
    }

    /**
     * 汇率: EXCHANGE_RATE
     */
    public final void setExchangeRate(java.lang.String value){
        if(onPropSet(PROP_ID_exchangeRate,value)){
            this._exchangeRate = value;
            internalClearRefs(PROP_ID_exchangeRate);
            
        }
    }
    
    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final java.lang.String getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final void setAmountSource(java.lang.String value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final java.lang.String getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.lang.String value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
        }
    }
    
    /**
     * 已核销源币金额: SETTLED_AMOUNT_SOURCE
     */
    public final java.lang.String getSettledAmountSource(){
         onPropGet(PROP_ID_settledAmountSource);
         return _settledAmountSource;
    }

    /**
     * 已核销源币金额: SETTLED_AMOUNT_SOURCE
     */
    public final void setSettledAmountSource(java.lang.String value){
        if(onPropSet(PROP_ID_settledAmountSource,value)){
            this._settledAmountSource = value;
            internalClearRefs(PROP_ID_settledAmountSource);
            
        }
    }
    
    /**
     * 已核销本位币金额: SETTLED_AMOUNT_FUNCTIONAL
     */
    public final java.lang.String getSettledAmountFunctional(){
         onPropGet(PROP_ID_settledAmountFunctional);
         return _settledAmountFunctional;
    }

    /**
     * 已核销本位币金额: SETTLED_AMOUNT_FUNCTIONAL
     */
    public final void setSettledAmountFunctional(java.lang.String value){
        if(onPropSet(PROP_ID_settledAmountFunctional,value)){
            this._settledAmountFunctional = value;
            internalClearRefs(PROP_ID_settledAmountFunctional);
            
        }
    }
    
    /**
     * 未核销源币金额: OPEN_AMOUNT_SOURCE
     */
    public final java.lang.String getOpenAmountSource(){
         onPropGet(PROP_ID_openAmountSource);
         return _openAmountSource;
    }

    /**
     * 未核销源币金额: OPEN_AMOUNT_SOURCE
     */
    public final void setOpenAmountSource(java.lang.String value){
        if(onPropSet(PROP_ID_openAmountSource,value)){
            this._openAmountSource = value;
            internalClearRefs(PROP_ID_openAmountSource);
            
        }
    }
    
    /**
     * 未核销本位币金额: OPEN_AMOUNT_FUNCTIONAL
     */
    public final java.lang.String getOpenAmountFunctional(){
         onPropGet(PROP_ID_openAmountFunctional);
         return _openAmountFunctional;
    }

    /**
     * 未核销本位币金额: OPEN_AMOUNT_FUNCTIONAL
     */
    public final void setOpenAmountFunctional(java.lang.String value){
        if(onPropSet(PROP_ID_openAmountFunctional,value)){
            this._openAmountFunctional = value;
            internalClearRefs(PROP_ID_openAmountFunctional);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 所属期间: PERIOD_ID
     */
    public final java.lang.Long getPeriodId(){
         onPropGet(PROP_ID_periodId);
         return _periodId;
    }

    /**
     * 所属期间: PERIOD_ID
     */
    public final void setPeriodId(java.lang.Long value){
        if(onPropSet(PROP_ID_periodId,value)){
            this._periodId = value;
            internalClearRefs(PROP_ID_periodId);
            
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
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinAccountingPeriod getPeriod(){
       return (app.erp.fin.dao.entity.ErpFinAccountingPeriod)internalGetRefEntity(PROP_NAME_period);
    }

    public final void setPeriod(app.erp.fin.dao.entity.ErpFinAccountingPeriod refEntity){
   
           if(refEntity == null){
           
                   this.setPeriodId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_period, refEntity,()->{
           
                           this.setPeriodId(refEntity.getId());
                       
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
