package co.ntweb.maigfrga.week1;

import java.util.*;

public class MaxFeeTxHandler {

    private UTXOPool utxoPool;
    public MaxFeeTxHandler(UTXOPool utxoPool) {
    	this.utxoPool = new UTXOPool(utxoPool);
    }

    public boolean containsUTXO(UTXO utxo) {
       return this.utxoPool.contains(utxo);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
    	UTXO uxto = null;
    	int idx = 0;
    	double totalInputs = 0d;
    	double totalOutputs = 0d;
    	List<UTXO> utxoList = new ArrayList<UTXO>();

        if(null == tx) return false;

    	for(Transaction.Input i: tx.getInputs()) {
    		uxto = new UTXO(i.prevTxHash, i.outputIndex);

            // Check if all outpus are in the unspent transactions pool
            if (!this.utxoPool.contains(uxto)) {
                return false;
            }

            // Getting the output associated to the current input
            Transaction.Output out = this.utxoPool.getTxOutput(uxto);
            if (out == null) return false;
            totalInputs += out.value;


            // check that the signatures on each input of {@code tx} are valid
            if (!Crypto.verifySignature(out.address, tx.getRawDataToSign(idx), i.signature)) {
                return false;
            }

            // check no UTXO is claimed multiple times by {@code tx}
            if(utxoList.contains(uxto)) {
                return false;
            } else {
                utxoList.add(uxto);
            }

            idx++;

    	}

    	// checks {@code tx}s output values are non-negative
    	for(Transaction.Output o: tx.getOutputs()) {
    	    if (o.value < 0d) {
    	        return false;
            }
            totalOutputs += o.value;
    	}

        // check if the sum of {@code tx}s input values is greater than or equal to the sum of its output
    	if (totalOutputs > totalInputs) return false;

    	return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleMaxTxs(Transaction[] possibleTxs) {
        if (possibleTxs == null || possibleTxs.length == 0) return null;

        Transaction t = null;
        List<Transaction> tList = new ArrayList<Transaction>();
        UTXO uxto = null;
        Transaction[] tArray = null;

        for(int i=0; i< possibleTxs.length; i++) {
            t = possibleTxs[i];
            if (this.isValidTx(t)) {
                double totalInputs = 0d;
                double totalOutpus = 0d;

                for(Transaction.Input input: t.getInputs()) {
                    uxto = new UTXO(input.prevTxHash, input.outputIndex);

                    // Getting the output associated to the current input
                    Transaction.Output out = this.utxoPool.getTxOutput(uxto);
                    if (out == null) continue;
                    totalInputs += out.value;
                    this.utxoPool.removeUTXO(uxto);
                }

                int outIndex = 0;
                for(Transaction.Output output: t.getOutputs()) {
                    uxto = new UTXO(t.getHash(), outIndex);
                    outIndex++;
                    this.utxoPool.addUTXO(uxto, output);
                    totalOutpus += output.value;
                }
                uxto = null;

                tList.add(t);
            }
            t = null;
        }

        if(tList.size() > 0) {
            int idx = 0;
            tArray = new Transaction[tList.size()];
            for(Transaction transaction: tList) {
                tArray[idx] = transaction;
                idx++;
            }
        }

        return tArray;
    }
    /**
     * Choose the most profitable transaction in a list of conflicting transactions
     * @param transactionList
     * @return
     */

    private Transaction maximizeTransactionFees(List<Transaction> transactionList) {
        Transaction best = transactionList.get(0);
        double bestFee = 0d;
        UTXO uxto;

        for(Transaction t: transactionList) {
            double currentFee = 0d;

            for(Transaction.Input input: t.getInputs()) {
                uxto = new UTXO(input.prevTxHash, input.outputIndex);

                // Getting the output associated to the current input
                Transaction.Output out = this.utxoPool.getTxOutput(uxto);
                if (out == null) continue;
                currentFee += out.value;
            }

            int outIndex = 0;
            for(Transaction.Output output: t.getOutputs()) {
                currentFee -= output.value;
            }

            if(currentFee > bestFee) {
                best = t;
                bestFee = currentFee;
            }

            currentFee = 0d;
        }
        return best;
    }


    /**
     * Finds a set of transactions with maximum total transaction fees -- i.e. maximize the sum over all
     * transactions in the set of (sum of input values - sum of output values)).
     * @param possibleTxs
     * @return
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        if (possibleTxs == null || possibleTxs.length == 0) return null;

        Map<UTXO, List<Transaction>> utxoTransactions = new HashMap<>();
        Transaction t = null;
        List<Transaction> tList = new ArrayList<Transaction>();
        UTXO uxto = null;


        // First step is to indentify transactions that try to access same unspent transaction
        for(int i=0; i< possibleTxs.length; i++) {
            t = possibleTxs[i];

            if (this.isValidTx(t)) {
                for(Transaction.Input input: t.getInputs()) {
                    uxto = new UTXO(input.prevTxHash, input.outputIndex);

                    if(utxoTransactions.containsKey(uxto)) {
                        utxoTransactions.get(uxto).add(t);
                    } else {
                        List<Transaction> tl = new ArrayList<>();
                        tl.add(t);
                        utxoTransactions.put(uxto, tl);
                    }

                }
            }

            t = null;
        }

        List<Transaction> transactionsToProcess = new ArrayList<>();

        // getting a final list of transactions to process
        for(List<Transaction> l: utxoTransactions.values()) {

            // If there is only one transaction that refers to a particular unspent transaction , add to the
            // possible transaction list, otherwise, add the most profitable transaction
            if(l.size() == 1) {
                transactionsToProcess.add(l.get(0));
            } else {
                transactionsToProcess.add(maximizeTransactionFees(l));
            }
        }

        Transaction[] tArray = new Transaction[transactionsToProcess.size()];
        int idx = 0;
        for(Transaction transaction: transactionsToProcess) {
            tArray[idx] = transaction;
            idx++;
        }
        return handleMaxTxs(tArray);
    }
}
