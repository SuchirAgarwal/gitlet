package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Suchir Agarwal
 */
public class Main {
    /** My repository. */
    private static Repo r;

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        File repoObject = new File("./.gitlet/repoObject");
        try {
            r = Utils.readObject(repoObject, Repo.class);
        } catch (Exception ignored) {
            r = null;
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
            System.out.println("Invalid command.");
            break;
        }
    }
}

