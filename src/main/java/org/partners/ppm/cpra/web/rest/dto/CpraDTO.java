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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.MoreObjects;

public class CpraDTO {

	class EthnicCpra {
		private String ethnicity;
		private double calculatedPRA;
		public String getEthnicity() {
			return ethnicity;
		}
		public void setEthnicity(String ethnicity) {
			this.ethnicity = ethnicity;
		}
		public double getCalculatedPRA() {
			return calculatedPRA;
		}
		public void setCalculatedPRA(double calculatedPRA) {
			this.calculatedPRA = calculatedPRA;
		}
		
		@JsonCreator
		public EthnicCpra() {
			// empty constructor for annotation
		}
		
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("ethnicity", String.valueOf(ethnicity))
					.add("calculatedPRA", String.valueOf(calculatedPRA))
				.toString();
		}
	}

	private String version;
	private double calculatedPRA;
	private List<String> antibodyList;
	private String unacceptableAntigenList;
	private List<EthnicCpra> ethnicCalculatedPRA = new ArrayList<>();
	private List<String> warnings = new ArrayList<>();

	@JsonCreator
	public CpraDTO() {
		// empty constructor for annotation
	}

	public String getVersion()
	{
		return version;
	}
	public void setVersion(String version)
	{
		this.version = version;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public void addWarning(String warning) {
		warnings.add(warning);
	}

	public double getCalculatedPRA() {
		return calculatedPRA;
	}

	public void setCalculatedPRA(double cpra) {
		this.calculatedPRA = cpra;
	}
	
	public List<String> getAntibodyList()
	{
		return antibodyList;
	}
	public void setAntibodyList(List<String> antibodyList)
	{
		this.antibodyList = antibodyList;
	}

	public String getUnacceptableAntigenList()
	{
		return unacceptableAntigenList;
	}
	public void setUnacceptableAntigenList(String unacceptableAntigenList)
	{
		this.unacceptableAntigenList = unacceptableAntigenList;
	}

	public List<EthnicCpra> getEthnicCalculatedPRA() {
		return ethnicCalculatedPRA;
	}

	public void addEthnicCalculatedPRA(String ethnicity, double calculatedPRA) {
		EthnicCpra ec = new EthnicCpra();
		ec.ethnicity = ethnicity;
		ec.calculatedPRA = calculatedPRA;
		ethnicCalculatedPRA.add(ec);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("calculatedPRA", String.valueOf(calculatedPRA))
				.add("antibodyList", String.valueOf(antibodyList))
				.add("version", String.valueOf(getVersion()))
				.add("warnings", getWarnings())
				.add("unacceptableAntigenList", String.valueOf(unacceptableAntigenList))
				.add("ethnicCalculatedPRA", ethnicCalculatedPRA.toString())
			.toString();
	}
}