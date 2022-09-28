package gitlet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


/**
 * This class represents a branch in GitTree data structure.
 * Contains pointers to GitCommits.
 * @author aarini
 */
public class GitBranch implements Serializable {
    /** GitBranch constructor with name BRANCHNAME, pointer
     * to HEAD GitCommit, and pointer to a GitStage.*/
    public GitBranch(String branchName, GitCommit head) {
        _name = branchName;
        _latestGitCommit = head;
        _gitStage = new GitStage(head);
        _commitIDs = new HashMap<>();
        _abbID = new HashMap<>();
        _commitIDs.put(head.getId(), head);
    }

    /** Creates a GitCommit with commit message MSG.*/
    public void gitCommit(String msg) {
        _latestGitCommit = new GitCommit(_gitStage, msg);
        _commitIDs.put(_latestGitCommit.getId(), _latestGitCommit);
        _gitStage = new GitStage(this.getLatestGitCommit());
    }

    /** Creates a GitCommit (merge commit) with commit message MSG and parents
     * FPAR and SPAR.*/
    public void gitCommit(String msg, String fPar, String sPar) {
        _latestGitCommit = new GitCommit(getGitStage(), msg, fPar, sPar);
        _commitIDs.put(_latestGitCommit.getId(), _latestGitCommit);
        _gitStage = new GitStage(getLatestGitCommit());
    }

    /** Returns length of the current branch.*/
    private int bLength() {
        int len;
        GitCommit com = getLatestGitCommit();
        for (len = 0; com != null; len++) {
            com = com.getParent();
        }
        return len;
    }


    /**Returns the ancestors of BCOM.*/
    public ArrayList<GitCommit> getAnces(GitCommit bCom) {
        ArrayList<GitCommit> par = new ArrayList<>();
        ArrayList<GitCommit> parM = new ArrayList<>();
        if (bCom == null) {
            String a;
        } else {
            String bId = bCom.getId();
            if (bId.equals("")) {
                return par;
            }
            if (bCom.getMsg().equals("initial commit")) {
                return par;
            }
            if (bCom.isMerged()) {
                parM = getAnces(_commitIDs.get(bCom.getSecParentS()));
            }
            ArrayList<GitCommit> result = getAnces(bCom.getParent());
            par.add(bCom);
            par.addAll(result);
        }
        if (!parM.isEmpty()) {
            par.addAll(parM);
        }
        return par;
    }



    /** Returns the name of the branch. */
    public String getName() {
        return this._name;
    }
    /** Returns the head of the branch, or the newest GitCommit.*/
    public GitCommit getLatestGitCommit() {
        return _latestGitCommit;
    }
    /** Set latest GitCommit to COM.*/
    public void setLatestGitCommit(GitCommit com) {
        _latestGitCommit = com;
    }
    /** Returns the current GitStage. */
    public GitStage getGitStage() {
        return _gitStage;
    }
    /** Stage file F.*/
    public void gitStageFile(String f) {
        _gitStage.add(f);
    }
    /** removes file N from the staging area.*/
    public void remove(String n) {
        _gitStage.remove(n);
    }
    /** Set GitStage to S.*/
    public void setGitStage(GitStage s) {
        _gitStage = s;
    }

    /** Mapping of Abbreviated IDs to their SHA-1 ID's.*/
    private HashMap<String, String> _abbID;
    /** Mapping of SHA-1 ID's to their GitCommits.*/
    private Map<String, GitCommit> _commitIDs;
    /** Name of the branch.*/
    private String _name;
    /** Head of the branch.*/
    private GitCommit _latestGitCommit;
    /** Picture of the current staging area.*/
    private GitStage _gitStage;
}
