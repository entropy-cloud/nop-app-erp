//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    
    import java.util.Map;

    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmFunnelStageMetricsOutputBean {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private Long _funnelId;

    
        @PropMeta(propId=2)
    
        public Long getFunnelId(){
            return _funnelId;
        }

        public void setFunnelId(Long value){
            this._funnelId = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private Long _stageId;

    
        @PropMeta(propId=4)
    
        public Long getStageId(){
            return _stageId;
        }

        public void setStageId(Long value){
            this._stageId = value;
        }


        private Integer _stageOrder;

    
        @PropMeta(propId=5)
    
        public Integer getStageOrder(){
            return _stageOrder;
        }

        public void setStageOrder(Integer value){
            this._stageOrder = value;
        }


        private String _stageName;

    
        @PropMeta(propId=6)
    
        public String getStageName(){
            return _stageName;
        }

        public void setStageName(String value){
            this._stageName = value;
        }


        private Integer _leadCountIn;

    
        @PropMeta(propId=7)
    
        public Integer getLeadCountIn(){
            return _leadCountIn;
        }

        public void setLeadCountIn(Integer value){
            this._leadCountIn = value;
        }


        private Integer _leadCountOut;

    
        @PropMeta(propId=8)
    
        public Integer getLeadCountOut(){
            return _leadCountOut;
        }

        public void setLeadCountOut(Integer value){
            this._leadCountOut = value;
        }


        private Integer _leadCountRemaining;

    
        @PropMeta(propId=9)
    
        public Integer getLeadCountRemaining(){
            return _leadCountRemaining;
        }

        public void setLeadCountRemaining(Integer value){
            this._leadCountRemaining = value;
        }


        private java.math.BigDecimal _conversionRate;

    
        @PropMeta(propId=10)
    
        public java.math.BigDecimal getConversionRate(){
            return _conversionRate;
        }

        public void setConversionRate(java.math.BigDecimal value){
            this._conversionRate = value;
        }


        private java.math.BigDecimal _dropOffRate;

    
        @PropMeta(propId=11)
    
        public java.math.BigDecimal getDropOffRate(){
            return _dropOffRate;
        }

        public void setDropOffRate(java.math.BigDecimal value){
            this._dropOffRate = value;
        }


        private java.math.BigDecimal _avgDaysInStage;

    
        @PropMeta(propId=12)
    
        public java.math.BigDecimal getAvgDaysInStage(){
            return _avgDaysInStage;
        }

        public void setAvgDaysInStage(java.math.BigDecimal value){
            this._avgDaysInStage = value;
        }


        private Integer _lostCount;

    
        @PropMeta(propId=13)
    
        public Integer getLostCount(){
            return _lostCount;
        }

        public void setLostCount(Integer value){
            this._lostCount = value;
        }


        private java.math.BigDecimal _lostAmount;

    
        @PropMeta(propId=14)
    
        public java.math.BigDecimal getLostAmount(){
            return _lostAmount;
        }

        public void setLostAmount(java.math.BigDecimal value){
            this._lostAmount = value;
        }


        private String _lostReasonTop;

    
        @PropMeta(propId=15)
    
        public String getLostReasonTop(){
            return _lostReasonTop;
        }

        public void setLostReasonTop(String value){
            this._lostReasonTop = value;
        }


        private String _remark;

    
        @PropMeta(propId=16)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private Long _delVersion;

    
        @PropMeta(propId=17)
    
        public Long getDelVersion(){
            return _delVersion;
        }

        public void setDelVersion(Long value){
            this._delVersion = value;
        }


        private Integer _version;

    
        @PropMeta(propId=18)
    
        public Integer getVersion(){
            return _version;
        }

        public void setVersion(Integer value){
            this._version = value;
        }


        private String _createdBy;

    
        @PropMeta(propId=19)
    
        public String getCreatedBy(){
            return _createdBy;
        }

        public void setCreatedBy(String value){
            this._createdBy = value;
        }


        private java.sql.Timestamp _createTime;

    
        @PropMeta(propId=20)
    
        public java.sql.Timestamp getCreateTime(){
            return _createTime;
        }

        public void setCreateTime(java.sql.Timestamp value){
            this._createTime = value;
        }


        private String _updatedBy;

    
        @PropMeta(propId=21)
    
        public String getUpdatedBy(){
            return _updatedBy;
        }

        public void setUpdatedBy(String value){
            this._updatedBy = value;
        }


        private java.sql.Timestamp _updateTime;

    
        @PropMeta(propId=22)
    
        public java.sql.Timestamp getUpdateTime(){
            return _updateTime;
        }

        public void setUpdateTime(java.sql.Timestamp value){
            this._updateTime = value;
        }


        private Map<String,Object> _funnel;

        public Map<String,Object> getFunnel(){
            return _funnel;
        }

        public void setFunnel(Map<String,Object> value){
            this._funnel = value;
        }


        private Map<String,Object> _stage;

        public Map<String,Object> getStage(){
            return _stage;
        }

        public void setStage(Map<String,Object> value){
            this._stage = value;
        }


        private Map<String,Object> _org;

        public Map<String,Object> getOrg(){
            return _org;
        }

        public void setOrg(Map<String,Object> value){
            this._org = value;
        }


    }
