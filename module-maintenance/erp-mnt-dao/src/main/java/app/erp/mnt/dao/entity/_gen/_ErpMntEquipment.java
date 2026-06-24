package app.erp.mnt.dao.entity._gen;

import io.nop.orm.model.IEntityModel;
import io.nop.orm.support.DynamicOrmEntity;
import io.nop.orm.support.OrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.orm.IOrmEntitySet; //NOPMD - suppressed UnusedImports - Auto Gen Code
import io.nop.api.core.convert.ConvertHelper;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;

import app.erp.mnt.dao.entity.ErpMntEquipment;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  设备: erp_mnt_equipment
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpMntEquipment extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 设备编码: CODE VARCHAR */
    public static final String PROP_NAME_code = "code";
    public static final int PROP_ID_code = 2;
    
    /* 设备名称: NAME VARCHAR */
    public static final String PROP_NAME_name = "name";
    public static final int PROP_ID_name = 3;
    
    /* 所属组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 4;
    
    /* 资产卡片(关联 assets 域): ASSET_ID BIGINT */
    public static final String PROP_NAME_assetId = "assetId";
    public static final int PROP_ID_assetId = 5;
    
    /* 关联工作中心(关联 mfg 域): WORKCENTER_ID BIGINT */
    public static final String PROP_NAME_workcenterId = "workcenterId";
    public static final int PROP_ID_workcenterId = 6;
    
    /* 位置ID: LOCATION_ID BIGINT */
    public static final String PROP_NAME_locationId = "locationId";
    public static final int PROP_ID_locationId = 7;
    
    /* 分类ID: CATEGORY_ID BIGINT */
    public static final String PROP_NAME_categoryId = "categoryId";
    public static final int PROP_ID_categoryId = 8;
    
    /* 状态: STATUS INTEGER */
    public static final String PROP_NAME_status = "status";
    public static final int PROP_ID_status = 9;
    
    /* 序列号: SERIAL_NO VARCHAR */
    public static final String PROP_NAME_serialNo = "serialNo";
    public static final int PROP_ID_serialNo = 10;
    
    /* 制造商: MANUFACTURER VARCHAR */
    public static final String PROP_NAME_manufacturer = "manufacturer";
    public static final int PROP_ID_manufacturer = 11;
    
    /* 型号: MODEL VARCHAR */
    public static final String PROP_NAME_model = "model";
    public static final int PROP_ID_model = 12;
    
    /* 安装日期: INSTALL_DATE DATE */
    public static final String PROP_NAME_installDate = "installDate";
    public static final int PROP_ID_installDate = 13;
    
    /* 保修到期: WARRANTY_EXPIRY DATE */
    public static final String PROP_NAME_warrantyExpiry = "warrantyExpiry";
    public static final int PROP_ID_warrantyExpiry = 14;
    
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
    public static final String PROP_NAME_location = "location";
    
    /* relation:  */
    public static final String PROP_NAME_category = "category";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    
    /* relation:  */
    public static final String PROP_NAME_visits = "visits";
    
    /* relation:  */
    public static final String PROP_NAME_schedules = "schedules";
    
    /* relation:  */
    public static final String PROP_NAME_requests = "requests";
    
    /* relation:  */
    public static final String PROP_NAME_sparePartUsages = "sparePartUsages";
    
    /* relation:  */
    public static final String PROP_NAME_downtimeEntries = "downtimeEntries";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[22];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_code] = PROP_NAME_code;
          PROP_NAME_TO_ID.put(PROP_NAME_code, PROP_ID_code);
      
          PROP_ID_TO_NAME[PROP_ID_name] = PROP_NAME_name;
          PROP_NAME_TO_ID.put(PROP_NAME_name, PROP_ID_name);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_assetId] = PROP_NAME_assetId;
          PROP_NAME_TO_ID.put(PROP_NAME_assetId, PROP_ID_assetId);
      
          PROP_ID_TO_NAME[PROP_ID_workcenterId] = PROP_NAME_workcenterId;
          PROP_NAME_TO_ID.put(PROP_NAME_workcenterId, PROP_ID_workcenterId);
      
          PROP_ID_TO_NAME[PROP_ID_locationId] = PROP_NAME_locationId;
          PROP_NAME_TO_ID.put(PROP_NAME_locationId, PROP_ID_locationId);
      
          PROP_ID_TO_NAME[PROP_ID_categoryId] = PROP_NAME_categoryId;
          PROP_NAME_TO_ID.put(PROP_NAME_categoryId, PROP_ID_categoryId);
      
          PROP_ID_TO_NAME[PROP_ID_status] = PROP_NAME_status;
          PROP_NAME_TO_ID.put(PROP_NAME_status, PROP_ID_status);
      
          PROP_ID_TO_NAME[PROP_ID_serialNo] = PROP_NAME_serialNo;
          PROP_NAME_TO_ID.put(PROP_NAME_serialNo, PROP_ID_serialNo);
      
          PROP_ID_TO_NAME[PROP_ID_manufacturer] = PROP_NAME_manufacturer;
          PROP_NAME_TO_ID.put(PROP_NAME_manufacturer, PROP_ID_manufacturer);
      
          PROP_ID_TO_NAME[PROP_ID_model] = PROP_NAME_model;
          PROP_NAME_TO_ID.put(PROP_NAME_model, PROP_ID_model);
      
          PROP_ID_TO_NAME[PROP_ID_installDate] = PROP_NAME_installDate;
          PROP_NAME_TO_ID.put(PROP_NAME_installDate, PROP_ID_installDate);
      
          PROP_ID_TO_NAME[PROP_ID_warrantyExpiry] = PROP_NAME_warrantyExpiry;
          PROP_NAME_TO_ID.put(PROP_NAME_warrantyExpiry, PROP_ID_warrantyExpiry);
      
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
    
    /* 设备编码: CODE */
    private java.lang.String _code;
    
    /* 设备名称: NAME */
    private java.lang.String _name;
    
    /* 所属组织: ORG_ID */
    private java.lang.Long _orgId;
    
    /* 资产卡片(关联 assets 域): ASSET_ID */
    private java.lang.Long _assetId;
    
    /* 关联工作中心(关联 mfg 域): WORKCENTER_ID */
    private java.lang.Long _workcenterId;
    
    /* 位置ID: LOCATION_ID */
    private java.lang.Long _locationId;
    
    /* 分类ID: CATEGORY_ID */
    private java.lang.Long _categoryId;
    
    /* 状态: STATUS */
    private java.lang.Integer _status;
    
    /* 序列号: SERIAL_NO */
    private java.lang.String _serialNo;
    
    /* 制造商: MANUFACTURER */
    private java.lang.String _manufacturer;
    
    /* 型号: MODEL */
    private java.lang.String _model;
    
    /* 安装日期: INSTALL_DATE */
    private java.time.LocalDate _installDate;
    
    /* 保修到期: WARRANTY_EXPIRY */
    private java.time.LocalDate _warrantyExpiry;
    
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
    

    public _ErpMntEquipment(){
        // for debug
    }

    protected ErpMntEquipment newInstance(){
        ErpMntEquipment entity = new ErpMntEquipment();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpMntEquipment cloneInstance() {
        ErpMntEquipment entity = newInstance();
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
      return "app.erp.mnt.dao.entity.ErpMntEquipment";
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
        
            case PROP_ID_orgId:
               return getOrgId();
        
            case PROP_ID_assetId:
               return getAssetId();
        
            case PROP_ID_workcenterId:
               return getWorkcenterId();
        
            case PROP_ID_locationId:
               return getLocationId();
        
            case PROP_ID_categoryId:
               return getCategoryId();
        
            case PROP_ID_status:
               return getStatus();
        
            case PROP_ID_serialNo:
               return getSerialNo();
        
            case PROP_ID_manufacturer:
               return getManufacturer();
        
            case PROP_ID_model:
               return getModel();
        
            case PROP_ID_installDate:
               return getInstallDate();
        
            case PROP_ID_warrantyExpiry:
               return getWarrantyExpiry();
        
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
        
            case PROP_ID_orgId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_orgId));
               }
               setOrgId(typedValue);
               break;
            }
        
            case PROP_ID_assetId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_assetId));
               }
               setAssetId(typedValue);
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
        
            case PROP_ID_locationId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_locationId));
               }
               setLocationId(typedValue);
               break;
            }
        
            case PROP_ID_categoryId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_categoryId));
               }
               setCategoryId(typedValue);
               break;
            }
        
            case PROP_ID_status:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_status));
               }
               setStatus(typedValue);
               break;
            }
        
            case PROP_ID_serialNo:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_serialNo));
               }
               setSerialNo(typedValue);
               break;
            }
        
            case PROP_ID_manufacturer:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_manufacturer));
               }
               setManufacturer(typedValue);
               break;
            }
        
            case PROP_ID_model:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_model));
               }
               setModel(typedValue);
               break;
            }
        
            case PROP_ID_installDate:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_installDate));
               }
               setInstallDate(typedValue);
               break;
            }
        
            case PROP_ID_warrantyExpiry:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_warrantyExpiry));
               }
               setWarrantyExpiry(typedValue);
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
        
            case PROP_ID_orgId:{
               onInitProp(propId);
               this._orgId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_assetId:{
               onInitProp(propId);
               this._assetId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_workcenterId:{
               onInitProp(propId);
               this._workcenterId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_locationId:{
               onInitProp(propId);
               this._locationId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_categoryId:{
               onInitProp(propId);
               this._categoryId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_status:{
               onInitProp(propId);
               this._status = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_serialNo:{
               onInitProp(propId);
               this._serialNo = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_manufacturer:{
               onInitProp(propId);
               this._manufacturer = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_model:{
               onInitProp(propId);
               this._model = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_installDate:{
               onInitProp(propId);
               this._installDate = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_warrantyExpiry:{
               onInitProp(propId);
               this._warrantyExpiry = (java.time.LocalDate)value;
               
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
     * 设备编码: CODE
     */
    public final java.lang.String getCode(){
         onPropGet(PROP_ID_code);
         return _code;
    }

    /**
     * 设备编码: CODE
     */
    public final void setCode(java.lang.String value){
        if(onPropSet(PROP_ID_code,value)){
            this._code = value;
            internalClearRefs(PROP_ID_code);
            
        }
    }
    
    /**
     * 设备名称: NAME
     */
    public final java.lang.String getName(){
         onPropGet(PROP_ID_name);
         return _name;
    }

    /**
     * 设备名称: NAME
     */
    public final void setName(java.lang.String value){
        if(onPropSet(PROP_ID_name,value)){
            this._name = value;
            internalClearRefs(PROP_ID_name);
            
        }
    }
    
    /**
     * 所属组织: ORG_ID
     */
    public final java.lang.Long getOrgId(){
         onPropGet(PROP_ID_orgId);
         return _orgId;
    }

    /**
     * 所属组织: ORG_ID
     */
    public final void setOrgId(java.lang.Long value){
        if(onPropSet(PROP_ID_orgId,value)){
            this._orgId = value;
            internalClearRefs(PROP_ID_orgId);
            
        }
    }
    
    /**
     * 资产卡片(关联 assets 域): ASSET_ID
     */
    public final java.lang.Long getAssetId(){
         onPropGet(PROP_ID_assetId);
         return _assetId;
    }

    /**
     * 资产卡片(关联 assets 域): ASSET_ID
     */
    public final void setAssetId(java.lang.Long value){
        if(onPropSet(PROP_ID_assetId,value)){
            this._assetId = value;
            internalClearRefs(PROP_ID_assetId);
            
        }
    }
    
    /**
     * 关联工作中心(关联 mfg 域): WORKCENTER_ID
     */
    public final java.lang.Long getWorkcenterId(){
         onPropGet(PROP_ID_workcenterId);
         return _workcenterId;
    }

    /**
     * 关联工作中心(关联 mfg 域): WORKCENTER_ID
     */
    public final void setWorkcenterId(java.lang.Long value){
        if(onPropSet(PROP_ID_workcenterId,value)){
            this._workcenterId = value;
            internalClearRefs(PROP_ID_workcenterId);
            
        }
    }
    
    /**
     * 位置ID: LOCATION_ID
     */
    public final java.lang.Long getLocationId(){
         onPropGet(PROP_ID_locationId);
         return _locationId;
    }

    /**
     * 位置ID: LOCATION_ID
     */
    public final void setLocationId(java.lang.Long value){
        if(onPropSet(PROP_ID_locationId,value)){
            this._locationId = value;
            internalClearRefs(PROP_ID_locationId);
            
        }
    }
    
    /**
     * 分类ID: CATEGORY_ID
     */
    public final java.lang.Long getCategoryId(){
         onPropGet(PROP_ID_categoryId);
         return _categoryId;
    }

    /**
     * 分类ID: CATEGORY_ID
     */
    public final void setCategoryId(java.lang.Long value){
        if(onPropSet(PROP_ID_categoryId,value)){
            this._categoryId = value;
            internalClearRefs(PROP_ID_categoryId);
            
        }
    }
    
    /**
     * 状态: STATUS
     */
    public final java.lang.Integer getStatus(){
         onPropGet(PROP_ID_status);
         return _status;
    }

    /**
     * 状态: STATUS
     */
    public final void setStatus(java.lang.Integer value){
        if(onPropSet(PROP_ID_status,value)){
            this._status = value;
            internalClearRefs(PROP_ID_status);
            
        }
    }
    
    /**
     * 序列号: SERIAL_NO
     */
    public final java.lang.String getSerialNo(){
         onPropGet(PROP_ID_serialNo);
         return _serialNo;
    }

    /**
     * 序列号: SERIAL_NO
     */
    public final void setSerialNo(java.lang.String value){
        if(onPropSet(PROP_ID_serialNo,value)){
            this._serialNo = value;
            internalClearRefs(PROP_ID_serialNo);
            
        }
    }
    
    /**
     * 制造商: MANUFACTURER
     */
    public final java.lang.String getManufacturer(){
         onPropGet(PROP_ID_manufacturer);
         return _manufacturer;
    }

    /**
     * 制造商: MANUFACTURER
     */
    public final void setManufacturer(java.lang.String value){
        if(onPropSet(PROP_ID_manufacturer,value)){
            this._manufacturer = value;
            internalClearRefs(PROP_ID_manufacturer);
            
        }
    }
    
    /**
     * 型号: MODEL
     */
    public final java.lang.String getModel(){
         onPropGet(PROP_ID_model);
         return _model;
    }

    /**
     * 型号: MODEL
     */
    public final void setModel(java.lang.String value){
        if(onPropSet(PROP_ID_model,value)){
            this._model = value;
            internalClearRefs(PROP_ID_model);
            
        }
    }
    
    /**
     * 安装日期: INSTALL_DATE
     */
    public final java.time.LocalDate getInstallDate(){
         onPropGet(PROP_ID_installDate);
         return _installDate;
    }

    /**
     * 安装日期: INSTALL_DATE
     */
    public final void setInstallDate(java.time.LocalDate value){
        if(onPropSet(PROP_ID_installDate,value)){
            this._installDate = value;
            internalClearRefs(PROP_ID_installDate);
            
        }
    }
    
    /**
     * 保修到期: WARRANTY_EXPIRY
     */
    public final java.time.LocalDate getWarrantyExpiry(){
         onPropGet(PROP_ID_warrantyExpiry);
         return _warrantyExpiry;
    }

    /**
     * 保修到期: WARRANTY_EXPIRY
     */
    public final void setWarrantyExpiry(java.time.LocalDate value){
        if(onPropSet(PROP_ID_warrantyExpiry,value)){
            this._warrantyExpiry = value;
            internalClearRefs(PROP_ID_warrantyExpiry);
            
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
    public final app.erp.md.dao.entity.ErpMdLocation getLocation(){
       return (app.erp.md.dao.entity.ErpMdLocation)internalGetRefEntity(PROP_NAME_location);
    }

    public final void setLocation(app.erp.md.dao.entity.ErpMdLocation refEntity){
   
           if(refEntity == null){
           
                   this.setLocationId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_location, refEntity,()->{
           
                           this.setLocationId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.mnt.dao.entity.ErpMntEquipmentCategory getCategory(){
       return (app.erp.mnt.dao.entity.ErpMntEquipmentCategory)internalGetRefEntity(PROP_NAME_category);
    }

    public final void setCategory(app.erp.mnt.dao.entity.ErpMntEquipmentCategory refEntity){
   
           if(refEntity == null){
           
                   this.setCategoryId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_category, refEntity,()->{
           
                           this.setCategoryId(refEntity.getId());
                       
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
       
    private final OrmEntitySet<app.erp.mnt.dao.entity.ErpMntVisit> _visits = new OrmEntitySet<>(this, PROP_NAME_visits,
        null, null,app.erp.mnt.dao.entity.ErpMntVisit.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mnt.dao.entity.ErpMntVisit> getVisits(){
       return _visits;
    }
       
    private final OrmEntitySet<app.erp.mnt.dao.entity.ErpMntSchedule> _schedules = new OrmEntitySet<>(this, PROP_NAME_schedules,
        null, null,app.erp.mnt.dao.entity.ErpMntSchedule.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mnt.dao.entity.ErpMntSchedule> getSchedules(){
       return _schedules;
    }
       
    private final OrmEntitySet<app.erp.mnt.dao.entity.ErpMntRequest> _requests = new OrmEntitySet<>(this, PROP_NAME_requests,
        null, null,app.erp.mnt.dao.entity.ErpMntRequest.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mnt.dao.entity.ErpMntRequest> getRequests(){
       return _requests;
    }
       
    private final OrmEntitySet<app.erp.mnt.dao.entity.ErpMntSparePartUsage> _sparePartUsages = new OrmEntitySet<>(this, PROP_NAME_sparePartUsages,
        null, null,app.erp.mnt.dao.entity.ErpMntSparePartUsage.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mnt.dao.entity.ErpMntSparePartUsage> getSparePartUsages(){
       return _sparePartUsages;
    }
       
    private final OrmEntitySet<app.erp.mnt.dao.entity.ErpMntDowntimeEntry> _downtimeEntries = new OrmEntitySet<>(this, PROP_NAME_downtimeEntries,
        null, null,app.erp.mnt.dao.entity.ErpMntDowntimeEntry.class);

    /**
     * 。 refPropName: , keyProp: {rel.keyProp}
     */
    public final IOrmEntitySet<app.erp.mnt.dao.entity.ErpMntDowntimeEntry> getDowntimeEntries(){
       return _downtimeEntries;
    }
       
}
// resume CPD analysis - CPD-ON
