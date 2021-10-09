import java.util.*;

public class TxHandler {

    /**
	 * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
	 * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
	 * constructor.
	 */
	UTXOPool pool;

	public TxHandler(UTXOPool utxoPool) {
		this.pool = new UTXOPool(utxoPool);
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
		// collect all prev-outputs, also verify all prev-outputs exists
		// also verify that current input is correctly signed before adding to ledger
		List<Transaction.Output> prevOutputs = new ArrayList<>();
		Set<UTXO> usedUtxos = new HashSet<>();
		for (int i = 0, inputsSize = tx.numInputs(); i < inputsSize; i++) {
			Transaction.Input input = tx.getInput(i);
			UTXO prevUtxo = new UTXO(input.prevTxHash, input.outputIndex);
			if (pool.contains(prevUtxo) && !usedUtxos.contains(prevUtxo)) {  // prev-transaction is in pool
				usedUtxos.add(prevUtxo);
				Transaction.Output prevO = pool.getTxOutput(prevUtxo);
				if (!Crypto.verifySignature(prevO.address, tx.getRawDataToSign(i), input.signature)) return false;
				prevOutputs.add(prevO);
			} else return false;
		}
		// verify that current outputs <= prev-outputs
		boolean allPositive	= tx.getOutputs().stream().allMatch((output) -> output.value >= 0);
		double outputSum = tx.getOutputs().stream().map((output) -> output.value).reduce(0.0, Double::sum);
		double inputSum = prevOutputs.stream().map(op -> op.value).reduce(0.0, Double::sum);
		return allPositive && inputSum >= outputSum;
	}

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
    	ArrayList<Transaction> validTxs = new ArrayList<>();
		for (Transaction tx: possibleTxs) {
			// if is valid --> update the pool, and add it to returned set
			if (isValidTx(tx)) { // add to ledger -i.e.- update the pool
				addToPool(tx);
				validTxs.add(tx);
			}
		}
		return validTxs.toArray(new Transaction[0]);
    }

    private void addToPool(Transaction tx) {
    	tx.getInputs().forEach(input -> {
    		pool.removeUTXO(new UTXO(input.prevTxHash, input.outputIndex));
		});
		ArrayList<Transaction.Output> outputs = tx.getOutputs();
		for (int i = 0, outputsSize = outputs.size(); i < outputsSize; i++) {
			UTXO newUtxo = new UTXO(tx.getHash(), i);
			pool.addUTXO(newUtxo, outputs.get(i));
		}
	}

}
