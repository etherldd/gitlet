package gitlet;

import java.io.File;
import java.util.*;

import static gitlet.Commit.generateInitCommit;
import static gitlet.Utils.*;
import static gitlet.Utils.readContentsAsString;

/** Represents a gitlet repository.
 *  does at a high level.
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File BLOBS_DIR = join(GITLET_DIR, "objects", "Blobs");
    public static final File COMMITS_DIR = join(GITLET_DIR, "objects", "commits");
    public static final File HEAD_DIR = join(GITLET_DIR, "refs", "head");
    public static final File ADDSTAGE_DIR = join(GITLET_DIR, "addStage");
    public static final File REMOVESTAGE_DIR = join(GITLET_DIR, "removeStage");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static void init() {
        /*
        overall design:
             |--.gitlet
                    |--objects
                            |--Blobs
                            |--commits
                                |--XXX(initSHA_value) (-> content : init_commit object)
                    |--refs
                            |--head
                                |--master (now)  (-> content : initSHA_value)
                                //  |--XXX  (later)
                    |--HEAD  (-> content : master)
                    |--addStage
                    |--removeStage
              |--user.dir
        */
        //.gitlet
        GITLET_DIR.mkdir();
        /* |--objects
                |--Blobs
                |--commits
                    |--XXX(initSHA_value) (-> content : init_commit object)
        */
        File objects = join(GITLET_DIR, "objects");
        objects.mkdir();
        File blobs = join(objects, "Blobs");
        File commits = join(objects, "commits");
        blobs.mkdir();
        commits.mkdir();
        Commit initCommit = generateInitCommit();
        writeCommit(initCommit);
        /*
         |--refs
            |--head
                |--master (now)
                //  |--XXX  (later)
        */
        File refs = join(GITLET_DIR, "refs");
        refs.mkdir();
        File head = join(refs, "head");
        head.mkdir();
        File masterInit = join(head, "master");
        writeContents(masterInit, initCommit.generateID()); // text file
        try {
            masterInit.createNewFile();
        }  catch (java.io.IOException excp) {
            excp.printStackTrace();
        }
        /*
         |--HEAD
        */
        writeContents(HEAD, "master");
        try {
            HEAD.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
        /*
         |--addStage
         |--removeStage
        */
        File addStage = join(GITLET_DIR, "addStage");
        File removeStage = join(GITLET_DIR, "removeStage");
        addStage.mkdir();
        removeStage.mkdir();
    }
    public static void writeCommit(Commit c) {
        File toaddFile = join(COMMITS_DIR, c.generateID());
        if (toaddFile.exists()) { //if already exist, ignore!
            return;
        }
        writeObject(toaddFile, c);
        try {
            toaddFile.createNewFile();
        }  catch (java.io.IOException excp) {
            excp.printStackTrace();
        }
    }
    public static Commit findCommit(String commitID) { //By default: commitID exists
        File found = new File(COMMITS_DIR, commitID);
        Commit result = readObject(found, Commit.class);
        return result;
    }
    public static void writeBlob(Blobs c) { //used by commit
        File toaddFile = join(BLOBS_DIR, c.generateID());
        if (toaddFile.exists()) { //if already exist, ignore! --> used for merge
            return;
        }
        writeObject(toaddFile, c);
        try {
            toaddFile.createNewFile();
        }  catch (java.io.IOException excp) {
            excp.printStackTrace();
        }
    }
    public static Blobs findBlobs(String blobsID) {
        File found = new File(BLOBS_DIR, blobsID);
        Blobs result = readObject(found, Blobs.class);
        return result;
    }
    //stage operation:
    public static void addStageBy(String name) {
        List<String> curFiles = plainFilenamesIn(CWD);
        if (!curFiles.contains(name)) { //this file does not exist in cur working sapce.
            System.out.println("File does not exist.");
            return;
        }
        File curnameFile = join(CWD, name);
        Blobs curnameBlobs = new Blobs(curnameFile, name); //get current file named "name"
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        Commit curCommit = findCommit(curCommitID);     //get current HEAD commit
        //this file doesn't change compared to commit
        if ((curCommit.getFileNameToObjectsIdMap().containsKey(name))
                && (curnameBlobs.generateID().compareTo
                (curCommit.getFileNameToObjectsIdMap().get(name)) == 0)) {
            File curNameFileInAddStage = join(ADDSTAGE_DIR, name);
            if (curNameFileInAddStage.exists()) {
                curNameFileInAddStage.delete();
            }
            File curNameFileInRemoveStage = join(REMOVESTAGE_DIR, name);
            if (curNameFileInRemoveStage.exists()) {
                curNameFileInRemoveStage.delete();
            }
            return;
        } else { // possible:
                // 1.old file of commit but changed.
                // 2.new file that haven't been tracked.
                // 3.old file that haven't been tracked (changed or unchanged).
            File newAdd = join(ADDSTAGE_DIR, name);
            if (newAdd.exists()) { // have added before
                Blobs curaddstageBlobs = readObject(newAdd, Blobs.class);
                if (curnameBlobs.generateID().compareTo
                        (curaddstageBlobs.generateID()) == 0) { //changed?
                    // 3.old file that haven't been tracked (unchanged).
                    return;
                }
            }
            // the other : override or creat
            writeObject(newAdd, curnameBlobs);
            try {
                newAdd.createNewFile();
            }  catch (java.io.IOException excp) {
                excp.printStackTrace();
            }
        }
    }
    public static void commitMerge(String message, Commit branchCommit) {
        List<String> curAddStageDir = plainFilenamesIn(ADDSTAGE_DIR);
        List<String> curRemoveStageDir = plainFilenamesIn(REMOVESTAGE_DIR);
        if (curAddStageDir.isEmpty() && curRemoveStageDir.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        //generate a new commit for result
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        Commit curCommit = findCommit(curCommitID);     //get current HEAD commit
        // get new commit copied map
        Commit newcommit = new Commit(message, curCommit, branchCommit);
        //operate map
        for (String itername : curAddStageDir) {
            File iternamefile = join(ADDSTAGE_DIR, itername);
            Blobs iterBlob = readObject(iternamefile, Blobs.class);
            //writeBlob(iterBlob) add to object domain from addstage
            newcommit.getFileNameToObjectsIdMap().put(itername, iterBlob.generateID()); // change map
            iternamefile.delete(); // delete from addstage
        }
        for (String itername : curRemoveStageDir) {
            File iternamefile = join(REMOVESTAGE_DIR, itername);
            newcommit.getFileNameToObjectsIdMap().remove(itername);
            iternamefile.delete(); // delete from removestage
        }
        writeCommit(newcommit);
        //update refs-heads-master:
        File curbranch = join(HEAD_DIR, curCommitBranchName);
        writeContents(curbranch, newcommit.generateID());
        try {
            curbranch.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
        //newcommit.dump();
    }
    public static void commit(String message) {
        List<String> curAddStageDir = plainFilenamesIn(ADDSTAGE_DIR);
        List<String> curRemoveStageDir = plainFilenamesIn(REMOVESTAGE_DIR);
        if (curAddStageDir.isEmpty() && curRemoveStageDir.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        //generate a new commit for result
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        Commit curCommit = findCommit(curCommitID);     //get current HEAD commit
        Commit newcommit = new Commit(message, curCommit); // get new commit copied map
        //operate map
        for (String itername : curAddStageDir) {
            File iternamefile = join(ADDSTAGE_DIR, itername);
            Blobs iterBlob = readObject(iternamefile, Blobs.class);
            writeBlob(iterBlob); // add to object domain from addstage
            newcommit.getFileNameToObjectsIdMap().put(itername, iterBlob.generateID()); // change map
            iternamefile.delete(); // delete from addstage
        }
        for (String itername : curRemoveStageDir) {
            File iternamefile = join(REMOVESTAGE_DIR, itername);
            newcommit.getFileNameToObjectsIdMap().remove(itername);
            iternamefile.delete(); // delete from removestage
        }
        writeCommit(newcommit);
        //update refs-heads-master:
        File curbranch = join(HEAD_DIR, curCommitBranchName);
        writeContents(curbranch, newcommit.generateID());
        try {
            curbranch.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
        //newcommit.dump();
    }
    public static void rm(String filename) {
        List<String> cwdFiles = plainFilenamesIn(CWD);
        List<String> addStageDirFiles = plainFilenamesIn(ADDSTAGE_DIR);
        List<String> removeStageDirFiles = plainFilenamesIn(REMOVESTAGE_DIR);
        boolean isneed = false; //judge is or not printf error message
        // delete from the add stage if exist
        if (addStageDirFiles.contains(filename)) {
            File curfileInaddstage = join(ADDSTAGE_DIR, filename);
            curfileInaddstage.delete();
            isneed = true;
        }
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        //get current HEAD commit
        Commit curCommit = findCommit(curCommitID);
        // !!! add to REMOVE_STAGE and delete from workspace
        if (curCommit.getFileNameToObjectsIdMap().containsKey(filename)) {
            // add to REMOVE_STAGE
            //There is possibly this filename has been in
            if (!removeStageDirFiles.contains(filename)) {
                //creat new filename (No need to have content)
                File newremoveStage = join(REMOVESTAGE_DIR, filename);
                //just keep the filename for map to delete key
                try {
                    newremoveStage.createNewFile();
                }  catch (java.io.IOException excp) { // text file
                    excp.printStackTrace();
                }
            }
            // delete from workspace
            if (cwdFiles.contains(filename)) {
                File fileInWorkSpace = join(CWD, filename);
                fileInWorkSpace.delete();
            }
        } else {
            if (!isneed) {
                System.out.println("No reason to remove the file.");
            }
            return;
        }
    }
    public static void log() {
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        Commit curCommit = findCommit(curCommitID);     //get current HEAD commit
        while (curCommit.getParentsNum() != 0) {
            if (curCommit.getParentsNum() == 1) {
                System.out.println("===");
                System.out.println("commit " + curCommit.generateID());
                System.out.println("Date: " + curCommit.getTimeStamp());
                System.out.println(curCommit.getMessage());
                System.out.println();
            } else if (curCommit.getParentsNum() == 2) {
                System.out.println("===");
                System.out.println("commit " + curCommit.generateID());
                String masterPar = new String(curCommit.getFirstParID());
                String branchPar = new String(curCommit.getSecondParID());
                masterPar = masterPar.substring(0, 7);
                branchPar = branchPar.substring(0, 7);
                System.out.println("Merge: " + masterPar + " " + branchPar); //add one message
                System.out.println("Date: " + curCommit.getTimeStamp());
                System.out.println(curCommit.getMessage());
                System.out.println();
            }
            String nextCommitID = curCommit.getFirstParID();
            curCommit = findCommit(nextCommitID);
        }
        //curCommit.GetParentsNum() == 0 (init_commit)
        System.out.println("===");
        System.out.println("commit " + curCommit.generateID());
        System.out.println("Date: " + curCommit.getTimeStamp());
        System.out.println(curCommit.getMessage());
        System.out.println();
    }
    public static void globalLog() {
        List<String> commitDirFiles = plainFilenamesIn(COMMITS_DIR);
        for (String iterCommit : commitDirFiles) {
            File itercomfile = join(COMMITS_DIR, iterCommit);
            Commit iterCom = readObject(itercomfile, Commit.class);
            System.out.println("===");
            System.out.println("commit " + iterCom.generateID());
            System.out.println("Date: " + iterCom.getTimeStamp());
            System.out.println(iterCom.getMessage());
            System.out.println();
        }
    }
    public static void find(String message) {
        List<String> commitDirFiles = plainFilenamesIn(COMMITS_DIR);
        int num = 0;
        for (String iterCommit : commitDirFiles) {
            File itercomfile = join(COMMITS_DIR, iterCommit);
            Commit iterCom = readObject(itercomfile, Commit.class);
            if (iterCom.getMessage().compareTo(message) == 0) {
                System.out.println(iterCom.generateID());
                num += 1;
            }
        }
        if (num == 0) {
            System.out.println("Found no commit with that message.");
        }
    }
    public static void status() {
        //=== Branches ===
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        System.out.println("=== Branches ===");
        List<String> headDirFiles = plainFilenamesIn(HEAD_DIR);
        for (String iterBranch : headDirFiles) {
            if (curCommitBranchName.compareTo(iterBranch) == 0) {
                System.out.print("*");
            }
            System.out.println(iterBranch);
        }
        System.out.println();
        //=== Staged Files ===
        System.out.println("=== Staged Files ===");
        List<String> addStageDirFiles = plainFilenamesIn(ADDSTAGE_DIR);
        for (String iterFileName : addStageDirFiles) {
            System.out.println(iterFileName);
        }
        System.out.println();
        //=== Removed Files ===
        System.out.println("=== Removed Files ===");
        List<String> removeStageDirFiles = plainFilenamesIn(REMOVESTAGE_DIR);
        for (String iterFileName : removeStageDirFiles) {
            System.out.println(iterFileName);
        }
        System.out.println();
        //=== Modifications Not Staged For Commit ===
        List<String> cwdFiles = plainFilenamesIn(CWD);
        Commit curCommit = findCommit(curCommitID);     //get current HEAD commit
        List<String> deletedFromWorkSpace = new ArrayList<>(); //compared to commit
        List<String> modifiedFromWorkSpace = new ArrayList<>(); //compared to commit
        for (String curCommitKey : curCommit.getFileNameToObjectsIdMap().keySet()) {
            //not in add stage && (modified || added)
            if (!addStageDirFiles.contains(curCommitKey)) {
                if (!cwdFiles.contains(curCommitKey)
                        && !removeStageDirFiles.contains(curCommitKey)) {
                    //not in work space and not update to removal
                    deletedFromWorkSpace.add(curCommitKey);
                }
                if (cwdFiles.contains(curCommitKey)) {
                    File curblobsfile = join(CWD, curCommitKey);
                    Blobs curblobs = new Blobs(curblobsfile, curCommitKey);
                    if (curblobs.generateID().compareTo
                            (curCommit.getFileNameToObjectsIdMap().get(curCommitKey)) != 0) {
                        modifiedFromWorkSpace.add(curCommitKey);
                        //in work space but have different id
                    }
                }
            } else {
                if (cwdFiles.contains(curCommitKey)) {
                    File curblobsfile = join(CWD, curCommitKey);
                    Blobs curblobs = new Blobs(curblobsfile, curCommitKey);
                    File curFileNameInAddStage = join(ADDSTAGE_DIR, curCommitKey);
                    Blobs curBlobsInAddStage = readObject(curFileNameInAddStage, Blobs.class);
                    if (curBlobsInAddStage.generateID().compareTo(curblobs.generateID()) != 0) {
                        modifiedFromWorkSpace.add(curCommitKey);
                    }
                } else {
                    deletedFromWorkSpace.add(curCommitKey);
                }
            }
        }
        List<String> addedFromWorkSpace = new ArrayList<>(); //untrcaked files
        for (String cwdFile : cwdFiles) {
            if (!curCommit.getFileNameToObjectsIdMap().containsKey(cwdFile)
                    && !addStageDirFiles.contains(cwdFile)) {
                // in work space but not in commit && not in add stage
                addedFromWorkSpace.add(cwdFile);
            }
            if (curCommit.getFileNameToObjectsIdMap().containsKey(cwdFile)
                    && removeStageDirFiles.contains(cwdFile)) {
                addedFromWorkSpace.add(cwdFile);
            }
        }
        //=== Modifications Not Staged For Commit ===
        System.out.println("=== Modifications Not Staged For Commit ===");
        for (String delIter : deletedFromWorkSpace) {
            System.out.println(delIter +  "(deleted)");
        }
        for (String modIter : modifiedFromWorkSpace) {
            System.out.println(modIter +  "(modified)");
        }
        System.out.println();
        //=== Untracked Files ===:
        System.out.println("=== Untracked Files ===");
        for (String addFile : addedFromWorkSpace) {
            System.out.println(addFile);
        }
        //System.out.println();
    }
    public static boolean changeCommit(Commit curCommit, Commit toChangeCommit) {
        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (String iterCwdFile : cwdFiles) {
            if (!curCommit.getFileNameToObjectsIdMap().containsKey(iterCwdFile)) {
                if (toChangeCommit.getFileNameToObjectsIdMap().containsKey(iterCwdFile)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    return false;
                }
            }
        }
        //write or overwrite all toChangeCommit, but if file is same in two commit, ignore it
        for (String toChangeCommitKey : toChangeCommit.getFileNameToObjectsIdMap().keySet()) {
            File iterfile = join(CWD, toChangeCommitKey);
            Blobs iterBlobs = findBlobs(toChangeCommit.getFileNameToObjectsIdMap().get(toChangeCommitKey));
            if (iterfile.exists()) { // override or ignore
                Blobs iterFileCwd = new Blobs(iterfile, toChangeCommitKey);
                if (iterFileCwd.generateID().compareTo(iterBlobs.generateID()) == 0) {
                    continue; //ignore
                }
            }
            writeContents(iterfile, iterBlobs.getBlobsContent());
            try {
                iterfile.createNewFile();
            }  catch (java.io.IOException excp) { // text file
                excp.printStackTrace();
            }
        }
        //delete all files tracked by curCommit but not tracked in the toChangeCommit
        Set<String> toChangeCommitSet = toChangeCommit.getFileNameToObjectsIdMap().keySet();
        Set<String> curCommitSet = curCommit.getFileNameToObjectsIdMap().keySet();
        Set<String> toDeleteFiles = new HashSet<>(curCommitSet);
        toDeleteFiles.removeAll(toChangeCommitSet);
        for (String iterToDelete : toDeleteFiles) {
            if (cwdFiles.contains(iterToDelete)) {
                File toDelete = join(CWD, iterToDelete);
                toDelete.delete(); // delete old commit file
            }
        }
        //clear stage
        List<String> addStageDirFiles = plainFilenamesIn(ADDSTAGE_DIR);
        List<String> removeStageDirFiles = plainFilenamesIn(REMOVESTAGE_DIR);
        for (String iter : addStageDirFiles) {
            File iterfile = join(ADDSTAGE_DIR, iter);
            iterfile.delete();
        }
        for (String iter : removeStageDirFiles) {
            File iterfile = join(REMOVESTAGE_DIR, iter);
            iterfile.delete();
        }
        return true;
    }
    public static void checkoutBranch(String branchName) {
        List<String> headDirFiles = plainFilenamesIn(HEAD_DIR);
        if (!headDirFiles.contains(branchName)) { //if not exist, error and return
            System.out.println("No such branch exists.");
            return;
        }
        //get toChangeBranchID
        File toChangeBranch = join(HEAD_DIR, branchName);
        String toChangeBranchID = readContentsAsString(toChangeBranch);
        //get curCommitID
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        if (branchName.compareTo(curCommitBranchName) == 0) { //checkout the current branch
            System.out.println("No need to checkout the current branch.");
            return;
        }
        Commit curCommit = findCommit(curCommitID);     //get current HEAD commit
        Commit toChangeCommit = findCommit(toChangeBranchID); //get to_change commit
        if (!changeCommit(curCommit, toChangeCommit)) {
            return;
            //There is an untracked file in the way; delete it, or add and commit it first.
        }
        //change HEAD to toChangeCommit
        writeContents(HEAD, branchName);
        try {
            HEAD.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
    }
    public static void checkoutCurCommitFile(String fileName) {
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        Commit curCommit = findCommit(curCommitID);
        //get current HEAD commit(last commit)
        if (!(curCommit.getFileNameToObjectsIdMap().containsKey(fileName))) {
            //that file doesn't exist
            System.out.println("File does not exist in that commit.");
            return;
        }
        //that file does exist
        String thatFileBlobsId = (curCommit.getFileNameToObjectsIdMap()).get(fileName);
        Blobs  thatfileBlobs = findBlobs(thatFileBlobsId);
        File newfile = join(CWD, fileName);
        writeContents(newfile, thatfileBlobs.getBlobsContent());
        try {
            newfile.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
    }
    public static String getCommitIdFromPrefix(String commitId) {
        List<String> commitDirFiles = plainFilenamesIn(COMMITS_DIR);
        for (String iter : commitDirFiles) {
            if (iter.startsWith(commitId)) {
                return iter;
            }
        }
        return "";
    }
    public static void checkoutIdCommitFile(String fileName, String commitId) {
        List<String> commitDirFiles = plainFilenamesIn(COMMITS_DIR);
        if (!commitDirFiles.contains(commitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        File thatCommit = join(COMMITS_DIR, commitId);
        Commit curCommit = readObject(thatCommit, Commit.class);
        if (!curCommit.getFileNameToObjectsIdMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit."); //that file doesn't exist
            return;
        }
        //that file does exist
        String thatFileBlobsId = curCommit.getFileNameToObjectsIdMap().get(fileName);
        Blobs  thatFileBlobs = findBlobs(thatFileBlobsId);
        File newfile = join(CWD, fileName);
        writeContents(newfile, thatFileBlobs.getBlobsContent());
        try {
            newfile.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
    }
    public static void creatBranch(String branchName) {
        List<String> headDirFiles = plainFilenamesIn(HEAD_DIR);
        if (headDirFiles.contains(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        //get current HEAD commit id
        String curCommitId = readContentsAsString(curCommitBranchFile);
        File newCreat = join(HEAD_DIR, branchName);
        writeContents(newCreat, curCommitId);
        try {
            newCreat.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
    }
    public static void removeBranch(String branchName) {
        List<String> headDirFiles = plainFilenamesIn(HEAD_DIR);
        if (!headDirFiles.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        String curCommitBranchName = readContentsAsString(HEAD);
        if (curCommitBranchName.compareTo(branchName) == 0) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        File toDeleteCreat = join(HEAD_DIR, branchName);
        toDeleteCreat.delete();
    }
    public static void reset(String commitId) {
        //get this commit
        List<String> commitDirFiles = plainFilenamesIn(COMMITS_DIR);
        if (!commitDirFiles.contains(commitId)) {
            System.out.println("No commit with that id exists.");
            return;
        }
        //get toChangeCommit
        Commit toChangeCommit = findCommit(commitId);
        //get current HEAD commit(last commit)
        String curCommitBranchName = readContentsAsString(HEAD);
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitID = readContentsAsString(curCommitBranchFile);
        Commit curCommit = findCommit(curCommitID);
        if (!changeCommit(curCommit, toChangeCommit)) {
            return;
        }
        //change branch point (curCommitBranchFile)
        writeContents(curCommitBranchFile, commitId);
        try {
            curCommitBranchFile.createNewFile();
        }  catch (java.io.IOException excp) { // text file
            excp.printStackTrace();
        }
    }
    public static Map<String, Integer> getAllParentsAndSelf(Commit cur) { //BFS
        Map<String, Integer> result = new HashMap<>();
        //queueStr and queueDepth at same path
        Queue<String> queueStr = new ArrayDeque<>();
        Queue<Integer> queueDepth = new ArrayDeque<>();
        //add curCommit to result
        String firstStr = cur.generateID();
        int firstDepth = 0;
        result.put(firstStr, firstDepth);
        //add all parents of curCommit to queue
        for (String iterId : cur.getParents()) {
            queueStr.add(iterId);
            queueDepth.add(firstDepth + 1);
        }
        while (!queueStr.isEmpty()) { //iterate queue
            String topStr = queueStr.peek(); //get top id
            queueStr.remove();
            int topDepth = queueDepth.peek(); //get top depth
            queueDepth.remove();
            //add to result
            result.put(topStr, topDepth);
            //add parents of curCommit
            Commit topCommit = findCommit(topStr);
            for (String iterId : topCommit.getParents()) {
                queueStr.add(iterId);
                queueDepth.add(topDepth + 1);
            }
        }
        return result;
    }
    public static Commit getSplitPoint(Commit master, Commit branch) {
        //suppose in master's position merging branch
        //return 0 in normal : different from master and branch
        //      split------master
        //           \-----branch
        //return 1 if : result is branch : have already merged
        //      branch-----master
        //return 2 if : result is master : check out branch. Current branch fast-forwarded.
        //      master-----branch
        Map<String, Integer> masterMap = getAllParentsAndSelf(master);
        Map<String, Integer> branchMap = getAllParentsAndSelf(branch);
        String resultCommitId = "";
        int resultDepthValue = Integer.MAX_VALUE;
        for (String iterBranchId : branchMap.keySet()) {
            if (masterMap.containsKey(iterBranchId)) {
                if (masterMap.get(iterBranchId) < resultDepthValue) {
                    resultCommitId = iterBranchId;
                    resultDepthValue = masterMap.get(iterBranchId);
                }
            }
        }
        return findCommit(resultCommitId);
    }
    public static Commit getSplitPointFail(Commit master, Commit branch) {
        //fail because of example:
        //---A---
        //       |---C(AB)----
        //      /             |--E(CD)  : E fail : split point should be B
        //---B--- ---D--------              // , as getSplitPointFail , be init(wrong)
        //
        //suppose in master's position merging branch
        //return 0 in normal : different from master and branch
        //      split------master
        //           \-----branch
        //return 1 if : result is branch : have already merged
        //      branch-----master
        //return 2 if : result is master : check out branch. Current branch fast-forwarded.
        //      master-----branch
        Commit masterCopy = master;
        Commit branchCopy = branch;
        Stack<Commit> masterStack = new Stack<>();
        Stack<Commit> branchStack = new Stack<>();
        //fill masterQueue
        while (masterCopy.getParentsNum() > 0) {
            masterStack.push(masterCopy);
            String tempLast = masterCopy.getFirstParID();
            masterCopy = findCommit(tempLast);
        }
        masterStack.push(masterCopy);
        //fill branchQueue
        while (branchCopy.getParentsNum() > 0) {
            branchStack.add(branchCopy);
            String tempLast = branchCopy.getFirstParID();
            branchCopy = findCommit(tempLast);
        }
        branchStack.add(branchCopy);
        Commit curSameTop = masterStack.peek(); //initCommit
        masterStack.pop();
        branchStack.pop();
        Commit splitPoint = curSameTop; //initCommit
        while (!(masterStack.isEmpty() || branchStack.isEmpty())
                && masterStack.peek().generateID().compareTo
                (branchStack.peek().generateID()) == 0) {
            splitPoint = masterStack.peek();
            masterStack.pop();
            branchStack.pop();
        }
        return splitPoint;
    }
    public static void merge(String branchName) {
        boolean isConflict = false; //indicating if print conflict message
        String curCommitBranchName = readContentsAsString(HEAD);
        if (curCommitBranchName.compareTo(branchName) == 0) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        //if there is unCommit addStage or removeStage:
        List<String> addStageDirFiles = plainFilenamesIn(ADDSTAGE_DIR);
        List<String> removeStageDirFiles = plainFilenamesIn(REMOVESTAGE_DIR);
        if (!(addStageDirFiles.isEmpty() && removeStageDirFiles.isEmpty())) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        //get passed branchName commit:
        List<String> headDirFiles = plainFilenamesIn(HEAD_DIR);
        if (!headDirFiles.contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        File branchFile = join(HEAD_DIR, branchName);
        String branchCommitId = readContentsAsString(branchFile);
        Commit branchCommit = findCommit(branchCommitId); //get branchCommit
        //get current HEAD commit
        File curCommitBranchFile = join(HEAD_DIR, curCommitBranchName);
        String curCommitId = readContentsAsString(curCommitBranchFile); //get current HEAD commit id
        Commit curCommit = findCommit(curCommitId); //get curCommit
        Commit splitPoint = getSplitPoint(curCommit, branchCommit); //get splitPoint
        if (branchCommit.generateID().compareTo(splitPoint.generateID()) == 0) {
            //have merged, needn't do anything
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        } else if (curCommit.generateID().compareTo(splitPoint.generateID()) == 0) {
            checkoutBranch(branchName);     //same effect as checkout branch
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        //judge if this commit will trigger an add conflict case:
        List<String> cwdFiles = plainFilenamesIn(CWD);
        for (String iterCwd : cwdFiles) {
            if (!curCommit.getFileNameToObjectsIdMap().containsKey(iterCwd)) {
                //not tracked in the curCommit
                //if iterCwd will be deleted, but I think there is nothing to delete
                //outside tracked files.
                //if iterCwd will be overWrite
                if (!splitPoint.getFileNameToObjectsIdMap().containsKey(iterCwd)) {
                    //only branch have
                    //will load to work space
                    System.out.println("There is an untracked file in the way; delete it"
                            + ", or add and commit it first.");
                    return;
                } else {
                    if (branchCommit.getFileNameToObjectsIdMap().containsKey(iterCwd)
                        && branchCommit.getFileNameToObjectsIdMap().get(iterCwd).compareTo
                            (splitPoint.getFileNameToObjectsIdMap().get(iterCwd)) != 0) {
                        //curCommit don't have, but branch have and modified
                        //a merge conflict:
                        //will generate a new file called iterCwd, which may override old file
                        //content is merge conflict(delete or modify)
                        System.out.println("There is an untracked file in the way; delete it"
                                + ", or add and commit it first.");
                        return;
                    }
                }
            }
        }
        //--------------------------------
        //splitPoint -----curCommit(now)
        //           \----branchCommit
        //--------------------------------
        //with empty addStage and removeStage, with three different commit
        //target : update stage and at last commit
        //List<String> ADDSTAGE_DIR_files = plainFilenamesIn(ADDSTAGE_DIR);
        // before all empty
        //List<String> REMOVESTAGE_DIR_files = plainFilenamesIn(REMOVESTAGE_DIR);
        // before all empty
        for (String iterSplit : splitPoint.getFileNameToObjectsIdMap().keySet()) { //0.iterate split
            //1.curCommit exist iterSplit
            if (curCommit.getFileNameToObjectsIdMap().containsKey(iterSplit)) {
                if (curCommit.getFileNameToObjectsIdMap().get(iterSplit).compareTo(
                        splitPoint.getFileNameToObjectsIdMap().get(iterSplit)) == 0) {
                    // 2.curCommit : Unmodified
                    if (!branchCommit.getFileNameToObjectsIdMap().containsKey(iterSplit)) { // 3.branch rm
                        // !!! add remove stage
                        // !!! remove from work space
                        File iterRm = join(REMOVESTAGE_DIR, iterSplit); //just a name, no content
                        try {
                            iterRm.createNewFile();
                        }  catch (java.io.IOException excp) {
                            excp.printStackTrace();
                        }
                        // !!! remove from work space
                        File curRmFile = join(CWD, iterSplit);
                        if (curRmFile.exists()) {
                            curRmFile.delete();
                        }
                    } else if (branchCommit.getFileNameToObjectsIdMap().get(iterSplit).compareTo(
                            //3.branch also unmodified
                            splitPoint.getFileNameToObjectsIdMap().get(iterSplit)) == 0) {
                        continue; //do nothing
                    } else if (branchCommit.getFileNameToObjectsIdMap().get(iterSplit).compareTo(
                            splitPoint.getFileNameToObjectsIdMap().get(iterSplit)) != 0) {
                        // 3.branch changed
                        // !!! add to addStage
                        // !!! update work space
                        Blobs iteradd = findBlobs(branchCommit.getFileNameToObjectsIdMap().get(iterSplit));
                        File iterAdd = join(ADDSTAGE_DIR, iterSplit);
                        writeObject(iterAdd, iteradd);
                        try {
                            iterAdd.createNewFile();
                        }  catch (java.io.IOException excp) {
                            excp.printStackTrace();
                        }
                        // !!! update work space
                        File updateFile = join(CWD, iterSplit);
                        writeContents(updateFile, iteradd.getBlobsContent()); //overwrite or add
                        try {
                            updateFile.createNewFile();
                        }  catch (java.io.IOException excp) {
                            excp.printStackTrace();
                        }
                    }
                } else { //2.curCommit : modified
                    if (!branchCommit.getFileNameToObjectsIdMap().containsKey(iterSplit)) {
                        //merge conflict:??
                        //add work space!!!!
                        //and add to addStage
                        isConflict = true;
                        Blobs curTemp = findBlobs(curCommit.getFileNameToObjectsIdMap().get(iterSplit));
                        String newConflictStr = mergeContent(curTemp.getBlobsContent(), "");
                        Blobs newConflictBlobs = new Blobs(newConflictStr, iterSplit);
                        File newAddStage = join(ADDSTAGE_DIR, iterSplit);
                        writeObject(newAddStage, newConflictBlobs);
                        try {
                            newAddStage.createNewFile();
                        }  catch (java.io.IOException excp) {
                            excp.printStackTrace();
                        }
                        // !!! update work space
                        File updateFile = join(CWD, iterSplit);
                        writeContents(updateFile, newConflictStr); //overwrite or add
                        try {
                            updateFile.createNewFile();
                        }  catch (java.io.IOException excp) {
                            excp.printStackTrace();
                        }
                    } else if (branchCommit.getFileNameToObjectsIdMap().get(iterSplit).compareTo(
                            curCommit.getFileNameToObjectsIdMap().get(iterSplit)) == 0) {
                        //3.branch also modify, but content modified is same : ignore
                        continue;
                    } else if (branchCommit.getFileNameToObjectsIdMap().get(iterSplit).compareTo(
                                splitPoint.getFileNameToObjectsIdMap().get(iterSplit)) == 0) {
                        //3.branch doesn't modify ignore
                        continue;
                    } else { // 3. branch and curCommit both modified, but in different way:
                        //merge conflict:??
                        //add work space!!!!
                        //and add to addStage
                        isConflict = true;
                        Blobs curTemp = findBlobs(curCommit.getFileNameToObjectsIdMap().get(iterSplit));
                        Blobs branchTemp = findBlobs(branchCommit.
                                getFileNameToObjectsIdMap().get(iterSplit));
                        String newConflictStr = mergeContent(curTemp.getBlobsContent(),
                                branchTemp.getBlobsContent());
                        Blobs newConflictBlobs = new Blobs(newConflictStr, iterSplit);
                        File newAddStage = join(ADDSTAGE_DIR, iterSplit);
                        writeObject(newAddStage, newConflictBlobs);
                        try {
                            newAddStage.createNewFile();
                        }  catch (java.io.IOException excp) {
                            excp.printStackTrace();
                        }
                        // !!! update work space
                        File updateFile = join(CWD, iterSplit);
                        writeContents(updateFile, newConflictStr); //overwrite or add
                        try {
                            updateFile.createNewFile();
                        }  catch (java.io.IOException excp) {
                            excp.printStackTrace();
                        }
                    }

                }
            } else { //1.curCommit  iterSplit doesn't exist (rm)
                if (!branchCommit.getFileNameToObjectsIdMap().containsKey(iterSplit)) {
                    //both rm
                    //ignore
                    continue;
                } else if (branchCommit.getFileNameToObjectsIdMap().get(iterSplit).compareTo(
                        splitPoint.getFileNameToObjectsIdMap().get(iterSplit)) != 0) {
                    //2. curCommit rm , branch modified
                    //merge conflict:??
                    //add work space!!!!
                    //and add to addStage
                    isConflict = true;
                    Blobs branchTemp = findBlobs(branchCommit.getFileNameToObjectsIdMap().get(iterSplit));
                    String newConflictStr = mergeContent("", branchTemp.getBlobsContent());
                    Blobs newConflictBlobs = new Blobs(newConflictStr, iterSplit);
                    File newAddStage = join(ADDSTAGE_DIR, iterSplit);
                    writeObject(newAddStage, newConflictBlobs);
                    try {
                        newAddStage.createNewFile();
                    }  catch (java.io.IOException excp) {
                        excp.printStackTrace();
                    }
                    // !!! update work space
                    File updateFile = join(CWD, iterSplit);
                    writeContents(updateFile, newConflictStr); //overwrite or add
                    try {
                        updateFile.createNewFile();
                    }  catch (java.io.IOException excp) {
                        excp.printStackTrace();
                    }
                } else {
                    //2.  only curCommit rm:
                    //ignore:
                    continue;
                }
            }
        }
        Set<String> curOutOfSplit = new HashSet<>();
        curOutOfSplit.addAll(curCommit.getFileNameToObjectsIdMap().keySet());
        curOutOfSplit.removeAll(splitPoint.getFileNameToObjectsIdMap().keySet());
        for (String iterCur : curOutOfSplit) {
            if (branchCommit.getFileNameToObjectsIdMap().containsKey(iterCur)
                    && branchCommit.getFileNameToObjectsIdMap().get(iterCur).compareTo(
                            curCommit.getFileNameToObjectsIdMap().get(iterCur)) != 0) {
                //curCommit and branch both add a new
                //  file called iterCur, but with different content
                //merge conflict ??
                //add work space!!!!
                //and add to addStage
                isConflict = true;
                Blobs curTemp = findBlobs(curCommit.getFileNameToObjectsIdMap().get(iterCur));
                Blobs branchTemp = findBlobs(branchCommit.getFileNameToObjectsIdMap().get(iterCur));
                String newConflictStr = mergeContent(curTemp.getBlobsContent(),
                        branchTemp.getBlobsContent());
                Blobs newConflictBlobs = new Blobs(newConflictStr, iterCur);
                File newAddStage = join(ADDSTAGE_DIR, iterCur);
                writeObject(newAddStage, newConflictBlobs);
                try {
                    newAddStage.createNewFile();
                }  catch (java.io.IOException excp) {
                    excp.printStackTrace();
                }
                // !!! update work space
                File updateFile = join(CWD, iterCur);
                writeContents(updateFile, newConflictStr); //overwrite or add
                try {
                    updateFile.createNewFile();
                }  catch (java.io.IOException excp) {
                    excp.printStackTrace();
                }
            }
        }
        Set<String> branchOutOfSplitAndCurCommit = new HashSet<>();
        branchOutOfSplitAndCurCommit.addAll(branchCommit.getFileNameToObjectsIdMap().keySet());
        branchOutOfSplitAndCurCommit.removeAll(splitPoint.getFileNameToObjectsIdMap().keySet());
        branchOutOfSplitAndCurCommit.removeAll(curCommit.getFileNameToObjectsIdMap().keySet());
        for (String iterBr : branchOutOfSplitAndCurCommit) {
            //branch new add files
            //add work space!!!!
            //and add to addStage
            Blobs iteradd = findBlobs(branchCommit.getFileNameToObjectsIdMap().get(iterBr));
            File iterAdd = join(ADDSTAGE_DIR, iterBr);
            writeObject(iterAdd, iteradd);
            try {
                iterAdd.createNewFile();
            }  catch (java.io.IOException excp) {
                excp.printStackTrace();
            }
            // !!! update work space
            File updateFile = join(CWD, iterBr); //write (don't exist before)
            writeContents(updateFile, iteradd.getBlobsContent()); //overwrite or add
            try {
                updateFile.createNewFile();
            }  catch (java.io.IOException excp) {
                excp.printStackTrace();
            }
        }
        commitMerge("Merged " + branchName
                + " into " + curCommitBranchName + ".", branchCommit);
        if (isConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }
    public static String mergeContent(String curCommitContent,
                                      String branchCommitContent) {
        String result = "<<<<<<< HEAD\n";
        result += curCommitContent;
        result += "=======\n";
        result += branchCommitContent;
        result += ">>>>>>>\n";
        return result;
    }
}
