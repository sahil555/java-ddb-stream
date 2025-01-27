#!/bin/bash

function myEcho() {
    echo -e "\n === \xE2\x9C\x94 $1  === \n"
}

myEcho "Deploying on PROD !!"

export AWS_PAGER="cat"

AWS_PROFILE_FOR_DEPLOYMENT=$1
SCALE_DOWN_SERVICE=$2

if [ ! "$AWS_PROFILE_FOR_DEPLOYMENT" ]; then
    myEcho "aws profile not set :( "
    exit 2
fi


ACCOUNT_ID=$(aws --profile $AWS_PROFILE_FOR_DEPLOYMENT sts get-caller-identity --output text --query Account)
#IMAGE_TAG=$(git rev-parse --short HEAD)
IMAGE_TAG=kyc_queue_latest
IMAGE_NAME=pii-validator
REGION=ap-south-1

if [ -z "$ACCOUNT_ID" ]; then
    myEcho "AWS Credentials not set , please try again :("
    exit 2
fi


myEcho "Starting deployment process for accountId : $ACCOUNT_ID" &&
    mvn package &&
    myEcho "Jar Created" &&
    aws --profile $AWS_PROFILE_FOR_DEPLOYMENT ecr get-login-password --region $REGION | docker login --username AWS --password-stdin $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com &&
    docker build --platform=linux/amd64 -t $IMAGE_NAME . &&
    myEcho "Image Build" &&
    docker tag $IMAGE_NAME:latest $ACCOUNT_ID.dkr.ecr.ap-south-1.amazonaws.com/$IMAGE_NAME:$IMAGE_TAG &&
    myEcho "Image Tagged" &&
    docker push $ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com/$IMAGE_NAME:$IMAGE_TAG &&
    myEcho "Image Pushed"