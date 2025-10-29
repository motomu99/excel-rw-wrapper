package com.example.csv;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Stream;

import com.opencsv.exceptions.CsvException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.opencsv.bean.MappingStrategy;
import lombok.extern.slf4j.Slf4j;

/**
 * CSVファイルをStreamとして読み込むビルダークラス
 * ビルダーパターンを使用してCSV読み込み処理を抽象化
 * 
 * 使用例:
 * <pre>
 * List<Person> persons = CsvStreamReader.of(Person.class, Paths.get("sample.csv"))
 *     .charset(CharsetType.UTF_8)
 *     .fileType(FileType.CSV)
 *     .skip(1)
 *     .process(stream -> stream.collect(Collectors.toList()));
 * </pre>
 */
@Slf4j
public class CsvStreamReader<T> {
    
    private final Class<T> beanClass;
    private final Path filePath;
    private CharsetType charsetType = CharsetType.UTF_8;
    private FileType fileType = FileType.CSV;
    private int skipLines = 0;
    private boolean usePositionMapping = false;
    
    private CsvStreamReader(Class<T> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }
    
    /**
     * CsvStreamReaderのインスタンスを作成
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @return CsvStreamReaderのインスタンス
     */
    public static <T> CsvStreamReader<T> of(Class<T> beanClass, Path filePath) {
        return new CsvStreamReader<>(beanClass, filePath);
    }
    
    /**
     * 文字エンコーディングを設定
     * 
     * @param charsetType 文字エンコーディングタイプ
     * @return このインスタンス
     */
    public CsvStreamReader<T> charset(CharsetType charsetType) {
        this.charsetType = charsetType;
        return this;
    }
    
    /**
     * ファイルタイプを設定
     * 
     * @param fileType ファイルタイプ
     * @return このインスタンス
     */
    public CsvStreamReader<T> fileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }
    
    /**
     * スキップする行数を設定
     * 
     * @param lines スキップする行数
     * @return このインスタンス
     */
    public CsvStreamReader<T> skip(int lines) {
        this.skipLines = lines;
        return this;
    }
    
    /**
     * 位置ベースマッピングを使用
     * 
     * @return このインスタンス
     */
    public CsvStreamReader<T> usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }
    
    /**
     * ヘッダーベースマッピングを使用（デフォルト）
     * 
     * @return このインスタンス
     */
    public CsvStreamReader<T> useHeaderMapping() {
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
     * @throws CsvException CSV解析エラー
     */
    public <R> R process(Function<Stream<T>, R> processor) throws IOException, CsvException {
        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStream is = charsetType.isWithBom() ? BomHandler.skipBom(fis) : fis;
             InputStreamReader isr = new InputStreamReader(is, Charset.forName(charsetType.getCharsetName()))) {
            
            MappingStrategy<T> strategy = MappingStrategyFactory.createStrategy(beanClass, usePositionMapping);
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(isr)
                    .withMappingStrategy(strategy)
                    .withSeparator(fileType.getDelimiter().charAt(0))
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();
            
            Stream<T> stream = csvToBean.stream();
            
            // データ行をスキップする処理
            if (skipLines > 0) {
                stream = stream.skip(skipLines);
            }
            
            // 呼び出し側でStreamを処理（try-with-resources内で完了）
            return processor.apply(stream);
        } catch (IOException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
        }
    }
}

