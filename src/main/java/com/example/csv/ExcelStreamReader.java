package com.example.csv;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

import com.example.csv.exception.HeaderNotFoundException;
import com.example.csv.exception.KeyColumnNotFoundException;
import com.example.csv.exception.SheetNotFoundException;
import com.example.csv.exception.CellValueConversionException;

import lombok.extern.slf4j.Slf4j;

/**
 * ExcelファイルをStreamとして読み込むビルダークラス
 * ビルダーパターンを使用してExcel読み込み処理を抽象化
 *
 * 使用例:
 * <pre>
 * // 基本的な使用方法
 * List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *     .sheetIndex(0)
 *     .skip(1)
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // ヘッダー行を自動検出（上から10行以内で「名前」列を探す）
 * List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 *
 * // ヘッダー行の探索範囲を20行に拡張
 * List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *     .headerKey("名前")
 *     .headerSearchRows(20)
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 * </pre>
 */
@Slf4j
public class ExcelStreamReader<T> {

    /** デフォルトのヘッダー探索行数 */
    private static final int DEFAULT_HEADER_SEARCH_ROWS = 10;

    /** サポートする数値型のセット */
    private static final Set<Class<?>> NUMERIC_TYPES = Set.of(
        Integer.class, int.class,
        Long.class, long.class,
        Double.class, double.class
    );

    private final Class<T> beanClass;
    private final Path filePath;
    private int sheetIndex = 0;
    private String sheetName = null;
    private int skipLines = 0;
    private boolean usePositionMapping = false;
    private String headerKeyColumn = null;
    private int headerSearchRows = DEFAULT_HEADER_SEARCH_ROWS;

    /** フィールド情報キャッシュ（パフォーマンス最適化用） */
    private Map<Field, FieldMappingInfo> fieldCache = null;

    private ExcelStreamReader(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * ExcelStreamReaderのインスタンスを作成
     *
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath Excelファイルのパス
     * @return ExcelStreamReaderのインスタンス
     */
    public static <T> ExcelStreamReader<T> of(Class<T> beanClass, Path filePath) {
        return new ExcelStreamReader<>(beanClass, filePath);
    }

    /**
     * シートのインデックスを設定（0から始まる）
     *
     * @param sheetIndex シートのインデックス
     * @return このインスタンス
     */
    public ExcelStreamReader<T> sheetIndex(int sheetIndex) {
        this.sheetIndex = sheetIndex;
        this.sheetName = null;
        return this;
    }

    /**
     * シート名を設定
     *
     * @param sheetName シート名
     * @return このインスタンス
     */
    public ExcelStreamReader<T> sheetName(String sheetName) {
        this.sheetName = sheetName;
        return this;
    }

    /**
     * スキップする行数を設定
     *
     * @param lines スキップする行数
     * @return このインスタンス
     */
    public ExcelStreamReader<T> skip(int lines) {
        this.skipLines = lines;
        return this;
    }

    /**
     * 位置ベースマッピングを使用
     *
     * @return このインスタンス
     */
    public ExcelStreamReader<T> usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーベースマッピングを使用（デフォルト）
     *
     * @return このインスタンス
     */
    public ExcelStreamReader<T> useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }

    /**
     * ヘッダー行を自動検出するためのキー列名を設定
     * 指定された列名を持つ行を、上から指定行数の範囲内で探してヘッダー行とする
     * また、この列の値が空になったらデータ読み込みを終了する
     *
     * <p>注意: process()メソッド実行時に以下の例外が投げられる可能性があります：
     * <ul>
     *   <li>{@link HeaderNotFoundException} - キー列を持つヘッダー行が見つからない場合</li>
     *   <li>{@link KeyColumnNotFoundException} - ヘッダー行にキー列が存在しない場合</li>
     * </ul>
     *
     * @param keyColumnName キーとなる列名
     * @return このインスタンス
     */
    public ExcelStreamReader<T> headerKey(String keyColumnName) {
        this.headerKeyColumn = keyColumnName;
        return this;
    }

    /**
     * ヘッダー行を探索する最大行数を設定（デフォルト: 10行）
     * headerKey()と組み合わせて使用する
     *
     * @param rows 探索する最大行数
     * @return このインスタンス
     */
    public ExcelStreamReader<T> headerSearchRows(int rows) {
        this.headerSearchRows = rows;
        return this;
    }

