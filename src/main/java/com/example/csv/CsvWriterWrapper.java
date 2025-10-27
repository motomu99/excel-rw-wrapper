package com.example.csv;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;

import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.bean.HeaderColumnNameMappingStrategy;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.opencsv.bean.MappingStrategy;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import lombok.extern.slf4j.Slf4j;

/**
 * CSV書き込みのラッパークラス
 * 関数型インターフェースを使用してCSV書き込み処理を抽象化
 * 
 * 使用例:
 * <pre>
 * List<Person> persons = Arrays.asList(new Person("田中", 25, "エンジニア", "東京"));
 * CsvWriterWrapper.execute(Person.class, 
 *     Paths.get("output.csv"), 
 *     instance -> instance.write(persons));
 * </pre>
 */
@Slf4j
public class CsvWriterWrapper {
    
    private Path filePath;
    private Class<?> beanClass;
    private Charset charset = Charset.forName("UTF-8");
    private FileType fileType = FileType.CSV;
    private boolean usePositionMapping = false;
    private boolean withBom = false;
    private String lineSeparator = LineSeparatorType.CRLF.getSeparator();

    private CsvWriterWrapper(Class<?> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * CSVファイルにBeanのListを書き込む（CsvWriterWrapperインスタンス用）
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング元のBeanクラス
     * @param filePath CSVファイルのパス
     * @param writerFunction 書き込み処理を行う関数
     * @return 処理結果（Void）
     * @throws IOException ファイル書き込みエラー
     */
    public static <T> Void execute(Class<T> beanClass, Path filePath, 
                                   Function<CsvWriterWrapper, Void> writerFunction) 
                                   throws IOException {
        CsvWriterWrapper wrapper = new CsvWriterWrapper(beanClass, filePath);
        return writerFunction.apply(wrapper);
    }

    /**
     * CSVファイルに書き込む関数
     * 標準のStatefulBeanToCsvを使用した書き込み
     * 
     * @param <T> Beanの型
     * @param beans 書き込むBeanのList
     * @return Void
     */
    @SuppressWarnings("unchecked")
    public <T> Void write(List<T> beans) {
        try (FileOutputStream fos = new FileOutputStream(filePath.toFile())) {
            
            // BOM付きUTF-8の場合、BOMを書き込む
            if (withBom) {
                fos.write(0xEF);
                fos.write(0xBB);
                fos.write(0xBF);
                log.debug("BOMを書き込みました");
            }
            
            try (OutputStreamWriter osw = new OutputStreamWriter(fos, charset)) {
                MappingStrategy<T> strategy;
                if (usePositionMapping) {
                    ColumnPositionMappingStrategy<T> positionStrategy = new ColumnPositionMappingStrategy<>();
                    positionStrategy.setType((Class<? extends T>) this.beanClass);
                    strategy = positionStrategy;
                } else {
                    HeaderColumnNameMappingStrategy<T> headerStrategy = new HeaderColumnNameMappingStrategy<>();
                    headerStrategy.setType((Class<? extends T>) this.beanClass);
                    strategy = headerStrategy;
                }
                
                StatefulBeanToCsv<T> beanToCsv = new StatefulBeanToCsvBuilder<T>(osw)
                        .withMappingStrategy(strategy)
                        .withSeparator(fileType.getDelimiter().charAt(0))
                        .withLineEnd(lineSeparator)
                        .build();
                
                beanToCsv.write(beans);
                
                log.info("CSVファイルへの書き込み完了: ファイルパス={}, 件数={}", filePath, beans.size());
            }
            
            return null;
        } catch (IOException e) {
            log.error("CSVファイル書き込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new RuntimeException(e);
        } catch (CsvDataTypeMismatchException | CsvRequiredFieldEmptyException e) {
            log.error("CSV書き込み中にデータ型エラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * 文字エンコーディングを設定
     * 
     * @param charsetType 文字エンコーディング
     * @return このインスタンス（メソッドチェーン用）
     */
    public CsvWriterWrapper setCharset(CharsetType charsetType) {
        this.charset = Charset.forName(charsetType.getCharsetName());
        this.withBom = charsetType.isWithBom();
        return this;
    }

    /**
     * ファイルタイプ（CSV/TSV）を設定
     * 
     * @param fileType ファイルタイプ
     * @return このインスタンス（メソッドチェーン用）
     */
    public CsvWriterWrapper setFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    /**
     * ポジションマッピングを使用（ヘッダーなし）
     * 
     * @return このインスタンス（メソッドチェーン用）
     */
    public CsvWriterWrapper usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーマッピングを使用（ヘッダーあり）
     * 
     * @return このインスタンス（メソッドチェーン用）
     */
    public CsvWriterWrapper useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }

    /**
     * 改行コードを設定
     * 
     * @param lineSeparatorType 改行コードタイプ
     * @return このインスタンス（メソッドチェーン用）
     */
    public CsvWriterWrapper setLineSeparator(LineSeparatorType lineSeparatorType) {
        this.lineSeparator = lineSeparatorType.getSeparator();
        return this;
    }
}

