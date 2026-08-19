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

import app.erp.drp.dao.entity.ErpInvDrpSupplierScore;

// tell cpd to start ignoring code - CPD-OFF
/**
 *  供应商可靠性评分: erp_inv_drp_supplier_score
 */
@SuppressWarnings({"PMD.UselessOverridingMethod","PMD.UnusedLocalVariable","java:S3008","java:S1602","java:S1128","java:S1161",
        "PMD.UnnecessaryFullyQualifiedName","PMD.EmptyControlStatement","java:S116","java:S115","java:S101","java:S3776"})
public class _ErpInvDrpSupplierScore extends DynamicOrmEntity{
    
    /* ID: ID BIGINT */
    public static final String PROP_NAME_id = "id";
    public static final int PROP_ID_id = 1;
    
    /* 业务组织: ORG_ID BIGINT */
    public static final String PROP_NAME_orgId = "orgId";
    public static final int PROP_ID_orgId = 2;
    
    /* 供应商: SUPPLIER_ID BIGINT */
    public static final String PROP_NAME_supplierId = "supplierId";
    public static final int PROP_ID_supplierId = 3;
    
    /* 物料: MATERIAL_ID BIGINT */
    public static final String PROP_NAME_materialId = "materialId";
    public static final int PROP_ID_materialId = 4;
    
    /* 样本数: SAMPLE_COUNT INTEGER */
    public static final String PROP_NAME_sampleCount = "sampleCount";
    public static final int PROP_ID_sampleCount = 5;
    
    /* 平均提前期(天): AVG_LEAD_TIME DECIMAL */
    public static final String PROP_NAME_avgLeadTime = "avgLeadTime";
    public static final int PROP_ID_avgLeadTime = 6;
    
    /* 提前期标准差(天): LEAD_TIME_STD_DEV DECIMAL */
    public static final String PROP_NAME_leadTimeStdDev = "leadTimeStdDev";
    public static final int PROP_ID_leadTimeStdDev = 7;
    
    /* 准时率: ON_TIME_RATE DECIMAL */
    public static final String PROP_NAME_onTimeRate = "onTimeRate";
    public static final int PROP_ID_onTimeRate = 8;
    
    /* 变异系数(σ/μ): VARIATION_COEFFICIENT DECIMAL */
    public static final String PROP_NAME_variationCoefficient = "variationCoefficient";
    public static final int PROP_ID_variationCoefficient = 9;
    
    /* 数量准确率: QUANTITY_ACCURACY DECIMAL */
    public static final String PROP_NAME_quantityAccuracy = "quantityAccuracy";
    public static final int PROP_ID_quantityAccuracy = 10;
    
    /* 质量合格率: QUALITY_PASS_RATE DECIMAL */
    public static final String PROP_NAME_qualityPassRate = "qualityPassRate";
    public static final int PROP_ID_qualityPassRate = 11;
    
    /* 准时率得分(满分40): ON_TIME_SCORE DECIMAL */
    public static final String PROP_NAME_onTimeScore = "onTimeScore";
    public static final int PROP_ID_onTimeScore = 12;
    
    /* 稳定性得分(满分30): STABILITY_SCORE DECIMAL */
    public static final String PROP_NAME_stabilityScore = "stabilityScore";
    public static final int PROP_ID_stabilityScore = 13;
    
    /* 数量准确率得分(满分20): QUANTITY_SCORE DECIMAL */
    public static final String PROP_NAME_quantityScore = "quantityScore";
    public static final int PROP_ID_quantityScore = 14;
    
    /* 质量合格率得分(满分10): QUALITY_SCORE DECIMAL */
    public static final String PROP_NAME_qualityScore = "qualityScore";
    public static final int PROP_ID_qualityScore = 15;
    
    /* 总分(满分100): TOTAL_SCORE DECIMAL */
    public static final String PROP_NAME_totalScore = "totalScore";
    public static final int PROP_ID_totalScore = 16;
    
