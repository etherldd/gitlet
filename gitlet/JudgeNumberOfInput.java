package gitlet;

public class JudgeNumberOfInput {
    public static boolean init(int number) {
        return number != 1;
    }
    public static boolean add(int number) {
        return number != 2;
    }
    public static boolean commit(int number) {
        return number != 2;
    }
    public static boolean rm(int number) {
        return number != 2;
    }
    public static boolean log(int number) {
        return number != 1;
    }
    public static boolean globalLog(int number) {
        return number != 1;
    }
    public static boolean find(int number) {
        return number != 2;
    }
    public static boolean status(int number) {
        return number != 1;
    }
    public static boolean checkout(int number) {
        return number <= 1 || number >= 5;
    }
    public static boolean branch(int number) {
        return number != 2;
    }
    public static boolean rmBranch(int number) {
        return number == 1;
    }
    public static boolean reset(int number) {
        return number != 2;
    }

    public static boolean merge(int number) {
        return number != 2;
    }
}
