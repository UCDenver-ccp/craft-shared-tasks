(ns craft-eval.coref)

;;; FOR ALL BUT BLANC SCORES, THE OUTPUT LOOKS LIKE:
;;; ====== TOTALS =======
;;; Identification of Mentions: Recall: (1173 / 1173) 100%  Precision: (1173 / 1173) 100%   F1: 100%
;;; --------------------------------------------------------------------------
;;; Coreference: Recall: (1173 / 1173) 100% Precision: (1173 / 1173) 100%   F1: 100%
;;; --------------------------------------------------------------------------
;;; ------------------ end /home/craft/eval-data/coreference/conllcoref/11532192.conll
;;;

(defn extract-coref-results-from-string [results-str metric]
      "extract coreference results from log output (most likely slurped from a log file)."
      (map (fn [m] (let [[match mention-recall-num mention-recall-den mention-precision-num mention-precision-den coref-recall-num coref-recall-den coref-precision-num coref-precision-den document-id] m]
                     (vector document-id {(keyword (str metric "-mention-tp")) (Float/parseFloat mention-precision-num)
                                          (keyword (str metric "-mention-fp")) (- (Integer/parseInt mention-precision-den) (Float/parseFloat mention-precision-num))
                                          (keyword (str metric "-mention-fn")) (- (Integer/parseInt mention-recall-den) (Float/parseFloat mention-recall-num))
                                          (keyword (str metric "-coref-tp"))   (Float/parseFloat coref-precision-num)
                                          (keyword (str metric "-coref-fp"))   (- (Integer/parseInt coref-precision-den) (Float/parseFloat coref-precision-num))
                                          (keyword (str metric "-coref-fn"))   (- (Integer/parseInt coref-recall-den) (Float/parseFloat coref-recall-num))})))
           (re-seq #"(?m)^Identification of Mentions: Recall: \(([\d\.]+) / (\d+)\) [\d\.]+%\tPrecision: \(([\d\.]+) / (\d+)\) [\d\.]+%\tF1: [\d\.]+%\n-+\nCoreference: Recall: \(([\d\.]+) / (\d+)\) [\d\.]+%\tPrecision: \(([\d\.]+) / (\d+)\) [\d\.]+%\tF1: [\d\.]+%\n-+\n-+ end .*/(\d+)\.conll"
                   results-str)))


(defn extract-coref-results-from-file [log-file]
      "extract coreference results from a coreference results log file."
      (println (str "Processing coref metric log file: " (.getAbsolutePath log-file)))
      (let [results-str (slurp log-file)
            metric (.replaceAll (.replace (.getName log-file) ".log" "") "\\." "-")]
           (extract-coref-results-from-string results-str metric)))


