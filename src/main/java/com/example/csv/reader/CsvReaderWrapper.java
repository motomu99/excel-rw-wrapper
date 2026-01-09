package com.example.csv.reader;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;
import com.example.common.config.CharsetType;
import com.example.common.config.FileType;
import com.example.common.mapping.MappingStrategyDetector;
import com.example.common.mapping.MappingStrategyFactory;
import com.example.common.util.BomHandler;
import com.example.common.util.CharsetDetector;
import com.example.exception.CsvReadException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import com.example.common.mapping.FieldMappingCache;
import com.example.common.model.RowData;
import com.opencsv.bean.MappingStrategy;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;

/**
 * CSV読み込みのラッパークラス
 * 
 * <p>このクラスはCSVファイルを読み込み、指定されたBeanクラスにマッピングします。
 * Builderパターンを使用して、柔軟な設定が可能です。</p>
 * 
 * <h3>使用例（新しいBuilderパターン - 推奨）:</h3>
 * <pre>
 * // 基本的な使い方
 * List&lt;Person&gt; persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
 *     .read();
 * 
 * // 詳細設定
 * List&lt;Person&gt; persons = CsvReaderWrapper.builder(Person.class, Paths.get("sample.csv"))
 *     .charset(CharsetType.UTF_8_BOM)
 *     .skipLines(1)
 *     .fileType(FileType.TSV)
 *     .read();
 * </pre>
 * 
 * <h3>使用例（従来のexecuteメソッド - 互換性維持）:</h3>
 * <pre>
 * List&lt;Person&gt; persons = CsvReaderWrapper.execute(Person.class, 
 *     Paths.get("sample.csv"), CsvReaderWrapper::read);
 * </pre>
 */
