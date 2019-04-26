# Calculated Panel Reactive Antibodies (CPRA) Web Service

[![License](http://img.shields.io/:license-MPL2-blue.svg)](https://www.github.com/persistentsystems/open-cpra-calculator/LICENSE)
[![Notice](https://img.shields.io/badge/Notice-Healthcare Disclaimer-red.svg)](https://www.github.com/persistentsystems/open-cpra-calculator/HD.txt)


An open-source [Spring Boot](http://projects.spring.io/spring-boot/)-based micro-service for calculating panel reactive antibodies using a list of patient antibodies. 

## Requirements

For building and running the application you need:

- [JDK 1.8](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org)

## Running the application locally

There are several ways to run the CPRA application on your local machine or server. One way is to execute the `main` method in the `org.partners.ppm.cpra.CpraApplication` class from your IDE.

Alternatively you can use the [Spring Boot Maven plugin](https://docs.spring.io/spring-boot/docs/current/reference/html/build-tool-plugins-maven-plugin.html) like so:

```shell
mvn spring-boot:run
```
To test the application:

```shell
mvn test
```
## Deploying the application to Docker

To cleanly compile, package, build and create a docker image of the application

```shell
./mvnw clean compile package install --update-snapshots docker:build -DskipTests
```

To run the application in Docker

```shell
./start.sh
```
or

```shell
docker-compose up -d
```


## Deploying the application to OpenShift

The easiest way to deploy the sample application to OpenShift is to use the [OpenShift CLI](https://docs.openshift.org/latest/cli_reference/index.html):

```shell
oc new-app codecentric/springboot-maven3-centos~https://github.com/persistentsystems/open-cpra-calculator
```

This will create:

* An ImageStream called "springboot-maven3-centos"
* An ImageStream called "open-cpra-calculator"
* A BuildConfig called "open-cpra-calculator"
* DeploymentConfig called "open-cpra-calculator"
* Service called "open-cpra-calculator"

If you want to access the app from outside your OpenShift installation, you have to expose the open-cpra-calculator service:

```shell
oc expose open-cpra-calculator --hostname=www.example.com
```
## Using the service with swagger

The calculator supports swagger for calling the api.
	
	http://localhost:8080/swagger-ui.html
 
The REST apis can also be called directly.

	http://localhost:8080/api/cpra/optn_2015/calculate?antibodyList=A9
 
 Returns the cpra using the 2015 OPTN frequency set for a patient with antibody A9:

 ```
 {
  "version": "optn_2015",
  "calculatedPRA": 0.23667027217061132,
  "antibodyList": [
    "A9"
  ],
  "unacceptableAntigenList": "A23;A24;A2402;A2403;A9",
  "ethnicCalculatedPRA": [
    {
      "ethnicity": "Caucasian",
      "calculatedPRA": 0.21382163418387967
    },
    {
      "ethnicity": "African American",
      "calculatedPRA": 0.24743334120973437
    },
    {
      "ethnicity": "Hispanic",
      "calculatedPRA": 0.30516605128048035
    },
    {
      "ethnicity": "Asian",
      "calculatedPRA": 0.42449417237435005
    }
  ],
  "warnings": [
    "No S1 haplotype frequencies found for HLA-A2402",
    "No S1 haplotype frequencies found for HLA-A9"
  ]
}
```
The calculator can provide information about its configured methods:

	http://localhost:8080/api/cpra/versions
	
Returns information about the configured and supported calculator versions that can be used in the api:

```
{
  "versions": [
    {
      "name": "optn_2015",
      "isDefault": true,
      "description": "A local implementation of the CPRA calculation using the datasets and algorithm published by the Organ Procurement and Transplantation Network (OPTN) and United Network for Organ Sharing (UNOS) in Sept 2015.",
      "calculator": "haplotype"
    },
    {
      "name": "bwh_2017",
      "isDefault": false,
      "description": "A local implementation of the CPRA calculation using frequency data from BWH Blood Bank and the diplotype frequency algorithm (basically counting allele exposure).",
      "calculator": "diplotype"
    }
  ]
}
```

## Configuring the CPRA service with allele frequency and other setup data

The Cpra service calculators are configured in a file specified by the cpra.config-path property in src/main/resources/application.properties:

```
cpra.config-path=config_example.csv
```
By default the config file is "config_example.csv", located in src/main/resources/config_example.csv. The configuration file is a set of key/value pairs.  Valid keys include:

	hlaCpraVersions
	hlaCpraCalculatorType
	hlaCpraCalculatorDescription
	hlaEthnicities
	hlaEthnicFrequencies
	hlaAlleles
	hlaAllelesWithFrequencies
	hlaUnacceptableAntigenEquivalences
	hlaDiplotypeFrequencies
	hlaHaplotypeFrequencies

 hlaCpraVersions 

	contains a semi-colon separated list of valid calcuator versions

	a calculator version is a meta data set with all necessary settings for calculating CPRA from a list of antibodies

 hlaCpraCalculatorType:&lt;version&gt;

	set the calculator type to be used for this version.  Supported types are "haplotype" and "diplotype". 

	***haplotype***
		- uses frequency data for multiple ethnicities for all haplotype combinations of alleles in 1, 2, 3, 4, and 5 allele combinations (e.g. A, B, C, AB, BC, AC, ABC, ...
		- calculator is UNOS/OPTN standard, cpra is 1 minus the probability of a cross match, using haplotypes combos
		- CPRA for each ethnicity is:
			probability of a positive crossmatch =
			1 – probability of a negative crossmatch =
			1 – (1 – S1 + S2 – S3 + S4 – S5)^2
		- where S1 is single allele prob, S2 is dual allele prob, S3 is triple allele prob, etc.
		- then average based on ethnic composition of the cohort (provided as percentage distribution in the dataset
			0 Caucasian, 1 African American, 2 Hispanic, 3 Asian

	***diplotype***
		- base cohort frequency dataset is the set of diplotypes with frequency of each combination in the population
		-  calculator then finds unique set of all matching diplotypes and adds the percentages of each
		-  this provides an "exposure" percentage which is the cPRA for this method

 hlaCpraCalculatorDescription:&lt;version&gt;

	a description of the specified version dataset and calculator combination

 hlaEthnicities:&lt;version&gt;

	list of ethnic population names associated with frequency value sets in haplotype data

	for "haplotype" calculator, set to a semi-colon separated list, in correct order, of the ethnic frequence data provided for haplotype data
	the number and order of the names of ethnic frequencies must match the order and number of frequencies provided for each haplotype entry

	for "diplotype" calculator, ignored, but can be set to "Default" or something like that.  this calculator does not yet support ethnic frequencies

 hlaEthnicFrequencies:&lt;version&gt;

	the list of percentage contribution of each ethnicity in the dataset

	for "haplotype", this is a semi-colon list of % contribution of each represented ethnicity in the dataset
	must be in the same order as the hlaEthnicities and the frequency values for each haplotype entry

	for "diplotype" calculator, ignored, but can be set to "1.0" 

 hlaAlleles:&lt;version&gt;

	the semi-colon list of "reportable" alelles supported by the calculator

 hlaAllelesWithFrequencies:&lt;version&gt;

	the semi-colon delimited list of "reportable" alelles supported by the calculator having frequency values in the haplotype or diplotype frequency sets

	this is an optimization to reduce computation where certain alleles have no frequency data, but do have unacceptable antigens, so may point to other antigens that do have frequencies

 hlaUnacceptableAntigenEquivalences:&lt;version&gt;:&lt;antibody&gt;

	a semi-colon delimited list of antigens deemed equivalent in terms of cross-reactivity with a given antibody

	NOTE: this value can also be used to "map" antigens for nomenclature equivalence purposes, for things like name changes

 hlaHaplotypeFrequencies:&lt;version&gt;:&lt;antigen&gt;[;&lt;antigen&gt;...] -- 1 antigen is S1, 2 antigens for S2, etc., upto 5 antigens at different alleles

	key is semi-colon delimited list of haplotype combinations for each combination level (S1, S2, S3, S4, S5) for example A2, or A2;B7, or A2;B7;C3

	value is a semi-colon delimited list of frequencies, one for each ethnic population in the dataset, in the specfied order in metadata hlaEthnicities

 hlaDiplotypeFrequencies:&lt;version&gt;:&lt;A1 antigen&gt;;&lt;B1 antigen&gt;;&lt;A2 antigen&gt;;&lt;B2 antigen&gt; -- the A &amp; B antigens of the diplotype
 
	key is the semi-colon delimited diplotype combination, such as A7;B4;A9;B57 or A2;B57;A13;B8
	the order of the alleles is not particularly important

	value is the frequency of this diplotype in the population
	all the values in the population should add to 1.0

## Notes

 1. What happens when antigen nomenclature or names change, for example A203 now should be A0203?
 
    If there is an antigen name change, you should add a new unacceptable antigen equivalent for that value mapping it to the original name,
    and make a copy of the antigen equivalent using the new name as the primary value and the old primary as part of the equivalent list.
    For example, if A203 has been renamed to A0203 in the standard, you would create an equivalent to allow reporting on the new value
        A0203 = A203
       "hlaUnacceptableAntigenEquivalences:optn_2015:A0203", "A203"
    The algorithm will then be able to use the A203 frequency data when reporting as A0203.  If A203 is reported, that will still work because
    the frequency table will remain the same.


## License and Copyright

MPL 2.0 w/ HD  
See [LICENSE](https://www.github.com/persistentsystems/open-cpra-calculator/LICENSE) file.  
See [HEALTHCARE DISCLAIMER](https://www.github.com/persistentsystems/open-cpra-calculator/HD.txt) file.  
© Persistent Systems, Inc. (https://www.github.com/persistentsystems/open-cpra-calculator)