    /**
     * Streamを処理する
     *
     * @param <R> 戻り値の型
     * @param processor Streamを処理する関数
     * @return 処理結果
     * @throws IOException ファイル読み込みエラー
     */
    public <R> R process(Function<Stream<T>, R> processor) throws IOException {
        // ファイル存在チェック
        if (!Files.exists(filePath)) {
            throw new IOException("ファイルが存在しません: " + filePath);
        }
        if (!Files.isReadable(filePath)) {
            throw new IOException("ファイルを読み込めません: " + filePath);
        }

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = getSheet(workbook);
            if (sheet == null) {
                if (sheetName != null) {
                    throw new SheetNotFoundException(sheetName);
                } else {
                    throw new SheetNotFoundException(sheetIndex);
                }
            }

            List<T> data = readSheet(sheet);
            Stream<T> stream = data.stream();

            // データ行をスキップする処理
            if (skipLines > 0) {
                stream = stream.skip(skipLines);
            }

            // 呼び出し側でStreamを処理
            return processor.apply(stream);

        } catch (IOException e) {
            log.error("Excelファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Excel処理中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new IOException("Excel処理中にエラーが発生しました", e);
        }
    }

    /**
     * ワークブックからシートを取得
     */
    private Sheet getSheet(Workbook workbook) {
        if (sheetName != null) {
            return workbook.getSheet(sheetName);
        } else {
            return workbook.getSheetAt(sheetIndex);
        }
    }

    /**
     * シートからデータを読み込む
     */
    private List<T> readSheet(Sheet sheet) throws Exception {
        List<T> result = new ArrayList<>();

        if (sheet.getPhysicalNumberOfRows() == 0) {
            return result;
        }

        // ヘッダー行を検出
        int headerRowIndex = findHeaderRow(sheet);
        if (headerRowIndex == -1) {
            if (headerKeyColumn != null) {
                log.error("キー列 '{}' を持つヘッダー行が {}行以内に見つかりませんでした", headerKeyColumn, headerSearchRows);
                throw new HeaderNotFoundException(headerKeyColumn, headerSearchRows);
            } else {
                log.error("ヘッダー行が見つかりませんでした");
                throw new HeaderNotFoundException("ヘッダー行が見つかりませんでした");
            }
        }

        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            return result;
        }

        Map<Integer, String> headerMap = new HashMap<>();
        Map<String, Integer> columnMap = new HashMap<>();

