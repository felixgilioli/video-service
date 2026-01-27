#!/bin/bash

# Filas SQS
awslocal sqs create-queue --queue-name video-processing-queue
awslocal sqs create-queue --queue-name video-status-queue
awslocal sqs create-queue --queue-name notification-queue

# Tópico SNS
awslocal sns create-topic --name video-completed-topic

# Inscreve filas no tópico (fan-out)
awslocal sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:000000000000:video-completed-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:us-east-1:000000000000:video-status-queue

awslocal sns subscribe \
  --topic-arn arn:aws:sns:us-east-1:000000000000:video-completed-topic \
  --protocol sqs \
  --notification-endpoint arn:aws:sqs:us-east-1:000000000000:notification-queue