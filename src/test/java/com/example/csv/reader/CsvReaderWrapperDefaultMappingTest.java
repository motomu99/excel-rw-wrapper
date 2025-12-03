package com.example.csv.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.csv.writer.CsvWriterWrapper;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * CsvReaderWrapperのデフォルトマッピング戦略判定のテスト
 */
class CsvReaderWrapperDefaultMappingTest {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NameBean {
        @CsvBindByName(column = "Name")
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PositionBean {
        @CsvBindByPosition(position = 0)
        private String name;
    }

    @Test
    @DisplayName("@CsvBindByNameがある場合、デフォルトでヘッダーマッピングが使用される")
    void testDefaultHeaderMapping(@TempDir Path tempDir) {
        Path path = tempDir.resolve("name.csv");
        List<NameBean> data = List.of(new NameBean("Taro"));
        
        // 書き込み（ヘッダーあり）
        CsvWriterWrapper.builder(NameBean.class, path)
            .write(data);
            
        // 読み込み（設定なし -> 自動判定）
        List<NameBean> result = CsvReaderWrapper.builder(NameBean.class, path)
            .read();
            
        assertEquals(1, result.size());
        assertEquals("Taro", result.get(0).getName());
    }

    @Test
    @DisplayName("@CsvBindByPositionがある場合、デフォルトで位置マッピングが使用される")
    void testDefaultPositionMapping(@TempDir Path tempDir) {
        Path path = tempDir.resolve("pos.csv");
        List<PositionBean> data = List.of(new PositionBean("Jiro"));
        
        // 書き込み（ヘッダーなし・位置指定）
        // 書き込み側はデフォルトがHeaderMappingなので、PositionBeanの場合は
        // 明示的にusePositionMappingしないとエラーになるか、意図しない出力になる可能性があるが、
        // ここではReaderのテストなので、WriterもPositionMappingで正しくファイルを作る。
        CsvWriterWrapper.builder(PositionBean.class, path)
            .usePositionMapping()
            .write(data);
            
        // 読み込み（設定なし -> 自動判定でPositionMappingになるはず）
        List<PositionBean> result = CsvReaderWrapper.builder(PositionBean.class, path)
            .read();
            
        assertEquals(1, result.size());
        assertEquals("Jiro", result.get(0).getName());
    }
    
    @Test
    @DisplayName("アノテーションがない場合、デフォルトはヘッダーマッピング（false）になる")
    void testDefaultNoAnnotation(@TempDir Path tempDir) {
        // 簡易的なBean
        class NoAnno { 
            @SuppressWarnings("unused")
            String val; 
            @SuppressWarnings("unused")
            public void setVal(String v) { val = v; }
        }
        
        Path path = tempDir.resolve("none.csv");
        // 1行だけのファイル
        try {
            java.nio.file.Files.writeString(path, "data");
        } catch (Exception e) {
            fail(e);
        }
        
        // HeaderMappingなら "data" はヘッダーとみなされ、データ行なし -> 空リスト
        List<NoAnno> result = CsvReaderWrapper.builder(NoAnno.class, path).read();
        assertTrue(result.isEmpty(), "デフォルトでヘッダーマッピングなら、1行目はヘッダー扱いになりデータは0件のはず");
    }
}
