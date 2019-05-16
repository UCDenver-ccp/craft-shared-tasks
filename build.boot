(def project 'edu.ucdenver.ccp/craft-eval)
(def version "0.1.0")

(set-env! :resource-paths #{"resources"}
          :source-paths #{"test/clojure" "src/clojure" "src/java"}
          :dependencies '[[org.clojure/clojure "RELEASE"]
                          [adzerk/boot-test "RELEASE" :scope "test"]
                          [test-with-files "0.1.1" :scope "test"]
                          [edu.ucdenver.ccp/file-conversion-onejar "0.2.1"]
                          [edu.ucdenver.ccp/ccp-nlp-evaluation-onejar "3.5.2"]]
          :repositories #(conj % ["bionlp" {:url "https://svn.code.sf.net/p/bionlp/code/repo/"}]))

(require '[adzerk.boot-test :refer [test]]
         '[clojure.java.io :refer [file]]
         '[craft-eval.coref :refer [serialize-coref-results]]
         '[craft-eval.dependency :refer [serialize-dependency-results]]
         ;;; uncomment the dependency below when universal dependency info is available in CRAFT
         ;'[craft-eval.univ-dependency :refer [serialize-dependency-results]]
         )

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

(def craft-version "3.1.1")

(deftask build
         "Build the project locally as a JAR."
         [d dir PATH #{str} "the set of directories to write to (target)."]
         (let [dir (if (seq dir) dir #{"target"})]
              (comp (aot) (pom) (uber :exclude #{#"(?i)^META-INF/INDEX.LIST$"
                                                 #"(?i)^META-INF/[^/]*\.(MF|SF|RSA|DSA)$"
                                                 #"META-INF/maven/"}) (jar) (target :dir dir))))

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
                                    {:eval-base-directory                  (file eval-base-directory)
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

;;; =========================================================================== ;;;
;;; =================== Concept Annotation Evaluation Tasks ===================
;;; =========================================================================== ;;;

(deftask concept-eval-setup
         "Sets constants used in the concept evaluation."
         [i input-directory VAL str "The path to the base directory of BioNLP-formatted concept annotation files to test.
                                     The input-directory is assumed to contain directories named according
                                     to the appropriate ontology to use for evaluation, .e.g  cl/ will contain
                                     BioNLP-formatted files with annotations from the Cell Ontology, and cl-ext/
                                     will contain BioNLP-formatted files with annotations from the Cell Ontology
                                     plus extension annotations. Directory names are treated case-insensitively.
                                     MUST BE ABSOLUTE PATH."
          g gold-standard-directory VAL str "The path to the directory containing the BioNLP-formatted gold standard concept
                                             annotation files. The input-directory is assumed to contain directories
                                             named according to the appropriate ontology to use for evaluation,
                                             .e.g  cl/ will contain BioNLP-formatted files with annotations from the
                                             Cell Ontology, and cl-ext/ will contain BioNLP-formatted files with
                                             annotations from the Cell Ontology plus extension annotations.
                                             MUST BE ABSOLUTE PATH."
          c craft-distribution-directory VAL str "The path to the directory containing the CRAFT distribution. This
                                                  directory is required as it contains the ontology files that will be
                                                  used for the concept valuation. MUST BE ABSOLUTE PATH."
          ]
         (with-pre-wrap fileset
                        (merge fileset
                               {:input-directory              (file input-directory)
                                :gold-standard-directory      (file gold-standard-directory)
                                :craft-distribution-directory (file craft-distribution-directory)})))


(deftask concept-eval-setup-docker []
         "Sets constants used in the concept annotation evaluation when running the evaluation inside a Docker container."
         (comp (concept-eval-setup :input-directory "/files-to-evaluate"
                                   :gold-standard-directory "/home/craft/eval-data/concept/bionlp"
                                   :craft-distribution-directory (str "/home/craft/CRAFT-" craft-version))))

(deftask check-env []
         "make sure the max heap size is at least 5g"
         (let [jvm-args (System/getenv "BOOT_JVM_OPTIONS")]
              (if (nil? jvm-args) (do (print "Please set JVM heap maximum to at least 5g using: export BOOT_JVM_OPTIONS='-Xmx5g -client'\n")
                                      (System/exit -1))
                                  (do (print (str "JVM args: " jvm-args "\n"))
                                      (let [heap-max-params (re-find #"-Xmx(\d+)(\w)" jvm-args)
                                            multiplier (if (not (nil? heap-max-params))
                                                         (case (last heap-max-params)
                                                               "g" (* 1024 1024 1024)
                                                               "m" (* 1024 1024)
                                                               "k" 1024
                                                               1)
                                                         0)
                                            gigs (if (not (nil? heap-max-params))
                                                   (* multiplier (Integer/parseInt (second heap-max-params)))
                                                   0)]
                                           (if (< gigs (* 5 1024 1024 1024))
                                             (do (print (str "Please set the max JVM heap size to at least 5g, e.g. export BOOT_JVM_OPTIONS='-Xmx5g -client'. The current JVM params are the following: '" jvm-args "'\n"))
                                                 (System/exit -1))))))))

(deftask run-concept-eval []
         (with-pre-wrap fileset
                        (let [input-directory (:input-directory fileset)
                              gold-directory (:gold-standard-directory fileset)
                              craft-distribution-directory (:craft-distribution-directory fileset)]
                             ;; make sure enough memory has been allocated
                             (check-env)
                             (require 'craft-eval.concept :reload)
                             ((resolve 'craft-eval.concept/run-concept-eval)
                               craft-distribution-directory
                               gold-directory
                               input-directory))
                        fileset))



(deftask eval-concept-annotations
         "Evaluates a set of files in the BioNLP file format against CRAFT concept annotations.
          If no input arguments are specified then this task uses the
          Docker-specific setup by default. The input-directory is assumed to contain directories
          named according to the appropriate ontology to use for evaluation, .e.g  cl/ will contain
          BioNLP-formatted files with annotations from the Cell Ontology, and cl-ext/ will contain
          BioNLP-formatted files with annotations from the Cell Ontology plus extension annotations."
         [i input-directory VAL str "The path to the base directory of BioNLP-formatted concept annotation files to test.
                                     The input-directory is assumed to contain directories named according
                                     to the appropriate ontology to use for evaluation, .e.g  cl/ will contain
                                     BioNLP-formatted files with annotations from the Cell Ontology, and cl-ext/
                                     will contain BioNLP-formatted files with annotations from the Cell Ontology
                                     plus extension annotations. Directory names are treated case-insensitively.
                                     MUST BE ABSOLUTE PATH."
          g gold-standard-directory VAL str "The path to the directory containing the BioNLP-formatted gold standard concept
                                             annotation files. The input-directory is assumed to contain directories
                                             named according to the appropriate ontology to use for evaluation,
                                             .e.g  cl/ will contain BioNLP-formatted files with annotations from the
                                             Cell Ontology, and cl-ext/ will contain BioNLP-formatted files with
                                             annotations from the Cell Ontology plus extension annotations.
                                             MUST BE ABSOLUTE PATH."
          c craft-distribution-directory VAL str "The path to the directory containing the CRAFT distribution. This
                                                  directory is required as it contains the ontology files that will be
                                                  used for the concept valuation. MUST BE ABSOLUTE PATH."]
         (comp (if (nil? input-directory)
                 (concept-eval-setup-docker)
                 (concept-eval-setup :input-directory input-directory
                                     :gold-standard-directory gold-standard-directory
                                     :craft-distribution-directory craft-distribution-directory))

               ;; run all concept annotation evaluations
               (run-concept-eval)))



;;; ==================================================================== ;;;
;;; ================= Dependency Parse Evaluation Tasks ================
;;; ==================================================================== ;;;

(deftask dependency-eval-setup
         "Sets constants used in the dependency parse evaluation."
         [b eval-base-directory VAL str "The path to the base directory that contains the dependency parse evaluation scripts directory. MUST BE ABSOLUTE PATH."
          i input-directory VAL str "The path to the directory of dependency parse files to test. MUST BE ABSOLUTE PATH."
          g gold-standard-directory VAL str "The path to the directory containing the gold standard dependency parse files. MUST BE ABSOLUTE PATH."
          s scorer-directory VAL str "The path to the MaltEval distribution directory. MUST BE ABSOLUTE PATH."]
         (with-pre-wrap fileset
                        (let [intermediate-results-directory (file eval-base-directory ".intermediate-results" "dependency")
                              dependency-eval-script (file eval-base-directory "dependency" "scripts" "run-dependency-eval.sh")]
                             ;; scrub the intermediate results directory if it exists
                             (if (.exists intermediate-results-directory) (.delete intermediate-results-directory))
                             (.mkdirs intermediate-results-directory)
                             (println (str "JUST MADE INTER DIR: " (.getAbsolutePath intermediate-results-directory)))
                             (merge fileset
                                    {:eval-base-directory                  (file eval-base-directory)
                                     :input-directory                      (file input-directory)
                                     :gold-standard-directory              (file gold-standard-directory)
                                     :scorer-directory                     (file scorer-directory)
                                     :dependency-intermediate-results-file (file intermediate-results-directory "dependency-results.out")
                                     :dependency-eval-script               dependency-eval-script}))))


(deftask dependency-eval-setup-docker []
         "Sets constants used in the dependency parse evaluation when running the evaluation inside a Docker container."
         (comp (dependency-eval-setup :eval-base-directory "/home/craft/evaluation"
                                      :input-directory "/files-to-evaluate"
                                      :gold-standard-directory (str "/home/craft/CRAFT-" craft-version "/structural-annotation/dependency/conllx")
                                      :scorer-directory "/home/craft/evaluation/dependency/malteval/dist-20141005")))


(deftask run-dependency-eval []
         "evaluate a set of dependency parse files in the CoNLL-U file format against CRAFT"
         (with-pre-wrap fileset
                        (println (str "Running dependency parse evaluation."))

                        (dosh (.getAbsolutePath (:dependency-eval-script fileset))
                              "-i" (.getAbsolutePath (:input-directory fileset))
                              "-g" (.getAbsolutePath (:gold-standard-directory fileset))
                              "-o" (.getAbsolutePath (:dependency-intermediate-results-file fileset))
                              "-s" (.getAbsolutePath (:scorer-directory fileset)))
                        fileset))


(deftask output-dependency-results []
         "Parse log files in the .intermediate-results directory and output to the final results
         file (which is written to the input directory containing the files being evaluated)."
         (with-pre-wrap fileset
                        (let [results-file (:dependency-intermediate-results-file fileset)
                              output-file (file (:input-directory fileset) "dependency_results.tsv")]
                             (with-open [w (clojure.java.io/writer output-file :encoding "UTF-8")]
                                        (serialize-dependency-results results-file w))
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
          s scorer-directory VAL str "The path to the MaltEval distribution directory. MUST BE ABSOLUTE PATH."]
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

;;; ============================================================================== ;;;
;;; ================= Universal Dependency Parse Evaluation Tasks ================
;;; ============================================================================== ;;;
;
;;; +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ;;;
;;; ++++++ Commented out as CRAFT does not currently have Universal Dependency information ++++++ ;;;
;;; +++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++ ;;;
;
;(deftask univ-dependency-eval-setup
;         "Sets constants used in the dependency parse evaluation."
;         [b eval-base-directory VAL str "The path to the base directory that contains the dependency parse evaluation scripts directory. MUST BE ABSOLUTE PATH."
;          i input-directory VAL str "The path to the directory of dependency parse files to test. MUST BE ABSOLUTE PATH."
;          g gold-standard-directory VAL str "The path to the directory containing the gold standard dependency parse files. MUST BE ABSOLUTE PATH."
;          s scorer-directory VAL str "The path to the conll18_ud_eval.py file. MUST BE ABSOLUTE PATH."]
;         (with-pre-wrap fileset
;                        (let [intermediate-results-directory (file eval-base-directory ".intermediate-results" "dependency")
;                              dependency-eval-script (file eval-base-directory "dependency" "scripts" "run-universal-dependency-eval.sh")]
;                             ;; scrub the intermediate results directory if it exists
;                             (if (.exists intermediate-results-directory) (.delete intermediate-results-directory))
;                             (.mkdirs intermediate-results-directory)
;                             (merge fileset
;                                    {:eval-base-directory                       (file eval-base-directory)
;                                     :input-directory                           (file input-directory)
;                                     :gold-standard-directory                   (file gold-standard-directory)
;                                     :scorer-directory                          (file scorer-directory)
;                                     :dependency-intermediate-results-directory intermediate-results-directory
;                                     :dependency-eval-script                    dependency-eval-script}))))
;
;
;(deftask univ-dependency-eval-setup-docker []
;         "Sets constants used in the dependency parse evaluation when running the evaluation inside a Docker container."
;         (comp (univ-dependency-eval-setup :eval-base-directory "/home/craft/evaluation"
;                                           :input-directory "/files-to-evaluate"
;                                           :gold-standard-directory (str "/home/craft/CRAFT-" craft-version "/structural-annotation/dependency/conllu")
;                                           :scorer-directory "/home/craft/evaluation/dependency")))
;
;
;(deftask run-univ-dependency-eval []
;         "evaluate a set of dependency parse files in the CoNLL-U file format against CRAFT"
;         (with-pre-wrap fileset
;                        (println (str "Running dependency parse evaluation."))
;
;                        (dosh (.getAbsolutePath (:dependency-eval-script fileset))
;                              "-i" (.getAbsolutePath (:input-directory fileset))
;                              "-g" (.getAbsolutePath (:gold-standard-directory fileset))
;                              "-o" (.getAbsolutePath (:dependency-intermediate-results-directory fileset))
;                              "-s" (.getAbsolutePath (:scorer-directory fileset)))
;                        fileset))
;
;
;(deftask output-univ-dependency-results []
;         "Parse log files in the .intermediate-results directory and output to the final results
;         file (which is written to the input directory containing the files being evaluated)."
;         (with-pre-wrap fileset
;                        (let [intermediate-results-directory (:dependency-intermediate-results-directory fileset)
;                              output-file (file (:input-directory fileset) "dependency_results.tsv")]
;                             (with-open [w (clojure.java.io/writer output-file :encoding "UTF-8")]
;                                        (serialize-dependency-results intermediate-results-directory w))
;                             (println (str "Evaluation complete. Please find the computed dependency parse evaluation results in "
;                                           (.getAbsolutePath output-file))))
;                        fileset))
;
;(deftask eval-univ-dependency
;         "Evaluates a set of files in the CoNLL-U file format against CRAFT dependency parses.
;          If no input arguments are specified then this tasks uses the
;          Docker-specific setup by default."
;         [b eval-base-directory VAL str "The path to the base directory that contains the dependency parse evaluation scripts directory. MUST BE ABSOLUTE PATH."
;          i input-directory VAL str "The path to the directory of dependency parse files to test. MUST BE ABSOLUTE PATH."
;          g gold-standard-directory VAL str "The path to the directory containing the gold standard dependency parse files. MUST BE ABSOLUTE PATH."
;          s scorer-directory VAL str "The path to the conll18_ud_eval.py file. MUST BE ABSOLUTE PATH."]
;         (comp (if (nil? eval-base-directory)
;                 (dependency-eval-setup-docker)
;                 (dependency-eval-setup :eval-base-directory eval-base-directory
;                                        :input-directory input-directory
;                                        :gold-standard-directory gold-standard-directory
;                                        :scorer-directory scorer-directory))
;
;               ;; run all dependency parse evaluations
;               (run-univ-dependency-eval)
;
;               ;; compile and output the results to a file in the input-directory
;               (output-dependency-results)))
