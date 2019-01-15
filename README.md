<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# The Central Bank CorDapp

This CorDapp comprises a demo of cash that can be issued, transferred and destroyed (aka removed from circulation). The CorDapp includes:

* An cash state definition that records an amount of any currency with bank, and owner parties. The cash state
* A contract that facilitates the verification of issuance, transfer (from one owner to another) and removing of cash.
* Three sets of flows for issuing, transferring and destroying cashs. 
* It also includes test cases for contract testing

The CorDapp allows you to issue, transfer (from old owner to new owner- this could be from bank to owner too) and remove cash. It does not currently 
have  an API yet (in works).

# Instructions for setting up

See https://docs.corda.net/tutorial-cordapp.html#running-the-example-cordapp.

```dtd
+......................+.........................+..................+..............+.........+....................+
UseCase                + Invoker +(Owner)Balance +(newOwner)Balance + Inputs       +Outputs  + Signers            +  
+......................+.........+...............+..................+..............+.........+....................+
Issue $100             + Bank    +(Bank)$100     + NA               + None         +$100     + Bank               +
+......................+.........+...............+..................+..............+.........+....................+
Xfer $50 Bank->newOwner+ Bank    +(Bank)$90      +(new) $50         + $100         + $50, $50+ Bank, newOwner     +
+......................+.........+...............+..................+..............+.........+....................+
Xfer $10 Owner->newOwner+ Owner  +(Owner)$40     +(new) $10         + $50          + $10,$40 + Owner, newOwner    +
+......................+.........+...............+..................+........................+....................+
Destroy $10            + newOwner+               +(newOwner)$0      + $10          +         + newOwner, Bank     +
+......................+.........+...............+..................+..............+.........+....................+

```



# TODO

1. Anonymous parties implementation.
2. Add front end
3. Add flow tests
