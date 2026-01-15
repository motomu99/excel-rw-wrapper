package com.example.excel.reader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.dhatim.fastexcel.reader.Row;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import com.example.common.converter.CellValueConverter;
import com.example.common.mapping.FieldMappingCache;
import com.example.exception.HeaderNotFoundException;
import com.example.exception.UncheckedExcelException;
import com.opencsv.bean.AbstractBeanField;
import com.opencsv.exceptions.CsvValidationException;

import lombok.extern.slf4j.Slf4j;

/**
 * Excelシートの行をBeanに変換するIterator（fastexcel版）
 * 
 * <p>ストリーミング処理で行ごとにBeanを生成し、メモリ効率を向上させます。
 * ヘッダー情報とフィールドマッピングを使用してBeanを生成します。</p>
 * 
 * @param <T> Beanの型
 */
@Slf4j
public class FastExcelRowIterator<T> implements Iterator<T> {

    private final Iterator<Row> rowIterator;
    private final Class<T> beanClass;
    private final String headerKeyColumn;
    private final int headerSearchRows;
    private final boolean usePositionMapping;
    private final boolean treatFirstRowAsData;
    
    private FastExcelHeaderDetector headerDetector = null;
    private FieldMappingCache fieldMappingCache = null;
    private boolean initialized = false;
    private T nextBean = null;
    // Iteratorパターンでフィールド名とメソッド名が同じになるのは一般的なパターン
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private boolean hasNext = false;
    private boolean hasNextComputed = false;
    private Row pendingFirstDataRow = null;
    
    /** 列数チェックを有効にするかどうか */
    private final boolean validateColumnCount;
    
    /** エラー行の情報を収集するリスト */
    private final List<ExcelReadError> errors;
    
    /** 期待される列数（ヘッダー行の列数） */
    private int expectedColumnCount = -1;
    
    /** 現在の行番号（0始まり） */
    private int currentRowNum = -1;

