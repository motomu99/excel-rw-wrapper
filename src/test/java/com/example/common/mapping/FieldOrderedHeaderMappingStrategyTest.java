package com.example.common.mapping;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * FieldOrderedHeaderMappingStrategyのテスト
 */
class FieldOrderedHeaderMappingStrategyTest {

    @NoArgsConstructor
    @AllArgsConstructor
    static class OrderedPerson {
        @CsvBindByName(column = "名前")
        private String name;

        @CsvBindByName(column = "年齢")
        private Integer age;

        @CsvBindByName(column = "職業")
        private String occupation;
    }
    
    @NoArgsConstructor
    @AllArgsConstructor
    static class ReversePerson {
        @CsvBindByName(column = "Z")
        private String zField;

        @CsvBindByName(column = "A")
        private String aField;
    }

    @Test
    @DisplayName("フィールド定義順にヘッダーが生成されること")
    void testGenerateHeader_Ordered() throws CsvRequiredFieldEmptyException {
        FieldOrderedHeaderMappingStrategy<OrderedPerson> strategy = new FieldOrderedHeaderMappingStrategy<>();
        strategy.setType(OrderedPerson.class);

        String[] header = strategy.generateHeader(new OrderedPerson());
        
        // OpenCSVの標準だとアルファベット順等になる可能性があるが、
        // この戦略ではフィールド順であることを確認
        assertArrayEquals(new String[]{"名前", "年齢", "職業"}, header);
    }
    
    @Test
    @DisplayName("カラム名に関わらずフィールド定義順であること")
    void testGenerateHeader_Reverse() throws CsvRequiredFieldEmptyException {
        FieldOrderedHeaderMappingStrategy<ReversePerson> strategy = new FieldOrderedHeaderMappingStrategy<>();
        strategy.setType(ReversePerson.class);

        String[] header = strategy.generateHeader(new ReversePerson());
        
        // カラム名が Z, A でも、フィールド順（Z, A）で出力されること
        // （OpenCSV標準のHeaderColumnNameMappingStrategyはアルファベット順ソートするため A, Z になる）
        assertArrayEquals(new String[]{"Z", "A"}, header);
    }
}
