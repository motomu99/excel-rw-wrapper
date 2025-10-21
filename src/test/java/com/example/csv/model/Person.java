package com.example.csv.model;

import com.opencsv.bean.CsvBindByName;

/**
 * 日本語サンプル用のPerson Beanクラス
 * てか、アノテーションで項目名を指定できるよ〜✨
 */
public class Person {

    @CsvBindByName(column = "名前")
    private String name;

    @CsvBindByName(column = "年齢")
    private Integer age;

    @CsvBindByName(column = "職業")
    private String occupation;

    @CsvBindByName(column = "出身地")
    private String birthplace;

    // デフォルトコンストラクタ
    public Person() {}

    // コンストラクタ
    public Person(String name, Integer age, String occupation, String birthplace) {
        this.name = name;
        this.age = age;
        this.occupation = occupation;
        this.birthplace = birthplace;
    }

    // Getter/Setter
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getBirthplace() {
        return birthplace;
    }

    public void setBirthplace(String birthplace) {
        this.birthplace = birthplace;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", age=" + age +
                ", occupation='" + occupation + '\'' +
                ", birthplace='" + birthplace + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return java.util.Objects.equals(name, person.name) &&
                java.util.Objects.equals(age, person.age) &&
                java.util.Objects.equals(occupation, person.occupation) &&
                java.util.Objects.equals(birthplace, person.birthplace);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, age, occupation, birthplace);
    }
}