    /**
     * FastExcelRowIteratorを作成
     * 
     * @param rowIterator 行のIterator
     * @param beanClass マッピング先のBeanクラス
     * @param headerKeyColumn キー列名（nullの場合は最初の行をヘッダーとする）
     * @param headerSearchRows ヘッダー探索行数
     * @param usePositionMapping 位置ベースマッピングを使用するかどうか
     * @param treatFirstRowAsData 最初の行をデータとして扱うかどうか
     * @param validateColumnCount 列数チェックを有効にするかどうか
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public FastExcelRowIterator(Iterator<Row> rowIterator, Class<T> beanClass, 
                           String headerKeyColumn, int headerSearchRows, 
                           boolean usePositionMapping, boolean treatFirstRowAsData,
                           boolean validateColumnCount) {
        // Iteratorはこのクラス内で読み取り専用として使用されるため、コピーを作成する必要はない
        this.rowIterator = rowIterator;
        this.beanClass = beanClass;
        this.headerKeyColumn = headerKeyColumn;
        this.headerSearchRows = headerSearchRows;
        this.usePositionMapping = usePositionMapping;
        this.treatFirstRowAsData = treatFirstRowAsData;
        this.validateColumnCount = validateColumnCount;
        this.errors = validateColumnCount ? new ArrayList<>() : null;
    }
    
    /**
     * FastExcelRowIteratorを作成（後方互換性のため）
     * 
     * @param rowIterator 行のIterator
     * @param beanClass マッピング先のBeanクラス
     * @param headerKeyColumn キー列名（nullの場合は最初の行をヘッダーとする）
     * @param headerSearchRows ヘッダー探索行数
     * @param usePositionMapping 位置ベースマッピングを使用するかどうか
     * @param treatFirstRowAsData 最初の行をデータとして扱うかどうか
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public FastExcelRowIterator(Iterator<Row> rowIterator, Class<T> beanClass, 
                           String headerKeyColumn, int headerSearchRows, 
                           boolean usePositionMapping, boolean treatFirstRowAsData) {
        this(rowIterator, beanClass, headerKeyColumn, headerSearchRows, 
             usePositionMapping, treatFirstRowAsData, false);
    }
    
    /**
     * エラー行の情報を取得
     * 
     * @return エラー行の情報リスト（列数チェックが無効の場合は空リスト）
     */
    public List<ExcelReadError> getErrors() {
        return errors != null ? new ArrayList<>(errors) : new ArrayList<>();
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
            Row row = fetchNextRow();
            while (row != null) {
                currentRowNum = row.getRowNum() - 1;
                
                // 空行をスキップ
                if (isEmptyRow(row)) {
                    row = fetchNextRow();
                    continue;
                }

                // 列数チェック
                if (validateColumnCount && expectedColumnCount >= 0) {
                    int actualColumnCount = getColumnCount(row);
                    if (actualColumnCount != expectedColumnCount) {
                    int lineNumber = currentRowNum + 1; // 1始まりに変換
                        ExcelReadError error = ExcelReadError.columnCountMismatch(
                            lineNumber, expectedColumnCount, actualColumnCount
                        );
                        errors.add(error);
                        log.warn("列数不一致を検出: 行={}, 期待値={}, 実際={}", 
                                lineNumber, expectedColumnCount, actualColumnCount);
                        // エラー行はスキップして次の行へ
                        row = fetchNextRow();
                        continue;
                    }
                }

                // キー列が指定されている場合、その列が空なら終了
                Integer keyColumnIndex = headerDetector.getKeyColumnIndex();
                if (keyColumnIndex != null) {
                    if (isEmptyCell(row, keyColumnIndex)) {
                        log.debug("キー列が空のため読み込みを終了: 行={}", currentRowNum);
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
                row = fetchNextRow();
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
        headerDetector = new FastExcelHeaderDetector(headerKeyColumn, headerSearchRows);
        boolean headerFound = headerDetector.detectHeader(rowIterator);
        
        if (!headerFound) {
            // 空のシートとして扱う
            return;
        }

        // ヘッダー行のインデックスを取得
        currentRowNum = headerDetector.getHeaderRowIndex();

        // フィールドキャッシュを構築
        fieldMappingCache = new FieldMappingCache(beanClass);

        // ヘッダー検証: Beanで定義されたカラムがExcelヘッダーに存在するかチェック（ヘッダーマッピングの場合のみ）
        if (!usePositionMapping) {
            Map<String, Integer> columnMap = headerDetector.getColumnMap();
            for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
                if (mappingInfo.getColumnName() != null) {
                    String normalizedColumnName = FastExcelHeaderDetector.normalizeHeaderValue(mappingInfo.getColumnName());
                    if (!columnMap.containsKey(normalizedColumnName)) {
                        log.error("必須ヘッダーカラム '{}' が見つかりません", mappingInfo.getColumnName());
                        throw new HeaderNotFoundException("必須ヘッダーカラムが見つかりません: " + mappingInfo.getColumnName());
                    }
                }
            }
        }

        // 列数チェック用に期待される列数を設定
        if (validateColumnCount && headerDetector.getHeaderRow() != null) {
            expectedColumnCount = getColumnCount(headerDetector.getHeaderRow());
            log.debug("列数チェックを有効化: 期待される列数={}", expectedColumnCount);
        }

        if (treatFirstRowAsData && headerKeyColumn == null && usePositionMapping && headerDetector.getHeaderRow() != null) {
            pendingFirstDataRow = headerDetector.getHeaderRow();
        }
    }

    /**
     * セルが空かどうかを判定
     */
    private boolean isEmptyCell(Row row, int index) {
        String value = CellValueConverter.getCellValueAsString(row, index);
        return value == null || value.trim().isEmpty();
    }

    /**
     * 行からBeanを作成
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private T createBean(Row row, Map<Integer, String> headerMap, Map<String, Integer> columnMap) throws Exception {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        for (FieldMappingCache.FieldMappingInfo mappingInfo : fieldMappingCache.getCache().values()) {
            Integer columnIndex = null;

                if (usePositionMapping) {
                    columnIndex = mappingInfo.getPosition();
                } else {
                    String normalizedColumnName = FastExcelHeaderDetector.normalizeHeaderValue(mappingInfo.getColumnName());
                    columnIndex = columnMap.get(normalizedColumnName);
            }

            if (columnIndex != null) {
                String columnName = headerMap.get(columnIndex);
                if (columnName == null) {
                    columnName = "列" + columnIndex;
                }
                
                // セルから文字列を取得
                String stringValue = CellValueConverter.getCellValueAsString(row, columnIndex);
                
                // セルが存在しない場合はスキップ
                if (stringValue == null || stringValue.isEmpty()) {
                    continue;
                }
                
                // Pre-assignment バリデータが指定されている場合、バリデーションを実行
                if (mappingInfo.getValidatorClass() != null && stringValue != null && !stringValue.isEmpty()) {
                    try {
                        // キャッシュされたコンストラクタとメソッドを使用
                        Object validator = mappingInfo.getValidatorConstructor().newInstance();
                        java.lang.reflect.Method validateMethod = mappingInfo.getValidatorMethod();
                        
                        // バリデータメソッドのパラメータ数チェック（意図が明確なリテラル使用）
                        final int singleParameterCount = 1;
                        if (validateMethod.getParameterCount() == singleParameterCount) {
                            validateMethod.invoke(validator, stringValue);
                        } else {
                            validateMethod.invoke(validator, stringValue, columnName);
                        }
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        if (cause instanceof CsvValidationException) {
                            log.error("バリデーションエラー: 行={}, 列='{}', 値='{}', エラー={}",
                                    currentRowNum + 1, columnName, stringValue, cause.getMessage());
                            throw new UncheckedExcelException(
                                    String.format("バリデーションエラー: 行=%d, 列='%s', 値='%s'", 
                                            currentRowNum + 1, columnName, stringValue), cause);
                        }
                        throw new UncheckedExcelException(
                                String.format("バリデータの実行に失敗: 行=%d, 列='%s'", 
                                        currentRowNum + 1, columnName), e);
                    } catch (Exception e) {
                            log.error("バリデータのインスタンス化または実行に失敗: 行={}, 列='{}', バリデータクラス={}",
                                    currentRowNum + 1, columnName, mappingInfo.getValidatorClass().getName(), e);
                        throw new UncheckedExcelException(
                                String.format("バリデータの実行に失敗: 行=%d, 列='%s'", 
                                        currentRowNum + 1, columnName), e);
                    }
                }
                
                // カスタムコンバーターが指定されている場合は、事前変換を実行
                Object value;
                if (mappingInfo.getConverterClass() != null) {
                    try {
                        // キャッシュされたコンストラクタとメソッドを使用
                        AbstractBeanField<?, ?> converter = mappingInfo.getConverterConstructor().newInstance();
                        java.lang.reflect.Method convertMethod = mappingInfo.getConverterMethod();

                        value = convertMethod.invoke(converter, stringValue);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        Throwable cause = e.getCause();
                        log.error("カスタムコンバーター実行エラー: 行={}, 列='{}', 値='{}', エラー={}",
                                currentRowNum + 1, columnName, stringValue,
                                cause != null ? cause.getMessage() : e.getMessage());
                        throw new UncheckedExcelException(
                                String.format("カスタムコンバーター実行エラー: 行=%d, 列='%s', 値='%s'",
                                        currentRowNum + 1, columnName, stringValue),
                                cause != null ? cause : e);
                    } catch (Exception e) {
                        log.error("カスタムコンバーターのインスタンス化または実行に失敗: 行={}, 列='{}', コンバータークラス={}",
                                currentRowNum + 1, columnName, mappingInfo.getConverterClass().getName(), e);
                        throw new UncheckedExcelException(
                                String.format("カスタムコンバーターの実行に失敗: 行=%d, 列='%s'",
                                        currentRowNum + 1, columnName),
                                e);
                    }
                } else {
                    // カスタムコンバーターが無い場合は従来の型変換ロジックを使用
                    value = CellValueConverter.convertCellValue(
                            row, columnIndex, mappingInfo.getField().getType(), currentRowNum, columnName);
                }

                // フィールドに設定
                mappingInfo.getField().set(bean, value);
            }
        }

        // 行番号フィールドが存在する場合は行番号を設定
        if (fieldMappingCache.hasLineNumberField()) {
            java.lang.reflect.Field lineNumberField = fieldMappingCache.getLineNumberField();
            // currentRowNumは0始まりなので、1始まりに変換
            int lineNumber = currentRowNum + 1;

            // フィールドの型に応じて値を設定
            Class<?> fieldType = lineNumberField.getType();
            if (fieldType == Integer.class || fieldType == int.class) {
                lineNumberField.set(bean, lineNumber);
            } else if (fieldType == Long.class || fieldType == long.class) {
                lineNumberField.set(bean, (long) lineNumber);
            }
        }

        return bean;
    }

    @SuppressWarnings("PMD.NullAssignment")
    private Row fetchNextRow() {
        if (pendingFirstDataRow != null) {
            Row row = pendingFirstDataRow;
            // 一度使用したらnullに設定してクリーンアップ（意図的な実装）
            pendingFirstDataRow = null;
            return row;
        }
        return rowIterator.hasNext() ? rowIterator.next() : null;
    }

    /**
     * 行が空かどうかを判定
     */
    private boolean isEmptyRow(Row row) {
        // 適切な範囲でセルをチェック（最大1000列まで）
        int cellCount = row.getCellCount();
        for (int i = 0; i < cellCount; i++) {
            String value = CellValueConverter.getCellValueAsString(row, i);
            if (value == null) {
                // セルがなくなったら終了
                if (i == 0) {
                    // 最初のセルがnullなら空行として扱う
                    return true;
                }
                // セルがなくなったら終了
                break;
            }
            if (value != null && !value.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 行の列数を取得
     * 
     * @param row 行
     * @return 列数（最後のセルインデックス + 1）
     */
    private int getColumnCount(Row row) {
        return row.getCellCount();
    }
}
