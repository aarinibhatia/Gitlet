package gitlet;

import java.io.*;
import java.text.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

/** Class describing a Gitlet Commit.
 * @author aarini
 */
public class GitCommit implements Serializable {

    /** GitCommit constructor, using information from STAGE.*/
    public GitCommit(GitStage stage) {
        comStage = stage;
        addedFiles = new HashMap<>();
        fileSHAs = new HashMap<>();
        if (stage != null) {
            time = new Date(); parent = stage.getlatestCom();
        } else {
            Date date = null;
            String dateStr = "Wed Dec 31 16:00:00 1969 -0800";
            SimpleDateFormat dateF = new SimpleDateFormat(
                    "EEE MMM d HH:mm:ss yyyy Z");
            try {
                date = dateF.parse(dateStr);
            } catch (ParseException p) {
                System.out.println(p.getMessage());
            }
            time = date;
        }

        if (parent != null) {
            addedFiles.putAll(parent.getAddedFilesMap());
            if (stage.getToBeRemoved() != null) {
                for (String f : stage.getToBeRemoved()) {
                    addedFiles.remove(f);
                }
            }
            fileSHAs.putAll(parent.fileSHAs);
        }
        if (stage != null) {
            if (stage.getLatestStaged() != null) {
                for (String f : stage.getLatestStaged()) {
                    File f0 = new File(f);
                    if ((f0).exists()) {
                        addedFiles.put(f, Utils.sha1(Utils.readContentsAsString(f0)));
                        fileSHAs.put(f, Utils.sha1(Utils.readContentsAsString(f0)));
                    }
                }
            }
            if (stage.getStagedFiles() != null) {
                for (String f : stage.getStagedFiles()) {
                    File f0 = new File(f);
                    if ((f0).exists()) {
                        addedFiles.put(f, Utils.sha1(Utils.readContentsAsString(f0)));
                        fileSHAs.put(f, Utils.sha1(Utils.readContentsAsString(f0)));
                    }
                }
            }
        }
        isMerged = false;
    }

    /** GitCommit constructor, with commit message MESSAGE,
     * using info from STAGE.*/
    public GitCommit(GitStage stage, String message) {
        this(stage); msg = message;
        Boolean tru = false;
        if (stage != null) {
            id = Utils.sha1(stage.getLatestStaged().toString(),
                    getParent().getId(), getMsg(), getTime().toString());
            tru = !stage.getToBeRemoved().isEmpty();
        } else {
            id = Utils.sha1(getTime().toString(), getMsg());
        }
        commitToDirectory(stage);
        if (!unchangedCommit() || tru) {
            comDir = ".gitlet" + seper + "objects" + seper + getId() + seper;
            createDir();
        } else if (stage.getLatestStaged() == null && stage.getToBeRemoved() == null) {
            System.out.println("No changes added to the commit.");
        }
    }

    /** GitCommit constructor for merge commits, using info from STAGE,
     * with commit message MESSAGE, and parents FPAR and SPAR.*/
    public GitCommit(GitStage stage, String message, String fPar, String sPar) {
        this(stage, message);
        isMerged = true;
        parents = "Merge: " + fPar + " " + sPar;
        parentS = fPar;
        secParentS = sPar;
    }

    /** Returns true if this commit contains FILE.*/
    public boolean contains(String file) {
        return (addedFiles != null && addedFiles.containsKey(file));
    }
    /** Returns file with name FILE. */
    public File getFile(String file) {
        return new File(comDir + fileSHAs.get(file) + seper + file);
    }

    /** Returns true if FILE has not been modified between
     * HEAD commit and this commit.*/
    public boolean unModified(GitCommit head, String file) {
        return (this.fileSHAs.containsKey(file)
                && (head.getfileSHAs()).containsKey(file))
                && (((head.getfileSHAs().get(file)).
                equals(this.fileSHAs.get(file)))
                || (this.getFile(file).isFile() && head.getFile(file).isFile()
                && ((Utils.readContentsAsString(this.getFile(file))).
                equals(Utils.readContentsAsString(head.getFile(file))))));
    }

