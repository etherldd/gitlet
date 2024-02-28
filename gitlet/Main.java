package gitlet;

import static gitlet.Repository.GITLET_DIR;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        String firstArg = args[0];
        boolean isinit = GITLET_DIR.exists();
        //have init ?
        if (!isinit && firstArg.compareTo("init") != 0) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        //repeat init ?
        else if (isinit && firstArg.compareTo("init") == 0) {
            System.out.println("A Gitlet version-control system already" +
                    " exists in the current directory.");
            return;
        }
        //command judge:
        int argsLength = args.length;
        switch(firstArg) { // wrong number or format of operands,
            // print the message Incorrect operands. and exit.
            case "init":
                if (JudgeNumberOfInput.init(argsLength)) { // wrong number
                    System.out.println(argsLength);
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.init();
                break;
            case "add":
                if (JudgeNumberOfInput.add(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.addStageBy(args[1]);
                break;
            case "commit":
                if (argsLength == 1) {
                    System.out.println("Please enter a commit message.");
                    return;
                }
                if (JudgeNumberOfInput.commit(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                String message = args[1];
                if (message == "") {
                    System.out.println("Please enter a commit message.");
                    return;
                }
                Repository.commit(message);
                break;
            case "rm":
                if (JudgeNumberOfInput.rm(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.rm(args[1]);
                break;
            case "log":
                if (JudgeNumberOfInput.log(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.log();
                break;
            case "global-log":
                if (JudgeNumberOfInput.globalLog(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.globalLog();
                break;
            case "find":
                if (JudgeNumberOfInput.find(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.find(args[1]);
                break;
            case "status":
                if (JudgeNumberOfInput.status(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.status();
                break;
            case "checkout":
                if (JudgeNumberOfInput.checkout(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                /*Three usage:1.checkout branch_name 2. checkout -- filename 3. checkout commitID -- filename
                * */
                if (argsLength == 2) { //1.checkout branch_name
                    Repository.checkoutBranch(args[1]);
                    return;
                } else if (argsLength == 3) { //2. checkout -- filename
                    if (args[1].compareTo("--") != 0) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    Repository.checkoutCurCommitFile(args[2]);
                    return;
                } else if (argsLength == 4) { //3. checkout commitID -- filename
                    if (args[2].compareTo("--") != 0) {
                        System.out.println("Incorrect operands.");
                        return;
                    }
                    Repository.checkoutIdCommitFile(args[3],
                            Repository.getCommitIdFromPrefix(args[1]));
                    return;
                }
                break;
            case "branch":
                if (JudgeNumberOfInput.branch(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.creatBranch(args[1]);
                break;
            case "rm-branch":
                if (JudgeNumberOfInput.rmBranch(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.removeBranch(args[1]);
                break;
            case "reset":
                if (JudgeNumberOfInput.reset(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.reset(args[1]);
                break;
            case "merge":
                if (JudgeNumberOfInput.merge(argsLength)) { // wrong number
                    System.out.println("Incorrect operands.");
                    return;
                }
                Repository.merge(args[1]);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
    }
}
