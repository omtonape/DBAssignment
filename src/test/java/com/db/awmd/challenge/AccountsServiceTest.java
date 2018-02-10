package com.db.awmd.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.exception.DuplicateAccountIdException;
import com.db.awmd.challenge.exception.InValidAmountException;
import com.db.awmd.challenge.exception.InsufficientBalanceException;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }
  
  @Test
  public void transerMoney_ZeroAmount() {
	  Account account = new Account("Id-1");
	  account.setBalance(new BigDecimal(0));
	  this.accountsService.createAccount(account);
	  
	  Account account2 = new Account("Id-2");
	  account.setBalance(new BigDecimal(10));
	  this.accountsService.createAccount(account2);
	  try {
		this.accountsService.transferAmount("Id-1", "Id-2", BigDecimal.ZERO);
		assertTrue("For zero amount transfer method should throw InvalidAmountException", false);
	} catch (InValidAmountException e) {
		assertTrue("For zero amount transfer method should throw InvalidAmountException", true);
	} catch (InsufficientBalanceException e) {
		assertTrue("For zero amount transfer method should throw InsufficientBalanceException", false);
	}
  }
  @Test
  public void transferMoney_NegativeAmount() {
	  Account account = new Account("Id-3");
	  account.setBalance(new BigDecimal(0));
	  this.accountsService.createAccount(account);
	  
	  Account account2 = new Account("Id-4");
	  account2.setBalance(new BigDecimal(10));
	  this.accountsService.createAccount(account2);
	  try {
		this.accountsService.transferAmount("Id-3", "Id-4", new BigDecimal(-1000));
		assertTrue("For negative amount transfer method should throw InvalidAmountException", false);
	} catch (InValidAmountException e) {
		assertTrue("For negative amount transfer method should throw InvalidAmountException", true);
	} catch (InsufficientBalanceException e) {
		assertTrue("For negative amount transfer method should throw InsufficientBalanceException", false);
	}
  }
  @Test
  public void transferMoney_TwoThreads() {
	  Account account = new Account("A");
	  account.setBalance(new BigDecimal(100));
	  this.accountsService.createAccount(account);
	  
	  Account account2 = new Account("B");
	  account2.setBalance(new BigDecimal(100));
	  this.accountsService.createAccount(account2);
	  Thread thread1 = new Thread( new Runnable() {
		@Override
		public void run() {
			System.out.println("Thread-1 Transfering Rs. 30 from A to B");
			try {
				 accountsService.transferAmount("A", "B", new BigDecimal(30));
			} catch (InValidAmountException  e) {
				assertTrue("For positive amount invalidAmountException should not be thrown", false);
				e.printStackTrace();
			}catch( InsufficientBalanceException w) {
				assertTrue("Another thread successdes over current thread : "+Thread.currentThread().getName()+"",true);
				w.printStackTrace();
			}
		}
	}, "Thread-1");
	  
	  Thread thread2 = new Thread( new Runnable() {
			@Override
			public void run() {
				System.out.println("Thread-2 Transfering Rs. 80 from A to B");
				try {
					 accountsService.transferAmount("A", "B", new BigDecimal(80));
				} catch (InValidAmountException  e) {
					assertTrue("For positive amount invalidAmountException should not be thrown", false);
					e.printStackTrace();
				}catch( InsufficientBalanceException w) {
					assertTrue("Another thread successdes over current thread : "+Thread.currentThread().getName()+"",true);
					w.printStackTrace();
				}
			}
		}, "Thread-2");
	  thread1.start();
	  thread2.start();
	  
	  try {
		thread1.join();
		thread2.join();
	} catch (InterruptedException e) {}
	  
	Account a = this.accountsService.getAccount("A");
	Account b = this.accountsService.getAccount("B");
	BigDecimal aBal = a.getBalance();
	BigDecimal bBal = b.getBalance();
	System.out.println("A's balance is :- "+aBal+" b's balance is :- "+bBal);
	if(aBal.compareTo(new BigDecimal(70)) == 0 ) {
		if(bBal.compareTo(new BigDecimal(130)) != 0) {
			System.out.println("If A's balance after deducting Rs.30 is 70 then B's balance must be 130");
			assertTrue("If A's balance after deducting Rs.30 is 70 then B's balance must be 130",false);
		}
	}
	if(aBal.compareTo(new BigDecimal(20)) == 0 ) {
		if(bBal.compareTo(new BigDecimal(180)) != 0) {
			System.out.println("If A's balance after deducting Rs.80 is 20 then B's balance must be Rs.180");
			assertTrue("If A's balance after deducting Rs.80 is 20 then B's balance must be Rs.180", false);
		}
	}
	if(aBal.compareTo(new BigDecimal(-10)) == 0 || bBal.compareTo(new BigDecimal(210)) == 0) {
		System.out.println("Operation not successful, datarace occured");
		assertTrue("Operation not successful, datarace occured",false);
	}
  }

  
  @Test
  public void transferMoney_DeadLock() {
	  Account account = new Account("C");
	  account.setBalance(new BigDecimal(100));
	  this.accountsService.createAccount(account);
	  
	  Account account2 = new Account("D");
	  account2.setBalance(new BigDecimal(100));
	  this.accountsService.createAccount(account2);
	  Thread thread1 = new Thread( new Runnable() {
		@Override
		public void run() {
			System.out.println("Thread-1 Transfering Rs. 40 from C to D");
			try {
				boolean success= accountsService.transferAmount("C", "D", new BigDecimal(40));
				System.out.println("is thread1 successfull ? "+success);
			} catch (InValidAmountException  e) {
				assertTrue("For positive amount invalidAmountException should not be thrown", false);
				e.printStackTrace();
			}catch( InsufficientBalanceException w) {
				assertTrue("Another thread successdes over current thread : "+Thread.currentThread().getName()+"",true);
				w.printStackTrace();
			}
		}
	}, "Thread-1");
	  Thread thread2 = new Thread( new Runnable() {
			@Override
			public void run() {
				System.out.println("Thread-2 Transfering Rs. 60 from D to C");
				try {
					 boolean success =  accountsService.transferAmount("D", "C", new BigDecimal(60));
					 System.out.println("is thread 2 successful :- "+success);
				} catch (InValidAmountException  e) {
					assertTrue("For positive amount invalidAmountException should not be thrown", false);
					e.printStackTrace();
				}catch( InsufficientBalanceException w) {
					assertTrue("Another thread successdes over current thread : "+Thread.currentThread().getName()+"",true);
					w.printStackTrace();
				}
			}
		}, "Thread-2");
	  thread1.start();
	  thread2.start();
	  
	  try {
		thread1.join();
		thread2.join();
	} catch (InterruptedException e) {}
	  
	Account c = this.accountsService.getAccount("C");
	Account d = this.accountsService.getAccount("D");
	BigDecimal aBal = c.getBalance();
	BigDecimal bBal = d.getBalance();
	System.out.println("C's balance is :- "+aBal+" d's balance is :- "+bBal);
	if(aBal.compareTo(new BigDecimal(120)) == 0 ) {
		if(bBal.compareTo(new BigDecimal(80)) != 0) {
			System.out.println("Both Thread successful..If C's balance after deducting Rs.40 and adding 60 is 120 then D's balance must be 80");
			assertTrue("Both Thread successful.. If C's balance after deducting Rs.40 and adding 60 is 120 then D's balance must be 80", false);
		}
	}else if(aBal.compareTo(new BigDecimal(60)) == 0 ){
		if(bBal.compareTo(new BigDecimal(140)) != 0) {
			System.out.println("Thread-1 succeds Thread 2 fails so C's balance is :"+ 60+" and D's balance is :-"+140);
			assertTrue("Thread-1 succeds Thread 2 fails so C's balance is :"+ 60+" and D's balance is :-"+140, true);
		}
	}else if(aBal.compareTo(new BigDecimal(160)) == 0 ){
		if(bBal.compareTo(new BigDecimal(40)) != 0) {
			System.out.println("Thread-1 fails Thread 2 succeds so C's balance is :"+ 160+" and D's balance is :-"+40);
			assertTrue("Thread-1 fails Thread 2 succeds so C's balance is :"+ 160+" and D's balance is :-", true);
		}
	}
	else {
		System.out.println("C's balance after deducting Rs.40 and adding 60 must be Rs 120");
		assertTrue("C's balance after deducting Rs.40 and adding 60 must be Rs 120", false);
	}
  }

  @Test
  public void transferMoney_BothThreadsHappy() {
	  Account account = new Account("E");
	  account.setBalance(new BigDecimal(100));
	  this.accountsService.createAccount(account);
	  
	  Account account2 = new Account("F");
	  account2.setBalance(new BigDecimal(100));
	  this.accountsService.createAccount(account2);
	  Thread thread1 = new Thread( new Runnable() {
		@Override
		public void run() {
			System.out.println("Thread-1 Transfering Rs. 40 from E to F");
			try {
				boolean success= accountsService.transferAmount("E", "F", new BigDecimal(40));
				System.out.println("is thread1 successfull ? "+success);
			} catch (InValidAmountException  e) {
				assertTrue("For positive amount invalidAmountException should not be thrown", false);
				e.printStackTrace();
			}catch( InsufficientBalanceException w) {
				assertTrue("Another thread successdes over current thread : "+Thread.currentThread().getName()+"",true);
				w.printStackTrace();
			}
		}
	}, "Thread-1");
	  Thread thread2 = new Thread( new Runnable() {
			@Override
			public void run() {
				System.out.println("Thread-2 Transfering Rs. 30 from E to F");
				try {
					 boolean success =  accountsService.transferAmount("E", "F", new BigDecimal(30));
					 System.out.println("is thread 2 successful :- "+success);
				} catch (InValidAmountException  e) {
					assertTrue("For positive amount invalidAmountException should not be thrown", false);
					e.printStackTrace();
				}catch( InsufficientBalanceException w) {
					assertTrue("Another thread successdes over current thread : "+Thread.currentThread().getName()+"",true);
					w.printStackTrace();
				}
			}
		}, "Thread-2");
	  thread1.start();
	  thread2.start();
	  
	  try {
		thread1.join();
		thread2.join();
	} catch (InterruptedException e) {}
	  
	Account c = this.accountsService.getAccount("E");
	Account d = this.accountsService.getAccount("F");
	BigDecimal aBal = c.getBalance();
	BigDecimal bBal = d.getBalance();
	System.out.println("E's balance is : "+aBal+" F's balance is : "+bBal);
	if(aBal.compareTo(new BigDecimal(30)) == 0 ) {
		if(bBal.compareTo(new BigDecimal(170)) != 0) {
			assertTrue("Both Thread successful.. If E's balance after deducting Rs.40 and  30  then F's balance must be 170", false);
		}
	}
	else if(aBal.compareTo(new BigDecimal(60)) == 0 ) {
		if(bBal.compareTo(new BigDecimal(140)) != 0) {
			assertTrue("Thread-1 successful thread 2 not.. If E's balance after deducting Rs.40 is 60  then F's balance must be 160", false);
		}
	}else if(aBal.compareTo(new BigDecimal(70)) == 0 ) {
		if(bBal.compareTo(new BigDecimal(130)) != 0) {
			assertTrue("Thread-1 successful thread 2 not.. If E's balance after deducting Rs.30 is 70  then F's balance must be 160", false);
		}
	}
	else {
		assertTrue("After 2 consecutive deductions  of Rs 40 and Rs 30  A's balance must be Rs 30 and B's should be Rs 170", false);
	}
  }
  
  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }
}
