(ns craft-eval.coref-test
    (:require [clojure.test :refer :all]
      [craft-eval.coref :refer :all]
      [test-with-files.core :refer [with-tmp-dir tmp-dir]]
      [clojure.java.io :as io])
    (:import java.io.StringWriter))

(def muc-results-str (clojure.string/join "\n"
                                          (list
                                            "Evaluating /tmp/craft/coref/gold-conll/11532192.conll"
                                            "version: 9.0.0-alpha /Users/bill/Dropbox/work/projects/craft-shared-task-2019/cr-task/reference-coreference-scorers.bill.git/lib/CorScorer.pm"
                                            " (48141) ; part 000:"
                                            "Total key mentions: 1173"
                                            "Total response mentions: 1174"
                                            "Strictly correct identified mentions: 1169"
                                            "Partially correct identified mentions: 0"
                                            "No identified: 4"
                                            "Invented: 5"
                                            "Recall: (925 / 930) 99.46%	Precision: (925 / 931) 99.35%	F1: 99.4%"
                                            "--------------------------------------------------------------------------"
                                            ""
                                            "====== TOTALS ======="
                                            "Identification of Mentions: Recall: (1169 / 1173) 99.65%	Precision: (1169 / 1174) 99.57%	F1: 99.61%"
                                            "--------------------------------------------------------------------------"
                                            "Coreference: Recall: (925 / 930) 99.46%	Precision: (925 / 931) 99.35%	F1: 99.4%"
                                            "--------------------------------------------------------------------------"
                                            "------------------ end /tmp/craft/coref/gold-conll/11532192.conll"
                                            "Evaluating /tmp/craft/coref/gold-conll/11597317.conll"
                                            "version: 9.0.0-alpha /Users/bill/Dropbox/work/projects/craft-shared-task-2019/cr-task/reference-coreference-scorers.bill.git/lib/CorScorer.pm"
                                            "(138691); part 000:"
                                            "Total key mentions: 354"
                                            "Total response mentions: 354"
                                            "Strictly correct identified mentions: 354"
                                            "Partially correct identified mentions: 0"
                                            "No identified: 0"
                                            "Invented: 0"
                                            "Recall: (291 / 291) 100%	Precision: (291 / 291) 100%	F1: 100%"
                                            "--------------------------------------------------------------------------"
                                            ""
                                            "====== TOTALS ======="
                                            "Identification of Mentions: Recall: (354 / 354) 100%	Precision: (354 / 354) 100%	F1: 100%"
                                            "--------------------------------------------------------------------------"
                                            "Coreference: Recall: (291 / 291) 100%	Precision: (291 / 291) 100%	F1: 100%"
                                            "--------------------------------------------------------------------------"
                                            "------------------ end /tmp/craft/coref/gold-conll/11597317.conll"

                                            )))

(deftest extract-coref-results-test
         (testing "Extract coref info from string"
                  (is (= '(["11532192" {:muc-mention-tp 1169
                                        :muc-mention-fp 5
                                        :muc-mention-fn 4
                                        :muc-coref-tp   925
                                        :muc-coref-fp   6
                                        :muc-coref-fn   5}]
                            ["11597317" {:muc-mention-tp 354
                                         :muc-mention-fp 0
                                         :muc-mention-fn 0
                                         :muc-coref-tp   291
                                         :muc-coref-fp   0
                                         :muc-coref-fn   0}])
                         (extract-coref-results-from-string muc-results-str "muc")))))


