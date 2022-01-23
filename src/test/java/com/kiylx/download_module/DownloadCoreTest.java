package com.kiylx.download_module;

import com.kiylx.download_module.lib_core.interfaces.DownloadTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DownloadCoreTest {

    @BeforeEach
    void setUp() {
    }

    @Test
    void testDownload() {
        Student student = new Student();
        System.out.print("-name:"+student.getName()+"-age:"+student.getAge()+"\n");
        Info info=new Info(student.getName(),student.getAge());
        System.out.print("-name:"+info.getName()+"-age:"+info.getAge()+"\n");

        student.setAge(12);
        student.setName("tom");
        System.out.print("-name:"+student.getName()+"-age:"+student.getAge()+"\n");
        System.out.print("-name:"+info.getName()+"-age:"+info.getAge()+"\n");
    }

}