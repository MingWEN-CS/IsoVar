package Util;

import Instrument.Instrument;
import IsoVar.Configuration;
import IsoVar.PatchSummary;
import IsoVar.VariableTrimInfo;
import org.javatuples.Triplet;

import java.io.*;
import java.util.*;

import static Instrument.Instrument.myHashCode;

public class EvaluationUtil {

    static double alpha = 0.4;
    static double beta = 1;
    static double gama = 0.2;
//    static double gama = 0;

    public static void main(String[] args) {
        Configuration config = new Configuration();
        try {
            List<List<Integer>> ranks = new ArrayList<>();
            File statistical = new File(config.metricRoot + "/statistic.txt");
            FileWriter stat = new FileWriter(statistical, true);
//            String[] projects = {"Time", "Chart", "Lang", "Math", "Closure", "Mockito"};
            String[] projects = {"Time"};
            for (String project : projects) {
                config.setProjectName(project);
//                File metricFile = new File(config.metricRoot + "/" + config.getProjectName() + "_ochiai.txt");
                File metricFile = new File(config.metricRoot + "/" + config.getProjectName() + ".txt");
                List<Integer> bears_no_work = Arrays.asList(4, 22, 95, 69, 89, 92, 93, 34, 77); // soot can not handle lambda?
                metricFile.getParentFile().mkdirs();
                int[] topn = new int[25];
                int top1 = 0, top5 = 0, top10 = 0;
//

//                for (double set = 0; set <= 1; set += 0.1) {
//                    alpha = 0.5;
//                    beta = 1;
//                    gama = set;
                FileWriter fw = new FileWriter(metricFile);
                ranks.clear();

                for (int i = 1; i <= config.bugNumber; i++) {
                    if (config.deprecated.contains(i))
                        continue;
                    config.setCurrent(i);
                    if (config.getProjectName().equals("Bears") && bears_no_work.contains(i))
                        continue;
                    System.out.println("handing " + config.getProjectName() + "_" + config.getCurrent());
                    HashSet<String> oracles = PatchSummary.readOracles(config, false); // remapping for ochiai
                    if (oracles.isEmpty())
                        continue;
                    List<Pair<String, Double>> indexes = readVarSuspicious(config); // IsoVar
                    if (project.equals("Math") && i == 39)
                        continue;
//                List<Pair<String, Double>> indexes = readBaseLine_Bears_Ochiai(config, "ochiai"); // Bears
//                    List<Pair<String, Double>> indexes = readBaseLine_D4j_Ochiai(config, "ochiai"); // D4j
                    indexes.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
                    List<Integer> rank = getOracleRanks(indexes, oracles);
                    System.out.println(EvaluationMetric.AP(rank) + "\t" + EvaluationMetric.RR(rank));
                    ranks.add(rank);
                    fw.write(config.getProjectName() + "_" + config.getCurrent() + "\t"
                            + rank + "\t" + EvaluationMetric.AP(rank) + "\t" + EvaluationMetric.RR(rank) + "\n");
//                    }

                    if (!rank.isEmpty() && rank.get(0) < 22) {
                        if (rank.get(0) < 1)
                            top1++;
                        if (rank.get(0) < 5)
                            top5++;
                        if (rank.get(0) < 10)
                            top10++;
                        topn[rank.get(0)]++;
                    }
//                if (i == 26 || i == 83 || i == 97 || i == 139) {
//                    double MAP = EvaluationMetric.MAP(ranks);
//                    double MRR = EvaluationMetric.MRR(ranks);
////                        stat.write(config.getProjectName() + "_" + i + "\t" + String.format("%.1f", set) + "\t" +
////                                MAP + "\t" + MRR + "\n");
////                        if (set == 0.6) {
//                    fw.write("MAP\t" + String.format("%.3f", MAP) + "\n");
//                    fw.write("MRR\t" + String.format("%.3f", MRR) + "\n");
//
//                    System.out.println("TOP-n ");
//                    fw.write("TOP-n ");
//                    for (int j = 1; j <= 20; j++) {
//                        System.out.print(topn[j] + "\t");
//                        fw.write(topn[j] + "\t");
//                    }
//
//                    fw.write("\ntop1 " + top1 + "\n");
//                    fw.write("top5 " + top5 + "\n");
//                    fw.write("top10 " + top10 + "\n\n");
////                        }
//                    ranks = new ArrayList<>();
//                    top1 = top5 = top10 = 0;
//                    topn = new int[25];
//                }

                }
                fw.write("TOP-n\t");
                for (int i = 1; i <= 20; i++) {
                    fw.write(topn[i] + "\t");
                }
                double MAP = EvaluationMetric.MAP(ranks);
                double MRR = EvaluationMetric.MRR(ranks);
                System.out.println(MAP + "\t" + MRR);
                stat.write(project + "\t" + String.format("%.3f", MAP) + "\t" + String.format("%.3f", MRR) +
                        "\t" + String.format("%.1f", alpha) + "\t" + String.format("%.1f", beta) + "\t" +
                        String.format("%.1f", gama) + "\n");
                fw.write("\nMAP\t" + String.format("%.3f", MAP) + "\n");
                fw.write("MRR\t" + String.format("%.3f", MRR) + "\n");
                fw.write("\ntop1 " + top1 + "\n");
                    fw.write("top5 " + top5 + "\n");
                    fw.write("top10 " + top10 + "\n\n");
                fw.write("\n");
                fw.close();
//            }
            }
            stat.write("\n");
            stat.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<Pair<String, Double>> readVarSuspicious(Configuration config) {
        List<Pair<String, Double>> list = new ArrayList<>();
//        File file = new File(config.similaritiesRoot + "_allmethod/" + config.getProjectName() + "/" +
//                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        File file = new File(config.getReport_dir() + "/" + config.getProjectName() + "/" +
                config.getProjectName() + "_" + config.getCurrent() + ".txt");
        try {
            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("\t");
                String var = lineSplit[0] + " " + lineSplit[1];
                String[] statStr = lineSplit[3].split(" ");
                double failFreq = Double.parseDouble(statStr[1]);
                double passFreq = Double.parseDouble(statStr[2]);
                if (passFreq > -0.001 && passFreq < 0.001)
                    passFreq = 0;
                double cosSim = Double.parseDouble(statStr[3].substring(0, statStr[3].length() - 1));
                double stat = failFreq / (failFreq + passFreq) - cosSim * alpha;
                if (cosSim == 0)
                    stat = 0;
                if (cosSim == 0 && config.getProjectName().equals("Math"))
                    stat = 1;

                String[] mutateStr = lineSplit[4].split(" ");
                double failingDiff = Double.parseDouble(mutateStr[1]);
                double passingDiff = Double.parseDouble(mutateStr[2].substring(0, mutateStr[2].length() - 1));
                double mutate = failingDiff - passingDiff * beta;
//                if (passingDiff > -0.001 && passingDiff < 0.001)
//                    passingDiff = 0;
//                if (mutate > -0.2 && mutate < 0.2)
//                    mutate = 0;

                double suspicious;
                if (mutate == 0)
                    suspicious = stat;
//                else if (mutate > 0)
//                    suspicious = (1 - gama) * stat + mutate * gama;
                else
                    suspicious = stat + mutate * gama;
                list.add(new Pair<>(var, suspicious));
            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return list;
    }

    public static List<Integer> getOracleRanks(List<Pair<String, Double>> indexes, HashSet<String> oracles) {
        List<Integer> ranks = new ArrayList<>();
        Collections.sort(indexes);
//		System.out.println(indexes.toString() + "\t" + oracles.toString());
        int index = indexes.size() - 1;
        while (index >= 0) {

            int count = 0;
            int high = index;

            if (oracles.contains(indexes.get(index).getKey())) count++;
//			System.out.println("====\t" + indexes.get(index).toString());
            index--;

            while (index >= 0 && indexes.get(high).getValue().equals(indexes.get(index).getValue())) {
//				System.out.println(indexes.get(index).toString());
                if (oracles.contains(indexes.get(index).getKey())) count++;
                index--;
            }

//			System.out.println(high + "\t" + index);

            int rank = indexes.size() - (high + index + 1) / 2 - (high + index + 1) % 2;

//			System.out.println(index + "\t" + rank + "\t" + count);
            if (count > 0) {
                int flag = -1;
                ranks.add(rank - 1);
                if (count % 2 == 0) flag = 1;
                for (int i = 1; i < count; i++) {
                    ranks.add(rank + (i + 1) / 2 * flag - 1);
                    flag = -1 * flag;
                }
            }
        }
        Collections.sort(ranks);
        return ranks;
    }

    private static List<Pair<String, Double>> readBaseLine_Bears_Ochiai(Configuration config, String baseline) {
        List<Pair<String, Double>> list = new ArrayList<>();
        Map<Integer, Map<VariableTrimInfo, Set<Integer>>> bbMappings = Instrument.readInstrMapping_Bears(config);
        Map<Integer, Map<Integer, Double>> ochiaiMap = readRanking_Bears_Ochiai(config, baseline);
        for (Map.Entry<Integer, Map<VariableTrimInfo, Set<Integer>>> entry : bbMappings.entrySet()) {
            int hash = entry.getKey();
            Map<VariableTrimInfo, Set<Integer>> varMapLines = entry.getValue();
            Map<Integer, Double> lineMapSus = ochiaiMap.get(hash);
            if (lineMapSus != null) {
                List<Pair<String, Double>> l = getVarPairSusInMethod_Bears(varMapLines, lineMapSus);
                list.addAll(l);
            }
        }

        return list;
    }

    private static List<Pair<String, Double>> readBaseLine_D4j_Ochiai(Configuration config, String name) {
        List<Pair<String, Double>> list = new ArrayList<>();
        Map<String, Map<VariableTrimInfo, Set<Integer>>> mappings = Instrument.readInstrMapping_D4j(config);
        List<Triplet<String, Integer, Double>> ochiaiList = readRanking_D4j_Ochiai(config, name);
        for (Map.Entry<String, Map<VariableTrimInfo, Set<Integer>>> entry : mappings.entrySet()) {
            String instr_className = entry.getKey();
            Map<VariableTrimInfo, Set<Integer>> varMapLines = entry.getValue();
            List<Pair<String, Double>> l = getVarPairSusInMethod_D4j(instr_className, varMapLines, ochiaiList);
            list.addAll(l);
        }
        return list;
    }

    private static List<Pair<String, Double>> getVarPairSusInMethod_D4j(String instr_className,
                                                                        Map<VariableTrimInfo, Set<Integer>> varMapLines,
                                                                        List<Triplet<String, Integer, Double>> ochiaiList) {
        List<Pair<String, Double>> list = new ArrayList<>();
        Map<VariableTrimInfo, List<Double>> varSus = new HashMap<>();
        for (Triplet<String, Integer, Double> ochiai : ochiaiList) {
            String ochiai_className = ochiai.getValue0();
            int ochiai_line = ochiai.getValue1();
            double sus = ochiai.getValue2();
            if (instr_className.equals(ochiai_className) ||
                    instr_className.startsWith(ochiai_className + "$")) {
                for (Map.Entry<VariableTrimInfo, Set<Integer>> entry : varMapLines.entrySet()) {
                    VariableTrimInfo var = entry.getKey();
                    Set<Integer> lines = entry.getValue();
                    if (lines.contains(ochiai_line)) {
                        List<Double> suss = varSus.get(var);
                        if (suss == null) {
                            suss = new ArrayList<>();
                            suss.add(sus);
                            varSus.put(var, suss);
                        } else {
                            suss.add(sus);
                        }
                    }
                }
            }
        }
        for (Map.Entry<VariableTrimInfo, List<Double>> entry : varSus.entrySet()) {
            VariableTrimInfo var = entry.getKey();
            List<Double> suss = entry.getValue();
            if (suss.isEmpty())
                list.add(new Pair<>(var.methodHash + " " + var.name, 0d));
            else {
                double sum = 0;
                for (Double sus : suss) {
                    sum += sus;
                }
                list.add(new Pair<>(var.methodHash + " " + var.name, sum / suss.size()));
            }
        }
        return list;
    }

    private static List<Triplet<String, Integer, Double>> readRanking_D4j_Ochiai(Configuration config, String name) {
        List<Triplet<String, Integer, Double>> list = new ArrayList<>();
        try {
//            File file = new File(config.getBaselineRankingFile(name));
            File file = new File("D:/repository/ochiai.txt");
            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("\t");
                String c = lineSplit[0];
                int lineNum = Integer.parseInt(lineSplit[1]);
                double sus = Double.parseDouble(lineSplit[2]);
                Triplet<String, Integer, Double> three = new Triplet<>(c, lineNum, sus);
                list.add(three);
            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

        public static int hashCode(String str) {
        int hash = 0;
        int itr = str.length() / 32;
        if (str.length() % 32 != 0)
            itr += 1;
        for (int i = 0; i < itr; i++) {
            if (i != itr - 1)
                hash += str.substring(32 * i, 32 * i + 32).hashCode();
            else hash += str.substring(32 * i).hashCode();
        }
        return hash;
    }

    private static List<Pair<String, Double>> getVarPairSusInMethod_Bears
            (Map<VariableTrimInfo, Set<Integer>> varMapLines,
             Map<Integer, Double> lineMapSus) {
        List<Pair<String, Double>> list = new ArrayList<>();
        for (Map.Entry<VariableTrimInfo, Set<Integer>> entry : varMapLines.entrySet()) {
            VariableTrimInfo var = entry.getKey();
            Set<Integer> lines = entry.getValue();
            List<Double> varSus = new ArrayList<>();
            for (Map.Entry<Integer, Double> susMap : lineMapSus.entrySet()) {
                int line = susMap.getKey();
                if (lines.contains(line))
                    varSus.add(susMap.getValue());
            }
            if (varSus.isEmpty())
                list.add(new Pair<>(var.methodHash + " " + var.name, 0d));
            else {
                double sum = 0;
                for (Double sus : varSus) {
                    sum += sus;
                }
                list.add(new Pair<>(var.methodHash + " " + var.name, sum / varSus.size()));
            }
        }
        return list;
    }

    private static Map<Integer, Map<Integer, Double>> readRanking_Bears_Ochiai(Configuration config, String
            baseline) {
        File file = new File(config.getBaselineRankingFile(baseline));
        Map<Integer, Map<Integer, Double>> map = new HashMap<>();
        try {
            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("name"))
                    continue;
                int first = line.indexOf(":");
                int second = line.indexOf(";");

                int lineNum = Integer.parseInt(line.substring(first + 1, second));
                double sus = Double.parseDouble(line.substring(second + 1));
                String methodSig = line.substring(0, first);
                int hash = myHashCode(methodSig);
                Map<Integer, Double> methodMap = map.get(hash);
                if (methodMap == null) {
                    methodMap = new HashMap<>();
                    methodMap.put(lineNum, sus);
                    map.put(hash, methodMap);
                } else {
                    methodMap.put(lineNum, sus);
                }
            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }

}


