package Util;

import Instrument.BBMapping;
import Instrument.Instrument;
import IsoVar.Configuration;
import IsoVar.VariableInfo;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.*;
import java.nio.file.Files;
import java.text.NumberFormat;
import java.util.*;

import static Instrument.Instrument.myHashCode;
import static Similarity.Statistical.readStatisticalResult;


public class BoostPrapr {
    public static final String APR_base = "APR_baseline/";
    public static final String rankingPath = "/data/MutationAnalysis/prapr/ranking/";
//    public static final String rankingPath = "E:/MutationTest/ranking/";

    public static void main(String[] args) throws IOException {
//        String[] projects = new String[]{"Closure"};
        String[] projects = new String[]{"Time", "Chart", "Lang", "Math", "Mockito"};
//        String[] projects = new String[]{"Time", "Chart", "Lang", "Math", "Closure", "Mockito"};
        File apr_task1 = new File(APR_base + "task1_statistic_prapr.txt");
        apr_task1.getParentFile().mkdirs();
        FileWriter apr_task1_fw = new FileWriter(apr_task1);
        apr_task1_fw.write("bugID\torigin_rank_avg\tnew_rank_avg\ttotalCorrect\ttotalPatch\n");
        int valid = 0, total = 0, nouse = 0; //for task 1

        File apr_task2 = new File(APR_base + "task2_statistic_prapr.txt");
        apr_task2.getParentFile().mkdirs();
        FileWriter apr_task2_fw = new FileWriter(apr_task2);
        apr_task2_fw.write("bugID\torigin_rank_avg\tnew_rank_avg\ttotalCorrect\ttotalPlausible\ttotalPatch\n");


        File apr_task3 = new File(APR_base + "task3_statistic_prapr.txt");
        apr_task3.getParentFile().mkdirs();
        FileWriter apr_task3_fw = new FileWriter(apr_task3);
        apr_task3_fw.write("bugID\texceed_origin\texceed_new\ttotalPlausible\ttotalPatch\tpercentChange\n");


        File apr_task4 = new File("prapr_ranking/IsoVar_results.txt");
        apr_task4.getParentFile().mkdirs();
        FileWriter apr_task4_fw = new FileWriter(apr_task4,true);
        apr_task4_fw.write("project\tid\ttotal plausibles\ttotal correct\trank_before\trank_after\n");


        for (String project : projects) {
            Configuration config = new Configuration();
            config.setProjectName(project);

            for (int id = 1; id <= config.bugNumber; id++) {
                if (!config.setCurrent(id))
                    continue;
//                if (config.deprecated.contains(id))
//                    continue;
                int origin = 0, exceed = 0; //for task 2
                int origin_task3 = 0, exceed_task3 = 0; // for task 3
                File projectRankingDir = new File(rankingPath + project + "-Processed/" + id);
                if (!projectRankingDir.exists() || !projectRankingDir.isDirectory()) {
//                    System.out.println("The directory "+projectRankingDir.getAbsolutePath()+" is not exist");
                    continue;
                }
                System.out.println("Start***" + project + "_" + id + "******");
                List<Prapr> plausibles = readFixReport(rankingPath + project + "-Processed/" + id + "/fix-report.log");
                List<Prapr> patches = readXML(rankingPath + project + "-Processed/" + id + "/mutations.xml");


//                Map<String, Double> varMapSus = EvaluationUtil_baseline.dataRead_itr(folderPath);
//                List<VariableInfo> varList = reviseSuspicious(config); //read results and get suspicious value for each variable

                resolvePatch(patches, config);
                int correctSize = 0;

                for (Prapr cor : plausibles) {
                    for (Prapr patch : patches) {
                        if (cor.mutantsHash == patch.mutantsHash) {
                            cor.originRank = patch.originRank;
                            cor.newRank = patch.newRank;
                            cor.originSuspicious = patch.originSuspicious;
                            cor.newSuspicious = patch.newSuspicious;
                            break;
                        }
                        if (patch.isCorrect)
                            correctSize++;
                    }
                }
                int incorrectSize = plausibles.size() - correctSize;

                double originRankSum = 0, newRankSum = 0;
                for (Prapr corr : plausibles) {
                    if (corr.isCorrect) {
                        originRankSum += corr.originRank;
                        newRankSum += corr.newRank;
                    }
                }

                apr_task1_fw.write(project + "_" + id + "\t" + originRankSum / correctSize
                        + "\t" + newRankSum / correctSize + "\t" + correctSize + "\t" + patches.size() + "\n");
                if (originRankSum > newRankSum)
                    valid++;
                if (patches.size() == 1)
                    nouse++;
                total++;

                int newMaxRank = 99999999;
                int originMaxRank = 99999999;
                for (Prapr patch : plausibles) {
                    if (!patch.isCorrect) {
                        if (patch.originRank == -1 || patch.newRank == -1)
                            System.err.println(project + " " + id);
                        if (patch.newRank < newMaxRank)
                            newMaxRank = patch.newRank;
                        if (patch.originRank < originMaxRank)
                            originMaxRank = patch.originRank;
                    } else {
                        if (patch.newRank <= newMaxRank)
                            exceed++;
                        if (patch.originRank <= originMaxRank)
                            origin++;
                    }
                }
                apr_task2_fw.write(project + "_" + id + "\t" + origin +
                        "\t" + exceed + "\t" + correctSize + "\t" + incorrectSize + "\t");
                NumberFormat nf = NumberFormat.getPercentInstance();
                nf.setMinimumFractionDigits(2);
                if (incorrectSize == 0)
                    apr_task2_fw.write("100%=>100%\n");
                else {
                    double oorigin = origin / (double) correctSize;
                    double nnew = exceed / (double) correctSize;
                    apr_task2_fw.write(nf.format(oorigin));
                    apr_task2_fw.write("=>");
                    apr_task2_fw.write(nf.format(nnew) + "\n");
                }

                if (incorrectSize != 0) {
                    for (Prapr plausible : plausibles) {
                        if (plausible.isCorrect)
                            continue;
                        if (plausible.originRank <= originRankSum / correctSize)
                            origin_task3++;
                        if (plausible.newRank <= newRankSum / correctSize)
                            exceed_task3++;
                    }
                    apr_task3_fw.write(project + "_" + id + "\t" + origin_task3 +
                            "\t" + exceed_task3 + "\t" + correctSize + "\t" + incorrectSize + "\t");
                    double oorigin_task3 = origin_task3 / (double) incorrectSize;
                    double nnew_task3 = exceed_task3 / (double) incorrectSize;

                    apr_task3_fw.write(nf.format(oorigin_task3) + "=>" + nf.format(nnew_task3) + "\n");
                }

                Comparator<Prapr> comparator_before = (o1, o2) -> Double.compare(o2.originSuspicious, o1.originSuspicious);
                plausibles.sort(comparator_before);
                int originRankAmongPlausible = 0;
                apr_task4_fw.write(config.getProjectName()+"\t"+config.getCurrent()+"\t"+plausibles.size()+"\t");
                for (Prapr plausible : plausibles) {
                    originRankAmongPlausible++;
                    if (plausible.isCorrect) {
                        System.out.println("rank before " + originRankAmongPlausible);
                        apr_task4_fw.write(originRankAmongPlausible+"\t");
                        break;
                    }
                }

                Comparator<Prapr> comparator_after = (o1, o2) -> Double.compare(o2.newSuspicious, o1.newSuspicious);
                plausibles.sort(comparator_after);
                int newRankAmongPlausible = 0;
                for (Prapr plausible : plausibles) {
                    newRankAmongPlausible++;
                    if (plausible.isCorrect) {
                        System.out.println("rank after " + newRankAmongPlausible);
                        apr_task4_fw.write(newRankAmongPlausible+"\n");
                        break;
                    }
                }
            }
        }
        apr_task1_fw.write("valid:" + valid + "/" + total + "\t" + "nouse:" + nouse + "/" + total + "\n\n");
        apr_task1_fw.close();
        apr_task2_fw.close();
        apr_task3_fw.close();
        apr_task4_fw.close();
    }


