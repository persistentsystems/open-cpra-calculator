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

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.partners.ppm.cpra.web.rest.dto.CpraDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CpraHaplotypeCalculatorHelper {
	
	private final Logger log = LoggerFactory.getLogger(CpraHaplotypeCalculatorHelper.class);
	
	private static final Integer UNOS_ETHNICITIES = 4;
	private static final String TOKEN_KEY_SUB_DELIMITER = ";";
	private static final String TOKEN_KEY_HLA_HAPLOTYPE_FREQUENCIES = "hlaHaplotypeFrequencies";

	String[] alleles;
	double[][] ethnicFreqs;
	TreeMap<String, TreeSet<String>> unacceptableAntigens;     // [ A => [2, 9], B => [4, 57], C => [ 01, 02, 03] ]
	TreeMap<String, List<String>> unacceptableAlleleVersions;  // [ A => [A2, A9], B => [B4, B57], C => [C01, C02, C03] ]
	CpraDTO dto;
	String version;
	CpraConfigHashMap cpraConfigHashMap;

	CpraHaplotypeCalculatorHelper(String[] alleles, TreeMap<String, TreeSet<String>> unacceptableAntigens, CpraDTO dto, String version,
			CpraConfigHashMap cpraConfigHashMap) {
		log.debug("Calculator(alleles={},unacceptableAntigens={},dto=...,version={},configHashMap=...)",alleles, unacceptableAntigens, version);
		this.alleles = alleles;
		this.ethnicFreqs = new double[alleles.length][UNOS_ETHNICITIES];
		for (int i = 0; i < alleles.length; i++) {
			for (int j = 0; j < UNOS_ETHNICITIES; j++) // 0 Caucasian, 1 African American, 2 Hispanic, 3 Asian
			{
				this.ethnicFreqs[i][j] = 0.0;
			}
		}
		this.unacceptableAntigens = unacceptableAntigens;
		this.dto = dto;
		this.version = version;
		this.cpraConfigHashMap = cpraConfigHashMap;

		this.unacceptableAlleleVersions = new TreeMap<>();

		// calculate the ethnic frequencies for all haplotype combinations
		this.calculateLevels();
	}

	public double[][] getResult() {
		return this.ethnicFreqs;
	}
	
	private void calculateLevels() {
		// which alleles do we have antibodies/unacceptable antigens for?
		ArrayList<String> allelesInUse = new ArrayList<>();
		for (String allele : alleles) {
			if (!unacceptableAntigens.get(allele).isEmpty()) {
				// add allele to our list of ones in use
				log.trace("Found unacceptable antigens for allele '{}'",allele);
				allelesInUse.add(allele);
				
				// create String List of allele versions for this allele
				List<String> alleleVersions = new LinkedList<>();
				
				// convert between set formats:
				// unacceptableAntigens in format [ A => [2, 9], B => [4, 57], C => [ 01, 02, 03] ]
				// unacceptableAlleleVersions in format [ A => [A2, A9], B => [B4, B57], C => [C01, C02, C03] ]
				TreeSet<String> versions = unacceptableAntigens.get(allele);
				for (String ver: versions) {
					alleleVersions.add(allele+ver);
				}
				unacceptableAlleleVersions.put(allele, alleleVersions);
			}
		}
		int nAlleles = allelesInUse.size();
		log.debug("Found {} alleles in use; {}", nAlleles, allelesInUse);

		// a place to store our required allele combinations based on the antibody/unacceptable antigen alleles present
		// the bitset represents the alleles in the "alleles with frequencies array", bit position = array position
		TreeMap<Integer, ArrayList<BitSet>> levelSets = new TreeMap<>();
		for (Integer s=1; s<=alleles.length; s++) {
			levelSets.put(s, new ArrayList<>());
		}
		log.debug("Created empty levelSets for use in cross products; size = {}",levelSets.size());
		
		// now get the unique combinations of these alleles
		//   algorithm:
		//      count from 1 to (2^n)-1    (2^2)-1 = 3
		//      for each number in the list, each bit represents a specific allele
		//             A  B  DR DQ C  each bit represents and allele in the combination
		//             1  0  0  0  1  antibodies include A and C (S2)
		//             0  1  0  0  1  antibodies include B and C (S2)
		//             1  0  1  1  1  antibodies include A, DR, DQ, and C (S4)
		//             0  1  0  0  0  antibodies include B (S1)
		//      but if we only have 2 allelesInUse, for example B and C, it would look like this:
		//             B  C
		//             0  1   C only (S1)
		//             1  0   B only (S1)
		//             1  1   B and C (S2)
		//      we'll add the list of set alleles to the levelSets TreeMap based on how many bits are set
		//      1 bit set means only 1 allele, so the S1 set
		//      2 bits set means a 2 allele combo, so the S2 set
		//      etc.
		BitSet bs;
		for (int i = 1; i<= Math.pow(2, nAlleles)-1; i++) {
			bs = BitSet.valueOf(new long[] {i});
			levelSets.get(bs.cardinality()).add(bs);
		}
		for(Map.Entry<Integer,ArrayList<BitSet>> entry : levelSets.entrySet()) {
			Integer key = entry.getKey();
			ArrayList<BitSet> value = entry.getValue();

			log.trace("{} = {}",key ,value);
		}

		// for each set level (S1, S2, .. S5) calculate the ethnic frequencies
		// for each of the combinations of all allele versions present in unacceptableAntigens
		//     e.g. if there are two B allele versions (B4, B57) and 3 C
		for (Integer s=1; s<=alleles.length; s++) {
			for (BitSet combo : levelSets.get(s)) {
				calculateCombo(s, combo, allelesInUse);
			}
		}
	}
	
	private void calculateCombo(Integer s, BitSet combo, ArrayList<String> allelesInUse) {
		log.trace("calculateCombo(s={}, combo={}, allelesInUse={}) ENTER",s,combo,allelesInUse);
		// determine which alleles we're looking to permutate
		LinkedList<List <String>> lists = new LinkedList<>();
		for (int i = combo.nextSetBit(0); i != -1; i = combo.nextSetBit(i + 1)) {
			// i is now the index of the next set bit, which is the index of the allele in allelesInUse array
			// so get a copy of the list of allele versions for that allele and add to the list for cross product
		    lists.add(unacceptableAlleleVersions.get(allelesInUse.get(i)));
		}
		
		// generate and loop through the allele combinations, adding up the ethnic frequencies
		List<String> result = new ArrayList<>();
		for (String combination : createAlleleCombinations(lists, result, 0, "")) {
			log.trace("GET frequencies for {}",combination);
			log.trace("GET haplotypeFrequencies:{}:{}", version, combination);
			String freqList = cpraConfigHashMap.get(TOKEN_KEY_HLA_HAPLOTYPE_FREQUENCIES, version, combination);
			if (freqList != null) {
				log.trace("FOUND hlaHaplotypeFrequencies:{}:{} = (freqList = {})", version, combination, freqList);
				String[] f = freqList.split(TOKEN_KEY_SUB_DELIMITER);
				for (int e = 0; e < f.length; e++) {
					ethnicFreqs[s-1][e] += Float.valueOf(f[e]);
					log.trace("ethnicFreqs[{}][{}] = {}", s-1, e, ethnicFreqs[s-1][e]);
				}
			} else if (s-1 == 0) {
				dto.addWarning("No S1 haplotype frequencies found for HLA-" + combination);
			}
		}
	}

	List<String> createAlleleCombinations(LinkedList<List<String>> lists, List<String> result, int depth, String current)
	{
	    if(depth == lists.size())
	    {
	       result.add(current);
	       return result;
	     }

	    for(int i = 0; i < lists.get(depth).size(); ++i)
	    {
	        createAlleleCombinations(lists, result, depth + 1, current + (current.length()==0?"":TOKEN_KEY_SUB_DELIMITER) + lists.get(depth).get(i));
	    }
	    return result;
	}
}
