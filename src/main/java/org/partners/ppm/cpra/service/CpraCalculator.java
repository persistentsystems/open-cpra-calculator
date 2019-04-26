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
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import org.partners.ppm.cpra.CpraRuntimeException;
import org.partners.ppm.cpra.web.rest.dto.CpraDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraDataSet;
import org.partners.ppm.cpra.web.rest.dto.CpraRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CpraCalculator {

	private final Logger log = LoggerFactory.getLogger(CpraCalculator.class);
	
	protected static final String EXCEPTION_UNKNOWN = "UNKNOWN_CPRA_CALCULATION_ERROR";
	
	protected static final String TOKEN_KEY_HLA_ALLELES = "hlaAlleles";
	protected static final String TOKEN_KEY_HLA_ALLELES_WITH_FREQUENCIES = "hlaAllelesWithFrequencies";
	protected static final String TOKEN_KEY_HLA_ETHNICITIES = "hlaEthnicities";
	protected static final String TOKEN_KEY_HLA_UNACCEPTABLE_ANGITEN_EQUIVS = "hlaUnacceptableAntigenEquivalences";
	protected static final String TOKEN_KEY_DELIMITER = ":";	
	protected static final String TOKEN_KEY_SUB_DELIMITER = ";";	

	protected static final String EXCEPTION_BAD_CONFIG = "BAD_CONFIGURATION";
	protected static final String EXCEPTION_BAD_ARGUMENT = "BAD_ARGUMENT";

	protected static final Integer UNOS_ETHNICITIES = 4; // the number of ethnicities reported against by UNOS for each HLA Frequencies

	protected CpraConfigHashMap cpraConfigHashMap;
	
	protected TreeMap<String, String[]> versionAlleles;
	protected TreeMap<String, String[]> versionAllelesWithFreqs;
	
	CpraCalculator(CpraConfigHashMap cpraConfigHashMap) {
		this.cpraConfigHashMap = cpraConfigHashMap;
		this.versionAlleles = new TreeMap<>();
		this.versionAllelesWithFreqs = new TreeMap<>();
	}

	public void initialize() {
		// default implementation ignored
	}
	
	public void initializeVersion(String version) {
		// get allele set for version, this is the set of all allowed allele names (A, B, C, DR, DQ, etc.)
		String alleleListStr = cpraConfigHashMap.get(TOKEN_KEY_HLA_ALLELES,version);
		String[] alleleSet = alleleListStr.split(TOKEN_KEY_SUB_DELIMITER);
		versionAlleles.put(version, alleleSet);

		// get alleles with frequency data set for version
		String alleleHlaFreqsListStr = cpraConfigHashMap.get(TOKEN_KEY_HLA_ALLELES_WITH_FREQUENCIES,version);
		String[] alleleSetWithFreqs = alleleHlaFreqsListStr.split(TOKEN_KEY_SUB_DELIMITER);
		versionAllelesWithFreqs.put(version, alleleSetWithFreqs);
	}

	public abstract
	CpraDTO calculate(CpraRequest request);
	
	public abstract 
	CpraDataSet selfCheck(String version);

	protected String[] helperEthnicityValues(String version)
	{
		String ethnicityList = cpraConfigHashMap.get(TOKEN_KEY_HLA_ETHNICITIES,version);
		if (ethnicityList != null) {
			return ethnicityList.split(TOKEN_KEY_SUB_DELIMITER);
		} else {
			return new String[0];
		}
	}

	protected List<String> helperAntibodyList(CpraRequest request) {
		// get list from the request
		List<String> antibodyList = request.getAntibodies();
		
		// Just to be safe, transform all of the characters into upper case in the antibodyList since that's what is stored
		// in the CPRA configuration dataset
		antibodyList.replaceAll(String::toUpperCase);
		return antibodyList;
	}
	
	protected TreeSet<String> helperUnacceptableAntigenSet(
			String version, 
			List<String> antibodyList, 
			String[] alleleSet) 
	{
		// Create unique list of unacceptable antigens TreeSet
		// The TreeSet prevents duplicates.
		TreeSet<String> unacceptableAntigens = new TreeSet<>();
		
		for (String antibody : antibodyList) {

			// add the patient antibody
			String[] allele = this.helperParseAllele(alleleSet, antibody);
				
			// verify antibody is of the form [A-z]*[0-9]*
			// and that the allele name is valid
			// then add get unacceptable antigens to add to our list
			if (allele.length!=2) {
				// Validation should catch this error
				log.warn("Invalid antibody in the input: {}", antibody);
				throw new CpraRuntimeException("Invalid antibody in the input");
			}
			
			// make sure allele is in our alleleSet
			if (!Arrays.asList(alleleSet).contains(allele[0])) {
				if (log.isErrorEnabled()) { 
					log.error("Invalid antibody allele '{}' not in alleleSet '{}'",allele[0],Arrays.toString(alleleSet));
				}
				throw new CpraRuntimeException("Invalid antibody allele");
			}

			// add the antibody to the list of unacceptable antigens
			unacceptableAntigens.add(antibody);

			// look up equivalents and add to the list also
			String aeaList = cpraConfigHashMap.get(TOKEN_KEY_HLA_UNACCEPTABLE_ANGITEN_EQUIVS,version,antibody);
			if (aeaList!=null) {
				for (String aea : aeaList.split(TOKEN_KEY_SUB_DELIMITER)) {
					unacceptableAntigens.add(aea);
				}
			}
		}

		// calculate and set unacceptable antigen list in DTO
		if (log.isDebugEnabled()) {
			log.debug("unacceptableAntigens are '{}'",unacceptableAntigens);
		}
		
		return unacceptableAntigens;
	}
	
	protected TreeMap<String, TreeSet<String>> helperUnacceptableAntigenMap( 
		String version, 
		List<String> antibodyList, 
		String[] alleleSet) 
	{
		TreeMap<String, TreeSet<String>> unacceptableAntigens = new TreeMap<>(); // TreeMap keeps the antigen alleles sorted, TreeSet keeps the antigens sorted
		for (String a : alleleSet) {
			unacceptableAntigens.put(a, new TreeSet<>());
		}
		
		for (String antibody : antibodyList) {
			log.debug("antibody: {}", antibody);

			String[] allele;

			// parse the patient antibody
			allele = this.helperParseAllele(alleleSet, antibody);
			if (allele.length!=2) {
				// Validation should catch this error
				log.warn("Invalid antibody in the input: {}", antibody);
				throw new CpraRuntimeException("Invalid antibody in the input");
			}

			// add to the set
			TreeSet<String> ts = unacceptableAntigens.get(allele[0]);
			if (ts==null) {
				throw new CpraRuntimeException("Invalid antibody allele");
			}
			ts.add(allele[1]);

			// look up equivalents and add to the set
			String aeaList = cpraConfigHashMap.get(TOKEN_KEY_HLA_UNACCEPTABLE_ANGITEN_EQUIVS,version,antibody);
			if (aeaList!=null) {
				for (String aea : aeaList.split(TOKEN_KEY_SUB_DELIMITER)) {
					allele = this.helperParseAllele(alleleSet, aea);
					unacceptableAntigens.get(allele[0]).add(allele[1]);
				}
			}
		}
		return unacceptableAntigens;
	}

	/*
	 * There needed to be a new way to find the allele that matches with the antibody. The old
	 * method was just to split the string into its alpha and numeric components (e.g., DQ1 = DQ (allele[0]) and 1 (allele[1]).
	 * But with the new Alleles from the Sept 2017 data, it can be DQ1 or DQB11 where allele[0] is either DQ or DQB1 and allele[1] = 1.
	 * 
	 */
	protected String[] helperParseAllele(String[] alleleSet, String antibody) {
		
		String[] allele = new String[]{};
		// Reverse sort the alleleSet by length
		Arrays.sort(alleleSet, Comparator.comparingInt(String::length).reversed());
		if (log.isTraceEnabled()) {
			log.trace("antibody ({}) alleleSet({})", antibody, Arrays.toString(alleleSet));
		}
		for (String a : alleleSet) {
			log.trace("allele = ({}) antibody = ({})", a, antibody);
			if (antibody.startsWith(a) && antibody.length() >= a.length()) {
				log.trace("Found {} within {}", a, antibody);
				// One final check to see if the rest of the antibody string has valid values. If it does not, just set
				// the allele, but not the type.
				// If the length of the rest of the antibody is 0, that is also valid.
				// For example: antibody = DR52 - the allele is DR52 with no type is acceptable. Just let the calculateLocal() method figure out what to do next.
				// It should be valid also when the allele is DR and the type is 52
				// http://hla.alleles.org/nomenclature/naming.html
				if (antibody.substring(a.length()).matches("^[\\dA-Z\\-*:]+$") ||
						0 == antibody.substring(a.length()).length()) {
					allele = new String[]{a, antibody.substring(a.length())};
				} else {
					allele = new String[]{a};
				}
				if (log.isTraceEnabled()) {
					log.trace("allele to be returned ({})", Arrays.toString(allele));
				}
				break;
			}
		}
		return allele;
	}
}
