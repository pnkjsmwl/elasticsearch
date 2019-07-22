package com.es.util;

import java.io.ByteArrayInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.DefaultRequest;
import com.amazonaws.Request;
import com.amazonaws.RequestConfig;
import com.amazonaws.Response;
import com.amazonaws.auth.AWS4Signer;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.http.AmazonHttpClient;
import com.amazonaws.http.ExecutionContext;
import com.amazonaws.http.HttpMethodName;
import com.es.model.Movie;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ElasticSearchUtil {
	private static final String ENDPOINT = "https://vpc-es-domain-2-di6xlsckmoxve3qaggqqiskflm.us-east-1.es.amazonaws.com";
	//private static final String ENDPOINT = "https://search-es-domain-3-liozz5wscdrqefoblzqrovznri.us-east-1.es.amazonaws.com";
	private static final String REGION = "us-east-1";
	private static final String SERVICE_NAME = "es";
	private static final String AWS_ACCESS_KEY = "AKIAVGINOJ2T3GJ5NJUD";
	private static final String AWS_SECRET_KEY = "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";

	@Autowired
	private Gson gson;

	public Request<?> generateRequest(String json, HttpMethodName method, String contentUri) throws Exception{
		Request<?> request = new DefaultRequest<Void>(SERVICE_NAME);
		request.setContent(new ByteArrayInputStream(json.getBytes()));
		request.setEndpoint(URI.create(ENDPOINT+contentUri));
		request.setHttpMethod(method);
		//request.addParameter("q", "tim");
		return request;
	}

	public void performSigningSteps(Request<?> requestToSign) throws Exception{
		AWS4Signer signer = new AWS4Signer();
		signer.setServiceName(SERVICE_NAME);
		signer.setRegionName(REGION);      
		AWSCredentials credentials = new BasicAWSCredentials( AWS_ACCESS_KEY, AWS_SECRET_KEY );
		signer.sign(requestToSign, credentials);
	}

	public void sendRequest(Request<?> request) throws Exception{
		ExecutionContext context = new ExecutionContext(true);

		ClientConfiguration clientConfiguration = new ClientConfiguration();
		AmazonHttpClient client = new AmazonHttpClient(clientConfiguration);

		MyHttpResponseHandler<Void> responseHandler = new MyHttpResponseHandler<Void>();
		MyErrorHandler errorHandler = new MyErrorHandler();
		RequestConfig requestConfig = RequestConfig.NO_OP;
		log.info(gson.toJson(request));
		System.out.println(request);
		Response<Void> response = client.execute(request, responseHandler, errorHandler, context, requestConfig);
		log.info("Response : "+response);
	}
	
	public Movie readJsonFile(String fileName) throws IOException {
		Resource resource = new ClassPathResource(fileName);
		JsonReader jsonReader = new JsonReader(new FileReader(resource.getFile()));
		Movie movie = gson.fromJson(jsonReader, Movie.class);
		log.info("Movie : "+movie.getTitle());
		return movie;
	}

}
