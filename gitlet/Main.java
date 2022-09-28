package gitlet;

import java.io.ObjectInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.ObjectOutputStream;
import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author aarini
 */
public class Main {

    /** Main function, gets input arguments ARGS.*/
    public static void main(String... args) {
        if (!isNull(args)) {
            operand = args[0];
            setArgs(args);
            GitTree dir = start();
            try {
                switch (operand) {
                    case "init":
                        dir = init();
                        break;
                    case "add":
                        dir.add(arg1);
                        break;
                    case "rm":
                        dir.remove(arg1);
                        break;
                    case "branch":
                        dir.addBranch(arg1);
                        break;
                    case "rm-branch":
                        dir.removeBranch(arg1);
                        break;
                    case "merge":
                        dir.merge(arg1);
                        break;
                    case "commit":
                        dir.commit(arg1);
                        break;
                    case "checkout":
                        checkout(dir, args);
                        break;
                    case "find":
                        dir.find(arg1);
                        break;
                    case "log":
                        dir.log();
                        break;
                    case "global-log":
                        dir.printGlobalLog();
                        break;
                    case "status":
                        dir.status();
                        break;
                    case "reset":
                        dir.reset(arg1);
                        break;
                    default:
                        System.out.println("No command with that name exists.");
                        break;
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                System.out.println("Incorrect operands.");
            } catch (NullPointerException a) {
                System.out.println("Not in an initialized Gitlet directory.");
            }
            save(dir);
        }
    }

    /** Returns an new Gitlet version-control system in the current directory,
     * if one doesn't already exist.*/
    private static GitTree init() {
        File direc = new File(".gitlet");
        if (direc.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            return null;
        } else {
            direc.mkdirs();
            return GitTree.init();
        }
    }

    /** Returns the Gitlet version-control system to run commands on.*/
    private static GitTree start() {
        File file0 = new File(".gitlet" + seper + "path");
        GitTree tree = null;
        if (file0.exists()) {
            try {
                ObjectInputStream input = new ObjectInputStream(new FileInputStream(file0));
                tree = (GitTree) input.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return tree;
    }

    /** Returns true if ARGS given in to the system are null.*/
    public static boolean isNull(String... args) {
        boolean result = false;
        if (args == null || args.length == 0) {
            System.out.println("Please enter a command.");
            result = true;
        }
        return result;
    }

    /** Sets arg1 and arg2 to the input ARGS.*/
    public static void setArgs(String... args) {
        if (args.length >= 2) {
            arg1 = args[1];
        }
        if (args.length >= 3) {
            arg2 = args[2];
        }
    }

    /** Saves the TREE, so it can be used again.*/
    private static void save(GitTree tree) {
        if (tree != null) {
            try {
                (new ObjectOutputStream(new FileOutputStream(
                        new File(".gitlet" + seper + "path")))).writeObject(tree);
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /** Checks out ARGS from TREE.*/
    private static void checkout(GitTree tree, String... args) {
        try {
            if (args.length == 3) {
                if (args[1].equals("--")) {
                    arg1 = args[2];
                    tree.checkoutFile(arg1);
                    return;
                }
            } else if (args.length == 2) {
                arg1 = args[1];
                tree.checkoutBranch(arg1);
                return;
            } else if (args.length == 4) {
                if (args[2].equals("--")) {
                    arg1 = args[1];
                    arg2 = args[3];
                    tree.checkoutComID(arg1, arg2);
                    return;
                }
            }
            throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) {
            System.out.println("Incorrect Operands.");
        }
    }

    /** Separator.*/
    private static String seper = File.separator;
    /** First operand input.*/
    private static String operand;
    /** Extra argument 1.*/
    private static String arg1;
    /** Extra argument 2.*/
    private static String arg2;
}
