package Util;

import Instrument.BBMapping;
import Instrument.Instrument;
import IsoVar.Configuration;
import IsoVar.VariableInfo;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static Similarity.Statistical.readStatisticalResult;
import static Util.FileToLines.fileToLines;

public class BoostTBar {
    private static String TBarResultsRoot = "/data/MutationAnalysis/TBar/Results/";

    public static void main(String[] args) throws IOException {
//        String[] projects = new String[]{"Chart_7", "Closure_62", "Lang_10", "Lang_22", "Math_15"
//                , "Math_30", "Math_79", "Math_85", "Time_2"};
        File correctDir = new File(TBarResultsRoot + "/PerfectFL/TBar/FixedBugs/");
        File tbar = new File("tbar_ranking/IsoVar_results.txt");
        tbar.getParentFile().mkdirs();
        FileWriter apr_task4_fw = new FileWriter(tbar);
        apr_task4_fw.write("project\tid\ttotal plausibles\ttotal correct\trank_before\trank_after\n");
        for (File file : Objects.requireNonNull(correctDir.listFiles())) {
            Configuration config = new Configuration();
            String[] split = file.getName().split("_");
            String name = split[0];
            config.setProjectName(name);
            if (split[1].contains("-"))
                split[1] = split[1].substring(0, (split[1].indexOf('-')));
            int id = Integer.parseInt(split[1]);
            if (!config.setCurrent(id))
                continue;
            if (name.equals("Closure") && (id == 102))
                continue;
            System.out.println("Start***" + file.getName() + "******");

            List<TBar> plausibles = readTBarResults(TBarResultsRoot + "FixPatterns/TBar/", config);
            boolean isCorrect = labelCorrectPatch(TBarResultsRoot + "PerfectFL/TBar/FixedBugs", config, plausibles);
            if (!isCorrect)
                continue;
            int totalCorrect = 0, originRank = -1, newRank = -1;

            Comparator<TBar> comparator = Comparator.comparingInt(o -> o.originRank);
            plausibles.sort(comparator);
            for (int i = 0; i < plausibles.size(); i++) {
                if (plausibles.get(i).isCorrect) {
                    originRank = i + 1;
                    break;
                }
            }

            comparator = (o1, o2) -> {
                int cmp1 = Double.compare(o2.suspicious, o1.suspicious);
                if (cmp1 == 0)
                    return Integer.compare(o1.originRank, o2.originRank);
                else return cmp1;
            };
            plausibles.sort(comparator);
            for (int i = 0; i < plausibles.size(); i++) {
                if (plausibles.get(i).isCorrect && newRank == -1)
                    newRank = i + 1;
                plausibles.get(i).newRank = i + 1;
            }
            apr_task4_fw.write(config.getProjectName() + "\t" + config.getCurrent() + "\t" + plausibles.size()
                    + "\t1\t" + originRank + "\t" + newRank + "\n");
        }
        apr_task4_fw.close();
    }

    private static boolean labelCorrectPatch(String path, Configuration config, List<TBar> plausibles) {
        boolean isCorrect = false;
        File dir = new File(path);
        if (!dir.isDirectory()) {
            System.out.printf("The path " + dir.getAbsolutePath() + " is not valid");
            System.exit(0);
        }
        for (File sub : Objects.requireNonNull(dir.listFiles())) {
            String pathPartten = config.getProjectName() + "_" + config.getCurrent();
            if (sub.getName().equals(pathPartten) ||
                    sub.getName().startsWith(pathPartten + "_") ||
                    sub.getName().startsWith(pathPartten + "-")) {
                isCorrect = !sub.getName().contains("P");
                for (File file : Objects.requireNonNull(sub.listFiles())) {
                    String name = file.getName();
                    int id = Integer.parseInt(name.substring(name.indexOf('_') + 1, name.indexOf('.')));
                    for (TBar plausible : plausibles) {
                        if (plausible.originRank == id) {
                            plausible.isCorrect = true;
                            break;
                        }
                    }
                }
            }
        }
        return isCorrect;
    }

    private static List<TBar> readTBarResults(String path, Configuration config) {
        Map<String, Double> varSusMap = readStatisticalResult(config);
        Map<Integer, BBMapping[]> bbMappings = Instrument.readInstrMapping(config);
        File dir = new File(path);
        List<TBar> plausibles = new ArrayList<>();
        if (!dir.isDirectory()) {
            System.out.printf("The path " + dir.getAbsolutePath() + " is not valid");
            System.exit(0);
        }
        for (File subdir : Objects.requireNonNull(dir.listFiles())) {
            File bug = new File(path + subdir.getName() + "/FixedBugs/"
                    + config.getProjectName() + "_" + config.getCurrent());
            if (!bug.exists())
                continue;
            for (File patch : Objects.requireNonNull(bug.listFiles())) {
                TBar plausible = resolvePatch(patch, config.getProjectName(),
                        varSusMap, bbMappings);
                plausibles.add(plausible);
            }
        }

        return plausibles;
    }

