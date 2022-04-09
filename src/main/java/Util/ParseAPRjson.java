//package Util;
//
//import IsoVar.VariableInfo;
//import com.alibaba.fastjson.JSON;
//import com.alibaba.fastjson.JSONArray;
//import com.alibaba.fastjson.JSONObject;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.NumberFormat;
//import java.util.*;
//
//
//public class ParseAPRjson {
//
//    public static int bugNumber;
//    public static Vector<Integer> deprecated;
//    public static final String APR_base = "APR_baseline/";
//    public static final String APRRootPath = "E:/MutationTest/data/RepairThemAll_experiment/results/Defects4J/";
//    public static final String correct = "E:/MutationTest/data/Exp-2-data/correct-patches/";
//    public static final String incorrect = "E:/MutationTest/data/Exp-2-data/incorrect-patches/";
//    public static final boolean isWithDifferentFile = false;
//
//    public static void main(String[] args) {
//        String[] tools = new String[]{"Arja/0/", "Cardumen/0/", "DynaMoth/7/", "GenProg/0/",
//                "jGenProg/0/", "jKali/0/", "jMutRepair/0/", "Kali/0/", "Nopol/7/", "NPEFix/0/", "RSRepair/0/"};
//        String[] projects = new String[]{"Time", "Chart", "Lang", "Math", "Closure", "Mockito"};
//        try {
//            File apr_task1 = new File(APR_base + "task1_statistic.txt");
//            apr_task1.getParentFile().mkdirs();
//            FileWriter apr_task1_fw = new FileWriter(apr_task1);
//            apr_task1_fw.write("bugID\ttool\torigin_rank_avg\tnew_rank_avg\ttotalCorrect\ttotalPatch\n");
//            int valid = 0, total = 0, nouse = 0; //for task 1
//            Map<String, List<PatchWithRank>> statisticForTools = new HashMap<>();
//
//            File apr_task2 = new File(APR_base + "task2_statistic.txt");
//            FileWriter apr_task2_fw = new FileWriter(apr_task2);
//            apr_task2_fw.write("bugID\ttool\texceedBestPlausible_origin\texceedBestPlausible_new\ttotalCorrect\ttotalPlausible\ttotalPatch\tpercentChange\n");
//
//            File apr_task3 = new File(APR_base + "task3_statistic.txt");
//            FileWriter apr_task3_fw = new FileWriter(apr_task3);
//            apr_task3_fw.write("bugID\ttool\texceed_origin\texceed_new\ttotalPlausible\ttotalPatch\tpercentChange\n");
//
//            for (String project : projects) {
//                getProjectInfo(project);
//                for (String tool : tools) {
//                    for (int id = 1; id <= bugNumber; id++) {
//                        int origin = 0, exceed = 0; //for task 2
//                        int origin_task3 = 0, exceed_task3 = 0; // for task 3
//
//                        if (deprecated.contains(id))
//                            continue;
//                        String path = APRRootPath + project + "/" + id + "/" + tool + "result.json";
//                        File jsonFile = new File(path);
//                        if (!jsonFile.exists())
//                            continue;
////                        System.out.println(path);
//                        String s = ParseFLJson.readJsonFile(jsonFile);
//                        JSONObject jobj = JSON.parseObject(s);
//                        JSONArray patches = (JSONArray) jobj.get("patches");
//                        if (patches.size() == 0)
//                            continue;
//                        String key = "";
//                        String toolPrefix = tool.split("/")[0];
//                        File correctFileDir = new File(correct + toolPrefix + "/Defects4J-" + project + "-" + id);
//                        if (!correctFileDir.exists() || !correctFileDir.isDirectory())
//                            continue;
//                        String[] files = correctFileDir.list();
//                        List<PatchWithRank> correct = new LinkedList<>();
//                        for (String value : files) {
//                            File file = new File(correctFileDir, value);
//                            if (file.getName().endsWith(".txt")) {
//                                String correctPatch = ParseFLJson.readJsonFile(file);
//                                int hash = EvaluationUtil.hashCode(correctPatch);
//                                correct.add(new PatchWithRank(hash, -1, -1));
//                            }
//                        }
//                        List<PatchWithRank> plausibles = new LinkedList<>();
//                        File inCorrectFileDir = new File(incorrect + toolPrefix + "/Defects4J-" + project + "-" + id);
//                        addIncorrect(inCorrectFileDir, plausibles, !correct.isEmpty());
//                        if (isWithDifferentFile) {
//                            File DifferentFilePatchesDir = new File(incorrect + toolPrefix + "-DifferentFilePatches/Defects4J-" + project + "-" + id);
//                            File DifferentFragPatchesDir = new File(incorrect + toolPrefix + "-DifferentFragPatches/Defects4J-" + project + "-" + id);
//                            addIncorrect(DifferentFilePatchesDir, plausibles, !correct.isEmpty());
//                            addIncorrect(DifferentFragPatchesDir, plausibles, !correct.isEmpty());
//                        }
//
//                        switch (toolPrefix) {
//                            case "Arja":
//                            case "DynaMoth":
//                            case "GenProg":
//                            case "Nopol":
//                            case "NPEFix":
//                            case "RSRepair":
//                            case "Kali":
//                                key = "patch";
//                                break;
//                            case "Cardumen":
//                            case "jGenProg":
//                            case "jKali":
//                            case "jMutRepair":
//                                key = "PATCH_DIFF_ORIG";
//                                break;
//                        }
////                        String patch = patches.getString(key);
////                        File file = new File(APR_base + "/" + project + "/" + project + "_" + id + "_" + toolPrefix + ".txt");
////                        FileWriter fw = null;
//                        String folderPath = project + "/" + project + "_" + id;
////                        Map<String, Double> varMapSus = EvaluationUtil_baseline.dataRead_itr(folderPath);
////                        List<VariableInfo> varList = EvaluationUtil_baseline.readData(folderPath);
////                        EvaluationUtil_baseline.reviseSuspicious(varMapSus, varList);//恢复每个变量的sus值
//
//                        List<PatchWithRank> list = new LinkedList<>();
//                        for (int i = 0; i < patches.size(); i++) {
//                            JSONObject subobj = patches.getJSONObject(i);
//                            String patch = subobj.getString(key);
//                            if (patch == null)
//                                continue;
//                            List<Double> sus = new LinkedList<>();
////                            if (i == 0)
////                                fw = new FileWriter(file);
//                            patch = patch.replace("\\n", "\n").replace("\\t", "\t").
//                                    replace("\\", "");
//                            resolvePatch(patch, varList, project, id, tool, sus);
//                            int hash = EvaluationUtil.hashCode(patch);
//                            double suspicious = 0;
//                            if (!sus.isEmpty()) {
//                                double res = 0;
//                                for (double d : sus)
//                                    res += d;
//                                suspicious = res / (double) sus.size();
//                            }
////                            fw.write(hash + "\t" + suspicious + "\n");
//                            list.add(new PatchWithRank(suspicious, hash, i + 1));
//                        }
//                        Comparator<PatchWithRank> comparator = (o1, o2) -> Double.compare(o2.suspicious, o1.suspicious);
//                        list.sort(comparator);
//                        for (int i = 0 ; i < list.size(); i++) {
//                            if (i > 0 && list.get(i).suspicious == list.get(i - 1).suspicious) {
//                                list.get(i).newRank = list.get(i - 1).newRank;
//                            } else {
//                                list.get(i).newRank = i + 1;
//                            }
//                        }
////                        if (fw != null)
////                            fw.close();
//
//                        for (PatchWithRank corr : correct) {
//                            for (PatchWithRank li : list) {
//                                if (li.hash == corr.hash) {
//                                    corr.originRank = li.originRank;
//                                    corr.newRank = li.newRank;
//                                    break;
//                                }
//                            }
//                            if (corr.originRank == -1 || corr.newRank == -1)
//                                System.err.println(project + " " + toolPrefix);
//                        }
//
//                        statisticForTools.putIfAbsent(toolPrefix, new LinkedList<>());
//                        double originRankSum = 0, newRankSum = 0;
//                        for (PatchWithRank corr : correct) {
//                            originRankSum += corr.originRank;
//                            newRankSum += corr.newRank;
//                            statisticForTools.get(toolPrefix).add(corr);
//                        }
//
//
//                        apr_task1_fw.write(project + "_" + id + "\t" + toolPrefix + "\t" + originRankSum / correct.size()
//                                + "\t" + newRankSum / correct.size() + "\t" + correct.size() + "\t" + list.size() + "\n");
//                        if (originRankSum > newRankSum)
//                            valid++;
//                        if (list.size() == 1)
//                            nouse++;
//                        total++;
//
//
//                        int newMaxRank = 99999999;
//                        int originMaxRank = 99999999;
//                        for (PatchWithRank incorr : plausibles) {
//                            for (PatchWithRank li : list) {
//                                if (li.hash == incorr.hash) {
//                                    incorr.originRank = li.originRank;
//                                    incorr.newRank = li.newRank;
//                                    break;
//                                }
//                            }
//                            if (incorr.originRank == -1 || incorr.newRank == -1)
//                                System.err.println(project + " " + toolPrefix);
//                            if (incorr.newRank < newMaxRank)
//                                newMaxRank = incorr.newRank;
//                            if (incorr.originRank < originMaxRank)
//                                originMaxRank = incorr.originRank;
//                        }
//                        for (PatchWithRank corr : correct) {
//                            if (corr.newRank <= newMaxRank)
//                                exceed++;
//                            if (corr.originRank <= originMaxRank)
//                                origin++;
//                        }
//                        apr_task2_fw.write(project + "_" + id + "\t" + toolPrefix + "\t" + origin +
//                                "\t" + exceed + "\t" + correct.size() + "\t" + plausibles.size() + "\t" + list.size() + "\t");
//                        NumberFormat nf = NumberFormat.getPercentInstance();
//                        nf.setMinimumFractionDigits(2);
//                        if (plausibles.isEmpty())
//                            apr_task2_fw.write("100%=>100%\n");
//                        else {
//                            double oorigin = origin / (double) correct.size();
//                            double nnew = exceed / (double) correct.size();
//                            apr_task2_fw.write(nf.format(oorigin));
//                            apr_task2_fw.write("=>");
//                            apr_task2_fw.write(nf.format(nnew) + "\n");
//                        }
//
//                        if (!plausibles.isEmpty()) {
//                            for (PatchWithRank plausible : plausibles) {
//                                if (plausible.originRank <= originRankSum / correct.size())
//                                    origin_task3++;
//                                if (plausible.newRank <= newRankSum / correct.size())
//                                    exceed_task3++;
//                            }
//                            apr_task3_fw.write(project + "_" + id + "\t" + toolPrefix + "\t" + origin_task3 +
//                                    "\t" + exceed_task3 + "\t" + correct.size() + "\t" + plausibles.size() + "\t");
//                            double oorigin_task3 = origin_task3 / (double) plausibles.size();
//                            double nnew_task3 = exceed_task3 / (double) plausibles.size();
//
//                            apr_task3_fw.write(nf.format(oorigin_task3) + "=>" + nf.format(nnew_task3) + "\n");
//                        }
//                    }
//                }
//            }
//            apr_task1_fw.write("valid:" + valid + "/" + total + "\t" + "nouse:" + nouse + "/" + total + "\n\n");
//            apr_task1_fw.write("statistic for tool\n\toriginRank_avg\tnewRank_avg\ttotalCorrect\n");
//            for (Map.Entry<String, List<PatchWithRank>> entry : statisticForTools.entrySet()) {
//                apr_task1_fw.write(entry.getKey() + "\t");
//                List<PatchWithRank> correct = entry.getValue();
//                double originSum = 0, newSum = 0;
//                for (PatchWithRank corr : correct) {
//                    originSum += corr.originRank;
//                    newSum += corr.newRank;
//                }
//                apr_task1_fw.write(originSum / correct.size() + "\t" + newSum / correct.size() + "\t" + correct.size() + "\n");
//
//            }
//            apr_task1_fw.close();
//            apr_task2_fw.close();
//            apr_task3_fw.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    public static void addIncorrect(File inCorrectFileDir, List<PatchWithRank> plausibles, boolean isCorrectExist) {
//        String[] files = inCorrectFileDir.list();
//        if (isCorrectExist && inCorrectFileDir.exists() && inCorrectFileDir.isDirectory()) {
//            for (String value : files) {
//                File file = new File(inCorrectFileDir, value);
//                if (file.getName().endsWith(".txt")) {
//                    String inCorrectPatch = ParseFLJson.readJsonFile(file);
//                    int hash = EvaluationUtil.hashCode(inCorrectPatch);
//                    plausibles.add(new PatchWithRank(hash, -1, -1));
//                }
//            }
//        }
//    }
//
//    public static Set<String> handle(String eString) {
//        Set<String> variable = new HashSet<>();
//        StringTokenizer st = new StringTokenizer(eString, ",!' <>{}()+-*?/\t.;");
//        while (st.hasMoreElements()) {
//            String elem = st.nextElement().toString();
//            if (elem.matches("^[a-zA-z].*"))
//                variable.add(elem);
////            System.out.println(st.nextElement());
//        }
//        return variable;
//    }
//
//    private static void resolvePatch(String patch, List<VariableInfo> varList, String project, int id, String tool, List<Double> sus) throws IOException {
//        Vector<LineWithVariable> add = new Vector<>();
//        Vector<LineWithVariable> sub = new Vector<>();
//        String[] slices = patch.split("\n");
//        String lastClass = null;
//        int addStart = 0, subStart = 0;
//        for (int i = 0; i < slices.length; i++) {
//            if (slices[i].startsWith("+++")) {
//                lastClass = slices[i].split("\t")[0].substring(4);
//                switch (project) {
//                    case "Time":
//                        lastClass = lastClass.substring(lastClass.indexOf("org/joda"));
//                        break;
//                    case "Chart":
//                        lastClass = lastClass.substring(lastClass.indexOf("org/jfree"));
//                        break;
//                    case "Lang":
//                        lastClass = lastClass.substring(lastClass.indexOf("org/apache"));
//                        break;
//                    case "Math":
//                        int index = lastClass.indexOf("org/apache");
//                        if (index != -1) {
//                            lastClass = lastClass.substring(index);
//                        } else lastClass = "org/apache/commons/math/" + lastClass;
//                        break;
//                    case "Closure":
//                        lastClass = lastClass.substring(lastClass.indexOf("com/google"));
//                        break;
//                    case "Mockito":
//                        index = lastClass.indexOf("org/mockito");
//                        if (index == -1) {
//                            lastClass = "org/mockito/" + lastClass;
//                        } else lastClass = lastClass.substring(lastClass.indexOf("org/mockito"));
//                        break;
//                }
//                lastClass = lastClass.substring(0, lastClass.indexOf(".java"));
//                continue;
//            }
//            if ((!add.isEmpty() || !sub.isEmpty()) && (slices[i].startsWith("@@") || i == slices.length - 1)) {
//                resolvePatchSuspicious(varList, lastClass, add, sub, sus);
////                if (!add.isEmpty())
////                    fw.write("a:");
////                for (LineWithVariable line : add) {
////                    if (add.size() - 1 == add.indexOf(line))
////                        fw.write(line.linenumber + "\t");
////                    else fw.write(line.linenumber + " ");
////                }
////                if (!sub.isEmpty())
////                    fw.write("s:");
////                else fw.write("\n");
////                for (LineWithVariable line : sub) {
////                    if (sub.size() - 1 == sub.indexOf(line))
////                        fw.write(line.linenumber + "\n");
////                    else fw.write(line.linenumber + " ");
////                }
//                add = new Vector<>();
//                sub = new Vector<>();
//            }
//
//            if (slices[i].startsWith("@@")) {
//                subStart = Integer.parseInt(slices[i].substring(slices[i].indexOf("-") + 1, slices[i].indexOf(",")));
//                addStart = Integer.parseInt(slices[i].substring(slices[i].indexOf("+") + 1, slices[i].lastIndexOf(",")));
//                continue;
//            }
//
//            if (slices[i].startsWith("+ ") || slices[i].startsWith("+\t")) {
//                Set<String> variable = handle(slices[i]);
//                add.add(new LineWithVariable(addStart, variable));
//                addStart++;
//            } else if (slices[i].startsWith("- ") || slices[i].startsWith("-\t")) {
//                Set<String> variable = handle(slices[i]);
//                sub.add(new LineWithVariable(subStart, variable));
//                subStart++;
//            } else {
//                addStart++;
//                subStart++;
//            }
//        }
//    }
//
//    private static void resolvePatchSuspicious(List<VariableInfo> varList, String className,
//                                               Vector<LineWithVariable> add, Vector<LineWithVariable> sub,
//                                               List<Double> sus) {
//        for (LineWithVariable patchLine : sub) {
//            for (VariableInfo var : varList) {
//                if (var.className.equals(className) && var. == patchLine.linenumber) {
//                    for (String varName : patchLine.variables) {
//                        if (var.variableName.equals(varName)) {
//                            sus.add(var.suspicious);
//                            break;
//                        }
//                    }
//                    sus.add(var.suspicious);
//                }
//            }
//        }
//
//        for (LineWithVariable patchLine : add) {
//            for (VariableInfo var : varList) {
//                if (var.className.equals(className) && (var.sourceLine - patchLine.linenumber) <= 15) {
//                    for (String varName : patchLine.variables) {
//                        if (var.variableName.equals(varName)) {
//                            sus.add(var.suspicious);
//                            break;
//                        }
//                    }
//                    sus.add(var.suspicious);
//                }
//            }
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
//                bugNumber = 133;
//                deprecated.add(63);
//                deprecated.add(93);
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
//}
//
//class LineWithVariable {
//    int linenumber;
//    Set<String> variables;
//
//    public LineWithVariable(int linenumber, Set<String> variables) {
//        this.linenumber = linenumber;
//        this.variables = variables;
//    }
//}
//
//class PatchWithRank {
//    int originRank;
//    int newRank;
//    double suspicious;
//    int hash;
//
//    public PatchWithRank(int hash, int originRank, int newRank) {
//        this.hash = hash;
//        this.originRank = originRank;
//        this.newRank = newRank;
//    }
//
//    public PatchWithRank(double suspicious, int hash, int originRank) {
//        this.suspicious = suspicious;
//        this.hash = hash;
//        this.originRank = originRank;
//    }
//}