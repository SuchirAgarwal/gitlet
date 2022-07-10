package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.HashSet;

public class Stage implements Serializable {

    /** Da directory. */
    private File dir = new File("./.gitlet/staging");

    /** Da directory, but as a string yee yee yee. */
    private String dirString = "./.gitlet/staging";

    /** Da removing thing yaaaaaa. */
    private HashSet<String> rms;

    public Stage() {
        dir.mkdir();
        rms = new HashSet<>();
    }

    public void clear() {
        for (String fileName : Utils.plainFilenamesIn(dirString)) {
            clear(fileName);
        }
        rms.clear();
    }

    public void clear(String fileName) {
        File toClear = Utils.join(dirString, fileName);
        if (rms.contains(fileName)) {
            rms.remove(fileName);
        }
        if (toClear.exists()) {
            toClear.delete();
        }
    }

    public void add(String fileName, byte[] blob) {
        File file = Utils.join(dir, fileName);
        if (file.isFile()) {
            Utils.writeContents(file, blob);
        } else {
            try {
                file.createNewFile();
                Utils.writeContents(file, blob);
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    public void rm(String fileName) {
        clear(fileName);
        rms.add(fileName);
    }

    public void unStageRm(String fileName) {
        if (rms.contains(fileName)) {
            rms.remove(fileName);
        }
    }

    public boolean isStaged(String fileName) {
        File file = Utils.join(dir, fileName);
        return file.isFile();
    }

    public boolean isStagedForRm(String fileName) {
        return rms.contains(fileName);
    }

    public HashSet<String> filesStagedForRm() {
        return rms;
    }

    public List<String> getFileNames() {
        return Utils.plainFilenamesIn(dir);
    }


    public byte[] getContents(String fileName) {
        return Utils.readContents(Utils.join(dir, fileName));
    }

    public boolean isEmpty() {
        return rms.isEmpty() && Utils.plainFilenamesIn(dir).isEmpty();
    }

}
