package com.example.excel.reader;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
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
    private static final DataFormatter HEADER_FORMATTER = new DataFormatter();

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
        String normalizedHeaderKey = normalizeHeaderValue(headerKeyColumn);
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
                    for (String candidate : getHeaderCandidates(cell)) {
                        if (normalizedHeaderKey != null && normalizedHeaderKey.equals(candidate)) {
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
     * @throws HeaderNotFoundException 正規化後のヘッダー名が衝突した場合
     */
    private void buildHeaderMaps() throws KeyColumnNotFoundException, HeaderNotFoundException {
        headerMap = new HashMap<>();
        columnMap = new HashMap<>();

        // ヘッダー情報を構築
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null) {
                List<String> candidates = getHeaderCandidates(cell);
                if (!candidates.isEmpty()) {
                    String originalHeader = HEADER_FORMATTER.formatCellValue(cell);
                    headerMap.put(i, candidates.get(0));
                    for (String candidate : candidates) {
                        Integer existingIndex = columnMap.putIfAbsent(candidate, i);
                        if (existingIndex != null && !existingIndex.equals(i)) {
                            String existingOriginal = headerMap.get(existingIndex);
                            log.error("正規化後のヘッダー名が衝突しました: 正規化後='{}', 列{}='{}', 列{}='{}'",
                                    candidate, existingIndex, existingOriginal, i, originalHeader);
                            throw new HeaderNotFoundException(
                                    String.format("正規化後のヘッダー名が衝突しました: 正規化後='%s', 列%d='%s', 列%d='%s'",
                                            candidate, existingIndex, existingOriginal, i, originalHeader));
                        }
                    }
                }
            }
        }

        // キー列のインデックスを取得（終了判定用）
        if (headerKeyColumn != null) {
            // 比較時も正規化済みの値を使用
            keyColumnIndex = columnMap.get(normalizeHeaderValue(headerKeyColumn));
            if (keyColumnIndex == null) {
                log.error("キー列 '{}' がヘッダー行に見つかりませんでした", headerKeyColumn);
                throw new KeyColumnNotFoundException(headerKeyColumn);
            }
        }
    }

    static String normalizeHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        normalized = normalized.replace("\uFEFF", "").replace("\u200B", "");
        normalized = normalized.replace("\u00A0", " ").replace("\u3000", " ");
        normalized = normalized.replaceAll("\\p{C}", "");
        normalized = normalized.replaceAll("\\s+[\\p{IsHiragana}\\p{IsKatakana}\\u30FC]+$", "");
        return normalized.trim();
    }

    private static List<String> getHeaderCandidates(Cell cell) {
        List<String> candidates = new ArrayList<>();
        String formatted = HEADER_FORMATTER.formatCellValue(cell);
        if (formatted != null && !formatted.trim().isEmpty()) {
            addCandidate(candidates, formatted.trim());
        }
        return candidates;
    }

    private static void addCandidate(List<String> candidates, String value) {
        String normalized = normalizeHeaderValue(value);
        if (normalized != null && !normalized.isEmpty() && !candidates.contains(normalized)) {
            candidates.add(normalized);
        }
        String compacted = compactHeaderValue(normalized);
        if (compacted != null && !compacted.isEmpty() && !candidates.contains(compacted)) {
            candidates.add(compacted);
        }
    }

    private static String compactHeaderValue(String value) {
        if (value == null) {
            return null;
        }
        String compacted = value.replaceAll("[\\p{Z}\\p{P}]", "");
        compacted = compacted.replaceAll("[\\p{IsHiragana}\\p{IsKatakana}\\u30FC]", "");
        return compacted.trim();
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

