package com.example.model;

import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 注文情報用のOrder Beanクラス
 * アノテーションで項目名を指定してBeanにマッピングできる
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @CsvBindByName(column = "注文ID")
    private String orderId;

    @CsvBindByName(column = "ユーザーID")
    private String userId;

    @CsvBindByName(column = "金額")
    private Integer amount;

    @CsvBindByName(column = "注文日")
    private String orderDate;
}