(def blanc-results-str (clojure.string/join "\n"
                                            (list
                                              "Evaluating /tmp/craft/coref/gold-conll/11532192.conll"
                                              "version: 9.0.0-alpha /Users/bill/Dropbox/work/projects/craft-shared-task-2019/cr-task/reference-coreference-scorers.bill.git/lib/CorScorer.pm"
                                              ":"
                                              "Total key mentions: 1173"
                                              "Total response mentions: 1174"
                                              "Strictly correct identified mentions: 1169"
                                              "Partially correct identified mentions: 0"
                                              "No identified: 4"
                                              "Invented: 5"
                                              ""
                                              "====== TOTALS ======="
                                              "Identification of Mentions: Recall: (1169 / 1173) 99.65%	Precision: (1169 / 1174) 99.57%	F1: 99.61%"
                                              "--------------------------------------------------------------------------"
                                              ""
                                              "Coreference:"
                                              "Coreference links: Recall: (11113 / 11207) 99.16%	Precision: (11113 / 11133) 99.82%	F1: 99.48%"
                                              "--------------------------------------------------------------------------"
                                              "Non-coreference links: Recall: (671534 / 676171) 99.31%	Precision: (671534 / 677418) 99.13%	F1: 99.22%"
                                              "--------------------------------------------------------------------------"
                                              "BLANC: Recall: (0.992377326191579 / 1) 99.23%	Precision: (0.994758808447183 / 1) 99.47%	F1: 99.35%"
                                              "--------------------------------------------------------------------------"
                                              "------------------ end /tmp/craft/coref/gold-conll/11532192.conll"
                                              "Evaluating /tmp/craft/coref/gold-conll/11597317.conll"
                                              "version: 9.0.0-alpha /Users/bill/Dropbox/work/projects/craft-shared-task-2019/cr-task/reference-coreference-scorers.bill.git/lib/CorScorer.pm"
                                              ":"
                                              "Total key mentions: 354"
                                              "Total response mentions: 354"
                                              "Strictly correct identified mentions: 354"
                                              "Partially correct identified mentions: 0"
                                              "No identified: 0"
                                              "Invented: 0"
                                              ""
                                              "====== TOTALS ======="
                                              "Identification of Mentions: Recall: (354 / 354) 100%	Precision: (354 / 354) 100%	F1: 100%"
                                              "--------------------------------------------------------------------------"
                                              ""
                                              "Coreference:"
                                              "Coreference links: Recall: (3153 / 3153) 100%	Precision: (3153 / 3153) 100%	F1: 100%"
                                              "--------------------------------------------------------------------------"
                                              "Non-coreference links: Recall: (59328 / 59328) 100%	Precision: (59328 / 59328) 100%	F1: 100%"
                                              "--------------------------------------------------------------------------"
                                              "BLANC: Recall: (1 / 1) 100%	Precision: (1 / 1) 100%	F1: 100%"
                                              "--------------------------------------------------------------------------"
                                              "------------------ end /tmp/craft/coref/gold-conll/11597317.conll"
                                              )))

(deftest extract-coref-results-blanc-test
         (testing "Extract coref info from string"
                  (is (= '(["11532192" {:blanc-mention-tp   1169
                                        :blanc-mention-fp   5
                                        :blanc-mention-fn   4
                                        :blanc-coref-tp     11113
                                        :blanc-coref-fp     20
                                        :blanc-coref-fn     94
                                        :blanc-non-coref-tp 671534
                                        :blanc-non-coref-fp 5884
                                        :blanc-non-coref-fn 4637}]
                            ["11597317" {:blanc-mention-tp   354
                                         :blanc-mention-fp   0
                                         :blanc-mention-fn   0
                                         :blanc-coref-tp     3153
                                         :blanc-coref-fp     0
                                         :blanc-coref-fn     0
                                         :blanc-non-coref-tp 59328
                                         :blanc-non-coref-fp 0
                                         :blanc-non-coref-fn 0}])
                         (extract-coref-results-blanc-from-string blanc-results-str "blanc")))))


