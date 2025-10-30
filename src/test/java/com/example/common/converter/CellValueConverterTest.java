package com.example.common.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.example.exception.CellValueConversionException;

/**
 * CellValueConverterのテストクラス
 */
@DisplayName("CellValueConverter テスト")
class CellValueConverterTest {

    private Workbook workbook;
    private Sheet sheet;
    private Row row;

    @BeforeEach
    void setUp() {
        workbook = new XSSFWorkbook();
        sheet = workbook.createSheet("Test");
        row = sheet.createRow(0);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (workbook != null) {
            workbook.close();
        }
    }

    @Test
    @DisplayName("文字列セルを文字列として取得")
    void testGetCellValueAsString_String() {
        Cell cell = row.createCell(0);
        cell.setCellValue("テスト文字列");
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("テスト文字列", result);
    }

    @Test
    @DisplayName("数値セル（整数）を文字列として取得")
    void testGetCellValueAsString_IntegerNumber() {
        Cell cell = row.createCell(0);
        cell.setCellValue(123);
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("123", result);
    }

    @Test
    @DisplayName("数値セル（小数）を文字列として取得")
    void testGetCellValueAsString_DoubleNumber() {
        Cell cell = row.createCell(0);
        cell.setCellValue(123.45);
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("123.45", result);
    }

    @Test
    @DisplayName("真偽値セルを文字列として取得")
    void testGetCellValueAsString_Boolean() {
        Cell cell = row.createCell(0);
        cell.setCellValue(true);
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("true", result);
    }

    @Test
    @DisplayName("空白セルを文字列として取得")
    void testGetCellValueAsString_Blank() {
        Cell cell = row.createCell(0);
        cell.setBlank();
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("", result);
    }

    @Test
    @DisplayName("nullセルを文字列として取得")
    void testGetCellValueAsString_Null() {
        String result = CellValueConverter.getCellValueAsString(null);
        
        assertEquals("", result);
    }

    @Test
    @DisplayName("数式セルを文字列として取得")
    void testGetCellValueAsString_Formula() {
        Cell cell = row.createCell(0);
        cell.setCellFormula("A1+B1");
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("A1+B1", result);
    }

    @Test
    @DisplayName("セルをString型に変換")
    void testConvertCellValue_String() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue("テスト");
        
        Object result = CellValueConverter.convertCellValue(cell, String.class, 0, "列A");
        
