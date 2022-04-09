package IsoVar;

import Instrument.BBMapping;
import Instrument.Instrument;
import Mutator.MutateController;
import Similarity.Statistical;
import Util.EvaluationUtil;
import Util.Pair;
import Util.RunCommand;
import Util.TestSuite;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class MainClass {

    private static String OPTION_PROJECT_NAME = "project_name";
    private static String OPTION_PHASE = "phase";
    private static String OPTION_ID = "ID";
    private static String OPTION_TIMEOUT = "timeout";
    private static String OPTION_DEBUG = "debug";
    private static String OPTION_SKIP_MUTATION = "skip_mutation";
    private static String OPTION_PATH_TO_PROJECT = "project_root";
    private static String OPTION_PATH_TO_BINARY = "binary";
    private static String OPTION_PATH_TO_TEST_BINARY = "test_binary";
    private static String OPTION_PATH_TO_DEPENDENCY = "dependency";
    private static String OPTION_PATH_TO_REPORT = "report_dir";
    private static String OPTION_MAX_MUTATION = "max_mutation";
    private static String OPTION_ALPHA = "alpha";
    private static String OPTION_BETA = "beta";
    private static String OPTION_GAMMA = "gamma";


    protected final Options options = new Options();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    protected CommandLine cmd = null;

    public static void main(String[] args) {
        MainClass main = new MainClass();
        main.run(args);
    }

    public static boolean isLinux() {
        return System.getProperty("os.name").toLowerCase().contains("linux");
    }

    protected void run(String[] args) {
        initializeCommandLineOptions();
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(110);

        // We need proper parameters
        final HelpFormatter formatter = new HelpFormatter();

        // Parse the command-line parameters
        try {
            CommandLineParser parser = new PosixParser();
            try {
                cmd = parser.parse(options, args);
                cmd.getArgs();
            } catch (ParseException ex) {
                ex.printStackTrace();
            }

            // Do we need to display the user manual?
            if (cmd.hasOption("?") || cmd.hasOption("help")) {
                formatter.printHelp("java -jar IsoVar.jar [OPTIONS]", options);
                return;
            }

            Configuration config = new Configuration();
            parseCommandOptions(cmd, config);
//            ProjectInfo project = new ProjectInfo(config.getProjectName());
            RunCommand cmd = new RunCommand(config);

            switch (config.getPhase()) {
                case "instrument":
                    Instrument instr = new Instrument(config);
                    instr.doInstr();

//                        instr.runDefects4jCompileAndTest(config);
                    break;

                case "analyze":
                    logger.info(String.format("perform statistical and mutation phase on project %s", config.getProjectRoot()));
                    cmd.readTestSuites();
                    // for statistical to file
                    Map<TestSuite, Map<Integer, Integer[]>> failingBBAccess, passingBBAccess;
                    Map<Integer, BBMapping[]> bbMappings = Instrument.readInstrMapping(config);
//                        // write
//                        cmd.runCommands_tofile(project, config);
//                        failingBBAccess = Instrument.readFailingBBAccess(config, cmd);
//                        passingBBAccess = Instrument.readPassingBBAccess(config, cmd);

                    //mem
                    long start = System.currentTimeMillis();
                    failingBBAccess = new HashMap<>();
                    passingBBAccess = new HashMap<>();
                    cmd.runCommands_tomem(failingBBAccess, passingBBAccess, null, null);
                    long end = System.currentTimeMillis();
                    int statistical = (int) ((end - start) / 1000);
                    logger.info(String.format("time cost for statistical phase is %d", statistical));

                    Statistical stat = new Statistical(bbMappings, failingBBAccess, config);
                    stat.doCalculate(failingBBAccess, passingBBAccess);
                    if (config.isSkip_mutation()) {
                        stat.writeStatisticalResult(config, bbMappings);
                        return;
                    }
//                        // for mutate
                    Map<VariableTrimInfo, Set<VariableInfo>> varsToBeMutate =
                            stat.prepareVarsToBeMutated(bbMappings, failingBBAccess);
                    Map<Integer, Set<String>> suites =
                            stat.shrinkPassingForMutatePhase(failingBBAccess, passingBBAccess,
                                    varsToBeMutate.keySet(), cmd);
                    MutateController controller = new MutateController(config, cmd);
                    controller.doMutate(suites, varsToBeMutate, failingBBAccess, passingBBAccess);
                    long end2 = System.currentTimeMillis();
                    int mutate = (int) ((end2 - end) / 1000);
                    logger.info(String.format("time cost for mutation phase is %d", mutate));
                    stat.writeStatisticalResult(config, bbMappings);
                    System.exit(0);
                    break;
                case "oracle":
                    PatchSummary patch = new PatchSummary(config);
                    patch.writeOracles();
                    break;
                case "evaluate":
                    EvaluationUtil.main(new String[]{""});
            }

        } catch (AbortAnalysisException e) {
            // Silently return
        } catch (Exception e) {
            System.err.printf("The analysis has failed. Error message: %s\n", e.getMessage());
            e.printStackTrace();
        }
    }

    private void initializeCommandLineOptions() {
        options.addOption("?", "help", false, "print this help message");
        options.addOption(OPTION_PHASE, true, "analyze phase, [instrument] or [analyze]");
        options.addOption(OPTION_PROJECT_NAME, true, "project name");
        options.addOption(OPTION_ID, true, "bug id");
        options.addOption(OPTION_TIMEOUT, true, "timeout for one test classes. default is 20s.");
        options.addOption(OPTION_DEBUG, true, "enable debug? default is false.");
        options.addOption(OPTION_PATH_TO_PROJECT, true, "target directory of project path");
        options.addOption(OPTION_PATH_TO_BINARY, true, "target directory of classes (relative to working directory)");
        options.addOption(OPTION_PATH_TO_TEST_BINARY, true, "Target directory of test classes (relative to working directory)");
        options.addOption(OPTION_PATH_TO_DEPENDENCY, true, "path to all dependency, eg, path_to_dependency/ that contain all .jar or" +
                " a file containing all dependency path.");
        options.addOption(OPTION_PATH_TO_REPORT, true, "report dir path. default is report/projectName/projectName_ID.txt");
        options.addOption(OPTION_MAX_MUTATION, true, "max mutation time for one variables. default is 10.");
        options.addOption(OPTION_ALPHA, true, "value of alpha, default is 0.4");
        options.addOption(OPTION_BETA, true, "value of beta, default is 0.9");
        options.addOption(OPTION_ALPHA, true, "value of gamma, default is 0.1");
        options.addOption(OPTION_SKIP_MUTATION, true, "skip muation phase? default is false");
    }

    protected void parseCommandOptions(CommandLine cmd, Configuration config) {
//        String root = cmd.getOptionValue(OPTION_ROOT_PATH);
//        if (root != null) {
//            File targetFile = new File(root);
//            if (!targetFile.isDirectory()) {
//                System.err.printf("Target path %s does not exist%n", targetFile.getCanonicalPath());
//                return;
//            }
//            config.setRootPath(root);
//        } else {
//            if (isLinux())
//                config.setRootPath("/data/MutationAnalysis/target_classes_comment_new/");
//            else config.setRootPath("E:/target_classes/");
//        }

        String project_root = cmd.getOptionValue(OPTION_PATH_TO_PROJECT);
        if (project_root != null) {
            config.setProjectRoot(project_root);
        } else config.setProjectRoot("null");

        String binary_dir = cmd.getOptionValue(OPTION_PATH_TO_BINARY);
        if (binary_dir != null) {
            config.setBinary_dir(binary_dir);
        }

        String test_binary_dir = cmd.getOptionValue(OPTION_PATH_TO_TEST_BINARY);
        if (test_binary_dir != null) {
            config.setTest_binary_dir(test_binary_dir);
        }

        String alpha = cmd.getOptionValue(OPTION_ALPHA);
        if (alpha != null) {
            config.setAlpha(Double.parseDouble(alpha));
        } else config.setAlpha(0.5);

        String beta = cmd.getOptionValue(OPTION_BETA);
        if (beta != null) {
            config.setBeta(Double.parseDouble(beta));
        } else config.setBeta(0.9);

        String gamma = cmd.getOptionValue(OPTION_GAMMA);
        if (gamma != null) {
            config.setGamma(Double.parseDouble(gamma));
        } else config.setGamma(0.1);

        String phase = cmd.getOptionValue(OPTION_PHASE);
        if (phase != null) {
            config.setPhase(phase);
        } else config.setPhase("instrument");

        String projectName = cmd.getOptionValue(OPTION_PROJECT_NAME);
        if (projectName != null) {
            config.setProjectName(projectName);
        } else config.setProjectName("Time");

        String id = cmd.getOptionValue(OPTION_ID);
        if (id != null) {
            int s = Integer.parseInt(id);
            config.setCurrent(s);
        } else config.setCurrent(1);

        String reportDir = cmd.getOptionValue(OPTION_PATH_TO_REPORT);
        if (reportDir != null) {
            config.setReport_dir(projectName);
        } else config.setReport_dir("report/");

        String timeout = cmd.getOptionValue(OPTION_TIMEOUT);
        if (timeout != null) {
            int s = Integer.parseInt(timeout);
            config.setTimeout(s);
        } else config.setTimeout(20);

        String debug = cmd.getOptionValue(OPTION_DEBUG);
        if (debug != null) {
            boolean s = Boolean.parseBoolean(debug);
            config.setDebug(s);
        } else config.setDebug(false);

        String skip_mutation = cmd.getOptionValue(OPTION_SKIP_MUTATION);
        if (skip_mutation != null) {
            boolean s = Boolean.parseBoolean(skip_mutation);
            config.setSkip_mutation(s);
        } else config.setSkip_mutation(false);

        String dependency = cmd.getOptionValue(OPTION_PATH_TO_DEPENDENCY);
        if (dependency != null) {
            config.setDependency(dependency);
        } else config.setDependency("junit/");

        String max_mutation = cmd.getOptionValue(OPTION_MAX_MUTATION);
        if (max_mutation != null) {
            int t = Integer.parseInt(max_mutation);
            config.setMax_mutation(t);
        } else config.setMax_mutation(10);
    }
}
