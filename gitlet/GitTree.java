package gitlet;
import java.io.*;
import java.util.*;

/** GitTree data structure used for commit tree-structure of Gitlet.
 * @author aarini */

public class GitTree implements Serializable {

    /**
     * GitTree constructor.
     */
    public GitTree() {
        _branchNames = new HashMap<>();
        _commitIDs = new HashMap<>();
        _abbID = new HashMap<>();
        _idOfMsg = new HashMap<>();
        untracked = new ArrayList<>();
        this.remoteDir = new HashMap<>();
    }

    /**
     * Returns a new Gitlet version-control system (.gitlet) in the current directory.
     */
    public static GitTree init() {
        GitTree initTree = new GitTree();
        GitCommit initCom = new GitCommit(null, "initial commit");
        ArrayList<String> ids = new ArrayList<>();
        ids.add(initCom.getId());
        initTree.firstID = initCom.getId();
        initTree._cBranch = new GitBranch("master", initCom);
        initTree._branchNames.put("master", initTree.currentBranch());
        initTree._idOfMsg.put("initial commit", ids);
        initTree._commitIDs.put(initCom.getId(), initCom);
        initTree._abbID.put(initCom.getId().substring(0, 8), initCom.getId());
        initTree.isUntrackedFiles();
        return initTree;
    }

