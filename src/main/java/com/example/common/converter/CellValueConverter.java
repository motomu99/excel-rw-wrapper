package com.example.common.converter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;

import com.example.exception.CellValueConversionException;

import lombok.extern.slf4j.Slf4j;

/**
 * Excelセルの値を様々な型に変換するユーティリティクラス
 * 
 * <p>POIのCellオブジェクトから値を取得し、指定された型に変換します。
 * String, 数値型（Integer, Long, Double）、Boolean、日付型（LocalDate, LocalDateTime）を
 * サポートしています。</p>
 */
@Slf4j
public class CellValueConverter {

    /** サポートする数値型のセット */
    private static final Set<Class<?>> NUMERIC_TYPES = Set.of(
        Integer.class, int.class,
        Long.class, long.class,
        Double.class, double.class
    );

    private CellValueConverter() {
        // ユーティリティクラスのためインスタンス化を禁止
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * セルの値を文字列として取得
     * 
     * @param cell セル
     * @return セルの値（文字列形式）
     */
    public static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield formatDateCell(cell);
                } else {
                    double value = cell.getNumericCellValue();
                    // 整数値かどうかをチェック（小数点以下が0の場合）
                    if (value == (long) value) {
                        yield String.valueOf((long) value);
                    } else {
                        yield String.valueOf(value);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * セルの値を指定された型に変換
     *
     * @param cell セル
     * @param targetType 変換先の型
     * @param rowIndex 行番号（エラーメッセージ用）
     * @param columnName 列名（エラーメッセージ用）
     * @return 変換された値
     * @throws CellValueConversionException 型変換に失敗した場合
     */
    public static Object convertCellValue(Cell cell, Class<?> targetType, int rowIndex, String columnName)
            throws CellValueConversionException {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return getCellValueAsString(cell);
            } else if (isNumericType(targetType)) {
                return convertToNumericType(cell, targetType);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return convertToBooleanType(cell);
            } else if (targetType == LocalDate.class) {
                return convertToLocalDate(cell);
            } else if (targetType == LocalDateTime.class) {
                return convertToLocalDateTime(cell);
            }
        } catch (IllegalArgumentException e) {
            String cellValue = getCellValueAsString(cell);
            log.error("セル値の変換に失敗しました: 行={}, 列='{}', 値='{}', 型={}",
                    rowIndex + 1, columnName, cellValue, targetType.getSimpleName());
            throw new CellValueConversionException(rowIndex, columnName, cellValue, targetType, e);
        }

        return null;
    }

    /**
     * 数値型かどうかを判定
     * 
     * @param type 判定する型
     * @return 数値型の場合true
     */
    private static boolean isNumericType(Class<?> type) {
        return NUMERIC_TYPES.contains(type);
    }

    /**
     * セルを数値型に変換
     * 
     * @param cell セル
     * @param targetType 変換先の型
     * @return 変換された数値
     */
    private static Number convertToNumericType(Cell cell, Class<?> targetType) {
        Number result = parseNumericCell(cell, targetType);
        if (result != null) {
            return result;
        }
        return parseNumericString(getCellValueAsString(cell), targetType);
    }

    /**
     * セルをBoolean型に変換
     * 
     * @param cell セル
     * @return 変換されたBoolean値
     */
    private static Boolean convertToBooleanType(Cell cell) {
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }
        return Boolean.parseBoolean(getCellValueAsString(cell));
    }

    /**
     * セルをLocalDate型に変換
     * 
     * @param cell セル
     * @return 変換されたLocalDate
     */
    private static LocalDate convertToLocalDate(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return convertDateToLocalDate(cell.getDateCellValue());
        }
        return LocalDate.parse(getCellValueAsString(cell));
    }

    /**
     * セルをLocalDateTime型に変換
     * 
     * @param cell セル
     * @return 変換されたLocalDateTime
     */
    private static LocalDateTime convertToLocalDateTime(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return convertDateToLocalDateTime(cell.getDateCellValue());
        }
        return LocalDateTime.parse(getCellValueAsString(cell));
    }

    /**
     * 数値セルから値を取得し、指定された型にキャスト
     * 
     * @param cell セル
     * @param targetType 変換先の型
     * @return 変換された数値（変換できない場合null）
     */
    private static Number parseNumericCell(Cell cell, Class<?> targetType) {
        if (cell.getCellType() == CellType.NUMERIC) {
            double numericValue = cell.getNumericCellValue();
            if (targetType == Integer.class || targetType == int.class) {
                return (int) numericValue;
            } else if (targetType == Long.class || targetType == long.class) {
                return (long) numericValue;
            } else if (targetType == Double.class || targetType == double.class) {
                return numericValue;
            }
        }
        return null;
    }

    /**
     * 文字列から数値型に変換
     * 
     * @param value 文字列値
     * @param targetType 変換先の型
     * @return 変換された数値（変換できない場合null）
     */
    private static Number parseNumericString(String value, Class<?> targetType) {
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value);
        }
        return null;
    }

    /**
     * Date型をLocalDateTimeに変換
     * 
     * @param date Date型の日付
     * @return LocalDateTime
     */
    private static LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Date型をLocalDateに変換
     * 
     * @param date Date型の日付
     * @return LocalDate
     */
    private static LocalDate convertDateToLocalDate(Date date) {
        return convertDateToLocalDateTime(date).toLocalDate();
    }

    /**
     * 日付セルを適切な文字列に変換
     * 
     * @param cell 日付セル
     * @return フォーマット済み日付文字列
     */
    private static String formatDateCell(Cell cell) {
        Date date = cell.getDateCellValue();
        LocalDateTime dateTime = convertDateToLocalDateTime(date);
        // 時刻が00:00:00の場合はLocalDateとして扱う
        if (dateTime.toLocalTime().equals(java.time.LocalTime.MIDNIGHT)) {
            return dateTime.toLocalDate().toString();
        } else {
            return dateTime.toString();
        }
    }
}

