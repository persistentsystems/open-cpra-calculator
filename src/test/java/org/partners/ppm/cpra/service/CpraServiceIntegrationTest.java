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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.validation.ConstraintViolationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.partners.ppm.cpra.CpraApplication;
import org.partners.ppm.cpra.CpraRuntimeException;
import org.partners.ppm.cpra.web.rest.dto.CpraDTO;
import org.partners.ppm.cpra.web.rest.dto.CpraRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CpraApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test, standalone")
public class CpraServiceIntegrationTest {

	private final Logger log = LoggerFactory.getLogger(CpraServiceIntegrationTest.class);

	@Autowired
	private CpraService cpraService;
	
	/*
	 * Wrapper method to the cpraService.calculate where it sets the version.
	 */
	private CpraDTO testCurrentWithAntibodyList(String antibodyList) throws Exception {
		CpraRequest request = new CpraRequest("optn_2015");
		request.addAntibodies(antibodyList);
		log.info("Input antibodyList: <{}>", antibodyList);
		return this.cpraService.calculate(request);
	}

	/*
	 * A wrapper method for assertThat. It logs to log.info the expected and the actual result from assertThat.
	 */
	private <T> void assertThatWrapper(String message, T actual, org.hamcrest.Matcher<T> matcher) throws Exception {
		log.info(message, actual.toString(), matcher.toString());
		assertThat(actual, matcher);
	}
	
	@Test(expected = CpraRuntimeException.class)
	public void testCpraCalculateUno() throws Exception {
		log.info("Test Case Name: Test Case Name: testCpraCalculateUno");
		CpraRequest request = new CpraRequest("unos");
		this.cpraService.calculate(request);
	}
	
