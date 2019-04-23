#!/bin/bash

function print_usage {
    echo "Usage:"
    echo "$(basename $0) [OPTIONS]"
    echo "  [-i <input directory>]: MUST BE ABSOLUTE PATH. The path to the directory of coreference files to test"
    echo "  [-g <gold standard directory>]: MUST BE ABSOLUTE PATH. The path to the directory containing the gold standard coreference files."
    echo "  [-o <output (results) directory>]: MUST BE ABSOLUTE PATH. The path to a directory where the output (coreference performance measures) will be written."
    echo "  [-m <metric>]: Indicator of the coreference metric to use. Must be one of: muc|bcub|ceafm|ceafe|blanc|lea"
    echo "  [-p <partial mention match flag]: Defaults to 'false'. If set to 'true' then partial mention matches are permitted, otherwise mentions must have identical spans to match."
    echo "  [-s <scorer directory>]: MUST BE ABSOLUTE PATH. The path to the scorer.pl file"
}

ALLOW_PARTIAL='false'

while getopts "i:g:o:p:m:s:h" OPTION; do
    case $OPTION in
        # the path to the coreference files to test
        i) INPUT_DIRECTORY=$OPTARG
           ;;
        # the path to the gold standard coreference files
        g) GOLD_DIRECTORY=$OPTARG
           ;;
        # the path where output logs are stored
        o) OUTPUT_DIRECTORY=$OPTARG
           ;;
        # partial mention match flag
        p) ALLOW_PARTIAL=$OPTARG
           ;;
        # The metric to use
        m) METRIC=$OPTARG
           ;;
         # The metric to use
        s) SCORER_DIRECTORY=$OPTARG
           ;;
        # HELP!
        h) print_usage; exit 0
           ;;
    esac
done

if [[ -z ${INPUT_DIRECTORY} || -z ${GOLD_DIRECTORY} || -z ${METRIC} || -z ${SCORER_DIRECTORY} || -z ${OUTPUT_DIRECTORY} ]]; then
    echo "input directory: ${INPUT_DIRECTORY}"
	echo "gold directory: ${GOLD_DIRECTORY}"
	echo "output directory: ${OUTPUT_DIRECTORY}"
	echo "metric: ${METRIC}"
	echo "scorer directory: ${SCORER_DIRECTORY}"
    print_usage
    exit 1
fi


LOG_FILE=${OUTPUT_DIRECTORY}/${METRIC}.log
if [ ${ALLOW_PARTIAL} = "true" ]; then
    LOG_FILE=${OUTPUT_DIRECTORY}/${METRIC}.allow_partial.log
fi

for filename in ${GOLD_DIRECTORY}/*.conll; do
    echo "Evaluating ${filename}"
    perl ${SCORER_DIRECTORY}/scorer.pl ${METRIC} ${filename} ${INPUT_DIRECTORY}/$(basename $filename) ${ALLOW_PARTIAL}
    echo "------------------ end ${filename}"
done > ${LOG_FILE}