    public static void resolvePatch(List<Prapr> patches, Configuration config) {

        Comparator<Prapr> comparator = (o1, o2) -> Double.compare(o2.originSuspicious, o1.originSuspicious);
        patches.sort(comparator);
        for (int i = 0; i < patches.size(); i++) {
//            if (i > 0 && patches.get(i).suspicious == patches.get(i - 1).suspicious) {
//                patches.get(i).originRank = patches.get(i - 1).originRank;
//            } else {
            patches.get(i).originRank = i + 1;
//            }
        }

//        for (VariableInfo var : varList) {
//            String id = var.className.replace("/", ".") + "#" + var.getLine();
//            if (tmp.get(id) != null) {
//                tmp.get(id).add(var.suspicious);
//            } else {
//                List<Double> li = new LinkedList<>();
//                li.add(var.suspicious);
//                tmp.put(id, li);
//            }
//        }
        Set<String> classSet = new HashSet<>();
        for (Prapr patch : patches) {
            classSet.add(patch.id.substring(0, patch.id.indexOf('#')));
        }
        Map<String, List<Double>> tmp = new HashMap<>();

        Map<String, Double> varSusMap = readStatisticalResult(config);
        Map<Integer, BBMapping[]> bbMappings = Instrument.readInstrMapping(config);
        for (Map.Entry<Integer, BBMapping[]> entry : bbMappings.entrySet()) {
            int hash = entry.getKey();
            BBMapping[] BBs = entry.getValue();
            if (BBs.length == 0 || BBs[0].vars.isEmpty())
                continue;
            String className = BBs[0].vars.get(0).className;
            if (className.contains("$"))
                className = className.substring(0, className.indexOf("$"));
            if (!classSet.contains(className))
                continue;
            for (BBMapping bb : BBs) {
                for (VariableInfo var : bb.vars) {
                    int line = var.getLine();
                    if (line == -1)
                        continue;
                    String name = var.name.contains("#") ? var.trueName : var.name;
                    String idForSus = hash + "#" + name;
                    if (!varSusMap.containsKey(idForSus))
                        continue;
                    double sus = varSusMap.get(idForSus);
                    String idForPatch = className + "#" + line;
                    List<Double> li = tmp.get(idForPatch);
                    if (li != null) {
                        li.add(sus);
                    } else {
                        List<Double> newL = new LinkedList<>();
                        newL.add(sus);
                        tmp.put(idForPatch, newL);
                    }

                }
            }
        }

        for (Prapr patch : patches) {
            if (tmp.get(patch.id) != null) {
                List<Double> li = tmp.get(patch.id);
                double avg = 0;
                for (double d : li) {
                    avg += d;
                }
                avg /= li.size();
//                patch.newSuspicious = patch.originSuspicious + avg * 0.2;
                patch.newSuspicious = patch.originSuspicious + avg;
            }
        }

        Comparator<Prapr> comparator_new = (o1, o2) -> Double.compare(o2.newSuspicious, o1.newSuspicious);
        patches.sort(comparator_new);
        for (int i = 0; i < patches.size(); i++) {
//            if (i > 0 && patches.get(i).suspicious == patches.get(i - 1).suspicious) {
//                patches.get(i).newRank = patches.get(i - 1).newRank;
//            } else {
            patches.get(i).newRank = i + 1;
//            }
        }
    }


