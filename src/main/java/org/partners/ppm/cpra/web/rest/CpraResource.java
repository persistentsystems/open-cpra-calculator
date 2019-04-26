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
package org.partners.ppm.cpra.web.rest;

import javax.validation.ConstraintViolationException;

import org.partners.ppm.cpra.CpraRuntimeException;
import org.partners.ppm.cpra.service.CpraService;
import org.partners.ppm.cpra.web.rest.dto.ArrayOfCpraSelfCheckDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraRequest;
import org.partners.ppm.cpra.web.rest.dto.CpraVersionsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cpra")
public class CpraResource {

	private static final Logger log = LoggerFactory.getLogger(CpraResource.class);
	
	// This following setting is particular to this Restful API
	public static final String ANTIBODY_DELIMITER = ";";
	public static final String DEFAULT_ANTIBODYLIST = "";
	
	private CpraService cpraService;
	
	@Autowired
	public CpraResource(CpraService cpraService) {
		this.cpraService = cpraService;
	}

	@GetMapping("/{version}/calculate")
    public CpraDTO calculateCpra(
    	@PathVariable String version, 
    	@RequestParam(required = false, defaultValue = "") String antibodyList) 
	{

		// Construct CpraRequest object from input
    		// NO validation here for version or arrayOfAntibodies
		CpraRequest request = new CpraRequest(version);
		if (antibodyList!=null && !antibodyList.isEmpty()) {
			log.debug("antibodyList: ({})", antibodyList);
			String[] antibodies = antibodyList.split(ANTIBODY_DELIMITER);
			for (String antibody : antibodies) {
				if (antibody.trim().length() > 0) {
					request.getAntibodies().add(antibody);
				}
			}
		} else {
			log.debug("No antibodyList sent; version '{}'",version);
		}

		// call service bean to do the actual calculation
		try {
			return cpraService.calculate(request);
		}
		catch (ConstraintViolationException e) {
			throw new CpraRuntimeException("Invalid request for Cpra calculator");
		}
	}

	@GetMapping("/versions")
	public CpraVersionsDTO getVersions() {

		// call service bean to get versions
		return cpraService.versions();
	}
	
	@GetMapping("/self-check")
	public ArrayOfCpraSelfCheckDTO selfCheck() {
		
		// call service bean to get versions
		return cpraService.selfCheck();
	}
}