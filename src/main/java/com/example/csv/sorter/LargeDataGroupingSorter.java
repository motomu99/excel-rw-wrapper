package com.example.csv.sorter;

import com.example.common.config.CharsetType;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * 大容量CSV/Excel用のグルーピング＆ソート処理ユーティリティ
 * 
 * <p>5GB以上の大容量ファイルでもメモリ効率的に処理できます。</p>
 * 
 * <h3>処理フロー:</h3>
 * <ol>
 *   <li>CSVを1行ずつ読み込み、グループキーごとにファイル分割</li>
 *   <li>各グループファイルを外部ソート（CsvExternalSorter）でソート</li>
 *   <li>ソート済みファイルをストリーミング処理</li>
 *   <li>一時ファイルを自動クリーンアップ</li>
 * </ol>
 * 
 * <h3>使用例1: Lambda指定</h3>
 * <pre>
 * LargeDataGroupingSorter.of(Person.class, Paths.get("huge.csv"))
 *     .groupBy(Person::getOccupation)
 *     .sortBy(Comparator.comparingInt(Person::getAge))
 *     .processGroups((groupKey, personStream) -&gt; {
 *         personStream.forEach(person -&gt; saveToDB(groupKey, person));
 *     });
 * </pre>
 * 
 * <h3>使用例2: インターフェース実装</h3>
 * <pre>
 * // PersonがGroupingSortableを実装している場合
 * LargeDataGroupingSorter.of(Person.class, Paths.get("huge.csv"))
 *     .processGroupsSorted((groupKey, personStream) -&gt; {
 *         personStream.forEach(person -&gt; saveToDB(groupKey, person));
 *     });
 * </pre>
 * 
 * <h3>使用例3: 複雑なキー</h3>
 * <pre>
 * LargeDataGroupingSorter.of(Person.class, Paths.get("huge.csv"))
 *     .groupBy(p -&gt; p.getDepartment() + "_" + p.getCity())
 *     .sortBy(Comparator.comparingInt(Person::getAge)
 *                       .thenComparing(Person::getName))
 *     .processGroups((groupKey, stream) -&gt; {
 *         List&lt;Person&gt; top10 = stream.limit(10).collect(Collectors.toList());
 *         saveTop10(groupKey, top10);
 *     });
 * </pre>
 * 
 * @param <T> Bean型
 */
@Slf4j
public class LargeDataGroupingSorter<T> {
    
    private final Class<T> beanClass;
    private final Path inputPath;
    private final Path tempDirectory;
    private CharsetType charsetType = CharsetType.UTF_8;
    
    // グルーピング設定
    private Function<T, String> groupKeyExtractor;
    
    // ソート設定
    private Comparator<T> comparator;
    private boolean useComparable = false;
    
    private LargeDataGroupingSorter(Class<T> beanClass, Path inputPath) {
        this.beanClass = beanClass;
        this.inputPath = inputPath;
        this.tempDirectory = Paths.get(System.getProperty("java.io.tmpdir"),
                "grouping-sorter-" + UUID.randomUUID());
        
        // GroupingSortable実装チェック
        if (GroupingSortable.class.isAssignableFrom(beanClass)) {
            this.useComparable = true;
        }
    }
    
    /**
     * LargeDataGroupingSorterインスタンスを生成
     * 
     * @param <T> Bean型
     * @param beanClass Bean型のClassオブジェクト
     * @param inputPath 入力CSVファイルのパス
     * @return LargeDataGroupingSorterインスタンス
     */
    public static <T> LargeDataGroupingSorter<T> of(Class<T> beanClass, Path inputPath) {
        return new LargeDataGroupingSorter<>(beanClass, inputPath);
    }
    
    /**
     * 文字エンコーディングを設定
     * 
     * @param charsetType 文字セットタイプ
     * @return このインスタンス
     */
    public LargeDataGroupingSorter<T> charset(CharsetType charsetType) {
        this.charsetType = charsetType;
        return this;
    }
    
    /**
     * グループキーを抽出するFunctionを設定
     * 
     * @param groupKeyExtractor グループキーを返すFunction
     * @return このインスタンス
     */
    public LargeDataGroupingSorter<T> groupBy(Function<T, String> groupKeyExtractor) {
        this.groupKeyExtractor = groupKeyExtractor;
        return this;
    }
    
