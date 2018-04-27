package co.ntweb.maigfrga.week1;

import java.util.ArrayList;
import java.util.List;

public class TxHandler {

	private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
    	this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * Returs true if unspend transaction exists in the pool
     * @param utxo
     * @return
     */
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
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        if (possibleTxs == null || possibleTxs.length == 0) return null;

        Transaction t = null;
        List<Transaction> tList = new ArrayList<Transaction>();
        UTXO uxto = null;
        Transaction[] tArray = new Transaction[0];

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

}
