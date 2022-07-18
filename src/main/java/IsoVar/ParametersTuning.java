package IsoVar;

import Util.EvaluationMetric;
import Util.Pair;
import fj.P;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import static Util.EvaluationUtil.getOracleRanks;
import static Util.EvaluationUtil.readVarSuspicious;

public class ParametersTuning {

    Configuration config;
    List<Project> projects;

    public ParametersTuning(Configuration config) {
        this.config = config;

        scanReports();
        double alpha = findOptimalParameter(-1, 0.5, 0.5);
        System.out.println("find optimal alpha " + String.format("%.3f", alpha));
//        double gamma = findOptimalParameter(alpha, 0.5, -1);
//        System.out.println("find optimal gamma " + String.format("%.3f", gamma));
//        double beta = findOptimalParameter(alpha, -1, gamma);
//        System.out.println("find optimal beta " + String.format("%.3f", beta));


        double beta = findOptimalParameter(alpha, -1, 0.5);
        System.out.println("find optimal beta " + String.format("%.3f", beta));
        double gamma = findOptimalParameter(alpha, beta, -1);
        System.out.println("find optimal gamma " + String.format("%.3f", gamma));

//        System.out.println(alpha + " " + beta + " " + gamma);

    }

    double findOptimalParameter(double alpha, double beta, double gamma) {
        double targetParam;

        List<List<Integer>> ranks = new ArrayList<>();
        double optMetric = -1;
        double optParam = -1;
        for (targetParam = -1; targetParam <= 1; targetParam += 0.01) { // find optimal alpha
            for (Project project : projects) {
                for (Pair<VariableTrimInfo, Double> pair : project.indexes) {
                    VariableTrimInfo varT = pair.getKey();
                    double stat = varT.failingFreq / (varT.failingFreq + varT.passingFreq) - varT.cosSim * judgeAlpha(alpha, targetParam);
                    double mutate = varT.failingDiff - varT.passingDiff * judgeBeta(beta, targetParam);
                    double suspicious;
                    if (mutate == 0)
                        suspicious = stat;
                    else
                        suspicious = stat - mutate * judgeGamma(gamma, targetParam);
                    pair.modifyValue(suspicious);
                }
                List<Integer> rank = getOracleRanks(project.indexes, project.oracles);
                ranks.add(rank);
            }
            double MRR = EvaluationMetric.MRR(ranks);
            double MAP = EvaluationMetric.MAP(ranks);
            ranks.clear();
            double metric =  MAP + MRR;
            if (metric > optMetric) {
                optMetric = metric;
                optParam = targetParam;
            }
        }
        if (gamma < 0)
            System.out.println("max metric = " + optMetric);
        return optParam;
    }

    double judgeAlpha(double alpha, double target) {
        return alpha < 0 ? target : alpha;
    }

    double judgeBeta(double beta, double target) {
        return beta < 0 ? target : beta;
    }

    double judgeGamma(double gamma, double target) {
        return gamma < 0 ? target : gamma;
    }


    public void printResult() {

    }

    public void scanReports() {
        projects = new ArrayList<>();
        File reportDir = new File(config.getReport_dir());
        if (reportDir.isDirectory()) {
            for (File file : Objects.requireNonNull(reportDir.listFiles())) {
                if (file.isDirectory()) {
                    for (File project : Objects.requireNonNull(file.listFiles())) {
                        String name = project.getName();
                        if (name.endsWith(".txt")) {
                            String[] split = name.split("_");
                            int id = Integer.parseInt(split[1].substring(0, split[1].indexOf('.')));
                            config.setProjectName(split[0]);
                            config.setCurrent(id);
                            List<Pair<VariableTrimInfo, Double>> indexes = readVarSuspicious(config); // read IsoVar's results
                            HashSet<VariableTrimInfo> oracles = PatchSummary.readOracles(config, false); // remapping for ochiai
                            projects.add(new Project(split[0], id, indexes, oracles));
                        }
                    }
                }
            }
        }
    }

}

class Project {
    String projectName;
    int id;
    List<Pair<VariableTrimInfo, Double>> indexes;
    HashSet<VariableTrimInfo> oracles;

    public Project(String projectName, int id, List<Pair<VariableTrimInfo, Double>> indexes, HashSet<VariableTrimInfo> oracles) {
        this.projectName = projectName;
        this.id = id;
        this.indexes = indexes;
        this.oracles = oracles;
    }
}