//package Util;
//
//import com.xzf.Mutator.BootMutator;
//import com.xzf.ReadVar.VariableInfo;
//import org.w3c.dom.Entity;
//import org.w3c.dom.ls.LSException;
//import sun.awt.HKSCS;
//
//import java.io.*;
//import java.util.*;
//
//import static org.objectweb.asm.Opcodes.ASM5;
//import static org.objectweb.asm.Opcodes.ASM6;
//
//public class EvaluationUtil_baseline {
//    private static final String suspiciousDataRoot = "SuspiciousProject/";
//    private static final String SimilaritiesOriginRoot = "Similarities_origin/";
//    private static final String SimilaritiesRoot = "Similarities/";
//    private static final String SimilaritiesItrRoot = "Similarities_itr/";
//    private static final String SpecialRoot = "Special_origin_RmDup/";
////        private static final String FL_baseline = "FL_baseline/";
//    private static final String FL_baseline = "G:/FL_Baseline/baseline/";
//    private static String metricPath = "Metric_baseline";
//    private static int bugNumber;
//    private static Vector<Integer> deprecated = new Vector<>();
//    //    static boolean isItr = false;
//    static boolean failSubPass = true;
//    static boolean onlyCrash = false;
//    static double alpha = 0.2;
//    private static final boolean isReviseSuspicious = true;
//
//
//    public static void main(String[] args) {
//        if (isReviseSuspicious)
//            metricPath += "_updated";
////        String[] tools = new String[]{"stacktrace", "slicing_count", "predicateswitching",
////                "slicing_intersection", "metallaxis", "ochiai", "muse", "slicing", "dstar"};
//        String[] tools = new String[]{"MCBFL_hybrid"};
////        String[] tools = new String[]{"HSFL_Ochiai"};
//        int bugid;
//        String[] projectNames = new String[]{"Time", "Chart", "Lang", "Math", "Closure"};
////        String[] projectNames = new String[]{"Closure"};
//        try {
//            File statisticsMAP = new File(metricPath + "/statisticsMAP_new.txt");
//            FileWriter statisticsWriteMAP = new FileWriter(statisticsMAP);
//            File statisticsMRR = new File(metricPath + "/statisticsMRR_new.txt");
//            FileWriter statisticsWriteMRR = new FileWriter(statisticsMRR);
//            statisticsWriteMAP.write("MAP\tTime\tChart\tMath\tLang\tClosure\n");
//            statisticsWriteMRR.write("MRR\tTime\tChart\tMath\tLang\tClosure\n");
//
//            for (String tool : tools) {
//                statisticsWriteMAP.write(tool + "\t");
//                statisticsWriteMRR.write(tool + "\t");
//                for (String projectName : projectNames) {
//                    failSubPass = !projectName.equals("Closure");
//                    getProjectInfo(projectName);
//                    BootMutator.getProjectInfo(projectName);
//                    Set<String> crashs = readCrashs("crashs.txt");
//                    File file = new File(metricPath + "/" + projectName + "_" + tool + ".txt");
//                    FileWriter fw = new FileWriter(file);
//                    List<List<Integer>> ranks = new LinkedList<>();
//                    for (bugid = 1; bugid <= bugNumber; bugid++) {
//                        if (deprecated.contains(bugid)) {
//                            continue;
//                        }
//                        if (onlyCrash && !crashs.contains(projectName + "_" + bugid))
//                            continue;
//                        System.out.println("start*****" + projectName + bugid + "*******");
//                        String folderPath = projectName + "/" + projectName + "_" + bugid;
//                        HashSet<String> oracles = new HashSet<>();
////                        Map<String, Double> baselineMap = readBaseline(folderPath, tool, oracles);
//                        Map<String, Double> baselineMap = readBaseline_new(projectName + bugid, tool, oracles);
//                        List<Pair<String, Double>> indexes;
//                        if (isReviseSuspicious) {
//                            Map<String, Double> varMapSus = dataRead_itr(folderPath);
//                            List<VariableInfo> varList = readData(folderPath);
//                            double delta = reviseSuspicious(varMapSus, varList);//恢复每个变量的sus值
////                            if (tool.equals("HSFL_Ochiai") || tool.equals("MCBFL"))
////                                normalize(varList, delta);
//                            indexes = updateSuspicious(baselineMap, varList);
//                        } else {
//                            indexes = new LinkedList<>();
//                            for (Map.Entry<String, Double> entry : baselineMap.entrySet()) {
//                                indexes.add(new Pair<>(entry.getKey(), entry.getValue()));
//                            }
//                        }
//                        List<Integer> rank = getOracleRanks(indexes, oracles);
//                        ranks.add(rank);
//                        fw.write(folderPath + "\t" + rank + "\t" + EvaluationMetric.AP(rank) + "\t" + EvaluationMetric.RR(rank) + "\n");
//                    }
//                    double MAP = EvaluationMetric.MAP(ranks);
//                    double MRR = EvaluationMetric.MRR(ranks);
//                    if (!projectName.equals("Closure")) {
//                        statisticsWriteMAP.write(MAP + "\t");
//                        statisticsWriteMRR.write(MRR + "\t");
//                    } else {
//                        statisticsWriteMAP.write(MAP + "\n");
//                        statisticsWriteMRR.write(MRR + "\n");
//                    }
//                    fw.write("MAP\t" + MAP + "\n");
//                    fw.write("MRR\t" + MRR + "\n");
//                    fw.close();
//                }
//            }
//            statisticsWriteMAP.close();
//            statisticsWriteMRR.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void getProjectInfo(String pid) {
//        deprecated = new Vector<>();
//        switch (pid) {
//            case "Chart":
//                bugNumber = 26;
//                break;
//            case "Closure":
//                deprecated.add(63);
//                deprecated.add(93);
//                deprecated.add(101);
//                deprecated.add(102);
//                bugNumber = 133;
//                break;
//            case "Lang":
//                deprecated.add(2);
//                bugNumber = 65;
//                break;
//            case "Math":
//                bugNumber = 106;
//                break;
//            case "Time":
//                deprecated.add(21);
//                bugNumber = 27;
//                break;
//            case "Mockito":
//                bugNumber = 38;
//                break;
//        }
//    }
//
//    public static Map<String, Double> dataRead(String folderPath) {
//        Map<String, Double> susMap = new HashMap<>();
//        String path = SimilaritiesRoot + folderPath + ".txt";
//        try {
//            File file = new File(path);
//            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
//            BufferedReader bf = new BufferedReader(inputReader);
//            // 按行读取字符串
//            String line;
//            String[] lineSplit;
//            while ((line = bf.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String className = lineSplit[0];
//                String methodName = lineSplit[1];
//                String variableName = lineSplit[2];
//                int sourceLine = Integer.parseInt(lineSplit[3]);
//                double suspicious = Double.parseDouble(lineSplit[7]);
//                suspicious = (double) Math.round(suspicious * 1000000) / 1000000;
//                String var = className + "#" + variableName + "#" + sourceLine;
//                susMap.put(var, suspicious);
//            }
//            bf.close();
//            inputReader.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return susMap;
//    }
//
//    public static Map<String, Double> dataRead_itr(String folderPath) {
//        Map<String, Double> susMap = new HashMap<>();
//        try {
//            String line;
//            String[] lineSplit;
//            File special = new File(SpecialRoot + folderPath + "_variable_Suspicious_Special.txt");
//            InputStreamReader ir_special = new InputStreamReader(new FileInputStream(special));
//            BufferedReader bf_special = new BufferedReader(ir_special);
//            Map<Integer, SusWithRes> map = new HashMap<>();
//            while ((line = bf_special.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String className = lineSplit[0];
//                int sourceLine = Integer.parseInt(lineSplit[1]);
//                String variableName = lineSplit[3];
//                String var = className + variableName + sourceLine;
//                map.put(hashCode(var), new SusWithRes(0, 0, 0));
//            }
//
//            File file_itr = new File(SimilaritiesItrRoot + folderPath + "_itr.txt");
//            InputStreamReader ir_itr = new InputStreamReader(new FileInputStream(file_itr));
//            BufferedReader bf_itr = new BufferedReader(ir_itr);
//
//            while ((line = bf_itr.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String className = lineSplit[0];
//                String variableName = lineSplit[2];
//                if (folderPath.startsWith("Lang"))
//                    variableName = lineSplit[1];
//                int sourceLine = Integer.parseInt(lineSplit[3]);
////                double originSus = Double.parseDouble(lineSplit[4]);
//                double fail = Double.parseDouble(lineSplit[5]);
//                double pass = Double.parseDouble(lineSplit[6]);
//                String var = className + variableName + sourceLine;
//                SusWithRes obj = map.get(hashCode(var));
//                if (obj != null) {
//                    obj.set(0, fail, pass);
//                }
//            }
//
//            File file_origin = new File(SimilaritiesRoot + folderPath + ".txt");
//            InputStreamReader ir_origin = new InputStreamReader(new FileInputStream(file_origin));
//            BufferedReader bf_origin = new BufferedReader(ir_origin);
//
//            // 按行读取字符串
//            String line_origin;
//            while ((line_origin = bf_origin.readLine()) != null) {
//                lineSplit = line_origin.split("\t");
//                String className = lineSplit[0];
////                String methodName = lineSplit[1];
//                String variableName = lineSplit[2];
//                int sourceLine = Integer.parseInt(lineSplit[3]);
//                SusWithRes obj = map.get(hashCode(className + variableName + sourceLine));
//                obj.origin = Double.parseDouble(lineSplit[7]);
//                if (failSubPass) {
//                    obj.origin += (obj.fail - obj.pass) * alpha;
//                } else obj.origin += (obj.pass - obj.fail) * alpha;
//                String var = className + "#" + variableName + "#" + sourceLine;
//                susMap.put(var, (double) Math.round(obj.origin * 1000000) / 1000000);
//            }
//            bf_special.close();
//            bf_origin.close();
//            bf_itr.close();
//            ir_special.close();
//            ir_origin.close();
//            ir_itr.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return susMap;
//    }
//
//    public static int hashCode(String str) {
//        int hash = 0;
//        int itr = str.length() / 32;
//        if (str.length() % 32 != 0)
//            itr += 1;
//        for (int i = 0; i < itr; i++) {
//            if (i != itr - 1)
//                hash += str.substring(32 * i, 32 * i + 32).hashCode();
//            else hash += str.substring(32 * i).hashCode();
//        }
//        return hash;
//    }
//
//    public static List<Integer> getOracleRanks(List<Pair<String, Double>> indexes, HashSet<String> oracles) {
//        List<Integer> ranks = new ArrayList<>();
//        Collections.sort(indexes);
////		System.out.println(indexes.toString() + "\t" + oracles.toString());
//        int index = indexes.size() - 1;
//        while (index >= 0) {
//
//            int count = 0;
//            int high = index;
//
//            if (oracles.contains(indexes.get(index).getKey())) count++;
////			System.out.println("====\t" + indexes.get(index).toString());
//            index--;
//
//            while (index >= 0 && indexes.get(high).getValue().equals(indexes.get(index).getValue())) {
////				System.out.println(indexes.get(index).toString());
//                if (oracles.contains(indexes.get(index).getKey())) count++;
//                index--;
//            }
//
////			System.out.println(high + "\t" + index);
//
//            int rank = indexes.size() - (high + index + 1) / 2 - (high + index + 1) % 2;
//
////			System.out.println(index + "\t" + rank + "\t" + count);
//            if (count > 0) {
//                int flag = -1;
//                ranks.add(rank - 1);
//                if (count % 2 == 0) flag = 1;
//                for (int i = 1; i < count; i++) {
//                    ranks.add(rank + (i + 1) / 2 * flag - 1);
//                    flag = -1 * flag;
//                }
//            }
//        }
//        Collections.sort(ranks);
//        return ranks;
//    }
//
//    public static Set<String> readCrashs(String path) {
//        Set<String> map = new HashSet<>();
//        try {
//            File read = new File(path);
//            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(read));
//            BufferedReader bf = new BufferedReader(inputReader);
//            String line;
//            while ((line = bf.readLine()) != null) {
//                map.add(line);
//            }
//            bf.close();
//            inputReader.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return map;
//    }
//
//    public static Map<String, Double> readBaseline_new(String folderPath, String tool, HashSet<String> oracles) {
//        String path = FL_baseline + tool + "/" + folderPath + ".txt";
//        Map<String, Double> map = new HashMap<>();
//        try {
//            File file = new File(path);
//            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
//            BufferedReader bf = new BufferedReader(inputReader);
//            String line;
//            String[] lineSplit;
//            while ((line = bf.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String id = lineSplit[0].replace(":", "#");
//                double value = Double.parseDouble(lineSplit[1]);
//                map.put(id, value);
//            }
//            inputReader.close();
//            bf.close();
//
//            path = FL_baseline + "faulty/" + folderPath + ".txt";
//            file = new File(path);
//            inputReader = new InputStreamReader(new FileInputStream(file));
//            bf = new BufferedReader(inputReader);
//            while ((line = bf.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String id = lineSplit[0].replace(":", "#");
//                if (lineSplit[1].equals("1.0"))
//                    oracles.add(id);
//            }
//            inputReader.close();
//            bf.close();
//
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return map;
//    }
//
//
//    public static Map<String, Double> readBaseline(String folderPath, String tool, HashSet<String> oracles) {
//        String path = FL_baseline + folderPath + ".txt";
//        Map<String, Double> map = new HashMap<>();
//        if (tool.equals("HSFL_Ochiai"))
//            readBaseline_extra(folderPath, tool, map);
//        try {
//            File file = new File(path);
//            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
//            BufferedReader bf = new BufferedReader(inputReader);
//            String line;
//            String[] lineSplit;
//            while ((line = bf.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String id = lineSplit[0];
//                boolean isOracle = lineSplit[10].equals("1");
//                double value = 99999999;
//                switch (tool) {
//                    case "stacktrace":
//                        value = Double.parseDouble(lineSplit[1]);
//                        break;
//                    case "slicing_count":
//                        value = Double.parseDouble(lineSplit[2]);
//                        break;
//                    case "predicateswitching":
//                        value = Double.parseDouble(lineSplit[3]);
//                        break;
//                    case "slicing_intersection":
//                        value = Double.parseDouble(lineSplit[4]);
//                        break;
//                    case "metallaxis":
//                        value = Double.parseDouble(lineSplit[5]);
//                        break;
//                    case "ochiai":
//                        value = Double.parseDouble(lineSplit[6]);
//                        break;
//                    case "muse":
//                        value = Double.parseDouble(lineSplit[7]);
//                        break;
//                    case "slicing":
//                        value = Double.parseDouble(lineSplit[8]);
//                        break;
//                    case "dstar":
//                        value = Double.parseDouble(lineSplit[9]);
//                        break;
////                    case "faulty":
////                        value = Double.parseDouble(lineSplit[10]);
////                        break;
//                }
//                if (!tool.equals("HSFL_Ochiai") && !tool.equals("MCBFL"))
//                    map.put(id, value);
//
//                if (isOracle)
//                    oracles.add(id);
////                indexes.add(new Pair<>(id, value));
////                list.add(new baseline(id, stacktrace, slicing_count, predicateswitching,
////                        slicing_intersection, metallaxis, ochiai, muse, slicing, dstar, faulty));
//            }
//            bf.close();
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return map;
//    }
//
//    public static void readBaseline_extra(String folderPath, String tool, Map<String, Double> map) {
//        String path = "G:/FL_Baseline/data/HSFL/" + folderPath + "/HSFL/HSFL_ochiai.txt";
////        else path = "G:/FL_Baseline/data/HSFL/" + folderPath + "/baseline/MCBFL_hybrid.txt";
//        try {
//            File file = new File(path);
//            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
//            BufferedReader bf = new BufferedReader(inputReader);
//            String line;
//            String[] lineSplit;
////            double max = -1;
////            double min = 2;
//            while ((line = bf.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String id = lineSplit[0] + "#" + lineSplit[1];
//                double value = Double.parseDouble(lineSplit[2]);
////                if (max < value)
////                    max = value;
////                if(value < min)
////                    min = value;
//                map.put(id, value);
//            }
//
////            for (Map.Entry<String, Double> entry : map.entrySet()) {
////                entry.setValue(entry.getValue() / (max - min));
////            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static void readOracle(String folderPath, HashSet<String> oracles) {
//        String path = SimilaritiesOriginRoot + folderPath + ".txt";
//        try {
//            File file = new File(path);
//            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
//            BufferedReader bf = new BufferedReader(inputReader);
//            // 按行读取字符串
//            String line;
//            String[] lineSplit;
//            while ((line = bf.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String className = lineSplit[0];
//                if (className.contains("$"))
//                    className = className.substring(0, className.indexOf("$"));
//                int sourceLine = Integer.parseInt(lineSplit[3]);
//                boolean isOracle = lineSplit.length == 9;
//                String var = className.replace("/", ".") + "#" + sourceLine;
//                if (isOracle)
//                    oracles.add(var);
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public static List<VariableInfo> readData(String folderPath) {
//        List<VariableInfo> varList = new LinkedList<>();
//        String path = suspiciousDataRoot + folderPath + "_Suspicious.txt";
//        try {
//            File file = new File(path);
//            InputStreamReader inputReader = new InputStreamReader(new FileInputStream(file));
//            BufferedReader bf = new BufferedReader(inputReader);
//            // 按行读取字符串
//            String line;
//            String[] lineSplit;
//            while ((line = bf.readLine()) != null) {
//                lineSplit = line.split("\t");
//                String className = lineSplit[0];
//                int sourceLine = Integer.parseInt(lineSplit[1]);
////                int lastAssignLine = Integer.parseInt(lineSplit[2]);
//                String variableName = lineSplit[3];
//                String variableDesc = lineSplit[4];
////                boolean isAssignment = Boolean.parseBoolean(lineSplit[5]);
//                int varIndex = Integer.parseInt(lineSplit[6]);
//                boolean isStaticMethod = Boolean.parseBoolean(lineSplit[7]);
//                String methodName = lineSplit[8];
//                String methodDesc = lineSplit[9];
//
//                varList.add(new VariableInfo(className, sourceLine, variableName,
//                        variableDesc, varIndex, isStaticMethod, methodName, methodDesc));
//            }
//
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return varList;
//    }
//
//    public static void normalize(List<VariableInfo> varList, double max) {
//        for (VariableInfo var : varList) {
//            var.suspicious /= max;
//        }
//    }
//
//    public static double reviseSuspicious(Map<String, Double> varMapSus, List<VariableInfo> varList) {
//        double max = -1;
//        double min = 2;
//        for (VariableInfo var : varList) {
//            String varId = var.className + "#" + var.variableName + "#" + var.sourceLine;
//            if (varMapSus.get(varId) != null) {
//                var.suspicious = varMapSus.get(varId);
//                if (max < var.suspicious)
//                    max = var.suspicious;
//                if (min > var.suspicious)
//                    min = var.suspicious;
//            }
//        }
//
//        for (VariableInfo var : varList) {
//            if (var.suspicious != 999) {
//                for (VariableInfo preVar : varList) {
//                    if (var.className.equals(preVar.className) && preVar.suspicious == 999
//                            && var.methodName.equals(preVar.methodName) && var.methodDesc.equals(preVar.methodDesc)
//                            && var.variableName.equals(preVar.variableName) && var.varIndex == preVar.varIndex
//                            && var.variableDesc.equals(preVar.variableDesc)) {
//                        preVar.suspicious = var.suspicious;
//                    }
//                }
//            }
//        }
//
//        for (VariableInfo var : varList) {
//            if (var.suspicious == 999) {
//                System.out.println(var.className + " " + var.sourceLine + " " + var.variableName);
//                System.err.println("exist 9999999999!!!!!");
//                System.exit(0);
//            }
////            String reviseClassName;
//            if (var.className.contains("$"))
//                var.className = var.className.substring(0, var.className.indexOf("$"));
////            String id = var.className.replace("/", ".") + "#" + var.sourceLine;
//        }
//        return max - min;
//    }
//
//    public static List<Pair<String, Double>> updateSuspicious(Map<String, Double> baselineMap,
//                                                              List<VariableInfo> varList) {
//        List<Pair<String, Double>> indexes = new LinkedList<>();
//        Map<String, List<Double>> tmp = new HashMap<>();
//        for (VariableInfo var : varList) {
////            if (var.suspicious == 999) {
////                System.err.println("exist 9999999999!!!!!");
////                System.exit(0);
////            }
////            String reviseClassName;
////            if (var.className.contains("$"))
////                reviseClassName = var.className.substring(0, var.className.indexOf("$"));
////            else reviseClassName = var.className;
//            String id = var.className.replace("/", ".") + "#" + var.sourceLine;
//            if (baselineMap.get(id) != null) {
//                if (tmp.get(id) != null) {
//                    tmp.get(id).add(var.suspicious);
//                } else {
//                    List<Double> li = new LinkedList<>();
//                    li.add(var.suspicious);
//                    tmp.put(id, li);
//                }
//            }
//        }
//
//        for (Map.Entry<String, List<Double>> entry : tmp.entrySet()) {
//            List<Double> li = entry.getValue();
//            double avg = 0;
//            for (double d : li) {
//                avg += d;
//            }
//            avg /= li.size();
//            if (baselineMap.get(entry.getKey()) != null) {
//                double base = baselineMap.get(entry.getKey());
//                baselineMap.put(entry.getKey(), base + avg * 0.1);
//            }
//        }
//
//        for (Map.Entry<String, Double> entry : baselineMap.entrySet()) {
//            indexes.add(new Pair<>(entry.getKey(), entry.getValue()));
//        }
//        return indexes;
//    }
//}
//
//
