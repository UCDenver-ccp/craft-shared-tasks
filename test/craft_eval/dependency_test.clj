(ns craft-eval.dependency-test
    (:require [clojure.test :refer :all]
      [craft-eval.dependency :refer :all]
      [test-with-files.core :refer [with-tmp-dir tmp-dir]]
      [clojure.java.io :as io])
    (:import java.io.StringWriter
      java.lang.Math))

(def document-dependency-log-str (clojure.string/join "\n"
                                                      (list
                                                        "Evaluation arguments: -e malt-eval-config.xml -s CRAFT.bill.git/derived-dependency/ -g CRAFT.bill.git/derived-dependency-gold/ --output result.out"
                                                        "===================================================="
                                                        "Gold:   CRAFT.bill.git/derived-dependency-gold/11532192.conll"
                                                        "Parsed: CRAFT.bill.git/derived-dependency/11532192.conll"
                                                        "===================================================="
                                                        "GroupBy-> Token"
                                                        ""
                                                        "===================================================="
                                                        ""
                                                        "accuracy / Metric:UAS   accuracy / Metric:LAS   Token"
                                                        "---------------------------------------------------------"
                                                        "0.997                   0.993                   Row mean"
                                                        "8072                    8072                    Row count"
                                                        "---------------------------------------------------------"
                                                        ""
                                                        "===================================================="
                                                        "Gold:   CRAFT.bill.git/derived-dependency-gold/11597317.conll"
                                                        "Parsed: CRAFT.bill.git/derived-dependency/11597317.conll"
                                                        "===================================================="
                                                        "GroupBy-> Token"
                                                        ""
                                                        "===================================================="
                                                        ""
                                                        "accuracy / Metric:UAS   accuracy / Metric:LAS   Token"
                                                        "---------------------------------------------------------"
                                                        "1                       1                       Row mean"
                                                        "2632                    2632                    Row count"
                                                        "---------------------------------------------------------")))

(deftest extract-document-dependency-results-test
         (testing "Extract dependency info from string"
                  (is (= '(["11532192" {:uas 0.997
                                        :las 0.993}]
                            ["11597317" {:uas 1.0
                                         :las 1.0}])
                         (extract-document-dependency-results-from-string document-dependency-log-str)))))

(def aggregate-dependency-log-str (clojure.string/join "\n"
                                                       (list
                                                         "===================================================="
                                                         "Mean:"
                                                         ""
                                                         "===================================================="
                                                         ""
                                                         "Agg. accuracy   File number"
                                                         "---------------------------"
                                                         "1               Row mean"
                                                         "67              Row count"
                                                         "---------------------------"
                                                         "0.997           <1>"
                                                         "1               <2>"
                                                         "1               <3>"
                                                         "1               <4>"
                                                         "Lots of intervening text here..."
                                                         "===================================================="
                                                         "Mean:"
                                                         ""
                                                         "===================================================="
                                                         ""
                                                         "Agg. accuracy   File number"
                                                         "---------------------------"
                                                         "1               Row mean"
                                                         "67              Row count"
                                                         "---------------------------"
                                                         "0.993           <1>"
                                                         "1               <2>"
                                                         "1               <3>"
                                                         )))

(deftest extract-aggregate-dependency-results-test
         (testing "Extract dependency info from string"
                  (is (= '(["TOTAL" {:uas 1.0
                                     :las 1.0}])
                         (extract-aggregate-dependency-results-from-string aggregate-dependency-log-str)))))



(deftest extract-dependency-results-from-file-test
         (with-tmp-dir
           (let [log-dir (io/file (io/as-file tmp-dir) "log-files1")
                 log-file (io/file log-dir "dependency.log")]

                (.mkdirs log-dir)
                (spit log-file (str document-dependency-log-str "\n" aggregate-dependency-log-str))

                (is (.exists log-file))

                (testing "Compile dependency results from sample log file containing only two documents"
                         (is (= {"11532192" {:uas 0.997
                                             :las 0.993}
                                 "11597317" {:uas 1.0
                                             :las 1.0}
                                 "TOTAL"    {:uas 1.0
                                             :las 1.0}}
                                (extract-dependency-results-from-file log-file)))))))


(deftest serialize-dependency-results-test
         (with-tmp-dir
           (let [log-dir (io/file (io/as-file tmp-dir) "log-files2")
                 log-file (io/file log-dir "dependency.log")
                 writer (StringWriter.)]

                (.mkdirs log-dir)
                (spit log-file (str document-dependency-log-str "\n" aggregate-dependency-log-str))

                (is (.exists log-file))

                (testing "that the results get output to the writer correctly."
                         (serialize-dependency-results log-file writer)
                         ;; test that the document lines are correct
                         (is (= (str (clojure.string/join "\t" '("#document-id" "UAS" "LAS")) "\n"
                                     (clojure.string/join "\t" '("11532192" "0.997" "0.993")) "\n"
                                     (clojure.string/join "\t" '("11597317" "1.0" "1.0")) "\n"
                                     (clojure.string/join "\t" '("TOTAL" "1.0" "1.0")))
                                (clojure.string/join "\n" (take 4 (clojure.string/split-lines (.toString writer))))))))))
