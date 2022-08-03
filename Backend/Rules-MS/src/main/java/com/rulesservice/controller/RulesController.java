package com.rulesservice.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.rulesservice.exception.MinimumBalanceException;
import com.rulesservice.feign.AccountFeign;
import com.rulesservice.model.Account;
import com.rulesservice.model.AccountInput;
import com.rulesservice.model.RulesInput;
import com.rulesservice.service.RulesService;

import lombok.extern.slf4j.Slf4j;

@RestController
@CrossOrigin()
@Slf4j
public class RulesController {

	private static final String INVALID = "Send Valid Details.";
	@Autowired
	public RulesService rulesService;
	@Autowired
	AccountFeign accountFeign;
	
	@GetMapping("/")
	public String home() {
		log.info("Home Check for Rules Microservice");
		log.info("health checkup ----->{}", "up");
		return "retail-bank-rules-ms";
	}

	/**
	 * The health method to check app
	 *
	 */
	@GetMapping("/health")
	public ResponseEntity<String> healthCheckup() {
		log.info("Health Check for rules Microservice");
		log.info("health checkup ----->{}", "up");
		return new ResponseEntity<>("UP. retail-bank-rules-ms", HttpStatus.OK);
	}

	

	@PostMapping("/evaluateMinBal")
	public ResponseEntity<?> evaluate(@RequestBody RulesInput account) throws MinimumBalanceException {
		if (account.getCurrentBalance() == 0) {
			throw new MinimumBalanceException(INVALID);
		} else {
			boolean status = rulesService.evaluate(account);

			return new ResponseEntity<Boolean>(status, HttpStatus.OK);
		}
	}

	@PostMapping("/serviceCharges")
	public ResponseEntity<?> serviceCharges(@RequestHeader("Authorization") String token) {
		rulesService.hasPermission(token);
		try {
			List<Account> body = accountFeign.getAllacc(token).getBody();
			for (Account acc : body) {
				if (rulesService.serviceCharges(acc) > 0) {
					accountFeign.servicecharge(token,
							new AccountInput(acc.getAccountId(), rulesService.serviceCharges(acc)));
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

		return ResponseEntity.ok(accountFeign.getAllacc(token).getBody());
	}

}
