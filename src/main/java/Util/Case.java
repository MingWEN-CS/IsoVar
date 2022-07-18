package Util;

import IsoVar.MainClass;

import java.util.Random;

public class Case {
    int a = 0;

    public static void main(String[] args) {
//        for (int i = 1; i <= 133; i++) {
////            System.out.print("--exclude \"Mockito/Mockito_"+i+"_buggy/.git/*\" ");
////            System.out.print("--exclude \"Mockito/Mockito_"+i+"_buggy/.gradle_local_home/*\" ");
//
//            System.out.print("Closure_"+i+"_fixed/ ");
//        }
//        for (char c = 'B';c<='S';c++) {
//            String fenzi="(";
//            String fenmu ="(";
//            for (int i = 1; i <= 9; i++) {
//                fenzi += String.valueOf(c)+i +"*T"+i;
//                if(i!=9)
//                    fenzi+="+";
//                fenmu += "T"+i;
//                if(i!=9)
//                    fenmu+="+";
//            }
//            System.out.printf("=%s)/%s)",fenzi,fenmu);
//            System.out.printf("\n");
//        }
//        =COUNTIFS(C1:C95,">0",C1:C95,"<=0.1")
        for (double i = 0; i <= 1; i+=0.1) {
            String j = String.format("%.1f",i);
            System.out.print("=COUNTIFS(E1:E95,\">");
            System.out.print(j);
            System.out.print("\",E1:E95,\"<=");
            String k = String.format("%.1f",i+0.1);
            System.out.println(k+"\")");
        }
    }

    public void a() {
        boolean v = MainClass.isLinux();
        v = v ^ true;

        char v0 = 's';
        v0 = (char) (v0 + 2);

        short v1 = Short.parseShort("2");
        v1 = (short) (v1 + 2);

        byte v2 = 4;
        v2 = (byte) (v2 + 21);

        int v3 = 10;
        v3 += 21;

        long v4 = 2012L;
        v4 += 21L;

        float v5 = 213f;
        v5 += 21.2f;

        double v6 = 421d;
        v6 += 1221.12d;

        String v7 = "hahha";
        v7 = v7.concat("a");
        System.out.println(v7);

        short[] a = {2, 2, 3};
        short[] aa = a;
        a[2] = Short.parseShort("12");

        for (int i = 0; i < a.length; i++) {
            a[i] = (short) (Short.parseShort("21") + 2);
        }
        a[(int) System.currentTimeMillis() % a.length]++;

        long[] b = {2, 2, 3};
        for (int i = 0; i < b.length; i++) {
            b[i] = 2;
        }
        b[(int) System.currentTimeMillis() % a.length] += 2L;

        boolean[] c = {true, true, true};
        for (int i = 0; i < b.length; i++) {
            c[i] ^= true;
        }
        c[(int) System.currentTimeMillis() % c.length] ^= true;

        String[] strs = {"1", "2"};
        String[] str2 = strs;
        str2[1] = "c";
//        strs[(int) System.currentTimeMillis() % c.length] = strs[(int) System.currentTimeMillis() % c.length].concat("x");


        this.a = Integer.parseInt("213");


    }
}