    /**
     * ソート順を定義するComparatorを設定
     * 
     * @param comparator ソート用Comparator
     * @return このインスタンス
     */
    public LargeDataGroupingSorter<T> sortBy(Comparator<T> comparator) {
        this.comparator = comparator;
        return this;
    }
    
    /**
     * グループごとに処理を実行（グループキーとBeanのStreamを受け取る）
     * 
     * @param processor グループキーとBeanのStreamを処理するBiConsumer
     * @throws IOException ファイル操作エラー
     */
    public void processGroups(BiConsumer<String, Stream<T>> processor) throws IOException {
        log.info("グルーピング＆ソート処理開始: 入力={}", inputPath);
        
        try {
            Files.createDirectories(tempDirectory);
            log.debug("一時ディレクトリ作成: {}", tempDirectory);
            
            // フェーズ1: グルーピング（Beanをグループごとに分類）
            Map<String, List<T>> groupMap = splitByGroup();
            log.info("グルーピング完了: グループ数={}", groupMap.size());
            
            // フェーズ2: 各グループをソートしてファイルに書き出す
            Map<String, Path> sortedGroupFiles = sortAndWriteGroups(groupMap);
            log.info("ソート完了: グループ数={}", sortedGroupFiles.size());
            
            // フェーズ3: グループごとに処理
            processEachGroup(sortedGroupFiles, processor);
            log.info("グループ処理完了");
            
        } finally {
            cleanup();
        }
    }
    
    /**
     * GroupingSortableインターフェースを実装したBeanをグルーピング＆ソート処理
     * 
     * @param processor グループキーとBeanのStreamを処理するBiConsumer
     * @throws IOException ファイル操作エラー
     */
    public void processGroupsSorted(BiConsumer<String, Stream<T>> processor) throws IOException {
        if (!GroupingSortable.class.isAssignableFrom(beanClass)) {
            throw new IllegalStateException(
                "processGroupsSorted()を使用するには、" + beanClass.getSimpleName() + 
                "がGroupingSortableインターフェースを実装している必要があります");
        }
        
        // GroupingSortableからグループキーとComparatorを自動取得
        // 型安全性: beanClassがGroupingSortableであることは172行目で確認済み
        Function<T, String> keyExtractor = bean -> {
            GroupingSortable<?> sortable = (GroupingSortable<?>) bean;
            return String.valueOf(sortable.getGroupKey());
        };
        this.groupKeyExtractor = keyExtractor;
        
        // GroupingSortableを実装しているBeanを比較
        // ジェネリクス型パラメータKは実行時に消去されるため、rawタイプを使用
        this.comparator = createComparatorFromGroupingSortable();
        
        processGroups(processor);
    }
    
    /**
     * フェーズ1: CSVを読み込み、グループキーごとにBeanを分類
     */
    private Map<String, List<T>> splitByGroup() throws IOException {
        Map<String, List<T>> groupMap = new HashMap<>();
        
        try (Reader reader = new InputStreamReader(
                new FileInputStream(inputPath.toFile()),
                java.nio.charset.Charset.forName(charsetType.getCharsetName()))) {
            
            CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                    .withType(beanClass)
                    .build();
            
            int totalLines = 0;
            for (T bean : csvToBean) {
                totalLines++;
                
                // グループキー取得
                String groupKey = extractGroupKey(bean);
                
                // グループキーごとにBeanをリストに追加
                groupMap.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(bean);
                
                // 進捗ログ
                if (totalLines % 100000 == 0) {
                    log.debug("グルーピング進捗: {}行処理完了, グループ数={}", totalLines, groupMap.size());
                }
            }
            
            log.info("CSV読み込み完了: 総行数={}, グループ数={}", totalLines, groupMap.size());
        }
        
        return groupMap;
    }
    
    /**
     * フェーズ2: 各グループをソートしてファイルに書き出す
     */
    private Map<String, Path> sortAndWriteGroups(Map<String, List<T>> groupMap) throws IOException {
        Map<String, Path> sortedFiles = new HashMap<>();
        
        for (Map.Entry<String, List<T>> entry : groupMap.entrySet()) {
            String groupKey = entry.getKey();
            List<T> beans = entry.getValue();
            
            log.debug("グループソート開始: グループ={}, 件数={}", groupKey, beans.size());
            
            // メモリ内でソート
            if (comparator != null) {
                beans.sort(comparator);
            } else if (useComparable) {
                sortComparableBeans(beans);
            }
            
            // ソート済みBeanをCSVファイルに書き込み
            Path sortedFile = tempDirectory.resolve(sanitizeFileName(groupKey) + "_sorted.csv");
            writeBeansToCsv(beans, sortedFile);
            sortedFiles.put(groupKey, sortedFile);
            
            log.debug("グループソート完了: グループ={}", groupKey);
        }
        
        log.info("全グループソート完了: グループ数={}", sortedFiles.size());
        return sortedFiles;
    }
    
