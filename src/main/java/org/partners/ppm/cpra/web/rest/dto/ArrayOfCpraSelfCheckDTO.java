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

public class ArrayOfCpraSelfCheckDTO {

	protected List<CpraDataSet> cpraDataSetList = new ArrayList<>();
	
	/**
     * Gets the value of the cpraDataSet property
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the object.
     * This is why there is not a <CODE>set</CODE> method for the cpraDataSet property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getCpraDataSet().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link CpraDataSet }
     * 
	 * @return the List of CpraDataSet
	 */
	public List<CpraDataSet> getCpraDataSet() {
		if (null == cpraDataSetList) {
			cpraDataSetList = new ArrayList<>();
		}
		return cpraDataSetList;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this).add("cpraDataSetList", cpraDataSetList).toString();
	}
	
}