        // ヘッダー情報を構築
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = getCellValue(cell);
                headerMap.put(i, headerValue);
                columnMap.put(headerValue, i);
            }
        }

        // キー列のインデックスを取得（終了判定用）
        Integer keyColumnIndex = null;
        if (headerKeyColumn != null) {
            keyColumnIndex = columnMap.get(headerKeyColumn);
            if (keyColumnIndex == null) {
                log.error("キー列 '{}' がヘッダー行に見つかりませんでした", headerKeyColumn);
                throw new KeyColumnNotFoundException(headerKeyColumn);
            }
        }

        // データ行を読み込む（ヘッダー行の次から）
        for (int rowIndex = headerRowIndex + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isEmptyRow(row)) {
                continue;
            }

            // キー列が指定されている場合、その列が空なら読み込みを終了
            if (keyColumnIndex != null) {
                Cell keyCell = row.getCell(keyColumnIndex);
                if (isEmptyCell(keyCell)) {
                    log.debug("キー列が空のため読み込みを終了: 行={}", rowIndex);
                    break;
                }
            }

            T bean = createBean(row, headerMap, columnMap);
            if (bean != null) {
                result.add(bean);
            }
        }

        return result;
    }

    /**
     * ヘッダー行を検出する
     * headerKeyColumnが指定されている場合は、その列名を持つ行を探す
     * 指定されていない場合は、最初の行をヘッダーとする
     *
     * @return ヘッダー行のインデックス（見つからない場合は-1）
     */
    private int findHeaderRow(Sheet sheet) {
        if (headerKeyColumn == null) {
            // キー列が指定されていない場合は最初の行をヘッダーとする
            return sheet.getFirstRowNum();
        }

        // 指定された行数の範囲内でキー列を探す
        int firstRow = sheet.getFirstRowNum();
        int searchLimit = Math.min(firstRow + headerSearchRows, sheet.getLastRowNum() + 1);

        for (int rowIndex = firstRow; rowIndex < searchLimit; rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            // 行内のすべてのセルをチェックして、キー列名があるか確認
            for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                Cell cell = row.getCell(cellIndex);
                if (cell != null) {
                    String cellValue = getCellValue(cell);
                    if (headerKeyColumn.equals(cellValue)) {
                        log.debug("ヘッダー行を検出: 行={}, キー列={}", rowIndex, headerKeyColumn);
                        return rowIndex;
                    }
                }
            }
        }

        return -1;
    }

    /**
     * セルが空かどうかを判定
     */
    private boolean isEmptyCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        String value = getCellValue(cell);
        return value == null || value.trim().isEmpty();
    }

    /**
     * 行からBeanを作成
     */
    private T createBean(Row row, Map<Integer, String> headerMap, Map<String, Integer> columnMap) throws Exception {
        // フィールドキャッシュの初期化（初回のみ）
        if (fieldCache == null) {
            buildFieldCache(columnMap);
        }

        T bean = beanClass.getDeclaredConstructor().newInstance();

        for (FieldMappingInfo mappingInfo : fieldCache.values()) {
            Integer columnIndex = null;

            if (usePositionMapping) {
                columnIndex = mappingInfo.position;
            } else {
                columnIndex = columnMap.get(mappingInfo.columnName);
            }

            if (columnIndex != null && columnIndex < row.getLastCellNum()) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    String columnName = headerMap.get(columnIndex);
                    if (columnName == null) {
                        columnName = "列" + columnIndex;
                    }
                    Object value = convertCellValue(cell, mappingInfo.field.getType(), row.getRowNum(), columnName);
                    mappingInfo.field.set(bean, value);
                }
            }
        }

        return bean;
    }

    /**
     * フィールドキャッシュを構築（初回のみ実行）
     */
    private void buildFieldCache(Map<String, Integer> columnMap) {
        fieldCache = new HashMap<>();
        Field[] fields = beanClass.getDeclaredFields();
        
        for (Field field : fields) {
            String columnName = null;
            Integer position = null;

            // 位置ベースマッピング
            CsvBindByPosition positionAnnotation = field.getAnnotation(CsvBindByPosition.class);
            if (positionAnnotation != null) {
                position = positionAnnotation.position();
            }

            // ヘッダーベースマッピング
            CsvBindByName nameAnnotation = field.getAnnotation(CsvBindByName.class);
            if (nameAnnotation != null) {
                columnName = nameAnnotation.column();
            }

            if (columnName != null || position != null) {
                fieldCache.put(field, new FieldMappingInfo(field, columnName, position));
            }
        }
    }

    /**
     * セルの値を文字列として取得
     */
    private String getCellValue(Cell cell) {
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
    private Object convertCellValue(Cell cell, Class<?> targetType, int rowIndex, String columnName)
            throws CellValueConversionException {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return getCellValue(cell);
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
            String cellValue = getCellValue(cell);
            log.error("セル値の変換に失敗しました: 行={}, 列='{}', 値='{}', 型={}",
                    rowIndex + 1, columnName, cellValue, targetType.getSimpleName());
            throw new CellValueConversionException(rowIndex, columnName, cellValue, targetType, e);
        }

        return null;
    }

    /**
     * 行が空かどうかを判定
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = getCellValue(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Date型をLocalDateTimeに変換
     */
    private LocalDateTime convertDateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    /**
     * Date型をLocalDateに変換
     */
    private LocalDate convertDateToLocalDate(Date date) {
        return convertDateToLocalDateTime(date).toLocalDate();
    }

    /**
     * 日付セルかどうかを判定し、適切な文字列に変換
     */
    private String formatDateCell(Cell cell) {
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
     * 数値型かどうかを判定
     */
    private boolean isNumericType(Class<?> type) {
        return NUMERIC_TYPES.contains(type);
    }

    /**
     * セルを数値型に変換
     */
    private Number convertToNumericType(Cell cell, Class<?> targetType) {
        Number result = parseNumericCell(cell, targetType);
        if (result != null) {
            return result;
        }
        return parseNumericString(getCellValue(cell), targetType);
    }

    /**
     * セルをBoolean型に変換
     */
    private Boolean convertToBooleanType(Cell cell) {
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        }
        return Boolean.parseBoolean(getCellValue(cell));
    }

    /**
     * セルをLocalDate型に変換
     */
    private LocalDate convertToLocalDate(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return convertDateToLocalDate(cell.getDateCellValue());
        }
        return LocalDate.parse(getCellValue(cell));
    }

    /**
     * セルをLocalDateTime型に変換
     */
    private LocalDateTime convertToLocalDateTime(Cell cell) {
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return convertDateToLocalDateTime(cell.getDateCellValue());
        }
        return LocalDateTime.parse(getCellValue(cell));
    }

    /**
     * 数値セルから値を取得し、指定された型にキャスト
     */
    private Number parseNumericCell(Cell cell, Class<?> targetType) {
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
     */
    private Number parseNumericString(String value, Class<?> targetType) {
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
     * フィールドマッピング情報を保持する内部クラス
     */
    private static class FieldMappingInfo {
        final Field field;
        final String columnName;
        final Integer position;

        FieldMappingInfo(Field field, String columnName, Integer position) {
            this.field = field;
            this.field.setAccessible(true);
            this.columnName = columnName;
            this.position = position;
        }
    }
}
