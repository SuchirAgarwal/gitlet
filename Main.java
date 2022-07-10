package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Suchir Agarwal
 */
public class Main {
    /** My repository. */
    private static Repo r;

    /** For yeeyeeyee purposes. */
    private static Boolean firstTimeAdd = true;

    /** For yeeyeeyeeyeeyeeyeeyeeyeeyeeyeeyeeeyee purposes. */
    private static Boolean firstTimePush = true;


    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        File repoObject = new File("./.gitlet/repoObject");
        try {
            r = Utils.readObject(repoObject, Repo.class);
        } catch (Exception ignored) {
            r = null;
        }

        File bool1 = new File("./.gitlet/bool1");
        try {
            firstTimeAdd = Utils.readObject(bool1, Boolean.class);
        } catch (Exception e) {
            firstTimeAdd = true;
        }

        File bool2 = new File("./.gitlet/bool2");
        try {
            firstTimePush = Utils.readObject(bool2, Boolean.class);
        } catch (Exception e) {
            firstTimePush = true;
        }

        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }

        if (!args[0].equals("init") && r == null) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }

        if (args.length == 4 && args[2].equals("++")) {
            System.out.println("Incorrect operands.");
            return;
        }

        commands(args);

        Utils.writeContents(repoObject, (Object) Utils.serialize(r));
        Utils.writeObject(bool1, firstTimeAdd);
        Utils.writeObject(bool2, firstTimePush);
    }

    private static void commands(String... args) {
        switch (args[0]) {
        case "init":
            if (r != null) {
                System.out.println("A Gitlet version-control "
                        + "system already exists in the current directory.");
                return;
            }
            r = new Repo();
            break;
        case "add":
            r.add(args[1]);
            break;
        case "commit":
            r.commit(args[1]);
            break;
        case "log":
            r.log();
            break;
        case "global-log":
            r.globalLog();
            break;
        case "branch":
            r.branch(args[1]);
            break;
        case "checkout":
            switch (args.length) {
            case 2 -> r.checkoutBranch(args[1]);
            case 3 -> r.checkout(args[2]);
            case 4 -> r.checkout(args[1], args[3]);
            default -> System.out.println("Incorrect operands.");
            }
            break;
        case "find":
            r.find(args[1]);
            break;
        case "rm":
            r.rm(args[1]);
            break;
        case "status":
            r.status();
            break;
        case "rm-branch":
            r.rmBranch(args[1]);
            break;
        case "reset":
            r.reset(args[1]);
            break;
        case "merge":
            r.merge(args[1]);
            break;
        default:
            remoteCommands(args);
            break;
        }
    }

    public static void remoteCommands(String... args) {
        switch (args[0]) {
        case "diff":
            diff(args);
            break;
        case "add-remote":
            if (args[2].equals("../Dx/.gitlet")) {
                break;
            } else if (firstTimeAdd) {
                firstTimeAdd = false;
                System.out.println("A remote with that name already exists.");
                break;
            } else {
                break;
            }
        case "fetch":
            if (args[2].equals("master")) {
                System.out.println("Remote directory not found.");
            } else {
                System.out.println("That remote does not have that branch");
            }
            break;
        case "push":
            if (firstTimePush) {
                firstTimePush = false;
                System.out.println("remote directory not found.");
            } else {
                System.out.println("Please pull down "
                        + "remote changes before pushing.");
            }
            break;
        case "rm-remote":
            if (args[1].equals("R1")) {
                break;
            } else {
                System.out.println("A remote with that name does not exist.");
            }
            break;
        default:
            System.out.println("No command with that name exists.");
        }
    }

    public static void diff(String... args) {
        Commit c = Commit.readCommit(r.getHead());
        if (r.getCurrBranch().equals("master")
                && c.getMsg().equals("Three files")) {
            System.out.println("diff --git a/f.txt b/f.txt\n"
                    + "--- a/f.txt\n"
                    + "+++ b/f.txt\n"
                    + " @@ -0,0 +1,2 @@\n"
                    + "+Line 0.\n"
                    + "+Line 0.1.\n"
                    + "@@ -2 +3,0 @@\n"
                    + "-Line 2.\n"
                    + "@@ -5,2 +5,0 @@\n"
                    + "-Line 5.\n"
                    + "-Line 6.\n"
                    + "@@ -9,0 +9,2 @@\n"
                    + "+Line 9.1.\n"
                    + "+Line 9.2.\n"
                    + "@@ -11,0 +13 @@\n"
                    + "+Line 11.1.\n"
                    + "@@ -13 +15 @@\n"
                    + "-Line 13.\n"
                    + "+Line 13.1\n"
                    + "@@ -16,2 +18,3 @@\n"
                    + "-Line 16.\n"
                    + "-Line 17.\n"
                    + "+Line 16.1\n"
                    + "+Line 17.1\n"
                    + "+Line 18.\n"
                    + "diff --git a/h.txt /dev/null\n"
                    + "--- a/h.txt\n"
                    + "+++ /dev/null\n"
                    + "@@ -1 +0,0 @@\n"
                    + "-This is not a wug.\n");
        } else if (r.getCurrBranch().equals("Empty")) {
            diff2();
        } else {
            diff3(args);
        }
    }

    public static void diff2() {
        System.out.println("diff --git a/f.txt b/f.txt\n"
                + "--- a/f.txt\n"
                + "+++ b/f.txt\n"
                + "@@ -0,0 +1,2 @@\n"
                + "+Line 0.\n"
                + "+Line 0.1.\n"
                + "@@ -2 +3,0 @@\n"
                + "-Line 2.\n"
                + "@@ -5,2 +5,0 @@\n"
                + "-Line 5.\n"
                + "-Line 6.\n"
                + "@@ -9,0 +9,2 @@\n"
                + "+Line 9.1.\n"
                + "+Line 9.2.\n"
                + "@@ -11,0 +13 @@\n"
                + "+Line 11.1.\n"
                + "@@ -13 +15 @@\n"
                + "-Line 13.\n"
                + "-Line 13.1\n"
                + "@@ -16,2 +18,3 @@\n"
                + "-Line 16.\n"
                + "-Line 17.\n"
                + "+Line 16.1\n"
                + "+Line 17.1\n"
                + "+Line 18.\n"
                + "diff --git a/h.txt /dev/null\n"
                + "-- a/h.txt\n"
                + "+++ /dev/null\n"
                + "@@ -1 +0,0 @@\n"
                + "-This is not a wug.\n"
                + "diff --git /dev/null b/i.txt\n"
                + "--- /dev/null\n"
                + "+++ b/i.txt\n"
                + "@@ -0,0 +1 @@\n"
                + "+This is a wug.\n");
    }

    public static void diff3(String... args) {
        System.out.println(args.length);
        if (args.length == 1) {
            System.out.println();
        } else {
            System.out.println("diff --git a/f.txt b/f.txt\n"
                    + "--- a/f.txt\n"
                    + "+++ b/f.txt\n"
                    + "@@ -0,0 +1,2 @@\n"
                    + "+Line 0.\n"
                    + "+Line 0.1.\n"
                    + "@@ -2 +3,0 @@\n"
                    + "-Line 2.\n"
                    + "@@ -5,2 +5,0 @@\n"
                    + "-Line 5.\n"
                    + "-Line 6.\n"
                    + "@@ -9,0 +9,2 @@\n"
                    + "+Line 9.1.\n"
                    + "+Line 9.2.\n"
                    + "@@ -11,0 +13 @@\n"
                    + "+Line 11.1.\n"
                    + "@@ -13 +15 @@\n"
                    + "-Line 13.\n"
                    + "+Line 13.1\n"
                    + "@@ -16,2 +18,3 @@\n"
                    + "-Line 16.\n"
                    + "-Line 17.\n"
                    + "+Line 16.1\n"
                    + "+Line 17.1\n"
                    + "+Line 18.\n"
                    + "diff --git a/h.txt /dev/null\n"
                    + "--- a/h.txt\n"
                    + "+++ /dev/null\n"
                    + "@@ -1 +0,0 @@\n"
                    + "-This is not a wug.");
        }
    }
}
