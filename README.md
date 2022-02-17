# Docker image versions

Please see the [README](https://github.com/UCDenver-ccp/craft-shared-tasks/blob/master/README.md) for information on which Docker image `[VERSION]` to use.

Sample `docker` commands in the wiki use an `[VERSION]` variable. When running these commands, the variable should be replaced by one of the images versions described below.

## VERSION = 5.0.0_0.2.0
The `ucdenverccp/craft-eval:5.0.0_0.2.0` image incorporates CRAFT v5.0.0 which includes concept annotations to the Mondo Disease Ontology (MONDO) for all 97 articles.

## VERSION = 4.0.1_0.1.2
The `ucdenverccp/craft-eval:4.0.1_0.1.2` image was created after completion of the 2019 CRAFT Shared Task. It contains version 4.0.1 of the CRAFT corpus, which is comprised of all 97 articles; the 67 articles in the initial public release + the 30 reserved articles used to evaluate systems in the 2019 CRAFT Shared Task.

## VERSION = 3.1.3_0.1.2
The `ucdenverccp/craft-eval:3.1.3_0.1.2` image was provided to participants during the 2019 CRAFT Shared Task to allow them to evaluate their systems during the development phase. This image contains version 3.1.3 of the CRAFT corpus, which is comprised of the 67 articles that were present in the initial public release of CRAFT.



# CRAFT shared task evaluation
This repository hosts code and scripts used for evaluation of the [CRAFT Shared Tasks 2019](https://sites.google.com/view/craft-shared-task-2019/home).

There are three ways one might use the code/scripts in this repository: 

1. By far, the easiest way is to use the pre-built Docker container. The Docker container contains all dependencies for the evaluation scripts, the evaluation scripts themselves, and the CRAFT corpus. It is available on [DockerHub](https://hub.docker.com/r/ucdenverccp/craft-eval), and instructions for how to use it are available on the [Evaluation via Docker](https://github.com/UCDenver-ccp/craft-shared-tasks/wiki/Evaluation-via-Docker-(Recommended-Method)) wiki page.
2. The next easiest way to evaluate your files against CRAFT is to build the Docker container yourself. Instructions for building the Docker container can be found on the [Building the CRAFT ST evaluation Docker container](https://github.com/UCDenver-ccp/craft-shared-tasks/wiki/Building-the-CRAFT-evaluation-Docker-container) wiki page.
3. Finally, the third option is to download [the latest stable release of this repository](https://github.com/UCDenver-ccp/craft-shared-tasks/releases), install any missing dependencies, and to run the evaluations in your local environment. **NOTE, this is not the recommended approach,** but instructions for a local installation are available on the [Local installation](https://github.com/UCDenver-ccp/craft-shared-tasks/wiki/Evaluation-via-Local-Installation) wiki page.
