package com.bank.contract;

import com.bank.MyCashContract;
import com.google.common.collect.ImmutableList;
import org.junit.Test;

import static com.bank.MyCashContract.ID;
import static net.corda.testing.node.NodeTestUtils.ledger;


public class MyCashContractDestroyTests extends MyCashContractUnitTests {



    @Test
    public void destroyMyCashTransactionMustHaveNoOutputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, oneDollarCash);
                tx.command(ImmutableList.of(alice.getPublicKey(), bank.getPublicKey()), new MyCashContract.Commands.DestroyCash());
                tx.output(ID, oneDollarCash);
                tx.failsWith("Destroy transaction should not create any outputs.");
                return null;
            });
            return null;
        }));
    }

    @Test
    public void destroyMyCashTransactionMustHaveOwnerAndBankSign() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.input(ID, oneDollarCash);
                tx.command(ImmutableList.of(bob.getPublicKey()), new MyCashContract.Commands.DestroyCash());
                tx.failsWith("The Owner of the cash and bank must sign  destroy transaction");
                return null;
            });
            return null;
        }));
    }


}
