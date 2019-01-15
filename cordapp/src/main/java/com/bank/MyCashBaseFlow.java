package com.bank;


import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import java.util.ArrayList;
import java.util.List;

abstract class MyCashBaseFlow extends FlowLogic<SignedTransaction> {

    /* --- Progress tracker set up -------------------------------------------------- */
    protected final ProgressTracker.Step GENERATING_TRANSACTION = new ProgressTracker.Step("Generating transaction based on new Cash.");
    protected final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying Cash contract constraints.");
    protected final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing Cash transaction with our private key.");
    protected final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering counterparty signatures.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    protected final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };
    // The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
    // checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
    // function.
    protected final ProgressTracker progressTracker = new ProgressTracker(
            GENERATING_TRANSACTION,
            VERIFYING_TRANSACTION,
            SIGNING_TRANSACTION,
            GATHERING_SIGS,
            FINALISING_TRANSACTION
    );

    Party getAvailableNotary() throws FlowException {
        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
        if (notaries.isEmpty()) {
            throw new FlowException("No available notary found.");
        }
        return notaries.get(0);
    }

    StateAndRef<MyCashState> getCashStateByLinearId(UniqueIdentifier linearId) throws FlowException {
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                null,
                ImmutableList.of(linearId),
                Vault.StateStatus.UNCONSUMED,
                null);

        List<StateAndRef<MyCashState>> states = getServiceHub().getVaultService().queryBy(MyCashState.class, queryCriteria).getStates();
        if (states.size() != 1) {
            throw new FlowException(String.format("MyCashState with id %s not found.", linearId));
        }
        return states.get(0);
    }

    List<StateAndRef<MyCashState>> getCashStateByLinearId(UniqueIdentifier[] linearId) throws FlowException {

        MyCashState[] inputMyCashState = new MyCashState[linearId.length];
        List<StateAndRef<MyCashState>> queriedStates = null;
        List<StateAndRef<MyCashState>> returnStates = new ArrayList<>();

        QueryCriteria queryCriteria = null;
        for (int i = 0; i < linearId.length; ++i) {
            queryCriteria = new QueryCriteria.LinearStateQueryCriteria(
                    null,
                    ImmutableList.of(linearId[i]),
                    Vault.StateStatus.UNCONSUMED,
                    null);
            queriedStates = getServiceHub().getVaultService().queryBy(MyCashState.class, queryCriteria).getStates();
            if (queriedStates.size() != 1) {
                throw new FlowException(String.format("MyCashState with id %s not found.", linearId));
            }
            returnStates.add(queriedStates.get(0));

        }


        return returnStates;
    }

    Party resolveIdentity(AbstractParty abstractParty) {
        return getServiceHub().getIdentityService().requireWellKnownPartyFromAnonymous(abstractParty);
    }

    @Override

    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }

    static class SignTxFlowNoChecking extends SignTransactionFlow {
        SignTxFlowNoChecking(FlowSession otherFlow, ProgressTracker progressTracker) {
            super(otherFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction tx) {

        }
    }

}