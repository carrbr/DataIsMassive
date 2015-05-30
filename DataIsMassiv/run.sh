#!/bin/bash
#set -x

function build_models() {
    echo
    echo "Build Models: models will be generated and stored in models/.  Expect this to take several hours."
    echo
    echo "building User Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createUCF model/UCF107_101.model data/UCF107_101_train 100
    echo "building Movie Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createMCF model/MCF.model data/UCF107_101_train 100
    echo "building Backwards User Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createUCF model/BUCF.model data/UCF107_101_train 100 back
    echo "building Backwards Movie Collaborative Filtering Model..."
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar createMCF model/BMCF.model data/UCF107_101_train 100 back
    echo
}

function generate_ratings() {
    echo
    echo "Generating Ratings: ratings will be stored in data/.  Expect this to take at least an hour."
    echo
    echo "generating ratings for User Collaborative Filtering model"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/UCF107_101.model data/UCF107_101_test data/UCF107_101_test_result
    echo "generating ratings for Movie Collaborative Filtering model"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/MCF.model data/UCF107_101_test data/MCF107_101_test_result
    echo "generating ratings for Backwards User Collaborative Filtering model"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/BUCF.model data/UCF107_101_test data/BUCF107_101_test_result
    echo "generating ratings for Backwards Movie Collaborative Filtering model"
#    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar \test model/BMCF.model data/UCF107_101_test data/BMCF107_101_test_result
    echo
}

function calculate_rmses() {
    echo
    echo "Calculating RMSEs.  This should be a relatively quick process."
    echo
    echo "RMSE for User Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/UCF107_101_test data/UCF107_101_test_result
    echo "RMSE for Movie Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/UCF107_101_test data/MCF107_101_test_result
    echo "RMSE for Backwards User Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/UCF107_101_test data/BUCF107_101_test_result
    echo "RMSE for Backwards Movie Collaborative Filtering model:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/UCF107_101_test data/BMCF107_101_test_result
    echo
}

function combine_results() {
    echo
    echo "generating aggregate result file: data/combine_result.txt"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar combine data/combine_result.txt data/UCF.log data/MCF.log data/BMCF.log data/BUCF.log
    echo "RMSE for combined result:"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar rmse data/UCF107_101_test data/combine_result.txt
    echo
}

function publish() {
    echo
    echo "publishing results to data/student_id_list.txt"
    java -Xms1024M -Xmx3072M -jar bin/data_is_massive.jar publish data/combine_result data/
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
    echo
}


# we will execute everything if passed no args, otherwise, execute only the requested function
if [ $# -eq 0 ] 
then
    build_models
    generate_ratings
    combine_results
    publish
else
    if [ "$1" == "build" ]
    then
        build_models
    elif [ "$1" == "rate" ]
    then
        generate_ratings
    elif [ "$1" == "combine" ]
    then
        combine_results
    elif [ "$1" == "rmse" ]
    then
        calculate_rmses
    elif [ "$1" == "publish" ]
    then
        publish
    else
        usage
    fi
fi
    exit 0
