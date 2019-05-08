(ns craft-eval.dependency)

;;; This code is specific to the output produced by the MaltEval tool.

;;; The dependency parse scoring script outputs results to console, which we pipe to a file.
;;; The output looks like the following for the individual documents:
;;;
;;; ====================================================
;;; Gold:   CRAFT.bill.git/derived-dependency-gold/11532192.conll
;;; Parsed: CRAFT.bill.git/derived-dependency/11532192.conll
;;; ====================================================
;;; GroupBy-> Token
;;;
;;; ====================================================
;;;
;;; accuracy / Metric:UAS   accuracy / Metric:LAS   Token
;;; ---------------------------------------------------------
;;; 0.997                   0.993                   Row mean
;;; 8072                    8072                    Row count
;;; ---------------------------------------------------------
;;;


(defn extract-document-dependency-results-from-string [log-str]
      "extract results for individual documents"
      (map (fn [m] (let [[match document-id uas las] m]
                        (vector document-id {:uas (Double/parseDouble uas)
                                             :las (Double/parseDouble las)})))
           (re-seq #"(?m)^Gold: .*[/\\](\d+)\.conll\nParsed: .*\d+\.conll\n===+\nGroupBy-> Token\n\n===+\n\naccuracy / Metric:UAS   accuracy / Metric:LAS   Token\n----+\n(\d\.?\d*)\s+(\d\.?\d*)\s+Row mean\n"
                   log-str)))

;;; The dependency parse looks like the following for aggregated scores (UAS is listed before LAS in the file):
;;;
;;; ====================================================
;;; Mean:
;;;
;;; ====================================================
;;;
;;; Agg. accuracy   File number
;;; ---------------------------
;;; 1               Row mean
;;; 67              Row count
;;; ---------------------------

(defn extract-aggregate-dependency-results-from-string [log-str]
      "extract aggregated results for all documents evaluated"
      (map (fn [m] (let [[match uas intervening las] m]
                        (vector "TOTAL" {:uas (Double/parseDouble uas)
                                         :las (Double/parseDouble las)})))
           (re-seq #"(?m)^Agg. accuracy   File number\n---+\n(\d\.?\d*)\s+Row mean\n(.*\n)+Agg. accuracy   File number\n---+\n(\d\.?\d*)\s+Row mean\n"
                   log-str)))


(defn extract-dependency-results-from-file [log-file]
      "extract dependency parse evaluation results from a log file created during the evaluation process.
      The regular expression below extracts the relevant information."
      (println (str "Processing file: " (.getAbsolutePath log-file)))
      (let [file-str (slurp log-file)]
           (into {} (conj (vec (extract-document-dependency-results-from-string file-str))
                          (first (extract-aggregate-dependency-results-from-string file-str))))))


(defn write-dependency-output-file-header [writer]
      "The dependency output file has 3 columns. This function writes the header for the file.
      The first column is document id. Then for each document there are scores for UAS and LAS."
      (.write writer (str (clojure.string/join "\t" '("#document-id" "UAS" "LAS")) "\n")))


(defn serialize-dependency-results [log-file writer]
      "Parse log file and output all results to the writer, including
      a line that has aggregated 'total' results."
      ;; write the file header
      (write-dependency-output-file-header writer)
      (doall (map (fn [input-vec]
                      (let [[document-id results-map] input-vec
                            uas (:uas results-map)
                            las (:las results-map)]
                           (.write writer (str (clojure.string/join "\t" (list document-id
                                                                               uas
                                                                               las))
                                               "\n"))))

                  (into (sorted-map) (extract-dependency-results-from-file log-file)))))
