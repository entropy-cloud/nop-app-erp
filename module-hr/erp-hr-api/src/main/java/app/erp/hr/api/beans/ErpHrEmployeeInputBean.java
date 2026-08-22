//__XGEN_FORCE_OVERRIDE__
    package app.erp.hr.api.beans;

    import com.fasterxml.jackson.annotation.JsonInclude;
    import io.nop.api.core.annotations.data.DataBean;
    import io.nop.api.core.annotations.meta.PropMeta;
    import io.nop.api.core.api.CrudInputBase;
    
    @DataBean
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @SuppressWarnings({"PMD","java:S116","java:S115"})
    public class ErpHrEmployeeInputBean extends CrudInputBase {

    
        private String _id;

    
        @PropMeta(propId=1)
    
        public String getId(){
            return _id;
        }

        public void setId(String value){
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


        private String _firstName;

    
        @PropMeta(propId=3)
    
        public String getFirstName(){
            return _firstName;
        }

        public void setFirstName(String value){
            this._firstName = value;
        }


        private String _lastName;

    
        @PropMeta(propId=4)
    
        public String getLastName(){
            return _lastName;
        }

        public void setLastName(String value){
            this._lastName = value;
        }


        private String _fullName;

    
        @PropMeta(propId=5)
    
        public String getFullName(){
            return _fullName;
        }

        public void setFullName(String value){
            this._fullName = value;
        }


        private String _gender;

    
        @PropMeta(propId=6)
    
        public String getGender(){
            return _gender;
        }

        public void setGender(String value){
            this._gender = value;
        }


        private java.time.LocalDate _birthDate;

    
        @PropMeta(propId=7)
    
        public java.time.LocalDate getBirthDate(){
            return _birthDate;
        }

        public void setBirthDate(java.time.LocalDate value){
            this._birthDate = value;
        }


        private String _idCardType;

    
        @PropMeta(propId=8)
    
        public String getIdCardType(){
            return _idCardType;
        }

        public void setIdCardType(String value){
            this._idCardType = value;
        }


        private String _idCardNo;

    
        @PropMeta(propId=9)
    
        public String getIdCardNo(){
            return _idCardNo;
        }

        public void setIdCardNo(String value){
            this._idCardNo = value;
        }


        private String _email;

    
        @PropMeta(propId=10)
    
        public String getEmail(){
            return _email;
        }

        public void setEmail(String value){
            this._email = value;
        }


        private String _mobilePhone;

    
        @PropMeta(propId=11)
    
        public String getMobilePhone(){
            return _mobilePhone;
        }

        public void setMobilePhone(String value){
            this._mobilePhone = value;
        }


        private String _maritalStatus;

    
        @PropMeta(propId=12)
    
        public String getMaritalStatus(){
            return _maritalStatus;
        }

        public void setMaritalStatus(String value){
            this._maritalStatus = value;
        }


        private String _nationality;

    
        @PropMeta(propId=13)
    
        public String getNationality(){
            return _nationality;
        }

        public void setNationality(String value){
            this._nationality = value;
        }


        private String _emergencyContact;

    
        @PropMeta(propId=14)
    
        public String getEmergencyContact(){
            return _emergencyContact;
        }

        public void setEmergencyContact(String value){
            this._emergencyContact = value;
        }


        private String _emergencyPhone;

    
        @PropMeta(propId=15)
    
        public String getEmergencyPhone(){
            return _emergencyPhone;
        }

        public void setEmergencyPhone(String value){
            this._emergencyPhone = value;
        }


        private String _departmentId;

    
        @PropMeta(propId=16)
    
        public String getDepartmentId(){
            return _departmentId;
        }

        public void setDepartmentId(String value){
            this._departmentId = value;
        }


        private String _positionId;

    
        @PropMeta(propId=17)
    
        public String getPositionId(){
            return _positionId;
        }

        public void setPositionId(String value){
            this._positionId = value;
        }


        private String _jobTitle;

    
        @PropMeta(propId=18)
    
        public String getJobTitle(){
            return _jobTitle;
        }

        public void setJobTitle(String value){
            this._jobTitle = value;
        }


        private String _superiorId;

    
        @PropMeta(propId=19)
    
        public String getSuperiorId(){
            return _superiorId;
        }

        public void setSuperiorId(String value){
            this._superiorId = value;
        }


        private String _costCenterId;

    
        @PropMeta(propId=20)
    
        public String getCostCenterId(){
            return _costCenterId;
        }

        public void setCostCenterId(String value){
            this._costCenterId = value;
        }


        private java.time.LocalDate _hireDate;

    
        @PropMeta(propId=21)
    
        public java.time.LocalDate getHireDate(){
            return _hireDate;
        }

        public void setHireDate(java.time.LocalDate value){
            this._hireDate = value;
        }


        private java.time.LocalDate _probationEndDate;

    
        @PropMeta(propId=22)
    
        public java.time.LocalDate getProbationEndDate(){
            return _probationEndDate;
        }

        public void setProbationEndDate(java.time.LocalDate value){
            this._probationEndDate = value;
        }


        private java.time.LocalDate _regularDate;

    
        @PropMeta(propId=23)
    
        public java.time.LocalDate getRegularDate(){
            return _regularDate;
        }

        public void setRegularDate(java.time.LocalDate value){
            this._regularDate = value;
        }


        private java.time.LocalDate _resignationDate;

    
        @PropMeta(propId=24)
    
        public java.time.LocalDate getResignationDate(){
            return _resignationDate;
        }

        public void setResignationDate(java.time.LocalDate value){
            this._resignationDate = value;
        }


        private String _resignationReason;

    
        @PropMeta(propId=25)
    
        public String getResignationReason(){
            return _resignationReason;
        }

        public void setResignationReason(String value){
            this._resignationReason = value;
        }


        private String _employmentStatus;

    
        @PropMeta(propId=26)
    
        public String getEmploymentStatus(){
            return _employmentStatus;
        }

        public void setEmploymentStatus(String value){
            this._employmentStatus = value;
        }


        private String _employeeType;

    
        @PropMeta(propId=27)
    
        public String getEmployeeType(){
            return _employeeType;
        }

        public void setEmployeeType(String value){
            this._employeeType = value;
        }


        private String _bankAccountId;

    
        @PropMeta(propId=28)
    
        public String getBankAccountId(){
            return _bankAccountId;
        }

        public void setBankAccountId(String value){
            this._bankAccountId = value;
        }


        private String _socialSecurityNo;

    
        @PropMeta(propId=29)
    
        public String getSocialSecurityNo(){
            return _socialSecurityNo;
        }

        public void setSocialSecurityNo(String value){
            this._socialSecurityNo = value;
        }


        private String _taxFileNo;

    
        @PropMeta(propId=30)
    
        public String getTaxFileNo(){
            return _taxFileNo;
        }

        public void setTaxFileNo(String value){
            this._taxFileNo = value;
        }


        private String _userAccountId;

    
        @PropMeta(propId=31)
    
        public String getUserAccountId(){
            return _userAccountId;
        }

        public void setUserAccountId(String value){
            this._userAccountId = value;
        }


        private String _orgId;

    
        @PropMeta(propId=32)
    
        public String getOrgId(){
            return _orgId;
        }

        public void setOrgId(String value){
            this._orgId = value;
        }


        private String _remark;

    
        @PropMeta(propId=33)
    
        public String getRemark(){
            return _remark;
        }

        public void setRemark(String value){
            this._remark = value;
        }


    }
