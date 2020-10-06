#!/bin/bash

mkdir -p /home/craft/baseline-cr/txt
mkdir -p /home/craft/baseline-cr/input
mkdir -p /home/craft/baseline-cr/output
mkdir -p /home/craft/baseline-cr/berkeley-system

# copy the 30 text files in the evaluation set to /home/craft/baseline-cr/txt
for f in /home/craft/craft-st-2019-2019_test_data/coreference-resolution/conllcoref/*.conll
do
    fname=$(basename $f)
    filename="${fname%.*}"
    txtfile=$(echo "/home/craft/CRAFT-4.0.1/articles/txt/$filename.txt")
    cp $txtfile /home/craft/baseline-cr/txt
done

