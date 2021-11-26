import java.util.ArrayList;
import java.util.List;

public class Test {
    public static void main(String[] args) {
      if (args.length > 0 && args[0] == "xcdadfda") {
        System.out.println("dfkj");
      }

      String s1 = "Hello World";
      String s2 = "Hello World";
      if(s1 == s2){}
      if(s1.equals(s2)){}

      String s3 = "woop";

      boolean b = s3 == s2;
      boolean c = b;

      String s = null;
      if (b){
          s = "Monday";
      }else if (!b){
          s = "Tuesday";
      }
      
      int i = s.length();

      Boolean ok = null;

      if(ok) System.out.println("ok");

      Other o = new Other();

      List<Object> l = new ArrayList<>();

      System.out.println(l.size());
      
      o.deadAssignment();
      o.doNothing();
      o.compareStr(s1, s2);
    }
}