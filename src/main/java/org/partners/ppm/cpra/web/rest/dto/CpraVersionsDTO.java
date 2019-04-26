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

public class CpraVersionsDTO {

	class Version {
		private String name;
		private Boolean isDefault;
		private String description;
		private String calculator;
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public Boolean getIsDefault() {
			return isDefault;
		}
		public void setIsDefault(Boolean isDefault) {
			this.isDefault = isDefault;
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}
		public String getCalculator() {
			return calculator;
		}
		public void setCalculator(String calculator) {
			this.calculator = calculator;
		}
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("name", String.valueOf(name))
					.add("isDefault", String.valueOf(isDefault))
					.add("description", String.valueOf(description))
					.add("calculator", String.valueOf(calculator))
				.toString();
		}
	}

	private List<Version> versions = new ArrayList<>();

	@JsonCreator
	public CpraVersionsDTO() {
		// empty constructor for annotation
	}

	public List<Version> getVersions()
	{
		return versions;
	}
	public void setVersions(List<Version> versions)
	{
		this.versions = versions;
	}

	public void addVersion(String name, Boolean isDefault, String description, String calculator) {
		Version v = new Version();
		v.name = name;
		v.isDefault = isDefault;
		v.description = description;
		v.calculator = calculator;
		if (versions==null) {
			versions = new ArrayList<>();
		}
		versions.add(v);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("versions", String.valueOf(versions))
			.toString();
	}
}