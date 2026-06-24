package app.erp.qa.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.qa.dao.entity.ErpQaSamplingPlan;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  抽样方案: erp_qa_sampling_plan
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpQaSamplingPlan extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* AQL 检验水平(如 II): AQL_LEVEL VARCHAR */
    public static final String PROP_NAME_aqlLevel = "aqlLevel";
    public static final int PROP_ID_aqlLevel = 4;
    
    /* 批量下限: LOT_SIZE_FROM DECIMAL */
    public static final String PROP_NAME_lotSizeFrom = "lotSizeFrom";
    public static final int PROP_ID_lotSizeFrom = 5;
    
    /* 批量上限: LOT_SIZE_TO DECIMAL */
    public static final String PROP_NAME_lotSizeTo = "lotSizeTo";
    public static final int PROP_ID_lotSizeTo = 6;
    
    /* 样本量: SAMPLE_SIZE DECIMAL */
    public static final String PROP_NAME_sampleSize = "sampleSize";
    public static final int PROP_ID_sampleSize = 7;
    
    /* 合格判定数(Ac): ACCEPT_NUMBER INTEGER */
    public static final String PROP_NAME_acceptNumber = "acceptNumber";
    public static final int PROP_ID_acceptNumber = 8;
    
    /* 不合格判定数(Re): REJECT_NUMBER INTEGER */
    public static final String PROP_NAME_rejectNumber = "rejectNumber";
    public static final int PROP_ID_rejectNumber = 9;
    
    /* 是否启用: IS_ACTIVE BOOLEAN */
    public static final String PROP_NAME_isActive = "isActive";
    public static final int PROP_ID_isActive = 10;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 11;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 12;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 13;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 14;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 15;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 16;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 17;
    

    private static int _PROP_ID_BOUND = 18;

    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[18];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_aqlLevel] = PROP_NAME_aqlLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_aqlLevel, PROP_ID_aqlLevel);
      
          PROP_ID_TO_NAME[PROP_ID_lotSizeFrom] = PROP_NAME_lotSizeFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_lotSizeFrom, PROP_ID_lotSizeFrom);
      
          PROP_ID_TO_NAME[PROP_ID_lotSizeTo] = PROP_NAME_lotSizeTo;
          PROP_NAME_TO_ID.put(PROP_NAME_lotSizeTo, PROP_ID_lotSizeTo);
      
          PROP_ID_TO_NAME[PROP_ID_sampleSize] = PROP_NAME_sampleSize;
          PROP_NAME_TO_ID.put(PROP_NAME_sampleSize, PROP_ID_sampleSize);
      
          PROP_ID_TO_NAME[PROP_ID_acceptNumber] = PROP_NAME_acceptNumber;
          PROP_NAME_TO_ID.put(PROP_NAME_acceptNumber, PROP_ID_acceptNumber);
      
          PROP_ID_TO_NAME[PROP_ID_rejectNumber] = PROP_NAME_rejectNumber;
          PROP_NAME_TO_ID.put(PROP_NAME_rejectNumber, PROP_ID_rejectNumber);
      
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
    
    /* 编码: CODE */
    private java.lang.String _code;
    
    /* 名称: NAME */
    private java.lang.String _name;
    
    /* AQL 检验水平(如 II): AQL_LEVEL */
    private java.lang.String _aqlLevel;
    
    /* 批量下限: LOT_SIZE_FROM */
    private java.lang.String _lotSizeFrom;
    
    /* 批量上限: LOT_SIZE_TO */
    private java.lang.String _lotSizeTo;
    
    /* 样本量: SAMPLE_SIZE */
    private java.lang.String _sampleSize;
    
    /* 合格判定数(Ac): ACCEPT_NUMBER */
    private java.lang.Integer _acceptNumber;
    
    /* 不合格判定数(Re): REJECT_NUMBER */
    private java.lang.Integer _rejectNumber;
    
    /* 是否启用: IS_ACTIVE */
    private java.lang.Boolean _isActive;
    
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
    

    public _ErpQaSamplingPlan(){
        // for debug
    }

    protected ErpQaSamplingPlan newInstance(){
        ErpQaSamplingPlan entity = new ErpQaSamplingPlan();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpQaSamplingPlan cloneInstance() {
        ErpQaSamplingPlan entity = newInstance();
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
      return "app.erp.qa.dao.entity.ErpQaSamplingPlan";
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
        
            case PROP_ID_aqlLevel:
               return getAqlLevel();
        
            case PROP_ID_lotSizeFrom:
               return getLotSizeFrom();
        
            case PROP_ID_lotSizeTo:
               return getLotSizeTo();
        
            case PROP_ID_sampleSize:
               return getSampleSize();
        
            case PROP_ID_acceptNumber:
               return getAcceptNumber();
        
            case PROP_ID_rejectNumber:
               return getRejectNumber();
        
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
        
            case PROP_ID_aqlLevel:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_aqlLevel));
               }
               setAqlLevel(typedValue);
               break;
            }
        
            case PROP_ID_lotSizeFrom:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lotSizeFrom));
               }
               setLotSizeFrom(typedValue);
               break;
            }
        
            case PROP_ID_lotSizeTo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_lotSizeTo));
               }
               setLotSizeTo(typedValue);
               break;
            }
        
            case PROP_ID_sampleSize:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_sampleSize));
               }
               setSampleSize(typedValue);
               break;
            }
        
            case PROP_ID_acceptNumber:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_acceptNumber));
               }
               setAcceptNumber(typedValue);
               break;
            }
        
            case PROP_ID_rejectNumber:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_rejectNumber));
               }
               setRejectNumber(typedValue);
               break;
            }
        
            case PROP_ID_isActive:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
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
        
            case PROP_ID_aqlLevel:{
               onInitProp(propId);
               this._aqlLevel = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lotSizeFrom:{
               onInitProp(propId);
               this._lotSizeFrom = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_lotSizeTo:{
               onInitProp(propId);
               this._lotSizeTo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_sampleSize:{
               onInitProp(propId);
               this._sampleSize = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_acceptNumber:{
               onInitProp(propId);
               this._acceptNumber = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_rejectNumber:{
               onInitProp(propId);
               this._rejectNumber = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_isActive:{
               onInitProp(propId);
               this._isActive = (java.lang.Boolean)value;
               
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
     * 编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * AQL 检验水平(如 II): AQL_LEVEL
     */
    public final java.lang.String getAqlLevel(){
         onPropGet(PROP_ID_aqlLevel);
         return _aqlLevel;
    }

    /**
     * AQL 检验水平(如 II): AQL_LEVEL
     */
    public final void setAqlLevel(java.lang.String value){
        if(onPropSet(PROP_ID_aqlLevel,value)){
            this._aqlLevel = value;
            internalClearRefs(PROP_ID_aqlLevel);
            
        }
    }
    
    /**
     * 批量下限: LOT_SIZE_FROM
     */
    public final java.lang.String getLotSizeFrom(){
         onPropGet(PROP_ID_lotSizeFrom);
         return _lotSizeFrom;
    }

    /**
     * 批量下限: LOT_SIZE_FROM
     */
    public final void setLotSizeFrom(java.lang.String value){
        if(onPropSet(PROP_ID_lotSizeFrom,value)){
            this._lotSizeFrom = value;
            internalClearRefs(PROP_ID_lotSizeFrom);
            
        }
    }
    
    /**
     * 批量上限: LOT_SIZE_TO
     */
    public final java.lang.String getLotSizeTo(){
         onPropGet(PROP_ID_lotSizeTo);
         return _lotSizeTo;
    }

    /**
     * 批量上限: LOT_SIZE_TO
     */
    public final void setLotSizeTo(java.lang.String value){
        if(onPropSet(PROP_ID_lotSizeTo,value)){
            this._lotSizeTo = value;
            internalClearRefs(PROP_ID_lotSizeTo);
            
        }
    }
    
    /**
     * 样本量: SAMPLE_SIZE
     */
    public final java.lang.String getSampleSize(){
         onPropGet(PROP_ID_sampleSize);
         return _sampleSize;
    }

    /**
     * 样本量: SAMPLE_SIZE
     */
    public final void setSampleSize(java.lang.String value){
        if(onPropSet(PROP_ID_sampleSize,value)){
            this._sampleSize = value;
            internalClearRefs(PROP_ID_sampleSize);
            
        }
    }
    
    /**
     * 合格判定数(Ac): ACCEPT_NUMBER
     */
    public final java.lang.Integer getAcceptNumber(){
         onPropGet(PROP_ID_acceptNumber);
         return _acceptNumber;
    }

    /**
     * 合格判定数(Ac): ACCEPT_NUMBER
     */
    public final void setAcceptNumber(java.lang.Integer value){
        if(onPropSet(PROP_ID_acceptNumber,value)){
            this._acceptNumber = value;
            internalClearRefs(PROP_ID_acceptNumber);
            
        }
    }
    
    /**
     * 不合格判定数(Re): REJECT_NUMBER
     */
    public final java.lang.Integer getRejectNumber(){
         onPropGet(PROP_ID_rejectNumber);
         return _rejectNumber;
    }

    /**
     * 不合格判定数(Re): REJECT_NUMBER
     */
    public final void setRejectNumber(java.lang.Integer value){
        if(onPropSet(PROP_ID_rejectNumber,value)){
            this._rejectNumber = value;
            internalClearRefs(PROP_ID_rejectNumber);
            
        }
    }
    
    /**
     * 是否启用: IS_ACTIVE
     */
    public final java.lang.Boolean getIsActive(){
         onPropGet(PROP_ID_isActive);
         return _isActive;
    }

    /**
     * 是否启用: IS_ACTIVE
     */
    public final void setIsActive(java.lang.Boolean value){
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
    
}
// resume CPD analysis - CPD-ON
