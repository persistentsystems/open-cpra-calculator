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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.partners.ppm.cpra.CpraRuntimeException;
import org.partners.ppm.cpra.web.rest.dto.CpraDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraDataSet;
import org.partners.ppm.cpra.web.rest.dto.CpraRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpraHaplotypeCalculator extends CpraCalculator {

	private final Logger log = LoggerFactory.getLogger(CpraHaplotypeCalculator.class);

	protected static final String TOKEN_KEY_HLA_ETHNIC_FREQUENCIES = "hlaEthnicFrequencies";
	protected static final String TOKEN_KEY_HLA_HAPLOTYPE_FREQUENCIES = "hlaHaplotypeFrequencies";

	CpraHaplotypeCalculator(CpraConfigHashMap cpraConfigHashMap) {
		super(cpraConfigHashMap);
	}

	@Override
	public CpraDTO calculate(CpraRequest request) {
		CpraDTO dto = new CpraDTO();

		// 0. set the version used in the response DTO
		String version = request.getVersion();
		dto.setVersion(version);

		//
		// 1. Start with Patient Antibodies
		//
		List<String> antibodyList = helperAntibodyList(request);
		dto.setAntibodyList(antibodyList);

		// get reportable allele set and alleles with frequency data for selected
		// version
		String[] alleleSet = versionAlleles.get(version);
		String[] alleleSetWithHlaFreqs = versionAllelesWithFreqs.get(version);

		if (log.isDebugEnabled()) {
			log.debug("version={}/{}; antibodyList={}/{}; alleleSet=({}); alleleSetWithHlaFreqs=({})",
					String.valueOf(dto.getVersion()), String.valueOf(version), String.valueOf(dto.getAntibodyList()),
					String.valueOf(antibodyList), Arrays.toString(alleleSet), Arrays.toString(alleleSetWithHlaFreqs));
		}

		// cPRA is zero if no antibodies, so save additional calculations
		if (antibodyList.isEmpty()) {

			// no antibodies specified
			dto.setCalculatedPRA(0.0);
			return dto;
		}

		//
		// 2. Expand Antibody List with Equivalents to create list of unacceptable
		// antigens
		//

		TreeMap<String, TreeSet<String>> unacceptableAntigens = helperUnacceptableAntigenMap(version, antibodyList,
				alleleSet);

		// build sorted unacceptable antigen list for dto
		TreeSet<String> sortList = new TreeSet<>();
		for (Map.Entry<String, TreeSet<String>> entry : unacceptableAntigens.entrySet()) {
			log.debug("allele: {}",entry.getKey());
			for (String a : entry.getValue()) {
				log.debug("version: {}",entry.getValue());
				sortList.add(entry.getKey() + a);
			}
		}
		StringBuilder unacceptableAntigenListBuilder = new StringBuilder();
		for (String a : sortList) {
			if (unacceptableAntigenListBuilder.length() > 0) {
				unacceptableAntigenListBuilder.append(TOKEN_KEY_SUB_DELIMITER);
			}
			unacceptableAntigenListBuilder.append(a);
		}
		//
		String unacceptableAntigenList = unacceptableAntigenListBuilder.toString();
		log.debug("unacceptableAntigenList={}",unacceptableAntigenList);
		dto.setUnacceptableAntigenList(unacceptableAntigenList);

		//
		// 3. Get Matching Haplotype Frequency Combinations for each level
		//
		// Initialize frequency totals for each ethnicity
		// enthicFreqs[0][] is single allele haplotypes
		// enthicFreqs[1][] is two allele haplotypes
		// enthicFreqs[2][] is three allele haplotypes
		// ...
		double[][] ethnicFreqs;
		log.debug("About to get matching haplotype frequency combinations. alleleSetWithHlaFreqs.length[{}]",
				alleleSetWithHlaFreqs.length);
		try {
			CpraHaplotypeCalculatorHelper calculator = new CpraHaplotypeCalculatorHelper(alleleSetWithHlaFreqs, unacceptableAntigens, dto, version, cpraConfigHashMap);
			ethnicFreqs = calculator.getResult();
		}
		catch(Exception ex) {
			log.error("Caught an exception: {}; {}", ex.getMessage(), ex.getStackTrace());
			throw new CpraRuntimeException("Caught an unexpected error retrieving HLA Frequencies.");
		}
		log.debug("Finished getting matching haplotype frequency combinations. alleleSetWithHlaFreqs.length[{}]",
				alleleSetWithHlaFreqs.length);

		//
		// 4. Calculate cPRA for each ethnicity
		//
		// CPRA for the ethnicity is:
		// probability of a positive crossmatch =
		// 1 – probability of a negative crossmatch =
		// 1 – (1 – S1 + S2 – S3 + S4 – S5)^2
		// 0 Caucasian, 1 African American, 2 Hispanic, 3 Asian
		/*
		 * for bwh_cpra, only 1 ethnicity is returned. so the equation should be: 1 - (1
		 * - S1 + (S2 or 0.0) - 0.0 + 0.0 - 0.0)^2
		 */
		if (log.isDebugEnabled()) {
			log.debug(
					"About to calculate ehtnicCpra. ethnicFreqs.deepToString({}) ethnicFreqs.row.length = ({}) ethnicFreqs.cols.length = ({})",
					Arrays.deepToString(ethnicFreqs), ethnicFreqs.length, ethnicFreqs[0].length);
		}

		// calculate values
		double[] ethnicCpra = calculateEthnicCpras(ethnicFreqs);

		// add to DTO
		String[] ethnicities = helperEthnicityValues(version);
		if (ethnicities.length <= 0) {
			log.error("ERROR: No ethnicities defined for haplotype calculator '{}'",version);
			throw new CpraRuntimeException("Error calculating final cPRA; no ethnicities defined.");
		}
		for (int i = 0; i < ethnicities.length; i++) {
			dto.addEthnicCalculatedPRA(ethnicities[i], ethnicCpra[i]);
		}

		//
		// 5. Get the final CPRA by applying ethnic weights:
		//
		// For each ethnicity multiply CPRA for that ethnicity by ethnic weight
		// Sum all the values to get the final CPRA
		double cpra = calculateOverallCpra(version, ethnicCpra);
		dto.setCalculatedPRA(cpra);

		String r = String.valueOf(cpra);
		if (log.isDebugEnabled()) {
			log.debug("Calculate cPRA for {}; cpra = {}", String.valueOf(antibodyList), r);
			log.debug("dto.toString={}", dto);
		}
		return dto;
	}

	private double calculateOverallCpra(String version, double[] ethnicCpra) {
		double cpra = 0.0;
		try {
			log.trace("About to calculate overall cPRA for version [{}].", version);
			String ethnicFrequencyList = cpraConfigHashMap.get(TOKEN_KEY_HLA_ETHNIC_FREQUENCIES, version);
			String[] ethnicFrequency = null;
			if (ethnicFrequencyList != null) {
				ethnicFrequency = ethnicFrequencyList.split(TOKEN_KEY_SUB_DELIMITER);
			} else {
				log.error("ERROR: failed to get halEthnicFrequencies:{}", version);
				throw new CpraRuntimeException(
						"Failed to get HLA Ethnic Frequencies; must be defined in configuration with key 'hlaEthnicFrequencies:"
						+ version + "'");
			}
			if (log.isTraceEnabled()) {
				log.trace("arrayOfEthnicFrequencies = [{}] ethnicFrequency[{}] ethnicFrequency.length = [{}]",
						ethnicFrequencyList, Arrays.toString(ethnicFrequency), ethnicFrequency.length);
			}

			for (int i = 0; i < ethnicFrequency.length; i++) {
				log.trace("ethnicCpra[{}]({}) * ethnicFrequency[{}]({})", i, ethnicCpra[i], i,
						Float.valueOf(ethnicFrequency[i]));
				cpra += ethnicCpra[i] * Float.valueOf(ethnicFrequency[i]);
			}
			log.trace("Calculated PRA = [{}]", cpra);

		} catch (Exception ex) {
			log.error("Caught an exception: {} ", ex);
			throw new CpraRuntimeException("Caught an unexpected error calculating final cPRA.");
		}
		return cpra;
	}

	private double[] calculateEthnicCpras(double[][] ethnicFreqs) {

		double[] ethnicCpra = new double[UNOS_ETHNICITIES];
		try {
			/*
			 * This simple math operation was replaced with a double for loop to account for
			 * when in some cases, there are no values for all of the frequencies.
			 */
			for (int currentEthnicity = 0; currentEthnicity < UNOS_ETHNICITIES; currentEthnicity++) {
				// Add up the ethnicFreqs and then call Math.pow
				double sumFreqsRow = 0.0;
				// Need to be careful with the operation here. The pattern is:
				// 1 - S1 + S2 - S3 + S4 - S5
				// S1 = ethnicFreqs[0][0 - 3] subtract (even)
				// S2 = ethnicFreqs[1][0 - 3] add (odd)
				// S3 = ethnicFreqs[2][0 - 3] subtract (even)
				// S4 = ethnicFreqs[3][0 - 3] add (odd)
				// S5 = ethnicFreqs[3][0 - 4] subtract (even)
				for (int currentFreq = 0; currentFreq < ethnicFreqs.length; currentFreq++) {
					log.trace("ethnicFreqs[{}][{}] = [{}]", currentFreq, currentEthnicity,
							ethnicFreqs[currentFreq][currentEthnicity]);
					switch (currentFreq) {
					case 0:
						log.trace("1 - ethnicFreqs[{}][{}]({})", currentFreq, currentEthnicity,
								ethnicFreqs[currentFreq][currentEthnicity]);
						sumFreqsRow = 1 - ethnicFreqs[currentFreq][currentEthnicity];
						break;
					case 1:
					case 3:
						log.trace("sumFreqsRow({}) + ethnicFreqs[{}][{}]({})", sumFreqsRow, currentFreq,
								currentEthnicity, ethnicFreqs[currentFreq][currentEthnicity]);
						sumFreqsRow += ethnicFreqs[currentFreq][currentEthnicity];
						break;
					case 2:
					case 4:
						log.trace("sumFreqsRow({}) - ethnicFreqs[{}][{}]({})", sumFreqsRow, currentFreq,
								currentEthnicity, ethnicFreqs[currentFreq][currentEthnicity]);
						sumFreqsRow -= ethnicFreqs[currentFreq][currentEthnicity];
						break;
					default:
						log.error("ERROR: unexpected currentFreq index '{}'; expecting 0-4",currentFreq);
					}
					log.trace("currentEthnicity = [{}] sumFreqsRow = [{}]", currentEthnicity, sumFreqsRow);
				}
				ethnicCpra[currentEthnicity] = 1 - Math.pow(sumFreqsRow, 2);
				log.trace("ethnicCpra[{}] = ({})", currentEthnicity, ethnicCpra[currentEthnicity]);
			}
		} catch (Exception ex) {
			String stackTrace = ExceptionUtils.getStackTrace(ex.getCause());
			log.error("Caught an exception: {} ", stackTrace);

			throw new CpraRuntimeException("Caught an unexpected error calculating ethnic cPRAs.");
		}
		return ethnicCpra;
	}

	@Override
	public CpraDataSet selfCheck(String version) {

		CpraDataSet cpraDataSet = new CpraDataSet();
		cpraDataSet.setVersion(version);

		// show hlaCpraCalculatorDescription
		String description = cpraConfigHashMap.get("hlaCpraCalculatorDescription", version);
		cpraDataSet.setVersionDescription(description);

		// show calculator type
		String calculator = cpraConfigHashMap.get("hlaCpraCalculatorVersion", version);
		cpraDataSet.setCalculator(calculator);

		// show list of hlaEthnicities
		cpraDataSet.setEthnicities(Arrays.asList(helperEthnicityValues(version)));

		// show list of hlaEthnicFrequencies
		String ethnicFrequencyList = cpraConfigHashMap.get(TOKEN_KEY_HLA_ETHNIC_FREQUENCIES, version);
		String[] ethnicFrequency = null;
		if (ethnicFrequencyList != null) {
			ethnicFrequency = ethnicFrequencyList.split(TOKEN_KEY_SUB_DELIMITER);
			cpraDataSet.setEthnicFrequencies(Arrays.asList(ethnicFrequency));
		} else {
			cpraDataSet.addWarning("ERROR: no hlaEthnicFrequencies");
		}

		// show list of hlaAlleles (reportable alleles)
		String hlaAllelesList = cpraConfigHashMap.get(TOKEN_KEY_HLA_ALLELES, version);
		log.warn("HLA_ALLLES = '{}'", hlaAllelesList);
		String[] hlaAlleles = null;
		if (null != hlaAllelesList) {
			hlaAlleles = hlaAllelesList.split(TOKEN_KEY_SUB_DELIMITER);
			log.warn("Found alleles for version '{}' using config search '{}'", version, TOKEN_KEY_HLA_ALLELES);
			cpraDataSet.setHlaAlleles(Arrays.asList(hlaAlleles));
		} else {
			log.warn("No alleles found for version '{}' using config search '{}'", version, TOKEN_KEY_HLA_ALLELES);
			cpraDataSet.setHlaAlleles(Arrays.asList("None Found"));
		}

		// show list of hlaAllelesWithFrequencies (alleles that have frequency data)
		String hlaAllelesWithFrequenciesList = cpraConfigHashMap.get(TOKEN_KEY_HLA_ALLELES_WITH_FREQUENCIES, version);
		String[] hlaAllelesWithFrequencies = null;
		if (null != hlaAllelesWithFrequenciesList) {
			hlaAllelesWithFrequencies = hlaAllelesWithFrequenciesList.split(TOKEN_KEY_SUB_DELIMITER);
			cpraDataSet.setHlaAllelesWithFrequencies(Arrays.asList(hlaAllelesWithFrequencies));
			for (String hlaAlleleWithFrequency : hlaAllelesWithFrequencies) {
				String keyPattern = TOKEN_KEY_HLA_HAPLOTYPE_FREQUENCIES + TOKEN_KEY_DELIMITER + version
						+ TOKEN_KEY_DELIMITER + hlaAlleleWithFrequency + "*";
				log.trace("keyPattern = [{}]", keyPattern);
				List<String> keysList = cpraConfigHashMap.keys(keyPattern);
				if (log.isTraceEnabled()) {
					log.trace("keysList = [{}]", keysList);
				}
			}
		}

		// for each allele with frequency data
		// get count of S1 data

		// for each reportable allele

		// for each unacceptable antigen equivalence value (the right side value of
		// equivalents)
		// (need to get all of the antigen equivalence entries for the reportable allele
		// prefix) - Jane Baronas' dictionary

		// confirm if there are S1 frequency values

		// for each frequency entry

		// confirm the allele prefixes are in the hlaAllelesWithFrequencies

		// load the list of lab reported alleles from the typing lab (maybe in a csv
		// file, or a service input string?)

		// for each lab reported allele

		// get unacceptable antigen equivalences

		// confirm frequency data for either the antigen itself or for its
		// equivalence(s)
		// i.e. Bw4 is reported, determine the list of equivalences
		// there may not be frequency data for Bw4, but there should be for the
		// equivalent unacceptable antigens

		// // // FUTURE: load the list of LifeTrak donor antigens

		// // // load list of acceptable antigen equivalences

		// // // confirm antigen allele is reportable (e.g. should be A or B)

		// // // confirm specific values have frequency data (not perfect but probably
		// helpful)
		// // // confirm count of patients with same HLA donor antigen (maybe from
		// Will's data?)
		return cpraDataSet;
	}
}
