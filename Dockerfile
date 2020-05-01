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

# install the CRAFT v4.0.1 distribution then run boot once to initialize it,
# then once more to download the dependencies for the CRAFT project
RUN cd /home/craft && \
    wget https://github.com/UCDenver-ccp/CRAFT/archive/v4.0.1.tar.gz && \
    tar -xzf v4.0.1.tar.gz && \
    rm v4.0.1.tar.gz && \
    cd /home/craft/CRAFT-4.0.1 && \
    boot -h && \
    boot dependency -h

# build annotation files required for evaluations
# 1. build CoNLL-Coref 2011/12 formatted files for the coreference annotations
# 2. build bionlp formatted files for concept annotations
RUN cd /home/craft/CRAFT-4.0.1 && \
    boot part-of-speech coreference convert -i -o /home/craft/eval-data/coreference/conllcoref && \
    boot concept -t CHEBI convert -b -o /home/craft/eval-data/concept/bionlp/chebi && \
    boot concept -t CHEBI -x convert -b -o /home/craft/eval-data/concept/bionlp/chebi_ext && \
    boot concept -t CL convert -b -o /home/craft/eval-data/concept/bionlp/cl && \
    boot concept -t CL -x convert -b -o /home/craft/eval-data/concept/bionlp/cl_ext && \
    boot concept -t GO_BP convert -b -o /home/craft/eval-data/concept/bionlp/go_bp && \
    boot concept -t GO_BP -x convert -b -o /home/craft/eval-data/concept/bionlp/go_bp_ext && \
    boot concept -t GO_CC convert -b -o /home/craft/eval-data/concept/bionlp/go_cc && \
    boot concept -t GO_CC -x convert -b -o /home/craft/eval-data/concept/bionlp/go_cc_ext && \
    boot concept -t GO_MF convert -b -o /home/craft/eval-data/concept/bionlp/go_mf && \
    boot concept -t GO_MF -x convert -b -o /home/craft/eval-data/concept/bionlp/go_mf_ext && \
    boot concept -t MOP convert -b -o /home/craft/eval-data/concept/bionlp/mop && \
    boot concept -t MOP -x convert -b -o /home/craft/eval-data/concept/bionlp/mop_ext && \
    boot concept -t NCBITaxon convert -b -o /home/craft/eval-data/concept/bionlp/ncbitaxon && \
    boot concept -t NCBITaxon -x convert -b -o /home/craft/eval-data/concept/bionlp/ncbitaxon_ext && \
    boot concept -t PR convert -b -o /home/craft/eval-data/concept/bionlp/pr && \
    boot concept -t PR -x convert -b -o /home/craft/eval-data/concept/bionlp/pr_ext && \
    boot concept -t SO convert -b -o /home/craft/eval-data/concept/bionlp/so && \
    boot concept -t SO -x convert -b -o /home/craft/eval-data/concept/bionlp/so_ext && \
    boot concept -t UBERON convert -b -o /home/craft/eval-data/concept/bionlp/uberon && \
    boot concept -t UBERON -x convert -b -o /home/craft/eval-data/concept/bionlp/uberon_ext

# install coreference evaluation software and run unit tests to make sure it works
RUN cd /home/craft && \
    mkdir -p evaluation/coreference && \
    cd /home/craft/evaluation/coreference && \
    git clone --single-branch --branch lea-integration https://github.com/bill-baumgartner/reference-coreference-scorers.git ./reference-coreference-scorers.git && \
    cd /home/craft/evaluation/coreference/reference-coreference-scorers.git && \
    perl test/test.pl


# install CoNLL 2018 universal dependency parse evaluation software
RUN cd /home/craft/evaluation && \
    mkdir universal-dependency && \
    cd universal-dependency && \
    wget http://universaldependencies.org/conll18/conll18_ud_eval.py && \
    python conll18_ud_eval.py -h

# install the MaltEval tool for dependency parse evaluation
RUN cd /home/craft/evaluation && \
    mkdir -p dependency/malteval && \
    cd dependency/malteval && \
    wget --no-check-certificate 'https://docs.google.com/uc?export=download&id=0B1KaZVnBJE8_QnhqNE52T2FZWVE' -O MaltEval-dist.zip && \
    unzip MaltEval-dist.zip

# copy coreference scripts to the container
USER root
COPY evaluation/coreference/scripts/run-coref-eval.sh /home/craft/evaluation/coreference/scripts/


COPY evaluation/dependency/scripts/run-universal-dependency-eval.sh /home/craft/evaluation/dependency/scripts/
COPY evaluation/dependency/scripts/run-dependency-eval.sh /home/craft/evaluation/dependency/scripts/
COPY evaluation/dependency/scripts/malt-eval-config.xml /home/craft/evaluation/dependency/malteval/dist-20141005
COPY build.boot /home/craft/evaluation/
COPY src/ /home/craft/evaluation/src/
COPY test/ /home/craft/evaluation/test/
COPY entrypoint.sh /
RUN chmod 755 /entrypoint.sh && \
    chown -R craft:craft /home/craft/evaluation


USER craft
RUN mkdir /home/craft/evaluation/resources/ && \
    chmod 755 /home/craft/evaluation/coreference/scripts/*.sh && \
    chmod 755 /home/craft/evaluation/dependency/scripts/*.sh && \
    cd /home/craft/evaluation && \
    boot build install

ENV BOOT_JVM_OPTIONS='-Xmx5g -client'

USER root
ENTRYPOINT ["/entrypoint.sh"]



