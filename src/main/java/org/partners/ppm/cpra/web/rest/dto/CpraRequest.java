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
import java.util.Arrays;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.MoreObjects;

public class CpraRequest {
	
	public static final String SPLIT_ALPHA_NUMERIC = "[^A-Z0-9]+|(?<=[A-Z])(?=[0-9])|(?<=[0-9])(?=[A-Z])";
	
	@NotNull
	@Size(min = 1)
	private String version;
	
	// use validator-collection - https://github.com/jirutka/validator-collection
	// @EachPattern(regexp="^[a-zA-Z]+[0-9\\-*:]+$", message = "${validatedValue} is not a valid antibody")
	private List<@Pattern(regexp="^[a-zA-Z]+[0-9\\-*:]+$", message = "${validatedValue} is not a valid antibody") String> antibodies;
	
	public CpraRequest(String version) {
		this.version = version;
		this.antibodies = new ArrayList<>();
	}
	
	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<String> getAntibodies() {
		return antibodies;
	}

	public void setAntibodies(List<String> antibodies) {
		this.antibodies = antibodies;
	}	
	
	public void addAntibodies(String antibodies) {
		if (StringUtils.isNotBlank(antibodies)) {
			this.setAntibodies(Arrays.asList(antibodies.split(";")));
		}
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("antibodies", String.join(", ", antibodies))
			.toString();
	}
}
