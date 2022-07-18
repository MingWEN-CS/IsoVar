package Util;

import Instrument.BBMapping;
import Instrument.Instrument;
import IsoVar.Configuration;
import com.github.difflib.DiffUtils;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Chunk;
import com.github.difflib.patch.Patch;
import fj.P;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static Similarity.Statistical.readStatisticalResult;
import static Util.FileToLines.fileToLines;

public class BoostSimFix {
    private static String SimFixResultsRoot = "SimFix/patch/";

    public static void main(String[] args) throws IOException {

        File simFix = new File("SimFix/IsoVar_results.txt");
        simFix.getParentFile().mkdirs();
        FileWriter apr_task4_fw = new FileWriter(simFix);
        apr_task4_fw.write("project\tid\t\ttotal plausibles\ttotal correct\trank_before\trank_after\n");
        File dir = new File(SimFixResultsRoot);
        if (!dir.exists()) {
            System.out.printf("SimFix results dir " + SimFixResultsRoot + " is not valid");
            System.exit(0);
        }
        Configuration config = new Configuration();

        for (File project : Objects.requireNonNull(dir.listFiles())) {
            if (project.getName().endsWith("txt"))
                continue;
            String projectName = project.getName().substring(0, 1).toUpperCase() + project.getName().substring(1);
            config.setProjectName(projectName);
            for (File id : Objects.requireNonNull(project.listFiles())) {
                int ID = Integer.parseInt(id.getName());
                if (!config.setCurrent(ID))
                    continue;
                System.out.println("Start***" + project + " " + ID + "******");

                List<SimFix> plausibles = readSimFixResults(id.getAbsolutePath() + "/0/", config);
                int totalCorrect = 0, originRank = -1, newRank = -1;
                for (SimFix plausible : plausibles) {
                    if (plausible.isCorrect) {
                        originRank = plausible.originRank;
                        break;
                    }
                }

//                Comparator<SimFix> comparator = (o1, o2) -> Double.compare(o2.suspicious, o1.suspicious);

                Comparator<SimFix> cmp = (o1, o2) -> {
                    int cmp1 = Double.compare(o2.suspicious, o1.suspicious);
                    if (cmp1 == 0)
                        return Integer.compare(o1.originRank, o2.originRank);
                    else return cmp1;
                };
                plausibles.sort(cmp);
                for (int i = 0; i < plausibles.size(); i++) {
                    if (plausibles.get(i).isCorrect && newRank == -1)
                        newRank = i + 1;
                    plausibles.get(i).newRank = i + 1;
                }
                apr_task4_fw.write(config.getProjectName() + "\t" + config.getCurrent() + "\t" + plausibles.size()
                        + "\t1\t" + originRank + "\t" + newRank + "\n");
            }
        }
        apr_task4_fw.close();
    }

    private static List<SimFix> readSimFixResults(String path, Configuration config) {
        Map<String, Double> varSusMap = readStatisticalResult(config);
        Map<Integer, BBMapping[]> bbMappings = Instrument.readInstrMapping(config);
        File dir = new File(path);
        List<SimFix> plausibles = new ArrayList<>();
        if (!dir.isDirectory()) {
            System.out.printf("The path " + dir.getAbsolutePath() + " is not valid");
            System.exit(0);
        }
        for (File patchFile : Objects.requireNonNull(dir.listFiles())) {
            SimFix plausible = resolvePatch(patchFile, varSusMap, bbMappings);
            plausibles.add(plausible);
        }

        return plausibles;
    }

