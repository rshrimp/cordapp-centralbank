package com.bank;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.NullKeys;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

public class MyCashFlows {
    /* --------------------- IssueMyCash Flow  ------------------------------------------------------------------------- */
    @InitiatingFlow
    @StartableByRPC
    public static class IssueMyCashFlow extends MyCashBaseFlow {

        private final Amount<Currency> issuedAmount;


        /* --- Constructor -------------------------------------------------------------- */
        public IssueMyCashFlow(Amount<Currency> issuedAmount) {
            this.issuedAmount = issuedAmount;


        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            /* --- Start  -------------------------------------------------- */
            // Step 1. GENERATING_TRANSACTION.
             final Party me = getOurIdentity(); //this is issuer's identity. We check it if it is banks identity in cash contract

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            //Flow is initiated by the bank and owner is bank since it is cash issuance state.
            final MyCashState myCashState = new MyCashState(issuedAmount, me, me);
            final List<PublicKey> requiredSigners = myCashState.getParticipantKeys();

            final TransactionBuilder utx = new TransactionBuilder(getAvailableNotary())//Obtain a reference to the notary we want to use.
                    .addOutputState(myCashState, MyCashContract.ID)
                    .addCommand(new MyCashContract.Commands.VerifyIssuedCash(), requiredSigners);


            // Step 2. SIGNING_TRANSACTION.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(utx, requiredSigners);

            // Step 2. VERIFYING_TRANSACTION.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            try {

                // We can now verify the transaction to ensure that it satisfies
                // the contracts of all the transaction's input and output states.

                signedTx.verify(getServiceHub());


                // We'll often want to perform our own additional verification
                // too. Just because a transaction is valid based on the contract
                // rules and requires our signature doesn't mean we have to
                // sign it! We need to make sure the transaction represents an
                // agreement we actually want to enter into.

                // To do this, we need to convert our ``SignedTransaction``
                // into a ``LedgerTransaction``. This will use our ServiceHub
                // to resolve the transaction's inputs and attachments into
                // actual objects, rather than just references.

                LedgerTransaction ledgerTx = signedTx.toLedgerTransaction(getServiceHub());
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Some this went wrong in issuing cash" + VERIFYING_TRANSACTION);
            }

            // Step 4. GATHERING_SIGS.
            progressTracker.setCurrentStep(GATHERING_SIGS);

            //nothing to do here as bank does not need to gather signatures from any other party to issue cash.


