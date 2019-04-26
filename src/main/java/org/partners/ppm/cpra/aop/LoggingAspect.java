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
package org.partners.ppm.cpra.aop;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class LoggingAspect {

	   @Pointcut("within(org.partners.ppm.cpra.*) || within(org.partners.hip.cpra.service..*) || within(org.partners.hip.cpra.web.rest..*)")
	    public void loggingPointcut() {
	        // Method is empty as this is just a Poincut, the implementations are in the advices.
	    }

}
