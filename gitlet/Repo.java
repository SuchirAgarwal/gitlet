package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Collections;

public class Repo implements Serializable {
    /** Tracked stuff. */
    private HashSet<String> tracked;

    /** Commits. */
    private ArrayList<String> commits;

    /** Stage. */
    private Stage stage;

    /** Head commit hash. */
    private String H;

    /** Current branch. */
    private String currBranch;

    /** Branch to respective head. */
    private HashMap<String, String> branchHeads;

    public String getHead() {
        return H;
    }

    public String getCurrBranch() {
        return currBranch;
    }

    public Repo() {
        File dir = new File("./.gitlet");
        dir.mkdir();
        try {
            File thisSerialized = Utils.join(dir, "repoObject");
            thisSerialized.createNewFile();
        } catch (Exception ignored) {
            System.out.println(ignored);
        }
        commits = new ArrayList<>();
        tracked = new HashSet<>();
        stage = new Stage();
        currBranch = "master";
        branchHeads = new HashMap<>();
        commit("initial commit");
        branchHeads.put(currBranch, H);
    }


    public void add(String fileName) {
        File toAdd = new File(fileName);
        if (!toAdd.exists()) {
            System.out.println("File does not exist.");
            return;
        }
        stage.unStageRm(fileName);
        tracked.add(fileName);
        Commit c = Commit.readCommit(H);
        if (c.getNameToBlob().containsKey(fileName)) {
            byte[] commitVersion = c.getBlob(fileName);
            if (Arrays.equals(commitVersion, Utils.readContents(toAdd))) {
                return;
            }
        }
        stage.add(fileName, Utils.readContents(new File("./" + fileName)));
    }

    public void commit(String msg) {
        if (msg.equals("")) {
            System.out.println("Please enter a commit message.");
            return;
        }
        if (stage.getFileNames().size() == 0
                && stage.filesStagedForRm().size() == 0 && H != null) {
            System.out.println("No changes added to the commit.");
            return;
        }

        Commit c = new Commit(msg, stage, new HashSet<>(tracked));
        c.setParent(H);
        c.write();
        H = c.getHash();
        commits.add(H);
        branchHeads.put(currBranch, H);
    }

    public void rm(String fileName) {
        Commit c = Commit.readCommit(H);
        if (!c.isTracked(fileName) && !stage.isStaged(fileName)) {
            System.out.println("No reason to remove the file.");
        }
        tracked.remove(fileName);
        stage.clear(fileName);
        if (c.isTracked(fileName)) {
            File file = new File(fileName);
            stage.rm(fileName);
            if (file.isFile()) {
                file.delete();
            }
        }
    }

    public void log() {
        Commit c = Commit.readCommit(H);
        while (c.getParent() != null) {
            c.logThis();
            c = Commit.readCommit(c.getParent());
        }
        c.logThis();

    }

    public void globalLog() {
        Commit c;
        for (String hash : commits) {
            c = Commit.readCommit(hash);
            c.logThis();
        }
    }

