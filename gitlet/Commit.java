package gitlet;

import java.io.File;
import java.util.Date;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.TreeMap;
import java.util.Random;

public class Commit implements Serializable {
    /** My message. */
    private final String msg;

    /** My date-time as a string. */
    private String timeString;

    /** The stage I refer to. */
    private final Stage stage;

    /** A random number in case of same hash value. */
    private double randomDouble;

    /** Is this a merge commit. */
    private boolean isMerge;

    /** The hash of my parent. */
    private String parent;

    /** Second parent in case of merge. */
    private String parent2;

    /** My hash. */
    private String hash;

    /** File names to blob hashes. */
    private TreeMap<String, String> nameToBlob;

    /** Files that I track. */
    private HashSet<String> tracked;

    /** Commit directory. */
    private static File cwd = new File("./.gitlet/commits/");

    public Commit(String damsg, Stage dastage, HashSet<String> datracked) {
        isMerge = false;
        randomDouble = new Random().nextDouble();
        msg = damsg;
        stage = dastage;
        Date time = new Date();
        tracked = datracked;
        nameToBlob = new TreeMap<>();
        SimpleDateFormat sdf = new SimpleDateFormat(
                "EEE MMM d HH:mm:ss yyyy ZZZZ");
        timeString = sdf.format(time);
        setHash();
    }

    public void write() {
        cwd.mkdir();
        for (String file : stage.getFileNames()) {
            File currFile = new File(file);
            Blob b = new Blob(Utils.readContents(currFile));
            b.write();
            nameToBlob.put(file, b.getHash());
        }
        if (parent != null) {
            Commit p = readCommit(parent);
            for (String file : p.getNameToBlob().keySet()) {
                if (!nameToBlob.containsKey(file)
                        && !stage.isStagedForRm(file)) {
                    nameToBlob.put(file, p.getNameToBlob().get(file));
                }
            }
        }
        File commitPath = Utils.join(cwd, hash);
        try {
            commitPath.createNewFile();
        } catch (Exception ignored) {
            System.out.println(ignored);
        }
        Utils.writeObject(commitPath, this);
        stage.clear();
    }

    public String getHash() {
        return hash;
    }

    public boolean isTracked(String fileName) {
        return tracked.contains(fileName);
    }

    public HashSet<String> getFileNames() {
        return new HashSet<String>(tracked);
    }

    public TreeMap<String, String> getNameToBlob() {
        return nameToBlob;
    }

    public String getBlobHash(String fileName) {
        return nameToBlob.get(fileName);
    }

    public byte[] getBlob(String fileName) {
        try {
            return Blob.readBlob(nameToBlob.get(fileName));
        } catch (Exception e) {
            return null;
        }
    }

    public void setParent(String daparent) {
        this.parent = daparent;
    }

    public String getParent() {
        return parent;
    }

    public String getMsg() {
        return msg;
    }

    private void setHash() {
        hash = Utils.sha1((Object) Utils.serialize(this));
    }

    public static Commit readCommit(String hash) {
        try {
            File toRead = Utils.join(cwd, hash);
            return Utils.readObject(toRead, Commit.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void logThis() {
        String firstLine = "===\n";
        String secondLine = "commit " + hash + "\n";
        String mergeLine = "";
        String thirdLine = "Date: " + timeString + "\n";
        String fourthLine = msg + "\n";
        if (isMerge) {
            mergeLine = "Merge: " + parent.substring(0, 7)
                    + " " + parent2.substring(0, 7) + "\n";
        }
        System.out.println(firstLine + secondLine
                + mergeLine + thirdLine + fourthLine);
    }

    public boolean isMerge() {
        return isMerge;
    }

    public void setIsMerge(boolean b) {
        isMerge = b;
    }

    public String getParent2() {
        return parent2;
    }

    public void setParent2(String s) {
        parent2 = s;
    }

    public static boolean hasParent(String goal, String curr) {
        if (curr == null) {
            return false;
        }
        Commit c = readCommit(curr);
        if (goal.equals(curr)) {
            return true;
        }
        boolean a = hasParent(goal, c.getParent());
        boolean b = hasParent(goal, c.getParent2());
        return a || b;
    }

    public static int getDistance(String goalHash, String currHash) {
        if (goalHash.equals(currHash)) {
            return 0;
        } else if (currHash == null) {
            return Integer.MAX_VALUE;
        } else if (Commit.readCommit(currHash).isMerge()) {
            return 1 + Math.min(getDistance(goalHash,
                            Commit.readCommit(currHash).getParent()),
                    getDistance(goalHash,
                            Commit.readCommit(currHash).getParent2()));
        } else {
            return 1 + getDistance(goalHash,
                    Commit.readCommit(currHash).getParent());
        }
    }


}
