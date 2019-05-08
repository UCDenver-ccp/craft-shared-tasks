FROM clojure:openjdk-8-boot-2.8.2

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

# install the CRAFT v3.1 distribution then run boot once to initialize it,
# then once more to download the dependencies for the CRAFT project
RUN cd /home/craft && \
    wget https://github.com/UCDenver-ccp/CRAFT/archive/v3.1.tar.gz && \
    tar -xzf v3.1.tar.gz && \
    rm v3.1.tar.gz && \
    cd /home/craft/CRAFT-3.1 && \
    boot -h && \
    boot dependency -h

# build annotation files required for evaluations
# 1. build CoNLL-Coref 2011/12 formatted files for the coreference annotations
# 2. build bionlp formatted files for concept annotations
RUN cd /home/craft/CRAFT-3.1 && \
    boot part-of-speech coreference convert -i -o /home/craft/eval-data/coreference/conllcoref

# install coreference evaluation software and run unit tests to make sure it works
RUN cd /home/craft && \
    mkdir -p evaluation/coreference && \
    cd /home/craft/evaluation/coreference && \
    git clone --single-branch --branch lea-integration https://github.com/bill-baumgartner/reference-coreference-scorers.git ./reference-coreference-scorers.git && \
    cd /home/craft/evaluation/coreference/reference-coreference-scorers.git && \
    perl test/test.pl

#
# uncomment this block when universal dependency info is available in CRAFT
#
# install CoNLL 2018 universal dependency parse evaluation software
#RUN cd /home/craft/evaluation && \
#    mkdir universal-dependency && \
#    cd universal-dependency && \
#    wget http://universaldependencies.org/conll18/conll18_ud_eval.py && \
#    python conll18_ud_eval.py -h

# install the MaltEval tool for dependency parse evaluation
RUN cd /home/craft/evaluation && \
    mkdir -p dependency/malteval && \
    cd dependency/malteval && \
    wget https://drive.google.com/uc?export=download&id=0B1KaZVnBJE8_QnhqNE52T2FZWVE && \
    unzip MaltEval-dist.zip

# copy coreference scripts to the container
USER root
COPY evaluation/coreference/scripts/run-coref-eval.sh /home/craft/evaluation/coreference/scripts/

# uncomment this block when universal dependency info is available in CRAFT
#COPY evaluation/dependency/scripts/run-universal-dependency-eval.sh /home/craft/evaluation/dependency/scripts/

COPY evaluation/dependency/scripts/run-dependency-eval.sh /home/craft/evaluation/dependency/scripts/
COPY evaluation/dependency/scripts/malteval-config.xml /home/craft/evaluation/dependency/malteval/dist-20141005
COPY build.boot /home/craft/evaluation/
COPY src/ /home/craft/evaluation/src/
COPY test/ /home/craft/evaluation/test/
COPY resources/ /home/craft/evaluation/resources/
COPY entrypoint.sh /
RUN chmod 755 /entrypoint.sh && \
    chown -R craft:craft /home/craft/evaluation


USER craft
RUN chmod 755 /home/craft/evaluation/coreference/scripts/*.sh && \
    chmod 755 /home/craft/evaluation/dependency/scripts/*.sh && \
    cd /home/craft/evaluation && \
    boot build install


USER root
ENTRYPOINT ["/entrypoint.sh"]

# install concept annotation evaluation software


