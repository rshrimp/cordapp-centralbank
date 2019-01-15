package com.bank.contract;

import com.bank.MyCashState;
import net.corda.core.contracts.Amount;
import net.corda.finance.Currencies;
import org.junit.Before;

import java.math.BigDecimal;
import java.util.Currency;

import static net.corda.finance.Currencies.DOLLARS;


public class test {


    private static class Cash{
        Amount<Currency> amount;

        public Cash(Amount<Currency> amount) {
            this.amount = amount;
        }

        public Amount<Currency> getAmount() {
            return amount;
        }
    }

    public static void main(String args[]){

        Amount<Currency> sumOfInputCash  = DOLLARS(0);//we are defaulting to USD
        Amount<Currency> sumOfOutputCash  = DOLLARS(7);//we are defaulting to USD
        Cash[] mycash = {
                new Cash(DOLLARS(1)),

                new Cash(DOLLARS(2)),
                new Cash(DOLLARS(3)),
        };

        long sum = 0;
        for (int i= 0; i< mycash.length;++i)
        {
            System.out.println(" amount with getQuantity returns " +i +":"+mycash[i].getAmount().getQuantity());
            sum += mycash[i].getAmount().getQuantity();
            sumOfInputCash = sumOfInputCash.plus(mycash[i].getAmount());
        }
          System.out.println("sum with long addition="+ sum);
        System.out.println("sumOfInputCash  ="+ sumOfInputCash.toDecimal());
        //System.out.println("sumOfInputCash - sumOfFirst int value="+ (sumOfInputCash.minus(mycash[1].getAmount())).toDecimal().intValue());


        System.out.println("****sumOfInputCash comprateTO sumOfOutput="+ (sumOfInputCash.compareTo(sumOfOutputCash)));
        System.out.println("****sumOfInputCash equals sumOfOutput="+ (sumOfInputCash.equals(sumOfOutputCash)));

        System.out.println("****sumOfInputCash minus sumOfOutput="+ (sumOfInputCash.minus(sumOfOutputCash)));
/*        Amount<Currency> first = DOLLARS(1);
        Amount<Currency> second = DOLLARS(2);
        Amount<Currency> third = DOLLARS(3);
        Amount<Currency> zero = DOLLARS(0);

        System.out.println("...................");
        System.out.println("first="+ first);
        System.out.println("quantity="+ first.getQuantity());
        System.out.println("Sum="+ sumOfInputCash.plus(first).plus(second).plus(third));

        System.out.println("zero.todecimal.intvalue=" +zero.toDecimal());
        System.out.println(zero.toDecimal().intValue() == 0);*/

    }

}
