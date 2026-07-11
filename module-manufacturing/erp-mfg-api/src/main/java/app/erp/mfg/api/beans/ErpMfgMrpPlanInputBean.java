//__XGEN_FORCE_OVERRIDE__
    package app.erp.mfg.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    import java.util.List;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpMfgMrpPlanInputBean extends CrudInputBase {

    
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


        private java.time.LocalDate _businessDate;

    
        @PropMeta(propId=4)
    
        public java.time.LocalDate getBusinessDate(){
            return _businessDate;
        }

        public void setBusinessDate(java.time.LocalDate value){
            this._businessDate = value;
        }


        private Integer _planningHorizonDays;

    
        @PropMeta(propId=5)
    
        public Integer getPlanningHorizonDays(){
            return _planningHorizonDays;
        }

        public void setPlanningHorizonDays(Integer value){
            this._planningHorizonDays = value;
        }


        private String _status;

    
        @PropMeta(propId=6)
    
        public String getStatus(){
            return _status;
        }

        public void setStatus(String value){
            this._status = value;
        }


        private String _remark;

    
        @PropMeta(propId=7)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


        private List<ErpMfgMrpPlanLineInputBean> _lines;

        public List<ErpMfgMrpPlanLineInputBean> getLines(){
            return _lines;
        }

        public void setLines(List<ErpMfgMrpPlanLineInputBean> value){
            this._lines = value;
        }


        private List<ErpMfgMrpDemandInputBean> _demands;

        public List<ErpMfgMrpDemandInputBean> getDemands(){
            return _demands;
        }

        public void setDemands(List<ErpMfgMrpDemandInputBean> value){
            this._demands = value;
        }


    }
