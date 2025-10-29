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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

import com.github.pjfanning.xlsx.StreamingReader;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

import com.example.csv.exception.HeaderNotFoundException;
import com.example.csv.exception.KeyColumnNotFoundException;
import com.example.csv.exception.SheetNotFoundException;
import com.example.csv.exception.CellValueConversionException;
import com.example.csv.exception.UncheckedExcelException;

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
     * Streamを処理する（メモリ効率の良いストリーミング処理）
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
             Workbook workbook = StreamingReader.builder()
                 .rowCacheSize(100)    // メモリに保持する行数
                 .bufferSize(4096)      // バッファサイズ
                 .open(fis)) {

            Sheet sheet;
            try {
                sheet = getSheet(workbook);
            } catch (com.github.pjfanning.xlsx.exceptions.MissingSheetException e) {
                // Streaming Readerの例外を自前の例外に変換
                if (sheetName != null) {
                    throw new SheetNotFoundException(sheetName);
                } else {
                    throw new SheetNotFoundException(sheetIndex);
                }
            }
            
            if (sheet == null) {
                if (sheetName != null) {
                    throw new SheetNotFoundException(sheetName);
                } else {
                    throw new SheetNotFoundException(sheetIndex);
                }
            }

            // ストリーミング処理用のIteratorを作成
            Iterator<T> iterator = createStreamingIterator(sheet);
            
            // IteratorをStreamに変換
            Stream<T> stream = StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, 0), 
                false
            );

            // データ行をスキップする処理
            if (skipLines > 0) {
                stream = stream.skip(skipLines);
            }

            // 呼び出し側でStreamを処理
            try {
                return processor.apply(stream);
            } catch (UncheckedExcelException e) {
                // ストリーミング処理中に発生したExcel固有の例外を元に戻す
                Throwable cause = e.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else if (cause instanceof Exception) {
                    throw new IOException("Excel処理中にエラーが発生しました", cause);
                } else {
                    throw e;
                }
            }

        } catch (IOException e) {
            log.error("Excelファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Excel処理中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new IOException("Excel処理中にエラーが発生しました", e);
        }
    }

    /**
     * ストリーミング処理用のIteratorを作成
     * 行ごとにBeanを生成することでメモリ効率を向上
     */
    private Iterator<T> createStreamingIterator(Sheet sheet) {
        return new Iterator<T>() {
            private final Iterator<Row> rowIterator = sheet.iterator();
            private Row headerRow = null;
            private Map<Integer, String> headerMap = null;
            private Map<String, Integer> columnMap = null;
            private Integer keyColumnIndex = null;
            private boolean initialized = false;
            private T nextBean = null;
            private boolean hasNext = false;
            private boolean hasNextComputed = false;

            @Override
            public boolean hasNext() {
                if (hasNextComputed) {
                    return hasNext;
                }

                // 初期化（ヘッダー行の検出）
                if (!initialized) {
                    try {
                        initializeHeader();
                    } catch (Exception e) {
                        throw new UncheckedExcelException("ヘッダー初期化エラー", e);
                    }
                    initialized = true;
                }

                // 空のシートの場合（headerMapがnull）
                if (headerMap == null || columnMap == null) {
                    hasNext = false;
                    hasNextComputed = true;
                    return false;
                }

                // 次のBeanを取得
                try {
                    while (rowIterator.hasNext()) {
                        Row row = rowIterator.next();
                        
                        // 空行をスキップ
                        if (row == null || isEmptyRow(row)) {
                            continue;
                        }

                        // キー列が指定されている場合、その列が空なら終了
                        if (keyColumnIndex != null) {
                            Cell keyCell = row.getCell(keyColumnIndex);
                            if (isEmptyCell(keyCell)) {
                                log.debug("キー列が空のため読み込みを終了: 行={}", row.getRowNum());
                                hasNext = false;
                                hasNextComputed = true;
                                return false;
                            }
                        }

                        // Beanを作成
                        nextBean = createBean(row, headerMap, columnMap);
                        if (nextBean != null) {
                            hasNext = true;
                            hasNextComputed = true;
                            return true;
                        }
                    }
                } catch (Exception e) {
                    throw new UncheckedExcelException("Bean作成エラー", e);
                }

                hasNext = false;
                hasNextComputed = true;
                return false;
            }

            @Override
            public T next() {
                if (!hasNextComputed) {
                    hasNext();
                }
                if (!hasNext) {
                    throw new NoSuchElementException();
                }
                hasNextComputed = false;
                return nextBean;
            }

            /**
             * ヘッダー行を初期化
             */
            private void initializeHeader() throws Exception {
                int headerRowIndex = findHeaderRowInStream();
                if (headerRowIndex == -1) {
                    if (headerKeyColumn != null) {
                        log.error("キー列 '{}' を持つヘッダー行が {}行以内に見つかりませんでした", headerKeyColumn, headerSearchRows);
                        throw new HeaderNotFoundException(headerKeyColumn, headerSearchRows);
                    } else {
                        // キー列が指定されていない場合、ヘッダーが見つからなければ空のシートとして扱う
                        log.debug("ヘッダー行が見つかりませんでした（空のシート）");
                        return;  // 空のIteratorとして動作
                    }
                }

                if (headerRow == null) {
                    // 空のシートとして扱う
                    log.debug("ヘッダー行がnullです（空のシート）");
                    return;
                }

                headerMap = new HashMap<>();
                columnMap = new HashMap<>();

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
                if (headerKeyColumn != null) {
                    keyColumnIndex = columnMap.get(headerKeyColumn);
                    if (keyColumnIndex == null) {
                        log.error("キー列 '{}' がヘッダー行に見つかりませんでした", headerKeyColumn);
                        throw new KeyColumnNotFoundException(headerKeyColumn);
                    }
                }

                // フィールドキャッシュを構築
                if (fieldCache == null) {
                    buildFieldCache(columnMap);
                }
            }

            /**
             * ストリーミング処理でヘッダー行を検出
             */
            private int findHeaderRowInStream() {
                if (headerKeyColumn == null) {
                    // キー列が指定されていない場合は最初の行をヘッダーとする
                    if (rowIterator.hasNext()) {
                        headerRow = rowIterator.next();
                        return headerRow.getRowNum();
                    }
                    return -1;
                }

                // 指定された行数の範囲内でキー列を探す
                int rowCount = 0;
                while (rowIterator.hasNext() && rowCount < headerSearchRows) {
                    Row row = rowIterator.next();
                    rowCount++;
                    
                    if (row == null) {
                        continue;
                    }

                    // 行内のすべてのセルをチェックして、キー列名があるか確認
                    for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                        Cell cell = row.getCell(cellIndex);
                        if (cell != null) {
                            String cellValue = getCellValue(cell);
                            if (headerKeyColumn.equals(cellValue)) {
                                log.debug("ヘッダー行を検出: 行={}, キー列={}", row.getRowNum(), headerKeyColumn);
                                headerRow = row;
                                return row.getRowNum();
                            }
                        }
                    }
                }

                return -1;
            }
        };
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
