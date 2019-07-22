package com.es.model;

import java.io.Serializable;
import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Movie implements Serializable{
	
	private static final long serialVersionUID = 1L;
	private String id;
	private String director;
	private List<String>  genre; 
	private String year;
	private List<String> actor;
	private String title;
}