	@Test
	public void testCpraCalculateCurrentNoAntibody() throws Exception {
		log.info("Test Case Name: Test Case Name: testCpraCalculateCurrentNoAntibody");
		CpraDTO dto = this.testCurrentWithAntibodyList("");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.0));
	}

	@Test
	public void testCpraUseCase3() throws Exception {
		log.info("Test Case Name: Test Case Name: testCpraUseCase3");
		CpraDTO dto = this.testCurrentWithAntibodyList("A9");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.23667027217061132));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A23;A24;A2402;A2403;A9"));
	}

	@Test
	public void testCpraUseCase4() throws Exception {
		log.info("Test Case Name: Test Case Name: testCpraUseCase4");
		CpraDTO dto = this.testCurrentWithAntibodyList("B57");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.06705253036669612));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("B57;B5701;B5703"));
	}
	
	@Test
	public void testCpraUseCase5() throws Exception {
		log.info("Test Case Name: Test Case Name: testCpraUseCase5");
		CpraDTO dto = this.testCurrentWithAntibodyList("A9;B57");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.29206652084852197));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A23;A24;A2402;A2403;A9;B57;B5701;B5703"));
	}

	@Test
	public void testCpraUseCase6a() throws Exception {
		log.info("Test Case Name: Test Case Name: testCpraUseCase6a");
		CpraDTO dto = this.testCurrentWithAntibodyList("DR52");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.622892362816549));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DR11;DR12;DR13;DR14;DR17;DR18;DR3;DR5;DR52;DR6"));
	}

	// The Typing Lab is not yet using DR52 but will run this test anyway. Rory Oct2017
	@Test
	public void testCpraUseCase6b() throws Exception {
		log.info("Test Case Name: Test Case Name: testCpraUseCase6b");
		CpraDTO dto = this.testCurrentWithAntibodyList("DR5252");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.622892362816549));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DR11;DR12;DR13;DR14;DR17;DR18;DR3;DR5;DR52;DR5252;DR6"));
	}

	// This test is really not going to work just yet mainly because the Typing Lab has not yet transitioned to the DBQ1 notation yet. It is still using DQ
	// The calculated PRA will not match that from the online OPTN calculator, but that's because there is probably a combination of the DQB1 HLA frequencies that are
	// not correct - Rory Oct2017
	@Test
	public void testCpraUseCase7a() throws Exception {
		log.info("Test Case Name: testCpraUseCase7a");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4;C01;C02;C03;DR1;DR2;DR3;DQB11;DQB12;DQB13");
		/* The OPTN calculator results in 100% for this combination.
		 */
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9999640878709916));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4;C01;C02;C03;C09;C10;DQ1;DQ2;DQ3;DQ5;DQ6;DQ7;DQ8;DQ9;DQB10201;DQB10202;DQB10301;DQB10302;DQB10303;DQB10319;DQB10501;DQB10502;DQB10601;DQB10602;DQB10603;DQB10604;DQB10609;DQB11;DQB12;DQB13;DQB15;DQB16;DQB17;DQB18;DQB19;DR0101;DR0102;DR0301;DR0302;DR1;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3"));
	}

	@Test
	public void testCpraUseCase7aA() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aA");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.7622328452544779));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3"));
	}

	@Test
	public void testCpraUseCase7aB() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aB");
		CpraDTO dto = this.testCurrentWithAntibodyList("B5;B7");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.322709065476639));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("B0702;B5;B51;B5101;B5102;B52;B7"));
	}

	@Test
	public void testCpraUseCase7aBW() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aBW");
		CpraDTO dto = this.testCurrentWithAntibodyList("BW4");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.6128343832886861));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B77;BW4"));
	}

	@Test
	public void testCpraUseCase7aC() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aC");
		CpraDTO dto = this.testCurrentWithAntibodyList("C01;C02;C03");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.4104978915255446));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("C01;C02;C03;C09;C10"));
	}

	@Test
	public void testCpraUseCase7aDR() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aDR");
		CpraDTO dto = this.testCurrentWithAntibodyList("DR1;DR2;DR3");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.5940911817764275));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DR0101;DR0102;DR0301;DR0302;DR1;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3"));
	}

	@Test
	public void testCpraUseCase7aDQB1() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aDQB1");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQB11;DQB12;DQB13");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.996551056703074));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ1;DQ2;DQ3;DQ5;DQ6;DQ7;DQ8;DQ9;DQB10201;DQB10202;DQB10301;DQB10302;DQB10303;DQB10319;DQB10501;DQB10502;DQB10601;DQB10602;DQB10603;DQB10604;DQB10609;DQB11;DQB12;DQB13;DQB15;DQB16;DQB17;DQB18;DQB19"));
	}

	@Test
	public void testCpraUseCase7aAB() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aAB");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.8206306786839033));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B5;B51;B5101;B5102;B52;B7"));
	}

	@Test
	public void testCpraUseCase7aABBW() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aABBW");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9348775376695679));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4"));
	}

	@Test
	public void testCpraUseCase7aABBWC() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aABBWC");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4;C01;C02;C03");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.965786539029409));
		log.info("Test Case Name: getUnacceptableAntigenList({})", dto.getUnacceptableAntigenList());
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4;C01;C02;C03;C09;C10"));
	}

	@Test
	public void testCpraUseCase7aABBWCDR() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aABBWCDR");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4;C01;C02;C03;DR1;DR2;DR3");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9829464884167299));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4;C01;C02;C03;C09;C10;DR0101;DR0102;DR0301;DR0302;DR1;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3"));
	}

	@Test
	public void testCpraUseCase7aABBWCDRDQB11() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aABBWCDRDQB11");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4;C01;C02;C03;DR1;DR2;DR3;DQB11");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9931897839539836)); 
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4;C01;C02;C03;C09;C10;DQ1;DQ5;DQ6;DQB10501;DQB10502;DQB10601;DQB10602;DQB10603;DQB10604;DQB10609;DQB11;DQB15;DQB16;DR0101;DR0102;DR0301;DR0302;DR1;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3"));
	}

	@Test
	public void testCpraUseCase7aABBWCDRDQB12() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aABBWCDRDQB12");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4;C01;C02;C03;DR1;DR2;DR3;DQB12");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9888441234477681)); 
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4;C01;C02;C03;C09;C10;DQ2;DQB10201;DQB10202;DQB12;DR0101;DR0102;DR0301;DR0302;DR1;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3"));
	}

	@Test
	public void testCpraUseCase7aABBWCDRDQB13() throws Exception {
		log.info("Test Case Name: testCpraUseCase7aABBWCDRDQB13");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4;C01;C02;C03;DR1;DR2;DR3;DQB13");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9934693058608153)); 
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4;C01;C02;C03;C09;C10;DQ3;DQ7;DQ8;DQ9;DQB10301;DQB10302;DQB10303;DQB10319;DQB13;DQB17;DQB18;DQB19;DR0101;DR0102;DR0301;DR0302;DR1;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3"));
	}

	@Test
	public void testCpraUseCase7b() throws Exception {
		log.info("Test Case Name: testCpraUseCase7b");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A3;B5;B7;BW4;C01;C02;C03;DR1;DR2;DR3;DQ1;DQ2;DQ3");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9999640878709916));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A2;A203;A3;B0702;B0802;B0803;B0804;B13;B1301;B1302;B1513;B1516;B1517;B17;B27;B37;B38;B44;B4402;B4403;B4415;B47;B49;B5;B51;B5101;B5102;B52;B53;B57;B5701;B5703;B58;B59;B63;B7;B77;BW4;C01;C02;C03;C09;C10;DQ1;DQ2;DQ3;DQ5;DQ6;DQ7;DQ8;DQ9;DR0101;DR0102;DR0301;DR0302;DR1;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3"));
	}

	@Test
	public void testCpraC1() throws Exception {
		log.info("Test Case Name: testCpraC1");
		CpraDTO dto = this.testCurrentWithAntibodyList("C1");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.0));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("C1"));
	}

	@Test
	public void testCpraCw1() throws Exception {
		log.info("Test Case Name: testCpraCw1");
		CpraDTO dto = this.testCurrentWithAntibodyList("CW1");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.0));
