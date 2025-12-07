package com.example.excel.reader;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;

import com.example.common.converter.CellValueConverter;
import com.example.exception.HeaderNotFoundException;
import com.example.exception.KeyColumnNotFoundException;

import lombok.extern.slf4j.Slf4j;

/**
 * Excelシートのヘッダー行を検出するクラス
 * 
 * <p>キー列を指定してヘッダー行を探索する機能を提供します。
 * 検出したヘッダー情報（カラム名→インデックスのマップ）を保持します。</p>
 */
@Slf4j
public class ExcelHeaderDetector {

    /** デフォルトのヘッダー探索行数 */
    private static final int DEFAULT_HEADER_SEARCH_ROWS = 10;

    private final String headerKeyColumn;
    private final int headerSearchRows;
    
    private Row headerRow = null;
    private Map<Integer, String> headerMap = null;
    private Map<String, Integer> columnMap = null;
    private Integer keyColumnIndex = null;

    /**
     * ヘッダー検出器を作成（キー列指定なし）
     */
    public ExcelHeaderDetector() {
        this(null, DEFAULT_HEADER_SEARCH_ROWS);
    }

    /**
     * ヘッダー検出器を作成
     * 
     * @param headerKeyColumn キー列名（nullの場合は最初の行をヘッダーとする）
     * @param headerSearchRows ヘッダー探索行数
     */
    public ExcelHeaderDetector(String headerKeyColumn, int headerSearchRows) {
        this.headerKeyColumn = headerKeyColumn;
        this.headerSearchRows = headerSearchRows;
    }

    /**
     * ヘッダー行を検出する
     * 
     * @param rowIterator 行のIterator
     * @return ヘッダー行が見つかった場合true
     * @throws HeaderNotFoundException キー列を持つヘッダー行が見つからない場合
     * @throws KeyColumnNotFoundException ヘッダー行にキー列が存在しない場合
     */
    public boolean detectHeader(Iterator<Row> rowIterator) throws HeaderNotFoundException, KeyColumnNotFoundException {
        int headerRowIndex = findHeaderRowInStream(rowIterator);
        
        if (headerRowIndex == -1) {
            if (headerKeyColumn != null) {
                log.error("キー列 '{}' を持つヘッダー行が {}行以内に見つかりませんでした", headerKeyColumn, headerSearchRows);
                throw new HeaderNotFoundException(headerKeyColumn, headerSearchRows);
            } else {
                // キー列が指定されていない場合、ヘッダーが見つからなければ空のシートとして扱う
                log.debug("ヘッダー行が見つかりませんでした（空のシート）");
                return false;
            }
        }

        if (headerRow == null) {
            // 空のシートとして扱う
            log.debug("ヘッダー行がnullです（空のシート）");
            return false;
        }

        buildHeaderMaps();
        
        return true;
    }

    /**
     * ストリーミング処理でヘッダー行を検出
     * 
     * @param rowIterator 行のIterator
     * @return ヘッダー行のインデックス（見つからない場合-1）
     */
    private int findHeaderRowInStream(Iterator<Row> rowIterator) {
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
                    String cellValue = CellValueConverter.getCellValueAsString(cell);
                    // nullチェックと空文字チェック（念のため）
                    if (cellValue != null && !cellValue.isEmpty()) {
                        // 空白を除去して比較
                        if (headerKeyColumn.trim().equals(cellValue.trim())) {
                            log.debug("ヘッダー行を検出: 行={}, キー列={}", row.getRowNum(), headerKeyColumn);
                            headerRow = row;
                            return row.getRowNum();
                        }
                    }
                }
            }
        }

        return -1;
    }

    /**
     * ヘッダーマップを構築
     * 
     * @throws KeyColumnNotFoundException キー列がヘッダー行に見つからない場合
     */
    private void buildHeaderMaps() throws KeyColumnNotFoundException {
        headerMap = new HashMap<>();
        columnMap = new HashMap<>();

        // ヘッダー情報を構築
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                String headerValue = CellValueConverter.getCellValueAsString(cell);
                if (headerValue != null) {
                    // カラム名の空白を除去してマップに格納
                    String trimmedHeader = headerValue.trim();
                    if (!trimmedHeader.isEmpty()) {
                        headerMap.put(i, trimmedHeader);
                        columnMap.put(trimmedHeader, i);
                    }
                }
            }
        }

        // キー列のインデックスを取得（終了判定用）
        if (headerKeyColumn != null) {
            // 比較時もトリムされた値を使用
            keyColumnIndex = columnMap.get(headerKeyColumn.trim());
            if (keyColumnIndex == null) {
                log.error("キー列 '{}' がヘッダー行に見つかりませんでした", headerKeyColumn);
                throw new KeyColumnNotFoundException(headerKeyColumn);
            }
        }
    }

    /**
     * ヘッダー行を取得
     * 
     * @return ヘッダー行
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Row getHeaderRow() {
        return headerRow;
    }

    /**
     * ヘッダーマップ（インデックス→カラム名）を取得
     * 
     * @return ヘッダーマップ
     */
    public Map<Integer, String> getHeaderMap() {
        return headerMap != null ? Collections.unmodifiableMap(headerMap) : null;
    }

    /**
     * カラムマップ（カラム名→インデックス）を取得
     * 
     * @return カラムマップ
     */
    public Map<String, Integer> getColumnMap() {
        return columnMap != null ? Collections.unmodifiableMap(columnMap) : null;
    }

    /**
     * キー列のインデックスを取得
     * 
     * @return キー列のインデックス（キー列が指定されていない場合null）
     */
    public Integer getKeyColumnIndex() {
        return keyColumnIndex;
    }
}