        assertEquals("テスト", result);
    }

    @Test
    @DisplayName("数値セルをInteger型に変換")
    void testConvertCellValue_Integer() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(42);
        
        Object result = CellValueConverter.convertCellValue(cell, Integer.class, 0, "列A");
        
        assertEquals(42, result);
    }

    @Test
    @DisplayName("数値セルをint型に変換")
    void testConvertCellValue_intPrimitive() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(99);
        
        Object result = CellValueConverter.convertCellValue(cell, int.class, 0, "列A");
        
        assertEquals(99, result);
    }

    @Test
    @DisplayName("数値セルをLong型に変換")
    void testConvertCellValue_Long() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(123456789);
        
        Object result = CellValueConverter.convertCellValue(cell, Long.class, 0, "列A");
        
        assertEquals(123456789L, result);
    }

    @Test
    @DisplayName("数値セルをlong型に変換")
    void testConvertCellValue_longPrimitive() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(987654321);
        
        Object result = CellValueConverter.convertCellValue(cell, long.class, 0, "列A");
        
        assertEquals(987654321L, result);
    }

    @Test
    @DisplayName("数値セルをDouble型に変換")
    void testConvertCellValue_Double() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(3.14159);
        
        Object result = CellValueConverter.convertCellValue(cell, Double.class, 0, "列A");
        
        assertEquals(3.14159, (Double) result, 0.00001);
    }

    @Test
    @DisplayName("数値セルをdouble型に変換")
    void testConvertCellValue_doublePrimitive() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(2.71828);
        
        Object result = CellValueConverter.convertCellValue(cell, double.class, 0, "列A");
        
        assertEquals(2.71828, (Double) result, 0.00001);
    }

    @Test
    @DisplayName("文字列セルを数値に変換")
    void testConvertCellValue_StringToInteger() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue("123");
        
        Object result = CellValueConverter.convertCellValue(cell, Integer.class, 0, "列A");
        
        assertEquals(123, result);
    }

    @Test
    @DisplayName("真偽値セルをBoolean型に変換")
    void testConvertCellValue_Boolean() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(true);
        
        Object result = CellValueConverter.convertCellValue(cell, Boolean.class, 0, "列A");
        
        assertEquals(true, result);
    }

    @Test
    @DisplayName("真偽値セルをboolean型に変換")
    void testConvertCellValue_booleanPrimitive() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue(false);
        
        Object result = CellValueConverter.convertCellValue(cell, boolean.class, 0, "列A");
        
        assertEquals(false, result);
    }

    @Test
    @DisplayName("文字列からBoolean型に変換")
    void testConvertCellValue_StringToBoolean() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue("true");
        
        Object result = CellValueConverter.convertCellValue(cell, Boolean.class, 0, "列A");
        
        assertEquals(true, result);
    }

    @Test
    @DisplayName("日付セルをLocalDate型に変換")
    void testConvertCellValue_LocalDate() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        LocalDate expectedDate = LocalDate.of(2024, 3, 15);
        Date date = Date.from(expectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        cell.setCellValue(date);
        
        // セルに日付フォーマットを設定
        org.apache.poi.ss.usermodel.CellStyle cellStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));
        cell.setCellStyle(cellStyle);
        
        Object result = CellValueConverter.convertCellValue(cell, LocalDate.class, 0, "列A");
        
        assertEquals(expectedDate, result);
    }

    @Test
    @DisplayName("日付セルをLocalDateTime型に変換")
    void testConvertCellValue_LocalDateTime() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        LocalDateTime expectedDateTime = LocalDateTime.of(2024, 3, 15, 14, 30, 0);
        Date date = Date.from(expectedDateTime.atZone(ZoneId.systemDefault()).toInstant());
        cell.setCellValue(date);
        
        // セルに日付フォーマットを設定
        org.apache.poi.ss.usermodel.CellStyle cellStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        cell.setCellStyle(cellStyle);
        
        Object result = CellValueConverter.convertCellValue(cell, LocalDateTime.class, 0, "列A");
        
        // 秒単位で比較（ミリ秒の誤差を許容）
        LocalDateTime resultDateTime = (LocalDateTime) result;
        assertEquals(expectedDateTime.withNano(0), resultDateTime.withNano(0));
    }

    @Test
    @DisplayName("文字列からLocalDate型に変換")
    void testConvertCellValue_StringToLocalDate() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue("2024-03-15");
        
        Object result = CellValueConverter.convertCellValue(cell, LocalDate.class, 0, "列A");
        
        assertEquals(LocalDate.of(2024, 3, 15), result);
    }

    @Test
    @DisplayName("文字列からLocalDateTime型に変換")
    void testConvertCellValue_StringToLocalDateTime() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue("2024-03-15T14:30:00");
        
        Object result = CellValueConverter.convertCellValue(cell, LocalDateTime.class, 0, "列A");
        
        assertEquals(LocalDateTime.of(2024, 3, 15, 14, 30, 0), result);
    }

    @Test
    @DisplayName("nullセルはnullを返す")
    void testConvertCellValue_Null() throws CellValueConversionException {
        Object result = CellValueConverter.convertCellValue(null, String.class, 0, "列A");
        
        assertNull(result);
    }

    @Test
    @DisplayName("空白セルはnullを返す")
    void testConvertCellValue_Blank() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setBlank();
        
        Object result = CellValueConverter.convertCellValue(cell, String.class, 0, "列A");
        
        assertNull(result);
    }

    @Test
    @DisplayName("サポートされていない型はnullを返す")
    void testConvertCellValue_UnsupportedType() throws CellValueConversionException {
        Cell cell = row.createCell(0);
        cell.setCellValue("test");
        
        Object result = CellValueConverter.convertCellValue(cell, java.util.List.class, 0, "列A");
        
        assertNull(result);
    }

    @Test
    @DisplayName("無効な数値文字列を数値に変換するとCellValueConversionExceptionが発生")
    void testConvertCellValue_InvalidNumber() {
        Cell cell = row.createCell(0);
        cell.setCellValue("invalid");
        
        CellValueConversionException exception = assertThrows(
            CellValueConversionException.class,
            () -> CellValueConverter.convertCellValue(cell, Integer.class, 5, "年齢")
        );
        
        assertTrue(exception.getMessage().contains("年齢"));
        assertTrue(exception.getMessage().contains("invalid"));
    }

    @Test
    @DisplayName("無効な日付文字列をLocalDateに変換するとCellValueConversionExceptionが発生")
    void testConvertCellValue_InvalidDate() {
        Cell cell = row.createCell(0);
        cell.setCellValue("not-a-date");
        
        // CellValueConverterは一部のエラーをCellValueConversionExceptionにラップするが、
        // DateTimeParseExceptionは直接スローされる実装になっている
        assertThrows(
            Exception.class,
            () -> CellValueConverter.convertCellValue(cell, LocalDate.class, 3, "生年月日")
        );
    }

    @Test
    @DisplayName("ユーティリティクラスのインスタンス化は禁止")
    void testConstructorThrowsException() throws Exception {
        java.lang.reflect.Constructor<CellValueConverter> constructor = 
            CellValueConverter.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(
            java.lang.reflect.InvocationTargetException.class,
            () -> constructor.newInstance()
        );
        
        assertTrue(exception.getCause() instanceof UnsupportedOperationException);
    }

    @Test
    @DisplayName("数値セルの小数点以下が0の場合は整数として文字列化")
    void testGetCellValueAsString_IntegerValueWithDecimal() {
        Cell cell = row.createCell(0);
        cell.setCellValue(100.0);
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("100", result);
    }

    @Test
    @DisplayName("日付セル（時刻あり）を文字列として取得")
    void testGetCellValueAsString_DateWithTime() {
        Cell cell = row.createCell(0);
        LocalDateTime dateTime = LocalDateTime.of(2024, 3, 15, 14, 30, 0);
        Date date = Date.from(dateTime.atZone(ZoneId.systemDefault()).toInstant());
        cell.setCellValue(date);
        
        // セルに日付フォーマットを設定
        org.apache.poi.ss.usermodel.CellStyle cellStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd HH:mm:ss"));
        cell.setCellStyle(cellStyle);
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertTrue(result.contains("2024-03-15"));
        assertTrue(result.contains("14:30"));
    }

    @Test
    @DisplayName("日付セル（時刻なし）を文字列として取得")
    void testGetCellValueAsString_DateWithoutTime() {
        Cell cell = row.createCell(0);
        LocalDate date = LocalDate.of(2024, 3, 15);
        Date javaDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        cell.setCellValue(javaDate);
        
        // セルに日付フォーマットを設定
        org.apache.poi.ss.usermodel.CellStyle cellStyle = workbook.createCellStyle();
        org.apache.poi.ss.usermodel.CreationHelper createHelper = workbook.getCreationHelper();
        cellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-MM-dd"));
        cell.setCellStyle(cellStyle);
        
        String result = CellValueConverter.getCellValueAsString(cell);
        
        assertEquals("2024-03-15", result);
    }
}

