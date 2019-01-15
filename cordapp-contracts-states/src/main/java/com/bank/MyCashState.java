package com.bank;


import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import org.jetbrains.annotations.NotNull;


import java.security.PublicKey;
import java.util.*;
import java.util.stream.Collectors;

// *********
// * State *
// *********
public class MyCashState implements LinearState {

    private final Amount<Currency> amount;
    private final AbstractParty bank;
    private final AbstractParty owner;
    private final UniqueIdentifier linearId;

    /* --- Constructors  --- */

    public MyCashState(Amount<Currency> amount, AbstractParty bank, AbstractParty owner) {
        this.amount = amount;
        this.bank = bank;
        this.owner = owner;
        this.linearId = new UniqueIdentifier();
    }

    /*  --- Getters ---*/

    public Amount<Currency> getAmount() {
        return amount;
    }

    public AbstractParty getBank() {
        return bank;
    }

    public AbstractParty getOwner() {
        return owner;
    }

    @NotNull
    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    /* --- utility functions for cash transfer ----*/
    public MyCashState withNewOwner(AbstractParty newOwner) {
        return new MyCashState(this.amount, this.bank, newOwner);
    }

    public MyCashState withoutOwner() {
        return new MyCashState(this.amount, this.bank, NullKeys.INSTANCE.getNULL_PARTY());
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(owner);
    }

    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }


    @Override
    public String toString() {
        return String.format("MyCashState(amount=%s, lender=%s, owner=%s, linearId=%s)", amount, bank, owner);
    }



}