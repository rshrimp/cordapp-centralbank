package com.bank.contract;

import com.bank.MyCashState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;

import java.util.List;

import static net.corda.finance.Currencies.DOLLARS;


/**
 * A base class to reduce the boilerplate when writing obligation contract tests.
 */
abstract class MyCashContractUnitTests {
    protected MockServices ledgerServices = new MockServices(
          ImmutableList.of("com.bank", "net.corda.testing.contracts"));



    protected TestIdentity bank = new TestIdentity(new CordaX500Name("Bank", "", "US"));
    protected TestIdentity alice = new TestIdentity(new CordaX500Name("Alice", "", "US"));
    protected TestIdentity bob = new TestIdentity(new CordaX500Name("Bob", "", "US"));
    protected TestIdentity charlie = new TestIdentity(new CordaX500Name("Charlie", "", "US"));


    //bank issues cash
    protected MyCashState hundredDollarCash = new MyCashState(DOLLARS(100), bank.getParty(), bank.getParty());

    protected MyCashState zeroDollarCash = new MyCashState(DOLLARS(0), bank.getParty(), bank.getParty());

    protected MyCashState ownerZeroDollarCash = new MyCashState(DOLLARS(0), bank.getParty(), alice.getParty());
    protected MyCashState oneDollarCash = new MyCashState(DOLLARS(1), bank.getParty(), alice.getParty());
    protected MyCashState tenDollarCash = new MyCashState(DOLLARS(10), bank.getParty(), bob.getParty());

    protected MyCashState fiveDollarCash = new MyCashState(DOLLARS(5), bank.getParty(), alice.getParty());
    protected MyCashState threeDollarCash = new MyCashState(DOLLARS(3), bank.getParty(), alice.getParty());
    protected MyCashState twoDollarCash = new MyCashState(DOLLARS(2), bank.getParty(), alice.getParty());
    protected MyCashState fiftyDollarCash = new MyCashState(DOLLARS(50), bank.getParty(), charlie.getParty());
    protected MyCashState fiftyDollarCashWithBank = new MyCashState(DOLLARS(50), bank.getParty(), charlie.getParty());


}

class DummyState implements ContractState {
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }
}

class DummyCommand implements CommandData {}