@Slf4j
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public class CsvReaderWrapper {
    
    private int skipLines = 0;
    private Path filePath;
    private Class<?> beanClass;
    private Charset charset = StandardCharsets.UTF_8;
    private FileType fileType = FileType.CSV;
    // Builderパターンでフィールド名とメソッド名が同じになるのは一般的なパターン
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    private Boolean usePositionMapping = null;
    private boolean withBom = false;
    /** 文字コードが明示的に指定されたかどうか（nullの場合は自動判別） */
    private CharsetType charsetType = null;

    private CsvReaderWrapper(Class<?> beanClass, Path filePath) {
        this.beanClass = beanClass;
        this.filePath = filePath;
    }

    /**
     * 現在の設定をコピーし、新しいファイルパスを設定したインスタンスを作成
     * 
     * @param newPath 新しいファイルパス
     * @return 設定がコピーされた新しいインスタンス
     */
    private CsvReaderWrapper cloneConfig(Path newPath) {
        CsvReaderWrapper clone = new CsvReaderWrapper(this.beanClass, newPath);
        clone.skipLines = this.skipLines;
        clone.charset = this.charset;
        clone.fileType = this.fileType;
        clone.usePositionMapping = this.usePositionMapping;
        clone.withBom = this.withBom;
        clone.charsetType = this.charsetType;
        return clone;
    }

    /**
     * Builderインスタンスを生成（推奨される使い方）
     * 
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @return Builderインスタンス
     */
    public static Builder builder(Class<?> beanClass, Path filePath) {
        return new Builder(beanClass, filePath);
    }

    /**
     * Builderインスタンスを生成（複数ファイル用）
     * 
     * @param beanClass マッピング先のBeanクラス
     * @param filePaths CSVファイルのパスリスト
     * @return Builderインスタンス
     */
    public static Builder builder(Class<?> beanClass, List<Path> filePaths) {
        return new Builder(beanClass, filePaths);
    }

    /**
     * CSVファイルを読み込んでBeanのListとして返す（従来の使い方 - 互換性維持）
     * 
     * <p>既存コードとの互換性のために残されています。
     * 新しいコードでは {@link #builder(Class, Path)} の使用を推奨します。</p>
     * 
     * @param <T> Beanの型
     * @param beanClass マッピング先のBeanクラス
     * @param filePath CSVファイルのパス
     * @param readerFunction 読み込み処理を行う関数
     * @return BeanのList
     * @throws IOException ファイル読み込みエラー
     * @throws CsvException CSV解析エラー
     * @deprecated 新しい {@link #builder(Class, Path)} メソッドを使用してください
     */
    @Deprecated(since = "2.0.0")
    public static <T> List<T> execute(Class<T> beanClass, Path filePath, 
                                     Function<CsvReaderWrapper, List<T>> readerFunction) 
                                     throws IOException, CsvException {
        CsvReaderWrapper wrapper = new CsvReaderWrapper(beanClass, filePath);
        return readerFunction.apply(wrapper);
    }

    /**
     * CSVファイルを読み込む
     * 
     * <p>設定されたパラメータに基づいてCSVファイルを読み込み、
     * Beanのリストとして返します。</p>
     * 
     * @param <T> Beanの型
     * @return BeanのList
     * @throws CsvReadException CSV読み込みエラー
     */
    public <T> List<T> read() {
        // マッピング戦略が未設定の場合、アノテーションから自動判定
        if (usePositionMapping == null) {
            usePositionMapping = MappingStrategyDetector.detectUsePositionMapping(beanClass)
                    .orElse(false); // デフォルトはヘッダーベース
        }

        // 文字コードが明示的に指定されていない場合は自動判別
        if (charsetType == null) {
            detectCharsetAndBom();
        }

        CsvColumnValidator.validate(filePath, charset, withBom, fileType.getDelimiter().charAt(0));

        try (FileInputStream fis = new FileInputStream(filePath.toFile());
             InputStream is = withBom ? BomHandler.skipBom(fis) : fis;
             InputStreamReader isr = new InputStreamReader(is, charset)) {
            
            MappingStrategy<T> strategy = createMappingStrategy();
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(isr)
                    .withMappingStrategy(strategy)
                    .withSeparator(fileType.getDelimiter().charAt(0))
                    .withIgnoreLeadingWhiteSpace(true)
                    .withIgnoreEmptyLine(true)
                    .build();

            List<T> result = csvToBean.parse();

            // 行番号フィールドが存在する場合は行番号を設定
            setLineNumbers(result, usePositionMapping);

            return applySkipLines(result);
        } catch (IOException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
        }
    }

    /**
     * 文字コードとBOMを自動判別して設定する
     * 
     * <p>ファイルが存在しない場合は即座にCsvReadExceptionを投げます。</p>
     * 文字コードの自動判別に失敗した場合はUTF-8をデフォルトとして使用します。
     * 
     * @throws CsvReadException ファイルが存在しない場合
     */
    private void detectCharsetAndBom() {
        try {
            Charset detectedCharset = CharsetDetector.detect(filePath);
            charset = detectedCharset;
            
            // UTF-8の場合はBOMの有無を確認
            if (StandardCharsets.UTF_8.equals(detectedCharset)) {
                withBom = BomHandler.hasBom(filePath);
            } else {
                withBom = false;
            }
            
            log.debug("文字コードを自動判別しました: charset={}, withBom={}", charset, withBom);
        } catch (FileNotFoundException e) {
            // ファイルが存在しない場合は即座にエラーにする
            log.error("ファイルが存在しません: ファイルパス={}", filePath, e);
            throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
        } catch (IOException e) {
            // 文字コードの自動判別に失敗した場合はUTF-8をデフォルトとして使用
            log.warn("文字コードの自動判別に失敗しました。UTF-8を使用します: {}", e.getMessage());
            charset = StandardCharsets.UTF_8;
            withBom = false;
        }
    }

    /**
     * マッピング戦略を生成
     * 
     * @param <T> Beanの型
     * @return マッピング戦略
     */
    @SuppressWarnings("unchecked")
    private <T> MappingStrategy<T> createMappingStrategy() {
        return MappingStrategyFactory.createStrategy((Class<T>) this.beanClass, usePositionMapping);
    }

    /**
     * 行番号を設定
     *
     * <p>@LineNumberアノテーションが付与されたフィールドがある場合、行番号を設定します。</p>
     *
     * @param <T> Beanの型
     * @param result Beanのリスト
     * @param usePositionMapping 位置ベースマッピングを使用するかどうか
     */
    private <T> void setLineNumbers(List<T> result, Boolean usePositionMapping) {
        if (result.isEmpty()) {
            return;
        }

        // FieldMappingCacheから行番号フィールドを取得
        FieldMappingCache fieldMappingCache = new FieldMappingCache(result.get(0).getClass());
        if (!fieldMappingCache.hasLineNumberField()) {
            return;
        }

        java.lang.reflect.Field lineNumberField = fieldMappingCache.getLineNumberField();
        Class<?> fieldType = lineNumberField.getType();

        // 位置ベースマッピング（ヘッダーなし）の場合は1行目から、
        // ヘッダーベースマッピングの場合は2行目からデータが始まる
        int lineNumber = (usePositionMapping != null && usePositionMapping) ? 1 : 2;

        for (T bean : result) {
            try {
                if (fieldType == Integer.class || fieldType == int.class) {
                    lineNumberField.set(bean, lineNumber);
                } else if (fieldType == Long.class || fieldType == long.class) {
                    lineNumberField.set(bean, (long) lineNumber);
                }
                lineNumber++;
            } catch (IllegalAccessException e) {
                log.error("行番号の設定に失敗しました: lineNumber={}, bean={}", lineNumber, bean, e);
                throw new CsvReadException("行番号の設定に失敗しました", e);
            }
        }
    }
    
    /**
     * 行番号を設定（元のファイルの行番号マッピングを使用）
     *
     * <p>@LineNumberアノテーションが付与されたフィールドがある場合、行番号を設定します。
     * エラー行を除外した一時ファイルを使用している場合、元のファイルの行番号を設定します。</p>
     *
     * @param <T> Beanの型
     * @param result Beanのリスト
     * @param usePositionMapping 位置ベースマッピングを使用するかどうか
     * @param originalLineNumberMap 一時ファイルの行番号 → 元のファイルの行番号のマッピング
     */
    private <T> void setLineNumbersWithMapping(List<T> result, Boolean usePositionMapping, 
                                               java.util.Map<Integer, Integer> originalLineNumberMap) {
        if (result.isEmpty()) {
            return;
        }

        // FieldMappingCacheから行番号フィールドを取得
        FieldMappingCache fieldMappingCache = new FieldMappingCache(result.get(0).getClass());
        if (!fieldMappingCache.hasLineNumberField()) {
            return;
        }

        java.lang.reflect.Field lineNumberField = fieldMappingCache.getLineNumberField();
        Class<?> fieldType = lineNumberField.getType();

        // 位置ベースマッピング（ヘッダーなし）の場合は1行目から、
        // ヘッダーベースマッピングの場合は2行目からデータが始まる
        int tempFileLineNumber = (usePositionMapping != null && usePositionMapping) ? 1 : 2;

        for (T bean : result) {
            try {
                // 一時ファイルの行番号から元のファイルの行番号を取得
                Integer originalLineNumber = originalLineNumberMap.get(tempFileLineNumber);
                int lineNumberToSet = originalLineNumber != null ? originalLineNumber : tempFileLineNumber;
                
                if (fieldType == Integer.class || fieldType == int.class) {
                    lineNumberField.set(bean, lineNumberToSet);
                } else if (fieldType == Long.class || fieldType == long.class) {
                    lineNumberField.set(bean, (long) lineNumberToSet);
                }
                tempFileLineNumber++;
            } catch (IllegalAccessException e) {
                log.error("行番号の設定に失敗しました: tempFileLineNumber={}, bean={}", tempFileLineNumber, bean, e);
                throw new CsvReadException("行番号の設定に失敗しました", e);
            }
        }
    }

    /**
     * CSVファイルを読み込んでRowDataのListとして返す
     * 
     * <p>既存のドメインモデルを変更せずに行番号情報を取得したい場合に使用します。</p>
     * 
     * @param <T> Beanの型
     * @return RowDataのList
     * @throws CsvReadException CSV読み込みエラー
     */
    public <T> List<RowData<T>> readWithLineNumber() {
        List<T> result = read();
        
        // 位置ベースマッピング（ヘッダーなし）の場合は1行目から、
        // ヘッダーベースマッピングの場合は2行目からデータが始まる
        int startLineNumber = (usePositionMapping != null && usePositionMapping) ? 1 : 2;
        
        List<RowData<T>> rowDataList = new ArrayList<>();
        int lineNumber = startLineNumber;
        for (T data : result) {
            rowDataList.add(new RowData<>(lineNumber, data));
            lineNumber++;
        }
        
        return rowDataList;
    }
    
    /**
     * 列数チェックを有効にしてCSVファイルを読み込み、エラー行も含めた結果を返す
     * 
     * <p>列数が不一致の行はスキップされ、エラー情報として記録されます。
     * 処理は最後まで続行され、成功した行とエラー行の情報が返されます。</p>
     * 
     * @param <T> Beanの型
     * @return CsvReadResult（成功した行のデータとエラー行の情報を含む）
     * @throws CsvReadException CSV読み込みエラー
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public <T> CsvReadResult<T> readWithValidation() {
        // マッピング戦略が未設定の場合、アノテーションから自動判定
        if (usePositionMapping == null) {
            usePositionMapping = MappingStrategyDetector.detectUsePositionMapping(beanClass)
                    .orElse(false); // デフォルトはヘッダーベース
        }

        // 文字コードが明示的に指定されていない場合は自動判別
        if (charsetType == null) {
            detectCharsetAndBom();
        }

        // 列数チェックを実行してエラー行を収集
        List<CsvReadError> columnErrors = CsvColumnValidator.validateAndCollectErrors(
            filePath, charset, withBom, fileType.getDelimiter().charAt(0)
        );
        
        // エラー行の行番号をSetに変換して高速検索
        java.util.Set<Integer> errorLineNumbers = columnErrors.stream()
            .map(CsvReadError::getLineNumber)
            .collect(java.util.stream.Collectors.toSet());

        // エラー行を除外した一時ファイルを作成
        // 元のファイルの行番号と一時ファイルの行番号のマッピングを作成
        Path tempFile = null;
        java.util.Map<Integer, Integer> originalLineNumberMap = new java.util.HashMap<>();
        try {
            if (!errorLineNumbers.isEmpty()) {
                // エラー行を除外した一時ファイルを作成
                // CsvColumnValidatorと同じロジックで空行をスキップして論理的な行番号を追跡
                tempFile = java.nio.file.Files.createTempFile("csv-read-", ".csv");
                try (FileInputStream fis1 = new FileInputStream(filePath.toFile());
                     InputStream is1 = withBom ? BomHandler.skipBom(fis1) : fis1;
                     InputStreamReader isr1 = new InputStreamReader(is1, charset);
                     com.opencsv.CSVReader csvReader = new com.opencsv.CSVReaderBuilder(isr1)
                         .withCSVParser(new com.opencsv.CSVParserBuilder()
                             .withSeparator(fileType.getDelimiter().charAt(0))
                             .withQuoteChar('"')
                             .withIgnoreQuotations(false)
                             .withIgnoreLeadingWhiteSpace(true)
                             .build())
                         .withFieldAsNull(com.opencsv.enums.CSVReaderNullFieldIndicator.NEITHER)
                         .build();
                     java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile.toFile());
                     java.io.OutputStreamWriter osw = new java.io.OutputStreamWriter(fos, charset);
                     CSVWriter csvWriter = new CSVWriter(osw, 
                         fileType.getDelimiter().charAt(0),
                         CSVWriter.DEFAULT_QUOTE_CHARACTER,
                         CSVWriter.DEFAULT_ESCAPE_CHARACTER,
                         CSVWriter.DEFAULT_LINE_END)) {
                    
                    String[] row;
                    int logicalLineNumber = 0; // 論理的な行番号（空行をスキップ）
                    int tempFileLineNumber = 0;
                    
                    try {
                        row = csvReader.readNext();
                        while (row != null) {
                            // 空行をスキップ（CsvColumnValidatorと同じロジック）
                            if (isRowEmptyForCsv(row)) {
                                // 空行も一時ファイルに書き込む（CSV形式で）
                                csvWriter.writeNext(new String[]{""}, false);
                                row = csvReader.readNext();
                                continue;
                            }
                            logicalLineNumber++;
                            
                            // エラー行でない場合は一時ファイルに書き込む
                            if (!errorLineNumbers.contains(logicalLineNumber)) {
                                tempFileLineNumber++;
                                // CSVパーサーが解析した行をそのままCSV形式で書き込む
                                csvWriter.writeNext(row, false);
                                // 一時ファイルの行番号 → 元のファイルの論理的行番号のマッピング
                                originalLineNumberMap.put(tempFileLineNumber, logicalLineNumber);
                            }
                            // エラー行はスキップ（一時ファイルに書き込まない）
                            
                            row = csvReader.readNext();
                        }
                    } catch (CsvValidationException e) {
                        throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
                    }
                }
            }

            // エラー行を除外したファイル（または元のファイル）を読み込む
            Path fileToRead = tempFile != null ? tempFile : filePath;
            
            try (FileInputStream fis = new FileInputStream(fileToRead.toFile());
                 InputStream is = withBom ? BomHandler.skipBom(fis) : fis;
                 InputStreamReader isr = new InputStreamReader(is, charset)) {
                
                MappingStrategy<T> strategy = createMappingStrategy();
                
                CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(isr)
                        .withMappingStrategy(strategy)
                        .withSeparator(fileType.getDelimiter().charAt(0))
                        .withIgnoreLeadingWhiteSpace(true)
                        .withIgnoreEmptyLine(true)
                        .build();

                List<T> result = csvToBean.parse();

                // 行番号フィールドが存在する場合は行番号を設定
                // エラー行を除外したファイルの場合は、元のファイルの行番号を設定
                if (!originalLineNumberMap.isEmpty()) {
                    setLineNumbersWithMapping(result, usePositionMapping, originalLineNumberMap);
                } else {
                    setLineNumbers(result, usePositionMapping);
                }

                List<T> finalData = applySkipLines(result);
                
                return new CsvReadResult<>(finalData, columnErrors);
            }
        } catch (IOException e) {
            log.error("CSVファイル読み込み中にエラーが発生: ファイルパス={}, エラー={}", filePath, e.getMessage(), e);
            throw new CsvReadException("CSVファイルの読み込みに失敗しました: " + filePath, e);
        } finally {
            // 一時ファイルを削除
            if (tempFile != null) {
                try {
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (IOException e) {
                    log.warn("一時ファイルの削除に失敗しました: {}", tempFile, e);
                }
            }
        }
    }

    /**
     * CSV行が空かどうかを判定（CsvColumnValidatorと同じロジック）
     * 
     * @param row CSV行
     * @return 空行の場合true
     */
    private static boolean isRowEmptyForCsv(String[] row) {
        return row.length == 1 && row[0].isEmpty();
    }
    
    /**
     * スキップ行数を適用
     *
     * @param <T> Beanの型
     * @param result 元のリスト
     * @return スキップ処理後のリスト
     */
    private <T> List<T> applySkipLines(List<T> result) {
        if (skipLines <= 0) {
            return result;
        }
        if (skipLines >= result.size()) {
            return new ArrayList<>();
        }
        return result.subList(skipLines, result.size());
    }

    /**
     * スキップ行数を設定（従来のAPI - 互換性維持）
     * 
     * @param skipLines スキップする行数
     * @return このインスタンス
     * @deprecated {@link Builder#skipLines(int)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvReaderWrapper setSkip(int skipLines) {
        this.skipLines = skipLines;
        return this;
    }

    /**
     * 文字セットを設定（従来のAPI - 互換性維持）
     * 
     * @param charsetType 文字セットタイプ
     * @return このインスタンス
     * @deprecated {@link Builder#charset(CharsetType)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvReaderWrapper setCharset(CharsetType charsetType) {
        this.charsetType = charsetType;
        this.charset = Charset.forName(charsetType.getCharsetName());
        this.withBom = charsetType.isWithBom();
        return this;
    }

    /**
     * ファイルタイプを設定（従来のAPI - 互換性維持）
     * 
     * @param fileType ファイルタイプ
     * @return このインスタンス
     * @deprecated {@link Builder#fileType(FileType)} を使用してください
     */
    @Deprecated(since = "2.0.0")
    public CsvReaderWrapper setFileType(FileType fileType) {
        this.fileType = fileType;
        return this;
    }

    /**
     * 位置ベースマッピングを使用（従来のAPI - 互換性維持）
     * 
     * @return このインスタンス
     */
    public CsvReaderWrapper usePositionMapping() {
        this.usePositionMapping = true;
        return this;
    }

    /**
     * ヘッダーベースマッピングを使用（従来のAPI - 互換性維持）
     * 
     * @return このインスタンス
     */
    public CsvReaderWrapper useHeaderMapping() {
        this.usePositionMapping = false;
        return this;
    }

    /**
     * CsvReaderWrapperのBuilderクラス
     * 
     * <p>Builderパターンを使用して、CsvReaderWrapperの設定を行います。
     * メソッドチェーンで設定を積み重ね、最後に{@link #read()}を呼び出すことで
     * CSVファイルを読み込みます。</p>
     */
    @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
    public static class Builder {
        private final CsvReaderWrapper wrapper;
        private final List<Path> filePaths;
        // Builderパターンでフィールド名とメソッド名が同じになるのは一般的なパターン
        private int parallelism = 1;
        
        private Builder(Class<?> beanClass, Path filePath) {
            this.wrapper = new CsvReaderWrapper(beanClass, filePath);
            this.filePaths = java.util.Collections.singletonList(filePath);
        }

        @SuppressFBWarnings("CT_CONSTRUCTOR_THROW")
        private Builder(Class<?> beanClass, List<Path> filePaths) {
            if (filePaths == null || filePaths.isEmpty()) {
                throw new IllegalArgumentException("filePaths must not be empty or null");
            }
            this.wrapper = new CsvReaderWrapper(beanClass, filePaths.get(0));
            this.filePaths = filePaths;
        }
        
        /**
         * スキップする行数を設定
         * 
         * @param skipLines スキップする行数（データ行のみ、ヘッダーは含まない）
         * @return このBuilderインスタンス
         */
        public Builder skipLines(int skipLines) {
            wrapper.skipLines = skipLines;
            return this;
        }
        
        /**
         * 文字セットを設定
         * 
         * <p>明示的に文字コードを指定した場合、自動判別は行われません。
         * 指定しない場合は、ファイルの文字コードを自動判別します。</p>
         * 
         * @param charsetType 文字セットタイプ
         * @return このBuilderインスタンス
         */
        public Builder charset(CharsetType charsetType) {
            wrapper.charsetType = charsetType;
            wrapper.charset = Charset.forName(charsetType.getCharsetName());
            wrapper.withBom = charsetType.isWithBom();
            return this;
        }
        
        /**
         * ファイルタイプを設定
         * 
         * @param fileType ファイルタイプ（CSVまたはTSV）
         * @return このBuilderインスタンス
         */
        public Builder fileType(FileType fileType) {
            wrapper.fileType = fileType;
            return this;
        }
        
        /**
         * 位置ベースマッピングを使用
         * 
         * <p>ヘッダー行がない場合に使用します。
         * カラムの位置に基づいてBeanのフィールドにマッピングします。</p>
         * 
         * @return このBuilderインスタンス
         */
        public Builder usePositionMapping() {
            wrapper.usePositionMapping = true;
            return this;
        }
        
        /**
         * ヘッダーベースマッピングを使用（デフォルト）
         * 
         * <p>ヘッダー行の名前に基づいてBeanのフィールドにマッピングします。</p>
         * 
         * @return このBuilderインスタンス
         */
        public Builder useHeaderMapping() {
            wrapper.usePositionMapping = false;
            return this;
        }
        
        /**
         * CSVファイルを読み込む
         * 
         * @param <T> Beanの型
         * @return BeanのList
         * @throws CsvReadException CSV読み込みエラー
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public <T> List<T> read() {
            // 複数ファイルの場合は並列処理（意図が明確なリテラル使用）
            final int singleFileThreshold = 1;
            if (filePaths.size() > singleFileThreshold) {
                return readAll();
            }
            return wrapper.read();
        }

        /**
         * CSVファイルを読み込んでRowDataのListとして返す
         * 
         * <p>既存のドメインモデルを変更せずに行番号情報を取得したい場合に使用します。</p>
         * 
         * @param <T> Beanの型
         * @return RowDataのList
         * @throws CsvReadException CSV読み込みエラー
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public <T> List<RowData<T>> readWithLineNumber() {
            // 複数ファイルの場合は並列処理（意図が明確なリテラル使用）
            final int singleFileThreshold = 1;
            if (filePaths.size() > singleFileThreshold) {
                return readAllWithLineNumber();
            }
            return wrapper.readWithLineNumber();
        }

        /**
         * 並列度を設定
         * 
         * @param parallelism 並列度（1以下の場合は逐次処理）
         * @return このBuilderインスタンス
         */
        public Builder parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        /**
         * 全ファイルを読み込む
         * 
         * <p>設定されたファイルリストを読み込み、
         * 並列度に従って処理を実行します。</p>
         * 
         * @param <T> Beanの型
         * @return BeanのList
         */
        public <T> List<T> readAll() {
            return com.example.common.reader.ParallelReadExecutor.readAll(
                filePaths,
                path -> wrapper.cloneConfig(path).read(),
                parallelism
            );
        }

        /**
         * 全ファイルを読み込んでRowDataのListとして返す
         * 
         * <p>設定されたファイルリストを読み込み、
         * 並列度に従って処理を実行します。</p>
         * 
         * @param <T> Beanの型
         * @return RowDataのList
         */
        public <T> List<RowData<T>> readAllWithLineNumber() {
            return com.example.common.reader.ParallelReadExecutor.readAll(
                filePaths,
                path -> wrapper.cloneConfig(path).readWithLineNumber(),
                parallelism
            );
        }
        
        /**
         * 列数チェックを有効にしてCSVファイルを読み込み、エラー行も含めた結果を返す
         * 
         * <p>列数が不一致の行はスキップされ、エラー情報として記録されます。
         * 処理は最後まで続行され、成功した行とエラー行の情報が返されます。</p>
         * 
         * @param <T> Beanの型
         * @return CsvReadResult（成功した行のデータとエラー行の情報を含む）
         * @throws CsvReadException CSV読み込みエラー
         */
        @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
        public <T> CsvReadResult<T> readWithValidation() {
            // 複数ファイルの場合は未サポート（意図が明確なリテラル使用）
            final int singleFileThreshold = 1;
            if (filePaths.size() > singleFileThreshold) {
                throw new CsvReadException("列数チェック機能は単一ファイルのみサポートしています");
            }
            
            return wrapper.readWithValidation();
        }
    }
}
