package com.sap.refapps.espm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudException;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class SaleApplicationContextInitializer
		implements ApplicationContextInitializer<ConfigurableApplicationContext> {
	
	private static final Logger logger = LoggerFactory.getLogger(SaleApplicationContextInitializer.class);

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		ConfigurableEnvironment applicationEnvironment = applicationContext.getEnvironment();
		Cloud cloud = getCloud();
		if (cloud != null) {
			applicationEnvironment.setActiveProfiles("cloud");

		} else {
			applicationEnvironment.setActiveProfiles("local");
		}

	}

	private Cloud getCloud() {
		try {
			CloudFactory cloudFactory = new CloudFactory();
			return cloudFactory.getCloud();
		} catch (CloudException ce) {
			logger.error("no suitable cloud found");
			return null;
		}
	}

}
