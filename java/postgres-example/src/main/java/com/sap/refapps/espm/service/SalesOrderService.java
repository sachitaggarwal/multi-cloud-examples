package com.sap.refapps.espm.service;

import java.math.BigDecimal;

import com.sap.refapps.espm.model.SalesOrder;
import com.sap.refapps.espm.model.Tax;

public interface SalesOrderService {

	Iterable<SalesOrder> getAll();

	SalesOrder getById(String salesOrderId);
	
	boolean delete(String salesOrderId);

	Iterable<SalesOrder> getByEmail(String customerEmail);

	boolean insert(SalesOrder salesOrder, Tax tax);

	boolean exists(String salesOrderId);

	SalesOrder save(SalesOrder salesOrder);

	Tax getTax(BigDecimal amount);

	Tax taxServiceFallback(BigDecimal amount);

}
