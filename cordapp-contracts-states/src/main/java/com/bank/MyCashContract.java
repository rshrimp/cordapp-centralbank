package com.bank;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;


import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


// ************
// * MyCashContract *
// ************
public class MyCashContract implements Contract {
    // This is used to identify our contract when building a transaction.
    public static final String ID = "com.bank.MyCashContract";


    /* -------------------------------------------------------------------------------------------------
       verify : A transaction is valid if the verify() function of the contract of all the transaction's
       input and output states does not throw an exception
     --------------------------------------------------------------------------------------------------- */
    @Override
    public void verify(LedgerTransaction tx) {

        // get the command to be processed
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());

        //check the command type and fork
        if (commandData instanceof Commands.VerifyIssuedCash) {
            verifyIssuedCash(tx, setOfSigners);
        } else if (commandData instanceof Commands.TransferCash) {
            transferCash(tx, setOfSigners);
        } else if (commandData instanceof Commands.DestroyCash) {
            destroyCash(tx, setOfSigners);
        } else {
            throw new IllegalArgumentException("Command has to be one of issue/transfer/destroy.");
        }
    }

    /* -----------------------------------------------------------------------
                    Processing of cash issuance
       ----------------------------------------------------------------------- */
    private void verifyIssuedCash(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            req.using("No inputs should be consumed when issuing Cash.", tx.getInputStates().isEmpty());

            //multiple cash states can be issued
            int numOfOutputStates = tx.getOutputStates().size();
            MyCashState cashOutputState = null;
            for (int i = 0; i < numOfOutputStates; ++i) {
                cashOutputState = (MyCashState) tx.getOutputStates().get(i);
                req.using("A newly issued cash must have a positive amount.", cashOutputState.getAmount().getQuantity() > 0);
                req.using("Only bank should sign cash issue transaction.", signers.contains(cashOutputState.getBank().getOwningKey()));
            }
            return null;
        });
    }

    /* -----------------------------------------------------------------------
                    Processing of transferring/moving cash
       ----------------------------------------------------------------------- */
    private void transferCash(LedgerTransaction tx, Set<PublicKey> signers) {

        List<MyCashState> cashInputs = tx.inputsOfType(MyCashState.class);
        List<MyCashState> cashOutputs = tx.outputsOfType(MyCashState.class);

        requireThat(req -> {
            req.using("A cash transfer transaction should consume at least one input state.", !tx.getInputStates().isEmpty());
            req.using("A cash transfer transaction should create at least one output state.", !tx.getOutputStates().isEmpty());


            Amount<Currency> sumOfInputCash  = net.corda.finance.Currencies.DOLLARS(0);//we are defaulting to USD
            Amount<Currency> sumOfOutputCash = net.corda.finance.Currencies.DOLLARS(0);//we are defaulting to USD

            Set<PublicKey> inputOwnerKeys = new HashSet<>();
            Set<PublicKey> outputOwnerKeys = new HashSet<>();




            for (int i = 0; i < cashInputs.size(); ++i) {
                sumOfInputCash = sumOfInputCash.plus(cashInputs.get(i).getAmount());
                inputOwnerKeys.add(cashInputs.get(i).getOwner().getOwningKey());
            }
            for (int i = 0; i < cashOutputs.size(); ++i) {
                sumOfOutputCash = sumOfOutputCash.plus(cashOutputs.get(i).getAmount());
                outputOwnerKeys.add(cashOutputs.get(i).getOwner().getOwningKey());
            }


            req.using("In a cash transfer sum of inputs should be greater than 0", sumOfInputCash.toDecimal().intValue() > 0);
            req.using("In a cash transfer sum of outputs should be greater than 0", sumOfOutputCash.toDecimal().intValue() > 0);

            req.using("In a cash transfer transaction sum of cash inputs should be equal to sum of cash outputs",
                    sumOfInputCash.compareTo(sumOfOutputCash) == 0);


            req.using("The old  owner must sign MyCashState transfer transaction",
                    signers.containsAll(inputOwnerKeys));
            req.using("The New  owner must sign MyCashState transfer transaction",
                    signers.containsAll(outputOwnerKeys));

            return null;
        });

    }

    /* -----------------------------------------------------------------------
                Processing of destroying cash
       ----------------------------------------------------------------------- */
    private void destroyCash(LedgerTransaction tx, Set<PublicKey> signers) {

        List<MyCashState> cashInputs = tx.inputsOfType(MyCashState.class);
        List<MyCashState> cashOutputs = tx.outputsOfType(MyCashState.class);

        Set<PublicKey> inputOwnerKeys = new HashSet<>();
        Set<PublicKey> bankKeys = new HashSet<>();

        requireThat(req -> {
            req.using("Destroy transaction should not create any outputs.",
                    cashOutputs.isEmpty());
            return null;
        });

        for (int i = 0; i < cashInputs.size(); ++i) {
            PublicKey inputOwner = cashInputs.get(i).getOwner().getOwningKey();
            inputOwnerKeys.add(inputOwner);

            if(cashInputs.get(i).getBank().getOwningKey() != null)
                bankKeys.add(cashInputs.get(i).getBank().getOwningKey());

            requireThat(req -> {
                req.using("The Owner of the cash and bank must sign  destroy transaction",
                        signers.containsAll(ImmutableList.of(inputOwnerKeys, bankKeys)));
                return null;
            });
        }

    }



    /* -----------------------------------------------------------------------
            Commands that are Used to indicate the transaction's intent.
    ----------------------------------------------------------------------- */

    public interface Commands extends CommandData {
        class VerifyIssuedCash implements Commands {
        }

        class TransferCash implements Commands {
        }

        class DestroyCash implements Commands {
        }
    }


}