(deftest compile-coref-results-test
         (with-tmp-dir
           (let [log-dir (io/file (io/as-file tmp-dir) "log-files1")
                 muc-results-file (io/file log-dir "muc.log")
                 blanc-results-file (io/file log-dir "blanc.log")]

                (.mkdirs log-dir)
                (spit muc-results-file muc-results-str)
                (spit blanc-results-file blanc-results-str)

                (is (.exists muc-results-file))
                (is (.exists blanc-results-file))

                (testing "Compile coref results from sample log files for two metrics containing only two documents"
                         (is (= {"11532192" {:blanc-mention-tp   1169
                                             :blanc-mention-fp   5
                                             :blanc-mention-fn   4
                                             :blanc-coref-tp     11113
                                             :blanc-coref-fp     20
                                             :blanc-coref-fn     94
                                             :blanc-non-coref-tp 671534
                                             :blanc-non-coref-fp 5884
                                             :blanc-non-coref-fn 4637
                                             :muc-mention-tp     1169
                                             :muc-mention-fp     5
                                             :muc-mention-fn     4
                                             :muc-coref-tp       925
                                             :muc-coref-fp       6
                                             :muc-coref-fn       5}
                                 "11597317" {:blanc-mention-tp   354
                                             :blanc-mention-fp   0
                                             :blanc-mention-fn   0
                                             :blanc-coref-tp     3153
                                             :blanc-coref-fp     0
                                             :blanc-coref-fn     0
                                             :blanc-non-coref-tp 59328
                                             :blanc-non-coref-fp 0
                                             :blanc-non-coref-fn 0
                                             :muc-mention-tp     354
                                             :muc-mention-fp     0
                                             :muc-mention-fn     0
                                             :muc-coref-tp       291
                                             :muc-coref-fp       0
                                             :muc-coref-fn       0}}
                                (compile-coref-results (io/as-file log-dir))))))))


(deftest compile-coref-results-test-with-one-allow_partial-file
         (with-tmp-dir
           (let [log-dir (io/file (io/as-file tmp-dir) "log-files3")
                 muc-results-file (io/file log-dir "muc.allow_partial.log")
                 blanc-results-file (io/file log-dir "blanc.log")]

                (.mkdirs log-dir)
                (spit muc-results-file muc-results-str)
                (spit blanc-results-file blanc-results-str)

                (is (.exists muc-results-file))
                (is (.exists blanc-results-file))

                (testing "Compile coref results from sample log files for two metrics containing only
                two documents where one of the documents is an allow_partial doc"
                         (is (= {"11532192" {:blanc-mention-tp             1169
                                             :blanc-mention-fp             5
                                             :blanc-mention-fn             4
                                             :blanc-coref-tp               11113
                                             :blanc-coref-fp               20
                                             :blanc-coref-fn               94
                                             :blanc-non-coref-tp           671534
                                             :blanc-non-coref-fp           5884
                                             :blanc-non-coref-fn           4637
                                             :muc-allow_partial-mention-tp 1169
                                             :muc-allow_partial-mention-fp 5
                                             :muc-allow_partial-mention-fn 4
                                             :muc-allow_partial-coref-tp   925
                                             :muc-allow_partial-coref-fp   6
                                             :muc-allow_partial-coref-fn   5}
                                 "11597317" {:blanc-mention-tp             354
                                             :blanc-mention-fp             0
                                             :blanc-mention-fn             0
                                             :blanc-coref-tp               3153
                                             :blanc-coref-fp               0
                                             :blanc-coref-fn               0
                                             :blanc-non-coref-tp           59328
                                             :blanc-non-coref-fp           0
                                             :blanc-non-coref-fn           0
                                             :muc-allow_partial-mention-tp 354
                                             :muc-allow_partial-mention-fp 0
                                             :muc-allow_partial-mention-fn 0
                                             :muc-allow_partial-coref-tp   291
                                             :muc-allow_partial-coref-fp   0
                                             :muc-allow_partial-coref-fn   0}}
                                (compile-coref-results (io/as-file log-dir))))))))


(deftest count-coref-output-file-headings-test
         (testing "Make sure there file headings count is correct"
                  (is (= 159 (count coref-output-file-headings)))))


