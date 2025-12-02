package com.example.excel.reader;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import com.example.common.converter.CellValueConverter;
import com.example.common.mapping.FieldMappingCache;
import com.example.exception.HeaderNotFoundException;
import com.example.exception.UncheckedExcelException;
import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Excelシートの行をBeanに変換するIterator
 * 
 * <p>ストリーミング処理で行ごとにBeanを生成し、メモリ効率を向上させます。
 * ヘッダー情報とフィールドマッピングを使用してBeanを生成します。</p>
 * 
 * @param <T> Beanの型
 */
@Slf4j
public class ExcelRowIterator<T> implements Iterator<T> {

    private final Iterator<Row> rowIterator;
    private final Class<T> beanClass;
    private final String headerKeyColumn;
    private final int headerSearchRows;
    private final boolean usePositionMapping;
    private final boolean treatFirstRowAsData;
    
    private ExcelHeaderDetector headerDetector = null;
    private FieldMappingCache fieldMappingCache = null;
    private boolean initialized = false;
    private T nextBean = null;
    private boolean hasNext = false;
    private boolean hasNextComputed = false;
    private Row pendingFirstDataRow = null;

    /**
     * ExcelRowIteratorを作成
     * 
     * @param rowIterator 行のIterator
     * @param beanClass マッピング先のBeanクラス
     * @param headerKeyColumn キー列名（nullの場合は最初の行をヘッダーとする）
     * @param headerSearchRows ヘッダー探索行数
     * @param usePositionMapping 位置ベースマッピングを使用するかどうか
     */
    public ExcelRowIterator(Iterator<Row> rowIterator, Class<T> beanClass, 
                           String headerKeyColumn, int headerSearchRows, 
                           boolean usePositionMapping, boolean treatFirstRowAsData) {
        this.rowIterator = rowIterator;
        this.beanClass = beanClass;
        this.headerKeyColumn = headerKeyColumn;
        this.headerSearchRows = headerSearchRows;
        this.usePositionMapping = usePositionMapping;
        this.treatFirstRowAsData = treatFirstRowAsData;
    }

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

        // 空のシートの場合（headerDetectorがnullまたはヘッダーマップがnull）
        if (headerDetector == null || headerDetector.getHeaderMap() == null || headerDetector.getColumnMap() == null) {
            hasNext = false;
            hasNextComputed = true;
            return false;
        }

        // 次のBeanを取得
        try {
            Row row;
            while ((row = fetchNextRow()) != null) {
                
                // 空行をスキップ
                if (row == null || isEmptyRow(row)) {
                    continue;
                }

                // キー列が指定されている場合、その列が空なら終了
                Integer keyColumnIndex = headerDetector.getKeyColumnIndex();
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
                nextBean = createBean(row, headerDetector.getHeaderMap(), headerDetector.getColumnMap());
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
        headerDetector = new ExcelHeaderDetector(headerKeyColumn, headerSearchRows);
        boolean headerFound = headerDetector.detectHeader(rowIterator);
        
        if (!headerFound) {
            // 空のシートとして扱う
            return;
        }

        // フィールドキャッシュを構築
        fieldMappingCache = new FieldMappingCache(beanClass);

        // ヘッダー検証: Beanで定義されたカラムがExcelヘッダーに存在するかチェック（ヘッダーマッピングの場合のみ）
        if (!usePositionMapping) {
            Map<String, Integer> columnMap = headerDetector.getColumnMap();
            for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                if (mappingInfo.columnName != null && !columnMap.containsKey(mappingInfo.columnName)) {
                    log.error("必須ヘッダーカラム '{}' が見つかりません", mappingInfo.columnName);
                    throw new HeaderNotFoundException("必須ヘッダーカラムが見つかりません: " + mappingInfo.columnName);
                }
            }
        }

        if (treatFirstRowAsData && headerKeyColumn == null && usePositionMapping && headerDetector.getHeaderRow() != null) {
            pendingFirstDataRow = headerDetector.getHeaderRow();
        }
    }

