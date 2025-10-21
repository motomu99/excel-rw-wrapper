package com.example.csv;

import com.example.csv.model.Employee;
import com.example.csv.model.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import com.opencsv.exceptions.CsvException;

/**
 * CsvBeanReaderのテストクラス
 * てか、Bean読み込みの使い方が分かるよ〜✨
 * 
 * ## Bean読み込みの基本的な使い方
 * 
 * ```java
 * // CsvBeanReaderのインスタンスを作成
 * CsvBeanReader csvBeanReader = new CsvBeanReader();
 * 
 * // CSVファイルをBeanのListとして読み込み
 * List<Person> persons = csvBeanReader.readCsvToBeans("path/to/your/file.csv", Person.class);
 * 
 * // Beanのプロパティにアクセス
 * for (Person person : persons) {
 *     System.out.println("名前: " + person.getName());
 *     System.out.println("年齢: " + person.getAge());
 * }
 * ```
 * 
 * ## アノテーションでの項目名指定
 * 
 * ```java
 * public class Person {
 *     @CsvBindByName(column = "名前")
 *     private String name;
 *     
 *     @CsvBindByName(column = "年齢")
 *     private Integer age;
 * }
 * ```
 * 
 * ## 日付フィールドの指定
 * 
 * ```java
 * @CsvBindByName(column = "hire_date")
 * @CsvDate("yyyy-MM-dd")
 * private LocalDate hireDate;
 * ```
 */
public class CsvBeanReaderTest {

    private CsvBeanReader csvBeanReader;

    @BeforeEach
    void setUp() {
        csvBeanReader = new CsvBeanReader();
    }

    @Test
    void testReadCsvToBeans() throws IOException, CsvException {
        // 基本的なBean読み込みの使い方
        // CsvBeanReader csvBeanReader = new CsvBeanReader();
        // List<Person> persons = csvBeanReader.readCsvToBeans("path/to/your/file.csv", Person.class);
        
        // 日本語サンプルCSVをPerson Beanとして読み込み
        List<Person> persons = csvBeanReader.readCsvToBeans("src/test/resources/sample.csv", Person.class);
        
        assertNotNull(persons);
        assertEquals(5, persons.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のPersonの確認
        Person firstPerson = persons.get(0);
        assertEquals("田中太郎", firstPerson.getName());
        assertEquals(25, firstPerson.getAge());
        assertEquals("エンジニア", firstPerson.getOccupation());
        assertEquals("東京", firstPerson.getBirthplace());
        
        // 2番目のPersonの確認
        Person secondPerson = persons.get(1);
        assertEquals("佐藤花子", secondPerson.getName());
        assertEquals(30, secondPerson.getAge());
        assertEquals("デザイナー", secondPerson.getOccupation());
        assertEquals("大阪", secondPerson.getBirthplace());
    }

    @Test
    void testReadCsvToBeansWithEmployee() throws IOException, CsvException {
        // Employee Beanでの読み込みテスト
        List<Employee> employees = csvBeanReader.readCsvToBeans("src/test/resources/employees.csv", Employee.class);
        
        assertNotNull(employees);
        assertEquals(5, employees.size()); // ヘッダーを除いた5件のデータ
        
        // 最初のEmployeeの確認
        Employee firstEmployee = employees.get(0);
        assertEquals(1L, firstEmployee.getId());
        assertEquals("John Smith", firstEmployee.getName());
        assertEquals("Engineering", firstEmployee.getDepartment());
        assertEquals(75000, firstEmployee.getSalary());
        assertEquals(LocalDate.of(2020, 1, 15), firstEmployee.getHireDate());
        
        // 2番目のEmployeeの確認
        Employee secondEmployee = employees.get(1);
        assertEquals(2L, secondEmployee.getId());
        assertEquals("Jane Doe", secondEmployee.getName());
        assertEquals("Marketing", secondEmployee.getDepartment());
        assertEquals(65000, secondEmployee.getSalary());
        assertEquals(LocalDate.of(2019, 3, 22), secondEmployee.getHireDate());
    }

    @Test
    void testReadCsvToBeansFromStream() throws IOException, CsvException {
        // InputStreamからのBean読み込みテスト
        try (var inputStream = getClass().getClassLoader().getResourceAsStream("sample.csv")) {
            assertNotNull(inputStream);
            
            List<Person> persons = csvBeanReader.readCsvToBeansFromStream(inputStream, Person.class);
            
            assertNotNull(persons);
            assertEquals(5, persons.size());
            
            // 最初のPersonの確認
            Person firstPerson = persons.get(0);
            assertEquals("田中太郎", firstPerson.getName());
            assertEquals(25, firstPerson.getAge());
        }
    }

    @Test
    void testReadCsvToBeansWithEmptyFile() {
        // 存在しないファイルの読み込みテスト
        assertThrows(IOException.class, () -> {
            csvBeanReader.readCsvToBeans("non-existent-file.csv", Person.class);
        });
    }

    @Test
    void testBeanMappingWithAnnotations() throws IOException, CsvException {
        // アノテーションによるマッピングのテスト
        List<Person> persons = csvBeanReader.readCsvToBeans("src/test/resources/sample.csv", Person.class);
        
        assertNotNull(persons);
        assertFalse(persons.isEmpty());
        
        // 全てのPersonが正しくマッピングされていることを確認
        for (Person person : persons) {
            assertNotNull(person.getName());
            assertNotNull(person.getAge());
            assertNotNull(person.getOccupation());
            assertNotNull(person.getBirthplace());
        }
    }

    @Test
    void testEmployeeWithDateMapping() throws IOException, CsvException {
        // 日付フィールドのマッピングテスト
        List<Employee> employees = csvBeanReader.readCsvToBeans("src/test/resources/employees.csv", Employee.class);
        
        assertNotNull(employees);
        assertFalse(employees.isEmpty());
        
        // 全てのEmployeeの日付が正しくマッピングされていることを確認
        for (Employee employee : employees) {
            assertNotNull(employee.getId());
            assertNotNull(employee.getName());
            assertNotNull(employee.getDepartment());
            assertNotNull(employee.getSalary());
            assertNotNull(employee.getHireDate());
        }
    }
}
