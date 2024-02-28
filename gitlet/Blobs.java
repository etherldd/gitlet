package gitlet;

import java.io.File;

import static gitlet.Utils.readContentsAsString;

public class Blobs implements Dumpable {
    private String blobsContent;
    private String name;
    public Blobs(File file, String name) {
        blobsContent = readContentsAsString(file);
        this.name = name;
    }
    public Blobs(String blobsContent, String name) {
        this.blobsContent = blobsContent;
        this.name = name;
    }
    public String getBlobsContent() {
        return blobsContent;
    }
    public String generateID() {
        return Utils.sha1(blobsContent.toString(), name);
    }
    @Override
    public void dump() {
        System.out.println("name : " + name);
        System.out.println("contents : " + blobsContent);
        System.out.println("SHA_VALUE : " + generateID());
    }
}
