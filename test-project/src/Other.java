public abstract class Other {
    public void doNothing() {

    }

    public void deadAssignment() {
        int x = 0;
    }

    public void deadAssignment(int y) {
        int x = y;
    }

    public boolean compareStr(String s1, String s2) {
        return s1 == s2;
    }

    public abstract void print();
    public abstract void print(String s);
}
