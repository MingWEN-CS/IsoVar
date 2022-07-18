package IsoVar;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import static IsoVar.MainClass.isLinux;

public class Configuration {
    public static final String instrMapping = "InstrMapping/";
    public static final String oracleRootPath = "Oracle/";
    public static final String mutantsRootPath = "Mutants/";
    public static final String ReportRoot = "report/";
    public final String metricRoot = "metric/";
    //    public final String similaritiesRoot = "Similarities/";
    public static final String instrMappingRoot = "InstrMapping/";
    public final String suspiciousProject = "SuspiciousProject/";
    public static final String IsoVarRoot = System.getProperty("user.dir");
    public Set<Integer> deprecated = new HashSet<>();
    public String targetPath;
    public String testPath;
    public int bugNumber;
    public String instrPath;
    String projectPrefix;
    private String projectName;
    private int timeout;
    private boolean debug;
    private int current;
    private boolean skip_mutation;
    private String project_root;
    private String binary_dir;
    private String test_binary_dir;
    private String report_dir;
    private String dependency;
    private String phase;
    private double alpha = 0.4;
    private double beta = 0.9;
    private double gamma = 1;
    private int max_mutation;


    public int getMax_mutation() {
        return max_mutation;
    }

    public void setMax_mutation(int max_mutation) {
        this.max_mutation = max_mutation;
    }

    public String getReport_dir() {
        return report_dir;
    }

    public void setReport_dir(String report_dir) {
        this.report_dir = report_dir;
    }

    public String getDependency() {
        return dependency;
    }

