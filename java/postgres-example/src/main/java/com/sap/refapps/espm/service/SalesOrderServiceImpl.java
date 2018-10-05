package com.sap.refapps.espm.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixException;
import com.sap.refapps.espm.model.SalesOrder;
import com.sap.refapps.espm.model.SalesOrderRepository;
import com.sap.refapps.espm.model.Tax;

@Service
public class SalesOrderServiceImpl implements SalesOrderService {

	private final SalesOrderRepository salesOrderRepository;

	private final RestTemplate restTemplate;

	private static final Logger logger = LoggerFactory.getLogger(SalesOrderServiceImpl.class);

	@Value("${tax.service}")
	private String taxServiceEndPoint;

	private Iterable<SalesOrder> salesOrder;

	@Autowired
	public SalesOrderServiceImpl(final SalesOrderRepository salesOrderRepository,
			final RestTemplate rest) {
		this.salesOrderRepository = salesOrderRepository;
		this.restTemplate = rest;
	}

	@Override
	public SalesOrder save(SalesOrder salesOrder) {
		return salesOrderRepository.save(salesOrder);
	}

	@Override
	public boolean insert(final SalesOrder salesOrder, final Tax tax) {

		final BigDecimal netAmount;
		// create MathContext object with 4 precision
		MathContext mc = new MathContext(15);
		netAmount = tax.getTaxAmount().add(salesOrder.getGrossAmount(), mc);
		salesOrder.setNetAmount(netAmount);
		salesOrder.setTaxAmount(tax.getTaxAmount());
		salesOrderRepository.save(salesOrder);
		return true;

	}

	@Override
	public Iterable<SalesOrder> getAll() {
		salesOrder = salesOrderRepository.findAll();
		return salesOrder;
	}

	@Override
	public Iterable<SalesOrder> getByEmail(String customerEmail) {
		salesOrder = salesOrderRepository.getAllSalesOrderForCustomer(customerEmail);
		return salesOrder;
	}

	@Override
	public SalesOrder getById(String salesOrderId) {
		return salesOrderRepository.findSalesOrderById(salesOrderId);
	}

	@Override
	public boolean exists(String salesOrderId) {
		return salesOrderRepository.existsById(salesOrderId);
	}

	@HystrixCommand(fallbackMethod = "taxServiceFallback", raiseHystrixExceptions = HystrixException.RUNTIME_EXCEPTION, commandKey = "taxCommandKey", groupKey = "taxThreadPoolKey")
	public Tax getTax(BigDecimal amount) {

		logger.info("Tax service is called to calculate tax for amount : {}", amount);
		URI uri = URI.create(taxServiceEndPoint + amount);
		return this.restTemplate.getForObject(uri, Tax.class);
	}

	public Tax taxServiceFallback(BigDecimal amount) {
		logger.info("Tax service is down. So a default tax will be set to the amount : {}", amount);
		final Tax tax = new Tax();
		tax.setTaxPercentage(00.00);
		tax.setTaxAmount(new BigDecimal(00.00));

		return tax;
	}

	@Override
	public boolean delete(String salesOrderId) {
		SalesOrder salesOrder = salesOrderRepository.findSalesOrderById(salesOrderId);
		salesOrderRepository.delete(salesOrder);
		return true;
	}

}
