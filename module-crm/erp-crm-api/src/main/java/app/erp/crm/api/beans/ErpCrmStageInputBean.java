//__XGEN_FORCE_OVERRIDE__
    package app.erp.crm.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpCrmStageInputBean extends CrudInputBase {

    
        private Long _id;

    
        @PropMeta(propId=1)
    
        public Long getId(){
            return _id;
        }

        public void setId(Long value){
            this._id = value;
        }


        private String _code;

    
        @PropMeta(propId=2)
    
        public String getCode(){
            return _code;
        }

        public void setCode(String value){
            this._code = value;
        }


        private Long _orgId;

    
        @PropMeta(propId=3)
    
        public Long getOrgId(){
            return _orgId;
        }

        public void setOrgId(Long value){
            this._orgId = value;
        }


        private String _stageName;

    
        @PropMeta(propId=4)
    
        public String getStageName(){
            return _stageName;
        }

        public void setStageName(String value){
            this._stageName = value;
        }


        private Integer _sequence;

    
        @PropMeta(propId=5)
    
        public Integer getSequence(){
            return _sequence;
        }

        public void setSequence(Integer value){
            this._sequence = value;
        }


        private Long _teamId;

    
        @PropMeta(propId=6)
    
        public Long getTeamId(){
            return _teamId;
        }

        public void setTeamId(Long value){
            this._teamId = value;
        }


        private Integer _defaultProbability;

    
        @PropMeta(propId=7)
    
        public Integer getDefaultProbability(){
            return _defaultProbability;
        }

        public void setDefaultProbability(Integer value){
            this._defaultProbability = value;
        }


        private Boolean _isWonStage;

    
        @PropMeta(propId=8)
    
        public Boolean getIsWonStage(){
            return _isWonStage;
        }

        public void setIsWonStage(Boolean value){
            this._isWonStage = value;
        }


        private String _remark;

    
        @PropMeta(propId=9)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
