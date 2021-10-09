import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;


/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {

    private final Set<Transaction> currentTxs = new HashSet<>();
    private final Set<Integer> followees = new HashSet<>();
    private final Set<Integer> blacklist = new HashSet<>();
    private int rc = 0;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
    }

    public void setFollowees(boolean[] followees) {
        for (int i = 0; i < followees.length; i++) {
            if (followees[i]) this.followees.add(i);
        }
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        currentTxs.addAll(pendingTransactions);
    }

    public Set<Transaction> sendToFollowers() {
        rc += 1;
        return currentTxs;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        if (rc >= 2) {
            Set<Integer> senders = candidates.stream().map((cd) -> cd.sender).collect(Collectors.toSet());
            for (int followee : followees) {
                if (!senders.contains(followee)) blacklist.add(followee);
            }
        }
        for (Candidate candidate : candidates) {
            if (rc < 2 && currentTxs.size() > 0) { break; }
            if (!blacklist.contains(candidate.sender)) currentTxs.add(candidate.tx);
        }
    }
}

// init --> 1
// max --> 1 + (followingCount/2)
// steps --> numRounds
// delta --> (max - init)/numRounds
