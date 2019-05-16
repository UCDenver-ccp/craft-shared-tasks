(ns craft-eval.concept
    (:import craft.eval.concept.CraftConceptEvaluationUtil
      edu.ucdenver.ccp.nlp.evaluation.bossy2013.BoundaryMatchStrategy))

(defn run-concept-eval [craft-distribution-directory gold-directory test-directory]
      (CraftConceptEvaluationUtil/evaluate craft-distribution-directory
                                           gold-directory
                                           test-directory
                                           BoundaryMatchStrategy/JACCARD))




