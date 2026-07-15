package app.erp.ast.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.ast.dao.entity.ErpAstInventory;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  资产盘点单: erp_ast_inventory
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpAstInventory extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 盘点单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 盘点名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 盘点状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 5;
    
    /* 范围-部门: RANGE_DEPARTMENT_ID BIGINT */
    public static final String PROP_NAME_rangeDepartmentId = "rangeDepartmentId";
    public static final int PROP_ID_rangeDepartmentId = 6;
    
    /* 范围-资产类别: RANGE_CATEGORY_ID BIGINT */
    public static final String PROP_NAME_rangeCategoryId = "rangeCategoryId";
    public static final int PROP_ID_rangeCategoryId = 7;
    
    /* 范围-地点: RANGE_LOCATION_ID BIGINT */
    public static final String PROP_NAME_rangeLocationId = "rangeLocationId";
    public static final int PROP_ID_rangeLocationId = 8;
    
    /* 盘点负责人: RESPONSIBLE_BY_ID BIGINT */
    public static final String PROP_NAME_responsibleById = "responsibleById";
    public static final int PROP_ID_responsibleById = 9;
    
    /* 盘点基准日: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 10;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 11;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 12;
    
    /* 盘盈行数: SURPLUS_COUNT INTEGER */
    public static final String PROP_NAME_surplusCount = "surplusCount";
    public static final int PROP_ID_surplusCount = 13;
    
    /* 盘亏行数: SHORTAGE_COUNT INTEGER */
    public static final String PROP_NAME_shortageCount = "shortageCount";
    public static final int PROP_ID_shortageCount = 14;
    
    /* 一致行数: MATCHED_COUNT INTEGER */
    public static final String PROP_NAME_matchedCount = "matchedCount";
    public static final int PROP_ID_matchedCount = 15;
    
    /* 盘盈金额: SURPLUS_AMOUNT DECIMAL */
    public static final String PROP_NAME_surplusAmount = "surplusAmount";
    public static final int PROP_ID_surplusAmount = 16;
    
    /* 盘亏金额: SHORTAGE_AMOUNT DECIMAL */
    public static final String PROP_NAME_shortageAmount = "shortageAmount";
    public static final int PROP_ID_shortageAmount = 17;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 18;
    
    /* 过账时间: POSTED_AT TIMESTAMP */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 19;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 20;
    
    /* 复核人: APPROVED_BY VARCHAR */
    public static final String PROP_NAME_approvedBy = "approvedBy";
    public static final int PROP_ID_approvedBy = 21;
    
    /* 复核时间: APPROVED_AT TIMESTAMP */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 22;
    
    /* 源币种金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 23;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 24;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 25;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 26;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 27;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 28;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 29;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 30;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 31;
    

    private static int _PROP_ID_BOUND = 32;

    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_rangeDepartment = "rangeDepartment";
    
    /* relation:  */
    public static final String PROP_NAME_rangeCategory = "rangeCategory";
    
    /* relation:  */
    public static final String PROP_NAME_rangeLocation = "rangeLocation";
    
    /* relation:  */
    public static final String PROP_NAME_responsibleBy = "responsibleBy";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_lines = "lines";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[32];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_rangeDepartmentId] = PROP_NAME_rangeDepartmentId;
          PROP_NAME_TO_ID.put(PROP_NAME_rangeDepartmentId, PROP_ID_rangeDepartmentId);
      
          PROP_ID_TO_NAME[PROP_ID_rangeCategoryId] = PROP_NAME_rangeCategoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_rangeCategoryId, PROP_ID_rangeCategoryId);
      
          PROP_ID_TO_NAME[PROP_ID_rangeLocationId] = PROP_NAME_rangeLocationId;
          PROP_NAME_TO_ID.put(PROP_NAME_rangeLocationId, PROP_ID_rangeLocationId);
      
          PROP_ID_TO_NAME[PROP_ID_responsibleById] = PROP_NAME_responsibleById;
          PROP_NAME_TO_ID.put(PROP_NAME_responsibleById, PROP_ID_responsibleById);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_surplusCount] = PROP_NAME_surplusCount;
          PROP_NAME_TO_ID.put(PROP_NAME_surplusCount, PROP_ID_surplusCount);
      
          PROP_ID_TO_NAME[PROP_ID_shortageCount] = PROP_NAME_shortageCount;
          PROP_NAME_TO_ID.put(PROP_NAME_shortageCount, PROP_ID_shortageCount);
      
          PROP_ID_TO_NAME[PROP_ID_matchedCount] = PROP_NAME_matchedCount;
          PROP_NAME_TO_ID.put(PROP_NAME_matchedCount, PROP_ID_matchedCount);
      
          PROP_ID_TO_NAME[PROP_ID_surplusAmount] = PROP_NAME_surplusAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_surplusAmount, PROP_ID_surplusAmount);
      
          PROP_ID_TO_NAME[PROP_ID_shortageAmount] = PROP_NAME_shortageAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_shortageAmount, PROP_ID_shortageAmount);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedBy] = PROP_NAME_approvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedBy, PROP_ID_approvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
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
    private java.lang.Long _id;
    
    /* 盘点单号: CODE */
    private java.lang.String _code;
    
    /* 盘点名称: NAME */
    private java.lang.String _name;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 盘点状态: STATUS */
    private java.lang.String _status;
    
    /* 范围-部门: RANGE_DEPARTMENT_ID */
    private java.lang.Long _rangeDepartmentId;
    
    /* 范围-资产类别: RANGE_CATEGORY_ID */
    private java.lang.Long _rangeCategoryId;
    
    /* 范围-地点: RANGE_LOCATION_ID */
    private java.lang.Long _rangeLocationId;
    
    /* 盘点负责人: RESPONSIBLE_BY_ID */
    private java.lang.Long _responsibleById;
    
    /* 盘点基准日: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 盘盈行数: SURPLUS_COUNT */
    private java.lang.Integer _surplusCount;
    
    /* 盘亏行数: SHORTAGE_COUNT */
    private java.lang.Integer _shortageCount;
    
    /* 一致行数: MATCHED_COUNT */
    private java.lang.Integer _matchedCount;
    
    /* 盘盈金额: SURPLUS_AMOUNT */
    private java.math.BigDecimal _surplusAmount;
    
    /* 盘亏金额: SHORTAGE_AMOUNT */
    private java.math.BigDecimal _shortageAmount;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账时间: POSTED_AT */
    private java.sql.Timestamp _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 复核人: APPROVED_BY */
    private java.lang.String _approvedBy;
    
    /* 复核时间: APPROVED_AT */
    private java.sql.Timestamp _approvedAt;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.math.BigDecimal _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.math.BigDecimal _amountFunctional;
    
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
    

    public _ErpAstInventory(){
        // for debug
    }

    protected ErpAstInventory newInstance(){
        ErpAstInventory entity = new ErpAstInventory();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpAstInventory cloneInstance() {
        ErpAstInventory entity = newInstance();
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
      return "app.erp.ast.dao.entity.ErpAstInventory";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_rangeDepartmentId:
               return getRangeDepartmentId();
        
            case PROP_ID_rangeCategoryId:
               return getRangeCategoryId();
        
            case PROP_ID_rangeLocationId:
               return getRangeLocationId();
        
            case PROP_ID_responsibleById:
               return getResponsibleById();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_surplusCount:
               return getSurplusCount();
        
            case PROP_ID_shortageCount:
               return getShortageCount();
        
            case PROP_ID_matchedCount:
               return getMatchedCount();
        
            case PROP_ID_surplusAmount:
               return getSurplusAmount();
        
            case PROP_ID_shortageAmount:
               return getShortageAmount();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_approvedBy:
               return getApprovedBy();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
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
        
            case PROP_ID_status:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_rangeDepartmentId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_rangeDepartmentId));
               }
               setRangeDepartmentId(typedValue);
               break;
            }
        
            case PROP_ID_rangeCategoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_rangeCategoryId));
               }
               setRangeCategoryId(typedValue);
               break;
            }
        
            case PROP_ID_rangeLocationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_rangeLocationId));
               }
               setRangeLocationId(typedValue);
               break;
            }
        
            case PROP_ID_responsibleById:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_responsibleById));
               }
               setResponsibleById(typedValue);
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
        
            case PROP_ID_surplusCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_surplusCount));
               }
               setSurplusCount(typedValue);
               break;
            }
        
            case PROP_ID_shortageCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_shortageCount));
               }
               setShortageCount(typedValue);
               break;
            }
        
            case PROP_ID_matchedCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_matchedCount));
               }
               setMatchedCount(typedValue);
               break;
            }
        
            case PROP_ID_surplusAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_surplusAmount));
               }
               setSurplusAmount(typedValue);
               break;
            }
        
            case PROP_ID_shortageAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_shortageAmount));
               }
               setShortageAmount(typedValue);
               break;
            }
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
               break;
            }
        
            case PROP_ID_postedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
               break;
            }
        
            case PROP_ID_postedBy:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_postedBy));
               }
               setPostedBy(typedValue);
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
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_approvedAt));
               }
               setApprovedAt(typedValue);
               break;
            }
        
            case PROP_ID_amountSource:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountSource));
               }
               setAmountSource(typedValue);
               break;
            }
        
            case PROP_ID_amountFunctional:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_amountFunctional));
               }
               setAmountFunctional(typedValue);
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
               this._id = (java.lang.Long)value;
               orm_id(); // 如果是设置主键字段，则触发watcher
               break;
            }
        
            case PROP_ID_code:{
               onInitProp(propId);
               this._code = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_rangeDepartmentId:{
               onInitProp(propId);
               this._rangeDepartmentId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_rangeCategoryId:{
               onInitProp(propId);
               this._rangeCategoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_rangeLocationId:{
               onInitProp(propId);
               this._rangeLocationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_responsibleById:{
               onInitProp(propId);
               this._responsibleById = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_businessDate:{
               onInitProp(propId);
               this._businessDate = (java.time.LocalDate)value;
               
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
        
            case PROP_ID_surplusCount:{
               onInitProp(propId);
               this._surplusCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_shortageCount:{
               onInitProp(propId);
               this._shortageCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_matchedCount:{
               onInitProp(propId);
               this._matchedCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_surplusAmount:{
               onInitProp(propId);
               this._surplusAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_shortageAmount:{
               onInitProp(propId);
               this._shortageAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedBy:{
               onInitProp(propId);
               this._approvedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approvedAt:{
               onInitProp(propId);
               this._approvedAt = (java.sql.Timestamp)value;
               
               break;
            }
        
            case PROP_ID_amountSource:{
               onInitProp(propId);
               this._amountSource = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_amountFunctional:{
               onInitProp(propId);
               this._amountFunctional = (java.math.BigDecimal)value;
               
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
     * 盘点单号: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 盘点单号: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 盘点名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 盘点名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
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
     * 盘点状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 盘点状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 范围-部门: RANGE_DEPARTMENT_ID
     */
    public final java.lang.Long getRangeDepartmentId(){
         onPropGet(PROP_ID_rangeDepartmentId);
         return _rangeDepartmentId;
    }

    /**
     * 范围-部门: RANGE_DEPARTMENT_ID
     */
    public final void setRangeDepartmentId(java.lang.Long value){
        if(onPropSet(PROP_ID_rangeDepartmentId,value)){
            this._rangeDepartmentId = value;
            internalClearRefs(PROP_ID_rangeDepartmentId);
            
        }
    }
    
    /**
     * 范围-资产类别: RANGE_CATEGORY_ID
     */
    public final java.lang.Long getRangeCategoryId(){
         onPropGet(PROP_ID_rangeCategoryId);
         return _rangeCategoryId;
    }

    /**
     * 范围-资产类别: RANGE_CATEGORY_ID
     */
    public final void setRangeCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_rangeCategoryId,value)){
            this._rangeCategoryId = value;
            internalClearRefs(PROP_ID_rangeCategoryId);
            
        }
    }
    
    /**
     * 范围-地点: RANGE_LOCATION_ID
     */
    public final java.lang.Long getRangeLocationId(){
         onPropGet(PROP_ID_rangeLocationId);
         return _rangeLocationId;
    }

    /**
     * 范围-地点: RANGE_LOCATION_ID
     */
    public final void setRangeLocationId(java.lang.Long value){
        if(onPropSet(PROP_ID_rangeLocationId,value)){
            this._rangeLocationId = value;
            internalClearRefs(PROP_ID_rangeLocationId);
            
        }
    }
    
    /**
     * 盘点负责人: RESPONSIBLE_BY_ID
     */
    public final java.lang.Long getResponsibleById(){
         onPropGet(PROP_ID_responsibleById);
         return _responsibleById;
    }

    /**
     * 盘点负责人: RESPONSIBLE_BY_ID
     */
    public final void setResponsibleById(java.lang.Long value){
        if(onPropSet(PROP_ID_responsibleById,value)){
            this._responsibleById = value;
            internalClearRefs(PROP_ID_responsibleById);
            
        }
    }
    
    /**
     * 盘点基准日: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 盘点基准日: BUSINESS_DATE
     */
    public final void setBusinessDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_businessDate,value)){
            this._businessDate = value;
            internalClearRefs(PROP_ID_businessDate);
            
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
     * 盘盈行数: SURPLUS_COUNT
     */
    public final java.lang.Integer getSurplusCount(){
         onPropGet(PROP_ID_surplusCount);
         return _surplusCount;
    }

    /**
     * 盘盈行数: SURPLUS_COUNT
     */
    public final void setSurplusCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_surplusCount,value)){
            this._surplusCount = value;
            internalClearRefs(PROP_ID_surplusCount);
            
        }
    }
    
    /**
     * 盘亏行数: SHORTAGE_COUNT
     */
    public final java.lang.Integer getShortageCount(){
         onPropGet(PROP_ID_shortageCount);
         return _shortageCount;
    }

    /**
     * 盘亏行数: SHORTAGE_COUNT
     */
    public final void setShortageCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_shortageCount,value)){
            this._shortageCount = value;
            internalClearRefs(PROP_ID_shortageCount);
            
        }
    }
    
    /**
     * 一致行数: MATCHED_COUNT
     */
    public final java.lang.Integer getMatchedCount(){
         onPropGet(PROP_ID_matchedCount);
         return _matchedCount;
    }

    /**
     * 一致行数: MATCHED_COUNT
     */
    public final void setMatchedCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_matchedCount,value)){
            this._matchedCount = value;
            internalClearRefs(PROP_ID_matchedCount);
            
        }
    }
    
    /**
     * 盘盈金额: SURPLUS_AMOUNT
     */
    public final java.math.BigDecimal getSurplusAmount(){
         onPropGet(PROP_ID_surplusAmount);
         return _surplusAmount;
    }

    /**
     * 盘盈金额: SURPLUS_AMOUNT
     */
    public final void setSurplusAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_surplusAmount,value)){
            this._surplusAmount = value;
            internalClearRefs(PROP_ID_surplusAmount);
            
        }
    }
    
    /**
     * 盘亏金额: SHORTAGE_AMOUNT
     */
    public final java.math.BigDecimal getShortageAmount(){
         onPropGet(PROP_ID_shortageAmount);
         return _shortageAmount;
    }

    /**
     * 盘亏金额: SHORTAGE_AMOUNT
     */
    public final void setShortageAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_shortageAmount,value)){
            this._shortageAmount = value;
            internalClearRefs(PROP_ID_shortageAmount);
            
        }
    }
    
    /**
     * 已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
        }
    }
    
    /**
     * 过账时间: POSTED_AT
     */
    public final java.sql.Timestamp getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
        }
    }
    
    /**
     * 过账人: POSTED_BY
     */
    public final java.lang.String getPostedBy(){
         onPropGet(PROP_ID_postedBy);
         return _postedBy;
    }

    /**
     * 过账人: POSTED_BY
     */
    public final void setPostedBy(java.lang.String value){
        if(onPropSet(PROP_ID_postedBy,value)){
            this._postedBy = value;
            internalClearRefs(PROP_ID_postedBy);
            
        }
    }
    
    /**
     * 复核人: APPROVED_BY
     */
    public final java.lang.String getApprovedBy(){
         onPropGet(PROP_ID_approvedBy);
         return _approvedBy;
    }

    /**
     * 复核人: APPROVED_BY
     */
    public final void setApprovedBy(java.lang.String value){
        if(onPropSet(PROP_ID_approvedBy,value)){
            this._approvedBy = value;
            internalClearRefs(PROP_ID_approvedBy);
            
        }
    }
    
    /**
     * 复核时间: APPROVED_AT
     */
    public final java.sql.Timestamp getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 复核时间: APPROVED_AT
     */
    public final void setApprovedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
        }
    }
    
    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final java.math.BigDecimal getAmountSource(){
         onPropGet(PROP_ID_amountSource);
         return _amountSource;
    }

    /**
     * 源币种金额: AMOUNT_SOURCE
     */
    public final void setAmountSource(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountSource,value)){
            this._amountSource = value;
            internalClearRefs(PROP_ID_amountSource);
            
        }
    }
    
    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final java.math.BigDecimal getAmountFunctional(){
         onPropGet(PROP_ID_amountFunctional);
         return _amountFunctional;
    }

    /**
     * 本位币金额: AMOUNT_FUNCTIONAL
     */
    public final void setAmountFunctional(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_amountFunctional,value)){
            this._amountFunctional = value;
            internalClearRefs(PROP_ID_amountFunctional);
            
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
    public final app.erp.md.dao.entity.ErpMdOrganization getRangeDepartment(){
       return (app.erp.md.dao.entity.ErpMdOrganization)internalGetRefEntity(PROP_NAME_rangeDepartment);
    }

    public final void setRangeDepartment(app.erp.md.dao.entity.ErpMdOrganization refEntity){
   
           if(refEntity == null){
           
                   this.setRangeDepartmentId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_rangeDepartment, refEntity,()->{
           
                           this.setRangeDepartmentId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.ast.dao.entity.ErpAstAssetCategory getRangeCategory(){
       return (app.erp.ast.dao.entity.ErpAstAssetCategory)internalGetRefEntity(PROP_NAME_rangeCategory);
    }

    public final void setRangeCategory(app.erp.ast.dao.entity.ErpAstAssetCategory refEntity){
   
           if(refEntity == null){
           
                   this.setRangeCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_rangeCategory, refEntity,()->{
           
                           this.setRangeCategoryId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdLocation getRangeLocation(){
       return (app.erp.md.dao.entity.ErpMdLocation)internalGetRefEntity(PROP_NAME_rangeLocation);
    }

    public final void setRangeLocation(app.erp.md.dao.entity.ErpMdLocation refEntity){
   
           if(refEntity == null){
           
                   this.setRangeLocationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_rangeLocation, refEntity,()->{
           
                           this.setRangeLocationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdEmployee getResponsibleBy(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_responsibleBy);
    }

    public final void setResponsibleBy(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setResponsibleById(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_responsibleBy, refEntity,()->{
           
                           this.setResponsibleById(refEntity.getId());
                       
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
       
    private final OrmEntitySet<app.erp.ast.dao.entity.ErpAstInventoryLine> _lines = new OrmEntitySet<>(this, PROP_NAME_lines,
        null, null,app.erp.ast.dao.entity.ErpAstInventoryLine.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.ast.dao.entity.ErpAstInventoryLine> getLines(){
       return _lines;
    }
       
}
// resume CPD analysis - CPD-ON
