package app.erp.drp.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.drp.dao.entity.ErpDrpScenarioVersion;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  DRP仿真版本: erp_drp_scenario_version
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpDrpScenarioVersion extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 仿真场景: SCENARIO_ID BIGINT */
    public static final String PROP_NAME_scenarioId = "scenarioId";
    public static final int PROP_ID_scenarioId = 2;
    
    /* 版本号: VERSION_NO INTEGER */
    public static final String PROP_NAME_versionNo = "versionNo";
    public static final int PROP_ID_versionNo = 3;
    
    /* 计算结果DRP计划: COMPUTED_DRP_PLAN_ID BIGINT */
    public static final String PROP_NAME_computedDrpPlanId = "computedDrpPlanId";
    public static final int PROP_ID_computedDrpPlanId = 4;
    
    /* 快照摘要: SNAPSHOT_SUMMARY VARCHAR */
    public static final String PROP_NAME_snapshotSummary = "snapshotSummary";
    public static final int PROP_ID_snapshotSummary = 5;
    
    /* 状态: STATUS VARCHAR */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 6;
    
    /* 转正式计划ID: PROMOTED_PLAN_ID BIGINT */
    public static final String PROP_NAME_promotedPlanId = "promotedPlanId";
    public static final int PROP_ID_promotedPlanId = 7;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 8;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 9;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 10;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 11;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 12;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 13;
    

    private static int _PROP_ID_BOUND = 14;

    
    /* relation:  */
    public static final String PROP_NAME_scenario = "scenario";
    
    /* relation:  */
    public static final String PROP_NAME_computedDrpPlan = "computedDrpPlan";
    
    /* relation:  */
    public static final String PROP_NAME_promotedPlan = "promotedPlan";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[14];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_scenarioId] = PROP_NAME_scenarioId;
          PROP_NAME_TO_ID.put(PROP_NAME_scenarioId, PROP_ID_scenarioId);
      
          PROP_ID_TO_NAME[PROP_ID_versionNo] = PROP_NAME_versionNo;
          PROP_NAME_TO_ID.put(PROP_NAME_versionNo, PROP_ID_versionNo);
      
          PROP_ID_TO_NAME[PROP_ID_computedDrpPlanId] = PROP_NAME_computedDrpPlanId;
          PROP_NAME_TO_ID.put(PROP_NAME_computedDrpPlanId, PROP_ID_computedDrpPlanId);
      
          PROP_ID_TO_NAME[PROP_ID_snapshotSummary] = PROP_NAME_snapshotSummary;
          PROP_NAME_TO_ID.put(PROP_NAME_snapshotSummary, PROP_ID_snapshotSummary);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_promotedPlanId] = PROP_NAME_promotedPlanId;
          PROP_NAME_TO_ID.put(PROP_NAME_promotedPlanId, PROP_ID_promotedPlanId);
      
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
    
    /* 仿真场景: SCENARIO_ID */
    private java.lang.Long _scenarioId;
    
    /* 版本号: VERSION_NO */
    private java.lang.Integer _versionNo;
    
    /* 计算结果DRP计划: COMPUTED_DRP_PLAN_ID */
    private java.lang.Long _computedDrpPlanId;
    
    /* 快照摘要: SNAPSHOT_SUMMARY */
    private java.lang.String _snapshotSummary;
    
    /* 状态: STATUS */
    private java.lang.String _status;
    
    /* 转正式计划ID: PROMOTED_PLAN_ID */
    private java.lang.Long _promotedPlanId;
    
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
    

    public _ErpDrpScenarioVersion(){
        // for debug
    }

    protected ErpDrpScenarioVersion newInstance(){
        ErpDrpScenarioVersion entity = new ErpDrpScenarioVersion();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpDrpScenarioVersion cloneInstance() {
        ErpDrpScenarioVersion entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpDrpScenarioVersion";
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
        
            case PROP_ID_scenarioId:
               return getScenarioId();
        
            case PROP_ID_versionNo:
               return getVersionNo();
        
            case PROP_ID_computedDrpPlanId:
               return getComputedDrpPlanId();
        
            case PROP_ID_snapshotSummary:
               return getSnapshotSummary();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_promotedPlanId:
               return getPromotedPlanId();
        
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
        
            case PROP_ID_scenarioId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_scenarioId));
               }
               setScenarioId(typedValue);
               break;
            }
        
            case PROP_ID_versionNo:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_versionNo));
               }
               setVersionNo(typedValue);
               break;
            }
        
            case PROP_ID_computedDrpPlanId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_computedDrpPlanId));
               }
               setComputedDrpPlanId(typedValue);
               break;
            }
        
            case PROP_ID_snapshotSummary:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_snapshotSummary));
               }
               setSnapshotSummary(typedValue);
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
        
            case PROP_ID_promotedPlanId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_promotedPlanId));
               }
               setPromotedPlanId(typedValue);
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
        
            case PROP_ID_scenarioId:{
               onInitProp(propId);
               this._scenarioId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_versionNo:{
               onInitProp(propId);
               this._versionNo = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_computedDrpPlanId:{
               onInitProp(propId);
               this._computedDrpPlanId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_snapshotSummary:{
               onInitProp(propId);
               this._snapshotSummary = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_promotedPlanId:{
               onInitProp(propId);
               this._promotedPlanId = (java.lang.Long)value;
               
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
     * 仿真场景: SCENARIO_ID
     */
    public final java.lang.Long getScenarioId(){
         onPropGet(PROP_ID_scenarioId);
         return _scenarioId;
    }

    /**
     * 仿真场景: SCENARIO_ID
     */
    public final void setScenarioId(java.lang.Long value){
        if(onPropSet(PROP_ID_scenarioId,value)){
            this._scenarioId = value;
            internalClearRefs(PROP_ID_scenarioId);
            
        }
    }
    
    /**
     * 版本号: VERSION_NO
     */
    public final java.lang.Integer getVersionNo(){
         onPropGet(PROP_ID_versionNo);
         return _versionNo;
    }

    /**
     * 版本号: VERSION_NO
     */
    public final void setVersionNo(java.lang.Integer value){
        if(onPropSet(PROP_ID_versionNo,value)){
            this._versionNo = value;
            internalClearRefs(PROP_ID_versionNo);
            
        }
    }
    
    /**
     * 计算结果DRP计划: COMPUTED_DRP_PLAN_ID
     */
    public final java.lang.Long getComputedDrpPlanId(){
         onPropGet(PROP_ID_computedDrpPlanId);
         return _computedDrpPlanId;
    }

    /**
     * 计算结果DRP计划: COMPUTED_DRP_PLAN_ID
     */
    public final void setComputedDrpPlanId(java.lang.Long value){
        if(onPropSet(PROP_ID_computedDrpPlanId,value)){
            this._computedDrpPlanId = value;
            internalClearRefs(PROP_ID_computedDrpPlanId);
            
        }
    }
    
    /**
     * 快照摘要: SNAPSHOT_SUMMARY
     */
    public final java.lang.String getSnapshotSummary(){
         onPropGet(PROP_ID_snapshotSummary);
         return _snapshotSummary;
    }

    /**
     * 快照摘要: SNAPSHOT_SUMMARY
     */
    public final void setSnapshotSummary(java.lang.String value){
        if(onPropSet(PROP_ID_snapshotSummary,value)){
            this._snapshotSummary = value;
            internalClearRefs(PROP_ID_snapshotSummary);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.String getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.String value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 转正式计划ID: PROMOTED_PLAN_ID
     */
    public final java.lang.Long getPromotedPlanId(){
         onPropGet(PROP_ID_promotedPlanId);
         return _promotedPlanId;
    }

    /**
     * 转正式计划ID: PROMOTED_PLAN_ID
     */
    public final void setPromotedPlanId(java.lang.Long value){
        if(onPropSet(PROP_ID_promotedPlanId,value)){
            this._promotedPlanId = value;
            internalClearRefs(PROP_ID_promotedPlanId);
            
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
    public final app.erp.drp.dao.entity.ErpDrpScenario getScenario(){
       return (app.erp.drp.dao.entity.ErpDrpScenario)internalGetRefEntity(PROP_NAME_scenario);
    }

    public final void setScenario(app.erp.drp.dao.entity.ErpDrpScenario refEntity){
   
           if(refEntity == null){
           
                   this.setScenarioId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_scenario, refEntity,()->{
           
                           this.setScenarioId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.drp.dao.entity.ErpDrpPlan getComputedDrpPlan(){
       return (app.erp.drp.dao.entity.ErpDrpPlan)internalGetRefEntity(PROP_NAME_computedDrpPlan);
    }

    public final void setComputedDrpPlan(app.erp.drp.dao.entity.ErpDrpPlan refEntity){
   
           if(refEntity == null){
           
                   this.setComputedDrpPlanId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_computedDrpPlan, refEntity,()->{
           
                           this.setComputedDrpPlanId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.drp.dao.entity.ErpDrpPlan getPromotedPlan(){
       return (app.erp.drp.dao.entity.ErpDrpPlan)internalGetRefEntity(PROP_NAME_promotedPlan);
    }

    public final void setPromotedPlan(app.erp.drp.dao.entity.ErpDrpPlan refEntity){
   
           if(refEntity == null){
           
                   this.setPromotedPlanId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_promotedPlan, refEntity,()->{
           
                           this.setPromotedPlanId(refEntity.getId());
                       
           });
           }
       
    }
       
}
// resume CPD analysis - CPD-ON
