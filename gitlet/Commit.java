package gitlet;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 */
public class Commit implements  Dumpable {
    /**
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    private String message;
    private Map<String, String> fileNameToObjectsId;
    private List<String> parents; //stores parents' SHA_id
    // if lots of parents exist(merge), let first be the main , second be the other branch
    private Date time;
    private String ID;
    public Commit(String message, int timeId) {
        this.message = message;
        time = new Date(timeId);
        fileNameToObjectsId = new HashMap<>();
        parents = new ArrayList<>();
        ID = creatID();
    }
    public Commit(String message, Commit parent) {
        this.message = message;
        time = new Date();
        parents = new ArrayList<>();
        parents.add(parent.generateID());
        fileNameToObjectsId = new HashMap<>();
        fileNameToObjectsId.putAll(parent.fileNameToObjectsId); // copy parent's map
        ID = creatID();
    }
    public Commit(String message, Commit parent, Commit secondParent) { // for merge
        this.message = message;
        time = new Date();
        parents = new ArrayList<>();
        parents.add(parent.generateID());
        parents.add(secondParent.generateID());
        fileNameToObjectsId = new HashMap<>();
        fileNameToObjectsId.putAll(parent.fileNameToObjectsId); // copy parent's map
        ID = creatID();
    }
    public Commit(String message, String[] parent) {
        this.message = message;
        time = new Date(0);
        parents = new ArrayList<>();
        for (int i = 0; i < parent.length; i += 1) {
            parents.add(parent[i]);
        }
        fileNameToObjectsId = null;
        ID = creatID();
    }
    public List<String> getParents() {
        return this.parents;
    }
    private static String dateToTimeStamp(Date date) {
        DateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.US);
        return dateFormat.format(date);
    }
    public Map<String, String> getFileNameToObjectsIdMap() {
        return fileNameToObjectsId;
    }
    public String getTimeStamp() {
        return dateToTimeStamp(time);
    }
    public String getMessage() {
        return this.message;
    }
    private String toStringMap() {
        String result = "";
        for (String iter : fileNameToObjectsId.keySet()) {
            result += iter;
            result += fileNameToObjectsId.get(iter);
        }
        return result;
    }
    public String generateID() {
        return ID;
    }
    private String creatID() {
//        return Utils.sha1(dateToTimeStamp(time), message, parents.toString(),
//                fileNameToObjectsId.toString());
        return Utils.sha1(dateToTimeStamp(time), message, parents.toString(),
                toStringMap());
    }
    public int getParentsNum() {
        return parents.size();
    }
    public String getFirstParID() {
        return parents.get(0);
    }
    public String getSecondParID() {
        return parents.get(1);
    }
    public static Commit generateInitCommit() {
        Commit result = new Commit("initial commit", 0);
        return result;
    }
    @Override
    public void dump() {
        System.out.println("message : " + message);
        System.out.println("time : " + dateToTimeStamp(time));
        System.out.println("parents : " + parents.toString());
        System.out.println("content map : " + fileNameToObjectsId.toString());
        System.out.println("ID : " + generateID());
    }
}