    private static SimFix resolvePatch(File patch, Map<String, Double> varSusMap,
                                       Map<Integer, BBMapping[]> bbMappings) {
        List<String> postPatch = new ArrayList<>();
        List<String> postPatchForDiff = new ArrayList<>();
        List<String> prePatch = new ArrayList<>();
        List<String> prePatchForDiff = new ArrayList<>();
        boolean pre = false, post = false;
        List<String> slices = fileToLines(patch.getAbsolutePath());
        String patchName = patch.getName();
//        List<String> vars = new ArrayList<>();
        Set<String> vars = new HashSet<>();
        int originRank = Integer.parseInt(patchName.split("_")[0]);
        StringBuilder className = new StringBuilder(patchName.substring(patchName.lastIndexOf('_') + 1, patchName.indexOf('.')));
        boolean isCorrect = patchName.contains("correct");
        int methodHash = -1;
        for (String line : slices) {
            if (line.startsWith("package")) {
                String Package = line.substring(line.indexOf(' ') + 1, line.indexOf(';'));
                className.insert(0, Package + ".");
            }

            if (line.contains("start of generated patch")) {
                post = true;
                continue;
            } else if (line.contains("end of generated patch")) {
                post = false;
                continue;
            } else if (line.contains("start of original code")) {
                pre = true;
                continue;
            } else if (line.contains("end of original code")) {
                pre = false;
                continue;
            }
            if (pre) {
                prePatch.add(line);
                prePatchForDiff.add(line.replaceAll("\\s+", "").trim());
            }
            if (post) {
                if (methodHash == -1)
                    methodHash = BoostTBar.resolveMethod(className.toString(), slices.indexOf(line), bbMappings);
                postPatch.add(line);
                postPatchForDiff.add(line.replaceAll("\\s+", "").trim());
            }
        }
        Patch<String> diff = DiffUtils.diff(prePatchForDiff, postPatchForDiff);
        List<AbstractDelta<String>> deltas = diff.getDeltas();
        for (AbstractDelta<String> delta : deltas) {
            switch (delta.getType()) {
                case INSERT:
                    //新增
                    Chunk<String> insert = delta.getTarget();
                    for (String line : insert.getLines()) {
                        int i = postPatchForDiff.indexOf(line);
                        vars.addAll(BoostTBar.handle(postPatch.get(i)));
//                        System.out.println("+ "+postPatchForDiff.get(i)+"\t\t"+postPatch.get(i));
                    }
                    break;
                case CHANGE:
                    //修改
                    Chunk<String> source = delta.getSource();
                    Chunk<String> target1 = delta.getTarget();
                    for (String line : source.getLines()) {
                        int i = prePatchForDiff.indexOf(line);
                        vars.addAll(BoostTBar.handle(prePatch.get(i)));
//                        System.out.println("- " + prePatchForDiff.get(i) + "\t\t" + prePatch.get(i));
                    }

                    for (String line : target1.getLines()) {
                        int i = postPatchForDiff.indexOf(line);
                        vars.addAll(BoostTBar.handle(postPatch.get(i)));
//                        System.out.println("+ " + postPatchForDiff.get(i) + "\t\t" + postPatch.get(i));
                    }
                    break;
                case DELETE:
                    //删除
                    Chunk<String> delete = delta.getSource();
                    for (String line : delete.getLines()) {
                        int i = prePatchForDiff.indexOf(line);
                        vars.addAll(BoostTBar.handle(prePatch.get(i)));
//                        System.out.println("- " + prePatchForDiff.get(i) + "\t\t" + prePatch.get(i));
                    }
//                    System.out.println("- " + (delete.getPosition() + 1) + " " + delete.getLines());
                    break;
                case EQUAL:
//                    System.out.println("无变化");
                    break;
            }
        }
        List<Double> sus = BoostTBar.resolvePatchSuspicious(methodHash, vars, varSusMap);
        return new SimFix(originRank, sus, vars, isCorrect);
    }
}

class SimFix {
    int originRank;
    double suspicious;
    boolean isCorrect;
    int newRank = -1;
    Set<String> vars;
    List<Double> sus;

    public SimFix(int originRank, List<Double> sus, Set<String> vars, boolean isCorrect) {
        this.originRank = originRank;
        this.vars = vars;
        double sum = 0;
        for (Double s : sus) {
            sum += s;
        }
        this.sus = sus;
        this.suspicious = sus.isEmpty() ? 0 : sum / sus.size();
        this.isCorrect = isCorrect;

    }
}