(deftest compile-coref-results-for-document-test
         (testing "Given a mapping from metric to tp/fp/fn counts, test that the proper p,r,&f get calculated and returned."
                  (is (= {:blanc-mention-tp   1169
                          :blanc-mention-fp   5
                          :blanc-mention-fn   4
                          :blanc-mention-p    1169/1174
                          :blanc-mention-r    1169/1173
                          :blanc-mention-f    (/ (* 2 1169) (+ (* 2 1169) 5 4))
                          :blanc-coref-tp     11113
                          :blanc-coref-fp     20
                          :blanc-coref-fn     94
                          :blanc-coref-p      11113/11133
                          :blanc-coref-r      11113/11207
                          :blanc-coref-f      (/ (* 2 11113) (+ (* 2 11113) 20 94))
                          :blanc-non-coref-tp 671534
                          :blanc-non-coref-fp 5884
                          :blanc-non-coref-fn 4637
                          :blanc-non-coref-p  671534/677418
                          :blanc-non-coref-r  671534/676171
                          :blanc-non-coref-f  (/ (* 2 671534) (+ (* 2 671534) 5884 4637))
                          :blanc-score        (/ (+ (/ (* 2 11113) (+ (* 2 11113) 20 94)) (/ (* 2 671534) (+ (* 2 671534) 5884 4637))) 2)
                          :muc-mention-tp     1169
                          :muc-mention-fp     5
                          :muc-mention-fn     4
                          :muc-mention-p      1169/1174
                          :muc-mention-r      1169/1173
                          :muc-mention-f      (/ (* 2 1169) (+ (* 2 1169) 5 4))
                          :muc-coref-tp       925
                          :muc-coref-fp       6
                          :muc-coref-fn       5
                          :muc-coref-p        925/931
                          :muc-coref-r        925/930
                          :muc-coref-f        (/ (* 2 925) (+ (* 2 925) 6 5))}
                         (compile-coref-results-for-document {:blanc-mention-tp   1169
                                                              :blanc-mention-fp   5
                                                              :blanc-mention-fn   4
                                                              :blanc-coref-tp     11113
                                                              :blanc-coref-fp     20
                                                              :blanc-coref-fn     94
                                                              :blanc-non-coref-tp 671534
                                                              :blanc-non-coref-fp 5884
                                                              :blanc-non-coref-fn 4637
                                                              :muc-mention-tp     1169
                                                              :muc-mention-fp     5
                                                              :muc-mention-fn     4
                                                              :muc-coref-tp       925
                                                              :muc-coref-fp       6
                                                              :muc-coref-fn       5})))))


