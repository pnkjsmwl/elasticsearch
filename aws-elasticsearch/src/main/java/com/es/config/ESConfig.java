package com.es.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import org.apache.http.HttpHost;
import org.apache.http.HttpRequestInterceptor;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.SdkClientException;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicSessionCredentials;
import com.amazonaws.auth.InstanceProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import com.amazonaws.services.securitytoken.model.GetSessionTokenRequest;
import com.amazonaws.services.securitytoken.model.GetSessionTokenResult;
import com.es.util.AWSRequestSigningApacheInterceptor;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ESConfig {

	@Value("${elasticsearch.endpoint}")
	private String endpoint;

	private static final String REGION = "us-east-1";
	private static final String SERVICE_NAME = "es";
	//private static final String EC2_ROLE_ARN = "arn:aws:iam::357047357095:role/MyEC2Role";
	//private static final String S3_ROLE_ARN = "arn:aws:iam::357047357095:role/MyS3Role";
	private static final String ROLE_SESSION_NAME = "-session";

	public BasicSessionCredentials getTempCred(String role_arn) {

		/* This works for Internet exposed ES */
		//RestHighLevelClient client = new RestHighLevelClient( RestClient.builder(new HttpHost(host, 80)));

		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
				//.withCredentials(new ProfileCredentialsProvider())
				.withRegion(REGION)
				.build();

		AssumeRoleRequest roleRequest = new AssumeRoleRequest().withRoleArn(role_arn).withRoleSessionName("es"+ROLE_SESSION_NAME);

		AssumeRoleResult response = stsClient.assumeRole(roleRequest);
		log.info("AssumeRoleResult : "+response.getCredentials());

		BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(
				response.getCredentials().getAccessKeyId(), 
				response.getCredentials().getSecretAccessKey(),
				response.getCredentials().getSessionToken());

		log.info("Session AccessKeyId : "+response.getCredentials().getAccessKeyId());
		log.info("Session SecretAccessKey : "+response.getCredentials().getSecretAccessKey());
		log.info("Session SessionToken : "+response.getCredentials().getSessionToken());

		return basicSessionCredentials;
	}

	public BasicSessionCredentials getTempCredWithSession() {

		AWSSecurityTokenService stsClient = AWSSecurityTokenServiceClientBuilder.standard()
				.withCredentials(new InstanceProfileCredentialsProvider(false))
				.withRegion(REGION)
				.build();

		/*
		 * AssumeRoleRequest roleRequest = new
		 * AssumeRoleRequest().withRoleArn(EC2_ROLE_ARN).withRoleSessionName("s3"+
		 * ROLE_SESSION_NAME);
		 * 
		 * AssumeRoleResult response = stsClient.assumeRole(roleRequest);
		 * log.info("AssumeRoleResult : "+response.getCredentials());
		 */


		GetSessionTokenRequest getSessionTokenRequest = new GetSessionTokenRequest().withDurationSeconds(7200); 
		GetSessionTokenResult sessionTokenResult = stsClient.getSessionToken(getSessionTokenRequest);

		Credentials sessionCredentials = sessionTokenResult 
				.getCredentials()
				.withSessionToken(sessionTokenResult.getCredentials().getSessionToken())
				.withExpiration(sessionTokenResult.getCredentials().getExpiration());

		BasicSessionCredentials basicSessionCredentials = new BasicSessionCredentials(
				sessionCredentials.getAccessKeyId(), sessionCredentials.getSecretAccessKey(),
				sessionCredentials.getSessionToken());

		log.info("Session AccessKeyId : "+basicSessionCredentials.getAWSAccessKeyId());
		log.info("Session SecretAccessKey : "+basicSessionCredentials.getAWSSecretKey());
		log.info("Session SessionToken : "+basicSessionCredentials.getSessionToken());

		return basicSessionCredentials;
	}

	//@Bean
	public AmazonS3 s3Client() throws IOException {
		log.info("Creating S3...");
		try {

			File file = new File("/home/ec2-user/dummy.txt");
			AWSCredentialsProvider credsProvider = new InstanceProfileCredentialsProvider(false);
			log.info(credsProvider.getCredentials().getAWSAccessKeyId());
			log.info(credsProvider.getCredentials().getAWSSecretKey());

			//BasicSessionCredentials basicSessionCredentials = getTempCredWithSession();
			AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
					.withCredentials(credsProvider)
					.withRegion(REGION)
					.build();

			S3Object object = s3Client.getObject("p2s-logs-bucket-nv", "data.json");
			InputStream objectData = object.getObjectContent();

			BufferedReader br = new BufferedReader(new InputStreamReader(objectData));
			String line;
			while ((line = br.readLine()) != null) {
				log.info(line);
			}
			br.close();


			ObjectListing objects = s3Client.listObjects("p2s-logs-bucket-nv");
			List<S3ObjectSummary> objectSummaries = objects.getObjectSummaries();
			log.info("No. of Objects: " + objectSummaries.size());


			List<Bucket> listBuckets = s3Client.listBuckets();
			listBuckets.forEach(b -> {
				log.info("Bucket : "+b.getName()); 
				ObjectListing objectListing =	s3Client.listObjects(b.getName()); 
				if("p2s-logs-bucket-nv".equals(b.getName()))
					s3Client.putObject(b.getName(), "dummy", file);
				log.info("No. of Objects: " +objectListing.getObjectSummaries().size()); 
			});


			return s3Client;
		} catch (AmazonServiceException e) {
			log.error("AmazonServiceException occured !!");
			e.printStackTrace();
		} catch (SdkClientException e) {
			log.error("SdkClientException occured !!");
			e.printStackTrace();
		} catch (Exception e) {
			log.error("Exception occured !!");
			e.printStackTrace();
		}
		return null;

	}


	@Bean//(destroyMethod = "close") 
	public RestHighLevelClient	restHighLevelClient() {
		log.info("Creating ES Client...");
		/*
		 * BasicSessionCredentials basicSessionCredentials = getTempCred(EC2_ROLE_ARN);
		 * 
		 * AWSCredentialsProvider credsProvider = new
		 * AWSStaticCredentialsProvider(basicSessionCredentials);
		 */

		AWSCredentialsProvider credsProvider = new InstanceProfileCredentialsProvider(false);
		log.info(credsProvider.getCredentials().getAWSAccessKeyId());
		log.info(credsProvider.getCredentials().getAWSSecretKey());

		AWS4Signer signer = new AWS4Signer(); 
		signer.setServiceName(SERVICE_NAME);
		signer.setRegionName(REGION); 
		HttpRequestInterceptor interceptor = new AWSRequestSigningApacheInterceptor(SERVICE_NAME, signer, credsProvider);

		RestClientBuilder builder =	RestClient.builder(HttpHost.create(endpoint))
				.setHttpClientConfigCallback(hacb -> hacb.addInterceptorLast(interceptor)); 

		//Header[] defaultHeaders = new Header[]{new BasicHeader("X-Amz-Security-Token",	basicSessionCredentials.getSessionToken())};
		//builder.setDefaultHeaders(defaultHeaders);

		RestHighLevelClient client = new RestHighLevelClient(builder);

		return client;

	}

	/*
	 * @Bean public RestHighLevelClient restHighLevelClientNew() {
	 * log.info("Creating ES Client 2..."); RestClientBuilder builder =
	 * RestClient.builder(HttpHost.create(endpoint)); RestHighLevelClient client =
	 * new RestHighLevelClient(builder); return client; }
	 */

}
