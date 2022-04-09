package Util;

import java.util.List;

public class MRR {
    public static double MRR(List<List<Integer>> ranks) {
        double ans = 0;
        int size = 0;
        for (int i = 0; i < ranks.size(); i++) {
            List<Integer> rank = ranks.get(i);
//			System.out.println(rank.get(0) + 1);
            ans += RR(rank);
//			if (ranks.get(i).size() > 0)
//				size++;
        }
        return ans / ranks.size();
    }

    public static double RR(List<Integer> rank) {
        if (rank.size() == 0) return 0;
        double ans = 1.0 / (rank.get(0) + 1);
        return ans;
    }

    public static double MRLR(List<List<Integer>> ranks) {
        double ans = 0;
        for (int i = 0; i < ranks.size(); i++) {
            List<Integer> rank = ranks.get(i);
//			System.out.println(rank.get(0) + 1);
            ans += 1.0 / (rank.get(rank.size() - 1) + 1);
        }
        return ans / ranks.size();
    }

    public static double MAP(List<List<Integer>> ranks) {
        double ans = 0;
        int size = 0;
        for (int i = 0; i < ranks.size(); i++) {

            ans += AP(ranks.get(i));
//			System.out.println(AP(ranks.get(i)));
//			if (ranks.get(i).size() > 0)
//				size++;
        }
        return ans / ranks.size();
    }

    public static double AP(List<Integer> rank) {

        double tmp = 0.0;
        if (rank.size() == 0) return tmp;
        for (int j = 0; j < rank.size(); j++) {
            int r = j + 1;
            tmp += r * 1.0 / (rank.get(j) + 1);
//			System.out.println(r * 1.0 /(rank.get(j) + 1));
        }
        return tmp / rank.size();

    }

}
