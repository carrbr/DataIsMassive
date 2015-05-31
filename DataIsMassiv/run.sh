#!/bin/bash
#set -x

function build_models() {
    echo
    echo "Build Models: models will be generated and stored in models/.  Expect this to take several hours."
    echo
    echo "building User Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createUCF model/UCF107_101.model data/$1 100
    echo "building Movie Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createMCF model/MCF.model data/$1 100
    echo "building Backwards User Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createUCF model/BUCF.model data/$1 100 back
    echo "building Backwards Movie Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createMCF model/BMCF.model data/$1 100 back
    echo 
    echo "creating Latent Factor Model, 75 features"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createBI model/LFFT.model 75
	echo "calculate biases"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar trainBI model/LFFT.model model/LFFT.model data/$1 0
	echo "train model with heat 1"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar trainBI model/LFFT.model model/LFFT.model data/$1 1 1
	echo "train model with heat .75"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar trainBI model/LFFT.model model/LFFT.model data/$1 1 0.75
	echo "train model with heat .5"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar trainBI model/LFFT.model model/LFFT.model data/$1 1 0.5
	echo "train model with heat .25"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar trainBI model/LFFT.model model/LFFT.model data/$1 1 0.25
	echo "train model with heat 0.01"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar trainBI model/LFFT.model model/LFFT.model data/$1 1 0.01
	echo
}

function generate_ratings() {
    echo
    echo "Generating Ratings: ratings will be stored in data/.  Expect this to take at least an hour."
    echo
    echo "generating ratings for User Collaborative Filtering model"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/UCF107_101.model data/$1 data/UCF107_101_test_result
    echo "generating ratings for Movie Collaborative Filtering model"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/MCF.model data/$1 data/MCF107_101_test_result
    echo "generating ratings for Backwards User Collaborative Filtering model"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/BUCF.model data/$1 data/BUCF107_101_test_result
    echo "generating ratings for Backwards Movie Collaborative Filtering model"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/BMCF.model data/$1 data/BMCF107_101_test_result
    echo
    echo "generating ratings for Latent Factor model"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/LFFT.model data/$1 data/result.txt
    echo
}

function calculate_rmses() {
    echo
    echo "Calculating RMSEs.  This should be a relatively quick process."
    echo
    echo "RMSE for User Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/$1 data/UCF107_101_test_result
    echo "RMSE for Movie Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/$1 data/MCF107_101_test_result
    echo "RMSE for Backwards User Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/$1 data/BUCF107_101_test_result
    echo "RMSE for Backwards Movie Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/$1 data/BMCF107_101_test_result
    echo
    echo "RMSE for Latent Factor model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/$1 data/result.txt
    echo
}

function combine_results() {
    echo
    echo "generating aggregate result file: data/combine_result.txt"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar combine sim data/cf_result.txt data/UCF.log data/MCF.log data/BMCF.log data/BUCF.log
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar combine nosim data/combine_result.txt data/cf_result.txt data/result.txt
    echo "RMSE for combined result (ignore this value if running on test.txt):"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/$1 data/combine_result.txt
    echo
}

function publish() {
    echo
    echo "publishing results to data/student_id_list.txt"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar publish data/combine_result.txt data/
    echo
}

function usage() {
    echo
    echo "invalid args..."
    echo "No arguments will run a full job, building models, generating ratings, combining ratings, and publish the result file"
    echo "Arg options:"
    echo "  build:      build models"
    echo "  rate:       generate ratings"
    echo "  combine:    combine ratings"
    echo "  rmse:       calculate the RMSEs"
    echo "  publish:    pubish result file for submission"
    echo "  all:        execute full job"
    echo
}


# we will execute everything if passed no args, otherwise, execute only the requested function
training_file="training.txt"
test_file="test.txt"
if [ $# -eq 0 ] 
then
    build_models
    generate_ratings
    combine_results
    publish
else
    if [ $# -gt 1 ]
    then
        test_file=$2
    fi
    if [ $# -gt 2 ]
    then
        training_file=$3
    fi
    if [ "$1" == "build" ]
    then
        build_models $training_file
    elif [ "$1" == "rate" ]
    then
        generate_ratings $test_file
    elif [ "$1" == "combine" ]
    then
        combine_results $test_file
    elif [ "$1" == "rmse" ]
    then
        calculate_rmses $test_file
    elif [ "$1" == "publish" ]
    then
        publish
    elif [ "$1" == "all" ]
    then
        build_models $training_file
        generate_ratings $test_file
        combine_results
        publish
    else
        usage
    fi
fi
    exit 0
