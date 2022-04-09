package Util;

import java.util.ArrayList;
import java.util.List;

public class Case2 {
    private int x = 1;
    private byte[] y = {4, 5, 6};
    private final String[] xx = {"1", "2", "3"};
    private final String z = "x";
    private Case3 case3 = new Case3();

    public void a(String a) {
        a = a.concat("s");
        System.out.println("xxsasa");
        Case3 case3 = new Case3();
        List<Integer> list = new ArrayList<>();
//        case3.b++;
//        Case3.c++;
        this.x=2;
//        this.case3.case4.case5.a++;
//        this.case3 = new Case3();
//        case3.a[1] = 5;
//        this.x++;
        int aa = b(this.x);
        int bb = c(this.x);
        this.x = c(aa);
        this.x = b(aa);
    }

    private int b(int x) {
        return x + 1;
    }

    private int c(int x) {
        return x + 1;
    }

}
