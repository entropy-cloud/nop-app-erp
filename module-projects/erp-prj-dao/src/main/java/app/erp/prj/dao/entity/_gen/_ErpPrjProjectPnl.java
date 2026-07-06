package app.erp.prj.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.prj.dao.entity.ErpPrjProjectPnl;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  项目损益汇总: erp_prj_project_pnl
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpPrjProjectPnl extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 项目: PROJECT_ID BIGINT */
    public static final String PROP_NAME_projectId = "projectId";
    public static final int PROP_ID_projectId = 3;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 汇总期间起: PERIOD_FROM DATE */
    public static final String PROP_NAME_periodFrom = "periodFrom";
    public static final int PROP_ID_periodFrom = 5;
    
    /* 汇总期间止: PERIOD_TO DATE */
    public static final String PROP_NAME_periodTo = "periodTo";
    public static final int PROP_ID_periodTo = 6;
    
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
    
    /* 收入合计: REVENUE_AMOUNT DECIMAL */
    public static final String PROP_NAME_revenueAmount = "revenueAmount";
    public static final int PROP_ID_revenueAmount = 11;
    
    /* 人工成本: COST_LABOR DECIMAL */
    public static final String PROP_NAME_costLabor = "costLabor";
    public static final int PROP_ID_costLabor = 12;
    
    /* 物料成本: COST_MATERIAL DECIMAL */
    public static final String PROP_NAME_costMaterial = "costMaterial";
    public static final int PROP_ID_costMaterial = 13;
    
    /* 费用成本: COST_EXPENSE DECIMAL */
    public static final String PROP_NAME_costExpense = "costExpense";
    public static final int PROP_ID_costExpense = 14;
    
    /* 分包成本: COST_SUBCONTRACT DECIMAL */
    public static final String PROP_NAME_costSubcontract = "costSubcontract";
    public static final int PROP_ID_costSubcontract = 15;
    
    /* 成本合计: TOTAL_COST DECIMAL */
    public static final String PROP_NAME_totalCost = "totalCost";
    public static final int PROP_ID_totalCost = 16;
    
    /* 毛利: GROSS_PROFIT DECIMAL */
    public static final String PROP_NAME_grossProfit = "grossProfit";
    public static final int PROP_ID_grossProfit = 17;
    
    /* 毛利率%: GROSS_MARGIN_PCT DECIMAL */
    public static final String PROP_NAME_grossMarginPct = "grossMarginPct";
    public static final int PROP_ID_grossMarginPct = 18;
    
    /* 已承诺成本: COMMITTED_COST DECIMAL */
    public static final String PROP_NAME_committedCost = "committedCost";
    public static final int PROP_ID_committedCost = 19;
    
    /* 预算: BUDGET_AMOUNT DECIMAL */
    public static final String PROP_NAME_budgetAmount = "budgetAmount";
    public static final int PROP_ID_budgetAmount = 20;
    
    /* 完工预测成本(EAC): FORECAST_COMPLETE_COST DECIMAL */
    public static final String PROP_NAME_forecastCompleteCost = "forecastCompleteCost";
    public static final int PROP_ID_forecastCompleteCost = 21;
    
    /* 计算状态: CALC_STATUS VARCHAR */
    public static final String PROP_NAME_calcStatus = "calcStatus";
    public static final int PROP_ID_calcStatus = 22;
    
    /* 单据状态: DOC_STATUS VARCHAR */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 23;
    
    /* 审核状态: APPROVE_STATUS VARCHAR */
    public static final String PROP_NAME_approveStatus = "approveStatus";
    public static final int PROP_ID_approveStatus = 24;
    
    /* 已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 25;
    
    /* 过账时间: POSTED_AT DATETIME */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 26;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 27;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 28;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 29;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 30;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 31;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 32;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 33;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 34;
    

    private static int _PROP_ID_BOUND = 35;

    
    /* relation:  */
    public static final String PROP_NAME_project = "project";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[35];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_projectId] = PROP_NAME_projectId;
          PROP_NAME_TO_ID.put(PROP_NAME_projectId, PROP_ID_projectId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_periodFrom] = PROP_NAME_periodFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_periodFrom, PROP_ID_periodFrom);
      
          PROP_ID_TO_NAME[PROP_ID_periodTo] = PROP_NAME_periodTo;
          PROP_NAME_TO_ID.put(PROP_NAME_periodTo, PROP_ID_periodTo);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_amountSource] = PROP_NAME_amountSource;
          PROP_NAME_TO_ID.put(PROP_NAME_amountSource, PROP_ID_amountSource);
      
          PROP_ID_TO_NAME[PROP_ID_amountFunctional] = PROP_NAME_amountFunctional;
          PROP_NAME_TO_ID.put(PROP_NAME_amountFunctional, PROP_ID_amountFunctional);
      
          PROP_ID_TO_NAME[PROP_ID_revenueAmount] = PROP_NAME_revenueAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_revenueAmount, PROP_ID_revenueAmount);
      
          PROP_ID_TO_NAME[PROP_ID_costLabor] = PROP_NAME_costLabor;
          PROP_NAME_TO_ID.put(PROP_NAME_costLabor, PROP_ID_costLabor);
      
          PROP_ID_TO_NAME[PROP_ID_costMaterial] = PROP_NAME_costMaterial;
          PROP_NAME_TO_ID.put(PROP_NAME_costMaterial, PROP_ID_costMaterial);
      
          PROP_ID_TO_NAME[PROP_ID_costExpense] = PROP_NAME_costExpense;
          PROP_NAME_TO_ID.put(PROP_NAME_costExpense, PROP_ID_costExpense);
      
          PROP_ID_TO_NAME[PROP_ID_costSubcontract] = PROP_NAME_costSubcontract;
          PROP_NAME_TO_ID.put(PROP_NAME_costSubcontract, PROP_ID_costSubcontract);
      
          PROP_ID_TO_NAME[PROP_ID_totalCost] = PROP_NAME_totalCost;
          PROP_NAME_TO_ID.put(PROP_NAME_totalCost, PROP_ID_totalCost);
      
          PROP_ID_TO_NAME[PROP_ID_grossProfit] = PROP_NAME_grossProfit;
          PROP_NAME_TO_ID.put(PROP_NAME_grossProfit, PROP_ID_grossProfit);
      
          PROP_ID_TO_NAME[PROP_ID_grossMarginPct] = PROP_NAME_grossMarginPct;
          PROP_NAME_TO_ID.put(PROP_NAME_grossMarginPct, PROP_ID_grossMarginPct);
      
          PROP_ID_TO_NAME[PROP_ID_committedCost] = PROP_NAME_committedCost;
          PROP_NAME_TO_ID.put(PROP_NAME_committedCost, PROP_ID_committedCost);
      
          PROP_ID_TO_NAME[PROP_ID_budgetAmount] = PROP_NAME_budgetAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_budgetAmount, PROP_ID_budgetAmount);
      
          PROP_ID_TO_NAME[PROP_ID_forecastCompleteCost] = PROP_NAME_forecastCompleteCost;
          PROP_NAME_TO_ID.put(PROP_NAME_forecastCompleteCost, PROP_ID_forecastCompleteCost);
      
          PROP_ID_TO_NAME[PROP_ID_calcStatus] = PROP_NAME_calcStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_calcStatus, PROP_ID_calcStatus);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
          PROP_ID_TO_NAME[PROP_ID_approveStatus] = PROP_NAME_approveStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_approveStatus, PROP_ID_approveStatus);
      
          PROP_ID_TO_NAME[PROP_ID_posted] = PROP_NAME_posted;
          PROP_NAME_TO_ID.put(PROP_NAME_posted, PROP_ID_posted);
      
          PROP_ID_TO_NAME[PROP_ID_postedAt] = PROP_NAME_postedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_postedAt, PROP_ID_postedAt);
      
          PROP_ID_TO_NAME[PROP_ID_postedBy] = PROP_NAME_postedBy;
          PROP_NAME_TO_ID.put(PROP_NAME_postedBy, PROP_ID_postedBy);
      
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
    
    /* 项目: PROJECT_ID */
    private java.lang.Long _projectId;
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 汇总期间起: PERIOD_FROM */
    private java.time.LocalDate _periodFrom;
    
    /* 汇总期间止: PERIOD_TO */
    private java.time.LocalDate _periodTo;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 源币种金额: AMOUNT_SOURCE */
    private java.math.BigDecimal _amountSource;
    
    /* 本位币金额: AMOUNT_FUNCTIONAL */
    private java.math.BigDecimal _amountFunctional;
    
    /* 收入合计: REVENUE_AMOUNT */
    private java.math.BigDecimal _revenueAmount;
    
    /* 人工成本: COST_LABOR */
    private java.math.BigDecimal _costLabor;
    
    /* 物料成本: COST_MATERIAL */
    private java.math.BigDecimal _costMaterial;
    
    /* 费用成本: COST_EXPENSE */
    private java.math.BigDecimal _costExpense;
    
    /* 分包成本: COST_SUBCONTRACT */
    private java.math.BigDecimal _costSubcontract;
    
    /* 成本合计: TOTAL_COST */
    private java.math.BigDecimal _totalCost;
    
    /* 毛利: GROSS_PROFIT */
    private java.math.BigDecimal _grossProfit;
    
    /* 毛利率%: GROSS_MARGIN_PCT */
    private java.lang.String _grossMarginPct;
    
    /* 已承诺成本: COMMITTED_COST */
    private java.math.BigDecimal _committedCost;
    
    /* 预算: BUDGET_AMOUNT */
    private java.math.BigDecimal _budgetAmount;
    
    /* 完工预测成本(EAC): FORECAST_COMPLETE_COST */
    private java.math.BigDecimal _forecastCompleteCost;
    
    /* 计算状态: CALC_STATUS */
    private java.lang.String _calcStatus;
    
    /* 单据状态: DOC_STATUS */
    private java.lang.String _docStatus;
    
    /* 审核状态: APPROVE_STATUS */
    private java.lang.String _approveStatus;
    
    /* 已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账时间: POSTED_AT */
    private java.time.LocalDateTime _postedAt;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
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
    

    public _ErpPrjProjectPnl(){
        // for debug
    }

    protected ErpPrjProjectPnl newInstance(){
        ErpPrjProjectPnl entity = new ErpPrjProjectPnl();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpPrjProjectPnl cloneInstance() {
        ErpPrjProjectPnl entity = newInstance();
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
      return "app.erp.prj.dao.entity.ErpPrjProjectPnl";
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
        
            case PROP_ID_projectId:
               return getProjectId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_periodFrom:
               return getPeriodFrom();
        
            case PROP_ID_periodTo:
               return getPeriodTo();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_amountSource:
               return getAmountSource();
        
            case PROP_ID_amountFunctional:
               return getAmountFunctional();
        
            case PROP_ID_revenueAmount:
               return getRevenueAmount();
        
            case PROP_ID_costLabor:
               return getCostLabor();
        
            case PROP_ID_costMaterial:
               return getCostMaterial();
        
            case PROP_ID_costExpense:
               return getCostExpense();
        
            case PROP_ID_costSubcontract:
               return getCostSubcontract();
        
            case PROP_ID_totalCost:
               return getTotalCost();
        
            case PROP_ID_grossProfit:
               return getGrossProfit();
        
            case PROP_ID_grossMarginPct:
               return getGrossMarginPct();
        
            case PROP_ID_committedCost:
               return getCommittedCost();
        
            case PROP_ID_budgetAmount:
               return getBudgetAmount();
        
            case PROP_ID_forecastCompleteCost:
               return getForecastCompleteCost();
        
            case PROP_ID_calcStatus:
               return getCalcStatus();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
            case PROP_ID_approveStatus:
               return getApproveStatus();
        
            case PROP_ID_posted:
               return getPosted();
        
            case PROP_ID_postedAt:
               return getPostedAt();
        
            case PROP_ID_postedBy:
               return getPostedBy();
        
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
        
            case PROP_ID_projectId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_projectId));
               }
               setProjectId(typedValue);
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
        
            case PROP_ID_periodFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodFrom));
               }
               setPeriodFrom(typedValue);
               break;
            }
        
            case PROP_ID_periodTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_periodTo));
               }
               setPeriodTo(typedValue);
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
        
            case PROP_ID_revenueAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_revenueAmount));
               }
               setRevenueAmount(typedValue);
               break;
            }
        
            case PROP_ID_costLabor:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_costLabor));
               }
               setCostLabor(typedValue);
               break;
            }
        
            case PROP_ID_costMaterial:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_costMaterial));
               }
               setCostMaterial(typedValue);
               break;
            }
        
            case PROP_ID_costExpense:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_costExpense));
               }
               setCostExpense(typedValue);
               break;
            }
        
            case PROP_ID_costSubcontract:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_costSubcontract));
               }
               setCostSubcontract(typedValue);
               break;
            }
        
            case PROP_ID_totalCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalCost));
               }
               setTotalCost(typedValue);
               break;
            }
        
            case PROP_ID_grossProfit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_grossProfit));
               }
               setGrossProfit(typedValue);
               break;
            }
        
            case PROP_ID_grossMarginPct:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_grossMarginPct));
               }
               setGrossMarginPct(typedValue);
               break;
            }
        
            case PROP_ID_committedCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_committedCost));
               }
               setCommittedCost(typedValue);
               break;
            }
        
            case PROP_ID_budgetAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_budgetAmount));
               }
               setBudgetAmount(typedValue);
               break;
            }
        
            case PROP_ID_forecastCompleteCost:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_forecastCompleteCost));
               }
               setForecastCompleteCost(typedValue);
               break;
            }
        
            case PROP_ID_calcStatus:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_calcStatus));
               }
               setCalcStatus(typedValue);
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
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
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
        
            case PROP_ID_projectId:{
               onInitProp(propId);
               this._projectId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_periodFrom:{
               onInitProp(propId);
               this._periodFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_periodTo:{
               onInitProp(propId);
               this._periodTo = (java.time.LocalDate)value;
               
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
        
            case PROP_ID_revenueAmount:{
               onInitProp(propId);
               this._revenueAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_costLabor:{
               onInitProp(propId);
               this._costLabor = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_costMaterial:{
               onInitProp(propId);
               this._costMaterial = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_costExpense:{
               onInitProp(propId);
               this._costExpense = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_costSubcontract:{
               onInitProp(propId);
               this._costSubcontract = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalCost:{
               onInitProp(propId);
               this._totalCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_grossProfit:{
               onInitProp(propId);
               this._grossProfit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_grossMarginPct:{
               onInitProp(propId);
               this._grossMarginPct = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_committedCost:{
               onInitProp(propId);
               this._committedCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_budgetAmount:{
               onInitProp(propId);
               this._budgetAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_forecastCompleteCost:{
               onInitProp(propId);
               this._forecastCompleteCost = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_calcStatus:{
               onInitProp(propId);
               this._calcStatus = (java.lang.String)value;
               
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
        
            case PROP_ID_posted:{
               onInitProp(propId);
               this._posted = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_postedAt:{
               onInitProp(propId);
               this._postedAt = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_postedBy:{
               onInitProp(propId);
               this._postedBy = (java.lang.String)value;
               
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
     * 项目: PROJECT_ID
     */
    public final java.lang.Long getProjectId(){
         onPropGet(PROP_ID_projectId);
         return _projectId;
    }

    /**
     * 项目: PROJECT_ID
     */
    public final void setProjectId(java.lang.Long value){
        if(onPropSet(PROP_ID_projectId,value)){
            this._projectId = value;
            internalClearRefs(PROP_ID_projectId);
            
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
     * 汇总期间起: PERIOD_FROM
     */
    public final java.time.LocalDate getPeriodFrom(){
         onPropGet(PROP_ID_periodFrom);
         return _periodFrom;
    }

    /**
     * 汇总期间起: PERIOD_FROM
     */
    public final void setPeriodFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodFrom,value)){
            this._periodFrom = value;
            internalClearRefs(PROP_ID_periodFrom);
            
        }
    }
    
    /**
     * 汇总期间止: PERIOD_TO
     */
    public final java.time.LocalDate getPeriodTo(){
         onPropGet(PROP_ID_periodTo);
         return _periodTo;
    }

    /**
     * 汇总期间止: PERIOD_TO
     */
    public final void setPeriodTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_periodTo,value)){
            this._periodTo = value;
            internalClearRefs(PROP_ID_periodTo);
            
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
     * 收入合计: REVENUE_AMOUNT
     */
    public final java.math.BigDecimal getRevenueAmount(){
         onPropGet(PROP_ID_revenueAmount);
         return _revenueAmount;
    }

    /**
     * 收入合计: REVENUE_AMOUNT
     */
    public final void setRevenueAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_revenueAmount,value)){
            this._revenueAmount = value;
            internalClearRefs(PROP_ID_revenueAmount);
            
        }
    }
    
    /**
     * 人工成本: COST_LABOR
     */
    public final java.math.BigDecimal getCostLabor(){
         onPropGet(PROP_ID_costLabor);
         return _costLabor;
    }

    /**
     * 人工成本: COST_LABOR
     */
    public final void setCostLabor(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_costLabor,value)){
            this._costLabor = value;
            internalClearRefs(PROP_ID_costLabor);
            
        }
    }
    
    /**
     * 物料成本: COST_MATERIAL
     */
    public final java.math.BigDecimal getCostMaterial(){
         onPropGet(PROP_ID_costMaterial);
         return _costMaterial;
    }

    /**
     * 物料成本: COST_MATERIAL
     */
    public final void setCostMaterial(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_costMaterial,value)){
            this._costMaterial = value;
            internalClearRefs(PROP_ID_costMaterial);
            
        }
    }
    
    /**
     * 费用成本: COST_EXPENSE
     */
    public final java.math.BigDecimal getCostExpense(){
         onPropGet(PROP_ID_costExpense);
         return _costExpense;
    }

    /**
     * 费用成本: COST_EXPENSE
     */
    public final void setCostExpense(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_costExpense,value)){
            this._costExpense = value;
            internalClearRefs(PROP_ID_costExpense);
            
        }
    }
    
    /**
     * 分包成本: COST_SUBCONTRACT
     */
    public final java.math.BigDecimal getCostSubcontract(){
         onPropGet(PROP_ID_costSubcontract);
         return _costSubcontract;
    }

    /**
     * 分包成本: COST_SUBCONTRACT
     */
    public final void setCostSubcontract(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_costSubcontract,value)){
            this._costSubcontract = value;
            internalClearRefs(PROP_ID_costSubcontract);
            
        }
    }
    
    /**
     * 成本合计: TOTAL_COST
     */
    public final java.math.BigDecimal getTotalCost(){
         onPropGet(PROP_ID_totalCost);
         return _totalCost;
    }

    /**
     * 成本合计: TOTAL_COST
     */
    public final void setTotalCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalCost,value)){
            this._totalCost = value;
            internalClearRefs(PROP_ID_totalCost);
            
        }
    }
    
    /**
     * 毛利: GROSS_PROFIT
     */
    public final java.math.BigDecimal getGrossProfit(){
         onPropGet(PROP_ID_grossProfit);
         return _grossProfit;
    }

    /**
     * 毛利: GROSS_PROFIT
     */
    public final void setGrossProfit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_grossProfit,value)){
            this._grossProfit = value;
            internalClearRefs(PROP_ID_grossProfit);
            
        }
    }
    
    /**
     * 毛利率%: GROSS_MARGIN_PCT
     */
    public final java.lang.String getGrossMarginPct(){
         onPropGet(PROP_ID_grossMarginPct);
         return _grossMarginPct;
    }

    /**
     * 毛利率%: GROSS_MARGIN_PCT
     */
    public final void setGrossMarginPct(java.lang.String value){
        if(onPropSet(PROP_ID_grossMarginPct,value)){
            this._grossMarginPct = value;
            internalClearRefs(PROP_ID_grossMarginPct);
            
        }
    }
    
    /**
     * 已承诺成本: COMMITTED_COST
     */
    public final java.math.BigDecimal getCommittedCost(){
         onPropGet(PROP_ID_committedCost);
         return _committedCost;
    }

    /**
     * 已承诺成本: COMMITTED_COST
     */
    public final void setCommittedCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_committedCost,value)){
            this._committedCost = value;
            internalClearRefs(PROP_ID_committedCost);
            
        }
    }
    
    /**
     * 预算: BUDGET_AMOUNT
     */
    public final java.math.BigDecimal getBudgetAmount(){
         onPropGet(PROP_ID_budgetAmount);
         return _budgetAmount;
    }

    /**
     * 预算: BUDGET_AMOUNT
     */
    public final void setBudgetAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_budgetAmount,value)){
            this._budgetAmount = value;
            internalClearRefs(PROP_ID_budgetAmount);
            
        }
    }
    
    /**
     * 完工预测成本(EAC): FORECAST_COMPLETE_COST
     */
    public final java.math.BigDecimal getForecastCompleteCost(){
         onPropGet(PROP_ID_forecastCompleteCost);
         return _forecastCompleteCost;
    }

    /**
     * 完工预测成本(EAC): FORECAST_COMPLETE_COST
     */
    public final void setForecastCompleteCost(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_forecastCompleteCost,value)){
            this._forecastCompleteCost = value;
            internalClearRefs(PROP_ID_forecastCompleteCost);
            
        }
    }
    
    /**
     * 计算状态: CALC_STATUS
     */
    public final java.lang.String getCalcStatus(){
         onPropGet(PROP_ID_calcStatus);
         return _calcStatus;
    }

    /**
     * 计算状态: CALC_STATUS
     */
    public final void setCalcStatus(java.lang.String value){
        if(onPropSet(PROP_ID_calcStatus,value)){
            this._calcStatus = value;
            internalClearRefs(PROP_ID_calcStatus);
            
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