;;; FOR BLANC SCORES, THE OUTPUT LOOKS LIKE:
;;;====== TOTALS =======
;;;Identification of Mentions: Recall: (681 / 681) 100%    Precision: (681 / 681) 100%     F1: 100%
;;;--------------------------------------------------------------------------
;;;
;;;Coreference:
;;;Coreference links: Recall: (4323 / 4323) 100%   Precision: (4323 / 4323) 100%   F1: 100%
;;;--------------------------------------------------------------------------
;;;Non-coreference links: Recall: (227217 / 227217) 100%   Precision: (227217 / 227217) 100%       F1: 100%
;;;--------------------------------------------------------------------------
;;;BLANC: Recall: (1 / 1) 100%     Precision: (1 / 1) 100% F1: 100%
;;;--------------------------------------------------------------------------
;;;------------------ end /home/craft/eval-data/coreference/conllcoref/15328533.conll
(defn extract-coref-results-blanc-from-string [results-str metric]
      "extract coreference results from a coreference results log file for the BLANC metric."
      (map (fn [m]
               (let [[match mention-recall-num mention-recall-den mention-precision-num mention-precision-den coref-recall-num coref-recall-den coref-precision-num coref-precision-den non-coref-recall-num non-coref-recall-den non-coref-precision-num non-coref-precision-den document-id] m]
                    (vector document-id {(keyword (str metric "-mention-tp"))   (Integer/parseInt mention-precision-num)
                                         (keyword (str metric "-mention-fp"))   (- (Integer/parseInt mention-precision-den) (Integer/parseInt mention-precision-num))
                                         (keyword (str metric "-mention-fn"))   (- (Integer/parseInt mention-recall-den) (Integer/parseInt mention-recall-num))
                                         (keyword (str metric "-coref-tp"))     (Integer/parseInt coref-precision-num)
                                         (keyword (str metric "-coref-fp"))     (- (Integer/parseInt coref-precision-den) (Integer/parseInt coref-precision-num))
                                         (keyword (str metric "-coref-fn"))     (- (Integer/parseInt coref-recall-den) (Integer/parseInt coref-recall-num))
                                         (keyword (str metric "-non-coref-tp")) (Integer/parseInt non-coref-precision-num)
                                         (keyword (str metric "-non-coref-fp")) (- (Integer/parseInt non-coref-precision-den) (Integer/parseInt non-coref-precision-num))
                                         (keyword (str metric "-non-coref-fn")) (- (Integer/parseInt non-coref-recall-den) (Integer/parseInt non-coref-recall-num))})))
           (re-seq #"(?m)^Identification of Mentions: Recall: \((\d+) / (\d+)\) [\d\.]+%\tPrecision: \((\d+) / (\d+)\) [\d\.]+%\tF1: [\d\.]+%\n-+\n\nCoreference:\nCoreference links: Recall: \((\d+) / (\d+)\) [\d\.]+%\tPrecision: \((\d+) / (\d+)\) [\d\.]+%\tF1: [\d\.]+%\n-+\nNon-coreference links: Recall: \((\d+) / (\d+)\) [\d\.]+%\tPrecision: \((\d+) / (\d+)\) [\d\.]+%\tF1: [\d\.]+%\n-+\nBLANC:.*\n-+\n-+ end .*/(\d+)\.conll"
                   results-str)))


(defn extract-coref-results-blanc-from-file [log-file]
      "extract coreference results from a coreference results log file."
      (println (str "Processing BLANC metric log file: " (.getAbsolutePath log-file)))
      (let [results-str (slurp log-file)
            metric (.replaceAll (.replace (.getName log-file) ".log" "") "\\." "-")]
           (extract-coref-results-blanc-from-string results-str metric)))



(defn compile-coref-results [log-directory]
      "Parse log files in the log directory and compile results for
      each document and each metric used to gauge performance. Returns a nested map structure:
      {Document-id  {metric-tpfpfn counts}}"
      (reduce (fn [m1 m2] (let [document-id (first m2)
                                performance-vals-map (last m2)
                                doc-map (get m1 document-id)
                                updated-doc-map (merge doc-map performance-vals-map)]
                               (merge m1 {document-id updated-doc-map})))
              {}
              (apply concat (map (fn [f] (if (.contains (.getName f) "blanc")
                                           (extract-coref-results-blanc-from-file f)
                                           (extract-coref-results-from-file f)))
                                 (filter #(and (.isFile %) (.endsWith (.getName %) ".log"))
                                         (file-seq log-directory))))))