            // Step 5. FINALISING_TRANSACTION.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(signedTx, FINALISING_TRANSACTION.childProgressTracker()));
        }
    }


    /* ---------------- TransferMyCash from one owner to another owner (essentially changing hands initiated by current owner only---- */
    @StartableByRPC
    @InitiatingFlow
    public static class TransferMyCash extends MyCashBaseFlow {


        private final UniqueIdentifier[] linearIds;
        private final Party newOwner;
        private final Amount<Currency> transferAmount;

        public TransferMyCash(UniqueIdentifier ids[], Party newOwner, Amount<Currency> amount) {
            this.newOwner = newOwner;
            this.transferAmount = amount;
            this.linearIds = new UniqueIdentifier[ids.length];
            for (int i = 0; i < ids.length; ++i) {
                this.linearIds[i] = ids[i];
            }
        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            /* --- Start  -------------------------------------------------- */
            // Step 1. GENERATING_TRANSACTION.

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            int numOfInputs = linearIds.length;

            Amount<Currency> sumOfInputCash  = net.corda.finance.Currencies.DOLLARS(0);//we are defaulting to USD

            List<StateAndRef<MyCashState>> inputMyCashState = getCashStateByLinearId(linearIds);


            for (int i = 0; i < numOfInputs; ++i) {
                //get cashState passed from the vault using linearId

                sumOfInputCash =sumOfInputCash.plus(inputMyCashState.get(i).getState().getData().getAmount());

                //this flow can only be initiated by the current owner of cash
                final AbstractParty cashOwner = inputMyCashState.get(i).getState().getData().getOwner();
                //abort if non owner of cash started this flow.
                if (!getOurIdentity().equals(cashOwner)) {
                    throw new IllegalStateException("Cash transfer can only be initiated by the current cash owner");
                }
            }

            /* check if we have enough cash to transfer it to the new owner */
            if (sumOfInputCash.compareTo(transferAmount) < 0) {
                throw new FlowException(String.format("Cash to be transferred  is more than available cash %s.", sumOfInputCash.getQuantity()));
            } else if (sumOfInputCash.equals(transferAmount)) {
                /*
                all of the inputs should be transferred to the new owner as input sum= transferAmount
                and the current owners input states are consumed. no new output should belong to current owner
                To:DO - need to find a better way!
                */
                transferAllCash(inputMyCashState);
            } else if (sumOfInputCash.compareTo(transferAmount) > 0) {
                //current owner will have left over cash which will be new output for him/her.
                final Amount<Currency> amountLeftToOwner = sumOfInputCash.minus(transferAmount);
                transferPartialCash(inputMyCashState, amountLeftToOwner);
            } else {
                throw new IllegalStateException(" An unknown situation has occurred during transfer of cash. " + GENERATING_TRANSACTION);
            }
            return null;

        }

        /* ----------------- Method to transfer all cash from owner to new owner ------------------------------------*/

        private SignedTransaction transferAllCash(List<StateAndRef<MyCashState>> inputMyCashState) throws FlowException {
            final TransactionBuilder builder = new TransactionBuilder(inputMyCashState.get(0).getState().getNotary());
            //both current and new owner to sign the outputs
            final List<PublicKey> requiredSigners = new ImmutableList.Builder<PublicKey>()
                    .add(getOurIdentity().getOwningKey())
                    .add(newOwner.getOwningKey()).build();


            MyCashState[] newMyCashState = new MyCashState[inputMyCashState.size()];

            for (int i = 0; i < linearIds.length; ++i) {
                newMyCashState[i] = inputMyCashState.get(i).getState().getData().withNewOwner(newOwner);
                builder.addInputState(inputMyCashState.get(i));
                builder.addOutputState(newMyCashState[i], MyCashContract.ID);
            }
            builder.addCommand(new MyCashContract.Commands.TransferCash(), requiredSigners);

            // Step 2. SIGNING_TRANSACTION.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder, requiredSigners);

            // Step 3. VERIFYING_TRANSACTION.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            try {
                signedTx.verify(getServiceHub());
                LedgerTransaction ledgerTx = signedTx.toLedgerTransaction(getServiceHub());
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Some this went wrong in transferCash Flow." + VERIFYING_TRANSACTION);
            }

            // Step 4. GATHERING_SIGS.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            //the new owner needs to accept the cash so we send it for the newOwner signing.
            FlowSession otherPartySession = initiateFlow(newOwner);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(signedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Step 5. FINALISING_TRANSACTION.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()));

        }

        /* ----------------- Method to transfer Partial cash from owner to new owner ------------------------------------*/
        private SignedTransaction transferPartialCash(List<StateAndRef<MyCashState>> inputMyCashState, Amount<Currency> leftOverAmount) throws FlowException {
            final TransactionBuilder builder = new TransactionBuilder(inputMyCashState.get(0).getState().getNotary());

            //left over cash stays with the input owner, but we have to create a new output
            MyCashState leftOverCash = new MyCashState(leftOverAmount, NullKeys.INSTANCE.getNULL_PARTY(), getOurIdentity());

            //both current and new owner to sign the outputs
            final List<PublicKey> requiredSigners = new ImmutableList.Builder<PublicKey>()
                    .add(getOurIdentity().getOwningKey())
                    .add(newOwner.getOwningKey()).build();


            MyCashState newMyCashState = new MyCashState(transferAmount, NullKeys.INSTANCE.getNULL_PARTY(), newOwner);

            builder.addOutputState(leftOverCash, MyCashContract.ID);
            builder.addOutputState(newMyCashState, MyCashContract.ID);

            //and now add all the input states from the existing owner
            for (int i = 0; i < linearIds.length; ++i) {
                builder.addInputState(inputMyCashState.get(i));
            }
            builder.addCommand(new MyCashContract.Commands.TransferCash(), requiredSigners);
            // Step 2. SIGNING_TRANSACTION.
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder, requiredSigners);

            // Step 3. VERIFYING_TRANSACTION.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            try {
                signedTx.verify(getServiceHub());
                LedgerTransaction ledgerTx = signedTx.toLedgerTransaction(getServiceHub());
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Some thing went wrong in partial transferCash Flow." + VERIFYING_TRANSACTION);
            }

            // Step 4. GATHERING_SIGS.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            //the new owner needs to accept the cash so we send it for the newOwner signing.
            FlowSession otherPartySession = initiateFlow(newOwner);
            final SignedTransaction fullySignedTx = subFlow(
                    new CollectSignaturesFlow(signedTx, ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            // Step 5. FINALISING_TRANSACTION.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()));


        }


    }

    /* --------------------- NewCashOwnerAcceptor Flow ------------------------------------------------------------------------- */
    @InitiatedBy(TransferMyCash.class) //this flow is called by TransferMyCash
    public static class NewCashOwnerAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public NewCashOwnerAcceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                    super(otherPartyFlow, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) {
                    //new user is glad to accept cash
                }
            }

            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }


    /* --------------------- DestroyCash to Bank--------------------------------------------- */
    @StartableByRPC
    @InitiatingFlow
    public static class DestroyMyCash extends MyCashBaseFlow {


        private final UniqueIdentifier[] linearIds;


        public DestroyMyCash(UniqueIdentifier[] linearIds, Amount<Currency> amount) {
            this.linearIds = linearIds;

        }

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            /* --- Start  -------------------------------------------------- */
            // Step 1. GENERATING_TRANSACTION.

            progressTracker.setCurrentStep(GENERATING_TRANSACTION);
            //get cashState passed from the vault using linearId
            List<StateAndRef<MyCashState>> inputMyCashState = getCashStateByLinearId(linearIds);

            for (int i = 0; i < inputMyCashState.size(); ++i) {
                //this flow can only be initiated by the current owner of cash
                final AbstractParty cashOwner = inputMyCashState.get(i).getState().getData().getOwner();
                //only  owner and bank together can destroy/make cash disappear
                if (!getOurIdentity().equals(cashOwner)) {
                    throw new IllegalStateException("Cash transfer can only be initiated by the current cash owner");
                }
            }

            // Step 2. VERIFYING_TRANSACTION.

            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            //no bank or other signers needs except for notary
            final TransactionBuilder builder = new TransactionBuilder(inputMyCashState.get(0).getState().getNotary());

            //and now add all the input states from the existing owner
            for (int i = 0; i < linearIds.length; ++i) {
                builder.addInputState(inputMyCashState.get(i));
            }
            builder.addCommand(new MyCashContract.Commands.DestroyCash(),
                    ImmutableList.of(getOurIdentity().getOwningKey(), inputMyCashState.get(0).getState().getData().getBank().getOwningKey())); //add both owner and bank keys

            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(builder, ImmutableList.of(getOurIdentity().getOwningKey()));

            // Step 3. VERIFYING_TRANSACTION.
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            try {
                signedTx.verify(getServiceHub());
                LedgerTransaction ledgerTx = signedTx.toLedgerTransaction(getServiceHub());
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Something went wrong in destroyCash Flow." + VERIFYING_TRANSACTION);
            }

            // Step 4. GATHERING_SIGS.
            progressTracker.setCurrentStep(GATHERING_SIGS);
            //nothing to do here

            // Step 5. FINALISING_TRANSACTION.
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            return subFlow(new FinalityFlow(signedTx, FINALISING_TRANSACTION.childProgressTracker()));
        }


    }


}
