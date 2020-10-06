#!/bin/bash

mkdir -p /home/craft/baseline-cr/input-tmp

cd /home/craft/baseline-cr/berkeley-system/berkeleycoref && \
java -cp berkeleycoref-1.1.jar -Xmx10g edu.berkeley.nlp.coref.preprocess.PreprocessingDriver ++base.conf \
  -execDir /home/craft/baseline-cr/berkeley-system/berkeleycoref \
  -inputDir /home/craft/baseline-cr/txt \
  -outputDir /home/craft/baseline-cr/input-tmp

# populate the text file directory
cd /home/craft/baseline-cr-code && mvn exec:java -Dexec.mainClass="edu.ucdenver.ccp.craft.sharedtask.baseline.cr.PopulateInputDirectory" \
     -Dexec.args="/home/craft/baseline-cr/input-tmp /home/craft/baseline-cr/input"

rm -rf /home/craft/baseline-cr/input-tmp
