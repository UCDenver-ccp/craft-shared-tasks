#!/bin/bash

function print_usage {
    echo "Usage:"
    echo "$(basename $0) [OPTIONS]"
    echo "  [-i <input directory>]: MUST BE ABSOLUTE PATH. The path to the directory of dependency files to test"
    echo "  [-g <gold standard directory>]: MUST BE ABSOLUTE PATH. The path to the directory containing the gold standard dependency files."
    echo "  [-o <output (results) directory>]: MUST BE ABSOLUTE PATH. The path to a directory where the output (dependency performance measures) will be written."
    echo "  [-s <scorer directory>]: MUST BE ABSOLUTE PATH. The path to the conll18_ud_eval.py file"
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
        o) OUTPUT_DIRECTORY=$OPTARG
           ;;
         # The metric to use
        s) SCORER_DIRECTORY=$OPTARG
           ;;
        # HELP!
        h) print_usage; exit 0
           ;;
    esac
done

if [[ -z ${INPUT_DIRECTORY} || -z ${GOLD_DIRECTORY} || -z ${SCORER_DIRECTORY} || -z ${OUTPUT_DIRECTORY} ]]; then
    echo "input directory: ${INPUT_DIRECTORY}"
	echo "gold directory: ${GOLD_DIRECTORY}"
	echo "output directory: ${OUTPUT_DIRECTORY}"
	echo "scorer directory: ${SCORER_DIRECTORY}"
    print_usage
    exit 1
fi


LOG_FILE=${OUTPUT_DIRECTORY}/dep_scores.log
for filename in ${GOLD_DIRECTORY}/*.conllu; do
    # the gold files in CRAFT have a .tree.conllu extension. The .tree part should probably not be there,
    # so we remove it here when looking for the system file to test
    basename=$(basename $filename .tree.conllu)
    fname="${basename%.*}.conllu"
    echo "Evaluating ${fname}"
    python ${SCORER_DIRECTORY}/conll18_ud_eval.py ${filename} ${INPUT_DIRECTORY}/${fname}
    echo "------------------ end ${fname}"
done > ${LOG_FILE}
