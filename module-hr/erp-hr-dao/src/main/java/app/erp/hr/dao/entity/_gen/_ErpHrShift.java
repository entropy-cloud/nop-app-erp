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

import app.erp.hr.dao.entity.ErpHrShift;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  班次模板: erp_hr_shift
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpHrShift extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 3;
    
    /* 班次名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 4;
    
    /* 班次类型: SHIFT_TYPE INTEGER */
    public static final String PROP_NAME_shiftType = "shiftType";
    public static final int PROP_ID_shiftType = 5;
    
    /* 上班时间: START_TIME VARCHAR */
    public static final String PROP_NAME_startTime = "startTime";
    public static final int PROP_ID_startTime = 6;
    
    /* 下班时间: END_TIME VARCHAR */
    public static final String PROP_NAME_endTime = "endTime";
    public static final int PROP_ID_endTime = 7;
    
    /* 迟到宽容分钟数: GRACE_LATE_MINUTES INTEGER */
    public static final String PROP_NAME_graceLateMinutes = "graceLateMinutes";
    public static final int PROP_ID_graceLateMinutes = 8;
    
    /* 早退宽容分钟数: GRACE_EARLY_LEAVE_MINUTES INTEGER */
    public static final String PROP_NAME_graceEarlyLeaveMinutes = "graceEarlyLeaveMinutes";
    public static final int PROP_ID_graceEarlyLeaveMinutes = 9;
    
    /* 需签到: REQUIRE_CLOCK_IN BOOLEAN */
    public static final String PROP_NAME_requireClockIn = "requireClockIn";
    public static final int PROP_ID_requireClockIn = 10;
    
    /* 需签退: REQUIRE_CLOCK_OUT BOOLEAN */
    public static final String PROP_NAME_requireClockOut = "requireClockOut";
    public static final int PROP_ID_requireClockOut = 11;
    
    /* 休息开始: REST_START_TIME VARCHAR */
    public static final String PROP_NAME_restStartTime = "restStartTime";
    public static final int PROP_ID_restStartTime = 12;
    
    /* 休息结束: REST_END_TIME VARCHAR */
    public static final String PROP_NAME_restEndTime = "restEndTime";
    public static final int PROP_ID_restEndTime = 13;
    
    /* 标准工时(分钟): TOTAL_WORK_MINUTES INTEGER */
    public static final String PROP_NAME_totalWorkMinutes = "totalWorkMinutes";
    public static final int PROP_ID_totalWorkMinutes = 14;
    
    /* 允许加班: ALLOW_OVERTIME BOOLEAN */
    public static final String PROP_NAME_allowOvertime = "allowOvertime";
    public static final int PROP_ID_allowOvertime = 15;
    
    /* 显示颜色: COLOR_HEX VARCHAR */
    public static final String PROP_NAME_colorHex = "colorHex";
    public static final int PROP_ID_colorHex = 16;
    
    /* 说明: DESCRIPTION VARCHAR */
    public static final String PROP_NAME_description = "description";
    public static final int PROP_ID_description = 17;
    
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
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_shiftType] = PROP_NAME_shiftType;
          PROP_NAME_TO_ID.put(PROP_NAME_shiftType, PROP_ID_shiftType);
      
          PROP_ID_TO_NAME[PROP_ID_startTime] = PROP_NAME_startTime;
          PROP_NAME_TO_ID.put(PROP_NAME_startTime, PROP_ID_startTime);
      
          PROP_ID_TO_NAME[PROP_ID_endTime] = PROP_NAME_endTime;
          PROP_NAME_TO_ID.put(PROP_NAME_endTime, PROP_ID_endTime);
      
          PROP_ID_TO_NAME[PROP_ID_graceLateMinutes] = PROP_NAME_graceLateMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_graceLateMinutes, PROP_ID_graceLateMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_graceEarlyLeaveMinutes] = PROP_NAME_graceEarlyLeaveMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_graceEarlyLeaveMinutes, PROP_ID_graceEarlyLeaveMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_requireClockIn] = PROP_NAME_requireClockIn;
          PROP_NAME_TO_ID.put(PROP_NAME_requireClockIn, PROP_ID_requireClockIn);
      
          PROP_ID_TO_NAME[PROP_ID_requireClockOut] = PROP_NAME_requireClockOut;
          PROP_NAME_TO_ID.put(PROP_NAME_requireClockOut, PROP_ID_requireClockOut);
      
          PROP_ID_TO_NAME[PROP_ID_restStartTime] = PROP_NAME_restStartTime;
          PROP_NAME_TO_ID.put(PROP_NAME_restStartTime, PROP_ID_restStartTime);
      
          PROP_ID_TO_NAME[PROP_ID_restEndTime] = PROP_NAME_restEndTime;
          PROP_NAME_TO_ID.put(PROP_NAME_restEndTime, PROP_ID_restEndTime);
      
          PROP_ID_TO_NAME[PROP_ID_totalWorkMinutes] = PROP_NAME_totalWorkMinutes;
          PROP_NAME_TO_ID.put(PROP_NAME_totalWorkMinutes, PROP_ID_totalWorkMinutes);
      
          PROP_ID_TO_NAME[PROP_ID_allowOvertime] = PROP_NAME_allowOvertime;
          PROP_NAME_TO_ID.put(PROP_NAME_allowOvertime, PROP_ID_allowOvertime);
      
          PROP_ID_TO_NAME[PROP_ID_colorHex] = PROP_NAME_colorHex;
          PROP_NAME_TO_ID.put(PROP_NAME_colorHex, PROP_ID_colorHex);
      
          PROP_ID_TO_NAME[PROP_ID_description] = PROP_NAME_description;
          PROP_NAME_TO_ID.put(PROP_NAME_description, PROP_ID_description);
      
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
    
    /* 业务组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 班次名称: NAME */
    private java.lang.String _name;
    
    /* 班次类型: SHIFT_TYPE */
    private java.lang.Integer _shiftType;
    
    /* 上班时间: START_TIME */
    private java.lang.String _startTime;
    
    /* 下班时间: END_TIME */
    private java.lang.String _endTime;
    
    /* 迟到宽容分钟数: GRACE_LATE_MINUTES */
    private java.lang.Integer _graceLateMinutes;
    
    /* 早退宽容分钟数: GRACE_EARLY_LEAVE_MINUTES */
    private java.lang.Integer _graceEarlyLeaveMinutes;
    
    /* 需签到: REQUIRE_CLOCK_IN */
    private java.lang.Boolean _requireClockIn;
    
    /* 需签退: REQUIRE_CLOCK_OUT */
    private java.lang.Boolean _requireClockOut;
    
    /* 休息开始: REST_START_TIME */
    private java.lang.String _restStartTime;
    
    /* 休息结束: REST_END_TIME */
    private java.lang.String _restEndTime;
    
    /* 标准工时(分钟): TOTAL_WORK_MINUTES */
    private java.lang.Integer _totalWorkMinutes;
    
    /* 允许加班: ALLOW_OVERTIME */
    private java.lang.Boolean _allowOvertime;
    
    /* 显示颜色: COLOR_HEX */
    private java.lang.String _colorHex;
    
    /* 说明: DESCRIPTION */
    private java.lang.String _description;
    
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
    

    public _ErpHrShift(){
        // for debug
    }

    protected ErpHrShift newInstance(){
        ErpHrShift entity = new ErpHrShift();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpHrShift cloneInstance() {
        ErpHrShift entity = newInstance();
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
      return "app.erp.hr.dao.entity.ErpHrShift";
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
        
            case PROP_ID_name:
               return getName();
        
            case PROP_ID_shiftType:
               return getShiftType();
        
            case PROP_ID_startTime:
               return getStartTime();
        
            case PROP_ID_endTime:
               return getEndTime();
        
            case PROP_ID_graceLateMinutes:
               return getGraceLateMinutes();
        
            case PROP_ID_graceEarlyLeaveMinutes:
               return getGraceEarlyLeaveMinutes();
        
            case PROP_ID_requireClockIn:
               return getRequireClockIn();
        
            case PROP_ID_requireClockOut:
               return getRequireClockOut();
        
            case PROP_ID_restStartTime:
               return getRestStartTime();
        
            case PROP_ID_restEndTime:
               return getRestEndTime();
        
            case PROP_ID_totalWorkMinutes:
               return getTotalWorkMinutes();
        
            case PROP_ID_allowOvertime:
               return getAllowOvertime();
        
            case PROP_ID_colorHex:
               return getColorHex();
        
            case PROP_ID_description:
               return getDescription();
        
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
        
            case PROP_ID_name:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_name));
               }
               setName(typedValue);
               break;
            }
        
            case PROP_ID_shiftType:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_shiftType));
               }
               setShiftType(typedValue);
               break;
            }
        
            case PROP_ID_startTime:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_startTime));
               }
               setStartTime(typedValue);
               break;
            }
        
            case PROP_ID_endTime:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_endTime));
               }
               setEndTime(typedValue);
               break;
            }
        
            case PROP_ID_graceLateMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_graceLateMinutes));
               }
               setGraceLateMinutes(typedValue);
               break;
            }
        
            case PROP_ID_graceEarlyLeaveMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_graceEarlyLeaveMinutes));
               }
               setGraceEarlyLeaveMinutes(typedValue);
               break;
            }
        
            case PROP_ID_requireClockIn:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_requireClockIn));
               }
               setRequireClockIn(typedValue);
               break;
            }
        
            case PROP_ID_requireClockOut:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_requireClockOut));
               }
               setRequireClockOut(typedValue);
               break;
            }
        
            case PROP_ID_restStartTime:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_restStartTime));
               }
               setRestStartTime(typedValue);
               break;
            }
        
            case PROP_ID_restEndTime:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_restEndTime));
               }
               setRestEndTime(typedValue);
               break;
            }
        
            case PROP_ID_totalWorkMinutes:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_totalWorkMinutes));
               }
               setTotalWorkMinutes(typedValue);
               break;
            }
        
            case PROP_ID_allowOvertime:{
               java.lang.Boolean typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBoolean(value,
                       err-> newTypeConversionError(PROP_NAME_allowOvertime));
               }
               setAllowOvertime(typedValue);
               break;
            }
        
            case PROP_ID_colorHex:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_colorHex));
               }
               setColorHex(typedValue);
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
        
            case PROP_ID_name:{
               onInitProp(propId);
               this._name = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_shiftType:{
               onInitProp(propId);
               this._shiftType = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_startTime:{
               onInitProp(propId);
               this._startTime = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_endTime:{
               onInitProp(propId);
               this._endTime = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_graceLateMinutes:{
               onInitProp(propId);
               this._graceLateMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_graceEarlyLeaveMinutes:{
               onInitProp(propId);
               this._graceEarlyLeaveMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_requireClockIn:{
               onInitProp(propId);
               this._requireClockIn = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_requireClockOut:{
               onInitProp(propId);
               this._requireClockOut = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_restStartTime:{
               onInitProp(propId);
               this._restStartTime = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_restEndTime:{
               onInitProp(propId);
               this._restEndTime = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_totalWorkMinutes:{
               onInitProp(propId);
               this._totalWorkMinutes = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_allowOvertime:{
               onInitProp(propId);
               this._allowOvertime = (java.lang.Boolean)value;
               
               break;
            }
        
            case PROP_ID_colorHex:{
               onInitProp(propId);
               this._colorHex = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_description:{
               onInitProp(propId);
               this._description = (java.lang.String)value;
               
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
     * 班次名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 班次名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 班次类型: SHIFT_TYPE
     */
    public final java.lang.Integer getShiftType(){
         onPropGet(PROP_ID_shiftType);
         return _shiftType;
    }

    /**
     * 班次类型: SHIFT_TYPE
     */
    public final void setShiftType(java.lang.Integer value){
        if(onPropSet(PROP_ID_shiftType,value)){
            this._shiftType = value;
            internalClearRefs(PROP_ID_shiftType);
            
        }
    }
    
    /**
     * 上班时间: START_TIME
     */
    public final java.lang.String getStartTime(){
         onPropGet(PROP_ID_startTime);
         return _startTime;
    }

    /**
     * 上班时间: START_TIME
     */
    public final void setStartTime(java.lang.String value){
        if(onPropSet(PROP_ID_startTime,value)){
            this._startTime = value;
            internalClearRefs(PROP_ID_startTime);
            
        }
    }
    
    /**
     * 下班时间: END_TIME
     */
    public final java.lang.String getEndTime(){
         onPropGet(PROP_ID_endTime);
         return _endTime;
    }

    /**
     * 下班时间: END_TIME
     */
    public final void setEndTime(java.lang.String value){
        if(onPropSet(PROP_ID_endTime,value)){
            this._endTime = value;
            internalClearRefs(PROP_ID_endTime);
            
        }
    }
    
    /**
     * 迟到宽容分钟数: GRACE_LATE_MINUTES
     */
    public final java.lang.Integer getGraceLateMinutes(){
         onPropGet(PROP_ID_graceLateMinutes);
         return _graceLateMinutes;
    }

    /**
     * 迟到宽容分钟数: GRACE_LATE_MINUTES
     */
    public final void setGraceLateMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_graceLateMinutes,value)){
            this._graceLateMinutes = value;
            internalClearRefs(PROP_ID_graceLateMinutes);
            
        }
    }
    
    /**
     * 早退宽容分钟数: GRACE_EARLY_LEAVE_MINUTES
     */
    public final java.lang.Integer getGraceEarlyLeaveMinutes(){
         onPropGet(PROP_ID_graceEarlyLeaveMinutes);
         return _graceEarlyLeaveMinutes;
    }

    /**
     * 早退宽容分钟数: GRACE_EARLY_LEAVE_MINUTES
     */
    public final void setGraceEarlyLeaveMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_graceEarlyLeaveMinutes,value)){
            this._graceEarlyLeaveMinutes = value;
            internalClearRefs(PROP_ID_graceEarlyLeaveMinutes);
            
        }
    }
    
    /**
     * 需签到: REQUIRE_CLOCK_IN
     */
    public final java.lang.Boolean getRequireClockIn(){
         onPropGet(PROP_ID_requireClockIn);
         return _requireClockIn;
    }

    /**
     * 需签到: REQUIRE_CLOCK_IN
     */
    public final void setRequireClockIn(java.lang.Boolean value){
        if(onPropSet(PROP_ID_requireClockIn,value)){
            this._requireClockIn = value;
            internalClearRefs(PROP_ID_requireClockIn);
            
        }
    }
    
    /**
     * 需签退: REQUIRE_CLOCK_OUT
     */
    public final java.lang.Boolean getRequireClockOut(){
         onPropGet(PROP_ID_requireClockOut);
         return _requireClockOut;
    }

    /**
     * 需签退: REQUIRE_CLOCK_OUT
     */
    public final void setRequireClockOut(java.lang.Boolean value){
        if(onPropSet(PROP_ID_requireClockOut,value)){
            this._requireClockOut = value;
            internalClearRefs(PROP_ID_requireClockOut);
            
        }
    }
    
    /**
     * 休息开始: REST_START_TIME
     */
    public final java.lang.String getRestStartTime(){
         onPropGet(PROP_ID_restStartTime);
         return _restStartTime;
    }

    /**
     * 休息开始: REST_START_TIME
     */
    public final void setRestStartTime(java.lang.String value){
        if(onPropSet(PROP_ID_restStartTime,value)){
            this._restStartTime = value;
            internalClearRefs(PROP_ID_restStartTime);
            
        }
    }
    
    /**
     * 休息结束: REST_END_TIME
     */
    public final java.lang.String getRestEndTime(){
         onPropGet(PROP_ID_restEndTime);
         return _restEndTime;
    }

    /**
     * 休息结束: REST_END_TIME
     */
    public final void setRestEndTime(java.lang.String value){
        if(onPropSet(PROP_ID_restEndTime,value)){
            this._restEndTime = value;
            internalClearRefs(PROP_ID_restEndTime);
            
        }
    }
    
    /**
     * 标准工时(分钟): TOTAL_WORK_MINUTES
     */
    public final java.lang.Integer getTotalWorkMinutes(){
         onPropGet(PROP_ID_totalWorkMinutes);
         return _totalWorkMinutes;
    }

    /**
     * 标准工时(分钟): TOTAL_WORK_MINUTES
     */
    public final void setTotalWorkMinutes(java.lang.Integer value){
        if(onPropSet(PROP_ID_totalWorkMinutes,value)){
            this._totalWorkMinutes = value;
            internalClearRefs(PROP_ID_totalWorkMinutes);
            
        }
    }
    
    /**
     * 允许加班: ALLOW_OVERTIME
     */
    public final java.lang.Boolean getAllowOvertime(){
         onPropGet(PROP_ID_allowOvertime);
         return _allowOvertime;
    }

    /**
     * 允许加班: ALLOW_OVERTIME
     */
    public final void setAllowOvertime(java.lang.Boolean value){
        if(onPropSet(PROP_ID_allowOvertime,value)){
            this._allowOvertime = value;
            internalClearRefs(PROP_ID_allowOvertime);
            
        }
    }
    
    /**
     * 显示颜色: COLOR_HEX
     */
    public final java.lang.String getColorHex(){
         onPropGet(PROP_ID_colorHex);
         return _colorHex;
    }

    /**
     * 显示颜色: COLOR_HEX
     */
    public final void setColorHex(java.lang.String value){
        if(onPropSet(PROP_ID_colorHex,value)){
            this._colorHex = value;
            internalClearRefs(PROP_ID_colorHex);
            
        }
    }
    
    /**
     * 说明: DESCRIPTION
     */
    public final java.lang.String getDescription(){
         onPropGet(PROP_ID_description);
         return _description;
    }

    /**
     * 说明: DESCRIPTION
     */
    public final void setDescription(java.lang.String value){
        if(onPropSet(PROP_ID_description,value)){
            this._description = value;
            internalClearRefs(PROP_ID_description);
            
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
