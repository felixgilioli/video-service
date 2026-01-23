#!/bin/bash
awslocal sqs create-queue --queue-name video-processing-queue
awslocal sqs create-queue --queue-name notification-queue