(def ordered-metric-keys
  '(:bcub-mention-tp :bcub-mention-fp :bcub-mention-fn :bcub-mention-p :bcub-mention-r :bcub-mention-f
     :bcub-coref-tp :bcub-coref-fp :bcub-coref-fn :bcub-coref-p :bcub-coref-r :bcub-coref-f

     :bcub-allow_partial-mention-tp :bcub-allow_partial-mention-fp :bcub-allow_partial-mention-fn :bcub-allow_partial-mention-p :bcub-allow_partial-mention-r :bcub-allow_partial-mention-f
     :bcub-allow_partial-coref-tp :bcub-allow_partial-coref-fp :bcub-allow_partial-coref-fn :bcub-allow_partial-coref-p :bcub-allow_partial-coref-r :bcub-allow_partial-coref-f

     :blanc-mention-tp :blanc-mention-fp :blanc-mention-fn :blanc-mention-p :blanc-mention-r :blanc-mention-f
     :blanc-coref-tp :blanc-coref-fp :blanc-coref-fn :blanc-coref-p :blanc-coref-r :blanc-coref-f
     :blanc-non-coref-tp :blanc-non-coref-fp :blanc-non-coref-fn :blanc-non-coref-p :blanc-non-coref-r :blanc-non-coref-f
     :blanc-score

     :blanc-allow_partial-mention-tp :blanc-allow_partial-mention-fp :blanc-allow_partial-mention-fn :blanc-allow_partial-mention-p :blanc-allow_partial-mention-r :blanc-allow_partial-mention-f
     :blanc-allow_partial-coref-tp :blanc-allow_partial-coref-fp :blanc-allow_partial-coref-fn :blanc-allow_partial-coref-p :blanc-allow_partial-coref-r :blanc-allow_partial-coref-f
     :blanc-allow_partial-non-coref-tp :blanc-allow_partial-non-coref-fp :blanc-allow_partial-non-coref-fn :blanc-allow_partial-non-coref-p :blanc-allow_partial-non-coref-r :blanc-allow_partial-non-coref-f
     :blanc-allow_partial-score

     :ceafe-mention-tp :ceafe-mention-fp :ceafe-mention-fn :ceafe-mention-p :ceafe-mention-r :ceafe-mention-f
     :ceafe-coref-tp :ceafe-coref-fp :ceafe-coref-fn :ceafe-coref-p :ceafe-coref-r :ceafe-coref-f

     :ceafe-allow_partial-mention-tp :ceafe-allow_partial-mention-fp :ceafe-allow_partial-mention-fn :ceafe-allow_partial-mention-p :ceafe-allow_partial-mention-r :ceafe-allow_partial-mention-f
     :ceafe-allow_partial-coref-tp :ceafe-allow_partial-coref-fp :ceafe-allow_partial-coref-fn :ceafe-allow_partial-coref-p :ceafe-allow_partial-coref-r :ceafe-allow_partial-coref-f

     :ceafm-mention-tp :ceafm-mention-fp :ceafm-mention-fn :ceafm-mention-p :ceafm-mention-r :ceafm-mention-f
     :ceafm-coref-tp :ceafm-coref-fp :ceafm-coref-fn :ceafm-coref-p :ceafm-coref-r :ceafm-coref-f

     :ceafm-allow_partial-mention-tp :ceafm-allow_partial-mention-fp :ceafm-allow_partial-mention-fn :ceafm-allow_partial-mention-p :ceafm-allow_partial-mention-r :ceafm-allow_partial-mention-f
     :ceafm-allow_partial-coref-tp :ceafm-allow_partial-coref-fp :ceafm-allow_partial-coref-fn :ceafm-allow_partial-coref-p :ceafm-allow_partial-coref-r :ceafm-allow_partial-coref-f

     :lea-mention-tp :lea-mention-fp :lea-mention-fn :lea-mention-p :lea-mention-r :lea-mention-f
     :lea-coref-tp :lea-coref-fp :lea-coref-fn :lea-coref-p :lea-coref-r :lea-coref-f

     :lea-allow_partial-mention-tp :lea-allow_partial-mention-fp :lea-allow_partial-mention-fn :lea-allow_partial-mention-p :lea-allow_partial-mention-r :lea-allow_partial-mention-f
     :lea-allow_partial-coref-tp :lea-allow_partial-coref-fp :lea-allow_partial-coref-fn :lea-allow_partial-coref-p :lea-allow_partial-coref-r :lea-allow_partial-coref-f

     :muc-mention-tp :muc-mention-fp :muc-mention-fn :muc-mention-p :muc-mention-r :muc-mention-f
     :muc-coref-tp :muc-coref-fp :muc-coref-fn :muc-coref-p :muc-coref-r :muc-coref-f

     :muc-allow_partial-mention-tp :muc-allow_partial-mention-fp :muc-allow_partial-mention-fn :muc-allow_partial-mention-p :muc-allow_partial-mention-r :muc-allow_partial-mention-f
     :muc-allow_partial-coref-tp :muc-allow_partial-coref-fp :muc-allow_partial-coref-fn :muc-allow_partial-coref-p :muc-allow_partial-coref-r :muc-allow_partial-coref-f))


