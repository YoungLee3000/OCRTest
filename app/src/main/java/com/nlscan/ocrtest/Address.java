package com.nlscan.ocrtest;

public class Address {

    private String name;

    private String phoneNumber;

    private String province;

    private String city;

    private String county;

    private String street;

    private String detail;

    public  Address(){

    }

    public Address(String name, String phoneNumber,
                   String province, String city, String county,
                   String street, String detail) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.province = province;
        this.city = city;
        this.county = county;
        this.street = street;
        this.detail = detail;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getCounty() {
        return county;
    }

    public void setCounty(String county) {
        this.county = county;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }
}
