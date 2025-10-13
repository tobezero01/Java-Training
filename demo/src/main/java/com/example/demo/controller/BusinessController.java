package com.example.demo.controller;

import com.example.demo.process.BusinessProcess;
import com.example.demo.process.TaskManager;
import com.example.demo.service.BusinessService;
import com.example.demo.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("api/v1/business/")
public class BusinessController {
    @Autowired
    private BusinessService businessService;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private TaskManager taskManager;

    @GetMapping("getSync")
    public String getSyncValue() {
        try {
            String key = "getSync";
            if (redisCacheService.checkExistsKey(key)) {
                return (String)redisCacheService.getValue(key);
            }
            String result1 = businessService.doBusiness("business 01 result ");
            String result2 = businessService.doBusiness("business 02 result ");
            String result3 = businessService.doBusiness("business 03 result ");
            String response = result1 + result2 + result3;
            redisCacheService.setValueWithTimeout(key, response,10, TimeUnit.SECONDS);
            return response;
        } catch (Exception ex) {
            System.out.printf(ex.getMessage());
        }
        return "not success";
    }


    @GetMapping("getASync")
    public String getASyncValue() {
        try {
            String response = "";
            List<String> inputs = Arrays.asList("business 01 result ","business 02 result ","business 03 result ");
            List<BusinessProcess> listTask = new ArrayList<>();
            for (String input : inputs) {
                BusinessProcess task = new BusinessProcess(businessService, input);
                listTask.add(task);
            }
            taskManager.execute(listTask);
            for (BusinessProcess task : listTask) {
                response = response + task.result;
            }
            return response;
        } catch (Exception ex) {
            System.out.printf(ex.getMessage());
        }
        return "not success";
    }

    @GetMapping("test")
    public String pushAll() {
        List<String> value = new ArrayList<>();
        value.add("a");
        value.add("b");
        value.add("c");
        redisCacheService.lPushAll("test", value);
        return "success";
    }

    @GetMapping("rPop")
    public String rpop() {
        if (redisCacheService.checkExistsKey("test")) {
            return (String)redisCacheService.rPop("test");
        }
       return "empty data";
    }
}