//		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("C1"));
	}

	@Test
	public void testCpraC01() throws Exception {
		log.info("Test Case Name: testCpraC01");
		CpraDTO dto = this.testCurrentWithAntibodyList("C01");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.07725089077176983));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("C01"));
	}

	@Test
	public void testCpraDQ1() throws Exception {
		log.info("Test Case Name: testCpraDQ1");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQ1");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.6440740590623333));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ1;DQ5;DQ6"));
	}

	@Test
	public void testCpraDQB11() throws Exception {
		log.info("Test Case Name: testCpraDQB11");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQB11");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.6440740590623333));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ1;DQ5;DQ6;DQB10501;DQB10502;DQB10601;DQB10602;DQB10603;DQB10604;DQB10609;DQB11;DQB15;DQB16"));
	}

	@Test
	public void testCpraDQ2() throws Exception {
		log.info("Test Case Name: testCpraDQ2");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQ2");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.36862861847170264));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ2"));
	}

	@Test
	public void testCpraDQB12() throws Exception {
		log.info("Test Case Name: testCpraDQB12");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQB12");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.36862861847170264));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ2;DQB10201;DQB10202;DQB12"));
	}

	@Test
	public void testCpraDQ3() throws Exception {
		log.info("Test Case Name: testCpraDQ3");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQ3");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.5609165635637898));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ3;DQ7;DQ8;DQ9"));
	}

	@Test
	public void testCpraDQB13() throws Exception {
		log.info("Test Case Name: testCpraDQB13");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQB13");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.5609165635637898));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ3;DQ7;DQ8;DQ9;DQB10301;DQB10302;DQB10303;DQB10319;DQB13;DQB17;DQB18;DQB19"));
	}

	@Test
	public void testCpraAllOfA() throws Exception {
		log.info("Test Case Name: testCpraAllOfA");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A2;A0201;A0202;A0203;A0205;A0206;A3;A9;A10;A11;A1101;A1102;A19;A23;A24;A2402;A2403;A25;A26;A28;A29;A2901;A2902;A30;A3001;A3002;A31;A32;A33;A3301;A3303;A34;A3401;A3402;A36;A43;A66;A6601;A6602;A68;A6801;A6802;A69;A74;A80");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9999999832353711));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A10;A11;A1101;A1102;A19;A2;A203;A23;A24;A2402;A2403;A25;A26;A28;A29;A2901;A2902;A3;A30;A3001;A3002;A31;A32;A33;A3301;A3303;A34;A3401;A3402;A36;A43;A66;A6601;A6602;A68;A6801;A6802;A69;A74;A80;A9"));
	}

	@Test
	public void testCpraAllOfB() throws Exception {
		log.info("Test Case Name: testCpraAllOfB");
		CpraDTO dto = this.testCurrentWithAntibodyList("B5;B7;B0702;B8;B0801;B0802;B0803;B0804;B12;B13;B1301;B1302;B14;B1401;B1402;B15;B1501;B1502;B1503;B1510;B1511;B1512;B1513;B1516;B1517;B16;B17;B18;B21;B22;B27;B2705;B2708;B35;B37;B38;B39;B3901;B3902;B3905;B3913;B40;B4001;B4002;B4005;B4006;B41;B42;B44;B4402;B4403;B4415;B45;B46;B47;B48;B49;B50;B51;B5101;B5102;B52;B53;B54;B55;B56;B57;B5701;B5703;B58;B59;B60;B61;B62;B63;B64;B65;B67;B70;B71;B72;B73;B75;B76;B77;B78;B81;B82");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9999999303142866));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("B0702;B0801;B0802;B0803;B0804;B12;B13;B1301;B1302;B14;B1401;B1402;B15;B1501;B1502;B1503;B1510;B1511;B1512;B1513;B1516;B1517;B16;B17;B18;B21;B22;B27;B2705;B2708;B35;B37;B38;B39;B3901;B3902;B3905;B3913;B40;B4001;B4002;B4005;B4006;B41;B42;B44;B4402;B4403;B4415;B44415;B45;B46;B47;B48;B49;B5;B50;B51;B5101;B5102;B52;B53;B54;B55;B56;B57;B5701;B5703;B58;B59;B60;B61;B62;B63;B64;B65;B67;B7;B70;B71;B72;B73;B75;B76;B77;B78;B8;B81;B82"));
	}

	@Test
	public void testCpraAllOfBW() throws Exception {
		log.info("Test Case Name: testCpraAllOfBW");
		CpraDTO dto = this.testCurrentWithAntibodyList("BW4;BW6");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9998507021388408));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("B0702;B0801;B0802;B0803;B0804;B13;B1301;B1302;B14;B1401;B1402;B1501;B1502;B1503;B1510;B1511;B1512;B1513;B1516;B1517;B17;B18;B22;B27;B2708;B35;B37;B38;B39;B3901;B3902;B3905;B3913;B40;B4001;B4002;B4005;B4006;B41;B42;B44;B4402;B4403;B4415;B45;B47;B48;B49;B5;B50;B51;B5101;B5102;B52;B53;B54;B55;B56;B57;B5701;B5703;B58;B59;B60;B61;B62;B63;B64;B65;B67;B7;B70;B71;B72;B75;B76;B77;B78;B8;B81;B82;BW4;BW6"));
	}

	@Test
	public void testCpraAllOfC() throws Exception {
		log.info("Test Case Name: testCpraAllOfC");
		CpraDTO dto = this.testCurrentWithAntibodyList("C01;C02;C03;C04;C05;C06;C07;C0701;C0702;C08;C09;C10;C12;C14;C15;C16;C17;C18");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.999999983235515));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("C01;C02;C03;C04;C05;C06;C07;C0701;C0702;C08;C09;C10;C12;C14;C15;C16;C17;C18"));
	}

	@Test
	public void testCpraAllOfDR() throws Exception {
		log.info("Test Case Name: testCpraAllOfDR");
		CpraDTO dto = this.testCurrentWithAntibodyList("DR1;DR0101;DR0102;DR103;DR2;DR3;DR0301;DR0302;DR4;DR0401;DR0402;DR0403;DR0404;DR0405;DR0407;DR5;DR6;DR7;DR8;DR9;DR0901;DR0902;DR10;DR11;DR1101;DR1104;DR12;DR1201;DR1202;DR13;DR1301;DR1303;DR14;DR1401;DR1402;DR1403;DR1404;DR1454;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9999999832353654));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DR0101;DR0102;DR0301;DR0302;DR0401;DR0402;DR0403;DR0404;DR0405;DR0407;DR0901;DR0902;DR1;DR10;DR103;DR11;DR1101;DR1104;DR12;DR1201;DR1202;DR13;DR1301;DR1303;DR14;DR1401;DR1402;DR1403;DR1404;DR1454;DR15;DR1501;DR1502;DR1503;DR16;DR1601;DR1602;DR17;DR18;DR2;DR3;DR4;DR5;DR6;DR7;DR8;DR9"));
	}

	@Test
	public void testCpraAllOfDR51() throws Exception {
		log.info("Test Case Name: testCpraAllOfDR51");
		CpraDTO dto = this.testCurrentWithAntibodyList("DR5151");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.29290555518766864));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DR15;DR16;DR2;DR51;DR5151"));
	}

	@Test
	public void testCpraAllOfDR52() throws Exception {
		log.info("Test Case Name: testCpraAllOfDR52");
		CpraDTO dto = this.testCurrentWithAntibodyList("DR5252");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.622892362816549));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DR11;DR12;DR13;DR14;DR17;DR18;DR3;DR5;DR52;DR5252;DR6"));
	}

	@Test
	public void testCpraAllOfDR53() throws Exception {
		log.info("Test Case Name: testCpraAllOfDR53");
		CpraDTO dto = this.testCurrentWithAntibodyList("DR5353");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.5001288721489336));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DR4;DR53;DR5353;DR7;DR9"));
	}

	@Test
	public void testCpraAllOfDQB1() throws Exception {
		log.info("Test Case Name: testCpraAllOfDQB1");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQB11;DQB12;DQB10201;DQB10202;DQB13;DQB10301;DQB10302;DQB10303;DQB10319;DQB14;DQB10401;DQB10402;DQB15;DQB10501;DQB10502;DQB16;DQB10601;DQB10602;DQB10603;DQB10604;DQB10609;DQB17;DQB18;DQB19");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9999999832353699));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("DQ1;DQ2;DQ3;DQ4;DQ5;DQ6;DQ7;DQ8;DQ9;DQB10201;DQB10202;DQB10301;DQB10302;DQB10303;DQB10319;DQB10401;DQB10402;DQB10501;DQB10502;DQB10601;DQB10602;DQB10603;DQB10604;DQB10609;DQB11;DQB12;DQB13;DQB14;DQB15;DQB16;DQB17;DQB18;DQB19"));
	}

	@Test(expected = ConstraintViolationException.class)
	public void testCpraAntibodyWithSpace() throws Exception {
		log.info("Test Case Name: testCpraAntibodyWithSpace");
		this.testCurrentWithAntibodyList(" A1");
	}

	@Test(expected = ConstraintViolationException.class)
	public void testCpraAntibodyWithSpecialCharacter() throws Exception {
		log.info("Test Case Name: testCpraAntibodyWithSpecialCharacter");
		this.testCurrentWithAntibodyList("A&1");
	}
	
	@Test(expected = CpraRuntimeException.class)
	public void testCpraAntibodyInvalidLocus() throws Exception {
		log.info("Test Case Name: testCpraAntibodyInvalidLocus");
		this.testCurrentWithAntibodyList("XA1");
	}

	/*
	 * This test is from real data for a typed patient. It results in a 99.2% cPRA from the app and the OPTN Calculator (99.25%). Make sure it returns the same value
	 */
	@Test
	public void testHighRefractoryA() throws Exception {
		log.info("Test Case Name: testHighRefractoryA");
		CpraDTO dto = this.testCurrentWithAntibodyList("A1;A11;A2;A24;A29;A3;A30;A31;A32;A34;A36;A68;A69;A74;A8;B13;B41;B42;B48;B55;B56;B57;B58;B60;B61;B63;B67;B7;B73;B8;B81");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.9924621400849223));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A1;A11;A1101;A1102;A2;A203;A24;A2402;A2403;A29;A2901;A2902;A3;A30;A3001;A3002;A31;A32;A34;A3401;A3402;A36;A68;A6801;A6802;A69;A74;A8;B0702;B0801;B0802;B0803;B0804;B13;B1301;B1302;B1516;B4002;B4006;B41;B42;B48;B55;B56;B57;B5701;B5703;B58;B60;B61;B63;B67;B7;B73;B8;B81"));
	}

	/*
	 * This test is from real data for a typed patient. It results in a 84.1% cPRA from the app and the OPTN Calculator (84.12%). Make sure it returns the same value
	 */
	@Test
	public void testHighRefractoryB() throws Exception {
		log.info("Test Case Name: testHighRefractoryB");
		CpraDTO dto = this.testCurrentWithAntibodyList("A2;A24;A68;A69;B35;B49;B50;B51;B52;B53;B56;B57;B62;B63;B71;B72;B75");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.8411811326874266));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A0201;A0202;A0203;A0205;A0206;A2;A203;A24;A2402;A2403;A68;A6801;A6802;A69;B1501;B1502;B1503;B1510;B1511;B1516;B35;B4005;B49;B50;B51;B5101;B5102;B52;B53;B56;B57;B5701;B5703;B62;B63;B71;B72;B75"));
	}



	/*
	 * This test is from real data for a typed patient. It results in a 42.2% cPRA from the app and the OPTN Calculator (42.25%). Make sure it returns the same value
	 */
	@Test
	public void testMedRefractory() throws Exception {
		log.info("Test Case Name: testMedRefractory");
		CpraDTO dto = this.testCurrentWithAntibodyList("A23;A24;A25;A33;A34;A66;A68;B63");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.4224858052941756));
		assertThatWrapper("\nExpected value for dto.getUnacceptableAntigenList() is <\"{}\">.\nResult is <{}>", dto.getUnacceptableAntigenList(), equalTo("A23;A24;A2402;A2403;A25;A33;A3301;A3303;A34;A3401;A3402;A66;A6601;A6602;A68;A6801;A6802;B1516;B63"));
	}


	// This really tests the naming HLA Naming Convention. There should only be alphanumerics and asterisk(s), colon(s), or dash(es)
	// http://hla.alleles.org/nomenclature/naming.html - except we don't start with the HLA- prefix. So for now, do not include
	// the dash.
	@Test
	public void testCpraAntibodyNamingConvention() throws Exception {
		log.info("Test Case Name: testCpraAntibodyNamingConvention");
		CpraDTO dto = this.testCurrentWithAntibodyList("DQA1*01:01");
		assertThatWrapper("\nExpected value for dto.getCalculatedPRA() is <{}>.\nResult is {}", dto.getCalculatedPRA(), equalTo(0.0));
	}
	

}
