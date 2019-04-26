/**
 * This Source Code Form is subject to the terms of the Mozilla Public License, v.
 * 2.0 with a Healthcare Disclaimer.
 *
 * A copy of the Mozilla Public License, v. 2.0 with the Healthcare Disclaimer can
 * be found under the top level directory, named LICENSE.
 *
 * If a copy of the MPL was not distributed with this file, You can obtain one at
 * http://mozilla.org/MPL/2.0/.
 *
 * If a copy of the Healthcare Disclaimer was not distributed with this file, You
 * can obtain one at the project website https://github.com/persistentsystems/open-cpra-calculator.
 *
 * Copyright (C) 2016-2018 Persistent Systems, Inc.
 */
package org.partners.ppm.cpra.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.partners.ppm.cpra.domain.MapItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

@Component
public class CpraConfigHashMap {

	private final Logger log = LoggerFactory.getLogger(CpraConfigHashMap.class);
	
	// key token parts
	private static final String TOKEN_KEY_DELIMITER = ":";

	private HashMap<String, String> cache = new HashMap<>();


	CpraConfigHashMap() {
		this.cache = new HashMap<>();
	}

	public List<String> keys(String keyPattern) {
		log.debug("keys({}); cache.size()={}", keyPattern, cache.size());
		Set<String> keys = new HashSet<>();

		keyPattern = keyPattern.replace("*", ".*"); // * should be like glob regex which means .* for java regex
		log.trace("new keyPattern={}", keyPattern);
		// use regex matching on keyPattern
		Pattern pattern = Pattern.compile(keyPattern);
		for (String key : cache.keySet()) {
			if (pattern.matcher(key).matches()) {
				keys.add(key);
			}
		}

		List<String> keysList = new ArrayList<>(keys);
		Collections.sort(keysList);
		return keysList;
	}

	public void loadConfiguration(String csvFile) {

		log.debug("loadConfiguration({})",csvFile);

		InputStream file = null;
		
		// configure the schema we want to read
		CsvSchema schema = CsvSchema.builder().addColumn("key").addColumn("value").addColumn("comment")
				.setUseHeader(true).setAllowComments(true).setQuoteChar('"')
				.build();
		CsvMapper mapper = new CsvMapper().enable(CsvParser.Feature.TRIM_SPACES);
		
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] resources = null;
		try {
			//  Use either "classpath*:/*.csv" or "myfile.csv"
			resources = resolver.getResources(csvFile);
		} catch (IOException e) {
			log.error("IO error resolving config resources for '{}'",csvFile);
		}
		for (Resource resource: resources){
			String filename = resource.getFilename();
		    log.info("Processing '{}'",filename);
		    
			// load file from resource
			try {
				file = new ClassPathResource(filename).getInputStream();
			} catch (IOException e1) {
				log.error("Failed to open resource '{}'; {}",filename,e1.getMessage());
			}


			// configure the reader on what bean to read and how we want to write
			// that bean
			ObjectReader mapReader = mapper.readerFor(MapItem.class).with(schema);
			int c = 0;

			// read from file
			try (Reader reader = new InputStreamReader(file)) {
				MappingIterator<MapItem> mi = mapReader.readValues(reader);
				while (mi.hasNext()) {
					c++;
					MapItem item = mi.nextValue();
					log.trace("{}:{}",item.getKey(),item.getValue());
					if (item.getKey().length()>0 && item.getValue().length()>0) {
						cache.put(item.getKey(), item.getValue());
					}
				}
			} catch (FileNotFoundException e) {
				log.error("Configuration file '{}'; not found as resource",csvFile);
			} catch (IOException e) {
				log.error("Configuration file '{}'; IO exception; {}",csvFile, e.getMessage());
			}
			log.trace("loadConfiguration({}) {} entries loaded out of {}",csvFile,cache.size(),c);
		}
	}

	public String get(String key) {
		return cache.get(key);
	}

	public String get(String key1, String key2) {
		StringBuilder sb = new StringBuilder();
		sb.append(key1).append(TOKEN_KEY_DELIMITER).append(key2);
		return get(sb.toString());
	}

	public String get(String key1, String key2, String key3) {
		StringBuilder sb = new StringBuilder();
		sb.append(key1).append(TOKEN_KEY_DELIMITER).append(key2).append(TOKEN_KEY_DELIMITER).append(key3);
		return get(sb.toString());
	}
}