    private static TBar resolvePatch(File patch, String name, Map<String, Double> varSusMap,
                                     Map<Integer, BBMapping[]> bbMappings) {
        Set<String> vars = new HashSet<>();
        String patchName = patch.getName();
        int id = Integer.parseInt(patchName.substring(patchName.indexOf('_') + 1, patchName.indexOf('.')));
        List<String> slices = fileToLines(patch.getAbsolutePath());
        String lastClass = null;
        int methodHash = -1;
        int addStart = 0, subStart = 0;
        for (String line : slices) {
            if (line.startsWith("+++")) {
                lastClass = line.split("\t")[0].substring(4);
                switch (name) {
                    case "Time":
                        lastClass = lastClass.substring(lastClass.indexOf("org/joda"));
                        break;
                    case "Chart":
                        lastClass = lastClass.substring(lastClass.indexOf("org/jfree"));
                        break;
                    case "Lang":
                        lastClass = lastClass.substring(lastClass.indexOf("org/apache"));
                        break;
                    case "Math":
                        int index = lastClass.indexOf("org/apache");
                        if (index != -1) {
                            lastClass = lastClass.substring(index);
                        } else lastClass = "org/apache/commons/math/" + lastClass;
                        break;
                    case "Closure":
                        lastClass = lastClass.substring(lastClass.indexOf("com/google"));
                        break;
                    case "Mockito":
                        index = lastClass.indexOf("org/mockito");
                        if (index == -1) {
                            lastClass = "org/mockito/" + lastClass;
                        } else lastClass = lastClass.substring(lastClass.indexOf("org/mockito"));
                        break;
                }
                lastClass = lastClass.substring(0, lastClass.indexOf(".java")).replace("/", ".");

            } else if (line.startsWith("@@")) {
                subStart = Integer.parseInt(line.substring(line.indexOf("-") + 1, line.indexOf(",")));
                addStart = Integer.parseInt(line.substring(line.indexOf("+") + 1, line.indexOf(",", line.indexOf("+"))));
            } else if (line.startsWith("+ ") || line.startsWith("+\t")) {
                if (methodHash == -1) {
                    methodHash = resolveMethod(lastClass, addStart, bbMappings);
                }
                Set<String> variable = handle(line);
                vars.addAll(variable);
                addStart++;
            } else if (line.startsWith("- ") || line.startsWith("-\t")) {
                if (methodHash == -1) {
                    methodHash = resolveMethod(lastClass, subStart, bbMappings);
                }
                Set<String> variable = handle(line);
                vars.addAll(variable);
                subStart++;
            } else {
                addStart++;
                subStart++;
            }
        }
        if (methodHash == -1) {
            methodHash = resolveMethod(lastClass, addStart, bbMappings);
        }
        if (methodHash == -1) {
            methodHash = resolveMethod(lastClass, addStart + 1, bbMappings);
        }
        if (methodHash == -1) {
            methodHash = resolveMethod(lastClass, addStart - 1, bbMappings);
        }
        List<Double> sus = resolvePatchSuspicious(methodHash, vars, varSusMap);
        return new TBar(id, vars, sus);
    }

    public static List<Double> resolvePatchSuspicious(int methodHash, Set<String> vars, Map<String, Double> varSusMap) {
        List<Double> sus = new ArrayList<>();
        for (String var : vars) {
            String id = methodHash + "#" + var;
            if (varSusMap.containsKey(id))
                sus.add(varSusMap.get(id));
//            if (!id.contains(".")) {
//                id = methodHash + "#this." + var;
//                if (varSusMap.containsKey(id))
//                    sus.add(varSusMap.get(id));
//            } else {
//                String newV = var.substring(var.indexOf('.'));
//                id = methodHash + "#" + newV;
//                if (varSusMap.containsKey(id))
//                    sus.add(varSusMap.get(id));
//
//                newV = var.substring(0, var.indexOf('.'));
//                id = methodHash + "#" + newV;
//                if (varSusMap.containsKey(id))
//                    sus.add(varSusMap.get(id));
//
//                id = methodHash + "#this." + newV;
//                if (varSusMap.containsKey(id))
//                    sus.add(varSusMap.get(id));
//            }
        }
        return sus;
//
    }

    public static int resolveMethod(String className, int line, Map<Integer, BBMapping[]> bbMappings) {
        for (Map.Entry<Integer, BBMapping[]> entry : bbMappings.entrySet()) {
            int hash = entry.getKey();
            for (BBMapping bbMapping : entry.getValue()) {
                for (VariableInfo var : bbMapping.vars) {
                    if (var.className.equals(className) || var.className.startsWith(className + "$")) {
                        if (var.getLine() == line)
                            return hash;
                    }
                }
            }
        }
        return -1;
    }

    public static Set<String> handle(String eString) {
        Set<String> variable = new HashSet<>();
        StringTokenizer st = new StringTokenizer(eString, ",!' <>{}()+-*?/\t;[]");
        while (st.hasMoreElements()) {
            String elem = st.nextElement().toString();
            if (elem.matches("^[a-zA-z].*")) {
                variable.add(elem);
                if (!elem.contains(".")) {
                    variable.add("this." + elem);
                } else {
                    String newV = elem.substring(elem.indexOf('.'));
                    variable.add(newV);

                    newV = elem.substring(0, elem.indexOf('.'));
                    variable.add(newV);

                    variable.add("this." + newV);
                }
//            System.out.println(st.nextElement());
            }
        }
        return variable;
    }

}


class TBar {
    int originRank;
    double suspicious;
    boolean isCorrect = false;
    int newRank = -1;
    Set<String> vars;
    List<Double> sus;

    public TBar(int originRank, Set<String> vars, List<Double> sus) {
        this.originRank = originRank;
        double sum = 0;
        for (Double s : sus) {
            sum += s;
        }
        this.suspicious = sus.isEmpty() ? 0 : sum / sus.size();
        this.vars = vars;
        this.sus = sus;

    }
}
