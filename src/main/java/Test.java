
public class Test{

    public static void main(String... args) {
        String s1 = "Hello IntraJ";
        String s2 = "Hello IntraJ";
        String s3 = "Unused var";

        if(s1 == s2)
            System.out.println("== used");

        if(s1.equals(s2))
            System.out.println(".equals used");
    }
}