    public static Map<String, String> reviseLineNumber(String bugid) {
        Map<String, String> map = new HashMap<>();
        switch (bugid) {
            case "Chart_1":
                map.put("org.jfree.chart.rendere.category.AbstractCategoryItemRenderer#1797", "org.jfree.chart.rendere.category.AbstractCategoryItemRenderer#1796");
                break;
            case "Lang_59":
                map.put("org.apache.commons.lang.text.StrBuilder#880", "org.apache.commons.lang.text.StrBuilder#879");
                map.put("org.apache.commons.lang.text.StrBuilder#884", "org.apache.commons.lang.text.StrBuilder#882");
                break;
            case "Math_5":
                map.put("org.apache.commons.math3.complex.Complex#305", "org.apache.commons.math3.complex.Complex#300");
                map.put("org.apache.commons.math3.complex.Complex#345", "org.apache.commons.math3.complex.Complex#344");
                break;
            case "Math_50":
                map.put("org.apache.commons.math.analysis.solvers.BaseSecantSolver#187", "org.apache.commons.math.analysis.solvers.BaseSecantSolver#131");
                map.put("org.apache.commons.math.analysis.solvers.BaseSecantSolver#188", "org.apache.commons.math.analysis.solvers.BaseSecantSolver#131");
                map.put("org.apache.commons.math.analysis.solvers.BaseSecantSolver#189", "org.apache.commons.math.analysis.solvers.BaseSecantSolver#131");

        }
        return map;
    }

