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
package org.partners.ppm.cpra.web.rest.dto;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.MoreObjects;

public class CpraDataSet {
	
	private String version;
	private String versionDescription;
	private String calculator;
	protected List<String> ethnicities;
	protected List<String> ethnicFrequencies;
	protected List<String> hlaAlleles;
	protected List<String> hlaAllelesWithFrequencies;
	protected List<String> warnings;

	/**
	 * @return the version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * @return the versionDescription
	 */
	public String getVersionDescription() {
		return versionDescription;
	}

	/**
	 * @param versionDescription the versionDescription to set
	 */
	public void setVersionDescription(String versionDescription) {
		this.versionDescription = versionDescription;
	}

	/**
	 * @return the calculator
	 */
	public String getCalculator() {
		return calculator;
	}

	/**
	 * @param calculator the calculator to set
	 */
	public void setCalculator(String calculator) {
		this.calculator = calculator;
	}

   /**
	* @return the ethnicities
	*/
	public List<String> getEthnicities() {
		return ethnicities;
	}

	/**
	 * @param ethnicities the ethnicities to set
	 */
	public void setEthnicities(List<String> ethnicities) {
		this.ethnicities = ethnicities;
	}
	
	/**
	 * @return the ethnicFrequencies
	 */
	public List<String> getEthnicFrequencies() {
		return ethnicFrequencies;
	}

	/**
	 * @param ethnicFrequencies the ethnicFrequencies to set
	 */
	public void setEthnicFrequencies(List<String> ethnicFrequencies) {
		this.ethnicFrequencies = ethnicFrequencies;
	}
	
	/**
	 * @return the hlaAlleles
	 */
	public List<String> getHlaAlleles() {
		return hlaAlleles;
	}
	
	/**
	 * @param hlaAlleles the hlaAlleles to set
	 */
	public void setHlaAlleles(List<String> hlaAlleles) {
		this.hlaAlleles = hlaAlleles;
	}

	/**
	 * @return the hlaAllelesWithFrequencies
	 */
	public List<String> getHlaAllelesWithFrequencies() {
		return hlaAllelesWithFrequencies;
	}

	/**
	 * @param hlaAllelesWithFrequencies the hlaAllelesWithFrequencies to set
	 */
	public void setHlaAllelesWithFrequencies(List<String> hlaAllelesWithFrequencies) {
		this.hlaAllelesWithFrequencies = hlaAllelesWithFrequencies;
	}

	/**
	 * @return the warnings
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	/**
	 * @param warnings the warnings to set
	 */
	public void setWarnings(List<String> warnings) {
		this.warnings = warnings;
	}

	/**
	 * @param warning the warning to add
	 */
	public void addWarning(String warning) {
		if (this.warnings == null) {
			this.warnings = new ArrayList<>();
		}
		this.warnings.add(warning);
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("version", version)
				.add("versionDescription", versionDescription)
				.add("calculator", calculator)
				.add("ethnicities", ethnicities.toString())
				.add("ethnicFrequencies", ethnicFrequencies.toString())
				.add("hlaAlleles", hlaAlleles.toString())
				.add("hlaAllelesWithFrequencies", hlaAllelesWithFrequencies.toString())
				.add("warnings", warnings.toString())
				.toString();
	}
}

