package com.example.common.mapping;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.CsvCustomBindByName;
import com.opencsv.bean.CsvCustomBindByPosition;
import com.opencsv.bean.AbstractBeanField;

/**
 * MappingStrategyDetectorのテスト
 */
class MappingStrategyDetectorTest {

    // ヘッダーベース
    static class NameBean {
        @CsvBindByName
        private String name;
    }

    // カスタムヘッダーベース
    static class CustomNameBean {
        @CsvCustomBindByName(converter = DummyConverter.class)
        private String name;
    }

    // 位置ベース
    static class PositionBean {
        @CsvBindByPosition(position = 0)
        private String name;
    }

    // カスタム位置ベース
    static class CustomPositionBean {
        @CsvCustomBindByPosition(position = 0, converter = DummyConverter.class)
        private String name;
    }

    // 両方混在
    static class MixedBean {
        @CsvBindByName
        @CsvBindByPosition(position = 0)
        private String name;
    }

    // アノテーションなし
    static class NoAnnotationBean {
        @SuppressWarnings("unused")
        private String name;
    }
    
    // ダミーコンバーター
    public static class DummyConverter extends AbstractBeanField<String, String> {
        @Override
        protected Object convert(String value) { return value; }
    }

    @Test
    @DisplayName("CsvBindByNameのみの場合、usePositionMapping=false(HeaderMapping)が返る")
    void testDetect_Name() {
        Optional<Boolean> result = MappingStrategyDetector.detectUsePositionMapping(NameBean.class);
        assertTrue(result.isPresent());
        assertFalse(result.get()); // false = HeaderMapping
    }

    @Test
    @DisplayName("CsvCustomBindByNameのみの場合、usePositionMapping=falseが返る")
    void testDetect_CustomName() {
        Optional<Boolean> result = MappingStrategyDetector.detectUsePositionMapping(CustomNameBean.class);
        assertTrue(result.isPresent());
        assertFalse(result.get());
    }

    @Test
    @DisplayName("CsvBindByPositionのみの場合、usePositionMapping=true(PositionMapping)が返る")
    void testDetect_Position() {
        Optional<Boolean> result = MappingStrategyDetector.detectUsePositionMapping(PositionBean.class);
        assertTrue(result.isPresent());
        assertTrue(result.get()); // true = PositionMapping
    }

    @Test
    @DisplayName("CsvCustomBindByPositionのみの場合、usePositionMapping=trueが返る")
    void testDetect_CustomPosition() {
        Optional<Boolean> result = MappingStrategyDetector.detectUsePositionMapping(CustomPositionBean.class);
        assertTrue(result.isPresent());
        assertTrue(result.get());
    }

    @Test
    @DisplayName("両方のアノテーションが存在する場合、IllegalArgumentExceptionがスローされる")
    void testDetect_Mixed() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            MappingStrategyDetector.detectUsePositionMapping(MixedBean.class);
        });
        assertTrue(ex.getMessage().contains("マッピング戦略を明示的に指定してください"));
    }

    @Test
    @DisplayName("アノテーションが存在しない場合、Emptyが返る")
    void testDetect_None() {
        Optional<Boolean> result = MappingStrategyDetector.detectUsePositionMapping(NoAnnotationBean.class);
        assertFalse(result.isPresent());
    }
}

