package com.example.excel.reader;

import static org.junit.jupiter.api.Assertions.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.exception.HeaderNotFoundException;
import com.opencsv.bean.CsvBindByName;

import lombok.Data;

@DisplayName("ヘッダー欠損時の挙動テスト")
class MissingHeaderTest {

    @TempDir
    Path tempDir;

    @Data
    public static class TestBean {
        @CsvBindByName(column = "必須カラム")
        private String requiredColumn;
        
        @CsvBindByName(column = "存在しないカラム")
        private String missingColumn;
    }

    @Test
    @DisplayName("存在しないカラムがBeanにある場合、HeaderNotFoundExceptionがスローされること")
    void testMissingColumnThrowsException() throws IOException {
        // テスト用Excel作成
        Path excelFile = tempDir.resolve("missing_column.xlsx");
        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fos = new FileOutputStream(excelFile.toFile())) {
            
            Sheet sheet = workbook.createSheet("Sheet1");
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("必須カラム");
            // "存在しないカラム" は作成しない
            
            Row dataRow = sheet.createRow(1);
            dataRow.createCell(0).setCellValue("値1");
            
            workbook.write(fos);
        }

        // 実行と検証
        Exception exception = assertThrows(IOException.class, () -> {
            ExcelReader.builder(TestBean.class, excelFile).read();
        });
        
        // 原因がHeaderNotFoundExceptionであることを確認
        // ExcelReaderは内部のUncheckedExcelExceptionをIOExceptionにラップしている可能性があるため、causeをチェック
        // あるいは ExcelReader.read() で HeaderNotFoundException をそのままスローするように修正されていれば直接キャッチできる
        
        Throwable cause = exception;
        boolean found = false;
        while (cause != null) {
            if (cause instanceof HeaderNotFoundException) {
                found = true;
                break;
            }
            cause = cause.getCause();
        }
        
        assertTrue(found, "HeaderNotFoundExceptionが原因に含まれていること");
        assertEquals("必須ヘッダーカラムが見つかりません: 存在しないカラム", cause.getMessage());
    }
}