    public void setDependency(String dependency) {
        try {
            if (!getPhase().equals("analyze")) {
                this.dependency = dependency;
                return;
            }
            File file = new File(dependency);
            if (!file.exists())
                throw new RuntimeException("dependency file is not exist.");
            Path path = file.toPath();
            BasicFileAttributes basicFileAttributes = Files.readAttributes(path, BasicFileAttributes.class);
            if (basicFileAttributes.isRegularFile()) {
                InputStreamReader inputReader = new InputStreamReader(Files.newInputStream(path));
                BufferedReader bf = new BufferedReader(inputReader);
                String line;
                while ((line = bf.readLine()) != null) {
                    if (isLinux())
                        line = line.replace(";", ":");
                    else
                        line = line.replace(":", ";");
                    this.dependency = System.getProperty("user.dir") + "/junit/*;" + line;
                }
                bf.close();
                inputReader.close();
            } else if (basicFileAttributes.isDirectory())
                this.dependency = System.getProperty("user.dir") + "/junit/*;" + dependency + "/*";
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getBinary_dir() {
        return binary_dir;
    }

    public void setBinary_dir(String binary_dir) {
        this.binary_dir = binary_dir;
    }

    public String getTest_binary_dir() {
        return test_binary_dir;
    }

    public void setTest_binary_dir(String test_binary_dir) {
        this.test_binary_dir = test_binary_dir;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public boolean isSkip_mutation() {
        return skip_mutation;
    }

    public void setSkip_mutation(boolean skip_mutation) {
        this.skip_mutation = skip_mutation;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public void reviseProjectPath(int id) {
        if (projectName.equals("Time") && id >= 12) {
            targetPath = "build/classes/";
            instrPath = "build/instr_classes/";
            testPath = "build/tests/";
        } else if (projectName.equals("Mockito") && (id >= 12 & id <= 17)) {
            targetPath = "target/classes/";
            testPath = "target/test-classes/";
            instrPath = "target/instr_classes/";
        } else if (projectName.equals("Mockito") && (id >= 18 & id <= 21)) {
            targetPath = "build/classes/main/";
            testPath = "build/classes/test/";
            instrPath = "build/instr_classes/main/";
        } else if (projectName.equals("Mockito") && (id >= 22 & id <= 38)) {
            testPath = "target/test-classes/";
            targetPath = "target/classes/";
            instrPath = "target/instr_classes/";
        } else if (projectName.equals("Lang") && id >= 21 && id <= 41) {
            testPath = "target/test-classes/";
        } else if (projectName.equals("Bears") && id >= 27 && id <= 83) {
            projectPrefix = "spoon";
        } else if (projectName.equals("Bears") && id >= 84 && id <= 97) {
            projectPrefix = "org";
        } else if (projectName.equals("Bears") && id >= 98 && id <= 139) {
            projectPrefix = "org";
        }
    }

    public int getCurrent() {
        return current;
    }

    public boolean setCurrent(int current) {
        this.current = current;
        if (deprecated.contains(current)) {
            System.err.println("the bug " + projectName + "_" + current + " is deprecated");
            return false;
        }
        reviseProjectPath(current);
        return true;
    }

    public String getBaselineRankingFile(String name) {
        if (projectName.equals("Bears"))
            return "bears/" + name + "_" + current + ".txt";
        else {
            if (projectName.equals("Mockito"))
                return "E:/data/HSFL/" + projectName + "/" + projectName + "_" + current + "/" + name + ".txt";
            else
                return "E:/data/HSFL/" + projectName + "/" + projectName + "_" + current + "/SBFL/" + name + ".txt";
        }

    }


    public String getProjectRoot() {
        return project_root;
    }

    public void setProjectRoot(String projectRoot) {
        File file = new File(projectRoot);
        if (getPhase().equals("parameters_tuning"))
            return;
        if (!file.exists())
            throw new RuntimeException("the target project " + projectRoot + " is no exist");
        project_root = projectRoot;
    }

    public String getFixedProjectTargetPath() {
        if (!projectName.equals("Bears"))
            return "/data/MutationAnalysis/target_classes/" +
//            return "E:/target_classes/" +
                    getProjectName() + "/" +
                    getProjectName() + "_" + getCurrent() + "_fixed/" + targetPath;
        else
            return "/data/luokaixuan/bears_fixed/" +
                    getProjectName() + "-" + getCurrent() +
                    "/" + targetPath;
    }

    public String getProjectTestPath() {
        return getProjectRoot() + "/" + testPath;
    }

    public String getProjectInstrPath() {
        return getProjectRoot() + "/" + instrPath;
    }

    public String getProjectTargetPath() {
        return getProjectRoot() + "/" + targetPath;
    }


    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
        deprecated.clear();
        readNoWork();
        switch (projectName) {
            case "Time":
                targetPath = "target/classes/";
                instrPath = "target/instr_classes/";
                testPath = "target/test-classes/";
//                deprecated.add(21);
                bugNumber = 27;
                projectPrefix = "org.joda";
                break;
            case "Chart":
                instrPath = "instr_build/";
                targetPath = "build/";
                testPath = "build-tests/";
                projectPrefix = "org.jfree";
                bugNumber = 26;
                break;
            case "Closure":
                targetPath = "build/classes/";
                instrPath = "build/instr_classes/";
                testPath = "build/test/";
                projectPrefix = "com.google";
                bugNumber = 133;
//                deprecated.add(63);
//                deprecated.add(93);
                break;
            case "Mockito":
                targetPath = "build/classes/main/";
                instrPath = "build/instr_classes/main/";
                testPath = "build/classes/test/";
                projectPrefix = "org";
                bugNumber = 38;
                break;
            case "Math":
                targetPath = "target/classes/";
                instrPath = "target/instr_classes/";
                testPath = "target/test-classes/";
                projectPrefix = "org.apache";
                bugNumber = 106;
                break;
            case "Lang":
                targetPath = "target/classes/";
                instrPath = "target/instr_classes/";
                testPath = "target/tests/";
                projectPrefix = "org.apache";
//                deprecated.add(2);
                bugNumber = 65;
                break;
            case "Bears":
                targetPath = "target/classes/";
                instrPath = "target/instr_classes/";
                testPath = "target/test-classes/";
                projectPrefix = "com.fasterxml";
                bugNumber = 139;
        }
    }

    private void readNoWork() {
        File file = new File("NoWork.txt");
        try {
            InputStreamReader inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("\t")[0].split("_");
                if (projectName.equals(lineSplit[0]))
                    deprecated.add(Integer.parseInt(lineSplit[1]));
            }
            bf.close();
            inputReader.close();
        } catch (IOException ignore) {
        }

    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getPatchFile() {
        if (!projectName.equals("Bears"))
//            return "Oracle/25.src.patch";
            return "/home/xiezifan/tools/defects4j/framework/projects/"
                    + projectName + "/patches/" + current + ".src.patch";
        else
            return "/data/luokaixuan/bears_fixed/" + projectName + "-" + current + ".patch";
//
    }

    public String getProjectPrefix() {
        return projectPrefix;
    }

    public void setProjectPrefix(String projectPrefix) {
        this.projectPrefix = projectPrefix;
    }
}
