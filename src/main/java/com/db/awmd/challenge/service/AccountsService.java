package com.db.awmd.challenge.service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.InValidAmountException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.repository.AccountsRepository;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AccountsService {

	@Getter
	private final AccountsRepository accountsRepository;

	@Getter
	private final NotificationService notificationService;

	private final Lock fromLock;
	private final Lock toLock;

	@Autowired
	public AccountsService(AccountsRepository accountsRepository, EmailNotificationService notificationService) {
		this.accountsRepository = accountsRepository;
		this.notificationService = notificationService;
		fromLock = new ReentrantLock();
		toLock = new ReentrantLock();
	}

	public void createAccount(Account account) {
		this.accountsRepository.createAccount(account);
	}

	public Account getAccount(String accountId) {
		return this.accountsRepository.getAccount(accountId);
	}

	/**
	 * This method takes fromAccountId, toAccountId and amount to transfer as input. If amount to
	 * transfer is zero or less than that then it throws InvalidAmountException, if
	 * amount to transfer is less than the balance in from account then it throws
	 * Insufficient balance exception. If due to some issues occured while
	 * transferring balance it will return false and notifies source and destination
	 * accounts with, appropriate error messages. if amount transfer is successful
	 * it returns true and notify the source and destination accounts with successs
	 * messages. This method uses ReentrantLock API from java concurrency package,
	 * it relies on tryLock(time,TimeUnit) method of ReentrantLock to avoid deadlock
	 * if more thread are simultaneously transferring amounts to and from between
	 * same accounts.
	 * 
	 * @param fromAccountId
	 * @param toAccountId
	 * @param amount
	 * @return
	 * @throws InValidAmountException
	 * @throws InsufficientBalanceException
	 */
	public boolean transferAmount(String fromAccountId, String toAccountId, BigDecimal amount)
			throws InValidAmountException, InsufficientBalanceException {
		validateTransferAmount(amount);
		boolean lockheld = false;
		try {
			lockheld = this.fromLock.tryLock();
			if (lockheld) {
				Account fromAccount = accountsRepository.getAccount(fromAccountId);
				validateTransfer(amount, fromAccount);
				boolean withdrawSuccess = withdraw(amount, fromAccount);
				boolean addSuccess = false;
				try {
					if (this.toLock.tryLock()) {
						addSuccess = addMoney(toAccountId, amount);
					}
				} finally {
					this.toLock.unlock();
				}
				Account toAccount = accountsRepository.getAccount(toAccountId);
				if (withdrawSuccess && addSuccess) {
					notificationService.notifyAboutTransfer(fromAccount,
							"Successfully transfered Rs. " + amount + " to " + toAccountId);
					notificationService.notifyAboutTransfer(toAccount,
							"Successfully recevied Rs. " + amount + " from " + fromAccountId);
					return true;
				} else {
					notificationService.notifyAboutTransfer(fromAccount,
							"Error!! occurred while transferring Rs. " + amount + " to " + toAccountId);
					notificationService.notifyAboutTransfer(toAccount,
							"Error!! occurred while receiving Rs. " + amount + " from " + fromAccountId);
					return false;
				}
			}
		} finally {
			if (lockheld)
				this.fromLock.unlock();
		}
		return false;
	}

	private void validateTransferAmount(BigDecimal amount) throws InValidAmountException {
		if (amount.compareTo(BigDecimal.ZERO) <= 0) {
			throw new InValidAmountException(
					"Amount provided should be positive integer, non positive integers are not allowed.");
		}
	}

	private void validateTransfer(BigDecimal amount, Account fromAccount) throws InsufficientBalanceException {
		if (fromAccount.getBalance().compareTo(amount) < 0) {
			log.info("{} not having sufficient balance", Thread.currentThread().getName());
			throw new InsufficientBalanceException("Insufficient balance.");
		}
	}

	private boolean addMoney(String toAccountId, BigDecimal amount) {
		try {
			log.info("{} Add thread currently sleeping", Thread.currentThread().getName());
			System.out.println(Thread.currentThread().getName() + " currently sleeping");
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Account toAccount = accountsRepository.getAccount(toAccountId);
		BigDecimal newBalance = toAccount.getBalance().add(amount);
		toAccount.setBalance(newBalance);
		log.info(" {} added money : {}  to account : {}", Thread.currentThread().getName(), newBalance, toAccountId);
		return true;
	}

	private boolean withdraw(BigDecimal amount, Account fromAccount) {
		try {
			log.info(" {} from thread currently sleeping", Thread.currentThread().getName());
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// log.error("Money withdrwan thread is interrupted", e);
			e.printStackTrace();
		}
		BigDecimal newAmount = fromAccount.getBalance().subtract(amount);
		fromAccount.setBalance(newAmount);
		log.info("{} money withdrawn", Thread.currentThread().getName());
		// System.out.println(Thread.currentThread().getName()+" money withdrawn");
		return true;
	}
}
