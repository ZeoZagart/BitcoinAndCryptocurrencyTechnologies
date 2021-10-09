// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

class BlockNode {
    private final Block currentBlock;
    private final BlockNode prev;
    private int height;
    private final UTXOPool utxoPool;

    BlockNode(BlockNode lastNode, Block _newBlock, UTXOPool pool) {
        currentBlock = _newBlock;
        prev = lastNode;
        utxoPool = pool;
        height = lastNode == null ? 1 : lastNode.height() + 1;
    }

    Optional<BlockNode> findRecursive(byte[] hashToLookFor) {
        BlockNode current = this;
        while (current != null && !AllBlockChains.baEquals(current.getBlock().getHash(), hashToLookFor)) {
            current = current.parent();
        }
        if (current != null) return Optional.of(current);
        else return Optional.empty();
    }

    BlockNode parent() {
        return prev;
    }

    Block getBlock() {
        return currentBlock;
    }

    UTXOPool getUtxoPool() {
        return new UTXOPool(utxoPool);
    }

    int height() {
        return height;
    }
}

class AllBlockChains {
    private final List<BlockNode> endNodes = new LinkedList<>();

    AllBlockChains(Block genesisBlock) {
        UTXOPool pool = createCoinbaseTxn(genesisBlock.getCoinbase(), new UTXOPool());
        BlockNode genesisNode = new BlockNode(null, genesisBlock, pool);
        endNodes.add(genesisNode);
    }

    BlockNode getMaxHeightBlock() {
        return endNodes.stream().reduce((maxNode, currentNode) -> {
            if (maxNode.height() < currentNode.height()) return currentNode;
            else return maxNode;
        }).get();
    }

    boolean addBlock(Block newBlock) {
        if (newBlock.getPrevBlockHash() == null) return false;
        BlockNode parent = findParent(newBlock);
        if (parent == null) return false;
        return addBlock(parent, newBlock);
    }

    private BlockNode findParent(Block currentBlock) {
        byte[] parentHash = currentBlock.getPrevBlockHash();
        for (BlockNode endNode : endNodes) {
            Optional<BlockNode> node = endNode.findRecursive(parentHash);
            if (node.isPresent()) return node.get();
        }
        return null;
    }

    private boolean addBlock(BlockNode prevBlockNode, Block currentBlock) {
        assert baEquals(prevBlockNode.getBlock().getHash(), currentBlock.getHash());
        if (prevBlockNode.height() + 1 <= getMaxHeightBlock().height() - BlockChain.CUT_OFF_AGE) return false;
        UTXOPool newPool = createNewUTXOPool(prevBlockNode, currentBlock);
        if (newPool == null) return false;
        BlockNode newNode = new BlockNode(prevBlockNode, currentBlock, newPool);
        // remove the parent from chain if he is in the chain
        endNodes.removeIf((node) -> baEquals(node.getBlock().getHash(), prevBlockNode.getBlock().getHash()));
        endNodes.add(newNode);
        return true;
    }

    private UTXOPool createNewUTXOPool(BlockNode currentNode, Block newBlock) {
        UTXOPool uPool = currentNode.getUtxoPool();
        TxHandler handler = new TxHandler(uPool);
        Transaction[] validTxs = handler.handleTxs(newBlock.getTransactions().toArray(new Transaction[0]));
        if (validTxs.length != newBlock.getTransactions().size()) return null;
        return createCoinbaseTxn(newBlock.getCoinbase(), handler.getUTXOPool());
    }

    private UTXOPool createCoinbaseTxn(Transaction coinBase, UTXOPool inpPool) {
        UTXOPool pool = new UTXOPool(inpPool);
        for (int i = 0, outputsSize = coinBase.getOutputs().size(); i < outputsSize; i++) {
            UTXO utxo = new UTXO(coinBase.getHash(), i);
            pool.addUTXO(utxo, coinBase.getOutput(i));
        }
        return pool;
    }

    public static boolean baEquals(byte[] a, byte[] b) {
        return Arrays.equals(a, b);
    }
}

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private final AllBlockChains chains;
    private final TransactionPool txPool;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        chains = new AllBlockChains(genesisBlock);
        txPool = new TransactionPool();
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        return chains.getMaxHeightBlock().getBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        return chains.getMaxHeightBlock().getUtxoPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        return txPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        return chains.addBlock(block);
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        txPool.addTransaction(tx);
    }
}