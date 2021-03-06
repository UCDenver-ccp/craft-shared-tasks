#!/bin/bash

function print_usage {
    echo "Usage:"
    echo "$(basename $0) [OPTIONS]"
    echo "  [-i <input directory>]: MUST BE ABSOLUTE PATH. The path to the directory of dependency files to test"
    echo "  [-g <gold standard directory>]: MUST BE ABSOLUTE PATH. The path to the directory containing the gold standard dependency files."
    echo "  [-o <output (results) file>]: MUST BE ABSOLUTE PATH. The path to a directory where the output (dependency performance measures) will be written."
    echo "  [-s <scorer directory>]: MUST BE ABSOLUTE PATH. The path to the MaltEval distribution (should contain lib/MaltEval.jar)"
}

ALLOW_PARTIAL='false'

while getopts "i:g:o:s:h" OPTION; do
    case $OPTION in
        # the path to the coreference files to test
        i) INPUT_DIRECTORY=$OPTARG
           ;;
        # the path to the gold standard coreference files
        g) GOLD_DIRECTORY=$OPTARG
           ;;
        # the path where output logs are stored
        o) OUTPUT_FILE=$OPTARG
           ;;
         # The metric to use
        s) SCORER_DIRECTORY=$OPTARG
           ;;
        # HELP!
        h) print_usage; exit 0
           ;;
    esac
done

MALTEVAL_CONFIG_FILE=${SCORER_DIRECTORY}/malt-eval-config.xml

if [ ! -f ${MALTEVAL_CONFIG_FILE} ]; then
    echo "Could not find the expected MaltEval configuration file: ${MALTEVAL_CONFIG_FILE}"
fi

if [[ -z ${INPUT_DIRECTORY} || -z ${GOLD_DIRECTORY} || -z ${SCORER_DIRECTORY} || -z ${OUTPUT_FILE} ]]; then
    echo "input directory: ${INPUT_DIRECTORY}"
	echo "gold directory: ${GOLD_DIRECTORY}"
	echo "output file: ${OUTPUT_FILE}"
	echo "scorer directory: ${SCORER_DIRECTORY}"
    print_usage
    exit 1
fi

java -jar ${SCORER_DIRECTORY}/lib/MaltEval.jar -e ${MALTEVAL_CONFIG_FILE} \
                                               -s "${INPUT_DIRECTORY}/" \
                                               -g "${GOLD_DIRECTORY}/" \
                                               --output ${OUTPUT_FILE}
