package com.es.util;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.http.HttpResponseHandler;

public class MyErrorHandler implements HttpResponseHandler<AmazonServiceException> {

	@Override
	public AmazonServiceException handle(com.amazonaws.http.HttpResponse response) throws Exception {
		System.out.println("In exception handler!");
		
		AmazonServiceException ase = new AmazonServiceException(response.getContent().toString());
		ase.setStatusCode(response.getStatusCode());
		ase.setErrorCode(response.getStatusText());
		return ase;
	}

	@Override
	public boolean needsConnectionLeftOpen() {
		return false;
	}
}