# CRAFT Shared Task 2019 -- CR Task Baseline

For the CRAFT Shared Task 2019, the [Berkeley coreference resolution system](http://nlp.cs.berkeley.edu/projects/coref.shtml) was used as a baseline system for the Coreference Resolution (CR) sub-task. This repository includes the code that was used to prepare input data for the Berkeley system and code that was used to process the output of the Berkeley system so that it could be evaluated by the CRAFT Shared Task 2019 evaluation platform.

## Dependencies

- [Docker](https://www.docker.com/) -- Code and scripts have been packaged in a [Docker container](https://hub.docker.com/r/ucdenverccp/craft-shared-task-2019-cr-baseline)
- [The Berkeley coreference system and accompanying model files](http://nlp.cs.berkeley.edu/projects/coref.shtml)

## Reproducing the CRAFT Shared Task 2019 CR Task Baseline

1. Select a directory on your machine where files can be written. This directory will be referred to as `[LOCAL_PATH]` in the steps below. **Note: Docker cannot write to an encrypted filesystem, so please make sure [LOCAL_PATH] references a directory that is not encrypted.**

2. Setup the directory structure </br>
   `docker run --rm -v [LOCAL_PATH]:/home/craft/baseline-cr ucdenverccp/craft-shared-task-2019-cr-baseline:0.1 /home/craft/setup.sh`

3. Download the [Berkeley coreference resolution system](http://nlp.cs.berkeley.edu/projects/coref.shtml) . Note, by downloading this system you are agreeing to their terms of use and [license](https://www.gnu.org/licenses/gpl-3.0.txt). </br> **[Note: the Berkeley download links appear to be broken -- 10/5/20]**

   - Download [berkeleycoref-1.1.tgz](http://nlp.cs.berkeley.edu/downloads/berkeleycoref-1.1.tgz)
   - Download [berkeleycoref-1.0-models.tgz](http://nlp.cs.berkeley.edu/downloads/berkeleycoref-1.0-models.tgz)
   - Download [number and gender data](http://www.cs.utexas.edu/~gdurrett/data/gender.data.tgz) produced by Shane Bergsma and Dekang Lin in "Bootstrapping Path-Based Pronoun Resolution"
   - Unpack the tarballs such that your directory structure looks like the following (Note: not all required files shown below, but there should be enough to validate your directory structure):

```
  [LOCAL_PATH]/berkeley-system/berkeleycoref
  [LOCAL_PATH]/berkeley-system/berkeleycoref/berkeleycoref-1.1.jar
  [LOCAL_PATH]/berkeley-system/berkeleycoref/models/coref-rawtext-final.ser
  [LOCAL_PATH]/berkeley-system/berkeleycoref/data/gender.data
```

4. Generate the input files for the baseline system. These files represent the 30 articles used as the evaluation set for the CRAFT Shared Task 2019. </br>
   `docker run --rm -v [LOCAL_PATH]:/home/craft/baseline-cr ucdenverccp/craft-shared-task-2019-cr-baseline:0.1 /home/craft/preprocess.sh`

5. Run the baseline system over the prepared input files. A script has been supplied, however please note that it may require adjustment to run in a Windows environment. </br>
   `docker run --rm -v [LOCAL_PATH]:/home/craft/baseline-cr ucdenverccp/craft-shared-task-2019-cr-baseline:0.1 /home/craft/run-baseline.sh`

6. Post-process the baseline system output and prepare it for evaluation. </br>
   `docker run --rm -v [LOCAL_PATH]:/home/craft/baseline-cr ucdenverccp/craft-shared-task-2019-cr-baseline:0.1 /home/craft/postprocess.sh`

7. Evaluate the baseline system output using the CRAFT Shared Task 2019 evaluation platform. Evaluation results will be placed in `[LOCAL_PATH]/output-postprocessed/coref_results.tsv` </br>
   `docker run --rm -v [LOCAL_PATH]/output-postprocessed:/files-to-evaluate ucdenverccp/craft-eval:4.0.1_0.1.2 sh -c 'cd /home/craft/evaluation && boot eval-coreference'`