    /* 评分等级: GRADE VARCHAR */
    public static final String PROP_NAME_grade = "grade";
    public static final int PROP_ID_grade = 17;
    
    /* 样本缺失标注: MISSING_DIMENSIONS VARCHAR */
    public static final String PROP_NAME_missingDimensions = "missingDimensions";
    public static final int PROP_ID_missingDimensions = 18;
    
    /* 统计窗口起: WINDOW_FROM DATE */
    public static final String PROP_NAME_windowFrom = "windowFrom";
    public static final int PROP_ID_windowFrom = 19;
    
    /* 统计窗口止: WINDOW_TO DATE */
    public static final String PROP_NAME_windowTo = "windowTo";
    public static final int PROP_ID_windowTo = 20;
    
    /* 计算时间: LAST_CALCULATED_AT TIMESTAMP */
    public static final String PROP_NAME_lastCalculatedAt = "lastCalculatedAt";
    public static final int PROP_ID_lastCalculatedAt = 21;
    
    /* 备注: REMARK VARCHAR */
    public static final String PROP_NAME_remark = "remark";
    public static final int PROP_ID_remark = 22;
    
    /* 逻辑删除版本: DEL_VERSION BIGINT */
    public static final String PROP_NAME_delVersion = "delVersion";
    public static final int PROP_ID_delVersion = 23;
    
    /* 数据版本: VERSION INTEGER */
    public static final String PROP_NAME_version = "version";
    public static final int PROP_ID_version = 24;
    
    /* 创建人: CREATED_BY VARCHAR */
    public static final String PROP_NAME_createdBy = "createdBy";
    public static final int PROP_ID_createdBy = 25;
    
    /* 创建时间: CREATE_TIME TIMESTAMP */
    public static final String PROP_NAME_createTime = "createTime";
    public static final int PROP_ID_createTime = 26;
    
    /* 修改人: UPDATED_BY VARCHAR */
    public static final String PROP_NAME_updatedBy = "updatedBy";
    public static final int PROP_ID_updatedBy = 27;
    
    /* 修改时间: UPDATE_TIME TIMESTAMP */
    public static final String PROP_NAME_updateTime = "updateTime";
    public static final int PROP_ID_updateTime = 28;
    

    private static int _PROP_ID_BOUND = 29;

    
    /* relation:  */
    public static final String PROP_NAME_supplier = "supplier";
    
    /* relation:  */
    public static final String PROP_NAME_material = "material";
    
    /* relation:  */
    public static final String PROP_NAME_org = "org";
    

    protected static final List<String> PK_PROP_NAMES = Arrays.asList(PROP_NAME_id);
    protected static final int[] PK_PROP_IDS = new int[]{PROP_ID_id};

