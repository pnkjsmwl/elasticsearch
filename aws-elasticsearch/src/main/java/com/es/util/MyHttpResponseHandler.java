package com.es.util;

import java.io.IOException;
import java.io.InputStream;

import com.amazonaws.AmazonWebServiceResponse;
import com.amazonaws.http.HttpResponseHandler;
import com.amazonaws.util.IOUtils;

public class MyHttpResponseHandler<T> implements HttpResponseHandler<AmazonWebServiceResponse<T>> {
	
	@Override
	public AmazonWebServiceResponse<T> handle(com.amazonaws.http.HttpResponse response) throws Exception {

		InputStream responseStream = response.getContent();
		String responseString = convertStreamToString(responseStream);
		System.out.println("Response : "+responseString);
		AmazonWebServiceResponse<T> awsResponse = new AmazonWebServiceResponse<T>();
		
		return awsResponse;
	}

	private String convertStreamToString(InputStream responseStream) throws IOException {
		return IOUtils.toString(responseStream);
	}

	@Override
	public boolean needsConnectionLeftOpen() {
		return false;
	}
}