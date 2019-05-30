# Change Log

## 0.1.1 - 2015-05-30
* Re-activated universal dependency evaluation scripts
* Revised concept evaluation to properly handle the extension class sets
* Now uses CRAFT v3.1.2

## 0.1.0 - 2015-05-15
### Initial commit
* Code and scripts to build a Docker container that serves as the evaluation platform for comparing against the [CRAFT Corpus](https://github.com/UCDenver-ccp/CRAFT).
* Evaluation metrics employed:
  * Dependency parse evaluation: The [MaltEval tool]()
  * Coreference resolution evaluation: [The CoNLL 2011/12 reference coreference scorers adapted to handle discontinuous annotations](https://github.com/bill-baumgartner/reference-coreference-scorers).
  * Concept annotation evaluation: An [implementation](https://github.com/UCDenver-ccp/ccp-nlp/tree/master/ccp-nlp-evaluation/src/main/java/edu/ucdenver/ccp/nlp/evaluation/bossy2013) of the methodology described in [Bossy at et. al, 2013](https://aclweb.org/anthology/papers/W/W13/W13-2024/).