;; document-id + blanc.score + blanc.allow_partial.score = 3
;; 5 metrics (non-blanc) * 2 (coref+mention) * 6 columns = 60
;; 1 metrics (blanc) * 3 (coref+non-coref+mention) * 6 columns = 18
;; (60 + 18) * 2 (allow_partial) = 156 + 3 = 159
(def coref-output-file-headings (conj ordered-metric-keys "#document-id"))

(defn write-coref-output-file-header [writer]
      "The coref output file has 81 columns. This function writes the header for the file.
      The first column is document id. Then for each metric, there are 6 columns: TP, FP, FN, P, R, F, except for BLANC which has 10 columns."
      (.write writer (str (clojure.string/join "\t" coref-output-file-headings) "\n")))


(defn prf [tp fp fn]
      "compute P, R, & F, and return a tab-delimited string: 'tp fp fn p r f'"
      (let [p (/ tp (+ tp fp))
            r (/ tp (+ tp fn))
            f (/ (* 2 p r) (+ p r))]
           (list tp fp fn p r f)))


(defn compile-coref-results-for-document [coref-results-map]
      "The input document-map maps from metric-tp/fp/fn keywords to counts of tp/fp/fn's
      that were extracted from the coref output log files. This function calculates P, R,& F,
      and returns a mapping from keywords to tp/fp/fn/p/r/f values for each metric. Keywords
      used are the same as those in ordered-metric-keys."
      (let [metrics '("bcub" "bcub-allow_partial" "blanc" "blanc-allow_partial" "ceafe" "ceafe-allow_partial" "ceafm" "ceafm-allow_partial" "lea" "lea-allow_partial" "muc" "muc-allow_partial")]
           (into (sorted-map)
                 (apply concat
                        (filter (fn [x] (not (nil? x)))     ;; filter out any nil values
                                (map (fn [metric]
                                         (let [mention-tp-key (keyword (str metric "-mention-tp"))
                                               mention-fp-key (keyword (str metric "-mention-fp"))
                                               mention-fn-key (keyword (str metric "-mention-fn"))
                                               mention-p-key (keyword (str metric "-mention-p"))
                                               mention-r-key (keyword (str metric "-mention-r"))
                                               mention-f-key (keyword (str metric "-mention-f"))
                                               coref-tp-key (keyword (str metric "-coref-tp"))
                                               coref-fp-key (keyword (str metric "-coref-fp"))
                                               coref-fn-key (keyword (str metric "-coref-fn"))
                                               coref-p-key (keyword (str metric "-coref-p"))
                                               coref-r-key (keyword (str metric "-coref-r"))
                                               coref-f-key (keyword (str metric "-coref-f"))
                                               non-coref-tp-key (keyword (str metric "-non-coref-tp"))
                                               non-coref-fp-key (keyword (str metric "-non-coref-fp"))
                                               non-coref-fn-key (keyword (str metric "-non-coref-fn"))
                                               non-coref-p-key (keyword (str metric "-non-coref-p"))
                                               non-coref-r-key (keyword (str metric "-non-coref-r"))
                                               non-coref-f-key (keyword (str metric "-non-coref-f"))
                                               blanc-score-key (keyword (str metric "-score"))]
                                              ;; for each metric, look to see if its metric-mention-tp key exists.
                                              ;; If it does, continue, if not, return empty [].
                                              (if (contains? coref-results-map mention-tp-key)
                                                (do
                                                  (let [[mention-tp mention-fp mention-fn
                                                         mention-p mention-r mention-f] (prf (get coref-results-map mention-tp-key)
                                                                                             (get coref-results-map mention-fp-key)
                                                                                             (get coref-results-map mention-fn-key))

                                                        [coref-tp coref-fp coref-fn
                                                         coref-p coref-r coref-f] (prf (get coref-results-map coref-tp-key)
                                                                                       (get coref-results-map coref-fp-key)
                                                                                       (get coref-results-map coref-fn-key))

                                                        ;; for BLANC metric only
                                                        has-blanc (.contains metric "blanc")
                                                        [non-coref-tp non-coref-fp non-coref-fn
                                                         non-coref-p non-coref-r non-coref-f] (if has-blanc
                                                                                                (prf (get coref-results-map non-coref-tp-key)
                                                                                                     (get coref-results-map non-coref-fp-key)
                                                                                                     (get coref-results-map non-coref-fn-key))
                                                                                                [nil nil nil nil nil nil])
                                                        blanc-score (if has-blanc (/ (+ coref-f non-coref-f) 2) nil)

                                                        results (list {mention-tp-key mention-tp}
                                                                      {mention-fp-key mention-fp}
                                                                      {mention-fn-key mention-fn}
                                                                      {mention-p-key mention-p}
                                                                      {mention-r-key mention-r}
                                                                      {mention-f-key mention-f}
                                                                      {coref-tp-key coref-tp}
                                                                      {coref-fp-key coref-fp}
                                                                      {coref-fn-key coref-fn}
                                                                      {coref-p-key coref-p}
                                                                      {coref-r-key coref-r}
                                                                      {coref-f-key coref-f})]

                                                       (if has-blanc
                                                         (concat results (list {non-coref-tp-key non-coref-tp}
                                                                               {non-coref-fp-key non-coref-fp}
                                                                               {non-coref-fn-key non-coref-fn}
                                                                               {non-coref-p-key non-coref-p}
                                                                               {non-coref-r-key non-coref-r}
                                                                               {non-coref-f-key non-coref-f}
                                                                               {blanc-score-key blanc-score}))
                                                         results)))
                                                ;; if there are no entries for a given metric then return nil
                                                nil)))
                                     metrics))))))


(defn write-results-for-document [document-id results-map writer]
      "writes the results for a single document using the order specified in ordered-metric-keys"
      (.write writer (str document-id "\t"
                          (clojure.string/join "\t" (map (fn [key] (get results-map key)) ordered-metric-keys))
                          "\n")))


(defn write-total-coref-stats [writer total-results-map]
      "Given a map of total counts for tp, fp, & fn for each metric, write a
      line containing tp, fp, fn, p, r, & f for each metric."
      (write-results-for-document "TOTAL" total-results-map writer))


(defn serialize-coref-results [log-directory writer]
      "Outputs all results to the writer, including a line that has aggregated 'total' results."
      ;; write the file header
      (write-coref-output-file-header writer)
      (write-total-coref-stats
        writer
        (compile-coref-results-for-document
          (reduce-kv (fn [totals document-id document-map]
                         ;; input is sorted at this point, so for each document, write a line
                         ;; containing the performance metrics for that document, then update the total
                         ;; counts in the 'totals' map, then after all documents have been
                         ;; processed, write the 'total' stats to the file.
                         (let [doc-results-map (compile-coref-results-for-document document-map)]

                              ;; write the output line for this particular document
                              (write-results-for-document document-id doc-results-map writer)

                              ;; keep track of tp/fp/fn counts so that the total p/r/f can be
                              ;; computed after all document performances have been output
                              (merge-with + totals
                                                (into (sorted-map)
                                                      (map
                                                        (fn [key] [key (get doc-results-map key)])
                                                        ;; this filter returns the keys for tp/fp/fn values
                                                        ;; as we don't need the p/r/f keys here
                                                        (filter (fn [key] (let [key-name (name key)
                                                                                endswith-tp (.endsWith key-name "-tp")
                                                                                endswith-fp (.endsWith key-name "-fp")
                                                                                endswith-fn (.endsWith key-name "-fn")]
                                                                               (or endswith-tp endswith-fp endswith-fn)))
                                                                ordered-metric-keys))))))
                     {}
                     (into (sorted-map) (compile-coref-results log-directory))))))