    private static final String[] PROP_ID_TO_NAME = new String[29];
    private static final Map<String,Integer> PROP_NAME_TO_ID = new HashMap<>();
    static{
      
          PROP_ID_TO_NAME[PROP_ID_id] = PROP_NAME_id;
          PROP_NAME_TO_ID.put(PROP_NAME_id, PROP_ID_id);
      
          PROP_ID_TO_NAME[PROP_ID_orgId] = PROP_NAME_orgId;
          PROP_NAME_TO_ID.put(PROP_NAME_orgId, PROP_ID_orgId);
      
          PROP_ID_TO_NAME[PROP_ID_supplierId] = PROP_NAME_supplierId;
          PROP_NAME_TO_ID.put(PROP_NAME_supplierId, PROP_ID_supplierId);
      
          PROP_ID_TO_NAME[PROP_ID_materialId] = PROP_NAME_materialId;
          PROP_NAME_TO_ID.put(PROP_NAME_materialId, PROP_ID_materialId);
      
          PROP_ID_TO_NAME[PROP_ID_sampleCount] = PROP_NAME_sampleCount;
          PROP_NAME_TO_ID.put(PROP_NAME_sampleCount, PROP_ID_sampleCount);
      
          PROP_ID_TO_NAME[PROP_ID_avgLeadTime] = PROP_NAME_avgLeadTime;
          PROP_NAME_TO_ID.put(PROP_NAME_avgLeadTime, PROP_ID_avgLeadTime);
      
          PROP_ID_TO_NAME[PROP_ID_leadTimeStdDev] = PROP_NAME_leadTimeStdDev;
          PROP_NAME_TO_ID.put(PROP_NAME_leadTimeStdDev, PROP_ID_leadTimeStdDev);
      
          PROP_ID_TO_NAME[PROP_ID_onTimeRate] = PROP_NAME_onTimeRate;
          PROP_NAME_TO_ID.put(PROP_NAME_onTimeRate, PROP_ID_onTimeRate);
      
          PROP_ID_TO_NAME[PROP_ID_variationCoefficient] = PROP_NAME_variationCoefficient;
          PROP_NAME_TO_ID.put(PROP_NAME_variationCoefficient, PROP_ID_variationCoefficient);
      
          PROP_ID_TO_NAME[PROP_ID_quantityAccuracy] = PROP_NAME_quantityAccuracy;
          PROP_NAME_TO_ID.put(PROP_NAME_quantityAccuracy, PROP_ID_quantityAccuracy);
      
          PROP_ID_TO_NAME[PROP_ID_qualityPassRate] = PROP_NAME_qualityPassRate;
          PROP_NAME_TO_ID.put(PROP_NAME_qualityPassRate, PROP_ID_qualityPassRate);
      
          PROP_ID_TO_NAME[PROP_ID_onTimeScore] = PROP_NAME_onTimeScore;
          PROP_NAME_TO_ID.put(PROP_NAME_onTimeScore, PROP_ID_onTimeScore);
      
          PROP_ID_TO_NAME[PROP_ID_stabilityScore] = PROP_NAME_stabilityScore;
          PROP_NAME_TO_ID.put(PROP_NAME_stabilityScore, PROP_ID_stabilityScore);
      
          PROP_ID_TO_NAME[PROP_ID_quantityScore] = PROP_NAME_quantityScore;
          PROP_NAME_TO_ID.put(PROP_NAME_quantityScore, PROP_ID_quantityScore);
      
          PROP_ID_TO_NAME[PROP_ID_qualityScore] = PROP_NAME_qualityScore;
          PROP_NAME_TO_ID.put(PROP_NAME_qualityScore, PROP_ID_qualityScore);
      
          PROP_ID_TO_NAME[PROP_ID_totalScore] = PROP_NAME_totalScore;
          PROP_NAME_TO_ID.put(PROP_NAME_totalScore, PROP_ID_totalScore);
      
          PROP_ID_TO_NAME[PROP_ID_grade] = PROP_NAME_grade;
          PROP_NAME_TO_ID.put(PROP_NAME_grade, PROP_ID_grade);
      
          PROP_ID_TO_NAME[PROP_ID_missingDimensions] = PROP_NAME_missingDimensions;
          PROP_NAME_TO_ID.put(PROP_NAME_missingDimensions, PROP_ID_missingDimensions);
      
          PROP_ID_TO_NAME[PROP_ID_windowFrom] = PROP_NAME_windowFrom;
          PROP_NAME_TO_ID.put(PROP_NAME_windowFrom, PROP_ID_windowFrom);
      
          PROP_ID_TO_NAME[PROP_ID_windowTo] = PROP_NAME_windowTo;
          PROP_NAME_TO_ID.put(PROP_NAME_windowTo, PROP_ID_windowTo);
      
          PROP_ID_TO_NAME[PROP_ID_lastCalculatedAt] = PROP_NAME_lastCalculatedAt;
          PROP_NAME_TO_ID.put(PROP_NAME_lastCalculatedAt, PROP_ID_lastCalculatedAt);
      
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
    
    /* 供应商: SUPPLIER_ID */
    private java.lang.Long _supplierId;
    
    /* 物料: MATERIAL_ID */
    private java.lang.Long _materialId;
    
    /* 样本数: SAMPLE_COUNT */
    private java.lang.Integer _sampleCount;
    
    /* 平均提前期(天): AVG_LEAD_TIME */
    private java.math.BigDecimal _avgLeadTime;
    
    /* 提前期标准差(天): LEAD_TIME_STD_DEV */
    private java.math.BigDecimal _leadTimeStdDev;
    
    /* 准时率: ON_TIME_RATE */
    private java.math.BigDecimal _onTimeRate;
    
    /* 变异系数(σ/μ): VARIATION_COEFFICIENT */
    private java.math.BigDecimal _variationCoefficient;
    
    /* 数量准确率: QUANTITY_ACCURACY */
    private java.math.BigDecimal _quantityAccuracy;
    
    /* 质量合格率: QUALITY_PASS_RATE */
    private java.math.BigDecimal _qualityPassRate;
    
    /* 准时率得分(满分40): ON_TIME_SCORE */
    private java.math.BigDecimal _onTimeScore;
    
    /* 稳定性得分(满分30): STABILITY_SCORE */
    private java.math.BigDecimal _stabilityScore;
    
    /* 数量准确率得分(满分20): QUANTITY_SCORE */
    private java.math.BigDecimal _quantityScore;
    
    /* 质量合格率得分(满分10): QUALITY_SCORE */
    private java.math.BigDecimal _qualityScore;
    
    /* 总分(满分100): TOTAL_SCORE */
    private java.math.BigDecimal _totalScore;
    
    /* 评分等级: GRADE */
    private java.lang.String _grade;
    
    /* 样本缺失标注: MISSING_DIMENSIONS */
    private java.lang.String _missingDimensions;
    
    /* 统计窗口起: WINDOW_FROM */
    private java.time.LocalDate _windowFrom;
    
    /* 统计窗口止: WINDOW_TO */
    private java.time.LocalDate _windowTo;
    
    /* 计算时间: LAST_CALCULATED_AT */
    private java.sql.Timestamp _lastCalculatedAt;
    
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
    

    public _ErpInvDrpSupplierScore(){
        // for debug
    }

    protected ErpInvDrpSupplierScore newInstance(){
        ErpInvDrpSupplierScore entity = new ErpInvDrpSupplierScore();
        entity.orm_attach(orm_enhancer());
        entity.orm_entityModel(orm_entityModel());
        return entity;
    }

    @Override
    public ErpInvDrpSupplierScore cloneInstance() {
        ErpInvDrpSupplierScore entity = newInstance();
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
      return "app.erp.drp.dao.entity.ErpInvDrpSupplierScore";
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
        
            case PROP_ID_supplierId:
               return getSupplierId();
        
            case PROP_ID_materialId:
               return getMaterialId();
        
            case PROP_ID_sampleCount:
               return getSampleCount();
        
            case PROP_ID_avgLeadTime:
               return getAvgLeadTime();
        
            case PROP_ID_leadTimeStdDev:
               return getLeadTimeStdDev();
        
            case PROP_ID_onTimeRate:
               return getOnTimeRate();
        
            case PROP_ID_variationCoefficient:
               return getVariationCoefficient();
        
            case PROP_ID_quantityAccuracy:
               return getQuantityAccuracy();
        
            case PROP_ID_qualityPassRate:
               return getQualityPassRate();
        
            case PROP_ID_onTimeScore:
               return getOnTimeScore();
        
            case PROP_ID_stabilityScore:
               return getStabilityScore();
        
            case PROP_ID_quantityScore:
               return getQuantityScore();
        
            case PROP_ID_qualityScore:
               return getQualityScore();
        
            case PROP_ID_totalScore:
               return getTotalScore();
        
            case PROP_ID_grade:
               return getGrade();
        
            case PROP_ID_missingDimensions:
               return getMissingDimensions();
        
            case PROP_ID_windowFrom:
               return getWindowFrom();
        
            case PROP_ID_windowTo:
               return getWindowTo();
        
            case PROP_ID_lastCalculatedAt:
               return getLastCalculatedAt();
        
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
        
            case PROP_ID_supplierId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_supplierId));
               }
               setSupplierId(typedValue);
               break;
            }
        
            case PROP_ID_materialId:{
               java.lang.Long typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLong(value,
                       err-> newTypeConversionError(PROP_NAME_materialId));
               }
               setMaterialId(typedValue);
               break;
            }
        
            case PROP_ID_sampleCount:{
               java.lang.Integer typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toInteger(value,
                       err-> newTypeConversionError(PROP_NAME_sampleCount));
               }
               setSampleCount(typedValue);
               break;
            }
        
            case PROP_ID_avgLeadTime:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_avgLeadTime));
               }
               setAvgLeadTime(typedValue);
               break;
            }
        
            case PROP_ID_leadTimeStdDev:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_leadTimeStdDev));
               }
               setLeadTimeStdDev(typedValue);
               break;
            }
        
            case PROP_ID_onTimeRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_onTimeRate));
               }
               setOnTimeRate(typedValue);
               break;
            }
        
            case PROP_ID_variationCoefficient:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_variationCoefficient));
               }
               setVariationCoefficient(typedValue);
               break;
            }
        
            case PROP_ID_quantityAccuracy:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_quantityAccuracy));
               }
               setQuantityAccuracy(typedValue);
               break;
            }
        
            case PROP_ID_qualityPassRate:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_qualityPassRate));
               }
               setQualityPassRate(typedValue);
               break;
            }
        
            case PROP_ID_onTimeScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_onTimeScore));
               }
               setOnTimeScore(typedValue);
               break;
            }
        
            case PROP_ID_stabilityScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_stabilityScore));
               }
               setStabilityScore(typedValue);
               break;
            }
        
            case PROP_ID_quantityScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_quantityScore));
               }
               setQuantityScore(typedValue);
               break;
            }
        
            case PROP_ID_qualityScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_qualityScore));
               }
               setQualityScore(typedValue);
               break;
            }
        
            case PROP_ID_totalScore:{
               java.math.BigDecimal typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toBigDecimal(value,
                       err-> newTypeConversionError(PROP_NAME_totalScore));
               }
               setTotalScore(typedValue);
               break;
            }
        
            case PROP_ID_grade:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_grade));
               }
               setGrade(typedValue);
               break;
            }
        
            case PROP_ID_missingDimensions:{
               java.lang.String typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toString(value,
                       err-> newTypeConversionError(PROP_NAME_missingDimensions));
               }
               setMissingDimensions(typedValue);
               break;
            }
        
            case PROP_ID_windowFrom:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_windowFrom));
               }
               setWindowFrom(typedValue);
               break;
            }
        
            case PROP_ID_windowTo:{
               java.time.LocalDate typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toLocalDate(value,
                       err-> newTypeConversionError(PROP_NAME_windowTo));
               }
               setWindowTo(typedValue);
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               java.sql.Timestamp typedValue = null;
               if(value != null){
                   typedValue = ConvertHelper.toTimestamp(value,
                       err-> newTypeConversionError(PROP_NAME_lastCalculatedAt));
               }
               setLastCalculatedAt(typedValue);
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
        
            case PROP_ID_supplierId:{
               onInitProp(propId);
               this._supplierId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_materialId:{
               onInitProp(propId);
               this._materialId = (java.lang.Long)value;
               
               break;
            }
        
            case PROP_ID_sampleCount:{
               onInitProp(propId);
               this._sampleCount = (java.lang.Integer)value;
               
               break;
            }
        
            case PROP_ID_avgLeadTime:{
               onInitProp(propId);
               this._avgLeadTime = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_leadTimeStdDev:{
               onInitProp(propId);
               this._leadTimeStdDev = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_onTimeRate:{
               onInitProp(propId);
               this._onTimeRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_variationCoefficient:{
               onInitProp(propId);
               this._variationCoefficient = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_quantityAccuracy:{
               onInitProp(propId);
               this._quantityAccuracy = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_qualityPassRate:{
               onInitProp(propId);
               this._qualityPassRate = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_onTimeScore:{
               onInitProp(propId);
               this._onTimeScore = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_stabilityScore:{
               onInitProp(propId);
               this._stabilityScore = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_quantityScore:{
               onInitProp(propId);
               this._quantityScore = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_qualityScore:{
               onInitProp(propId);
               this._qualityScore = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_totalScore:{
               onInitProp(propId);
               this._totalScore = (java.math.BigDecimal)value;
               
               break;
            }
        
            case PROP_ID_grade:{
               onInitProp(propId);
               this._grade = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_missingDimensions:{
               onInitProp(propId);
               this._missingDimensions = (java.lang.String)value;
               
               break;
            }
        
            case PROP_ID_windowFrom:{
               onInitProp(propId);
               this._windowFrom = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_windowTo:{
               onInitProp(propId);
               this._windowTo = (java.time.LocalDate)value;
               
               break;
            }
        
            case PROP_ID_lastCalculatedAt:{
               onInitProp(propId);
               this._lastCalculatedAt = (java.sql.Timestamp)value;
               
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
     * 供应商: SUPPLIER_ID
     */
    public final java.lang.Long getSupplierId(){
         onPropGet(PROP_ID_supplierId);
         return _supplierId;
    }

    /**
     * 供应商: SUPPLIER_ID
     */
    public final void setSupplierId(java.lang.Long value){
        if(onPropSet(PROP_ID_supplierId,value)){
            this._supplierId = value;
            internalClearRefs(PROP_ID_supplierId);
            
        }
    }
    
    /**
     * 物料: MATERIAL_ID
     */
    public final java.lang.Long getMaterialId(){
         onPropGet(PROP_ID_materialId);
         return _materialId;
    }

    /**
     * 物料: MATERIAL_ID
     */
    public final void setMaterialId(java.lang.Long value){
        if(onPropSet(PROP_ID_materialId,value)){
            this._materialId = value;
            internalClearRefs(PROP_ID_materialId);
            
        }
    }
    
    /**
     * 样本数: SAMPLE_COUNT
     */
    public final java.lang.Integer getSampleCount(){
         onPropGet(PROP_ID_sampleCount);
         return _sampleCount;
    }

    /**
     * 样本数: SAMPLE_COUNT
     */
    public final void setSampleCount(java.lang.Integer value){
        if(onPropSet(PROP_ID_sampleCount,value)){
            this._sampleCount = value;
            internalClearRefs(PROP_ID_sampleCount);
            
        }
    }
    
    /**
     * 平均提前期(天): AVG_LEAD_TIME
     */
    public final java.math.BigDecimal getAvgLeadTime(){
         onPropGet(PROP_ID_avgLeadTime);
         return _avgLeadTime;
    }

    /**
     * 平均提前期(天): AVG_LEAD_TIME
     */
    public final void setAvgLeadTime(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_avgLeadTime,value)){
            this._avgLeadTime = value;
            internalClearRefs(PROP_ID_avgLeadTime);
            
        }
    }
    
    /**
     * 提前期标准差(天): LEAD_TIME_STD_DEV
     */
    public final java.math.BigDecimal getLeadTimeStdDev(){
         onPropGet(PROP_ID_leadTimeStdDev);
         return _leadTimeStdDev;
    }

    /**
     * 提前期标准差(天): LEAD_TIME_STD_DEV
     */
    public final void setLeadTimeStdDev(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_leadTimeStdDev,value)){
            this._leadTimeStdDev = value;
            internalClearRefs(PROP_ID_leadTimeStdDev);
            
        }
    }
    
    /**
     * 准时率: ON_TIME_RATE
     */
    public final java.math.BigDecimal getOnTimeRate(){
         onPropGet(PROP_ID_onTimeRate);
         return _onTimeRate;
    }

    /**
     * 准时率: ON_TIME_RATE
     */
    public final void setOnTimeRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_onTimeRate,value)){
            this._onTimeRate = value;
            internalClearRefs(PROP_ID_onTimeRate);
            
        }
    }
    
    /**
     * 变异系数(σ/μ): VARIATION_COEFFICIENT
     */
    public final java.math.BigDecimal getVariationCoefficient(){
         onPropGet(PROP_ID_variationCoefficient);
         return _variationCoefficient;
    }

    /**
     * 变异系数(σ/μ): VARIATION_COEFFICIENT
     */
    public final void setVariationCoefficient(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_variationCoefficient,value)){
            this._variationCoefficient = value;
            internalClearRefs(PROP_ID_variationCoefficient);
            
        }
    }
    
    /**
     * 数量准确率: QUANTITY_ACCURACY
     */
    public final java.math.BigDecimal getQuantityAccuracy(){
         onPropGet(PROP_ID_quantityAccuracy);
         return _quantityAccuracy;
    }

    /**
     * 数量准确率: QUANTITY_ACCURACY
     */
    public final void setQuantityAccuracy(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_quantityAccuracy,value)){
            this._quantityAccuracy = value;
            internalClearRefs(PROP_ID_quantityAccuracy);
            
        }
    }
    
    /**
     * 质量合格率: QUALITY_PASS_RATE
     */
    public final java.math.BigDecimal getQualityPassRate(){
         onPropGet(PROP_ID_qualityPassRate);
         return _qualityPassRate;
    }

    /**
     * 质量合格率: QUALITY_PASS_RATE
     */
    public final void setQualityPassRate(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_qualityPassRate,value)){
            this._qualityPassRate = value;
            internalClearRefs(PROP_ID_qualityPassRate);
            
        }
    }
    
    /**
     * 准时率得分(满分40): ON_TIME_SCORE
     */
    public final java.math.BigDecimal getOnTimeScore(){
         onPropGet(PROP_ID_onTimeScore);
         return _onTimeScore;
    }

    /**
     * 准时率得分(满分40): ON_TIME_SCORE
     */
    public final void setOnTimeScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_onTimeScore,value)){
            this._onTimeScore = value;
            internalClearRefs(PROP_ID_onTimeScore);
            
        }
    }
    
    /**
     * 稳定性得分(满分30): STABILITY_SCORE
     */
    public final java.math.BigDecimal getStabilityScore(){
         onPropGet(PROP_ID_stabilityScore);
         return _stabilityScore;
    }

    /**
     * 稳定性得分(满分30): STABILITY_SCORE
     */
    public final void setStabilityScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_stabilityScore,value)){
            this._stabilityScore = value;
            internalClearRefs(PROP_ID_stabilityScore);
            
        }
    }
    
    /**
     * 数量准确率得分(满分20): QUANTITY_SCORE
     */
    public final java.math.BigDecimal getQuantityScore(){
         onPropGet(PROP_ID_quantityScore);
         return _quantityScore;
    }

    /**
     * 数量准确率得分(满分20): QUANTITY_SCORE
     */
    public final void setQuantityScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_quantityScore,value)){
            this._quantityScore = value;
            internalClearRefs(PROP_ID_quantityScore);
            
        }
    }
    
    /**
     * 质量合格率得分(满分10): QUALITY_SCORE
     */
    public final java.math.BigDecimal getQualityScore(){
         onPropGet(PROP_ID_qualityScore);
         return _qualityScore;
    }

    /**
     * 质量合格率得分(满分10): QUALITY_SCORE
     */
    public final void setQualityScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_qualityScore,value)){
            this._qualityScore = value;
            internalClearRefs(PROP_ID_qualityScore);
            
        }
    }
    
    /**
     * 总分(满分100): TOTAL_SCORE
     */
    public final java.math.BigDecimal getTotalScore(){
         onPropGet(PROP_ID_totalScore);
         return _totalScore;
    }

    /**
     * 总分(满分100): TOTAL_SCORE
     */
    public final void setTotalScore(java.math.BigDecimal value){
        if(onPropSet(PROP_ID_totalScore,value)){
            this._totalScore = value;
            internalClearRefs(PROP_ID_totalScore);
            
        }
    }
    
    /**
     * 评分等级: GRADE
     */
    public final java.lang.String getGrade(){
         onPropGet(PROP_ID_grade);
         return _grade;
    }

    /**
     * 评分等级: GRADE
     */
    public final void setGrade(java.lang.String value){
        if(onPropSet(PROP_ID_grade,value)){
            this._grade = value;
            internalClearRefs(PROP_ID_grade);
            
        }
    }
    
    /**
     * 样本缺失标注: MISSING_DIMENSIONS
     */
    public final java.lang.String getMissingDimensions(){
         onPropGet(PROP_ID_missingDimensions);
         return _missingDimensions;
    }

    /**
     * 样本缺失标注: MISSING_DIMENSIONS
     */
    public final void setMissingDimensions(java.lang.String value){
        if(onPropSet(PROP_ID_missingDimensions,value)){
            this._missingDimensions = value;
            internalClearRefs(PROP_ID_missingDimensions);
            
        }
    }
    
    /**
     * 统计窗口起: WINDOW_FROM
     */
    public final java.time.LocalDate getWindowFrom(){
         onPropGet(PROP_ID_windowFrom);
         return _windowFrom;
    }

    /**
     * 统计窗口起: WINDOW_FROM
     */
    public final void setWindowFrom(java.time.LocalDate value){
        if(onPropSet(PROP_ID_windowFrom,value)){
            this._windowFrom = value;
            internalClearRefs(PROP_ID_windowFrom);
            
        }
    }
    
    /**
     * 统计窗口止: WINDOW_TO
     */
    public final java.time.LocalDate getWindowTo(){
         onPropGet(PROP_ID_windowTo);
         return _windowTo;
    }

    /**
     * 统计窗口止: WINDOW_TO
     */
    public final void setWindowTo(java.time.LocalDate value){
        if(onPropSet(PROP_ID_windowTo,value)){
            this._windowTo = value;
            internalClearRefs(PROP_ID_windowTo);
            
        }
    }
    
    /**
     * 计算时间: LAST_CALCULATED_AT
     */
    public final java.sql.Timestamp getLastCalculatedAt(){
         onPropGet(PROP_ID_lastCalculatedAt);
         return _lastCalculatedAt;
    }

    /**
     * 计算时间: LAST_CALCULATED_AT
     */
    public final void setLastCalculatedAt(java.sql.Timestamp value){
        if(onPropSet(PROP_ID_lastCalculatedAt,value)){
            this._lastCalculatedAt = value;
            internalClearRefs(PROP_ID_lastCalculatedAt);
            
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
    public final app.erp.md.dao.entity.ErpMdPartner getSupplier(){
       return (app.erp.md.dao.entity.ErpMdPartner)internalGetRefEntity(PROP_NAME_supplier);
    }

    public final void setSupplier(app.erp.md.dao.entity.ErpMdPartner refEntity){
   
           if(refEntity == null){
           
                   this.setSupplierId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_supplier, refEntity,()->{
           
                           this.setSupplierId(refEntity.getId());
                       
           });
           }
       
    }
       
    /**
     * 
     */
    public final app.erp.md.dao.entity.ErpMdMaterial getMaterial(){
       return (app.erp.md.dao.entity.ErpMdMaterial)internalGetRefEntity(PROP_NAME_material);
    }

    public final void setMaterial(app.erp.md.dao.entity.ErpMdMaterial refEntity){
   
           if(refEntity == null){
           
                   this.setMaterialId(null);
               
           }else{
           internalSetRefEntity(PROP_NAME_material, refEntity,()->{
           
                           this.setMaterialId(refEntity.getId());
                       
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
