;(set-env! :dependencies '[[danielsz/boot-shell "0.0.1"]])
(require '[clojure.java.io :refer [file]])

;;; ==================================================================== ;;;
;;; =================== Coreference Evaluation Tasks ===================
;;; ==================================================================== ;;;

(deftask coref-eval-setup
         "Sets constants used in the coreference evaluation."
         [b eval-base-directory VAL str "The path to the base directory that contains the coreference evaluation scripts directory. MUST BE ABSOLUTE PATH."
          i input-directory VAL str "The path to the directory of coreference files to test. MUST BE ABSOLUTE PATH."
          g gold-standard-directory VAL str "The path to the directory containing the gold standard coreference files. MUST BE ABSOLUTE PATH."
          s scorer-directory VAL str "The path to the scorer.pl file. MUST BE ABSOLUTE PATH."]
         (with-pre-wrap fileset
                        (let [intermediate-results-directory (file (:eval-base-dir fileset) ".intermediate-results" "coref")
                              coref-eval-script (file (:eval-base-dir fileset) "coreference" "scripts" "run-coref-eval.sh")]
                             ;; scrub the intermediate results directory if it exists
                             (if (.exists intermediate-results-directory) (.delete intermediate-results-directory))
                             (.mkdirs intermediate-results-directory)
                             (merge fileset
                                    {:eval-base-dir                        (file eval-base-directory)
                                     :input-directory                      (file input-directory)
                                     :gold-standard-directory              (file gold-standard-directory)
                                     :scorer-directory                     (file scorer-directory)
                                     :coref-intermediate-results-directory intermediate-results-directory
                                     :coref-eval-script                    coref-eval-script}))))


(deftask coref-eval-setup-docker []
         "Sets constants used in the coreference evaluation when running the evaluation inside a Docker container."
         (comp (coref-eval-setup :eval-base-directory "/home/craft/evaluation"
                                 :input-directory "/files-to-evaluate"
                                 :gold-standard-directory "/home/craft/eval-data/coreference/conllcoref"
                                 :scorer-directory "/home/craft/evaluation/coreference/reference-coreference-scorers.git")))


(deftask run-coref-eval
         "evaluate a set of files in the CoNLL-Coref 2011/12 file format against CRAFT"
         [m metric VAL str "Indicator of the coreference metric to use. Must be one of: muc|bcub|ceafm|ceafe|blanc|lea"
          p allow-partial-matches bool "Defaults to 'false'. If set to 'true' then partial mention matches are permitted, otherwise mentions must have identical spans to match."]
         (with-pre-wrap fileset
                        ;(let [script (str (.getAbsolutePath (:coref-eval-script fileset))
                        ;                  " -i " (.getAbsolutePath (:input-directory fileset))
                        ;                  " -g " (.getAbsolutePath (:gold-standard-directory fileset))
                        ;                  " -o " (.getAbsolutePath (:coref-intermediate-results-directory fileset))
                        ;                  " -s " (.getAbsolutePath (:scorer-directory fileset))
                        ;                  " -m " metric
                        ;                  " -p " allow-partial-matches)]
                        ;     (println (str "==== SCRIPT: " script)))
                        (println (str "Running coreference evaluation. Metric = " metric))

                        (dosh (.getAbsolutePath (:coref-eval-script fileset))
                              "-i" (.getAbsolutePath (:input-directory fileset))
                              "-g" (.getAbsolutePath (:gold-standard-directory fileset))
                              "-o" (.getAbsolutePath (:coref-intermediate-results-directory fileset))
                              "-s" (.getAbsolutePath (:scorer-directory fileset))
                              "-m" metric
                              "-p" (str allow-partial-matches))
                        fileset))


;;; The coreference scoring scripts output results to console, which we pipe to a file.
;;; Depending on the scoring metric (the Blanc metric output is slightly different),
;;; the relevant portion of the output will take one of two forms as shown below.
;;;
;;; FOR ALL BUT BLANC SCORES, THE OUTPUT LOOKS LIKE:
;;; ====== TOTALS =======
;;; Identification of Mentions: Recall: (1173 / 1173) 100%  Precision: (1173 / 1173) 100%   F1: 100%
;;; --------------------------------------------------------------------------
;;; Coreference: Recall: (1173 / 1173) 100% Precision: (1173 / 1173) 100%   F1: 100%
;;; --------------------------------------------------------------------------
;;; ------------------ end /home/craft/eval-data/coreference/conllcoref/11532192.conll
;;;
;;;
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
(defn extract-coref-results [log-file]
      "extract coreference results from a coreference results log file. There are two
      slightly different formats (blanc is different from the others) handled by the
      regular expressions below."
      (println (str "Processing file: " (.getAbsolutePath log-file)))
      (map (fn [m] (let [[match mention-recall-num mention-recall-den mention-precision-num mention-precision-den coref-recall-num coref-recall-den coref-precision-num coref-precision-den document-id] m
                         metric (.replace (.getName log-file) ".log" "")]
                        ;(println (str "========\n" match "\n___________"))
                        (vector document-id metric {:mention-tp (Integer/parseInt mention-precision-num)
                                                    :mention-fp (- (Integer/parseInt mention-precision-num) (Integer/parseInt mention-precision-den))
                                                    :mention-fn (- (Integer/parseInt mention-recall-num) (Integer/parseInt mention-recall-den))
                                                    :coref-tp   (Integer/parseInt coref-precision-num)
                                                    :coref-fp   (- (Integer/parseInt coref-precision-num) (Integer/parseInt coref-precision-den))
                                                    :coref-fn   (- (Integer/parseInt coref-recall-num) (Integer/parseInt coref-recall-den))})))

           (if (.contains (.getName log-file) "blanc")
             (re-seq #"(?m)^Identification of Mentions: Recall: \((\d+) / (\d+)\) \d+%\tPrecision: \((\d+) / (\d+)\) \d+%\tF1: \d+%\n-+\n\nCoreference:\nCoreference links: Recall: \((\d+) / (\d+)\) \d+%\tPrecision: \((\d+) / (\d+)\) \d+%\tF1: \d+%\n-+\nNon-coreference links:.*\n-+\nBLANC:.*\n-+\n-+ end .*/(\d+)\.conll"
                     (slurp log-file))
             (re-seq #"(?m)^Identification of Mentions: Recall: \((\d+) / (\d+)\) \d+%\tPrecision: \((\d+) / (\d+)\) \d+%\tF1: \d+%\n-+\nCoreference: Recall: \((\d+) / (\d+)\) \d+%\tPrecision: \((\d+) / (\d+)\) \d+%\tF1: \d+%\n-+\n-+ end .*/(\d+)\.conll"
                     (slurp log-file)))))

(defn compile-coref-results [intermediate-results-directory]
      "Parse log files in the .intermediate-results directory and compile results for
      each document and each metric used to gauge performance."
      (reduce (fn [m1 m2] (let [document-id (first m2)
                                metric (second m2)
                                performance-vals-map (last m2)
                                doc-map (get m1 document-id)
                                updated-doc-map (merge doc-map {metric performance-vals-map})]

                               (if (not (contains? m1 document-id))
                                 (assoc m1 document-id {metric performance-vals-map})
                                 (merge m1 {document-id updated-doc-map}))))
              {}
              (apply concat (map (fn [f] (extract-coref-results f))
                                 (filter #(and (.isFile %) (.endsWith (.getName %) ".log"))
                                         (file-seq intermediate-results-directory))))))


(defn write-coref-output-file-header [writer]
      "The coref output file has 73 columns. This function writes the header for the file.
      The first column is document id. Then for each metric, there are 6 columns: TP, FP, FN, P, R, F"
      (.write writer (str (clojure.string/join "\t" '("#document-id"
                                                       "bcub.mention.tp" "bcub.mention.fp" "bcub.mention.fn" "bcub.mention.p" "bcub.mention.r" "bcub.mention.f"
                                                       "bcub.coref.tp" "bcub.coref.fp" "bcub.coref.fn" "bcub.coref.p" "bcub.coref.r" "bcub.coref.f"
                                                       "bcub.allow_partial.mention.tp" "bcub.allow_partial.mention.fp" "bcub.allow_partial.mention.fn" "bcub.allow_partial.mention.p" "bcub.allow_partial.mention.r" "bcub.allow_partial.mention.f"
                                                       "bcub.allow_partial.coref.tp" "bcub.allow_partial.coref.fp" "bcub.allow_partial.coref.fn" "bcub.allow_partial.coref.p" "bcub.allow_partial.coref.r" "bcub.allow_partial.coref.f"
                                                       "blanc.mention.tp" "blanc.mention.fp" "blanc.mention.fn" "blanc.mention.p" "blanc.mention.r" "blanc.mention.f"
                                                       "blanc.coref.tp" "blanc.coref.fp" "blanc.coref.fn" "blanc.coref.p" "blanc.coref.r" "blanc.coref.f"
                                                       "blanc.allow_partial.mention.tp" "blanc.allow_partial.mention.fp" "blanc.allow_partial.mention.fn" "blanc.allow_partial.mention.p" "blanc.allow_partial.mention.r" "blanc.allow_partial.mention.f"
                                                       "blanc.allow_partial.coref.tp" "blanc.allow_partial.coref.fp" "blanc.allow_partial.coref.fn" "blanc.allow_partial.coref.p" "blanc.allow_partial.coref.r" "blanc.allow_partial.coref.f"
                                                       "ceafe.mention.tp" "ceafe.mention.fp" "ceafe.mention.fn" "ceafe.mention.p" "ceafe.mention.r" "ceafe.mention.f"
                                                       "ceafe.coref.tp" "ceafe.coref.fp" "ceafe.coref.fn" "ceafe.coref.p" "ceafe.coref.r" "ceafe.coref.f"
                                                       "ceafe.allow_partial.mention.tp" "ceafe.allow_partial.mention.fp" "ceafe.allow_partial.mention.fn" "ceafe.allow_partial.mention.p" "ceafe.allow_partial.mention.r" "ceafe.allow_partial.mention.f"
                                                       "ceafe.allow_partial.coref.tp" "ceafe.allow_partial.coref.fp" "ceafe.allow_partial.coref.fn" "ceafe.allow_partial.coref.p" "ceafe.allow_partial.coref.r" "ceafe.allow_partial.coref.f"
                                                       "ceafm.mention.tp" "ceafm.mention.fp" "ceafm.mention.fn" "ceafm.mention.p" "ceafm.mention.r" "ceafm.mention.f"
                                                       "ceafm.coref.tp" "ceafm.coref.fp" "ceafm.coref.fn" "ceafm.coref.p" "ceafm.coref.r" "ceafm.coref.f"
                                                       "ceafm.allow_partial.mention.tp" "ceafm.allow_partial.mention.fp" "ceafm.allow_partial.mention.fn" "ceafm.allow_partial.mention.p" "ceafm.allow_partial.mention.r" "ceafm.allow_partial.mention.f"
                                                       "ceafm.allow_partial.coref.tp" "ceafm.allow_partial.coref.fp" "ceafm.allow_partial.coref.fn" "ceafm.allow_partial.coref.p" "ceafm.allow_partial.coref.r" "ceafm.allow_partial.coref.f"
                                                       "lea.mention.tp" "lea.mention.fp" "lea.mention.fn" "lea.mention.p" "lea.mention.r" "lea.mention.f"
                                                       "lea.coref.tp" "lea.coref.fp" "lea.coref.fn" "lea.coref.p" "lea.coref.r" "lea.coref.f"
                                                       "lea.allow_partial.mention.tp" "lea.allow_partial.mention.fp" "lea.allow_partial.mention.fn" "lea.allow_partial.mention.p" "lea.allow_partial.mention.r" "lea.allow_partial.mention.f"
                                                       "lea.allow_partial.coref.tp" "lea.allow_partial.coref.fp" "lea.allow_partial.coref.fn" "lea.allow_partial.coref.p" "lea.allow_partial.coref.r" "lea.allow_partial.coref.f"
                                                       "muc.mention.tp" "muc.mention.fp" "muc.mention.fn" "muc.mention.p" "muc.mention.r" "muc.mention.f"
                                                       "muc.coref.tp" "muc.coref.fp" "muc.coref.fn" "muc.coref.p" "muc.coref.r" "muc.coref.f"
                                                       "muc.allow_partial.mention.tp" "muc.allow_partial.mention.fp" "muc.allow_partial.mention.fn" "muc.allow_partial.mention.p" "muc.allow_partial.mention.r" "muc.allow_partial.mention.f"
                                                       "muc.allow_partial.coref.tp" "muc.allow_partial.coref.fp" "muc.allow_partial.coref.fn" "muc.allow_partial.coref.p" "muc.allow_partial.coref.r" "muc.allow_partial.coref.f"))
                          "\n")))

(defn compile-coref-results-for-document [document-map]
      "The input document-map maps from metrics to a map of performance values extracted
      from the coref output log files. This function calculated TP, FP, and FN, and
      subsequently, P, R, & F, and returns a vector of 72 values (12 metrics * 6 values per metric)."
      (reduce-kv (fn [results metric tpfpfn-map]
                     (let [mention-tp (:mention-tp tpfpfn-map)
                           mention-fp (:mention-fp tpfpfn-map)
                           mention-fn (:mention-fn tpfpfn-map)
                           mention-p (/ mention-tp (+ mention-tp mention-fp))
                           mention-r (/ mention-tp (+ mention-tp mention-fn))
                           mention-f (/ (* 2 mention-p mention-r) (+ mention-p mention-r))

                           coref-tp (:coref-tp tpfpfn-map)
                           coref-fp (:coref-fp tpfpfn-map)
                           coref-fn (:coref-fn tpfpfn-map)
                           coref-p (/ coref-tp (+ coref-tp coref-fp))
                           coref-r (/ coref-tp (+ coref-tp coref-fn))
                           coref-f (/ (* 2 coref-p coref-r) (+ coref-p coref-r))]

                          (conj results mention-tp mention-fp mention-fn mention-p mention-r mention-f coref-tp coref-fp coref-fn coref-p coref-r coref-f)))
                 []
                 (into (sorted-map) document-map)))

(defn update-totals [totals counts-map]
      "This function is used to track the total tp, fp, and fn counts for each metric.
      It returns an updated map of total tp, fp, and fn counts for all metrics."
      (reduce-kv (fn [t k v]
                     (if (contains? t k)
                       (update t k + v)
                       (assoc t k v)))
                 totals
                 counts-map))

(defn prf [tp fp fn]
      "compute P, R, & F, and return a tab-delimited string: 'tp fp fn p r f'"
      (let [p (/ tp (+ tp fp))
            r (/ tp (+ tp fn))
            f (/ (* 2 p r) (+ p r))]
           (clojure.string/join "\t" (list tp fp fn p r f))))

(defn print-total-stats [writer counts-map]
      "Given a map of total counts for tp, fp, & fn for each metric, write a
      line containing tp, fp, fn, p, r, & f for each metric."
      (.write writer (str (clojure.string/join "\t"
                                               (list "TOTAL"
                                                     (prf (:bcub-mention-tp counts-map) (:bcub-mention-fp counts-map) (:bcub-mention-fn counts-map))
                                                     (prf (:bcub-coref-tp counts-map) (:bcub-coref-fp counts-map) (:bcub-coref-fn counts-map))
                                                     (prf (:bcub-allow_partial-mention-tp counts-map) (:bcub-allow_partial-mention-fp counts-map) (:bcub-allow_partial-mention-fn counts-map))
                                                     (prf (:bcub-allow_partial-coref-tp counts-map) (:bcub-allow_partial-coref-fp counts-map) (:bcub-allow_partial-coref-fn counts-map))
                                                     (prf (:blanc-mention-tp counts-map) (:blanc-mention-fp counts-map) (:blanc-mention-fn counts-map))
                                                     (prf (:blanc-coref-tp counts-map) (:blanc-coref-fp counts-map) (:blanc-coref-fn counts-map))
                                                     (prf (:blanc-allow_partial-mention-tp counts-map) (:blanc-allow_partial-mention-fp counts-map) (:blanc-allow_partial-mention-fn counts-map))
                                                     (prf (:blanc-allow_partial-coref-tp counts-map) (:blanc-allow_partial-coref-fp counts-map) (:blanc-allow_partial-coref-fn counts-map))
                                                     (prf (:ceafe-mention-tp counts-map) (:ceafe-mention-fp counts-map) (:ceafe-mention-fn counts-map))
                                                     (prf (:ceafe-coref-tp counts-map) (:ceafe-coref-fp counts-map) (:ceafe-coref-fn counts-map))
                                                     (prf (:ceafe-allow_partial-mention-tp counts-map) (:ceafe-allow_partial-mention-fp counts-map) (:ceafe-allow_partial-mention-fn counts-map))
                                                     (prf (:ceafe-allow_partial-coref-tp counts-map) (:ceafe-allow_partial-coref-fp counts-map) (:ceafe-allow_partial-coref-fn counts-map))
                                                     (prf (:ceafm-mention-tp counts-map) (:ceafm-mention-fp counts-map) (:ceafm-mention-fn counts-map))
                                                     (prf (:ceafm-coref-tp counts-map) (:ceafm-coref-fp counts-map) (:ceafm-coref-fn counts-map))
                                                     (prf (:ceafm-allow_partial-mention-tp counts-map) (:ceafm-allow_partial-mention-fp counts-map) (:ceafm-allow_partial-mention-fn counts-map))
                                                     (prf (:ceafm-allow_partial-coref-tp counts-map) (:ceafm-allow_partial-coref-fp counts-map) (:ceafm-allow_partial-coref-fn counts-map))
                                                     (prf (:lea-mention-tp counts-map) (:lea-mention-fp counts-map) (:lea-mention-fn counts-map))
                                                     (prf (:lea-coref-tp counts-map) (:lea-coref-fp counts-map) (:lea-coref-fn counts-map))
                                                     (prf (:lea-allow_partial-mention-tp counts-map) (:lea-allow_partial-mention-fp counts-map) (:lea-allow_partial-mention-fn counts-map))
                                                     (prf (:lea-allow_partial-coref-tp counts-map) (:lea-allow_partial-coref-fp counts-map) (:lea-allow_partial-coref-fn counts-map))
                                                     (prf (:muc-mention-tp counts-map) (:muc-mention-fp counts-map) (:muc-mention-fn counts-map))
                                                     (prf (:muc-coref-tp counts-map) (:muc-coref-fp counts-map) (:muc-coref-fn counts-map))
                                                     (prf (:muc-allow_partial-mention-tp counts-map) (:muc-allow_partial-mention-fp counts-map) (:muc-allow_partial-mention-fn counts-map))
                                                     (prf (:muc-allow_partial-coref-tp counts-map) (:muc-allow_partial-coref-fp counts-map) (:muc-allow_partial-coref-fn counts-map))
                                                     ))
                          "\n")))


(deftask output-coref-results []
         "Parse log files in the .intermediate-results directory and output to the final results
         file (which is written to the input directory containing the files being evaluated)."
         (with-pre-wrap fileset
                        (let [intermediate-results-directory (:coref-intermediate-results-directory fileset)
                              output-file (file (:input-directory fileset) "coref_results.tsv")]
                             (with-open [w (clojure.java.io/writer output-file :encoding "UTF-8")]
                                        ;; write the file header
                                        (write-coref-output-file-header w)
                                        (print-total-stats
                                          w
                                          (reduce-kv (fn [totals document-id document-map]
                                                         ;; input is sorted at this point, so for each document, write a line
                                                         ;; containing the performance metrics for that document. Update the total
                                                         ;; counts in the 'totals' variable, then after all documents have been
                                                         ;; processed, write the 'total' stats to the file.
                                                         (let [doc-results (compile-coref-results-for-document document-map)
                                                               [bcub-mention-tp bcub-mention-fp bcub-mention-fn _ _ _
                                                                bcub-coref-tp bcub-coref-fp bcub-coref-fn _ _ _
                                                                bcub-allow_partial-mention-tp bcub-allow_partial-mention-fp bcub-allow_partial-mention-fn _ _ _
                                                                bcub-allow_partial-coref-tp bcub-allow_partial-coref-fp bcub-allow_partial-coref-fn _ _ _
                                                                blanc-mention-tp blanc-mention-fp blanc-mention-fn _ _ _
                                                                blanc-coref-tp blanc-coref-fp blanc-coref-fn _ _ _
                                                                blanc-allow_partial-mention-tp blanc-allow_partial-mention-fp blanc-allow_partial-mention-fn _ _ _
                                                                blanc-allow_partial-coref-tp blanc-allow_partial-coref-fp blanc-allow_partial-coref-fn _ _ _
                                                                ceafe-mention-tp ceafe-mention-fp ceafe-mention-fn _ _ _
                                                                ceafe-coref-tp ceafe-coref-fp ceafe-coref-fn _ _ _
                                                                ceafe-allow_partial-mention-tp ceafe-allow_partial-mention-fp ceafe-allow_partial-mention-fn _ _ _
                                                                ceafe-allow_partial-coref-tp ceafe-allow_partial-coref-fp ceafe-allow_partial-coref-fn _ _ _
                                                                ceafm-mention-tp ceafm-mention-fp ceafm-mention-fn _ _ _
                                                                ceafm-coref-tp ceafm-coref-fp ceafm-coref-fn _ _ _
                                                                ceafm-allow_partial-mention-tp ceafm-allow_partial-mention-fp ceafm-allow_partial-mention-fn _ _ _
                                                                ceafm-allow_partial-coref-tp ceafm-allow_partial-coref-fp ceafm-allow_partial-coref-fn _ _ _
                                                                lea-mention-tp lea-mention-fp lea-mention-fn _ _ _
                                                                lea-coref-tp lea-coref-fp lea-coref-fn _ _ _
                                                                lea-allow_partial-mention-tp lea-allow_partial-mention-fp lea-allow_partial-mention-fn _ _ _
                                                                lea-allow_partial-coref-tp lea-allow_partial-coref-fp lea-allow_partial-coref-fn _ _ _
                                                                muc-mention-tp muc-mention-fp muc-mention-fn _ _ _
                                                                muc-coref-tp muc-coref-fp muc-coref-fn _ _ _
                                                                muc-allow_partial-mention-tp muc-allow_partial-mention-fp muc-allow_partial-mention-fn _ _ _
                                                                muc-allow_partial-coref-tp muc-allow_partial-coref-fp muc-allow_partial-coref-fn _ _ _] doc-results]

                                                              (.write w (str document-id "\t"
                                                                             (clojure.string/join "\t" doc-results) "\n"))

                                                              (update-totals totals {:bcub-mention-tp                bcub-mention-tp
                                                                                     :bcub-mention-fp                bcub-mention-fp
                                                                                     :bcub-mention-fn                bcub-mention-fn
                                                                                     :bcub-coref-tp                  bcub-coref-tp
                                                                                     :bcub-coref-fp                  bcub-coref-fp
                                                                                     :bcub-coref-fn                  bcub-coref-fn
                                                                                     :bcub-allow_partial-mention-tp  bcub-allow_partial-mention-tp
                                                                                     :bcub-allow_partial-mention-fp  bcub-allow_partial-mention-fp
                                                                                     :bcub-allow_partial-mention-fn  bcub-allow_partial-mention-fn
                                                                                     :bcub-allow_partial-coref-tp    bcub-allow_partial-coref-tp
                                                                                     :bcub-allow_partial-coref-fp    bcub-allow_partial-coref-fp
                                                                                     :bcub-allow_partial-coref-fn    bcub-allow_partial-coref-fn
                                                                                     :blanc-mention-tp               blanc-mention-tp
                                                                                     :blanc-mention-fp               blanc-mention-fp
                                                                                     :blanc-mention-fn               blanc-mention-fn
                                                                                     :blanc-coref-tp                 blanc-coref-tp
                                                                                     :blanc-coref-fp                 blanc-coref-fp
                                                                                     :blanc-coref-fn                 blanc-coref-fn
                                                                                     :blanc-allow_partial-mention-tp blanc-allow_partial-mention-tp
                                                                                     :blanc-allow_partial-mention-fp blanc-allow_partial-mention-fp
                                                                                     :blanc-allow_partial-mention-fn blanc-allow_partial-mention-fn
                                                                                     :blanc-allow_partial-coref-tp   blanc-allow_partial-coref-tp
                                                                                     :blanc-allow_partial-coref-fp   blanc-allow_partial-coref-fp
                                                                                     :blanc-allow_partial-coref-fn   blanc-allow_partial-coref-fn
                                                                                     :ceafe-mention-tp               ceafe-mention-tp
                                                                                     :ceafe-mention-fp               ceafe-mention-fp
                                                                                     :ceafe-mention-fn               ceafe-mention-fn
                                                                                     :ceafe-coref-tp                 ceafe-coref-tp
                                                                                     :ceafe-coref-fp                 ceafe-coref-fp
                                                                                     :ceafe-coref-fn                 ceafe-coref-fn
                                                                                     :ceafe-allow_partial-mention-tp ceafe-allow_partial-mention-tp
                                                                                     :ceafe-allow_partial-mention-fp ceafe-allow_partial-mention-fp
                                                                                     :ceafe-allow_partial-mention-fn ceafe-allow_partial-mention-fn
                                                                                     :ceafe-allow_partial-coref-tp   ceafe-allow_partial-coref-tp
                                                                                     :ceafe-allow_partial-coref-fp   ceafe-allow_partial-coref-fp
                                                                                     :ceafe-allow_partial-coref-fn   ceafe-allow_partial-coref-fn
                                                                                     :ceafm-mention-tp               ceafm-mention-tp
                                                                                     :ceafm-mention-fp               ceafm-mention-fp
                                                                                     :ceafm-mention-fn               ceafm-mention-fn
                                                                                     :ceafm-coref-tp                 ceafm-coref-tp
                                                                                     :ceafm-coref-fp                 ceafm-coref-fp
                                                                                     :ceafm-coref-fn                 ceafm-coref-fn
                                                                                     :ceafm-allow_partial-mention-tp ceafm-allow_partial-mention-tp
                                                                                     :ceafm-allow_partial-mention-fp ceafm-allow_partial-mention-fp
                                                                                     :ceafm-allow_partial-mention-fn ceafm-allow_partial-mention-fn
                                                                                     :ceafm-allow_partial-coref-tp   ceafm-allow_partial-coref-tp
                                                                                     :ceafm-allow_partial-coref-fp   ceafm-allow_partial-coref-fp
                                                                                     :ceafm-allow_partial-coref-fn   ceafm-allow_partial-coref-fn
                                                                                     :lea-mention-tp                 lea-mention-tp
                                                                                     :lea-mention-fp                 lea-mention-fp
                                                                                     :lea-mention-fn                 lea-mention-fn
                                                                                     :lea-coref-tp                   lea-coref-tp
                                                                                     :lea-coref-fp                   lea-coref-fp
                                                                                     :lea-coref-fn                   lea-coref-fn
                                                                                     :lea-allow_partial-mention-tp   lea-allow_partial-mention-tp
                                                                                     :lea-allow_partial-mention-fp   lea-allow_partial-mention-fp
                                                                                     :lea-allow_partial-mention-fn   lea-allow_partial-mention-fn
                                                                                     :lea-allow_partial-coref-tp     lea-allow_partial-coref-tp
                                                                                     :lea-allow_partial-coref-fp     lea-allow_partial-coref-fp
                                                                                     :lea-allow_partial-coref-fn     lea-allow_partial-coref-fn
                                                                                     :muc-mention-tp                 muc-mention-tp
                                                                                     :muc-mention-fp                 muc-mention-fp
                                                                                     :muc-mention-fn                 muc-mention-fn
                                                                                     :muc-coref-tp                   muc-coref-tp
                                                                                     :muc-coref-fp                   muc-coref-fp
                                                                                     :muc-coref-fn                   muc-coref-fn
                                                                                     :muc-allow_partial-mention-tp   muc-allow_partial-mention-tp
                                                                                     :muc-allow_partial-mention-fp   muc-allow_partial-mention-fp
                                                                                     :muc-allow_partial-mention-fn   muc-allow_partial-mention-fn
                                                                                     :muc-allow_partial-coref-tp     muc-allow_partial-coref-tp
                                                                                     :muc-allow_partial-coref-fp     muc-allow_partial-coref-fp
                                                                                     :muc-allow_partial-coref-fn     muc-allow_partial-coref-fn})))
                                                     {}
                                                     (into (sorted-map) (compile-coref-results intermediate-results-directory))))))
                        ;(println (str "Evaluation complete. Please find the computed coreference evaluation results in "
                        ;              (.getAbsolutePath (file (:input-directory fileset) "coref_results.tsv"))))
                        fileset))

(deftask eval-coreference
         "Evaluates a set of files in the CoNLL-Coref 2011/12 file format against CRAFT coreference
          identity chain annotations. If no input arguments are specified then this tasks uses the
          Docker-specific setup by default."
         [b eval-base-directory VAL str "The path to the base directory that contains the coreference evaluation scripts directory. MUST BE ABSOLUTE PATH."
          i input-directory VAL str "The path to the directory of coreference files to test. MUST BE ABSOLUTE PATH."
          g gold-standard-directory VAL str "The path to the directory containing the gold standard coreference files. MUST BE ABSOLUTE PATH."
          s scorer-directory VAL str "The path to the scorer.pl file. MUST BE ABSOLUTE PATH."]
         (comp (if (nil? eval-base-directory)
                 (coref-eval-setup-docker)
                 (coref-eval-setup :eval-base-directory eval-base-directory
                                   :input-directory input-directory
                                   :gold-standard-directory gold-standard-directory
                                   :scorer-directory scorer-directory))

               ;; run all coreference evaluations
               (run-coref-eval :metric "muc" :allow-partial-matches false)
               (run-coref-eval :metric "muc" :allow-partial-matches true)
               (run-coref-eval :metric "bcub" :allow-partial-matches false)
               (run-coref-eval :metric "bcub" :allow-partial-matches true)
               (run-coref-eval :metric "ceafm" :allow-partial-matches false)
               (run-coref-eval :metric "ceafm" :allow-partial-matches true)
               (run-coref-eval :metric "ceafe" :allow-partial-matches false)
               (run-coref-eval :metric "ceafe" :allow-partial-matches true)
               (run-coref-eval :metric "blanc" :allow-partial-matches false)
               (run-coref-eval :metric "blanc" :allow-partial-matches true)
               (run-coref-eval :metric "lea" :allow-partial-matches false)
               (run-coref-eval :metric "lea" :allow-partial-matches true)

               ;; compile and output the results to a file in the input-directory
               (output-coref-results)))