    /** Returns true if current commit is the same as its parent.*/
    public boolean unchangedCommit() {
        if (parent != null) {
            Set pFls = parent.getAddedFiles();
            Set cFls = this.getAddedFiles();
            if (cFls.size() == pFls.size()) {
                for (String name : addedFiles.keySet()) {
                    if (pFls.contains(name)) {
                        if (!fileSHAs.get(name).
                                equals(parent.getfileSHAs().get(name))) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /** Checks out FILE.*/
    public void checkout(String file) {
        File f = new File(file);
        File f0 = new File(comDir + fileSHAs.get(file) + seper + f);
        try {
            Utils.writeContents(f, Utils.readContentsAsString(f0));
        } catch (GitletException e) {
            System.out.println(e.getMessage());
        }
    }

    /** Checks out the current commit.*/
    public void checkout() {
        Set<String> added = this.addedFiles.keySet();
        for (String a : added) {
            checkout(a);
        }
        String cP = (new File(comDir)).getAbsolutePath();
        File[] f0 = (new File(cP.substring(0,
                cP.length() - comDir.length() + 1))).listFiles();
        if (f0 != null) {
            for (File f: f0) {
                if (!added.contains(f.getName())) {
                    Utils.restrictedDelete(f);
                }
            }
        }
    }

    /** Creates a directory, saving the files.*/
    public void createDir() {
        (new File(comDir)).mkdirs();
        if (addedFiles.isEmpty()) {
            return;
        }
        for (String name : addedFiles.keySet()) {
            File f = new File(name);
            Path p = Paths.get(comDir + fileSHAs.get(name) + seper + name);
            try {
                Files.createDirectories(p.getParent());
                Files.createFile(p);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            if (f.isFile()) {
                Utils.writeContents(p.toFile(), Utils.readContentsAsString(f));
                addedFiles.put(name, comDir);
            }
        }
    }

    /** Adds files to be tracked, from STAGE, to the new commit.*/
    public void commitToDirectory(GitStage stage) {
        if (stage == null) {
            return;
        } else {
            for (String file : stage.getStagedFiles()) {
                String id1 = stage.getlatestCom().getfileSHAs().get(file);
                File f = new File(file);
                if (f.isFile()) {
                    String id0 = Utils.sha1(Utils.readContentsAsString(f));
                    if (fileSHAs.containsKey(file)) {
                        if (!fileSHAs.get(file).equals(id0)) {
                            addedFiles.put(file, id);
                            fileSHAs.replace(file, id0);
                        }
                    } else {
                        addedFiles.put(file, id);
                        fileSHAs.put(file, id0);
                    }
                } else {
                    continue;
                }
            }
        }
    }

    /**
     * Returns the working directory's files.
     */
    public File[] wDFiles() {
        File fx = new File(comDir);
        int dLen = comDir.length();
        int abLen = fx.getAbsolutePath().length();
        String wPath = fx.getAbsolutePath().substring(0, abLen - dLen + 1);
        File wDir = new File(wPath);
        File[] fileList = wDir.listFiles();
        return fileList;
    }

    @Override
    public String toString() {
        StringBuilder strBldr = new StringBuilder();
        strBldr.append("=== \n").append("commit ").append(id).append(" \n");
        if (isMerged) {
            strBldr.append(parents).append("\n");
        }
        SimpleDateFormat dtForm = new SimpleDateFormat(
                "EEE MMM d HH:mm:ss yyyy Z");
        return (strBldr.append("Date: ").append(dtForm.
                format(time)).append(" \n").append(msg)).toString();
    }

    /** Returns SHA ID of the commit.*/
    public String getId() {
        return id;
    }
    /** Returns true if commit IsMerged.*/
    public Boolean isMerged() {
        return isMerged;
    }
    /** Returns commit message.*/
    public String getMsg() {
        return msg;
    }
    /** Returns commit time. */
    public Date getTime() {
        return time;
    }
    /** Returns commit's parent.*/
    public GitCommit getParent() {
        return parent;
    }
    /** Returns commit's parent.*/
    public String getParentS() {
        return parentS;
    }
    /** Returns commit's 2nd parent.*/
    public String getSecParentS() {
        return secParentS;
    }
    /** Returns directory of the commit.*/
    public String getCommitDirectory() {
        return comDir;
    }
    /** Sets directory of the commit.*/
    public void setCommitDirectory(String t) {
        comDir = t;
    }
    /** Returns File IDs. */
    public Map<String, String> getfileSHAs() {
        return fileSHAs;
    }
    /** Returns file names.*/
    public Set<String> getAddedFiles() {
        return addedFiles.keySet();
    }
    /** Returns file names map.*/
    public Map<String, String> getAddedFilesMap() {
        return addedFiles;
    }
    /** Returns separator.*/
    public String getSep() {
        return seper;
    }
    /** Set untracked files to SET.*/
    public void setComUntracked(ArrayList<String> set) {
        comUntracked = set;
    }
    /** Returns untracked files.*/
    public ArrayList<String> getComUntracked() {
        return comUntracked;
    }

    /** Commit message.*/
    private String msg;
    /** Commit time.*/
    private Date time;
    /** SHA ID of commit.*/
    private String id;
    /** Directory the commit is in. */
    private String comDir;
    /** Parent of current commit.*/
    private GitCommit parent;
    /** Parent of current commit Str.*/
    private String parentS;
    /** 2nd Parent of merge  commit Str.*/
    private String secParentS;
    /** Merge parent IDs for print.*/
    private String parents;
    /** Separator for the system.*/
    private String seper = File.separator;
    /**True if commit is a merge.*/
    private boolean isMerged;
    /** Mapping of file names to their SHA IDs.*/
    private Map<String, String> fileSHAs;
    /** Mapping of added files' name to its commit SHA ID.*/
    private Map<String, String> addedFiles;
    /** Arraylist of untracked files.*/
    private ArrayList<String> comUntracked;
    /** Commit Stage.*/
    private GitStage comStage;


}
