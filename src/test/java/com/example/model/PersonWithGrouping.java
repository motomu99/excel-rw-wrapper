package com.example.model;

import com.example.csv.sorter.GroupingSortable;
import com.opencsv.bean.CsvBindByName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * GroupingSortableインターフェースを実装したPersonクラス
 * 
 * <p>職業でグルーピングし、年齢でソートする例</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonWithGrouping implements GroupingSortable<String> {
    
    @CsvBindByName(column = "名前")
    private String name;
    
    @CsvBindByName(column = "年齢")
    private Integer age;
    
    @CsvBindByName(column = "職業")
    private String occupation;
    
    @CsvBindByName(column = "出身地")
    private String birthplace;
    
    /**
     * 職業をグループキーとして返す
     */
    @Override
    public String getGroupKey() {
        return occupation != null ? occupation : "不明";
    }
    
    /**
     * 年齢でソート（昇順）
     */
    @Override
    public int compareTo(GroupingSortable<String> other) {
        if (!(other instanceof PersonWithGrouping)) {
            return 0;
        }
        PersonWithGrouping p = (PersonWithGrouping) other;
        
        // 年齢がnullの場合の処理
        if (this.age == null && p.age == null) {
            return 0;
        }
        if (this.age == null) {
            return 1;  // nullは後ろ
        }
        if (p.age == null) {
            return -1;  // nullは後ろ
        }
        
        return Integer.compare(this.age, p.age);
    }
}

