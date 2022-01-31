package com.kiylx.download_module.utils;

import org.junit.jupiter.api.Test;

class UtilsTest {
    @Test
    public void test1(){
        String name="sdgfsa.fjkb_20211214161000.exe";
        String ext=Utils.PassFileNameGetExtension(name);
        System.out.println(ext);
    }

}