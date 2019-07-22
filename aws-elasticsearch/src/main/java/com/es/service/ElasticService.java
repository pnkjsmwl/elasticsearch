package com.es.service;

import java.net.URI;
import java.util.Map;

import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.es.model.Movie;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@RestController
@Slf4j
public class ElasticService {

	@Value("${elasticsearch.endpoint}")
	private String endpoint;

	@Autowired
	private Gson gson;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private RestHighLevelClient client;

	@GetMapping("/check")
	public void simpleGet() {
		try
		{
			GetRequest getRequest = new GetRequest("movies","_doc","");
			GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
			log.info("Result : "+getResponse);

			/*
			 * Request<?> request =
			 * elasticSearchUtil.generateRequest("",HttpMethodName.GET,"");
			 * 
			 * elasticSearchUtil.performSigningSteps(request);
			 * 
			 * elasticSearchUtil.sendRequest(request);
			 */

		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	@PostMapping("/add")
	public String simplePost(@RequestBody Movie movie) {

		try
		{
			/*
			 * Movie movie = elasticSearchUtil.readJsonFile("data.json");
			 * Request<?> request = elasticSearchUtil.generateRequest(json,HttpMethodName.POST,"/movies/_doc");
			 * elasticSearchUtil.performSigningSteps(request);
			 * elasticSearchUtil.sendRequest(request);
			 */

			String json = gson.toJson(movie);
			System.out.println(json);

			Map<String, Object> convertDocumentToMap = convertDocumentToMap(movie);
			log.info("DocumentToMap : "+convertDocumentToMap);
			IndexRequest indexRequest = new IndexRequest("movies", "_doc", movie.getId()).source(convertDocumentToMap);
			IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);
			String name = indexResponse.getResult().name();

			log.info("Result : "+name);
			return name;

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@GetMapping("/search")
	public void searchWithParam(@RequestParam String key, @RequestParam String value) {

		try
		{
			SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder(); 
			//searchSourceBuilder.query(QueryBuilders.termQuery(key, value)); 
			searchSourceBuilder.query(QueryBuilders.termsQuery(key, value));
			SearchRequest searchRequest = new SearchRequest(); // search in all indices 
			searchRequest.source(searchSourceBuilder); 

			SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

			SearchHits hits = searchResponse.getHits();

			SearchHit[] searchHits = hits.getHits();

			if(searchHits!=null) {

				log.info("Total Hits : "+searchHits.length);
				for (SearchHit hit : searchHits) {

					String sourceAsString = hit.getSourceAsString();
					log.info("Response: "+gson.toJsonTree(sourceAsString));
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	@GetMapping("/simple")
	public void get() {

		try
		{
			RestTemplate rt = new RestTemplate();

			URI uri = UriComponentsBuilder.fromUriString(endpoint)
					.build()
					.toUri();

			ResponseEntity<String> forEntity = rt.getForEntity(uri, String.class);
			log.info("Status code : "+forEntity.getStatusCode());
			log.info("Response : "+gson.toJson(forEntity.getBody()));

		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> convertDocumentToMap(Movie movie) {
		return objectMapper.convertValue(movie, Map.class);
	}
}