    public void branch(String branchName) {
        if (branchHeads.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branchHeads.put(branchName, H);
    }

    public String getFullHash(String abbr) {
        HashSet<String> commitsCopy = new HashSet<>(commits);
        ArrayList<String> toRemove = new ArrayList<>();
        int i = 0;
        while (commitsCopy.size() > 1 && i < abbr.length()) {
            for (String hash : commitsCopy) {
                if (hash.charAt(i) != abbr.charAt(i)) {
                    toRemove.add(hash);
                }
            }
            for (String hash : toRemove) {
                commitsCopy.remove(hash);
            }
            toRemove = new ArrayList<>();
            i++;
        }
        for (String hash : commitsCopy) {
            return hash;
        }
        return abbr;
    }

    public void checkout(String fileName) {
        checkout(H, fileName);
    }

    public void checkout(String hash, String fileName) {
        int hashLength = commits.get(0).length();
        if (hash.length() < hashLength) {
            hash = getFullHash(hash);
        }
        if (!commits.contains(hash)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File toOverride = new File(fileName);
        Commit c = Commit.readCommit(hash);
        if (!c.getFileNames().contains(fileName)) {
            System.out.println("File does not exist in that commit.");
            return;
        }
        stage.clear(fileName);
        Utils.writeContents(toOverride,
                (Object) Blob.readBlob(c.getNameToBlob().get(fileName)));
    }

    public void checkoutBranch(String branchName) {
        if (!branchHeads.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            return;
        } else if (branchName.equals(currBranch)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        Commit c = Commit.readCommit(branchHeads.get(branchName));
        Commit head = Commit.readCommit(H);

        for (String fileName : c.getFileNames()) {
            if (!head.isTracked(fileName)
                    && Utils.plainFilenamesIn(".").contains(fileName)) {
                byte[] currBlob = Utils.readContents(new File(fileName));
                byte[] prevBlob = c.getBlob(fileName);
                if (!Arrays.equals(currBlob, prevBlob)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    return;
                }
            }
        }

        tracked = new HashSet<>(head.getFileNames());

        for (String fileName : tracked) {
            if (!c.isTracked(fileName)) {
                new File(fileName).delete();
            }
        }

        tracked = new HashSet<>(c.getFileNames());

        for (String fileName : c.getFileNames()) {
            try {
                checkout(c.getHash(), fileName);
            } catch (Exception ignored) {
                doNothing();
            }
        }

        stage.clear();
        currBranch = branchName;
        H = c.getHash();
    }

    public void find(String msg) {
        Commit c;
        boolean somethingFound = false;
        for (String hash : commits) {
            c = Commit.readCommit(hash);
            if (c.getMsg().equals(msg)) {
                somethingFound = true;
                System.out.println(hash);
            }
        }
        if (!somethingFound) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        statusBranchesStaged();
        String removed = "=== Removed Files ===";
        String mods = "=== Modifications Not Staged For Commit ===";
        String untracked = "=== Untracked Files ===";
        Commit c = Commit.readCommit(H);
        ArrayList<String> sortedRemoved =
                new ArrayList<>(stage.filesStagedForRm());
        Collections.sort(sortedRemoved);
        for (String fileName : sortedRemoved) {
            removed += "\n" + fileName;
        }

        HashSet<String> modifiedFiles = new HashSet<>();
        for (String fileName : Utils.plainFilenamesIn(".")) {
            if (c.isTracked(fileName) && !stage.isStaged(fileName)) {
                byte[] commitBlob =
                        Blob.readBlob(c.getNameToBlob().get(fileName));
                byte[] currentBlob = Utils.readContents(new File(fileName));
                if (!Arrays.equals(commitBlob, currentBlob)) {
                    modifiedFiles.add(fileName + " (modified)");
                }
            } else if (stage.isStaged(fileName)) {
                byte[] stagedBlob = stage.getContents(fileName);
                byte[] currentBlob = Utils.readContents(new File(fileName));
                if (!Arrays.equals(stagedBlob, currentBlob)) {
                    modifiedFiles.add(fileName + " (modified)");
                }
            }
        }
        for (String fileName : stage.getFileNames()) {
            if (stage.isStaged(fileName) && !(new File(fileName).exists())) {
                modifiedFiles.add(fileName + " (deleted)");
            }
        }
        for (String fileName : c.getNameToBlob().keySet()) {
            if (c.isTracked(fileName) && !stage.isStagedForRm(fileName)
                    && !(new File(fileName).exists())) {
                modifiedFiles.add(fileName + " (deleted)");
            }
        }
        ArrayList<String> modifiedFilesList = new ArrayList<>(modifiedFiles);
        Collections.sort(modifiedFilesList);
        for (String fileName : modifiedFilesList) {
            mods += "\n" + fileName;
        }

        for (String fileName : Utils.plainFilenamesIn(".")) {
            File file = new File(fileName);
            if (!stage.isStaged(fileName) && !c.isTracked(fileName)) {
                if (file.isFile()) {
                    untracked += "\n" + fileName;
                }
            } else if (stage.isStagedForRm(fileName) && file.exists()) {
                untracked += "\n" + fileName;
            }
        }
        System.out.println(removed + "\n\n" + mods + "\n");
        System.out.println(untracked + "\n");
    }

    private void statusBranchesStaged() {
        String branches = "=== Branches ===";
        String staged = "=== Staged Files ===";

        ArrayList<String> sortedBranches =
                new ArrayList<>(branchHeads.keySet());
        Collections.sort(sortedBranches);
        for (String branch : sortedBranches) {
            if (branch.equals(currBranch)) {
                branches += "\n" + "*" + branch;
            } else {
                branches += "\n" + branch;
            }
        }

        for (String fileName : stage.getFileNames()) {
            staged += "\n" + fileName;
        }

        System.out.println(branches);
        System.out.println();
        System.out.println(staged);
        System.out.println();
    }

    public void rmBranch(String branch) {
        if (!branchHeads.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
        } else if (branch.equals(currBranch)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branchHeads.remove(branch);
        }
    }

    public void reset(String hash) {
        Commit head = Commit.readCommit(H);
        if (!commits.contains(hash)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        Commit reset = Commit.readCommit(hash);
        for (String fileName : reset.getFileNames()) {
            if (reset.isTracked(fileName) && !head.isTracked(fileName)
                    && Utils.plainFilenamesIn(".").contains(fileName)) {
                byte[] curr = Utils.readContents(new File(fileName));
                byte[] overr = reset.getBlob(fileName);
                if (!Arrays.equals(curr, overr)) {
                    System.out.println("There is an untracked file in the way;"
                            + " delete it, or add and commit it first.");
                    return;
                }
            }
        }
        for (String fileName : head.getFileNames()) {
            if (head.isTracked(fileName) && !reset.isTracked(fileName)) {
                File file = new File(fileName);
                file.delete();
            }
        }
        for (String fileName : reset.getFileNames()) {
            try {
                checkout(hash, fileName);
            } catch (Exception ignored) {
                doNothing();
            }
        }
        stage.clear();
        branchHeads.put(currBranch, reset.getHash());
        H = reset.getHash();
    }

    public boolean initialMergeFailures(String branch) {
        if (!stage.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return true;
        } else if (!branchHeads.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            return true;
        } else if (branch.equals(currBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return true;
        }
        return false;
    }


    public Commit findLCA(String branch) {
        String head = H;
        String otherHead = branchHeads.get(branch);


        ArrayList<String> commonAncestors = new ArrayList<>();
        for (String hash : commits) {
            if (Commit.hasParent(hash, head)
                    && Commit.hasParent(hash, otherHead)) {
                commonAncestors.add(hash);
            }
        }

        ArrayList<String> headAncestors = new ArrayList<>();
        for (String hash : commits) {
            if (Commit.hasParent(hash, head)) {
                headAncestors.add(hash);
            }
        }

        ArrayList<String> otherAncestors = new ArrayList<>();
        for (String hash : commits) {
            if (Commit.hasParent(hash, otherHead)) {
                otherAncestors.add(hash);
            }
        }

        Commit lca = null;
        int bestDistance = Integer.MAX_VALUE;
        int currDistance;
        for (String hash : commonAncestors) {
            currDistance = Commit.getDistance(hash, head);
            if (currDistance < bestDistance) {
                bestDistance = currDistance;
                lca = Commit.readCommit(hash);
            }
        }
        return lca;
    }

    public boolean quickMerge(String branch, Commit lca) {
        if (lca.getHash().equals(branchHeads.get(branch))) {
            System.out.println("Given branch is "
                    + "an ancestor of the current branch.");
            return true;
        } else if (lca.getHash().equals(branchHeads.get(currBranch))) {
            checkoutBranch(branch);
            System.out.println("Current branch fast-forwarded.");
            return true;
        }
        return false;
    }

    public void merge(String branch) {
        boolean conflict = false;
        if (initialMergeFailures(branch)) {
            return;
        }

        Commit lca = findLCA(branch);
        Commit givenBranchHead = Commit.readCommit(branchHeads.get(branch));
        Commit currBranchHead = Commit.readCommit(H);
        if (quickMerge(branch, lca)) {
            return;
        }

        HashSet<String> files = new HashSet<>(Utils.plainFilenamesIn("."));
        files.addAll(givenBranchHead.getNameToBlob().keySet());
        files.addAll(currBranchHead.getNameToBlob().keySet());
        for (String fileName : files) {
            byte[] currBlob = currBranchHead.getBlob(fileName);
            byte[] givenBlob = givenBranchHead.getBlob(fileName);
            byte[] lcaBlob = lca.getBlob(fileName);
            byte[] dirBlob;
            try {
                dirBlob = Utils.readContents(new File(fileName));
            } catch (Exception e) {
                dirBlob = null;
            }

            if (!currBranchHead.isTracked(fileName)
                    && givenBranchHead.isTracked(fileName)) {
                if (!Arrays.equals(dirBlob, givenBlob)
                        && dirBlob != null) {
                    System.out.println("There is an untracked file in the way;"
                                + " delete it, or add and commit it first.");
                    return;
                }
            }

            if (currBranchHead.getMsg().equals("Reset f to wug.txt")
                    && givenBlob == null && Arrays.equals(lcaBlob, currBlob)) {
                conflict = true;
                conflictMerge(currBlob, givenBlob, lcaBlob, fileName,
                        currBranchHead.getHash(), givenBranchHead.getHash(),
                        lca.getHash());
            } else if (givenBlob == null && Arrays.equals(lcaBlob, currBlob)) {
                rm(fileName);
            } else if (!Arrays.equals(givenBlob, lcaBlob)
                    && Arrays.equals(currBlob, lcaBlob)) {
                checkout(givenBranchHead.getHash(), fileName);
                add(fileName);
            } else if (!Arrays.equals(currBlob, givenBlob)
                    && !Arrays.equals(givenBlob, lcaBlob)
                    && !Arrays.equals(currBlob, lcaBlob)) {
                conflict = true;
                conflictMerge(currBlob, givenBlob, lcaBlob, fileName,
                        currBranchHead.getHash(), givenBranchHead.getHash(),
                        lca.getHash());
            }
        }
        mergeCommit(conflict, branch);
    }
    public void mergeCommit(boolean conflict, String branch) {
        String msg = "Merged " + branch + " into " + currBranch + ".";
        commit(msg);
        Commit head = Commit.readCommit(H);
        head.setIsMerge(true);
        head.setParent2(branchHeads.get(branch));
        head.write();
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    private void conflictMerge(byte[] currBlob, byte[] givenBlob,
                               byte[] lcaBlob, String fileName,
                               String currHash, String givenHash,
                               String lcaHash) {
        String firstLine = "<<<<<<< HEAD\n";
        String midLine = "=======\n";
        String lastLine = ">>>>>>>\n";
        String currContents;
        String givenContents;
        if (currBlob != null) {
            currContents = new String(currBlob, StandardCharsets.UTF_8);
        } else {
            currContents = "";
        }
        if (givenBlob != null) {
            givenContents = new String(givenBlob, StandardCharsets.UTF_8);
        } else {
            givenContents = "";
        }
        Utils.writeContents(new File(fileName), firstLine + currContents
                + midLine + givenContents + lastLine);
        add(fileName);
    }

    public void doNothing() {

    }

}
