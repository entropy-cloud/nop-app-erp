package app.erp.aps.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code

import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.aps.dao.entity.ErpApsDispatchRule;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  自动派工规则: erp_aps_dispatch_rule
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpApsDispatchRule extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 工作中心: WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 3;
    
    /* 规则名称: RULE_NAME VARCHAR */
    public static final String PROP_NAME_ruleName = "ruleName";
    public static final int PROP_ID_ruleName = 4;
    
    /* 自动派工启用: ENABLE_AUTO BOOLEAN */
    public static final String PROP_NAME_enableAuto = "enableAuto";
    public static final int PROP_ID_enableAuto = 5;
    
    /* 物料齐套检查: REQUIRE_MATERIAL BOOLEAN */
    public static final String PROP_NAME_requireMaterial = "requireMaterial";
    public static final int PROP_ID_requireMaterial = 6;
    
    /* 操作工检查: REQUIRE_OPERATOR BOOLEAN */
    public static final String PROP_NAME_requireOperator = "requireOperator";
    public static final int PROP_ID_requireOperator = 7;
    
    /* 工装检查: REQUIRE_TOOLING BOOLEAN */
    public static final String PROP_NAME_requireTooling = "requireTooling";
    public static final int PROP_ID_requireTooling = 8;
    
    /* 前瞻窗口(分钟): MAX_LOOKAHEAD_MINUTES INTEGER */
    public static final String PROP_NAME_maxLookaheadMinutes = "maxLookaheadMinutes";
    public static final int PROP_ID_maxLookaheadMinutes = 9;
    
    /* 提前派工(分钟): DISPATCH_AHEAD_MINUTES INTEGER */
    public static final String PROP_NAME_dispatchAheadMinutes = "dispatchAheadMinutes";
    public static final int PROP_ID_dispatchAheadMinutes = 10;
    
    /* 自动确认物料: AUTO_CONFIRM_MATERIAL BOOLEAN */
    public static final String PROP_NAME_autoConfirmMaterial = "autoConfirmMaterial";
    public static final int PROP_ID_autoConfirmMaterial = 11;
    
    /* 最大并行工序数: MAX_CONCURRENT_OPS INTEGER */
    public static final String PROP_NAME_maxConcurrentOps = "maxConcurrentOps";
    public static final int PROP_ID_maxConcurrentOps = 12;
    
    /* 优先级阈值: PRIORITY_THRESHOLD INTEGER */
    public static final String PROP_NAME_priorityThreshold = "priorityThreshold";
    public static final int PROP_ID_priorityThreshold = 13;
    
    /* 允许时段(JSON): ENABLED_HOURS VARCHAR */
    public static final String PROP_NAME_enabledHours = "enabledHours";
    public static final int PROP_ID_enabledHours = 14;
    
    /* 暂停到: HOLD_UNTIL DATETIME */
    public static final String PROP_NAME_holdUntil = "holdUntil";
    public static final int PROP_ID_holdUntil = 15;
    
    /* 暂停原因: HOLD_REASON VARCHAR */
    public static final String PROP_NAME_holdReason = "holdReason";
    public static final int PROP_ID_holdReason = 16;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 17;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 18;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 19;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 20;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 21;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 22;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 23;
    

    private static int _PROP_ID_BOUND = 24;

    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[24];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_ruleName] = PROP_NAME_ruleName;
          PROP_NAME_TO_ID.put(PROP_NAME_ruleName, PROP_ID_ruleName);
      
          PROP_ID_TO_NAME[PROP_ID_enableAuto] = PROP_NAME_enableAuto;
          PROP_NAME_TO_ID.put(PROP_NAME_enableAuto, PROP_ID_enableAuto);
      
          PROP_ID_TO_NAME[PROP_ID_requireMaterial] = PROP_NAME_requireMaterial;
          PROP_NAME_TO_ID.put(PROP_NAME_requireMaterial, PROP_ID_requireMaterial);
      
          PROP_ID_TO_NAME[PROP_ID_requireOperator] = PROP_NAME_requireOperator;
          PROP_NAME_TO_ID.put(PROP_NAME_requireOperator, PROP_ID_requireOperator);
      
          PROP_ID_TO_NAME[PROP_ID_requireTooling] = PROP_NAME_requireTooling;
          PROP_NAME_TO_ID.put(PROP_NAME_requireTooling, PROP_ID_requireTooling);
      
          PROP_ID_TO_NAME[PROP_ID_maxLookaheadMinutes] = PROP_NAME_maxLookaheadMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_maxLookaheadMinutes, PROP_ID_maxLookaheadMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_dispatchAheadMinutes] = PROP_NAME_dispatchAheadMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_dispatchAheadMinutes, PROP_ID_dispatchAheadMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_autoConfirmMaterial] = PROP_NAME_autoConfirmMaterial;
          PROP_NAME_TO_ID.put(PROP_NAME_autoConfirmMaterial, PROP_ID_autoConfirmMaterial);
      
          PROP_ID_TO_NAME[PROP_ID_maxConcurrentOps] = PROP_NAME_maxConcurrentOps;
          PROP_NAME_TO_ID.put(PROP_NAME_maxConcurrentOps, PROP_ID_maxConcurrentOps);
      
          PROP_ID_TO_NAME[PROP_ID_priorityThreshold] = PROP_NAME_priorityThreshold;
          PROP_NAME_TO_ID.put(PROP_NAME_priorityThreshold, PROP_ID_priorityThreshold);
      
          PROP_ID_TO_NAME[PROP_ID_enabledHours] = PROP_NAME_enabledHours;
          PROP_NAME_TO_ID.put(PROP_NAME_enabledHours, PROP_ID_enabledHours);
      
          PROP_ID_TO_NAME[PROP_ID_holdUntil] = PROP_NAME_holdUntil;
          PROP_NAME_TO_ID.put(PROP_NAME_holdUntil, PROP_ID_holdUntil);
      
          PROP_ID_TO_NAME[PROP_ID_holdReason] = PROP_NAME_holdReason;
          PROP_NAME_TO_ID.put(PROP_NAME_holdReason, PROP_ID_holdReason);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 工作中心: WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 规则名称: RULE_NAME */
    private java.lang.String _ruleName;
    
    /* 自动派工启用: ENABLE_AUTO */
    private java.lang.Boolean _enableAuto;
    
    /* 物料齐套检查: REQUIRE_MATERIAL */
    private java.lang.Boolean _requireMaterial;
    
    /* 操作工检查: REQUIRE_OPERATOR */
    private java.lang.Boolean _requireOperator;
    
    /* 工装检查: REQUIRE_TOOLING */
    private java.lang.Boolean _requireTooling;
    
    /* 前瞻窗口(分钟): MAX_LOOKAHEAD_MINUTES */
    private java.lang.Integer _maxLookaheadMinutes;
    
    /* 提前派工(分钟): DISPATCH_AHEAD_MINUTES */
    private java.lang.Integer _dispatchAheadMinutes;
    
    /* 自动确认物料: AUTO_CONFIRM_MATERIAL */
    private java.lang.Boolean _autoConfirmMaterial;
    
    /* 最大并行工序数: MAX_CONCURRENT_OPS */
    private java.lang.Integer _maxConcurrentOps;
    
    /* 优先级阈值: PRIORITY_THRESHOLD */
    private java.lang.Integer _priorityThreshold;
    
    /* 允许时段(JSON): ENABLED_HOURS */
    private java.lang.String _enabledHours;
    
    /* 暂停到: HOLD_UNTIL */
    private java.time.LocalDateTime _holdUntil;
    
    /* 暂停原因: HOLD_REASON */
    private java.lang.String _holdReason;
    
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
    

    public _ErpApsDispatchRule(){
        // for debug
    }

    protected ErpApsDispatchRule newInstance(){
        ErpApsDispatchRule entity = new ErpApsDispatchRule();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpApsDispatchRule cloneInstance() {
        ErpApsDispatchRule entity = newInstance();
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
      return "app.erp.aps.dao.entity.ErpApsDispatchRule";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_ruleName:
               return getRuleName();
        
            case PROP_ID_enableAuto:
               return getEnableAuto();
        
            case PROP_ID_requireMaterial:
               return getRequireMaterial();
        
            case PROP_ID_requireOperator:
               return getRequireOperator();
        
            case PROP_ID_requireTooling:
               return getRequireTooling();
        
            case PROP_ID_maxLookaheadMinutes:
               return getMaxLookaheadMinutes();
        
            case PROP_ID_dispatchAheadMinutes:
               return getDispatchAheadMinutes();
        
            case PROP_ID_autoConfirmMaterial:
               return getAutoConfirmMaterial();
        
            case PROP_ID_maxConcurrentOps:
               return getMaxConcurrentOps();
        
            case PROP_ID_priorityThreshold:
               return getPriorityThreshold();
        
            case PROP_ID_enabledHours:
               return getEnabledHours();
        
            case PROP_ID_holdUntil:
               return getHoldUntil();
        
            case PROP_ID_holdReason:
               return getHoldReason();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_workcenterId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_workcenterId));
               }
               setWorkcenterId(typedValue);
               break;
            }
        
            case PROP_ID_ruleName:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_ruleName));
               }
               setRuleName(typedValue);
               break;
            }
        
            case PROP_ID_enableAuto:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_enableAuto));
               }
               setEnableAuto(typedValue);
               break;
            }
        
            case PROP_ID_requireMaterial:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_requireMaterial));
               }
               setRequireMaterial(typedValue);
               break;
            }
        
            case PROP_ID_requireOperator:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_requireOperator));
               }
               setRequireOperator(typedValue);
               break;
            }
        
            case PROP_ID_requireTooling:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_requireTooling));
               }
               setRequireTooling(typedValue);
               break;
            }
        
            case PROP_ID_maxLookaheadMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxLookaheadMinutes));
               }
               setMaxLookaheadMinutes(typedValue);
               break;
            }
        
            case PROP_ID_dispatchAheadMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_dispatchAheadMinutes));
               }
               setDispatchAheadMinutes(typedValue);
               break;
            }
        
            case PROP_ID_autoConfirmMaterial:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_autoConfirmMaterial));
               }
               setAutoConfirmMaterial(typedValue);
               break;
            }
        
            case PROP_ID_maxConcurrentOps:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_maxConcurrentOps));
               }
               setMaxConcurrentOps(typedValue);
               break;
            }
        
            case PROP_ID_priorityThreshold:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_priorityThreshold));
               }
               setPriorityThreshold(typedValue);
               break;
            }
        
            case PROP_ID_enabledHours:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_enabledHours));
               }
               setEnabledHours(typedValue);
               break;
            }
        
            case PROP_ID_holdUntil:{
               java.time.LocalDateTime typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDateTime(value,
                       err-> newTypeConversionError(PROP_NAME_holdUntil));
               }
               setHoldUntil(typedValue);
               break;
            }
        
            case PROP_ID_holdReason:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_holdReason));
               }
               setHoldReason(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_ruleName:{
               onInitProp(propId);
               this._ruleName = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_enableAuto:{
               onInitProp(propId);
               this._enableAuto = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_requireMaterial:{
               onInitProp(propId);
               this._requireMaterial = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_requireOperator:{
               onInitProp(propId);
               this._requireOperator = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_requireTooling:{
               onInitProp(propId);
               this._requireTooling = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_maxLookaheadMinutes:{
               onInitProp(propId);
               this._maxLookaheadMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_dispatchAheadMinutes:{
               onInitProp(propId);
               this._dispatchAheadMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_autoConfirmMaterial:{
               onInitProp(propId);
               this._autoConfirmMaterial = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_maxConcurrentOps:{
               onInitProp(propId);
               this._maxConcurrentOps = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_priorityThreshold:{
               onInitProp(propId);
               this._priorityThreshold = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_enabledHours:{
               onInitProp(propId);
               this._enabledHours = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_holdUntil:{
               onInitProp(propId);
               this._holdUntil = (java.time.LocalDateTime)value;
               
               break;
            }
        
            case PROP_ID_holdReason:{
               onInitProp(propId);
               this._holdReason = (java.lang.String)value;
               
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
     * 工作中心: WORKCENTER_ID
     */
    public final java.lang.Long getWorkcenterId(){
         onPropGet(PROP_ID_workcenterId);
         return _workcenterId;
    }

    /**
     * 工作中心: WORKCENTER_ID
     */
    public final void setWorkcenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_workcenterId,value)){
            this._workcenterId = value;
            internalClearRefs(PROP_ID_workcenterId);
            
        }
    }
    
    /**
     * 规则名称: RULE_NAME
     */
    public final java.lang.String getRuleName(){
         onPropGet(PROP_ID_ruleName);
         return _ruleName;
    }

    /**
     * 规则名称: RULE_NAME
     */
    public final void setRuleName(java.lang.String value){
        if(onPropSet(PROP_ID_ruleName,value)){
            this._ruleName = value;
            internalClearRefs(PROP_ID_ruleName);
            
        }
    }
    
    /**
     * 自动派工启用: ENABLE_AUTO
     */
    public final java.lang.Boolean getEnableAuto(){
         onPropGet(PROP_ID_enableAuto);
         return _enableAuto;
    }

    /**
     * 自动派工启用: ENABLE_AUTO
     */
    public final void setEnableAuto(java.lang.Boolean value){
        if(onPropSet(PROP_ID_enableAuto,value)){
            this._enableAuto = value;
            internalClearRefs(PROP_ID_enableAuto);
            
        }
    }
    
    /**
     * 物料齐套检查: REQUIRE_MATERIAL
     */
    public final java.lang.Boolean getRequireMaterial(){
         onPropGet(PROP_ID_requireMaterial);
         return _requireMaterial;
    }

    /**
     * 物料齐套检查: REQUIRE_MATERIAL
     */
    public final void setRequireMaterial(java.lang.Boolean value){
        if(onPropSet(PROP_ID_requireMaterial,value)){
            this._requireMaterial = value;
            internalClearRefs(PROP_ID_requireMaterial);
            
        }
    }
    
    /**
     * 操作工检查: REQUIRE_OPERATOR
     */
    public final java.lang.Boolean getRequireOperator(){
         onPropGet(PROP_ID_requireOperator);
         return _requireOperator;
    }

    /**
     * 操作工检查: REQUIRE_OPERATOR
     */
    public final void setRequireOperator(java.lang.Boolean value){
        if(onPropSet(PROP_ID_requireOperator,value)){
            this._requireOperator = value;
            internalClearRefs(PROP_ID_requireOperator);
            
        }
    }
    
    /**
     * 工装检查: REQUIRE_TOOLING
     */
    public final java.lang.Boolean getRequireTooling(){
         onPropGet(PROP_ID_requireTooling);
         return _requireTooling;
    }

    /**
     * 工装检查: REQUIRE_TOOLING
     */
    public final void setRequireTooling(java.lang.Boolean value){
        if(onPropSet(PROP_ID_requireTooling,value)){
            this._requireTooling = value;
            internalClearRefs(PROP_ID_requireTooling);
            
        }
    }
    
    /**
     * 前瞻窗口(分钟): MAX_LOOKAHEAD_MINUTES
     */
    public final java.lang.Integer getMaxLookaheadMinutes(){
         onPropGet(PROP_ID_maxLookaheadMinutes);
         return _maxLookaheadMinutes;
    }

    /**
     * 前瞻窗口(分钟): MAX_LOOKAHEAD_MINUTES
     */
    public final void setMaxLookaheadMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxLookaheadMinutes,value)){
            this._maxLookaheadMinutes = value;
            internalClearRefs(PROP_ID_maxLookaheadMinutes);
            
        }
    }
    
    /**
     * 提前派工(分钟): DISPATCH_AHEAD_MINUTES
     */
    public final java.lang.Integer getDispatchAheadMinutes(){
         onPropGet(PROP_ID_dispatchAheadMinutes);
         return _dispatchAheadMinutes;
    }

    /**
     * 提前派工(分钟): DISPATCH_AHEAD_MINUTES
     */
    public final void setDispatchAheadMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_dispatchAheadMinutes,value)){
            this._dispatchAheadMinutes = value;
            internalClearRefs(PROP_ID_dispatchAheadMinutes);
            
        }
    }
    
    /**
     * 自动确认物料: AUTO_CONFIRM_MATERIAL
     */
    public final java.lang.Boolean getAutoConfirmMaterial(){
         onPropGet(PROP_ID_autoConfirmMaterial);
         return _autoConfirmMaterial;
    }

    /**
     * 自动确认物料: AUTO_CONFIRM_MATERIAL
     */
    public final void setAutoConfirmMaterial(java.lang.Boolean value){
        if(onPropSet(PROP_ID_autoConfirmMaterial,value)){
            this._autoConfirmMaterial = value;
            internalClearRefs(PROP_ID_autoConfirmMaterial);
            
        }
    }
    
    /**
     * 最大并行工序数: MAX_CONCURRENT_OPS
     */
    public final java.lang.Integer getMaxConcurrentOps(){
         onPropGet(PROP_ID_maxConcurrentOps);
         return _maxConcurrentOps;
    }

    /**
     * 最大并行工序数: MAX_CONCURRENT_OPS
     */
    public final void setMaxConcurrentOps(java.lang.Integer value){
        if(onPropSet(PROP_ID_maxConcurrentOps,value)){
            this._maxConcurrentOps = value;
            internalClearRefs(PROP_ID_maxConcurrentOps);
            
        }
    }
    
    /**
     * 优先级阈值: PRIORITY_THRESHOLD
     */
    public final java.lang.Integer getPriorityThreshold(){
         onPropGet(PROP_ID_priorityThreshold);
         return _priorityThreshold;
    }

    /**
     * 优先级阈值: PRIORITY_THRESHOLD
     */
    public final void setPriorityThreshold(java.lang.Integer value){
        if(onPropSet(PROP_ID_priorityThreshold,value)){
            this._priorityThreshold = value;
            internalClearRefs(PROP_ID_priorityThreshold);
            
        }
    }
    
    /**
     * 允许时段(JSON): ENABLED_HOURS
     */
    public final java.lang.String getEnabledHours(){
         onPropGet(PROP_ID_enabledHours);
         return _enabledHours;
    }

    /**
     * 允许时段(JSON): ENABLED_HOURS
     */
    public final void setEnabledHours(java.lang.String value){
        if(onPropSet(PROP_ID_enabledHours,value)){
            this._enabledHours = value;
            internalClearRefs(PROP_ID_enabledHours);
            
        }
    }
    
    /**
     * 暂停到: HOLD_UNTIL
     */
    public final java.time.LocalDateTime getHoldUntil(){
         onPropGet(PROP_ID_holdUntil);
         return _holdUntil;
    }

    /**
     * 暂停到: HOLD_UNTIL
     */
    public final void setHoldUntil(java.time.LocalDateTime value){
        if(onPropSet(PROP_ID_holdUntil,value)){
            this._holdUntil = value;
            internalClearRefs(PROP_ID_holdUntil);
            
        }
    }
    
    /**
     * 暂停原因: HOLD_REASON
     */
    public final java.lang.String getHoldReason(){
         onPropGet(PROP_ID_holdReason);
         return _holdReason;
    }

    /**
     * 暂停原因: HOLD_REASON
     */
    public final void setHoldReason(java.lang.String value){
        if(onPropSet(PROP_ID_holdReason,value)){
            this._holdReason = value;
            internalClearRefs(PROP_ID_holdReason);
            
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
