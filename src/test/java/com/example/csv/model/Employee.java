package com.example.csv.model;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvDate;

import java.time.LocalDate;

/**
 * 英語サンプル用のEmployee Beanクラス
 * てか、アノテーションで項目名を指定できるよ〜✨
 */
public class Employee {

    @CsvBindByName(column = "id")
    private Long id;

    @CsvBindByName(column = "name")
    private String name;

    @CsvBindByName(column = "department")
    private String department;

    @CsvBindByName(column = "salary")
    private Integer salary;

    @CsvBindByName(column = "hire_date")
    @CsvDate("yyyy-MM-dd")
    private LocalDate hireDate;

    // デフォルトコンストラクタ
    public Employee() {}

    // コンストラクタ
    public Employee(Long id, String name, String department, Integer salary, LocalDate hireDate) {
        this.id = id;
        this.name = name;
        this.department = department;
        this.salary = salary;
        this.hireDate = hireDate;
    }

    // Getter/Setter
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getSalary() {
        return salary;
    }

    public void setSalary(Integer salary) {
        this.salary = salary;
    }

    public LocalDate getHireDate() {
        return hireDate;
    }

    public void setHireDate(LocalDate hireDate) {
        this.hireDate = hireDate;
    }

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", department='" + department + '\'' +
                ", salary=" + salary +
                ", hireDate=" + hireDate +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return java.util.Objects.equals(id, employee.id) &&
                java.util.Objects.equals(name, employee.name) &&
                java.util.Objects.equals(department, employee.department) &&
                java.util.Objects.equals(salary, employee.salary) &&
                java.util.Objects.equals(hireDate, employee.hireDate);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name, department, salary, hireDate);
    }
}
