package gitlet;


import java.io.File;
import java.util.ArrayList;
import java.io.Serializable;
import java.util.Arrays;

/** Class defining the staging area of Gitlet.
 * @author aarini
 */
class GitStage implements Serializable {

    /** GitStage Constructor using LATEST commit.*/
    public GitStage(GitCommit latest) {
        latestCom = latest;
        toRemove = new ArrayList<String>();
        latestStaged = new ArrayList<String>();
        _stgdFiles = new ArrayList<String>();
        if (latest.getAddedFiles() != null) {
            _stgdFiles.addAll(latest.getAddedFiles());
        }
    }

    /** Returns true if FILE has not been modified since previous commit.*/
    public boolean unChanged(String file) {
        String last = null;
        File f = new File(file);
        if (f.isFile()) {
            String curSHA = Utils.sha1(Utils.readContentsAsString(f));
            if (latestCom.contains(file)) {
                last = latestCom.getfileSHAs().get(file);
            }
            if ((curSHA).equals(last)) {
                return true;
            }
        }
        return false;
    }


    /** Returns true if the directory has untracked files with ref to a certain commit.*/
    public boolean isUntrackedFiles(GitCommit com) {
        File fx = new File(com.getCommitDirectory());
        int dLen = com.getCommitDirectory().length();
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

                if (!latestStaged.contains(name)) {
                    if (com.contains(name)) {
                        String str = Utils.sha1(Utils.readContentsAsString(file0));
                        String sha = com.getfileSHAs().get(name);
                        if (!sha.equals(str)) {
                            com.getComUntracked().add(name);
                            return true;
                        }
                    } else {
                        com.getComUntracked().add(name);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Checks out FILE.*/
    public void checkout(String file) {
        _stgdFiles.remove(file);
        latestStaged.remove(file);
        toRemove.remove(file);
    }

    /** Removes FILE from staging area, marks it to not to be tracked
     * in the next commit if it is in the current commit, removes
     * it from working directory if not already removed by user.*/
    public void remove(String file) {
        if (!(latestStaged.contains(file) || _stgdFiles.contains(file) || this.latestCom.contains(file))) {
            System.out.println(" No reason to remove the file.");
        } else {
            if (this.latestCom.contains(file)) {
                toRemove.add(file);
                latestCom.getAddedFilesMap().remove(file);
                Utils.restrictedDelete(file);
            }
            if (Arrays.asList(latestCom.wDFiles()).contains(file)) {
                Utils.restrictedDelete(file);
            }
            latestStaged.remove(file);
            _stgdFiles.remove(file);
        }
    }

    /** Adds FILE to be staged if it has been modified and exists, and
     * deletes the to be removed mark if it had been marked as such.*/
    public void add(String file) {
        File f0 = new File(file);
        if (!(f0).exists()) {
            System.out.println("File does not exist.");
            return;
        }
        String sha = Utils.sha1(Utils.readContentsAsString(f0));
        if (sha.equals(latestCom.getfileSHAs().get(file)) || unChanged(file)) {
            _stgdFiles.remove(file);
            latestStaged.remove(file);
        } else {
            latestStaged.add(file);
            _stgdFiles.add(file);
        }
        if (toRemove.contains(file)) {
            toRemove.remove(file);
        }

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== Staged Files ===");
        for (String file : latestStaged) {
            sb.append(file).append("\n");
        }
        sb.append("\n=== Removed Files ===");
        for (String file : toRemove) {
            sb.append(file).append("\n");
        }
        return sb.toString();
    }

    /** Reads in bytes from File1 and File2. Returns true if contents are equal. */
    private boolean fileEquals(File file1, File file2) {
        if (file1 != null && file2 != null) {
            byte[] fp1 = Utils.readContents(file1);
            byte[] fp2 = Utils.readContents(file2);
            return Arrays.equals(fp1, fp2);
        }
        return false;
    }

    /** Returns the newest commit.*/
    public GitCommit getlatestCom() { return latestCom; }
    /** Returns Arraylist of the already staged files.*/
    public ArrayList<String> getStagedFiles() { return _stgdFiles; }
    /** Returns Arraylist of new files just added to stage.*/
    public ArrayList<String> getLatestStaged() { return latestStaged; }
    /** Returns list of files to be removed. */
    public ArrayList<String> getToBeRemoved() { return toRemove; }
    /** Clears the stage.*/
    public void clearStage() {
        _stgdFiles.clear();
        latestStaged.clear();
        toRemove.clear();
    }

    /** The latest/newest commit.*/
    private GitCommit latestCom;
    /** Arraylist of the new files just added to stage. Cleared after each commit*/
    private ArrayList<String> latestStaged;
    /** List of files to not be tracked in the next GitCommit.*/
    private ArrayList<String> toRemove;
    /** Arraylist of already staged files.*/
    private ArrayList<String> _stgdFiles;
}
