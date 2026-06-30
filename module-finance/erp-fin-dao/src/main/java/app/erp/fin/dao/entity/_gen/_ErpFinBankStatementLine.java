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

import app.erp.fin.dao.entity.ErpFinBankStatementLine;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  银行对账单行: erp_fin_bank_statement_line
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinBankStatementLine extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 对账单ID: STATEMENT_ID BIGINT */
    public static final String PROP_NAME_statementId = "statementId";
    public static final int PROP_ID_statementId = 2;
    
    /* 行号: LINE_NO INTEGER */
    public static final String PROP_NAME_lineNo = "lineNo";
    public static final int PROP_ID_lineNo = 3;
    
    /* 交易日期: TRANSACTION_DATE DATE */
    public static final String PROP_NAME_transactionDate = "transactionDate";
    public static final int PROP_ID_transactionDate = 4;
    
    /* 摘要(银行): DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 5;
    
    /* 银行参考号: REF_NO VARCHAR */
    public static final String PROP_NAME_refNo = "refNo";
    public static final int PROP_ID_refNo = 6;
    
    /* 借贷方向: DC_DIRECTION INTEGER */
    public static final String PROP_NAME_dcDirection = "dcDirection";
    public static final int PROP_ID_dcDirection = 7;
    
    /* 金额: AMOUNT DECIMAL */
    public static final String PROP_NAME_amount = "amount";
    public static final int PROP_ID_amount = 8;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 9;
    
    /* 交易后余额: BALANCE_AFTER DECIMAL */
    public static final String PROP_NAME_balanceAfter = "balanceAfter";
    public static final int PROP_ID_balanceAfter = 10;
    
    /* 匹配状态: MATCH_STATUS INTEGER */
    public static final String PROP_NAME_matchStatus = "matchStatus";
    public static final int PROP_ID_matchStatus = 11;
    
    /* 匹配凭证行ID: MATCHED_LINE_ID BIGINT */
    public static final String PROP_NAME_matchedLineId = "matchedLineId";
    public static final int PROP_ID_matchedLineId = 12;
    
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
    public static final String PROP_NAME_statement = "statement";
    
    /* relation:  */
    public static final String PROP_NAME_matchedLine = "matchedLine";
    
    /* relation:  */
    public static final String PROP_NAME_currency = "currency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[20];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_statementId] = PROP_NAME_statementId;
          PROP_NAME_TO_ID.put(PROP_NAME_statementId, PROP_ID_statementId);
      
          PROP_ID_TO_NAME[PROP_ID_lineNo] = PROP_NAME_lineNo;
          PROP_NAME_TO_ID.put(PROP_NAME_lineNo, PROP_ID_lineNo);
      
          PROP_ID_TO_NAME[PROP_ID_transactionDate] = PROP_NAME_transactionDate;
          PROP_NAME_TO_ID.put(PROP_NAME_transactionDate, PROP_ID_transactionDate);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
          PROP_ID_TO_NAME[PROP_ID_refNo] = PROP_NAME_refNo;
          PROP_NAME_TO_ID.put(PROP_NAME_refNo, PROP_ID_refNo);
      
          PROP_ID_TO_NAME[PROP_ID_dcDirection] = PROP_NAME_dcDirection;
          PROP_NAME_TO_ID.put(PROP_NAME_dcDirection, PROP_ID_dcDirection);
      
          PROP_ID_TO_NAME[PROP_ID_amount] = PROP_NAME_amount;
          PROP_NAME_TO_ID.put(PROP_NAME_amount, PROP_ID_amount);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_balanceAfter] = PROP_NAME_balanceAfter;
          PROP_NAME_TO_ID.put(PROP_NAME_balanceAfter, PROP_ID_balanceAfter);
      
          PROP_ID_TO_NAME[PROP_ID_matchStatus] = PROP_NAME_matchStatus;
          PROP_NAME_TO_ID.put(PROP_NAME_matchStatus, PROP_ID_matchStatus);
      
          PROP_ID_TO_NAME[PROP_ID_matchedLineId] = PROP_NAME_matchedLineId;
          PROP_NAME_TO_ID.put(PROP_NAME_matchedLineId, PROP_ID_matchedLineId);
      
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
    
    /* 对账单ID: STATEMENT_ID */
    private java.lang.Long _statementId;
    
    /* 行号: LINE_NO */
    private java.lang.Integer _lineNo;
    
    /* 交易日期: TRANSACTION_DATE */
    private java.time.LocalDate _transactionDate;
    
    /* 摘要(银行): DESCRIPTION */
    private java.lang.String _description;
    
    /* 银行参考号: REF_NO */
    private java.lang.String _refNo;
    
    /* 借贷方向: DC_DIRECTION */
    private java.lang.Integer _dcDirection;
    
    /* 金额: AMOUNT */
    private java.lang.String _amount;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 交易后余额: BALANCE_AFTER */
    private java.lang.String _balanceAfter;
    
    /* 匹配状态: MATCH_STATUS */
    private java.lang.Integer _matchStatus;
    
    /* 匹配凭证行ID: MATCHED_LINE_ID */
    private java.lang.Long _matchedLineId;
    
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
    

    public _ErpFinBankStatementLine(){
        // for debug
    }

    protected ErpFinBankStatementLine newInstance(){
        ErpFinBankStatementLine entity = new ErpFinBankStatementLine();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinBankStatementLine cloneInstance() {
        ErpFinBankStatementLine entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinBankStatementLine";
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
        
            case PROP_ID_statementId:
               return getStatementId();
        
            case PROP_ID_lineNo:
               return getLineNo();
        
            case PROP_ID_transactionDate:
               return getTransactionDate();
        
            case PROP_ID_description:
               return getDescription();
        
            case PROP_ID_refNo:
               return getRefNo();
        
            case PROP_ID_dcDirection:
               return getDcDirection();
        
            case PROP_ID_amount:
               return getAmount();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_balanceAfter:
               return getBalanceAfter();
        
            case PROP_ID_matchStatus:
               return getMatchStatus();
        
            case PROP_ID_matchedLineId:
               return getMatchedLineId();
        
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
        
            case PROP_ID_statementId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_statementId));
               }
               setStatementId(typedValue);
               break;
            }
        
            case PROP_ID_lineNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_lineNo));
               }
               setLineNo(typedValue);
               break;
            }
        
            case PROP_ID_transactionDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_transactionDate));
               }
               setTransactionDate(typedValue);
               break;
            }
        
            case PROP_ID_description:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_description));
               }
               setDescription(typedValue);
               break;
            }
        
            case PROP_ID_refNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_refNo));
               }
               setRefNo(typedValue);
               break;
            }
        
            case PROP_ID_dcDirection:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_dcDirection));
               }
               setDcDirection(typedValue);
               break;
            }
        
            case PROP_ID_amount:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_amount));
               }
               setAmount(typedValue);
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
        
            case PROP_ID_balanceAfter:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_balanceAfter));
               }
               setBalanceAfter(typedValue);
               break;
            }
        
            case PROP_ID_matchStatus:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_matchStatus));
               }
               setMatchStatus(typedValue);
               break;
            }
        
            case PROP_ID_matchedLineId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_matchedLineId));
               }
               setMatchedLineId(typedValue);
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
        
            case PROP_ID_statementId:{
               onInitProp(propId);
               this._statementId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_lineNo:{
               onInitProp(propId);
               this._lineNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_transactionDate:{
               onInitProp(propId);
               this._transactionDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_refNo:{
               onInitProp(propId);
               this._refNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_dcDirection:{
               onInitProp(propId);
               this._dcDirection = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_amount:{
               onInitProp(propId);
               this._amount = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_currencyId:{
               onInitProp(propId);
               this._currencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_balanceAfter:{
               onInitProp(propId);
               this._balanceAfter = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_matchStatus:{
               onInitProp(propId);
               this._matchStatus = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_matchedLineId:{
               onInitProp(propId);
               this._matchedLineId = (java.lang.Long)value;
               
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
     * 对账单ID: STATEMENT_ID
     */
    public final java.lang.Long getStatementId(){
         onPropGet(PROP_ID_statementId);
         return _statementId;
    }

    /**
     * 对账单ID: STATEMENT_ID
     */
    public final void setStatementId(java.lang.Long value){
        if(onPropSet(PROP_ID_statementId,value)){
            this._statementId = value;
            internalClearRefs(PROP_ID_statementId);
            
        }
    }
    
    /**
     * 行号: LINE_NO
     */
    public final java.lang.Integer getLineNo(){
         onPropGet(PROP_ID_lineNo);
         return _lineNo;
    }

    /**
     * 行号: LINE_NO
     */
    public final void setLineNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_lineNo,value)){
            this._lineNo = value;
            internalClearRefs(PROP_ID_lineNo);
            
        }
    }
    
    /**
     * 交易日期: TRANSACTION_DATE
     */
    public final java.time.LocalDate getTransactionDate(){
         onPropGet(PROP_ID_transactionDate);
         return _transactionDate;
    }

    /**
     * 交易日期: TRANSACTION_DATE
     */
    public final void setTransactionDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_transactionDate,value)){
            this._transactionDate = value;
            internalClearRefs(PROP_ID_transactionDate);
            
        }
    }
    
    /**
     * 摘要(银行): DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 摘要(银行): DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
        }
    }
    
    /**
     * 银行参考号: REF_NO
     */
    public final java.lang.String getRefNo(){
         onPropGet(PROP_ID_refNo);
         return _refNo;
    }

    /**
     * 银行参考号: REF_NO
     */
    public final void setRefNo(java.lang.String value){
        if(onPropSet(PROP_ID_refNo,value)){
            this._refNo = value;
            internalClearRefs(PROP_ID_refNo);
            
        }
    }
    
    /**
     * 借贷方向: DC_DIRECTION
     */
    public final java.lang.Integer getDcDirection(){
         onPropGet(PROP_ID_dcDirection);
         return _dcDirection;
    }

    /**
     * 借贷方向: DC_DIRECTION
     */
    public final void setDcDirection(java.lang.Integer value){
        if(onPropSet(PROP_ID_dcDirection,value)){
            this._dcDirection = value;
            internalClearRefs(PROP_ID_dcDirection);
            
        }
    }
    
    /**
     * 金额: AMOUNT
     */
    public final java.lang.String getAmount(){
         onPropGet(PROP_ID_amount);
         return _amount;
    }

    /**
     * 金额: AMOUNT
     */
    public final void setAmount(java.lang.String value){
        if(onPropSet(PROP_ID_amount,value)){
            this._amount = value;
            internalClearRefs(PROP_ID_amount);
            
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
     * 交易后余额: BALANCE_AFTER
     */
    public final java.lang.String getBalanceAfter(){
         onPropGet(PROP_ID_balanceAfter);
         return _balanceAfter;
    }

    /**
     * 交易后余额: BALANCE_AFTER
     */
    public final void setBalanceAfter(java.lang.String value){
        if(onPropSet(PROP_ID_balanceAfter,value)){
            this._balanceAfter = value;
            internalClearRefs(PROP_ID_balanceAfter);
            
        }
    }
    
    /**
     * 匹配状态: MATCH_STATUS
     */
    public final java.lang.Integer getMatchStatus(){
         onPropGet(PROP_ID_matchStatus);
         return _matchStatus;
    }

    /**
     * 匹配状态: MATCH_STATUS
     */
    public final void setMatchStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_matchStatus,value)){
            this._matchStatus = value;
            internalClearRefs(PROP_ID_matchStatus);
            
        }
    }
    
    /**
     * 匹配凭证行ID: MATCHED_LINE_ID
     */
    public final java.lang.Long getMatchedLineId(){
         onPropGet(PROP_ID_matchedLineId);
         return _matchedLineId;
    }

    /**
     * 匹配凭证行ID: MATCHED_LINE_ID
     */
    public final void setMatchedLineId(java.lang.Long value){
        if(onPropSet(PROP_ID_matchedLineId,value)){
            this._matchedLineId = value;
            internalClearRefs(PROP_ID_matchedLineId);
            
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
    public final app.erp.fin.dao.entity.ErpFinBankStatement getStatement(){
       return (app.erp.fin.dao.entity.ErpFinBankStatement)internalGetRefEntity(PROP_NAME_statement);
    }

    public final void setStatement(app.erp.fin.dao.entity.ErpFinBankStatement refEntity){
   
           if(refEntity == null){
           
                   this.setStatementId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_statement, refEntity,()->{
           
                           this.setStatementId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinVoucherLine getMatchedLine(){
       return (app.erp.fin.dao.entity.ErpFinVoucherLine)internalGetRefEntity(PROP_NAME_matchedLine);
    }

    public final void setMatchedLine(app.erp.fin.dao.entity.ErpFinVoucherLine refEntity){
   
           if(refEntity == null){
           
                   this.setMatchedLineId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_matchedLine, refEntity,()->{
           
                           this.setMatchedLineId(refEntity.getId());
                       
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
