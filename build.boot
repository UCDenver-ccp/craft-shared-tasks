(def project 'edu.ucdenver.ccp/craft-eval)
(def version "0.1.0-SNAPSHOT")

(set-env! :resource-paths #{"resources" "src"}
          :source-paths #{"test"}
          :dependencies '[[org.clojure/clojure "RELEASE"]
                          [adzerk/boot-test "RELEASE" :scope "test"]
                          [test-with-files "0.1.1" :scope "test"]])

(require '[adzerk.boot-test :refer [test]]
         '[clojure.java.io :refer [file]]
         '[craft-eval.coref :refer [serialize-coref-results]]
         '[craft-eval.dependency :refer [serialize-dependency-results]])


(task-options!
  aot {:namespace #{'craft-eval.coref}}
  pom {:project     project
       :version     version
       :description "This project contains code used for post-processing log files produced
       while running evaluation scripts to guage performance of dependency parsers and
       coreference systems over the CRAFT corpus."
       :url         "https://github.com/UCDenver-ccp/craft-shared-tasks"
       :scm         {:url "https://github.com/UCDenver-ccp/craft-shared-tasks"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}}
  repl {:init-ns 'craft-eval.coref}
  jar {:file (str "craft-eval-" version "-standalone.jar")})

(deftask build
         "Build the project locally as a JAR."
         [d dir PATH #{str} "the set of directories to write to (target)."]
         (let [dir (if (seq dir) dir #{"target"})]
              (comp (aot) (pom) (uber :exclude #{#"(?i)^META-INF/INDEX.LIST$"
                                                 #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                                                 #"META-INF/maven/"}) (jar) (target :dir dir))))

;;; ==================================================================== ;;;
;;; ================= Dependency Parse Evaluation Tasks ================
;;; ==================================================================== ;;;

(deftask dependency-eval-setup
         "Sets constants used in the dependency parse evaluation."
         [b eval-base-directory VAL str "The path to the base directory that contains the dependency parse evaluation scripts directory. MUST BE ABSOLUTE PATH."
          i input-directory VAL str "The path to the directory of dependency parse files to test. MUST BE ABSOLUTE PATH."
          g gold-standard-directory VAL str "The path to the directory containing the gold standard dependency parse files. MUST BE ABSOLUTE PATH."
          s scorer-directory VAL str "The path to the conll18_ud_eval.py file. MUST BE ABSOLUTE PATH."]
         (with-pre-wrap fileset
                        (let [intermediate-results-directory (file eval-base-directory ".intermediate-results" "dependency")
                              dependency-eval-script (file eval-base-directory "dependency" "scripts" "run-dependency-eval.sh")]
                             ;; scrub the intermediate results directory if it exists
                             (if (.exists intermediate-results-directory) (.delete intermediate-results-directory))
                             (.mkdirs intermediate-results-directory)
                             (merge fileset
                                    {:eval-base-directory                             (file eval-base-directory)
                                     :input-directory                           (file input-directory)
                                     :gold-standard-directory                   (file gold-standard-directory)
                                     :scorer-directory                          (file scorer-directory)
                                     :dependency-intermediate-results-directory intermediate-results-directory
                                     :dependency-eval-script                    dependency-eval-script}))))


(deftask dependency-eval-setup-docker []
         "Sets constants used in the dependency parse evaluation when running the evaluation inside a Docker container."
         (comp (dependency-eval-setup :eval-base-directory "/home/craft/evaluation"
                                      :input-directory "/files-to-evaluate"
                                      :gold-standard-directory "/home/craft/CRAFT-3.1/structural-annotation/dependency/conllu"
                                      :scorer-directory "/home/craft/evaluation/dependency")))


(deftask run-dependency-eval []
         "evaluate a set of dependency parse files in the CoNLL-U file format against CRAFT"
         (with-pre-wrap fileset
                        (println (str "Running dependency parse evaluation."))

                        (dosh (.getAbsolutePath (:dependency-eval-script fileset))
                              "-i" (.getAbsolutePath (:input-directory fileset))
                              "-g" (.getAbsolutePath (:gold-standard-directory fileset))
                              "-o" (.getAbsolutePath (:dependency-intermediate-results-directory fileset))
                              "-s" (.getAbsolutePath (:scorer-directory fileset)))
                        fileset))


(deftask output-dependency-results []
         "Parse log files in the .intermediate-results directory and output to the final results
         file (which is written to the input directory containing the files being evaluated)."
         (with-pre-wrap fileset
                        (let [intermediate-results-directory (:dependency-intermediate-results-directory fileset)
                              output-file (file (:input-directory fileset) "dependency_results.tsv")]
                             (with-open [w (clojure.java.io/writer output-file :encoding "UTF-8")]
                                        (serialize-dependency-results intermediate-results-directory w))
                             (println (str "Evaluation complete. Please find the computed dependency parse evaluation results in "
                                           (.getAbsolutePath output-file))))
                        fileset))

(deftask eval-dependency
         "Evaluates a set of files in the CoNLL-U file format against CRAFT dependency parses.
          If no input arguments are specified then this tasks uses the
          Docker-specific setup by default."
         [b eval-base-directory VAL str "The path to the base directory that contains the dependency parse evaluation scripts directory. MUST BE ABSOLUTE PATH."
          i input-directory VAL str "The path to the directory of dependency parse files to test. MUST BE ABSOLUTE PATH."
          g gold-standard-directory VAL str "The path to the directory containing the gold standard dependency parse files. MUST BE ABSOLUTE PATH."
          s scorer-directory VAL str "The path to the conll18_ud_eval.py file. MUST BE ABSOLUTE PATH."]
         (comp (if (nil? eval-base-directory)
                 (dependency-eval-setup-docker)
                 (dependency-eval-setup :eval-base-directory eval-base-directory
                                        :input-directory input-directory
                                        :gold-standard-directory gold-standard-directory
                                        :scorer-directory scorer-directory))

               ;; run all dependency parse evaluations
               (run-dependency-eval)

               ;; compile and output the results to a file in the input-directory
               (output-dependency-results)))

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
                        (let [intermediate-results-directory (file eval-base-directory ".intermediate-results" "coref")
                              coref-eval-script (file eval-base-directory "coreference" "scripts" "run-coref-eval.sh")]
                             ;; scrub the intermediate results directory if it exists
                             (if (.exists intermediate-results-directory) (.delete intermediate-results-directory))
                             (.mkdirs intermediate-results-directory)
                             (merge fileset
                                    {:eval-base-directory                        (file eval-base-directory)
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
                        (println (str "Running coreference evaluation. Metric = " metric))

                        (dosh (.getAbsolutePath (:coref-eval-script fileset))
                              "-i" (.getAbsolutePath (:input-directory fileset))
                              "-g" (.getAbsolutePath (:gold-standard-directory fileset))
                              "-o" (.getAbsolutePath (:coref-intermediate-results-directory fileset))
                              "-s" (.getAbsolutePath (:scorer-directory fileset))
                              "-m" metric
                              "-p" (str allow-partial-matches))
                        fileset))


(deftask output-coref-results []
         "Parse log files in the .intermediate-results directory and output to the final results
         file (which is written to the input directory containing the files being evaluated)."
         (with-pre-wrap fileset
                        (let [intermediate-results-directory (:coref-intermediate-results-directory fileset)
                              output-file (file (:input-directory fileset) "coref_results.tsv")]
                             (with-open [w (clojure.java.io/writer output-file :encoding "UTF-8")]
                                        (serialize-coref-results intermediate-results-directory w))
                             (println (str "Evaluation complete. Please find the computed coreference evaluation results in "
                                           (.getAbsolutePath output-file))))
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
