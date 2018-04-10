package co.ntweb.maigfrga.week1;

// Copyright (C) 2016-2017 Enrique Albertos
// Distributed under the GNU GPL v2 software license


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.After;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.HashMap;
import java.util.Map;

import co.ntweb.maigfrga.utils.TestFactory;
import org.junit.Test;
/**
 * Unit tests for {@link TxHandler#isValidTx(Transaction)}
 * <p>
 * Test Strategy:
 * Test 1: test isValidTx() with valid transactions
 * Test 2: test isValidTx() with transactions containing signatures of incorrect data
 * Test 3: test isValidTx() with transactions containing signatures using incorrect private keys
 * Test 4: test isValidTx() with transactions whose total output value exceeds total input value
 * Test 5: test isValidTx() with transactions that claim outputs not in the current utxoPool
 * Test 6: test isValidTx() with transactions that claim the same UTXO multiple times
 * Test 7: test isValidTx() with transactions that contain a negative output value
 * 
 * @author ealbertos
 *
 */

public class IsValidTest {

	private TestFactory factory;

	@Before
	public void initialize() {
		factory = new TestFactory();
	}

	@After
	public void destroy() {
		factory = null;
	}

	private static void assertTestSetIsValid(final UtxoTestSet utxoTestSet) {
		final ValidationLists<Transaction> trxsValidation = utxoTestSet.getValidationLists();
		
		// Instantiate student solution
		final TxHandler txHandler = new TxHandler(utxoTestSet.getUtxoPool());
		
		// Check validation of all the transactions in the set
		for (Transaction tx: trxsValidation.allElements()) {
			assertEquals(txHandler.isValidTx(tx), trxsValidation.isValid(tx) );
		}
	}

	// Test that that all outputs claimed by a given transaction are in the current UTXO pool
	@Test
	public void testUTXOExistsInPool() throws InvalidKeyException, NoSuchAlgorithmException, SignatureException {

		KeyPair pk_scrooge = factory.createAddress();
        KeyPair pk_alice = factory.createAddress();
        KeyPair pk_strange = factory.createAddress();

		// Create a UTXO pool that has an initial root transaction with a valid
        // unspent trasaction        
        Map<byte[], UTXOPool> m = factory.createUtxoPool(pk_scrooge, 10);        
        byte[] rootHash = (byte[]) m.keySet().toArray()[0];
		UTXOPool pool = (UTXOPool) m.values().toArray()[0];
        Transaction rootTransaction = factory.getTransaction(rootHash);
        
        // List of inputs to create a transaction, every input is a hash of the transaction 
        // and the output index
        Map<byte[], Integer> inputs = new HashMap<>();
        inputs.put(rootTransaction.getHash(), 0);
        
        Map<PublicKey, Double> outputs = new HashMap<>();
        outputs.put(pk_alice.getPublic(), 5d);
        
        Transaction validTransaction = factory.createTransaction(pk_scrooge, inputs, outputs);


		final TxHandler txHandler = new TxHandler(pool);
        // check if root transaction is valid
		assertTrue(txHandler.isValidTx(rootTransaction));

        Map<PublicKey, Double> outputs = new HashMap<>();
        outputs.put(pk_alice.getPublic(), 5d);
		// create a spare transaction that is not in the UTXOPool
        Transaction tx = factory.createTransaction(pk_strange, outputs);
        assertFalse(txHandler.isValidTx(tx));
	}

	// Test 1: test isValidTx() with valid transactions
	@Test
	public void testIsValidWithValidTransactions()
			throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Create a new set of transactions for testing		
		final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setCorruptedPercentage(0) // All valid transactions
				.build();
		// check against student solution
		assertTestSetIsValid(utxoTestSet);
		
	}

	
	// Test 2: test isValidTx() with transactions containing signatures of incorrect data
	@Test
	public void testIsValidWithInvalidSignatures() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Create a new set of transactions for testing				
		final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setForceCorruptedSignature(true)
				.setCorruptedPercentage(.20) // probability of 20% of invalid transactions
				.build();
		
		// check against student solution
		assertTestSetIsValid(utxoTestSet);
		
	}
	
	// Test 3: test isValidTx() with transactions containing signatures using incorrect private keys
	@Test
	public void testIsValidSignaturesWithInvalidPrivateKeys() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Create a new set of transactions for testing				
		final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setInvalidPrivateKeys(true) // corrupt the private key that signs
				.setCorruptedPercentage(.20) // probability of 20% of invalid transactions
				.build();
		
		// check against student solution
		assertTestSetIsValid(utxoTestSet);
		
	}

	// Test 4: test isValidTx() with transactions whose total output value exceeds total input value
	@Test
	public void testIsValidTotalOutputExceedsTotalInput() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Create a new set of transactions for testing				
		final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setInvalidTotals(true)  // create transactions with invalid total value
				.setCorruptedPercentage(.20) // probability of 20% of invalid transactions
				.build();
		
		// check against student solution
		assertTestSetIsValid(utxoTestSet);
		
	}

	
	// Test 5: test isValidTx() with transactions that claim outputs not in the current utxoPool
	@Test
	public void testIsValidTransactionsClamingOuputsNotInThePool() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Create a new set of transactions for testing				
		final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setClaimingOutputsNotInPool(true)  // create transactions claiming outputs not in the pool
				.setCorruptedPercentage(.20) // probability of 20% of invalid transactions
				.build();
		
		// check against student solution
		assertTestSetIsValid(utxoTestSet);
		
	}
	
    // Test 6: test isValidTx() with transactions that claim the same UTXO multiple times
	@Test
	public void testIsValidTransactionsClaimingTheSameUTXOSeveralTimes() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Create a new set of transactions for testing				
		final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setClaimingUtxoSeveralTimes(true)  // create transactions claiming the same output several times
				.setCorruptedPercentage(.20) // probability of 20% of invalid transactions
				.build();

		assertTestSetIsValid(utxoTestSet);
		
	}
	
    // Test 7: test isValidTx() with transactions that contain a negative output value
	@Test
	public void testIsValidTransactionsWithNegativeOutput() throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		// Create a new set of transactions for testing				
		final UtxoTestSet utxoTestSet = UtxoTestSet.builder()
				.setPeopleSize(10)
				.setUtxoTxNumber(10)
				.setMaxUtxoTxOutput(10)
				.setMaxValue(200)
				.setTxPerTest(10)
				.setMaxInput(10)
				.setMaxOutput(10)
				.setNegativeOutputs(true)  // create transactions with negative values
				.setCorruptedPercentage(.20) // probability of 20% of invalid transactions
				.build();

		assertTestSetIsValid(utxoTestSet);
		
	}
	



}
