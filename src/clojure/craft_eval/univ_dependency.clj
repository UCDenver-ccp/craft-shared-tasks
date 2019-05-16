(ns craft-eval.univ-dependency)

;;; This code is specific to the output produced by the CoNLL 2018 conll18_ud_eval.py script.

;;; The dependency parse scoring script outputs results to console, which we pipe to a file.
;;; The output looks like the following:
;;;
;;; Evaluating 11597317.conllu
;;; LAS F1 Score: 100.00
;;; MLAS Score: 100.00
;;; BLEX Score: 100.00
;;; ------------------ end 11597317.conllu

(defn extract-dependency-results-from-string [log-str]
      (map (fn [m] (let [[match document-id las mlas blex] m]
                        (vector document-id {:las  (Double/parseDouble las)
                                             :mlas (Double/parseDouble mlas)
                                             :blex (Double/parseDouble blex)})))
           (re-seq #"(?m)^Evaluating (\d+)\.conllu\nLAS F1 Score: (\d+\.\d+)\nMLAS Score: (\d+\.\d+)\nBLEX Score: (\d+\.\d+)\n-+ end \d+\.conllu"
                   log-str)))


(defn extract-dependency-results-from-file [log-file]
      "extract dependency parse evaluation results from a log file created during the evaluation process.
      The regular expression below extracts the relevant information."
      (println (str "Processing file: " (.getAbsolutePath log-file)))
      (extract-dependency-results-from-string (slurp log-file)))


(defn compile-dependency-results [intermediate-results-directory]
      "Parse log file in the .intermediate-results directory and compile results for
      each document and each metric used to gauge performance."
      (reduce (fn [m1 m2] (let [document-id (first m2)
                                performance-vals-map (last m2)]
                               (assoc m1 document-id performance-vals-map)))
              {}
              (apply concat (map (fn [f] (extract-dependency-results-from-file f))
                                 (filter #(and (.isFile %) (.endsWith (.getName %) ".log"))
                                         (file-seq intermediate-results-directory))))))


(defn write-dependency-output-file-header [writer]
      "The dependency output file has 4 columns. This function writes the header for the file.
      The first column is document id. Then for each document there are scores for LAS, MLAS, and BLEX"
      (.write writer (str (clojure.string/join "\t" '("#document-id" "LAS" "MLAS" "BLEX")) "\n")))


(defn write-total-dependency-stats [writer counts-map]
      "Given a map of total counts for tp, fp, & fn for each metric, write a
      line containing tp, fp, fn, p, r, & f for each metric."
      (.write writer (str (clojure.string/join "\t"
                                               (list "AVERAGE"
                                                     (/ (:las-sum counts-map) (:total-count counts-map))
                                                     (/ (:mlas-sum counts-map) (:total-count counts-map))
                                                     (/ (:blex-sum counts-map) (:total-count counts-map)))) "\n")))

(defn serialize-dependency-results [log-directory writer]
      "Parse log files in the specified log directory and output all results to
      the writer, including a line that has aggregated 'total' results."
      ;; write the file header
      (write-dependency-output-file-header writer)
      (write-total-dependency-stats
        writer
        (reduce-kv (fn [totals document-id results-map]
                       ;; input is sorted at this point, so for each document, write a line
                       ;; containing the performance metrics for that document. Update the total
                       ;; counts in the 'totals' variable, then after all documents have been
                       ;; processed, write the 'total' stats to the file.

                       (let [las (:las results-map)
                             mlas (:mlas results-map)
                             blex (:blex results-map)]
                            (.write writer (str (clojure.string/join "\t" (list document-id
                                                                                las
                                                                                mlas
                                                                                blex))
                                                "\n"))

                            (merge-with + totals {:total-count 1
                                                  :las-sum     las
                                                  :mlas-sum    mlas
                                                  :blex-sum    blex})))
                   {}
                   (into (sorted-map) (compile-dependency-results log-directory)))))
