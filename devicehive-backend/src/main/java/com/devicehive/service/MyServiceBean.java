package com.devicehive.service;


import org.springframework.stereotype.Service;

@Service
public class MyServiceBean {

    public void printMethod() {
        System.out.println("My name is " + MyServiceBean.class.getName()+" Cena");
    }
}
