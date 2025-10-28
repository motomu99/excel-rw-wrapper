package com.example.csv.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 日付フィールドを持つテスト用モデル
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonWithDate {
    
    @CsvBindByName(column = "名前")
    private String name;
    
    @CsvBindByName(column = "誕生日")
    private LocalDate birthDate;
    
    @CsvBindByName(column = "登録日時")
    private LocalDateTime registeredAt;
}