(deftest serialize-coref-results-test
         (with-tmp-dir
           (let [log-dir (io/file (io/as-file tmp-dir) "log-files2")
                 bcub-log-file (io/file log-dir "bcub.log")
                 bcub-allow-partial-log-file (io/file log-dir "bcub.allow_partial.log")
                 blanc-log-file (io/file log-dir "blanc.log")
                 blanc-allow-partial-log-file (io/file log-dir "blanc.allow_partial.log")
                 ceafe-log-file (io/file log-dir "ceafe.log")
                 ceafe-allow-partial-log-file (io/file log-dir "ceafe.allow_partial.log")
                 ceafm-log-file (io/file log-dir "ceafm.log")
                 ceafm-allow-partial-log-file (io/file log-dir "ceafm.allow_partial.log")
                 lea-log-file (io/file log-dir "lea.log")
                 lea-allow-partial-log-file (io/file log-dir "lea.allow_partial.log")
                 muc-log-file (io/file log-dir "muc.log")
                 muc-allow-partial-log-file (io/file log-dir "muc.allow_partial.log")
                 writer (StringWriter.)]

                (.mkdirs log-dir)

                (spit bcub-log-file muc-results-str)
                (spit bcub-allow-partial-log-file muc-results-str)
                (spit ceafe-log-file muc-results-str)
                (spit ceafe-allow-partial-log-file muc-results-str)
                (spit ceafm-log-file muc-results-str)
                (spit ceafm-allow-partial-log-file muc-results-str)
                (spit lea-log-file muc-results-str)
                (spit lea-allow-partial-log-file muc-results-str)
                (spit muc-log-file muc-results-str)
                (spit muc-allow-partial-log-file muc-results-str)

                (spit blanc-log-file blanc-results-str)
                (spit blanc-allow-partial-log-file blanc-results-str)

                (is (.exists bcub-log-file))
                (is (.exists bcub-allow-partial-log-file))
                (is (.exists blanc-log-file))
                (is (.exists blanc-allow-partial-log-file))
                (is (.exists ceafe-log-file))
                (is (.exists ceafe-allow-partial-log-file))
                (is (.exists ceafm-log-file))
                (is (.exists ceafm-allow-partial-log-file))
                (is (.exists lea-log-file))
                (is (.exists lea-allow-partial-log-file))
                (is (.exists muc-log-file))
                (is (.exists muc-allow-partial-log-file))

                (testing "that the results get output to the writer correctly."
                         (serialize-coref-results log-dir writer)
                         (is (= "#document-id\t:bcub-mention-tp\t:bcub-mention-fp\t:bcub-mention-fn\t:bcub-mention-p\t:bcub-mention-r\t:bcub-mention-f\t:bcub-coref-tp\t:bcub-coref-fp\t:bcub-coref-fn\t:bcub-coref-p\t:bcub-coref-r\t:bcub-coref-f\t:bcub-allow_partial-mention-tp\t:bcub-allow_partial-mention-fp\t:bcub-allow_partial-mention-fn\t:bcub-allow_partial-mention-p\t:bcub-allow_partial-mention-r\t:bcub-allow_partial-mention-f\t:bcub-allow_partial-coref-tp\t:bcub-allow_partial-coref-fp\t:bcub-allow_partial-coref-fn\t:bcub-allow_partial-coref-p\t:bcub-allow_partial-coref-r\t:bcub-allow_partial-coref-f\t:blanc-mention-tp\t:blanc-mention-fp\t:blanc-mention-fn\t:blanc-mention-p\t:blanc-mention-r\t:blanc-mention-f\t:blanc-coref-tp\t:blanc-coref-fp\t:blanc-coref-fn\t:blanc-coref-p\t:blanc-coref-r\t:blanc-coref-f\t:blanc-non-coref-tp\t:blanc-non-coref-fp\t:blanc-non-coref-fn\t:blanc-non-coref-p\t:blanc-non-coref-r\t:blanc-non-coref-f\t:blanc-score\t:blanc-allow_partial-mention-tp\t:blanc-allow_partial-mention-fp\t:blanc-allow_partial-mention-fn\t:blanc-allow_partial-mention-p\t:blanc-allow_partial-mention-r\t:blanc-allow_partial-mention-f\t:blanc-allow_partial-coref-tp\t:blanc-allow_partial-coref-fp\t:blanc-allow_partial-coref-fn\t:blanc-allow_partial-coref-p\t:blanc-allow_partial-coref-r\t:blanc-allow_partial-coref-f\t:blanc-allow_partial-non-coref-tp\t:blanc-allow_partial-non-coref-fp\t:blanc-allow_partial-non-coref-fn\t:blanc-allow_partial-non-coref-p\t:blanc-allow_partial-non-coref-r\t:blanc-allow_partial-non-coref-f\t:blanc-allow_partial-score\t:ceafe-mention-tp\t:ceafe-mention-fp\t:ceafe-mention-fn\t:ceafe-mention-p\t:ceafe-mention-r\t:ceafe-mention-f\t:ceafe-coref-tp\t:ceafe-coref-fp\t:ceafe-coref-fn\t:ceafe-coref-p\t:ceafe-coref-r\t:ceafe-coref-f\t:ceafe-allow_partial-mention-tp\t:ceafe-allow_partial-mention-fp\t:ceafe-allow_partial-mention-fn\t:ceafe-allow_partial-mention-p\t:ceafe-allow_partial-mention-r\t:ceafe-allow_partial-mention-f\t:ceafe-allow_partial-coref-tp\t:ceafe-allow_partial-coref-fp\t:ceafe-allow_partial-coref-fn\t:ceafe-allow_partial-coref-p\t:ceafe-allow_partial-coref-r\t:ceafe-allow_partial-coref-f\t:ceafm-mention-tp\t:ceafm-mention-fp\t:ceafm-mention-fn\t:ceafm-mention-p\t:ceafm-mention-r\t:ceafm-mention-f\t:ceafm-coref-tp\t:ceafm-coref-fp\t:ceafm-coref-fn\t:ceafm-coref-p\t:ceafm-coref-r\t:ceafm-coref-f\t:ceafm-allow_partial-mention-tp\t:ceafm-allow_partial-mention-fp\t:ceafm-allow_partial-mention-fn\t:ceafm-allow_partial-mention-p\t:ceafm-allow_partial-mention-r\t:ceafm-allow_partial-mention-f\t:ceafm-allow_partial-coref-tp\t:ceafm-allow_partial-coref-fp\t:ceafm-allow_partial-coref-fn\t:ceafm-allow_partial-coref-p\t:ceafm-allow_partial-coref-r\t:ceafm-allow_partial-coref-f\t:lea-mention-tp\t:lea-mention-fp\t:lea-mention-fn\t:lea-mention-p\t:lea-mention-r\t:lea-mention-f\t:lea-coref-tp\t:lea-coref-fp\t:lea-coref-fn\t:lea-coref-p\t:lea-coref-r\t:lea-coref-f\t:lea-allow_partial-mention-tp\t:lea-allow_partial-mention-fp\t:lea-allow_partial-mention-fn\t:lea-allow_partial-mention-p\t:lea-allow_partial-mention-r\t:lea-allow_partial-mention-f\t:lea-allow_partial-coref-tp\t:lea-allow_partial-coref-fp\t:lea-allow_partial-coref-fn\t:lea-allow_partial-coref-p\t:lea-allow_partial-coref-r\t:lea-allow_partial-coref-f\t:muc-mention-tp\t:muc-mention-fp\t:muc-mention-fn\t:muc-mention-p\t:muc-mention-r\t:muc-mention-f\t:muc-coref-tp\t:muc-coref-fp\t:muc-coref-fn\t:muc-coref-p\t:muc-coref-r\t:muc-coref-f\t:muc-allow_partial-mention-tp\t:muc-allow_partial-mention-fp\t:muc-allow_partial-mention-fn\t:muc-allow_partial-mention-p\t:muc-allow_partial-mention-r\t:muc-allow_partial-mention-f\t:muc-allow_partial-coref-tp\t:muc-allow_partial-coref-fp\t:muc-allow_partial-coref-fn\t:muc-allow_partial-coref-p\t:muc-allow_partial-coref-r\t:muc-allow_partial-coref-f\n11532192\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t11113\t20\t94\t11113/11133\t11113/11207\t11113/11170\t671534\t5884\t4637\t335767/338709\t671534/676171\t1343068/1353589\t30044504117/30239178260\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t11113\t20\t94\t11113/11133\t11113/11207\t11113/11170\t671534\t5884\t4637\t335767/338709\t671534/676171\t1343068/1353589\t30044504117/30239178260\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\t1169\t5\t4\t1169/1174\t1169/1173\t2338/2347\t925\t6\t5\t925/931\t185/186\t1850/1861\n11597317\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t3153\t0\t0\t1\t1\t1\t59328\t0\t0\t1\t1\t1\t1\t354\t0\t0\t1\t1\t1\t3153\t0\t0\t1\t1\t1\t59328\t0\t0\t1\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\t354\t0\t0\t1\t1\t1\t291\t0\t0\t1\t1\t1\nTOTAL\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t14266\t20\t94\t7133/7143\t7133/7180\t14266/14323\t730862\t5884\t4637\t365431/368373\t730862/735499\t1461724/1472245\t20969660011/21086965135\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t14266\t20\t94\t7133/7143\t7133/7180\t14266/14323\t730862\t5884\t4637\t365431/368373\t730862/735499\t1461724/1472245\t20969660011/21086965135\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\t1523\t5\t4\t1523/1528\t1523/1527\t3046/3055\t1216\t6\t5\t608/611\t1216/1221\t2432/2443\n"
                                (.toString writer)))))))











