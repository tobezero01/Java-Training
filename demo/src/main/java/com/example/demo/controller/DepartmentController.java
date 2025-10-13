package com.example.demo.controller;

import com.example.demo.entity.Department;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api/v1/depart/")
public class DepartmentController {

    @Autowired
    @Qualifier("depart1")
    Department department1;

    @Autowired
    @Qualifier("depart2")
    Department department2;


    @GetMapping("getAllDepart")
    public List<Department> getAll() {
        List<Department> departments = new ArrayList<>();
        departments.add(department1);
        departments.add(department2);
        return departments;
    }
}
