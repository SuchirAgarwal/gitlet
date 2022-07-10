package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.Random;

public class Blob implements Serializable {
    /** The Blob I represent. */
    private byte[] blob;

    /** For differentiating blobs in case same contents. */
    private double randomDouble;

    /** My hash. */
    private String hash;

    public Blob(byte[] bloob) {
        randomDouble = new Random().nextDouble() * 100;
        blob = bloob;
        hash = Utils.sha1(Utils.serialize(this));
    }

    public void write() {
        File dir = new File("./.gitlet/blobs");
        dir.mkdir();
        File thisFile = Utils.join(dir, hash);
        try {
            thisFile.createNewFile();
        } catch (Exception ignored) {
            System.out.println(ignored);
        }
        Utils.writeObject(thisFile, this);
    }

    public static byte[] readBlob(String blobHash) {
        File toRead = new File("./.gitlet/blobs/" + blobHash);
        return Utils.readObject(toRead, Blob.class).getBlob();
    }

    public String getHash() {
        return hash;
    }

    public byte[] getBlob() {
        return blob;
    }
}
