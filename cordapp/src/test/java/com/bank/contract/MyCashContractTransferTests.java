package com.bank.contract;

import com.bank.MyCashContract;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static com.bank.MyCashContract.ID;
import static net.corda.testing.node.NodeTestUtils.ledger;


public class MyCashContractTransferTests extends MyCashContractUnitTests {


    /* ---------Transfer command is needed ----------------------------------------------*/
    @Test
    public void transactionMustIncludeTransferCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, oneDollarCash);
                tx.output(ID, oneDollarCash.withNewOwner(charlie.getParty()));
                tx.fails();
                tx.command(ImmutableList.of(alice.getPublicKey(), charlie.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.verifies();
                return null;
            });
            return null;
        }));
    }

    /* ---------Transfer should have at least one input and one output ------------------*/

    @Test
    public void mustHaveAtleastOneInputOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(ID, oneDollarCash.withNewOwner(charlie.getParty()));
                tx.output(ID, oneDollarCash.withNewOwner(charlie.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("A cash transfer transaction should consume at least one input state.");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(ID, oneDollarCash);
                tx.input(ID, oneDollarCash);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("A cash transfer transaction should create at least one output state.");
                return null;
            });
            return null;
        }));
    }

    /* ---------Transfer command should have input cash more than 0 ------------------------*/
    @Test
    public void totalInputAmountShouldBeMoreThan0() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, ownerZeroDollarCash);
                tx.output(ID, tenDollarCash);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("In a cash transfer sum of inputs should be greater than 0");
                return null;
            });
            return null;
        }));
    }

    /* ---------Transfer command should have output cash more than 0 ------------------------*/

    @Test
    public void totalOutputAmountShouldBeMoreThan0() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, oneDollarCash);
                tx.output(ID, zeroDollarCash);
                tx.command(ImmutableList.of(oneDollarCash.getOwner().getOwningKey(), zeroDollarCash.getOwner().getOwningKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("In a cash transfer sum of outputs should be greater than 0");
                return null;
            });
            return null;
        }));
    }

    /* ---------Transfer command input cash should sum to output cash sum-----------------------*/
    @Test
    public void totalInputAmountShouldBeEqualToOutputAmount() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, oneDollarCash);
                tx.input(ID, oneDollarCash);
                tx.output(ID, oneDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, oneDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, oneDollarCash.withNewOwner(alice.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("In a cash transfer transaction sum of cash inputs should be equal to sum of cash outputs");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(ID, oneDollarCash);
                tx.input(ID, oneDollarCash);
                tx.output(ID, oneDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, oneDollarCash.withNewOwner(bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.verifies();
                return null;
            });

            return null;

        }));
    }

    //partial transfer of cash from old to new owner;
    //for .e.g current owner has $(5,3,2)
    //transfer amount is $9
    //so output states are $9 to new owner and $1 to old owner
    // and add all the $(5,3,2) input states.
    @Test
    public void totalInputAmountMoreThanTransferAmount() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, fiveDollarCash);
                tx.input(ID, threeDollarCash);
                tx.input(ID, twoDollarCash);
                tx.output(ID, fiveDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, threeDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, oneDollarCash.withNewOwner(bob.getParty()));
                //tx.output(ID, oneDollarCash);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("In a cash transfer transaction sum of cash inputs should be equal to sum of cash outputs");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(ID, fiveDollarCash);
                tx.input(ID, threeDollarCash);
                tx.input(ID, twoDollarCash);
                tx.output(ID, fiveDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, threeDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, oneDollarCash.withNewOwner(bob.getParty()));
                tx.output(ID, oneDollarCash);
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.verifies();
                return null;
            });
            return null;

        }));
    }


    @Test
    public void newOwnerShouldSignTheTransfer() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, fiveDollarCash);
                tx.output(ID, fiveDollarCash.withNewOwner(bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("The New  owner must sign MyCashState transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(ID, fiveDollarCash);
                tx.output(ID, fiveDollarCash.withNewOwner(bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.verifies();
                return null;
            });
            return null;

        }));
    }
    @Test
    public void oldOwnerShouldSignTheTransfer() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, fiveDollarCash);
                tx.output(ID, fiveDollarCash.withNewOwner(bob.getParty()));
                tx.command(ImmutableList.of(bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("The old  owner must sign MyCashState transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(ID, fiveDollarCash);
                tx.output(ID, fiveDollarCash.withNewOwner(bob.getParty()));
                tx.command(ImmutableList.of(alice.getPublicKey(), bob.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.verifies();
                return null;
            });
            return null;

        }));
    }

    @Test
    public void transferFromBankToConsumer() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, hundredDollarCash);
                tx.output(ID, fiftyDollarCash.withNewOwner(charlie.getParty()));
                tx.output(ID, fiftyDollarCashWithBank);
                tx.command(ImmutableList.of(charlie.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.failsWith("The old  owner must sign MyCashState transfer transaction");
                return null;
            });
            ledger.transaction(tx -> {
                tx.input(ID, hundredDollarCash);
                tx.output(ID, fiftyDollarCash.withNewOwner(charlie.getParty()));
                tx.output(ID, fiftyDollarCashWithBank);
                tx.command(ImmutableList.of(bank.getPublicKey(), charlie.getPublicKey()), new MyCashContract.Commands.TransferCash());
                tx.verifies();
                return null;
            });
            return null;

        }));
    }

}