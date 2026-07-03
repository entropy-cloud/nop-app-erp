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

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  员工借款单: erp_fin_employee_advance
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinEmployeeAdvance extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 借款人: EMPLOYEE_ID BIGINT */
    public static final String PROP_NAME_employeeId = "employeeId";
    public static final int PROP_ID_employeeId = 4;
    
    /* 借款类型: ADVANCE_TYPE VARCHAR */
    public static final String PROP_NAME_advanceType = "advanceType";
    public static final int PROP_ID_advanceType = 5;
    
    /* 借款日期: BUSINESS_DATE DATE */
    public static final String PROP_NAME_businessDate = "businessDate";
    public static final int PROP_ID_businessDate = 6;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 7;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 8;
    
    /* 源币种金额: AMOUNT_SOURCE DECIMAL */
    public static final String PROP_NAME_amountSource = "amountSource";
    public static final int PROP_ID_amountSource = 9;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL DECIMAL */
    public static final String PROP_NAME_amountFunctional = "amountFunctional";
    public static final int PROP_ID_amountFunctional = 10;
    
    /* 已清算金额: SETTLED_AMOUNT DECIMAL */
    public static final String PROP_NAME_settledAmount = "settledAmount";
    public static final int PROP_ID_settledAmount = 11;
    
    /* 未还金额: OUTSTANDING_AMOUNT DECIMAL */
    public static final String PROP_NAME_outstandingAmount = "outstandingAmount";
    public static final int PROP_ID_outstandingAmount = 12;
    
    /* 关联项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 13;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 14;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 15;
    
    /* 审核人: APPROVED_BY VARCHAR */
    public static final String PROP_NAME_approvedBy = "approvedBy";
    public static final int PROP_ID_approvedBy = 16;
    
    /* 审核时间: APPROVED_AT DATETIME */
    public static final String PROP_NAME_approvedAt = "approvedAt";
    public static final int PROP_ID_approvedAt = 17;
    
    /* 是否已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 18;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 19;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 20;
    
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
    public static final String PROP_NAME_employee = "employee";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_project = "project";
    
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
      
          PROP_ID_TO_NAME[PROP_ID_employeeId] = PROP_NAME_employeeId;
          PROP_NAME_TO_ID.put(PROP_NAME_employeeId, PROP_ID_employeeId);
      
          PROP_ID_TO_NAME[PROP_ID_advanceType] = PROP_NAME_advanceType;
          PROP_NAME_TO_ID.put(PROP_NAME_advanceType, PROP_ID_advanceType);
      
          PROP_ID_TO_NAME[PROP_ID_businessDate] = PROP_NAME_businessDate;
          PROP_NAME_TO_ID.put(PROP_NAME_businessDate, PROP_ID_businessDate);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_settledAmount] = PROP_NAME_settledAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_settledAmount, PROP_ID_settledAmount);
      
          PROP_ID_TO_NAME[PROP_ID_outstandingAmount] = PROP_NAME_outstandingAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_outstandingAmount, PROP_ID_outstandingAmount);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approvedBy] = PROP_NAME_approvedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedBy, PROP_ID_approvedBy);
      
          PROP_ID_TO_NAME[PROP_ID_approvedAt] = PROP_NAME_approvedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_approvedAt, PROP_ID_approvedAt);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
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
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 借款人: EMPLOYEE_ID */
    private java.lang.Long _employeeId;
    
    /* 借款类型: ADVANCE_TYPE */
    private java.lang.String _advanceType;
    
    /* 借款日期: BUSINESS_DATE */
    private java.time.LocalDate _businessDate;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.math.BigDecimal _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.math.BigDecimal _amountFunctional;
    
    /* 已清算金额: SETTLED_AMOUNT */
    private java.math.BigDecimal _settledAmount;
    
    /* 未还金额: OUTSTANDING_AMOUNT */
    private java.math.BigDecimal _outstandingAmount;
    
    /* 关联项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
    /* 审核人: APPROVED_BY */
    private java.lang.String _approvedBy;
    
    /* 审核时间: APPROVED_AT */
    private java.time.LocalDateTime _approvedAt;
    
    /* 是否已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 过账时间: POSTED_AT */
    private java.time.LocalDateTime _postedAt;
    
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
    

    public _ErpFinEmployeeAdvance(){
        // for debug
    }

    protected ErpFinEmployeeAdvance newInstance(){
        ErpFinEmployeeAdvance entity = new ErpFinEmployeeAdvance();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinEmployeeAdvance cloneInstance() {
        ErpFinEmployeeAdvance entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinEmployeeAdvance";
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
        
            case PROP_ID_employeeId:
               return getEmployeeId();
        
            case PROP_ID_advanceType:
               return getAdvanceType();
        
            case PROP_ID_businessDate:
               return getBusinessDate();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
            case PROP_ID_settledAmount:
               return getSettledAmount();
        
            case PROP_ID_outstandingAmount:
               return getOutstandingAmount();
        
            case PROP_ID_projectId:
               return getProjectId();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_approvedBy:
               return getApprovedBy();
        
            case PROP_ID_approvedAt:
               return getApprovedAt();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
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
        
            case PROP_ID_employeeId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_employeeId));
               }
               setEmployeeId(typedValue);
               break;
            }
        
            case PROP_ID_advanceType:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_advanceType));
               }
               setAdvanceType(typedValue);
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
        
            case PROP_ID_settledAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_settledAmount));
               }
               setSettledAmount(typedValue);
               break;
            }
        
            case PROP_ID_outstandingAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_outstandingAmount));
               }
               setOutstandingAmount(typedValue);
               break;
            }
        
            case PROP_ID_projectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_projectId));
               }
               setProjectId(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
               break;
            }
        
            case PROP_ID_approveStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_approveStatus));
               }
               setApproveStatus(typedValue);
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
        
            case PROP_ID_posted:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_posted));
               }
               setPosted(typedValue);
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
        
            case PROP_ID_postedAt:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_postedAt));
               }
               setPostedAt(typedValue);
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
        
            case PROP_ID_employeeId:{
               onInitProp(propId);
               this._employeeId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_advanceType:{
               onInitProp(propId);
               this._advanceType = (java.lang.String)value;
               
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
        
            case PROP_ID_settledAmount:{
               onInitProp(propId);
               this._settledAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_outstandingAmount:{
               onInitProp(propId);
               this._outstandingAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_approveStatus:{
               onInitProp(propId);
               this._approveStatus = (java.lang.String)value;
               
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
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.time.LocalDateTime)value;
               
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
     * 借款人: EMPLOYEE_ID
     */
    public final java.lang.Long getEmployeeId(){
         onPropGet(PROP_ID_employeeId);
         return _employeeId;
    }

    /**
     * 借款人: EMPLOYEE_ID
     */
    public final void setEmployeeId(java.lang.Long value){
        if(onPropSet(PROP_ID_employeeId,value)){
            this._employeeId = value;
            internalClearRefs(PROP_ID_employeeId);
            
        }
    }
    
    /**
     * 借款类型: ADVANCE_TYPE
     */
    public final java.lang.String getAdvanceType(){
         onPropGet(PROP_ID_advanceType);
         return _advanceType;
    }

    /**
     * 借款类型: ADVANCE_TYPE
     */
    public final void setAdvanceType(java.lang.String value){
        if(onPropSet(PROP_ID_advanceType,value)){
            this._advanceType = value;
            internalClearRefs(PROP_ID_advanceType);
            
        }
    }
    
    /**
     * 借款日期: BUSINESS_DATE
     */
    public final java.time.LocalDate getBusinessDate(){
         onPropGet(PROP_ID_businessDate);
         return _businessDate;
    }

    /**
     * 借款日期: BUSINESS_DATE
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
     * 已清算金额: SETTLED_AMOUNT
     */
    public final java.math.BigDecimal getSettledAmount(){
         onPropGet(PROP_ID_settledAmount);
         return _settledAmount;
    }

    /**
     * 已清算金额: SETTLED_AMOUNT
     */
    public final void setSettledAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_settledAmount,value)){
            this._settledAmount = value;
            internalClearRefs(PROP_ID_settledAmount);
            
        }
    }
    
    /**
     * 未还金额: OUTSTANDING_AMOUNT
     */
    public final java.math.BigDecimal getOutstandingAmount(){
         onPropGet(PROP_ID_outstandingAmount);
         return _outstandingAmount;
    }

    /**
     * 未还金额: OUTSTANDING_AMOUNT
     */
    public final void setOutstandingAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_outstandingAmount,value)){
            this._outstandingAmount = value;
            internalClearRefs(PROP_ID_outstandingAmount);
            
        }
    }
    
    /**
     * 关联项目: PROJECT_ID
     */
    public final java.lang.Long getProjectId(){
         onPropGet(PROP_ID_projectId);
         return _projectId;
    }

    /**
     * 关联项目: PROJECT_ID
     */
    public final void setProjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectId,value)){
            this._projectId = value;
            internalClearRefs(PROP_ID_projectId);
            
        }
    }
    
    /**
     * 单据状态: DOC_STATUS
     */
    public final java.lang.String getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 单据状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.String value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
        }
    }
    
    /**
     * 审核状态: APPROVE_STATUS
     */
    public final java.lang.String getApproveStatus(){
         onPropGet(PROP_ID_approveStatus);
         return _approveStatus;
    }

    /**
     * 审核状态: APPROVE_STATUS
     */
    public final void setApproveStatus(java.lang.String value){
        if(onPropSet(PROP_ID_approveStatus,value)){
            this._approveStatus = value;
            internalClearRefs(PROP_ID_approveStatus);
            
        }
    }
    
    /**
     * 审核人: APPROVED_BY
     */
    public final java.lang.String getApprovedBy(){
         onPropGet(PROP_ID_approvedBy);
         return _approvedBy;
    }

    /**
     * 审核人: APPROVED_BY
     */
    public final void setApprovedBy(java.lang.String value){
        if(onPropSet(PROP_ID_approvedBy,value)){
            this._approvedBy = value;
            internalClearRefs(PROP_ID_approvedBy);
            
        }
    }
    
    /**
     * 审核时间: APPROVED_AT
     */
    public final java.time.LocalDateTime getApprovedAt(){
         onPropGet(PROP_ID_approvedAt);
         return _approvedAt;
    }

    /**
     * 审核时间: APPROVED_AT
     */
    public final void setApprovedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_approvedAt,value)){
            this._approvedAt = value;
            internalClearRefs(PROP_ID_approvedAt);
            
        }
    }
    
    /**
     * 是否已过账: POSTED
     */
    public final java.lang.Boolean getPosted(){
         onPropGet(PROP_ID_posted);
         return _posted;
    }

    /**
     * 是否已过账: POSTED
     */
    public final void setPosted(java.lang.Boolean value){
        if(onPropSet(PROP_ID_posted,value)){
            this._posted = value;
            internalClearRefs(PROP_ID_posted);
            
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
     * 过账时间: POSTED_AT
     */
    public final java.time.LocalDateTime getPostedAt(){
         onPropGet(PROP_ID_postedAt);
         return _postedAt;
    }

    /**
     * 过账时间: POSTED_AT
     */
    public final void setPostedAt(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_postedAt,value)){
            this._postedAt = value;
            internalClearRefs(PROP_ID_postedAt);
            
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
    public final app.erp.md.dao.entity.ErpMdEmployee getEmployee(){
       return (app.erp.md.dao.entity.ErpMdEmployee)internalGetRefEntity(PROP_NAME_employee);
    }

    public final void setEmployee(app.erp.md.dao.entity.ErpMdEmployee refEntity){
   
           if(refEntity == null){
           
                   this.setEmployeeId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_employee, refEntity,()->{
           
                           this.setEmployeeId(refEntity.getId());
                       
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
    public final app.erp.prj.dao.entity.ErpPrjProject getProject(){
       return (app.erp.prj.dao.entity.ErpPrjProject)internalGetRefEntity(PROP_NAME_project);
    }

    public final void setProject(app.erp.prj.dao.entity.ErpPrjProject refEntity){
   
           if(refEntity == null){
           
                   this.setProjectId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_project, refEntity,()->{
           
                           this.setProjectId(refEntity.getId());
                       
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
