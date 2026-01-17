package com.example.common.converter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.dhatim.fastexcel.reader.Row;

import com.example.exception.CellValueConversionException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    private static final DataFormatter FORMATTER = new DataFormatter();

    @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
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
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return formatDateCell(cell);
            } else {
                double value = cell.getNumericCellValue();
                // 整数値かどうかをチェック（小数点以下が0の場合）
                if (value == (long) value) {
                    return String.valueOf((long) value);
                } else {
                    return String.valueOf(value);
                }
            }
        }
        if (cell.getCellType() == CellType.STRING) {
            // STRINGセルはFORMATTERで見た目の値を取得し、フリガナ除去のみ適用
            String formattedValue = FORMATTER.formatCellValue(cell);
            return removeTrailingFurigana(formattedValue);
        }
        // その他のセルタイプ（BLANK等）はFORMATTERで処理
        String formattedValue = FORMATTER.formatCellValue(cell);
        return formattedValue == null ? "" : formattedValue.trim();
    }

    /**
     * 末尾のフリガナを除去（データ完全性を保つための最小限の処理）
     * 
     * @param value 文字列値
     * @return フリガナ除去後の文字列
     */
    private static String removeTrailingFurigana(String value) {
        if (value == null) {
            return "";
        }
        // 末尾の空白+かな文字（フリガナ）のみ除去
        String cleaned = value.replaceAll("\\s+[\\p{IsHiragana}\\p{IsKatakana}\\u30FC]+$", "");
        return cleaned.trim();
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

    /**
     * fastexcelのRowからセルの値を文字列として取得
     * 
     * @param row 行
     * @param index セルインデックス
     * @return セルの値（文字列形式）
     */
    public static String getCellValueAsString(Row row, int index) {
        if (row == null || !row.hasCell(index)) {
            return null;
        }

        try {
            org.dhatim.fastexcel.reader.Cell cell = row.getCell(index);
            if (cell == null || cell.getType() == org.dhatim.fastexcel.reader.CellType.EMPTY) {
                return null;
            }

            if (cell.getType() == org.dhatim.fastexcel.reader.CellType.FORMULA) {
                return cell.getFormula();
            }

            return cell.getText();
        } catch (Exception e) {
            log.debug("セルの取得またはテキスト変換に失敗しました: index={}", index, e);
            return null;
        }
    }

    /**
     * fastexcelのRowからセルの値を指定された型に変換
     *
     * @param row 行
     * @param index セルインデックス
     * @param targetType 変換先の型
     * @param rowNum 行番号（エラーメッセージ用、0始まり）
     * @param columnName 列名（エラーメッセージ用）
     * @return 変換された値
     * @throws CellValueConversionException 型変換に失敗した場合
     */
    public static Object convertCellValue(Row row, int index, Class<?> targetType, int rowNum, String columnName)
            throws CellValueConversionException {
        if (row == null) {
            return null;
        }

        try {
            // セルが存在するかチェック
            String cellText = getCellValueAsString(row, index);
            if (cellText == null || cellText.isEmpty()) {
                return null;
            }

            if (targetType == String.class) {
                // 文字列型はそのまま返す（空白も保持）
                return cellText;
            } else if (isNumericType(targetType)) {
                // 数値型の場合は、空白文字列をnullとして扱う
                String trimmed = cellText.trim();
                if (trimmed.isEmpty()) {
                    return null;
                }
                return convertToNumericType(row, index, targetType, trimmed);
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                return convertToBooleanType(row, index, cellText.trim());
            } else if (targetType == LocalDate.class) {
                return convertToLocalDate(row, index, cellText.trim());
            } else if (targetType == LocalDateTime.class) {
                return convertToLocalDateTime(row, index, cellText.trim());
            }
        } catch (IllegalArgumentException e) {
            String cellValue = getCellValueAsString(row, index);
            log.error("セル値の変換に失敗しました: 行={}, 列='{}', 値='{}', 型={}",
                    rowNum + 1, columnName, cellValue, targetType.getSimpleName());
            throw new CellValueConversionException(rowNum, columnName, cellValue, targetType, e);
        }

        return null;
    }

    /**
     * fastexcelのRowから数値型に変換
     * 
     * @param row 行
     * @param index セルインデックス
     * @param targetType 変換先の型
     * @param cellText セルのテキスト（フォールバック用）
     * @return 変換された数値
     */
    private static Number convertToNumericType(Row row, int index, Class<?> targetType, String cellText) {
        // まず数値として取得を試みる
        try {
            Optional<BigDecimal> optNumber = row.getCellAsNumber(index);
            if (optNumber.isPresent()) {
                BigDecimal num = optNumber.get();
                if (targetType == Integer.class || targetType == int.class) {
                    return num.intValue();
                } else if (targetType == Long.class || targetType == long.class) {
                    return num.longValue();
                } else if (targetType == Double.class || targetType == double.class) {
                    return num.doubleValue();
                }
            }
        } catch (org.dhatim.fastexcel.reader.ExcelReaderException ignored) {
            // 数値取得に失敗した場合は文字列からのパースにフォールバック
            log.trace("fastexcelからの数値取得に失敗しました。文字列パースを試みます。");
        }
        // 数値として取得できない場合は文字列からパース
        return parseNumericString(cellText, targetType);
    }

    /**
     * fastexcelのRowからBoolean型に変換
     * 
     * @param row 行
     * @param index セルインデックス
     * @param cellText セルのテキスト（フォールバック用）
     * @return 変換されたBoolean値
     */
    private static Boolean convertToBooleanType(Row row, int index, String cellText) {
        try {
            Optional<Boolean> optBoolean = row.getCellAsBoolean(index);
            if (optBoolean.isPresent()) {
                return optBoolean.get();
            }
        } catch (org.dhatim.fastexcel.reader.ExcelReaderException ignored) {
            // Boolean取得に失敗した場合はフォールバック
            log.trace("fastexcelからのBoolean取得に失敗しました。文字列パースを試みます。");
        }
        return Boolean.parseBoolean(cellText);
    }

    /**
     * fastexcelのRowからLocalDate型に変換
     * 
     * @param row 行
     * @param index セルインデックス
     * @param cellText セルのテキスト（フォールバック用）
     * @return 変換されたLocalDate
     */
    private static LocalDate convertToLocalDate(Row row, int index, String cellText) {
        try {
            Optional<LocalDateTime> optDateTime = row.getCellAsDate(index);
            if (optDateTime.isPresent()) {
                return optDateTime.get().toLocalDate();
            }
        } catch (org.dhatim.fastexcel.reader.ExcelReaderException ignored) {
            // 日付取得に失敗した場合はフォールバック
            log.trace("fastexcelからの日付取得に失敗しました。文字列パースを試みます。");
        }
        return LocalDate.parse(cellText);
    }

    /**
     * fastexcelのRowからLocalDateTime型に変換
     * 
     * @param row 行
     * @param index セルインデックス
     * @param cellText セルのテキスト（フォールバック用）
     * @return 変換されたLocalDateTime
     */
    private static LocalDateTime convertToLocalDateTime(Row row, int index, String cellText) {
        try {
            Optional<LocalDateTime> optDateTime = row.getCellAsDate(index);
            if (optDateTime.isPresent()) {
                return optDateTime.get();
            }
        } catch (org.dhatim.fastexcel.reader.ExcelReaderException ignored) {
            // 日時取得に失敗した場合はフォールバック
            log.trace("fastexcelからの日時取得に失敗しました。文字列パースを試みます。");
        }
        return LocalDateTime.parse(cellText);
    }
}