    /**
     * セルが空かどうかを判定
     */
    private boolean isEmptyCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        String value = CellValueConverter.getCellValueAsString(cell);
        return value == null || value.trim().isEmpty();
    }

    /**
     * 行からBeanを作成
     */
    private T createBean(Row row, Map<Integer, String> headerMap, Map<String, Integer> columnMap) throws Exception {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
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
                    
                    // セルから文字列を取得
                    String stringValue = CellValueConverter.getCellValueAsString(cell);
                    
                    // Pre-assignment バリデータが指定されている場合、バリデーションを実行
                    if (mappingInfo.validatorClass != null && stringValue != null && !stringValue.isEmpty()) {
                        try {
                            // キャッシュされたコンストラクタとメソッドを使用
                            Object validator = mappingInfo.validatorConstructor.newInstance();
                            java.lang.reflect.Method validateMethod = mappingInfo.validatorMethod;
                            
                            if (validateMethod.getParameterCount() == 1) {
                                validateMethod.invoke(validator, stringValue);
                            } else {
                                validateMethod.invoke(validator, stringValue, columnName);
                            }
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            Throwable cause = e.getCause();
                            if (cause instanceof CsvValidationException) {
                                log.error("バリデーションエラー: 行={}, 列='{}', 値='{}', エラー={}",
                                        row.getRowNum() + 1, columnName, stringValue, cause.getMessage());
                                throw new UncheckedExcelException(
                                        String.format("バリデーションエラー: 行=%d, 列='%s', 値='%s'", 
                                                row.getRowNum() + 1, columnName, stringValue), cause);
                            }
                            throw new UncheckedExcelException(
                                    String.format("バリデータの実行に失敗: 行=%d, 列='%s'", 
                                            row.getRowNum() + 1, columnName), e);
                        } catch (Exception e) {
                            log.error("バリデータのインスタンス化または実行に失敗: 行={}, 列='{}', バリデータクラス={}",
                                    row.getRowNum() + 1, columnName, mappingInfo.validatorClass.getName(), e);
                            throw new UncheckedExcelException(
                                    String.format("バリデータの実行に失敗: 行=%d, 列='%s'", 
                                            row.getRowNum() + 1, columnName), e);
                        }
                    }
                    
                    // カスタムコンバーターが指定されている場合は、事前変換を実行
                    Object value;
                    if (mappingInfo.converterClass != null) {
                        try {
                            // キャッシュされたコンストラクタとメソッドを使用
                            AbstractBeanField<?, ?> converter = mappingInfo.converterConstructor.newInstance();
                            java.lang.reflect.Method convertMethod = mappingInfo.converterMethod;

                            value = convertMethod.invoke(converter, stringValue);
                        } catch (java.lang.reflect.InvocationTargetException e) {
                            Throwable cause = e.getCause();
                            log.error("カスタムコンバーター実行エラー: 行={}, 列='{}', 値='{}', エラー={}",
                                    row.getRowNum() + 1, columnName, stringValue,
                                    cause != null ? cause.getMessage() : e.getMessage());
                            throw new UncheckedExcelException(
                                    String.format("カスタムコンバーター実行エラー: 行=%d, 列='%s', 値='%s'",
                                            row.getRowNum() + 1, columnName, stringValue),
                                    cause != null ? cause : e);
                        } catch (Exception e) {
                            log.error("カスタムコンバーターのインスタンス化または実行に失敗: 行={}, 列='{}', コンバータークラス={}",
                                    row.getRowNum() + 1, columnName, mappingInfo.converterClass.getName(), e);
                            throw new UncheckedExcelException(
                                    String.format("カスタムコンバーターの実行に失敗: 行=%d, 列='%s'",
                                            row.getRowNum() + 1, columnName),
                                    e);
                        }
                    } else {
                        // カスタムコンバーターが無い場合は従来の型変換ロジックを使用
                        value = CellValueConverter.convertCellValue(
                                cell, mappingInfo.field.getType(), row.getRowNum(), columnName);
                    }

                    // フィールドに設定
                    mappingInfo.field.set(bean, value);
                }
            }
        }

        return bean;
    }

    private Row fetchNextRow() {
        if (pendingFirstDataRow != null) {
            Row row = pendingFirstDataRow;
            pendingFirstDataRow = null;
            return row;
        }
        return rowIterator.hasNext() ? rowIterator.next() : null;
    }

    /**
     * 行が空かどうかを判定
     */
    private boolean isEmptyRow(Row row) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String value = CellValueConverter.getCellValueAsString(cell);
                if (value != null && !value.trim().isEmpty()) {
                    return false;
                }
            }
        }
        return true;
    }
}

