package com.example.excel.reader;

import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

import com.example.common.converter.CellValueConverter;
import com.example.common.mapping.FieldMappingCache;
import com.example.exception.UncheckedExcelException;

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
    
    private ExcelHeaderDetector headerDetector = null;
    private FieldMappingCache fieldMappingCache = null;
    private boolean initialized = false;
    private T nextBean = null;
    private boolean hasNext = false;
    private boolean hasNextComputed = false;

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
                           boolean usePositionMapping) {
        this.rowIterator = rowIterator;
        this.beanClass = beanClass;
        this.headerKeyColumn = headerKeyColumn;
        this.headerSearchRows = headerSearchRows;
        this.usePositionMapping = usePositionMapping;
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
            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                
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
                    Object value = CellValueConverter.convertCellValue(cell, mappingInfo.field.getType(), row.getRowNum(), columnName);
                    mappingInfo.field.set(bean, value);
                }
            }
        }

        return bean;
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

