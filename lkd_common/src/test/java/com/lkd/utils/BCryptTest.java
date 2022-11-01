package com.lkd.utils;

import org.testng.annotations.Test;

public class BCryptTest{
    @Test
    public void generatePwd(){
        String salt = BCrypt.gensalt();
        String pwd = BCrypt.hashpw("admin",salt);
        System.out.println(pwd);
    }
}
