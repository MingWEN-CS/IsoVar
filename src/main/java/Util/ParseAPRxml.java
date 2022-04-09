//package Util;
//
//import IsoVar.VariableInfo;
//import org.dom4j.Attribute;
//import org.dom4j.Document;
//import org.dom4j.DocumentException;
//import org.dom4j.Element;
//import org.dom4j.io.SAXReader;
//
//import java.io.*;
//import java.net.MalformedURLException;
//import java.text.NumberFormat;
//import java.util.*;
//
//
//public class ParseAPRxml {
//    public static final String APR_base = "APR_baseline/";
//    //    public static final String rankingPath = "/data/MutationAnalysis/prapr/ranking/";
//    public static final String rankingPath = "G:/ranking/";
//
//    public static void main(String[] args) throws IOException {
//        List<Integer> closure = Arrays.asList(10, 11, 31, 46, 73, 86);
////        List<Integer> closure = Arrays.asList(62, 70, 92, 126);
//        List<Integer> time = Arrays.asList(4, 11);
////        List<Integer> time = Arrays.asList(11);
////        List<Integer> chart = Arrays.asList(1, 8, 11, 12, 20, 24, 26);
//        List<Integer> chart = Arrays.asList(20, 24, 26);
//        List<Integer> lang = Arrays.asList(26, 33, 59);
////        List<Integer> lang = Arrays.asList(33);
//        List<Integer> math = Arrays.asList(5, 33, 34, 50, 58, 59, 70, 75, 82);
////        List<Integer> math = Arrays.asList(5);
//        List<Integer> mockito = Arrays.asList(5);
////        String[] projects = new String[]{"Math"};
//        String[] projects = new String[]{"Chart", "Lang", "Math", "Closure", "Mockito"};
//        File apr_task1 = new File(APR_base + "task1_statistic_prapr.txt");
//        FileWriter apr_task1_fw = new FileWriter(apr_task1);
//        apr_task1_fw.write("bugID\torigin_rank_avg\tnew_rank_avg\ttotalCorrect\ttotalPatch\n");
//        int valid = 0, total = 0, nouse = 0; //for task 1
//
//        File apr_task2 = new File(APR_base + "task2_statistic_prapr.txt");
//        FileWriter apr_task2_fw = new FileWriter(apr_task2);
//        apr_task2_fw.write("bugID\torigin_rank_avg\tnew_rank_avg\ttotalCorrect\ttotalPlausible\ttotalPatch\n");
//
//
//        File apr_task3 = new File(APR_base + "task3_statistic_prapr.txt");
//        FileWriter apr_task3_fw = new FileWriter(apr_task3);
//        apr_task3_fw.write("bugID\texceed_origin\texceed_new\ttotalPlausible\ttotalPatch\tpercentChange\n");
//
//
//        for (String project : projects) {
//            ParseAPRjson.getProjectInfo(project);
//            for (int id = 1; id <= ParseAPRjson.bugNumber; id++) {
//                if (ParseAPRjson.deprecated.contains(id))
//                    continue;
//                if (project.equals("Time") && !time.contains(id))
//                    continue;
//                if (project.equals("Chart") && !chart.contains(id))
//                    continue;
//                if (project.equals("Lang") && !lang.contains(id))
//                    continue;
//                if (project.equals("Math") && !math.contains(id))
//                    continue;
//                if (project.equals("Closure") && !closure.contains(id))
//                    continue;
//                if (project.equals("Mockito") && !mockito.contains(id))
//                    continue;
//                int origin = 0, exceed = 0; //for task 2
//                int origin_task3 = 0, exceed_task3 = 0; // for task 3
//                File projectRankingDir = new File(rankingPath + project + "-Processed/" + id);
//                if (!projectRankingDir.exists() || !projectRankingDir.isDirectory())
//                    continue;
//                System.out.println("Start***" + project + "_" + id + "******");
//                List<Prapr> plausibles = new LinkedList<>();
//                List<Prapr> correct = readFixReport(rankingPath + project + "-Processed/" + id + "/fix-report.log", plausibles);
//                List<Prapr> patches = readXML(rankingPath + project + "-Processed/" + id + "/mutations.xml");
//
////                System.out.println("correct");
////                for (Prapr prapr : correct) {
////                    System.out.println(prapr.id + " " + prapr.mutantsHash);
////                }
////                System.out.println("plausibles");
////                for (Prapr prapr : plausibles) {
////                    System.out.println(prapr.id + " " + prapr.mutantsHash);
////                }
////                System.out.println("patches");
////                for (Prapr prapr : patches) {
////                    System.out.println(prapr.id + " " + prapr.mutantsHash);
////                }
//
//                String folderPath = project + "/" + project + "_" + id;
//                Map<String, Double> varMapSus = EvaluationUtil_baseline.dataRead_itr(folderPath);
//                List<VariableInfo> varList = EvaluationUtil_baseline.readData(folderPath);
//                EvaluationUtil_baseline.reviseSuspicious(varMapSus, varList);//恢复每个变量的sus值
//
//                resolvePatch(patches, varList);
//
//                for (Prapr cor : correct) {
//                    for ( patch : patches) {
//                        if (cor.mutantsHash == patch.mutantsHash) {
//                            cor.originRank = patch.originRank;
//                            cor.newRank = patch.newRank;
//                            cor.originSuspicious = patch.originSuspicious;
//                            cor.newSuspicious = patch.newSuspicious;
//                            break;
//                        }
//                    }
//                }
//
//                double originRankSum = 0, newRankSum = 0;
//                for (Prapr corr : correct) {
//                    originRankSum += corr.originRank;
//                    newRankSum += corr.newRank;
//                }
//
//                apr_task1_fw.write(project + "_" + id + "\t" + originRankSum / correct.size()
//                        + "\t" + newRankSum / correct.size() + "\t" + correct.size() + "\t" + patches.size() + "\n");
//                if (originRankSum > newRankSum)
//                    valid++;
//                if (patches.size() == 1)
//                    nouse++;
//                total++;
//
//                int newMaxRank = 99999999;
//                int originMaxRank = 99999999;
//                for (Prapr incorr : plausibles) {
//                    for (Prapr patch : patches) {
//                        if (patch.mutantsHash == incorr.mutantsHash) {
//                            incorr.originRank = patch.originRank;
//                            incorr.newRank = patch.newRank;
//                            break;
//                        }
//                    }
//                    if (incorr.originRank == -1 || incorr.newRank == -1)
//                        System.err.println(project + " " + id);
//                    if (incorr.newRank < newMaxRank)
//                        newMaxRank = incorr.newRank;
//                    if (incorr.originRank < originMaxRank)
//                        originMaxRank = incorr.originRank;
//                }
//                for (Prapr corr : correct) {
//                    if (corr.newRank <= newMaxRank)
//                        exceed++;
//                    if (corr.originRank <= originMaxRank)
//                        origin++;
//                }
//                apr_task2_fw.write(project + "_" + id + "\t" + origin +
//                        "\t" + exceed + "\t" + correct.size() + "\t" + plausibles.size() + "\t");
//                NumberFormat nf = NumberFormat.getPercentInstance();
//                nf.setMinimumFractionDigits(2);
//                if (plausibles.isEmpty())
//                    apr_task2_fw.write("100%=>100%\n");
//                else {
//                    double oorigin = origin / (double) correct.size();
//                    double nnew = exceed / (double) correct.size();
//                    apr_task2_fw.write(nf.format(oorigin));
//                    apr_task2_fw.write("=>");
//                    apr_task2_fw.write(nf.format(nnew) + "\n");
//                }
//
//                if (!plausibles.isEmpty()) {
//                    for (Prapr plausible : plausibles) {
//                        if (plausible.originRank <= originRankSum / correct.size())
//                            origin_task3++;
//                        if (plausible.newRank <= newRankSum / correct.size())
//                            exceed_task3++;
//                    }
//                    apr_task3_fw.write(project + "_" + id + "\t" + origin_task3 +
//                            "\t" + exceed_task3 + "\t" + correct.size() + "\t" + plausibles.size() + "\t");
//                    double oorigin_task3 = origin_task3 / (double) plausibles.size();
//                    double nnew_task3 = exceed_task3 / (double) plausibles.size();
//
//                    apr_task3_fw.write(nf.format(oorigin_task3) + "=>" + nf.format(nnew_task3) + "\n");
//                }
//
////                for (Prapr plausible : plausibles) {
////                    for (Prapr patch : patches) {
////                        if(plausible.mutantsHash == patch.mutantsHash){
////                            plausible.originRank = patch.originRank;;
////                            plausible.newRank = patch.newRank;;
////                        }
////                    }
////                }
//
//            }
//        }
//        apr_task1_fw.write("valid:" + valid + "/" + total + "\t" + "nouse:" + nouse + "/" + total + "\n\n");
//        apr_task1_fw.close();
//        apr_task2_fw.close();
//    }
//
//    public static void resolvePatch(List<Prapr> patches, List<VariableInfo> varList) {
//        Comparator<Prapr> comparator = (o1, o2) -> Double.compare(o2.originSuspicious, o1.originSuspicious);
//        patches.sort(comparator);
//        for (int i = 0; i < patches.size(); i++) {
////            if (i > 0 && patches.get(i).suspicious == patches.get(i - 1).suspicious) {
////                patches.get(i).originRank = patches.get(i - 1).originRank;
////            } else {
//            patches.get(i).originRank = i + 1;
////            }
//        }
////        for (VariableInfo variableInfo : varList) {
////            System.out.println(variableInfo.className + " " + variableInfo.sourceLine + " " + variableInfo.variableName + " " + variableInfo.suspicious);
////        }
//
//        Map<String, List<Double>> tmp = new HashMap<>();
//        for (VariableInfo var : varList) {
//            String id = var.className.replace("/", ".") + "#" + var.sourceLine;
//            if (tmp.get(id) != null) {
//                tmp.get(id).add(var.suspicious);
//            } else {
//                List<Double> li = new LinkedList<>();
//                li.add(var.suspicious);
//                tmp.put(id, li);
//            }
//        }
//        for (Prapr patch : patches) {
//            if (tmp.get(patch.id) != null) {
//                List<Double> li = tmp.get(patch.id);
//                double avg = 0;
//                for (double d : li) {
//                    avg += d;
//                }
//                avg /= li.size();
//                patch.newSuspicious = patch.originSuspicious + avg * 0.2;
//            }
//        }
//
//        Comparator<Prapr> comparator_new = (o1, o2) -> Double.compare(o2.newSuspicious, o1.newSuspicious);
//        patches.sort(comparator_new);
//        for (int i = 0; i < patches.size(); i++) {
////            if (i > 0 && patches.get(i).suspicious == patches.get(i - 1).suspicious) {
////                patches.get(i).newRank = patches.get(i - 1).newRank;
////            } else {
//            patches.get(i).newRank = i + 1;
////            }
//        }
//    }
//
//
//    public static Map<String, String> reviseLineNumber(String bugid) {
//        Map<String, String> map = new HashMap<>();
//        switch (bugid) {
//            case "Chart_1":
//                map.put("org.jfree.chart.rendere.category.AbstractCategoryItemRenderer#1797", "org.jfree.chart.rendere.category.AbstractCategoryItemRenderer#1796");
//                break;
//            case "Lang_59":
//                map.put("org.apache.commons.lang.text.StrBuilder#880", "org.apache.commons.lang.text.StrBuilder#879");
//                map.put("org.apache.commons.lang.text.StrBuilder#884", "org.apache.commons.lang.text.StrBuilder#882");
//                break;
//            case "Math_5":
//                map.put("org.apache.commons.math3.complex.Complex#305", "org.apache.commons.math3.complex.Complex#300");
//                map.put("org.apache.commons.math3.complex.Complex#345", "org.apache.commons.math3.complex.Complex#344");
//                break;
//            case "Math_50":
//                map.put("org.apache.commons.math.analysis.solvers.BaseSecantSolver#187", "org.apache.commons.math.analysis.solvers.BaseSecantSolver#131");
//                map.put("org.apache.commons.math.analysis.solvers.BaseSecantSolver#188", "org.apache.commons.math.analysis.solvers.BaseSecantSolver#131");
//                map.put("org.apache.commons.math.analysis.solvers.BaseSecantSolver#189", "org.apache.commons.math.analysis.solvers.BaseSecantSolver#131");
//
//        }
//        return map;
//    }
//
//    public static List<Prapr> readXML(String path) {
//        List<Prapr> patchs = new LinkedList<>();
//        SAXReader reader = new SAXReader();
//        try {
//            Document document = reader.read(new File(path));
//            Element element = document.getRootElement();
//            Iterator it = element.elementIterator();
//            while (it.hasNext()) {
//                Element ele = (Element) it.next();
//                Iterator itt = ele.elementIterator();
//                double suspicious = -1;
//                String description = null;
////                String mutator = null;
//                String linenumber = null;
//                String className = null;
//                while (itt.hasNext()) {
//                    Element eleChild = (Element) itt.next();
//                    if (eleChild.getName().equals("mutatedClass")) {
//                        className = eleChild.getStringValue();
//                        if (className.contains("$"))
//                            className = className.substring(0, className.indexOf("$"));
//                    }
//                    if (eleChild.getName().equals("lineNumber"))
//                        linenumber = eleChild.getStringValue();
////                    if (eleChild.getName().equals("mutator"))
////                        mutator = eleChild.getStringValue();
//                    if (eleChild.getName().equals("description"))
//                        description = eleChild.getStringValue();
//                    if (eleChild.getName().equals("suspValue"))
//                        suspicious = Double.parseDouble(eleChild.getStringValue());
//                }
//                int hash = BootMutator.hashCode(className + linenumber + description);
//                String id = className + "#" + linenumber;
//                patchs.add(new Prapr(-1, -1, hash, suspicious, id));
//            }
//        } catch (DocumentException | MalformedURLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//        return patchs;
//    }
//
//    public static List<Prapr> readFixReport(String path, List<Prapr> plausibles) {
//        List<Prapr> correct = new LinkedList<>();
//        try {
//            String line;
//            boolean isCorrect = false;
//            File fix = new File(path);
//            InputStreamReader ir = new InputStreamReader(new FileInputStream(fix));
//            BufferedReader bf = new BufferedReader(ir);
//            while ((line = bf.readLine()) != null) {
//                if (line.startsWith("*"))
//                    isCorrect = true;
//                if (line.startsWith("\tMutator")) {
////                    String mutator = line.substring(line.indexOf("Mutator") + 9);
//                    line = bf.readLine();
//                    String description = line.substring(line.indexOf("description") + 15);
//                    line = bf.readLine();
//                    String className = line.substring(line.indexOf("File Name") + 11).replace("/", ".");
//                    if (className.contains("google"))
//                        className = className.substring(0, className.indexOf(".java", 20));
//                    else className = className.substring(0, className.indexOf(".java"));
//                    line = bf.readLine();
//                    String linenumber = line.substring(line.indexOf("Line Number") + 13);
//                    int hash = BootMutator.hashCode(className + linenumber + description);
//                    String id = className + "#" + linenumber;
//                    Prapr prapr = new Prapr(-1, -1, hash, id);
//                    if (isCorrect)
//                        correct.add(prapr);
//                    else plausibles.add(prapr);
//                    isCorrect = false;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return correct;
//    }
//}
//
//
//class Prapr {
//    int originRank;
//    int newRank;
//    int mutantsHash;
//    double originSuspicious;
//    double newSuspicious;
//    String id; //class#line
//
//    public Prapr(int mutantsHash, double originSuspicious, String id) {
//        this.mutantsHash = mutantsHash;
//        this.originSuspicious = originSuspicious;
//        this.id = id;
//    }
//
//    public Prapr(int originRank, int newRank, int mutantsHash, double originSuspicious, String id) {
//        this.originRank = originRank;
//        this.newRank = newRank;
//        this.mutantsHash = mutantsHash;
//        this.originSuspicious = originSuspicious;
//        this.id = id;
//    }
//
//    public Prapr(int originRank, int newRank, int mutantsHash, String id) {
//        this.originRank = originRank;
//        this.newRank = newRank;
//        this.mutantsHash = mutantsHash;
//        this.id = id;
//    }
//
//    public Prapr(int mutantsHash, String id) {
//        this.mutantsHash = mutantsHash;
//        this.id = id;
//    }
//}