package app.erp.hr.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.hr.dao.entity.ErpHrRoleCompetency;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  岗位胜任力要求: erp_hr_role_competency
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrRoleCompetency extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 岗位: POSITION_ID BIGINT */
    public static final String PROP_NAME_positionId = "positionId";
    public static final int PROP_ID_positionId = 2;
    
    /* 胜任力: COMPETENCY_ID BIGINT */
    public static final String PROP_NAME_competencyId = "competencyId";
    public static final int PROP_ID_competencyId = 3;
    
    /* 要求等级: REQUIRED_LEVEL INTEGER */
    public static final String PROP_NAME_requiredLevel = "requiredLevel";
    public static final int PROP_ID_requiredLevel = 4;
    
    /* 权重: WEIGHT DECIMAL */
    public static final String PROP_NAME_weight = "weight";
    public static final int PROP_ID_weight = 5;
    
    /* 是否关键: IS_CRITICAL BOOLEAN */
    public static final String PROP_NAME_isCritical = "isCritical";
    public static final int PROP_ID_isCritical = 6;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 7;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 8;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 9;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 10;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 11;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 12;
    

    private static int _PROP_ID_BOUND = 13;

    
    /* relation:  */
    public static final String PROP_NAME_position = "position";
    
    /* relation:  */
    public static final String PROP_NAME_competency = "competency";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[13];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_positionId] = PROP_NAME_positionId;
          PROP_NAME_TO_ID.put(PROP_NAME_positionId, PROP_ID_positionId);
      
          PROP_ID_TO_NAME[PROP_ID_competencyId] = PROP_NAME_competencyId;
          PROP_NAME_TO_ID.put(PROP_NAME_competencyId, PROP_ID_competencyId);
      
          PROP_ID_TO_NAME[PROP_ID_requiredLevel] = PROP_NAME_requiredLevel;
          PROP_NAME_TO_ID.put(PROP_NAME_requiredLevel, PROP_ID_requiredLevel);
      
          PROP_ID_TO_NAME[PROP_ID_weight] = PROP_NAME_weight;
          PROP_NAME_TO_ID.put(PROP_NAME_weight, PROP_ID_weight);
      
          PROP_ID_TO_NAME[PROP_ID_isCritical] = PROP_NAME_isCritical;
          PROP_NAME_TO_ID.put(PROP_NAME_isCritical, PROP_ID_isCritical);
      
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
    
    /* 岗位: POSITION_ID */
    private java.lang.Long _positionId;
    
    /* 胜任力: COMPETENCY_ID */
    private java.lang.Long _competencyId;
    
    /* 要求等级: REQUIRED_LEVEL */
    private java.lang.Integer _requiredLevel;
    
    /* 权重: WEIGHT */
    private java.math.BigDecimal _weight;
    
    /* 是否关键: IS_CRITICAL */
    private java.lang.Boolean _isCritical;
    
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
    

    public _ErpHrRoleCompetency(){
        // for debug
    }

    protected ErpHrRoleCompetency newInstance(){
        ErpHrRoleCompetency entity = new ErpHrRoleCompetency();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrRoleCompetency cloneInstance() {
        ErpHrRoleCompetency entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrRoleCompetency";
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
        
            case PROP_ID_positionId:
               return getPositionId();
        
            case PROP_ID_competencyId:
               return getCompetencyId();
        
            case PROP_ID_requiredLevel:
               return getRequiredLevel();
        
            case PROP_ID_weight:
               return getWeight();
        
            case PROP_ID_isCritical:
               return getIsCritical();
        
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
        
            case PROP_ID_positionId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_positionId));
               }
               setPositionId(typedValue);
               break;
            }
        
            case PROP_ID_competencyId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_competencyId));
               }
               setCompetencyId(typedValue);
               break;
            }
        
            case PROP_ID_requiredLevel:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_requiredLevel));
               }
               setRequiredLevel(typedValue);
               break;
            }
        
            case PROP_ID_weight:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_weight));
               }
               setWeight(typedValue);
               break;
            }
        
            case PROP_ID_isCritical:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_isCritical));
               }
               setIsCritical(typedValue);
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
        
            case PROP_ID_positionId:{
               onInitProp(propId);
               this._positionId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_competencyId:{
               onInitProp(propId);
               this._competencyId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_requiredLevel:{
               onInitProp(propId);
               this._requiredLevel = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_weight:{
               onInitProp(propId);
               this._weight = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_isCritical:{
               onInitProp(propId);
               this._isCritical = (java.lang.Boolean)value;
               
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
     * 岗位: POSITION_ID
     */
    public final java.lang.Long getPositionId(){
         onPropGet(PROP_ID_positionId);
         return _positionId;
    }

    /**
     * 岗位: POSITION_ID
     */
    public final void setPositionId(java.lang.Long value){
        if(onPropSet(PROP_ID_positionId,value)){
            this._positionId = value;
            internalClearRefs(PROP_ID_positionId);
            
        }
    }
    
    /**
     * 胜任力: COMPETENCY_ID
     */
    public final java.lang.Long getCompetencyId(){
         onPropGet(PROP_ID_competencyId);
         return _competencyId;
    }

    /**
     * 胜任力: COMPETENCY_ID
     */
    public final void setCompetencyId(java.lang.Long value){
        if(onPropSet(PROP_ID_competencyId,value)){
            this._competencyId = value;
            internalClearRefs(PROP_ID_competencyId);
            
        }
    }
    
    /**
     * 要求等级: REQUIRED_LEVEL
     */
    public final java.lang.Integer getRequiredLevel(){
         onPropGet(PROP_ID_requiredLevel);
         return _requiredLevel;
    }

    /**
     * 要求等级: REQUIRED_LEVEL
     */
    public final void setRequiredLevel(java.lang.Integer value){
        if(onPropSet(PROP_ID_requiredLevel,value)){
            this._requiredLevel = value;
            internalClearRefs(PROP_ID_requiredLevel);
            
        }
    }
    
    /**
     * 权重: WEIGHT
     */
    public final java.math.BigDecimal getWeight(){
         onPropGet(PROP_ID_weight);
         return _weight;
    }

    /**
     * 权重: WEIGHT
     */
    public final void setWeight(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_weight,value)){
            this._weight = value;
            internalClearRefs(PROP_ID_weight);
            
        }
    }
    
    /**
     * 是否关键: IS_CRITICAL
     */
    public final java.lang.Boolean getIsCritical(){
         onPropGet(PROP_ID_isCritical);
         return _isCritical;
    }

    /**
     * 是否关键: IS_CRITICAL
     */
    public final void setIsCritical(java.lang.Boolean value){
        if(onPropSet(PROP_ID_isCritical,value)){
            this._isCritical = value;
            internalClearRefs(PROP_ID_isCritical);
            
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
    public final app.erp.hr.dao.entity.ErpHrPosition getPosition(){
       return (app.erp.hr.dao.entity.ErpHrPosition)internalGetRefEntity(PROP_NAME_position);
    }

    public final void setPosition(app.erp.hr.dao.entity.ErpHrPosition refEntity){
   
           if(refEntity == null){
           
                   this.setPositionId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_position, refEntity,()->{
           
                           this.setPositionId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.hr.dao.entity.ErpHrCompetency getCompetency(){
       return (app.erp.hr.dao.entity.ErpHrCompetency)internalGetRefEntity(PROP_NAME_competency);
    }

    public final void setCompetency(app.erp.hr.dao.entity.ErpHrCompetency refEntity){
   
           if(refEntity == null){
           
                   this.setCompetencyId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_competency, refEntity,()->{
           
                           this.setCompetencyId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
