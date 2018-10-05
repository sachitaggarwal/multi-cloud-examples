package com.sap.refapps.espm.controller;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.sap.refapps.espm.model.SalesOrder;
import com.sap.refapps.espm.model.Tax;
import com.sap.refapps.espm.service.SalesOrderService;

@RestController
@RequestMapping("sale.svc/api/v1/salesOrders")
public class SalesOrderController {

	protected static final String V1_PATH = "/v1/salesOrders";

	@Autowired
	private SalesOrderService salesOrderService;

	@PostMapping
	public ResponseEntity<String> createSalesOrder(@RequestBody final SalesOrder salesOrder)
			throws HystrixRuntimeException {

		salesOrder.setSalesOrderId(UUID.randomUUID().toString());
		final Tax tax = salesOrderService.getTax(salesOrder.getGrossAmount());
		salesOrderService.insert(salesOrder, tax);
		return new ResponseEntity<>("Sales order Created", HttpStatus.ACCEPTED);
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/email/{customerEmail}")
	public ResponseEntity<Iterable<SalesOrder>> getSalesOrdersByCustomerEmail(
			@PathVariable("customerEmail") final String customerEmail) {

		final Iterable<SalesOrder> salesOrders = salesOrderService.getByEmail(customerEmail);
		if (salesOrders != null)
			return new ResponseEntity<>(salesOrders, HttpStatus.OK);
		return errorMessage("Sales order not found", HttpStatus.NOT_FOUND);
	}

	@SuppressWarnings("unchecked")
	@GetMapping("/{salesOrderId}")
	public ResponseEntity<SalesOrder> getSalesOrderById(@PathVariable("salesOrderId") final String salesOrderId) {

		final SalesOrder salesOrders = salesOrderService.getById(salesOrderId);
		if (salesOrders != null)
			return new ResponseEntity<>(salesOrders, HttpStatus.OK);
		return errorMessage("Sales order not found", HttpStatus.NOT_FOUND);
	}

	@GetMapping
	public ResponseEntity<Iterable<SalesOrder>> getAllSalesOrders() {
		final Iterable<SalesOrder> salesOrders = salesOrderService.getAll();
		return new ResponseEntity<>(salesOrders, HttpStatus.OK);

	}

	@PutMapping("/{salesOrderId}")
	public ResponseEntity<?> updateSalesOrder(@RequestBody final SalesOrder salesOrder,
			@PathVariable("salesOrderId") final String salesOrderId) {
		if (!salesOrderService.exists(salesOrderId))
			return errorMessage("Invalid sales order id : " + salesOrderId, HttpStatus.BAD_REQUEST);
		salesOrderService.save(salesOrder);
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);
		return new ResponseEntity<>("Sales Order updated for id : " + salesOrderId, httpHeaders, HttpStatus.OK);
	}

	@SuppressWarnings("unchecked")
	@DeleteMapping("/{salesOrderId}")
	public ResponseEntity<SalesOrder> deleteSalesOrderById(@PathVariable("salesOrderId") final String salesOrderId) {

		final SalesOrder salesOrder = salesOrderService.getById(salesOrderId);
		if (salesOrder != null) {
			boolean response = salesOrderService.delete(salesOrderId);
			if(response) {
				return new ResponseEntity<>(salesOrder, HttpStatus.OK);
			}else {
				return new ResponseEntity<>(salesOrder, HttpStatus.BAD_REQUEST);
			}
		}
		return errorMessage("Sales order not found", HttpStatus.NOT_FOUND);
	}
	
	/**
	 * @param message
	 * @param status
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private ResponseEntity errorMessage(String message, HttpStatus status) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(org.springframework.http.MediaType.TEXT_PLAIN);
		return ResponseEntity.status(status).headers(headers).body(message);
	}

}
