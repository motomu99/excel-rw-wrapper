package com.example.csv;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

import lombok.extern.slf4j.Slf4j;

/**
 * ExcelファイルをStreamとして読み込むビルダークラス
 * ビルダーパターンを使用してExcel読み込み処理を抽象化
 *
 * 使用例:
 * <pre>
 * List&lt;Person&gt; persons = ExcelStreamReader.of(Person.class, Paths.get("sample.xlsx"))
 *     .sheetIndex(0)
 *     .skip(1)
 *     .process(stream -&gt; stream.collect(Collectors.toList()));
 * </pre>
 */
@Slf4j
public class ExcelStreamReader<T> {

    private final Class<T> beanClass;
    private final Path filePath;
    private int sheetIndex = 0;
    private String sheetName = null;
    private int skipLines = 0;
    private boolean usePositionMapping = false;

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
     * Streamを処理する
     *
     * @param <R> 戻り値の型
     * @param processor Streamを処理する関数
     * @return 処理結果
     * @throws IOException ファイル読み込みエラー
     */
    public <R> R process(Function<Stream<T>, R> processor) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = getSheet(workbook);
            if (sheet == null) {
                throw new IOException("指定されたシートが見つかりません");
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

        // ヘッダー行を取得（最初の行）
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
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

        // データ行を読み込む（ヘッダー行の次から）
        for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isEmptyRow(row)) {
                continue;
            }

            T bean = createBean(row, headerMap, columnMap);
            if (bean != null) {
                result.add(bean);
            }
        }

        return result;
    }

    /**
     * 行からBeanを作成
     */
    private T createBean(Row row, Map<Integer, String> headerMap, Map<String, Integer> columnMap) throws Exception {
        T bean = beanClass.getDeclaredConstructor().newInstance();

        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);

            Integer columnIndex = null;

            if (usePositionMapping) {
                // 位置ベースマッピング
                CsvBindByPosition positionAnnotation = field.getAnnotation(CsvBindByPosition.class);
                if (positionAnnotation != null) {
                    columnIndex = positionAnnotation.position();
                }
            } else {
                // ヘッダーベースマッピング
                CsvBindByName nameAnnotation = field.getAnnotation(CsvBindByName.class);
                if (nameAnnotation != null) {
                    String columnName = nameAnnotation.column();
                    columnIndex = columnMap.get(columnName);
                }
            }

            if (columnIndex != null && columnIndex < row.getLastCellNum()) {
                Cell cell = row.getCell(columnIndex);
                if (cell != null) {
                    Object value = convertCellValue(cell, field.getType());
                    field.set(bean, value);
                }
            }
        }

        return bean;
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
            case NUMERIC -> String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

    /**
     * セルの値を指定された型に変換
     */
    private Object convertCellValue(Cell cell, Class<?> targetType) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        try {
            if (targetType == String.class) {
                return getCellValue(cell);
            } else if (targetType == Integer.class || targetType == int.class) {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return (int) cell.getNumericCellValue();
                } else {
                    return Integer.parseInt(getCellValue(cell));
                }
            } else if (targetType == Long.class || targetType == long.class) {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return (long) cell.getNumericCellValue();
                } else {
                    return Long.parseLong(getCellValue(cell));
                }
            } else if (targetType == Double.class || targetType == double.class) {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return cell.getNumericCellValue();
                } else {
                    return Double.parseDouble(getCellValue(cell));
                }
            } else if (targetType == Boolean.class || targetType == boolean.class) {
                if (cell.getCellType() == CellType.BOOLEAN) {
                    return cell.getBooleanCellValue();
                } else {
                    return Boolean.parseBoolean(getCellValue(cell));
                }
            }
        } catch (Exception e) {
            log.warn("セル値の変換に失敗しました: セル={}, 型={}, エラー={}", cell, targetType, e.getMessage());
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
}
