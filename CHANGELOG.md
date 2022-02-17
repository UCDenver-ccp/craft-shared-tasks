# Change Log

## 0.2.0 - 2022-02-16
* Makes use of CRAFT v5.0.0 which now includes annotations to the Mondo Disease Ontology

## 0.1.2 - 2019-07-03
* Incorporated validation of coreference input files into the evaluation process. Validation is primarily focused on properly formatted discontinuous spans but also checks for annotations that are members of mulitple identity chains and redundant annotations within an identity chain (both are prohibited). The adoption of v0.2.2 of the file-conversion project (see https://github.com/UCDenver-ccp/file-conversion/blob/master/CHANGES.md) addresses the concerns raised in https://github.com/UCDenver-ccp/craft-shared-tasks/issues/1
* Fixed minor bug in a unit test so that the correct function is now called
* Incremented CRAFT version to v3.1.3

## 0.1.1 - 2019-05-30
* Re-activated universal dependency evaluation scripts
* Revised concept evaluation to properly handle the extension class sets
* Now uses CRAFT v3.1.2

## 0.1.0 - 2019-05-15
### Initial commit
* Code and scripts to build a Docker container that serves as the evaluation platform for comparing against the [CRAFT Corpus](https://github.com/UCDenver-ccp/CRAFT).
* Evaluation metrics employed:
  * Dependency parse evaluation: The [MaltEval tool]()
  * Coreference resolution evaluation: [The CoNLL 2011/12 reference coreference scorers adapted to handle discontinuous annotations](https://github.com/bill-baumgartner/reference-coreference-scorers).
  * Concept annotation evaluation: An [implementation](https://github.com/UCDenver-ccp/ccp-nlp/tree/master/ccp-nlp-evaluation/src/main/java/edu/ucdenver/ccp/nlp/evaluation/bossy2013) of the methodology described in [Bossy at et. al, 2013](https://aclweb.org/anthology/papers/W/W13/W13-2024/).