    /**
     * Stages the file FILE (adds a copy of it in its
     * current state to the staging area).
     */
    public void add(String file) {
        if (file != null) {
            _cBranch.gitStageFile(file);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Unstages FILE if currently staged, and if it is tracked in the current commit
     * it marks it to not to be included in next commit and removes it from the working
     * directory (if user has not already removed it).
     */
    public void remove(String file) {
        if (file != null) {
            _cBranch.remove(file);
        } else {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Prints the commitIDs of all GitCommits that have
     * commit message MSG, one ID per line.
     */
    public void find(String msg) {
        if (msg != null && _idOfMsg.containsKey(msg)) {
            for (String id : _idOfMsg.get(msg)) {
                System.out.println(id);
            }
        } else {
            if (msg == null) {
                throw new ArrayIndexOutOfBoundsException();
            }
            if (!_idOfMsg.containsKey(msg)) {
                System.out.println("Found no commit with that message.");
            }
        }
    }

    /**
     * Creates a new GitCommit with commit message MSG.
     */
    public void commit(String msg) {
        if (msg == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (msg.length() == 0) {
            System.out.println("Please enter a commit message.");
            return;
        }
        _cBranch.gitCommit(msg);
        GitCommit com = cbCom();
        String cID = com.getId();
        _commitIDs.put(cID, com);
        ArrayList<String> msgIDs = _idOfMsg.get(msg);
        if (msgIDs == null) {
            msgIDs = new ArrayList<String>();
        }
        msgIDs.add(cID);
        _idOfMsg.put(msg, msgIDs);
        _abbID.put(cID.substring(0, 8), cID);
        cbGitStage().clearStage();
    }

    /**
     * Adds a new BRANCH that points at the current head node.
     */
    public void addBranch(String branch) {
        if (branch == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (_branchNames.containsKey(branch)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        _branchNames.put(branch, new GitBranch(branch, cbCom()));
    }

    /**
     * Deletes pointer associated with BRANCH; does not delete commits created under it.
     */
    public void removeBranch(String branch) {
        if (branch == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (!_branchNames.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
        }
        if (branch.equals(cbName())) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        _branchNames.remove(branch);
    }

    /**
     * Starting at current head commit, displays each commit backwards
     * along commit tree until initial commit, following
     * first parent commit links and ignoring second parents for merge commits.
     */
    public void log() {
        GitCommit com = cbCom();
        while (com != null) {
            System.out.println(com.toString());
            System.out.println();
            com = com.getParent();
        }
    }

    /**
     * Log function, but for all commits ever made.
     */
    public void printGlobalLog() {
        for (String key : _commitIDs.keySet()) {
            System.out.println((_commitIDs.get(key)).toString());
            System.out.println();
        }
    }

    /**
     * Returns true if the directory has untracked files.
     */
    public boolean isUntrackedFiles() {
        File fx = new File(cbCom().getCommitDirectory());
        int dLen = cbCom().getCommitDirectory().length();
        int abLen = fx.getAbsolutePath().length();
        String wPath = fx.getAbsolutePath().substring(0, abLen - dLen + 1);
        File wDir = new File(wPath);
        File[] fileList = wDir.listFiles();
        if (fileList == null) {
            return false;
        }
        for (File file0 : fileList) {
            if (file0.isFile()) {
                String name = file0.getName();
                GitStage stg = cbGitStage();

                if (stg == null) {
                    return false;
                }
                if (!stg.getLatestStaged().contains(name)) {
                    if (cbCom().contains(name)) {
                        String str = Utils.sha1(Utils.readContentsAsString(file0));
                        String sha = cbCom().getfileSHAs().get(name);
                        if (!sha.equals(str)) {
                            untracked.add(name);
                            return true;
                        }
                    } else {
                        untracked.add(name);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns true if there are untracked changes.
     */
    public boolean isUntracked() {
        boolean result = false;
        if (isUntrackedFiles()) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it or add it first.");
            result = true;
        }
        return result;
    }

    /**
     * Merges BRANCH and current branch.
     */
    public void merge(String branch) {
        if (branch == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (!_branchNames.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branch.equals(cbName())) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        if (isUntracked()) {
            return;
        }
        merge(_branchNames.get(branch));
    }

    /**
     * Merges the current branch with given branch B.
     */
    public void merge(GitBranch b) {
        boolean conflict = false;
        GitCommit ch = _cBranch.getLatestGitCommit();
        GitCommit gh = b.getLatestGitCommit();
        GitCommit spl = split(_cBranch, b);
        ArrayList<String> allFiles = new ArrayList<>();
        allFiles.addAll(ch.getAddedFiles());
        for (String x : gh.getAddedFiles()) {
            if (!allFiles.contains(x))
                allFiles.add(x);
        }
        for (String x : spl.getAddedFiles()) {
            if (!allFiles.contains(x))
                allFiles.add(x);
        }
        if (!(cbGitStage().getLatestStaged()).isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (spl.equals(gh) || spl.getId().equals(gh.getId())) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            return;
        } else if (spl.equals(ch) || spl.getId().equals(ch.getId())) {
            _cBranch.setLatestGitCommit(gh);
            System.out.println("Current branch fast-forwarded.");
            return;
        } else {
            for (String file : allFiles) {
                if (!spl.contains(file) && !gh.contains(file) && ch.contains(file)) {

                } else if (!spl.contains(file) && gh.contains(file) && !ch.contains(file)) {
                    gh.checkout(file);
                    cbGitStage().add(file);
                } else if (spl.contains(file) && !ch.unModified(spl, file) && !gh.contains(file)) {
                    remove(file);
                    untracked.add(file);
                } else if (spl.contains(file) && !ch.contains(file) && gh.unModified(spl, file)) {

                } else if (!gh.unModified(spl, file) && ch.unModified(spl, file)) {
                    mergeSimple(gh, file);
                } else if (!ch.unModified(spl, file) && gh.unModified(spl, file)) {

                } else if (!gh.unModified(spl, file) && !ch.unModified(spl, file) && gh.unModified(ch, file)) {

                } else if (!gh.contains(file) && !ch.contains(file)) {
                    if (Arrays.asList(wDFiles()).contains(file)) {
                    }
                } else if ((!gh.unModified(spl, file) && !ch.unModified(spl, file)
                        && (!gh.unModified(ch, file) || !gh.getFile(file).equals(ch.getFile(file))))
                        || (!gh.unModified(spl, file) && !ch.contains(file)) || (!ch.unModified(spl, file) && !gh.contains(file))
                        || (!spl.contains(file) && !gh.unModified(ch, file))) {
                    mergeDifficult(ch, gh, file);
                    conflict = true;
                }
                new GitCommit(cbGitStage(), "Merged " + b.getName() + " into " + _cBranch.getName() + ".",
                        ch.getId().substring(0, 7), gh.getId().substring(0, 7));
            }
            if (conflict) {
                System.out.println("Encountered a merge conflict.");
            }
        }
    }


    /**
     * Changes FILE in working directory according
     * to conflicts in GHEAD and CHEAD.
     */
    public void mergeDifficult(GitCommit cHead, GitCommit gHead, String file) {
        String cStr = "";
        String gStr = "";
        Boolean hair = true;
        if (cHead.contains(file)) {
            cStr = Utils.readContentsAsString(cHead.getFile(file));
        }
        if (gHead.contains(file)) {
            gStr = Utils.readContentsAsString(gHead.getFile(file));
        }
        Utils.writeContents(new File(file), "<<<<<<< HEAD\n",
                cStr, "=======\n", gStr, ">>>>>>>\n");
    }

    /**
     * Since split, if FILE unchanged in current head but
     * changed in GHEAD, merge it into current head.
     */
    public void mergeSimple(GitCommit gHead, String file) {
        File file0 = new File(file);
        if (file0.exists()) {
            File file1 = new File(gHead.getCommitDirectory()
                    + gHead.getfileSHAs().get(file) + gHead.getSep() + file);
            Utils.writeContents(file0, Utils.readContentsAsString(file1));
        }
    }


    /**
     * Returns the ID of the split point of branch1 and branch2. Takes a
     * parameter to differentiate for the pull case, where the split point may
     * be non-existent, as compared to a local case, where the split point must
     * be initial commit if nothing else. Redundant now, but there for reference
     * purposes.
     */
    private GitCommit split(GitBranch branch1, GitBranch branch2) {
        ArrayList<GitCommit> ancestors1 = new ArrayList<>();
        ArrayList<GitCommit> ancestors2 = new ArrayList<>();
        GitCommit c1 = branch1.getLatestGitCommit();
        GitCommit c2 = branch2.getLatestGitCommit();
        if (c1 == null && c2 == null) {
            return _commitIDs.get(firstID);
        }
        int i = 0;
        int j = 0;
        for (GitCommit com = c1; ; com = com.getParent()) {
            if (com == null) {
                break;
            } else {
                ancestors1.add(com);
                i++;
            }
        }
        for (GitCommit com = c2; ; com = com.getParent()) {
            if (com == null) {
                break;
            } else {
                ancestors2.add(com);
                j++;
            }
        }
        ancestors1.retainAll(ancestors2);
        if (!ancestors1.isEmpty()) {
            return ancestors1.get(0);
        }
        return _commitIDs.get(firstID);
    }

    /**
     * Checks out FILE.
     */
    public void checkoutFile(String file) {
        if (!cbCom().contains(file)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        cbCom().checkout(file);
        cbGitStage().checkout(file);
    }

    /**
     * Checks out FILE from the commit with SHA-1 ID.
     */
    public void checkoutComID(String id, String file) {
        if (id == null || (!_commitIDs.containsKey(id) && !_abbID.containsKey(id))) {
            System.out.println("No commit with that id exists.");
            return;
        }
        if (id.length() == 8) {
            id = _abbID.get(id);
        }
        if (!_commitIDs.get(id).contains(file)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        (_commitIDs.get(id)).checkout(file);
        cbGitStage().checkout(file);
    }

    /**
     * Checks out BRANCH. Exits out if branch does not exist,
     * if it is the current branch, or if there are untracked files.
     */
    public void checkoutBranch(String branch) {
        if (!_branchNames.containsKey(branch)) {
            System.out.println("No such branch exists.");
            return;
        }
        if (cbName().equals(branch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        if (isUntracked()) {
            return;
        }
        for (String f0 : cbCom().getAddedFiles()) {
            String sha = cbCom().getfileSHAs().get(f0);
            File f = new File(f0);
            if (f.isFile()) {
                String fileStr = Utils.sha1(Utils.readContentsAsString(f));
                if (!_branchNames.get(branch).getLatestGitCommit().getAddedFiles().contains(f0)) {
                    Utils.restrictedDelete(f0);
                }
                if (sha.equals(fileStr)) {
                    Utils.restrictedDelete(f0);
                }
            }
        }
        _branchNames.get(branch).getLatestGitCommit().checkout();
        if (!cbName().equals(branch)) {
            cbGitStage().clearStage();
        }
        _cBranch = _branchNames.get(branch);
    }

    /**
     * Checks out all the files tracked by the commit with the given commit ID,
     * removes tracked files not present in that commit, and
     * moves the current branch's head to that commit node.
     */
    public void reset(String id) {
        if (id == null) {
            throw new ArrayIndexOutOfBoundsException();
        }
        if (!_commitIDs.containsKey(id) && !_abbID.containsKey(id)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        if (id.length() == 8) {
            id = _abbID.get(id);
        }
        GitCommit com = _commitIDs.get(id);
        if (isUntracked()) {
            return;
        }
        com.checkout();
        _cBranch.getGitStage().clearStage();
        _cBranch.setLatestGitCommit(com);
    }

    /**
     * Prints all existing branches, marks current branch with a *, and
     * displays Staged files and files marked for untracking.
     */
    public void status() {
        System.out.println("=== Branches ===");
        Object[] sortedB = _branchNames.keySet().toArray();
        Arrays.sort(sortedB);
        for (Object branch : sortedB) {
            if (branch.equals(cbName())) {
                System.out.println("*" + branch);
            } else {
                System.out.println(branch);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        Object[] sortedS = cbGitStage().getStagedFiles().toArray();
        Arrays.sort(sortedS);
        for (Object staged : sortedS) {
            System.out.println(staged);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        Object[] sortedR = cbGitStage().getToBeRemoved().toArray();
        Arrays.sort(sortedR);
        for (Object toRemove : sortedR) {
            System.out.println(toRemove);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();
        System.out.println("=== Untracked Files ===");
        Object[] sortedU = untracked.toArray();
        Arrays.sort(sortedU);
        for (Object untr : sortedU) {
            System.out.println(untr);
        }
        System.out.println();
    }

    /**
     * Returns ArrayList of ancestors of CommitID.
     */
    public ArrayList<String> getAncestors(String commitId) {
        ArrayList<String> par = new ArrayList<>();
        ArrayList<String> parM = new ArrayList<>();
        if (commitId.equals("")) {
            return par;
        }
        if (_commitIDs.containsKey(commitId)) {
            GitCommit current = _commitIDs.get(commitId);
            if (current == null) {
                String a;
            } else {
                if (current.isMerged()) {
                    parM = getAncestors(current.getSecParentS());
                }
            }
            for (String s : _idOfMsg.get("initial commit")) {
                if (current == _commitIDs.get(s)) {
                    return par;
                }
            }
            ArrayList<String> result1 = getAncestors(current.getParent().getId());
            par.add(commitId);
            par.addAll(result1);
        }
        if (!parM.isEmpty()) {
            par.addAll(parM);
        }
        return par;
    }

    /**
     * Returns the working directory's files.
     */
    public File[] wDFiles() {
        File fx = new File(cbCom().getCommitDirectory());
        int dLen = cbCom().getCommitDirectory().length();
        int abLen = fx.getAbsolutePath().length();
        String wPath = fx.getAbsolutePath().substring(0, abLen - dLen + 1);
        File wDir = new File(wPath);
        File[] fileList = wDir.listFiles();
        return fileList;
    }

    /** Returns the current branch.*/
    public GitBranch currentBranch() { return this._cBranch; }
    /** Returns the current branch's name.*/
    public String cbName() { return this.currentBranch().getName(); }
    /** Returns the current branch's latest commit.*/
    public GitCommit cbCom() { return this.currentBranch().getLatestGitCommit(); }
    /** Returns the current branch's stage.*/
    public GitStage cbGitStage() { return this.currentBranch().getGitStage(); }
    /** Returns Arraylist of untracked files.*/
    public ArrayList<String> getUntracked() { return untracked; }
    /** Returns Current Directory/GitTree.*/
    public GitTree curDir() { return this; }
    /** Returns the remote directory/GitTree of this GitTree.*/
    public Map<String, String> getRemoteDir() { return this.remoteDir; }
    /** Returns true if COM is an ancestor of SPLIT.*/
    public boolean isAncestor(GitCommit com, GitCommit split) {
        return com.equals(split);
    }
    /** Returns First SHA ID ever.*/
    private String getFirstID() { return firstID; }

    /** First SHA ID ever.*/
    private String firstID;
    /** Current GitBranch.*/
    private GitBranch _cBranch;
    /** Mapping of GitBranch names to their GitBranch.*/
    private Map<String, GitBranch> _branchNames;
    /** Abbreviated IDs. */
    private HashMap<String, String> _abbID;
    /** Mapping of commit messages to their commit's SHA-1 ID.*/
    private Map<String, ArrayList<String>> _idOfMsg;
    /** Mapping of SHA-1 ID's to their GitCommits.*/
    private Map<String, GitCommit> _commitIDs;
    /** Arraylist of untracked files.*/
    private ArrayList<String> untracked;
    /** the remote directory/GitTree.*/
    private Map<String, String> remoteDir;

}
