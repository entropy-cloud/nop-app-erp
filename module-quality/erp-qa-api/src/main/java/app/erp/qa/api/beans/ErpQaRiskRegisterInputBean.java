//__XGEN_FORCE_OVERRIDE__
    package app.erp.qa.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpQaRiskRegisterInputBean extends CrudInputBase {

    
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


        private java.time.LocalDate _riskDate;

    
        @PropMeta(propId=3)
    
        public java.time.LocalDate getRiskDate(){
            return _riskDate;
        }

        public void setRiskDate(java.time.LocalDate value){
            this._riskDate = value;
        }


        private String _description;

    
        @PropMeta(propId=4)
    
        public String getDescription(){
            return _description;
        }

        public void setDescription(String value){
            this._description = value;
        }


        private String _category;

    
        @PropMeta(propId=5)
    
        public String getCategory(){
            return _category;
        }

        public void setCategory(String value){
            this._category = value;
        }


        private Integer _likelihood;

    
        @PropMeta(propId=6)
    
        public Integer getLikelihood(){
            return _likelihood;
        }

        public void setLikelihood(Integer value){
            this._likelihood = value;
        }


        private Integer _severity;

    
        @PropMeta(propId=7)
    
        public Integer getSeverity(){
            return _severity;
        }

        public void setSeverity(Integer value){
            this._severity = value;
        }


        private Integer _riskScore;

    
        @PropMeta(propId=8)
    
        public Integer getRiskScore(){
            return _riskScore;
        }

        public void setRiskScore(Integer value){
            this._riskScore = value;
        }


        private String _mitigation;

    
        @PropMeta(propId=9)
    
        public String getMitigation(){
            return _mitigation;
        }

        public void setMitigation(String value){
            this._mitigation = value;
        }


        private String _ownerId;

    
        @PropMeta(propId=10)
    
        public String getOwnerId(){
            return _ownerId;
        }

        public void setOwnerId(String value){
            this._ownerId = value;
        }


        private String _status;

    
        @PropMeta(propId=11)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=12)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
