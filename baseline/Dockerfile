FROM clojure:openjdk-8-boot-2.8.2

# This Dockerfile accomplishes two tasks:
# 1) Prepare the CRAFT coreference gold standard test files for processing by the Berkeley Neural Coref system
# 2) Modify the output of the Berkeley Neural Coref system so that it can be processed by the CRAFT Shared Task evaluation code.


RUN apt-get update && apt-get install -y \
    curl \
    maven \
    git \
    unzip \
    wget \
    less \
    vim \
    python \
    gosu

# create the 'craft' user
RUN groupadd craft && \
    useradd --create-home --shell /bin/bash --no-log-init -g craft craft

USER craft

WORKDIR /home/craft

# install the gold standard data for the CRAFT shared task as this includes token files for the 
# coreference annotations -- the evaluation code requires the tokenization match exactly so token 
# files were provided to the task participants
RUN wget https://github.com/UCDenver-ccp/craft-st-2019/archive/2019_test_data.tar.gz && \
    tar -xzvf 2019_test_data.tar.gz

# install the CRAFT v4.0.1 distribution then run boot once to initialize it,
# then once more to download the dependencies for the CRAFT project
RUN wget https://github.com/UCDenver-ccp/CRAFT/archive/v4.0.1.tar.gz && \
    tar -xzf v4.0.1.tar.gz && \
    rm v4.0.1.tar.gz && \
    cd /home/craft/CRAFT-4.0.1 && \
    boot -h && \
    boot dependency -h


# build CoNLL-Coref 2011/12 formatted files for the coreference annotations
RUN cd /home/craft/CRAFT-4.0.1 && \
    boot part-of-speech coreference convert -i -o /home/craft/eval-data/coreference/conllcoref

RUN mkdir -p /home/craft/baseline-cr-code
COPY pom.xml /home/craft/baseline-cr-code/
COPY src/ /home/craft/baseline-cr-code/src/
WORKDIR /home/craft/baseline-cr-code
RUN mvn clean install

COPY --chown=craft:craft scripts/ /home/craft/

RUN chmod 755 /home/craft/*.sh 
