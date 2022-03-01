package com.example.twofactor;

public class User {
    public String userName;
    public String phoneNum;
    public String companyNum;

    public User(){

    }



    public User(String userName, String phoneNum, String companyNum){
        this.userName = userName;
        this.phoneNum = phoneNum;
        this.companyNum = companyNum;
    }

    public String getUserName(){
        return userName;
    }

    public String getPhoneNum(){
        return phoneNum;
    }

    public String getCompanyNum(){return companyNum;}

    public void setCompanyNum(String companyNum){this.companyNum = companyNum;}

    public void setUserName(String userName){
        this.userName = userName;
    }

    public void setPhoneNum (String phoneNum){
        this.phoneNum = phoneNum;
    }

}
