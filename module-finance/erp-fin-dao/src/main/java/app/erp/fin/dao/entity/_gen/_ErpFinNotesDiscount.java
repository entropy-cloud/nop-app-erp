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

import app.erp.fin.dao.entity.ErpFinNotesDiscount;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  票据贴现明细: erp_fin_notes_discount
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpFinNotesDiscount extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 应收票据: NOTES_RECEIVABLE_ID BIGINT */
    public static final String PROP_NAME_notesReceivableId = "notesReceivableId";
    public static final int PROP_ID_notesReceivableId = 2;
    
    /* 核算组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 贴现日: DISCOUNT_DATE DATE */
    public static final String PROP_NAME_discountDate = "discountDate";
    public static final int PROP_ID_discountDate = 4;
    
    /* 贴现银行账户: BANK_ID BIGINT */
    public static final String PROP_NAME_bankId = "bankId";
    public static final int PROP_ID_bankId = 5;
    
    /* 票面金额: FACE_AMOUNT DECIMAL */
    public static final String PROP_NAME_faceAmount = "faceAmount";
    public static final int PROP_ID_faceAmount = 6;
    
    /* 贴现息: DISCOUNT_INTEREST DECIMAL */
    public static final String PROP_NAME_discountInterest = "discountInterest";
    public static final int PROP_ID_discountInterest = 7;
    
    /* 实得金额: NET_AMOUNT DECIMAL */
    public static final String PROP_NAME_netAmount = "netAmount";
    public static final int PROP_ID_netAmount = 8;
    
    /* 币种: CURRENCY_ID BIGINT */
    public static final String PROP_NAME_currencyId = "currencyId";
    public static final int PROP_ID_currencyId = 9;
    
    /* 汇率: EXCHANGE_RATE DECIMAL */
    public static final String PROP_NAME_exchangeRate = "exchangeRate";
    public static final int PROP_ID_exchangeRate = 10;
    
    /* 汇兑损益: EXCHANGE_GAIN_LOSS DECIMAL */
    public static final String PROP_NAME_exchangeGainLoss = "exchangeGainLoss";
    public static final int PROP_ID_exchangeGainLoss = 11;
    
    /* 是否已过账: POSTED BOOLEAN */
    public static final String PROP_NAME_posted = "posted";
    public static final int PROP_ID_posted = 12;
    
    /* 过账人: POSTED_BY VARCHAR */
    public static final String PROP_NAME_postedBy = "postedBy";
    public static final int PROP_ID_postedBy = 13;
    
    /* 过账时间: POSTED_AT TIMESTAMP */
    public static final String PROP_NAME_postedAt = "postedAt";
    public static final int PROP_ID_postedAt = 14;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 15;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 16;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 17;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 18;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 19;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 20;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 21;
    

    private static int _PROP_ID_BOUND = 22;

    
    /* relation:  */
    public static final String PROP_NAME_notesReceivable = "notesReceivable";
    
    /* relation:  */
    public static final String PROP_NAME_bank = "bank";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_notesReceivableId] = PROP_NAME_notesReceivableId;
          PROP_NAME_TO_ID.put(PROP_NAME_notesReceivableId, PROP_ID_notesReceivableId);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_discountDate] = PROP_NAME_discountDate;
          PROP_NAME_TO_ID.put(PROP_NAME_discountDate, PROP_ID_discountDate);
      
          PROP_ID_TO_NAME[PROP_ID_bankId] = PROP_NAME_bankId;
          PROP_NAME_TO_ID.put(PROP_NAME_bankId, PROP_ID_bankId);
      
          PROP_ID_TO_NAME[PROP_ID_faceAmount] = PROP_NAME_faceAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_faceAmount, PROP_ID_faceAmount);
      
          PROP_ID_TO_NAME[PROP_ID_discountInterest] = PROP_NAME_discountInterest;
          PROP_NAME_TO_ID.put(PROP_NAME_discountInterest, PROP_ID_discountInterest);
      
          PROP_ID_TO_NAME[PROP_ID_netAmount] = PROP_NAME_netAmount;
          PROP_NAME_TO_ID.put(PROP_NAME_netAmount, PROP_ID_netAmount);
      
          PROP_ID_TO_NAME[PROP_ID_currencyId] = PROP_NAME_currencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_currencyId, PROP_ID_currencyId);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeRate] = PROP_NAME_exchangeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeRate, PROP_ID_exchangeRate);
      
          PROP_ID_TO_NAME[PROP_ID_exchangeGainLoss] = PROP_NAME_exchangeGainLoss;
          PROP_NAME_TO_ID.put(PROP_NAME_exchangeGainLoss, PROP_ID_exchangeGainLoss);
      
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
    
    /* 应收票据: NOTES_RECEIVABLE_ID */
    private java.lang.Long _notesReceivableId;
    
    /* 核算组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 贴现日: DISCOUNT_DATE */
    private java.time.LocalDate _discountDate;
    
    /* 贴现银行账户: BANK_ID */
    private java.lang.Long _bankId;
    
    /* 票面金额: FACE_AMOUNT */
    private java.math.BigDecimal _faceAmount;
    
    /* 贴现息: DISCOUNT_INTEREST */
    private java.math.BigDecimal _discountInterest;
    
    /* 实得金额: NET_AMOUNT */
    private java.math.BigDecimal _netAmount;
    
    /* 币种: CURRENCY_ID */
    private java.lang.Long _currencyId;
    
    /* 汇率: EXCHANGE_RATE */
    private java.math.BigDecimal _exchangeRate;
    
    /* 汇兑损益: EXCHANGE_GAIN_LOSS */
    private java.math.BigDecimal _exchangeGainLoss;
    
    /* 是否已过账: POSTED */
    private java.lang.Boolean _posted;
    
    /* 过账人: POSTED_BY */
    private java.lang.String _postedBy;
    
    /* 过账时间: POSTED_AT */
    private java.sql.Timestamp _postedAt;
    
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
    

    public _ErpFinNotesDiscount(){
        // for debug
    }

    protected ErpFinNotesDiscount newInstance(){
        ErpFinNotesDiscount entity = new ErpFinNotesDiscount();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpFinNotesDiscount cloneInstance() {
        ErpFinNotesDiscount entity = newInstance();
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
      return "app.erp.fin.dao.entity.ErpFinNotesDiscount";
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
        
            case PROP_ID_notesReceivableId:
               return getNotesReceivableId();
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_discountDate:
               return getDiscountDate();
        
            case PROP_ID_bankId:
               return getBankId();
        
            case PROP_ID_faceAmount:
               return getFaceAmount();
        
            case PROP_ID_discountInterest:
               return getDiscountInterest();
        
            case PROP_ID_netAmount:
               return getNetAmount();
        
            case PROP_ID_currencyId:
               return getCurrencyId();
        
            case PROP_ID_exchangeRate:
               return getExchangeRate();
        
            case PROP_ID_exchangeGainLoss:
               return getExchangeGainLoss();
        
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
        
            case PROP_ID_notesReceivableId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_notesReceivableId));
               }
               setNotesReceivableId(typedValue);
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
        
            case PROP_ID_discountDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_discountDate));
               }
               setDiscountDate(typedValue);
               break;
            }
        
            case PROP_ID_bankId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_bankId));
               }
               setBankId(typedValue);
               break;
            }
        
            case PROP_ID_faceAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_faceAmount));
               }
               setFaceAmount(typedValue);
               break;
            }
        
            case PROP_ID_discountInterest:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_discountInterest));
               }
               setDiscountInterest(typedValue);
               break;
            }
        
            case PROP_ID_netAmount:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_netAmount));
               }
               setNetAmount(typedValue);
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
        
            case PROP_ID_exchangeGainLoss:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_exchangeGainLoss));
               }
               setExchangeGainLoss(typedValue);
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
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
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
        
            case PROP_ID_notesReceivableId:{
               onInitProp(propId);
               this._notesReceivableId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_discountDate:{
               onInitProp(propId);
               this._discountDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_bankId:{
               onInitProp(propId);
               this._bankId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_faceAmount:{
               onInitProp(propId);
               this._faceAmount = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_discountInterest:{
               onInitProp(propId);
               this._discountInterest = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_netAmount:{
               onInitProp(propId);
               this._netAmount = (java.math.BigDecimal)value;
               
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
        
            case PROP_ID_exchangeGainLoss:{
               onInitProp(propId);
               this._exchangeGainLoss = (java.math.BigDecimal)value;
               
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
               this._postedAt = (java.sql.Timestamp)value;
               
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
     * 应收票据: NOTES_RECEIVABLE_ID
     */
    public final java.lang.Long getNotesReceivableId(){
         onPropGet(PROP_ID_notesReceivableId);
         return _notesReceivableId;
    }

    /**
     * 应收票据: NOTES_RECEIVABLE_ID
     */
    public final void setNotesReceivableId(java.lang.Long value){
        if(onPropSet(PROP_ID_notesReceivableId,value)){
            this._notesReceivableId = value;
            internalClearRefs(PROP_ID_notesReceivableId);
            
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
     * 贴现日: DISCOUNT_DATE
     */
    public final java.time.LocalDate getDiscountDate(){
         onPropGet(PROP_ID_discountDate);
         return _discountDate;
    }

    /**
     * 贴现日: DISCOUNT_DATE
     */
    public final void setDiscountDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_discountDate,value)){
            this._discountDate = value;
            internalClearRefs(PROP_ID_discountDate);
            
        }
    }
    
    /**
     * 贴现银行账户: BANK_ID
     */
    public final java.lang.Long getBankId(){
         onPropGet(PROP_ID_bankId);
         return _bankId;
    }

    /**
     * 贴现银行账户: BANK_ID
     */
    public final void setBankId(java.lang.Long value){
        if(onPropSet(PROP_ID_bankId,value)){
            this._bankId = value;
            internalClearRefs(PROP_ID_bankId);
            
        }
    }
    
    /**
     * 票面金额: FACE_AMOUNT
     */
    public final java.math.BigDecimal getFaceAmount(){
         onPropGet(PROP_ID_faceAmount);
         return _faceAmount;
    }

    /**
     * 票面金额: FACE_AMOUNT
     */
    public final void setFaceAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_faceAmount,value)){
            this._faceAmount = value;
            internalClearRefs(PROP_ID_faceAmount);
            
        }
    }
    
    /**
     * 贴现息: DISCOUNT_INTEREST
     */
    public final java.math.BigDecimal getDiscountInterest(){
         onPropGet(PROP_ID_discountInterest);
         return _discountInterest;
    }

    /**
     * 贴现息: DISCOUNT_INTEREST
     */
    public final void setDiscountInterest(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_discountInterest,value)){
            this._discountInterest = value;
            internalClearRefs(PROP_ID_discountInterest);
            
        }
    }
    
    /**
     * 实得金额: NET_AMOUNT
     */
    public final java.math.BigDecimal getNetAmount(){
         onPropGet(PROP_ID_netAmount);
         return _netAmount;
    }

    /**
     * 实得金额: NET_AMOUNT
     */
    public final void setNetAmount(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_netAmount,value)){
            this._netAmount = value;
            internalClearRefs(PROP_ID_netAmount);
            
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
     * 汇兑损益: EXCHANGE_GAIN_LOSS
     */
    public final java.math.BigDecimal getExchangeGainLoss(){
         onPropGet(PROP_ID_exchangeGainLoss);
         return _exchangeGainLoss;
    }

    /**
     * 汇兑损益: EXCHANGE_GAIN_LOSS
     */
    public final void setExchangeGainLoss(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_exchangeGainLoss,value)){
            this._exchangeGainLoss = value;
            internalClearRefs(PROP_ID_exchangeGainLoss);
            
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
    public final app.erp.fin.dao.entity.ErpFinNotesReceivable getNotesReceivable(){
       return (app.erp.fin.dao.entity.ErpFinNotesReceivable)internalGetRefEntity(PROP_NAME_notesReceivable);
    }

    public final void setNotesReceivable(app.erp.fin.dao.entity.ErpFinNotesReceivable refEntity){
   
           if(refEntity == null){
           
                   this.setNotesReceivableId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_notesReceivable, refEntity,()->{
           
                           this.setNotesReceivableId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.fin.dao.entity.ErpFinFundAccount getBank(){
       return (app.erp.fin.dao.entity.ErpFinFundAccount)internalGetRefEntity(PROP_NAME_bank);
    }

    public final void setBank(app.erp.fin.dao.entity.ErpFinFundAccount refEntity){
   
           if(refEntity == null){
           
                   this.setBankId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_bank, refEntity,()->{
           
                           this.setBankId(refEntity.getId());
                       
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
