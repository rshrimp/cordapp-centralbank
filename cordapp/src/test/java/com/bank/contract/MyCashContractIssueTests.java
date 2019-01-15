package com.bank.contract;

import com.bank.MyCashContract;
import com.google.common.collect.ImmutableList;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.contracts.DummyState;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;

import static com.bank.MyCashContract.ID;
import static net.corda.testing.node.NodeTestUtils.ledger;




public class MyCashContractIssueTests extends MyCashContractUnitTests {

    @Test
    public void mustHandleMultipleCommandValues() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(ID, hundredDollarCash);
                tx.command(ImmutableList.of(bank.getPublicKey()), new DummyCommand());
                tx.failsWith("Required com.bank.MyCashContract.Commands command");
                return null;
            });
            return null;
        }));
    }
    
    
    @Test
    public void transactionMustIncludeCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(ID, hundredDollarCash);
                tx.fails();
                tx.command( ImmutableList.of(bank.getPublicKey()), new MyCashContract.Commands.VerifyIssuedCash());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    @Test
    public void issueMyCashTransactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, new DummyState());
                tx.command( ImmutableList.of(bank.getPublicKey()), new MyCashContract.Commands.VerifyIssuedCash());
                tx.output(ID, hundredDollarCash);
                tx.failsWith("No inputs should be consumed when issuing Cash.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.output(ID, hundredDollarCash);
                tx.output(ID, hundredDollarCash);
                tx.command(ImmutableList.of(bank.getPublicKey()), new MyCashContract.Commands.VerifyIssuedCash());
                tx.verifies(); // As there are no input states.
                return null;
            });
            return null;
        }));
    }


    @Test
    public void cannotIssueZeroValueCash() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(bank.getPublicKey()), new MyCashContract.Commands.VerifyIssuedCash());
                tx.output(ID, zeroDollarCash); // Zero amount fails.
                tx.failsWith("A newly issued cash must have a positive amount.");
                return null;
            });
        return null;
        }));
    }

    @Test
    public void nonbankEntityCantIssueCash() {
        TestIdentity fraudIdentity = new TestIdentity(new CordaX500Name("fraud", "", "GB"));
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(fraudIdentity.getPublicKey()), new MyCashContract.Commands.VerifyIssuedCash());
                tx.output(ID, hundredDollarCash);
                tx.fails();//fraudster issued cash
                return null;
            });
            return null;
        }));
    }
    
}