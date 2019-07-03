(ns craft-eval.univ-dependency-test
    (:require [clojure.test :refer :all]
      [craft-eval.univ-dependency :refer :all]
      [test-with-files.core :refer [with-tmp-dir tmp-dir]]
      [clojure.java.io :as io])
    (:import java.io.StringWriter
      java.lang.Math))

(def dependency-log-str (clojure.string/join "\n"
                                             (list
                                               "Evaluating 11532192.conllu"
                                               "LAS F1 Score: 99.88"
                                               "MLAS Score: 99.73"
                                               "BLEX Score: 99.76"
                                               "------------------ end 11532192.conllu"
                                               "Evaluating 11597317.conllu"
                                               "LAS F1 Score: 100.00"
                                               "MLAS Score: 100.00"
                                               "BLEX Score: 100.00"
                                               "------------------ end 11597317.conllu")))

(deftest extract-dependency-results-test
         (testing "Extract dependency info from string"
                  (is (= '(["11532192" {:las  99.88
                                        :mlas 99.73
                                        :blex 99.76}]
                            ["11597317" {:las  100.00
                                         :mlas 100.00
                                         :blex 100.00}])
                         (extract-dependency-results-from-string dependency-log-str)))))




(deftest compile-dependency-results-test
         (with-tmp-dir
           (let [log-dir (io/file (io/as-file tmp-dir) "log-files1")
                 log-file (io/file log-dir "dependency.log")]

                (.mkdirs log-dir)
                (spit log-file dependency-log-str)

                (is (.exists log-file))

                (testing "Compile dependency results from sample log file containing only two documents"
                         (is (= {"11532192" {:las  99.88
                                             :mlas 99.73
                                             :blex 99.76}
                                 "11597317" {:las  100.00
                                             :mlas 100.00
                                             :blex 100.00}}
                                (compile-dependency-results (io/as-file log-dir))))))))



(deftest serialize-dependency-results-test
         (with-tmp-dir
           (let [log-dir (io/file (io/as-file tmp-dir) "log-files2")
                 log-file (io/file log-dir "dependency.log")
                 writer (StringWriter.)]

                (.mkdirs log-dir)
                (spit log-file dependency-log-str)

                (is (.exists log-file))

                (testing "that the results get output to the writer correctly."
                         (serialize-univ-dependency-results log-dir writer)
                         ;; test that the document lines are correct
                         (is (= (str (clojure.string/join "\t" '("#document-id" "LAS" "MLAS" "BLEX")) "\n"
                                     (clojure.string/join "\t" '("11532192" "99.88" "99.73" "99.76")) "\n"
                                     (clojure.string/join "\t" '("11597317" "100.0" "100.0" "100.0")))
                                (clojure.string/join "\n" (take 3 (clojure.string/split-lines (.toString writer))))))

                         ;; now test the final AVERAGE line for correct values
                         ;; unable to do this as a string test due to some round-off error appearing in the string

                         (let [match (re-find #"AVERAGE\t(\d+.\d+)\t(\d+.\d+)\t(\d+.\d+)"
                                              (last (clojure.string/split-lines (.toString writer))))]
                              (is (= 4 (count match)))
                              (is (< (Math/abs (- (/ 199.88 2.0) (Double/parseDouble (nth match 1)))) 1e-6))
                              (is (< (Math/abs (- (/ 199.73 2.0) (Double/parseDouble (nth match 2)))) 1e-6))
                              (is (< (Math/abs (- (/ 199.76 2.0) (Double/parseDouble (nth match 3)))) 1e-6)))))))
