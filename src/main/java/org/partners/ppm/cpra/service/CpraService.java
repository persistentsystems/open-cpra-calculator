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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.Valid;

import org.partners.ppm.cpra.CpraRuntimeException;
import org.partners.ppm.cpra.config.CpraConfiguration;
import org.partners.ppm.cpra.web.rest.dto.ArrayOfCpraSelfCheckDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraDataSet;
import org.partners.ppm.cpra.web.rest.dto.CpraRequest;
import org.partners.ppm.cpra.web.rest.dto.CpraVersionsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class CpraService {

	private final Logger log = LoggerFactory.getLogger(CpraService.class);

	// calculators
	private static final String CPRA_CALCULATOR_HAPLOTYPE = "haplotype";
	private static final String CPRA_CALCULATOR_DIPLOTYPE = "diplotype";

	private static final List<String> calculatorList = Arrays.asList(CPRA_CALCULATOR_HAPLOTYPE, CPRA_CALCULATOR_DIPLOTYPE); 
	
	// versions
	private static final String CPRA_VERSION_CURRENT = "current";  // token for default calculator, 
	                                                               // configured by first version in the configuration version list
	private List<String> versionList;
	private Map<String, String> versionCalculator;
	
	private CpraConfiguration cpraConfig;
	private CpraConfigHashMap cpraConfigHashMap;
	
	private CpraHaplotypeCalculator haplotypeCalculator;
	private CpraDiplotypeCalculator diplotypeCalculator;
	
	@Autowired
	public CpraService(CpraConfiguration cpraConfig, CpraConfigHashMap cpraConfigHashMap) {
		this.cpraConfig = cpraConfig;
		this.cpraConfigHashMap = cpraConfigHashMap;
		this.haplotypeCalculator = new CpraHaplotypeCalculator(cpraConfigHashMap);
		this.diplotypeCalculator = new CpraDiplotypeCalculator(cpraConfigHashMap);
	}
	
	@PostConstruct
	public void initService()
	{
		log.debug("CpraService.initService(); load configuration: {} ", cpraConfig.getConfigPath());

		// load the Redis database with cPRA data sets
		String configPath = cpraConfig.getConfigPath();
		cpraConfigHashMap.loadConfiguration(configPath);
		
		// initialize the calculators and versions
		haplotypeCalculator.initialize();
		diplotypeCalculator.initialize();

		// set version list
		String versionListString = cpraConfigHashMap.get("hlaCpraVersions");
		if (versionListString==null) {
			log.error("ERROR: hlaCpraVersions not specified in configuration '{}'; this is a required value",configPath);
			throw configurationException("ERROR: hlaCpraVersions not specified in configuration; this is a required value");
		}
		this.versionList = Arrays.asList(versionListString.split(";"));

		// set version calculators
		this.versionCalculator = new HashMap<>();
		String calculator;
		for (String v : this.versionList) {
			log.debug("set version info for '{}'",v);
			// calculator type for the version
			calculator = cpraConfigHashMap.get("hlaCpraCalculatorType",v);
			this.versionCalculator.put(v, calculator);
			log.debug("setVersionCalculators(): {}, {}",v,calculator);

			if (calculator.equalsIgnoreCase(CPRA_CALCULATOR_DIPLOTYPE)) {
				this.diplotypeCalculator.initializeVersion(v);
			} else if (calculator.equalsIgnoreCase(CPRA_CALCULATOR_HAPLOTYPE)) {
				this.haplotypeCalculator.initializeVersion(v);
			} else {
				log.error("Invalid calculator '{}' for version '{}'",calculator,v);
				throw badCalculatorException(calculator,v);
			}
		}
	}
	
	public CpraVersionsDTO versions() {
		CpraVersionsDTO dto = new CpraVersionsDTO();
		int i = 0;
		for (String name : versionList) {
			Boolean isDefault ;
			if (i==0) {
				isDefault = true;
			}  else {
				isDefault = false;
			}
			String description = cpraConfigHashMap.get("hlaCpraCalculatorDescription:"+name);
			String calculator = versionCalculator.get(name);
			dto.addVersion(name, isDefault, description, calculator);
			i++;
		}
		return dto;
	}
	
	public CpraDTO calculate(@Valid CpraRequest request) {		
		
		String requestedVersion = request.getVersion();
		String impliedVersion = null;
		
		// With the combination of the Constructor Injection and the @PostConstruct notation, the cache may
		// have a value, but the versionList can be null. The @PostConstruct is called before the actual CpraService is
		// fully instantiated. In this case, the csv is loaded, meaning the cache now has data and the
		// versionList is set. But because the CpraService object is not yet fully instantiated, the versionList is back
		// to an uninitialized state once @PostConstruct exits even though cache is already initiated.
		// To do: Figure out a better way to load the .csv file before CpraService is instantiated. Probably
		// create a separate @Configuration item.
		if (null != cpraConfigHashMap && null == versionList) {
			initService();
		} else if (null == cpraConfigHashMap) {
			throw new CpraRuntimeException("The cPRA database has not been initiated properly. Please contact a system administrator");
		}
		
		if (!this.versionList.contains(requestedVersion) && !requestedVersion.equalsIgnoreCase(CPRA_VERSION_CURRENT)) {
			throw new CpraRuntimeException("Invalid version for Cpra calculator; version can be 'current' or "+versionList.toString());
		}
		// if user requests "current" calculator version, then use first from the list in configuration version list
		if (requestedVersion.equalsIgnoreCase(CPRA_VERSION_CURRENT)) {
			impliedVersion = versionList.get(0);
			log.debug("using current version '{}'",impliedVersion);
			request.setVersion(impliedVersion);
		} else {
			impliedVersion = requestedVersion;
		}
		
		String calculator = versionCalculator.get(impliedVersion);
		if (calculator.equalsIgnoreCase(CPRA_CALCULATOR_DIPLOTYPE)) {
			return diplotypeCalculator.calculate(request);
		} else if (calculator.equalsIgnoreCase(CPRA_CALCULATOR_HAPLOTYPE)) {
			return haplotypeCalculator.calculate(request);
		} else {
			log.error("Invalid calculator '{}' for requested '{}' and actual '{}'",calculator,requestedVersion,impliedVersion);
			throw badCalculatorException(calculator,impliedVersion);
		}
	}

	/*
	 * Conduct a self check of the service and the cPRA datasets in Redis to determine integrity
	 */
	public ArrayOfCpraSelfCheckDTO selfCheck() {

		ArrayOfCpraSelfCheckDTO cpraSelfCheckDto = new ArrayOfCpraSelfCheckDTO();
		
		StringBuilder response = new StringBuilder();
		response.append("CpraService Self Check:\n");
		if (log.isDebugEnabled()) {
			log.debug("response = [{}]", response);
		}

		// collect selfCheckinfo for each hlaCpraCalculatorVersions value
		String calculator;
		CpraDataSet cpraDataSet;
		for (String v : this.versionList) {
			// calculator type for the version
			calculator = cpraConfigHashMap.get("hlaCpraCalculatorType",v);
			this.versionCalculator.put(v, calculator);
			log.trace("setVersionCalculators(): {}, {}",v,calculator);

			if (calculator.equalsIgnoreCase(CPRA_CALCULATOR_DIPLOTYPE)) {
				cpraDataSet = this.diplotypeCalculator.selfCheck(v);
			} else if (calculator.equalsIgnoreCase(CPRA_CALCULATOR_HAPLOTYPE)) {
				cpraDataSet = this.haplotypeCalculator.selfCheck(v);
			} else {
				log.error("Invalid calculator '{}' for version '{}'",calculator,v);
				throw badCalculatorException(calculator, v);
			}
			cpraSelfCheckDto.getCpraDataSet().add(cpraDataSet);
		}
		
		if (log.isDebugEnabled()) {
			log.debug("About to return from selfCheck: response = [{}]", response);
		}
		return cpraSelfCheckDto;
	}
	
	private CpraRuntimeException badCalculatorException(String version, String calculator) {
		return new CpraRuntimeException("Invalid calculator '"+calculator+"' for Cpra version '"+version+"'; calculator can be "+String.join(",",calculatorList));
	}

	private CpraRuntimeException configurationException(String message) {
		return new CpraRuntimeException("Invalid configuration; "+message);
	}
	
}

