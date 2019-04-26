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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.partners.ppm.cpra.CpraRuntimeException;
import org.partners.ppm.cpra.web.rest.dto.CpraDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraDataSet;
import org.partners.ppm.cpra.web.rest.dto.CpraRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpraDiplotypeCalculator extends CpraCalculator {

	private final Logger log = LoggerFactory.getLogger(CpraDiplotypeCalculator.class);

	private static final String TOKEN_KEY_DIPLOTYPE_FREQ = "hlaDiplotypeFrequencies";

	// optimized set of diplotype entries for each version allowing lookup of diplotypes by alleles:
	//    HashMap<Version, HashMap<Allele, HashSet<Diplotype>>>
	//    bwh_cpra_v2 
	private HashMap<String, HashMap<String, HashSet<Diplotype>>> diplotypeHashMap;

	CpraDiplotypeCalculator(CpraConfigHashMap cpraConfigHashMap) {
		super(cpraConfigHashMap);
	}
	
	class Diplotype {
		private Set<String> alleles = new TreeSet<>(); // set for sorted and unique values
		private Double frequency;
		
		Diplotype(String a1, String a2, String b1, String b2) {
			add(a1);
			add(a2);
			add(b1);
			add(b2);
		}
		Diplotype(String list, Double freq) {
			for (String a : list.split(TOKEN_KEY_SUB_DELIMITER)) {
				add(a);
			}
			setFrequency(freq);
		}
		Diplotype() {}

		public boolean add(String allele) {
			return alleles.add(allele);
		}
		void setFrequency(Double frequency) {
			this.frequency = frequency;
		}
		Double getFrequency() {
			return this.frequency;
		}
		Set<String> getAlleles() {
			return this.alleles;
		}
		// returns allele set as concatenated string in sort order, e.g. A7A9B2B44
		public String toString() {
			return StringUtils.join(alleles, null);
		}
	}

	@Override
	public void initialize() {
		super.initialize();
		// optimize the cpra diplotype-based calculation (HashMap<Version, HashMap<Allele, HashSet<Diplotype>>>)
		diplotypeHashMap = new HashMap<>();
	}

	@Override
	public void initializeVersion(String version) {
		log.debug("initializeVersion({})",version);
		super.initializeVersion(version);
			
		// Create tuple sets for each allele, allowing lookup of diplotype frequency by allele
		// A2 -> A2;A7;B57;B2:0.0021, A2;A5;B3;B7:0.00313, ...
		// A7 -> A2;A7;B57;B2:0.0021, ...
		// Then collect the unique tuples for the patient
		HashMap<String, HashSet<Diplotype>> versionDiplotypeHashMap =  new HashMap<>();
		diplotypeHashMap.put(version, versionDiplotypeHashMap);
		
		// get all the diplotypes and frequencies for the version
		List<String> keys = cpraConfigHashMap.keys(TOKEN_KEY_DIPLOTYPE_FREQ+TOKEN_KEY_DELIMITER+version+TOKEN_KEY_DELIMITER+"*");
		log.debug("Found '{}' keys in config for calculator '{}'",keys.size(),version);
		for (String key : keys) {

			// create Diplotype object for our data structure
			String val = cpraConfigHashMap.get(key);
			String[] tmp = key.split(":");
			String diplotype = tmp.length>0?tmp[tmp.length-1]:null;
			Diplotype d = new Diplotype(diplotype,Double.valueOf(val));

			log.trace("Creating diplotype for key '{}', diplotype '{}', freq '{}'",key, diplotype, val);
			// for each diplotype/freq allele
			for (String a : d.getAlleles()) {

				// create a hashmap entry for that allele pointing to the diplotype
				versionDiplotypeHashMap
					.computeIfAbsent(a, it -> new HashSet<>())
					.add(d);
			}
		}
	}
	
	// This calculator uses a set of diplotype frequencies (A1, A2, B1, B2) from the historical blood bank inventory.
	// The method is to
	//   1. Determine unacceptable antigens from the patient antibodies
	//   2. Add up the unique set of diplotype frequencies which include any of those antigens, counting each diplotype only once if there is an antigen match
	//      For example, inventory diplotype A2,A9,B4,B57 with frequency 0.0001 would only be counted once if a patient had antibodies A2 and B4, or A9 and A2.
	@Override
	public CpraDTO calculate(CpraRequest request) {
		
		// create the DTO
		CpraDTO dto = new CpraDTO();
		
		// 0. include selected version in the response
		String version = request.getVersion();
		dto.setVersion(version);
		
		//
		// 1. Start with Patient Antibodies
		//

		// Just to be safe, transform all of the characters into upper case in the antibodyList since that's what is stored
		// in the CPRA configuration dataset
		List<String> antibodyList = helperAntibodyList(request);
		dto.setAntibodyList(antibodyList);

		// get allele sets for selected version (those reportable and those with frequencies)
		String[] alleleSet = versionAlleles.get(version);
		String[] alleleSetWithHlaFreqs = versionAllelesWithFreqs.get(version);
		
		if (log.isDebugEnabled()) {
			log.debug("version={}/{}; antibodyList={}/{}; alleleSet=({}); alleleSetWithHlaFreqs=({})",
					String.valueOf(dto.getVersion()),
					String.valueOf(version),
					String.valueOf(dto.getAntibodyList()),
					String.valueOf(antibodyList),
					Arrays.toString(alleleSet),
					Arrays.toString(alleleSetWithHlaFreqs));
		}

		//
		// 2. Expand Antibody List with Equivalents to create list of unacceptable antigens
		//
		
		// OPTIMIZATION: don't bother calculating cpra or determining unacceptable antigens if there are no antibodies
		if (antibodyList.isEmpty()) {
			dto.setCalculatedPRA(0.0);
			return dto;
		} 

		// Create unique list of unacceptable antigens TreeSet
		// The TreeSet prevents duplicates.
		TreeSet<String> unacceptableAntigens = helperUnacceptableAntigenSet(version, antibodyList, alleleSet);

		int c = 0;
		StringBuilder unacceptableAntigenListBuilder = new StringBuilder();
		for (String ua : unacceptableAntigens) {
			unacceptableAntigenListBuilder.append(c>0?TOKEN_KEY_SUB_DELIMITER:"").append(ua);
			c++;
		}
		dto.setUnacceptableAntigenList(unacceptableAntigenListBuilder.toString());
		
		if (log.isTraceEnabled()) {
			for (String entry : unacceptableAntigens) {
				log.trace("After expanding AntibodyList: {}", entry);
			}
		}

		//
		// 3. Get matching diplotype frequencies for each unacceptable antigen and add them up
		//
		try {
			Double cpra = calculateOverallCpra(version,unacceptableAntigens);
			// check for slight overage due to significant digit inaccuracy to prevent > 1 value
			if (cpra>1.0 && cpra<1.0001) {
				cpra = 1.0;
			} else if (cpra>1.0) {
				dto.addWarning("CPRA is > 1 due to a computational or source data set issue.");
			} else if (cpra<0.0) {
				dto.addWarning("CPRA is < 0 due to a computational or source data set issue.");
			}
			dto.setCalculatedPRA(cpra);
			if (log.isDebugEnabled()) {
				log.debug("CPRA = {}",cpra);
			}

		} catch (Exception ex) {
			String stackTrace = ExceptionUtils.getStackTrace(ex.getCause());
			log.error("Caught an exception: {}; {}", ex.getMessage(), stackTrace);
			throw new CpraRuntimeException("Caught an unexpected error retrieving HLA Frequencies.");
		}

		if (log.isDebugEnabled()) {
			log.debug("dto.toString={}", dto);
		}
		return dto;
	}
	
	private Double calculateOverallCpra(String version, TreeSet<String> unacceptableAntigens) {
		// for each unacceptable antigen get its diplotype entries and add to our unique set of matches
		Set<Diplotype> matches = new HashSet<>();
		HashMap<String, HashSet<Diplotype>> dhm = diplotypeHashMap.get(version);
		for (String ua : unacceptableAntigens) {
			log.trace("Get matching diplotypes; for ua='{}'",ua);
			HashSet<Diplotype> hsd = dhm.get(ua);
			if (hsd!=null) {
				for (Diplotype d : dhm.get(ua)) {
					if (log.isTraceEnabled()) {
						log.trace("Get matching diplotypes; for ua='{}'; adding '{}'",ua,d);
					}
					matches.add(d); // relying on the HashSet to only keep a unique set of Diplotypes
				}
			}
		}
		log.debug("Found {} matches in diplotype set",matches.size());
		
		// now add up the frequencies for the matches
		Double cpra = 0.0;
		for (Diplotype d : matches) {
			cpra += d.getFrequency();
		}
		return cpra;
	}

	@Override
	public CpraDataSet selfCheck(String version) {

		CpraDataSet cpraDataSet = new CpraDataSet();
		cpraDataSet.setVersion(version);

		// show hlaCpraCalculatorDescription
		String description = cpraConfigHashMap.get("hlaCpraCalculatorDescription",version);
		cpraDataSet.setVersionDescription(description);

		// show calculator type
		String calculator = cpraConfigHashMap.get("hlaCpraCalculatorVersion",version);
		cpraDataSet.setCalculator(calculator);
	
		// show list of hlaEthnicities
		cpraDataSet.setEthnicities(Arrays.asList(helperEthnicityValues(version)));
	
		// show list of hlaEthnicFrequencies
		String ethnicFrequencyList = cpraConfigHashMap.get("hlaEthnicFrequencies",version);
		String[] ethnicFrequency = null;
		if (ethnicFrequencyList!=null) 
		{
			ethnicFrequency = ethnicFrequencyList.split(TOKEN_KEY_SUB_DELIMITER);
			cpraDataSet.setEthnicFrequencies(Arrays.asList(ethnicFrequency));
		}
		else
		{
			cpraDataSet.addWarning("ERROR: failed to get halEthnicFrequencies");
		}
	
		// show list of hlaAlleles (reportable alleles)
		String hlaAllelesList = cpraConfigHashMap.get(TOKEN_KEY_HLA_ALLELES,version);
		String[] hlaAlleles = null;
		if (null != hlaAllelesList) {
			hlaAlleles = hlaAllelesList.split(TOKEN_KEY_SUB_DELIMITER);
			cpraDataSet.setHlaAlleles(Arrays.asList(hlaAlleles));
		}
	
		// show list of hlaAllelesWithFrequencies (alleles that have frequency data)
		String hlaAllelesWithFrequenciesList = cpraConfigHashMap.get(TOKEN_KEY_HLA_ALLELES_WITH_FREQUENCIES,version);
		String[] hlaAllelesWithFrequencies = null;
		if (null != hlaAllelesWithFrequenciesList) {
			hlaAllelesWithFrequencies = hlaAllelesWithFrequenciesList.split(TOKEN_KEY_SUB_DELIMITER);
			cpraDataSet.setHlaAllelesWithFrequencies(Arrays.asList(hlaAllelesWithFrequencies));
			for (String hlaAlleleWithFrequency : hlaAllelesWithFrequencies) {
				String keyPattern = "haplotypeFrequencies:"+version+":"+hlaAlleleWithFrequency+"*";
				log.trace("keyPattern = [{}]", keyPattern);
				List<String> keysList = cpraConfigHashMap.keys(keyPattern);
				if (log.isDebugEnabled()) {
					log.debug("keysList = [{}]", keysList);
				}
			}
		}
		
	
		// for each allele with frequency data
			// get count of S1 data
	
		// for each reportable allele
	
			// for each unacceptable antigen equivalence value (the right side value of equivalents)
			// (need to get all of the antigen equivalence entries for the reportable allele prefix) - Jane Baronas' dictionary
	
				// confirm if there are S1 frequency values
	
		// for each frequency entry
	
			// confirm the allele prefixes are in the hlaAllelesWithFrequencies
	
		// load the list of lab reported alleles from the typing lab (maybe in a csv file, or a service input string?)
	
		// for each lab reported allele
	
			// get unacceptable antigen equivalences
	
			// confirm frequency data for either the antigen itself or for its equivalence(s)
			// i.e. Bw4 is reported, determine the list of equivalences
			// there may not be frequency data for Bw4, but there should be for the equivalent unacceptable antigens
	
		// // // FUTURE: load the list of LifeTrak donor antigens
	
			// // // load list of acceptable antigen equivalences
	
			// // // confirm antigen allele is reportable (e.g. should be A or B)
	
			// // // confirm specific values have frequency data (not perfect but probably helpful)
			// // // confirm count of patients with same HLA donor antigen (maybe from Will's data?)
		return cpraDataSet;
	}
}
