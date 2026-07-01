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

import app.erp.fin.dao.entity.ErpFinBankStatement;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  银行对账单: erp_fin_bank_statement
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinBankStatement extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 单号: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 资金账户: FUND_ACCOUNT_ID BIGINT */
    public static final String PROP_NAME_fundAccountId = "fundAccountId";
    public static final int PROP_ID_fundAccountId = 4;
    
    /* 对账日期: STATEMENT_DATE DATE */
    public static final String PROP_NAME_statementDate = "statementDate";
    public static final int PROP_ID_statementDate = 5;
    
    /* 期初余额: BEGINNING_BALANCE DECIMAL */
    public static final String PROP_NAME_beginningBalance = "beginningBalance";
    public static final int PROP_ID_beginningBalance = 6;
    
    /* 期末余额: ENDING_BALANCE DECIMAL */
    public static final String PROP_NAME_endingBalance = "endingBalance";
    public static final int PROP_ID_endingBalance = 7;
    
    /* 借方发生额: TOTAL_DEBIT DECIMAL */
    public static final String PROP_NAME_totalDebit = "totalDebit";
    public static final int PROP_ID_totalDebit = 8;
    
    /* 贷方发生额: TOTAL_CREDIT DECIMAL */
    public static final String PROP_NAME_totalCredit = "totalCredit";
    public static final int PROP_ID_totalCredit = 9;
    
    /* 导入时间: IMPORT_TIME DATETIME */
    public static final String PROP_NAME_importTime = "importTime";
    public static final int PROP_ID_importTime = 10;
    
    /* 状态: DOC_STATUS INTEGER */
    public static final String PROP_NAME_docStatus = "docStatus";
    public static final int PROP_ID_docStatus = 11;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 12;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 13;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 14;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 15;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 16;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 17;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 18;
    

    private static int _PROP_ID_BOUND = 19;

    
    /* relation:  */
    public static final String PROP_NAME_fundAccount = "fundAccount";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[19];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_fundAccountId] = PROP_NAME_fundAccountId;
          PROP_NAME_TO_ID.put(PROP_NAME_fundAccountId, PROP_ID_fundAccountId);
      
          PROP_ID_TO_NAME[PROP_ID_statementDate] = PROP_NAME_statementDate;
          PROP_NAME_TO_ID.put(PROP_NAME_statementDate, PROP_ID_statementDate);
      
          PROP_ID_TO_NAME[PROP_ID_beginningBalance] = PROP_NAME_beginningBalance;
          PROP_NAME_TO_ID.put(PROP_NAME_beginningBalance, PROP_ID_beginningBalance);
      
          PROP_ID_TO_NAME[PROP_ID_endingBalance] = PROP_NAME_endingBalance;
          PROP_NAME_TO_ID.put(PROP_NAME_endingBalance, PROP_ID_endingBalance);
      
          PROP_ID_TO_NAME[PROP_ID_totalDebit] = PROP_NAME_totalDebit;
          PROP_NAME_TO_ID.put(PROP_NAME_totalDebit, PROP_ID_totalDebit);
      
          PROP_ID_TO_NAME[PROP_ID_totalCredit] = PROP_NAME_totalCredit;
          PROP_NAME_TO_ID.put(PROP_NAME_totalCredit, PROP_ID_totalCredit);
      
          PROP_ID_TO_NAME[PROP_ID_importTime] = PROP_NAME_importTime;
          PROP_NAME_TO_ID.put(PROP_NAME_importTime, PROP_ID_importTime);
      
          PROP_ID_TO_NAME[PROP_ID_docStatus] = PROP_NAME_docStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_docStatus, PROP_ID_docStatus);
      
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
    
    /* 资金账户: FUND_ACCOUNT_ID */
    private java.lang.Long _fundAccountId;
    
    /* 对账日期: STATEMENT_DATE */
    private java.time.LocalDate _statementDate;
    
    /* 期初余额: BEGINNING_BALANCE */
    private java.math.BigDecimal _beginningBalance;
    
    /* 期末余额: ENDING_BALANCE */
    private java.math.BigDecimal _endingBalance;
    
    /* 借方发生额: TOTAL_DEBIT */
    private java.math.BigDecimal _totalDebit;
    
    /* 贷方发生额: TOTAL_CREDIT */
    private java.math.BigDecimal _totalCredit;
    
    /* 导入时间: IMPORT_TIME */
    private java.time.LocalDateTime _importTime;
    
    /* 状态: DOC_STATUS */
    private java.lang.Integer _docStatus;
    
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
    

    public _ErpFinBankStatement(){
        // for debug
    }

    protected ErpFinBankStatement newInstance(){
        ErpFinBankStatement entity = new ErpFinBankStatement();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinBankStatement cloneInstance() {
        ErpFinBankStatement entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinBankStatement";
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
        
            case PROP_ID_fundAccountId:
               return getFundAccountId();
        
            case PROP_ID_statementDate:
               return getStatementDate();
        
            case PROP_ID_beginningBalance:
               return getBeginningBalance();
        
            case PROP_ID_endingBalance:
               return getEndingBalance();
        
            case PROP_ID_totalDebit:
               return getTotalDebit();
        
            case PROP_ID_totalCredit:
               return getTotalCredit();
        
            case PROP_ID_importTime:
               return getImportTime();
        
            case PROP_ID_docStatus:
               return getDocStatus();
        
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
        
            case PROP_ID_fundAccountId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_fundAccountId));
               }
               setFundAccountId(typedValue);
               break;
            }
        
            case PROP_ID_statementDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_statementDate));
               }
               setStatementDate(typedValue);
               break;
            }
        
            case PROP_ID_beginningBalance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_beginningBalance));
               }
               setBeginningBalance(typedValue);
               break;
            }
        
            case PROP_ID_endingBalance:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_endingBalance));
               }
               setEndingBalance(typedValue);
               break;
            }
        
            case PROP_ID_totalDebit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalDebit));
               }
               setTotalDebit(typedValue);
               break;
            }
        
            case PROP_ID_totalCredit:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalCredit));
               }
               setTotalCredit(typedValue);
               break;
            }
        
            case PROP_ID_importTime:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_importTime));
               }
               setImportTime(typedValue);
               break;
            }
        
            case PROP_ID_docStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_docStatus));
               }
               setDocStatus(typedValue);
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
        
            case PROP_ID_fundAccountId:{
               onInitProp(propId);
               this._fundAccountId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_statementDate:{
               onInitProp(propId);
               this._statementDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_beginningBalance:{
               onInitProp(propId);
               this._beginningBalance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_endingBalance:{
               onInitProp(propId);
               this._endingBalance = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalDebit:{
               onInitProp(propId);
               this._totalDebit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalCredit:{
               onInitProp(propId);
               this._totalCredit = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_importTime:{
               onInitProp(propId);
               this._importTime = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_docStatus:{
               onInitProp(propId);
               this._docStatus = (java.lang.Integer)value;
               
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
     * 资金账户: FUND_ACCOUNT_ID
     */
    public final java.lang.Long getFundAccountId(){
         onPropGet(PROP_ID_fundAccountId);
         return _fundAccountId;
    }

    /**
     * 资金账户: FUND_ACCOUNT_ID
     */
    public final void setFundAccountId(java.lang.Long value){
        if(onPropSet(PROP_ID_fundAccountId,value)){
            this._fundAccountId = value;
            internalClearRefs(PROP_ID_fundAccountId);
            
        }
    }
    
    /**
     * 对账日期: STATEMENT_DATE
     */
    public final java.time.LocalDate getStatementDate(){
         onPropGet(PROP_ID_statementDate);
         return _statementDate;
    }

    /**
     * 对账日期: STATEMENT_DATE
     */
    public final void setStatementDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_statementDate,value)){
            this._statementDate = value;
            internalClearRefs(PROP_ID_statementDate);
            
        }
    }
    
    /**
     * 期初余额: BEGINNING_BALANCE
     */
    public final java.math.BigDecimal getBeginningBalance(){
         onPropGet(PROP_ID_beginningBalance);
         return _beginningBalance;
    }

    /**
     * 期初余额: BEGINNING_BALANCE
     */
    public final void setBeginningBalance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_beginningBalance,value)){
            this._beginningBalance = value;
            internalClearRefs(PROP_ID_beginningBalance);
            
        }
    }
    
    /**
     * 期末余额: ENDING_BALANCE
     */
    public final java.math.BigDecimal getEndingBalance(){
         onPropGet(PROP_ID_endingBalance);
         return _endingBalance;
    }

    /**
     * 期末余额: ENDING_BALANCE
     */
    public final void setEndingBalance(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_endingBalance,value)){
            this._endingBalance = value;
            internalClearRefs(PROP_ID_endingBalance);
            
        }
    }
    
    /**
     * 借方发生额: TOTAL_DEBIT
     */
    public final java.math.BigDecimal getTotalDebit(){
         onPropGet(PROP_ID_totalDebit);
         return _totalDebit;
    }

    /**
     * 借方发生额: TOTAL_DEBIT
     */
    public final void setTotalDebit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalDebit,value)){
            this._totalDebit = value;
            internalClearRefs(PROP_ID_totalDebit);
            
        }
    }
    
    /**
     * 贷方发生额: TOTAL_CREDIT
     */
    public final java.math.BigDecimal getTotalCredit(){
         onPropGet(PROP_ID_totalCredit);
         return _totalCredit;
    }

    /**
     * 贷方发生额: TOTAL_CREDIT
     */
    public final void setTotalCredit(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalCredit,value)){
            this._totalCredit = value;
            internalClearRefs(PROP_ID_totalCredit);
            
        }
    }
    
    /**
     * 导入时间: IMPORT_TIME
     */
    public final java.time.LocalDateTime getImportTime(){
         onPropGet(PROP_ID_importTime);
         return _importTime;
    }

    /**
     * 导入时间: IMPORT_TIME
     */
    public final void setImportTime(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_importTime,value)){
            this._importTime = value;
            internalClearRefs(PROP_ID_importTime);
            
        }
    }
    
    /**
     * 状态: DOC_STATUS
     */
    public final java.lang.Integer getDocStatus(){
         onPropGet(PROP_ID_docStatus);
         return _docStatus;
    }

    /**
     * 状态: DOC_STATUS
     */
    public final void setDocStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_docStatus,value)){
            this._docStatus = value;
            internalClearRefs(PROP_ID_docStatus);
            
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
    public final app.erp.fin.dao.entity.ErpFinFundAccount getFundAccount(){
       return (app.erp.fin.dao.entity.ErpFinFundAccount)internalGetRefEntity(PROP_NAME_fundAccount);
    }

    public final void setFundAccount(app.erp.fin.dao.entity.ErpFinFundAccount refEntity){
   
           if(refEntity == null){
           
                   this.setFundAccountId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_fundAccount, refEntity,()->{
           
                           this.setFundAccountId(refEntity.getId());
                       
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
