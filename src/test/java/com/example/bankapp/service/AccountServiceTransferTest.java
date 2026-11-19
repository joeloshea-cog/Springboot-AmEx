package com.example.bankapp.service;

import com.example.bankapp.model.Account;
import com.example.bankapp.repository.AccountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountServiceTransferTest {

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void negativeTransferAmountIsRejectedAndBalancesUnchanged() {
        Account sender = accountService.registerAccount("neg-sender", "pw");
        Account recipient = accountService.registerAccount("neg-recipient", "pw");
        recipient.setBalance(new BigDecimal("200"));
        accountRepository.save(recipient);

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "neg-recipient", new BigDecimal("-150")));

        assertEquals(0, accountService.findAccountByUsername("neg-sender").getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(0, accountService.findAccountByUsername("neg-recipient").getBalance().compareTo(new BigDecimal("200")));
    }

    @Test
    void zeroTransferAmountIsRejected() {
        Account sender = accountService.registerAccount("zero-sender", "pw");
        accountService.registerAccount("zero-recipient", "pw");

        assertThrows(RuntimeException.class,
                () -> accountService.transferAmount(sender, "zero-recipient", BigDecimal.ZERO));
    }

    @Test
    void positiveTransferMovesFunds() {
        Account sender = accountService.registerAccount("pos-sender", "pw");
        sender.setBalance(new BigDecimal("100"));
        accountRepository.save(sender);
        accountService.registerAccount("pos-recipient", "pw");

        accountService.transferAmount(sender, "pos-recipient", new BigDecimal("40"));

        assertEquals(0, accountService.findAccountByUsername("pos-sender").getBalance().compareTo(new BigDecimal("60")));
        assertEquals(0, accountService.findAccountByUsername("pos-recipient").getBalance().compareTo(new BigDecimal("40")));
    }
}