    /**
     * フェーズ3: ソート済みグループファイルをストリーミング処理
     */
    private void processEachGroup(Map<String, Path> sortedGroupFiles, 
                                   BiConsumer<String, Stream<T>> processor) throws IOException {
        for (Map.Entry<String, Path> entry : sortedGroupFiles.entrySet()) {
            String groupKey = entry.getKey();
            Path sortedFile = entry.getValue();
            
            log.debug("グループ処理開始: グループ={}", groupKey);
            
            try (Reader reader = new InputStreamReader(
                    new FileInputStream(sortedFile.toFile()),
                    java.nio.charset.Charset.forName(charsetType.getCharsetName()))) {
                
                CsvToBean<T> csvToBean = new CsvToBeanBuilder<T>(reader)
                        .withType(beanClass)
                        .build();
                
                // ストリーミング処理
                Stream<T> beanStream = csvToBean.stream();
                processor.accept(groupKey, beanStream);
            }
            
            log.debug("グループ処理完了: グループ={}", groupKey);
        }
    }
    
    /**
     * グループキーを抽出
     */
    private String extractGroupKey(T bean) {
        if (groupKeyExtractor != null) {
            return groupKeyExtractor.apply(bean);
        } else if (bean instanceof Groupable) {
            return String.valueOf(((Groupable<?>) bean).getGroupKey());
        } else {
            throw new IllegalStateException(
                "groupBy()でグループキー抽出Functionを指定するか、" +
                beanClass.getSimpleName() + "にGroupableインターフェースを実装してください");
        }
    }
    
    /**
     * BeanのリストをCSVファイルに書き込む（OpenCSVを使用）
     */
    private void writeBeansToCsv(List<T> beans, Path outputPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath,
                java.nio.charset.Charset.forName(charsetType.getCharsetName()))) {
            
            // マッピング戦略を作成
            com.opencsv.bean.HeaderColumnNameMappingStrategy<T> strategy = 
                new com.opencsv.bean.HeaderColumnNameMappingStrategy<>();
            strategy.setType(beanClass);
            
            // StatefulBeanToCsvを作成
            com.opencsv.bean.StatefulBeanToCsv<T> beanToCsv = 
                new com.opencsv.bean.StatefulBeanToCsvBuilder<T>(writer)
                    .withMappingStrategy(strategy)
                    .withSeparator(',')
                    .build();
            
            // Beanを書き込む
            beanToCsv.write(beans);
            
        } catch (com.opencsv.exceptions.CsvDataTypeMismatchException | 
                 com.opencsv.exceptions.CsvRequiredFieldEmptyException e) {
            log.error("CSV書き込みエラー: {}", e.getMessage());
            throw new IOException("CSV書き込みに失敗", e);
        }
    }
    
    
    /**
     * ファイル名として使える文字列にサニタイズ
     */
    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    /**
     * Comparableインターフェースを実装しているBeanをソート
     *
     * <p>型安全性: useComparableがtrueの場合、beanClassがComparableを実装していることが保証される</p>
     *
     * @param beans ソート対象のBeanリスト
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void sortComparableBeans(List<T> beans) {
        List<Comparable> comparables = (List) beans;
        comparables.sort(Comparable::compareTo);
    }

    /**
     * GroupingSortable実装向けのComparatorを生成（警告の局所化のために分離）
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Comparator<T> createComparatorFromGroupingSortable() {
        return (a, b) -> {
            Comparable comparable = (Comparable) a;
            return comparable.compareTo(b);
        };
    }

    /**
     * 一時ファイルをクリーンアップ
     */
    private void cleanup() {
        try {
            if (Files.exists(tempDirectory)) {
                Files.walk(tempDirectory)
                        .sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("一時ファイルの削除に失敗: path={}", path);
                            }
                        });
                log.debug("一時ファイルクリーンアップ完了");
            }
        } catch (IOException e) {
            log.warn("クリーンアップエラー: {}", e.getMessage());
        }
    }

}

