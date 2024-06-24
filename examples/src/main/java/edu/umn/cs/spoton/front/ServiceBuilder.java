package edu.umn.cs.spoton.front;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;

public class ServiceBuilder {


  public static AmazonS3 prepareS3() {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials("access_key", "secret_key");
    return AmazonS3ClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(InternalConfig.getInstance()
                                                           .getLocalStackEndPointApi(),
                                                       InternalConfig.getInstance().REGION))
        .withPathStyleAccessEnabled(true)
        .build();
  }


  public static AmazonTextract prepareTextract() {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials("access_key", "secret_key");
    return AmazonTextractClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(InternalConfig.getInstance().localStackEndpoint,
                                                       InternalConfig.getInstance().REGION))
        .build();
  }


  public static DynamoDB prepareAmazonDynamoDB() {
    BasicAWSCredentials awsCredentials = new BasicAWSCredentials("access_key", "secret_key");

    AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard()
        .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(InternalConfig.getInstance()
                                                           .getLocalStackEndPointApi(),
                                                       InternalConfig.getInstance().REGION))
        .build();

    return new DynamoDB(client);
  }
}