    public static List<Prapr> readXML(String path) {
        List<Prapr> patchs = new LinkedList<>();
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(new File(path));
            Element element = document.getRootElement();
            Iterator it = element.elementIterator();
            while (it.hasNext()) {
                Element ele = (Element) it.next();
                Iterator itt = ele.elementIterator();
                double suspicious = -1;
                String description = null;
//                String mutator = null;
                String linenumber = null;
                String className = null;
                while (itt.hasNext()) {
                    Element eleChild = (Element) itt.next();
                    if (eleChild.getName().equals("mutatedClass")) {
                        className = eleChild.getStringValue();
                        if (className.contains("$"))
                            className = className.substring(0, className.indexOf("$"));
                    }
                    if (eleChild.getName().equals("lineNumber"))
                        linenumber = eleChild.getStringValue();
//                    if (eleChild.getName().equals("mutator"))
//                        mutator = eleChild.getStringValue();
                    if (eleChild.getName().equals("description"))
                        description = eleChild.getStringValue();
                    if (eleChild.getName().equals("suspValue"))
                        suspicious = Double.parseDouble(eleChild.getStringValue());
                }
                int hash = myHashCode(className + linenumber + description);
                String id = className + "#" + linenumber;
                patchs.add(new Prapr(-1, -1, hash, suspicious, id));
            }
        } catch (DocumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return patchs;
    }

    public static List<Prapr> readFixReport(String path) {
        List<Prapr> plausible = new LinkedList<>();
        try {
            String line;
            boolean isCorrect = false;
            File fix = new File(path);
            InputStreamReader ir = new InputStreamReader(Files.newInputStream(fix.toPath()));
            BufferedReader bf = new BufferedReader(ir);
            while ((line = bf.readLine()) != null) {
                if (line.startsWith("*"))
                    isCorrect = true;
                if (line.startsWith("\tMutator")) {
//                    String mutator = line.substring(line.indexOf("Mutator") + 9);
                    line = bf.readLine();
                    String description = line.substring(line.indexOf("description") + 15);
                    line = bf.readLine();
                    String className = line.substring(line.indexOf("File Name") + 11).replace("/", ".");
                    if (className.contains("google"))
                        className = className.substring(0, className.indexOf(".java", 20));
                    else className = className.substring(0, className.indexOf(".java"));
                    line = bf.readLine();
                    String linenumber = line.substring(line.indexOf("Line Number") + 13);
                    int hash = myHashCode(className + linenumber + description);
                    String id = className + "#" + linenumber;
                    Prapr prapr = new Prapr(-1, -1, hash, id, isCorrect);
                    plausible.add(prapr);
//                    if (isCorrect)
//                        correct.add(prapr);
//                    else plausible.add(prapr);
                    isCorrect = false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return plausible;
    }
}


class Prapr {
    int originRank;
    int newRank;
    int mutantsHash;
    double originSuspicious;
    double newSuspicious;
    String id; //class#line
    boolean isCorrect;


    public Prapr(int mutantsHash, double originSuspicious, String id) {
        this.mutantsHash = mutantsHash;
        this.originSuspicious = originSuspicious;
        this.id = id;
    }

    public Prapr(int originRank, int newRank, int mutantsHash, double originSuspicious, String id) {
        this.originRank = originRank;
        this.newRank = newRank;
        this.mutantsHash = mutantsHash;
        this.originSuspicious = originSuspicious;
        this.id = id;

    }

    public Prapr(int originRank, int newRank, int mutantsHash, String id, boolean isCorrect) {
        this.originRank = originRank;
        this.newRank = newRank;
        this.mutantsHash = mutantsHash;
        this.id = id;
        this.isCorrect = isCorrect;
    }

    public Prapr(int mutantsHash, String id) {
        this.mutantsHash = mutantsHash;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prapr prapr = (Prapr) o;
        return mutantsHash == prapr.mutantsHash;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutantsHash);
    }
}