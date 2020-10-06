#!/bin/bash

mkdir -p /home/craft/baseline-cr/output-postprocessed

cd /home/craft/baseline-cr-code &&  mvn exec:java -Dexec.mainClass="edu.ucdenver.ccp.craft.sharedtask.baseline.cr.BerkeleyOutputMapper" \
    -Dexec.args="/home/craft/baseline-cr/output /home/craft/craft-st-2019-2019_test_data/coreference-resolution/conllcoref /home/craft/baseline-cr/output-postprocessed"

