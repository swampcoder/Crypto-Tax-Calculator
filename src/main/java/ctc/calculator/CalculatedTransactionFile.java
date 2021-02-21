package ctc.calculator;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;

import ctc.enums.Currency;
import ctc.enums.TradeType;
import ctc.transaction.files.TransactionFile;
import ctc.transactions.Transaction;

/**
 * CalculatedTransaction:
 * A Transaction with an additional gain/loss field
 */

public class CalculatedTransactionFile extends TransactionFile {

    // A collection of Assets currently in possession, indexed by Currecny
    private final HashMap<ctc.enums.Currency, LinkedList<Asset>> assets;

    /**
     * CalculatedTransactionFile    - Takes an ArrayList of Transactions
     *                                and calculates the gain/loss for any
     *                                Transaction considered to be a "Sell"
     */
    public CalculatedTransactionFile(ArrayList<Transaction> transactions) {

        // Initialize assets HashMap to hold crypto is FIFO manner
        assets = new HashMap<ctc.enums.Currency, LinkedList<Asset>>();
        for (ctc.enums.Currency currency : ctc.enums.Currency.values()) {
            assets.put(currency, new LinkedList<Asset>());
        }
        Collections.sort(transactions);

        // Calculate gain/loss for each Transaction
        for (Transaction t: transactions) {
            CalculatedTransaction ct = new CalculatedTransaction(t);

            // BUYs add an item to assets.
            // If the currency used was crypto, then gain/losses were realized
            if (t.getType() == TradeType.BUY) {
                assets.get(t.getMajor()).add(new Asset(
                        t.getDate(),
                        t.getMajor(),
                        t.getAmount(),
                        t.getMajorRate()
                ));

                // Set gain/loss
                if (!t.getMinor().isFiat()) {
                    ct.setGainLoss(sell(t.getMinor(), t.getAmount().multiply(t.getLocalRate()), t.getMinorRate()));
                } else {
                    ct.setGainLoss(BigDecimal.ZERO);
                }
            } else {

                // SELLs lead to a gain/loss being realized
                // If the currency obtained was crypto, then it is considered an asset to track

                // e.g. sell ETH for BTC
                //      sell ETH for CAD
                //      sell CAD for ETH
                ct.setGainLoss(sell(t.getMajor(), t.getAmount(), t.getMajorRate()));

                // Obtained crypto
                if (!t.getMinor().isFiat()) {
                    assets.get(t.getMinor()).add(new Asset(
                            t.getDate(),
                            t.getMinor(),
                            t.getAmount().multiply(t.getLocalRate()),
                            t.getMinorRate()
                    ));
                }
            }

            // Paying fees is also taxable
            if (!t.getFeeCurrency().isFiat()) {
                BigDecimal feeGainLoss = sell(t.getFeeCurrency(), t.getFeeAmount(), t.getFeeRate());
                ct.setGainLoss(ct.getGainLoss().add(feeGainLoss));
            }
            addTransaction(ct);
        }
    }

    /**
     * sell    - Removes the specified amount of the specified currency from assets
     *           This information along with the rate being sold is enough to be able to
     *           calculate gain/losses
     *
     * @param currencySold  - the currency being sold
     * @param amountSold    - the amount being sold
     * @param rateSold      - the rate the currency is being sold
     * @return BigDecimal   - The gain/loss from this transaction
     */
    private BigDecimal sell(ctc.enums.Currency currencySold, BigDecimal amountSold, BigDecimal rateSold) {
        BigDecimal amountBought;
        BigDecimal rateBought;
        BigDecimal gainLoss = BigDecimal.ZERO;

        // Keep taking away from assets until the amount has been "sold"
        while (!amountSold.equals(BigDecimal.ZERO)) {
            try {
                Asset earliest = assets.get(currencySold).element();    // reference to first element
                amountBought = earliest.getAmount();
                rateBought = earliest.getRate();

                // Take amount from assets. Calculate profit
                // If more amount, take next element. repeat
                if (amountBought.compareTo(amountSold) >= 0) {
                    // This can cover all sold
                    earliest.setAmount(amountBought.subtract(amountSold));
                    gainLoss = gainLoss.add((rateSold.subtract(rateBought)).multiply(amountSold));

                    if (amountBought.compareTo(amountSold) == 0) {
                        assets.get(currencySold).remove();
                    }
                    amountSold = BigDecimal.ZERO;
                } else {
                    // Need this and another
                    gainLoss = gainLoss.add((rateSold.subtract(rateBought)).multiply(amountBought));
                    amountSold = amountSold.subtract(amountBought);
                    assets.get(currencySold).remove();
                }

            } catch (NoSuchElementException e) {
            	System.out.println("Curr=" + currencySold);
                System.err.println("No history of " + currencySold + " being bought before being sold ("
                        + amountSold.toString() + " at " + rateSold.toString() + "). Is this a fork or token?)");
                return rateSold.multiply(amountSold);
            }
        }
        return gainLoss;
    }

    /**
     * outputAssets - Outputs the remaining assets after all Transactions are processed
     *                Used to carry over to future calculations (e.g. next tax year)
     */
    public void outputAssets() {
        LinkedList<Asset> currencyAssets;

        System.out.println("=== REMAINING ASSETS ===");
        for (Map.Entry<Currency, LinkedList<Asset> > entry : assets.entrySet()) {
            currencyAssets = entry.getValue();
            if (!currencyAssets.isEmpty()) {
                for (Asset asset : currencyAssets) {
                    System.out.println(asset.toString());
                }
                System.out.println();
            }
        }
    }

    @Override
    public void writeToCsv(String fileName) {
        int cells = numberOfCells();

        String [][] additional = new String[3][cells];
        BigDecimal totalGainLoss = BigDecimal.ZERO;
        BigDecimal totalFees = BigDecimal.ZERO;

        for (Transaction t: getTransactions()) {
            if (t instanceof CalculatedTransaction) {
                totalGainLoss = totalGainLoss.add( ((CalculatedTransaction) t).getGainLoss());
                totalFees = totalFees.add(t.getFee());
            }
        }

        additional[1][cells-2] = String.format("%.2f", totalFees);
        additional[1][cells-1] = String.format("%.2f", totalGainLoss);
        additional[2][cells-2] = "Capital Gains: ";
        additional[2][cells-1] = String.format("%.2f", totalGainLoss.subtract(totalFees));

        super.writeToCsv(fileName, additional);
    }
    
    public static void main(String[] args) throws IOException 
    {
    	Transaction btc1 = new Transaction();
    	Transaction btc2 = new Transaction();
    	btc1.type(TradeType.BUY).amount(0.5).localRate(100).major("BTC").minor("USD").date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)).feeAmount(0).feeCurrency("USD").build();
    	btc2.type(TradeType.SELL).amount(0.4).localRate(4000).major("BTC").minor("USD").date(System.currentTimeMillis() - TimeUnit.DAYS.toMillis(10)).feeAmount(0).feeCurrency("USD").build();
    	
    	ArrayList<Transaction> list = new ArrayList<Transaction>();
    	list.add(btc1);
    	list.add(btc2);
    	CalculatedTransactionFile ctc = new CalculatedTransactionFile(list);
    	ctc.outputAssets();
    	
    	for(Transaction ct : ctc.getTransactions()) 
    	{
    		CalculatedTransaction calcTx = (CalculatedTransaction) ct;
    		System.out.println("GAIN=" + calcTx.getGainLoss());
    	}
    }
}
