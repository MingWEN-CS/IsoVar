package Util;

import IsoVar.Configuration;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import static IsoVar.MainClass.isLinux;

public class RunCommand {
    public List<TestSuite> failing = new ArrayList<>();
    public List<TestSuite> passing = new ArrayList<>();
    public Set<String> passingClazz = new HashSet<>();
    Configuration config;

    public static short timeoutHit = 0;

    public RunCommand(Configuration config) {
        this.config = config;
    }

    public void readTestSuites() {
        if (config.getProjectName().equals("Bears")) {
            readBearsTestSuites();
            return;
        }

        String failingPath, passingPath;
        failingPath = config.getProjectRoot() + "/trigger";
        try {
            File file = new File(failingPath);
            InputStreamReader inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            while ((line = bf.readLine()) != null) {
                String[] spilt = line.split("::");
                failing.add(new TestSuite(spilt[0], spilt[1], true));
            }
            bf.close();
            inputReader.close();

            passingPath = config.getProjectRoot() + "/alltest";
            file = new File(passingPath);
            inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            bf = new BufferedReader(inputReader);
            while ((line = bf.readLine()) != null) {
                passingClazz.add(line);
            }
            bf.close();
            inputReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void readBearsTestSuites() {
        String failingPath, passingPath;
        failingPath = "/Bears-" + config.getCurrent() + "_failing.txt";
        File file = new File(failingPath);
        InputStreamReader inputReader;
        try {
            inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            BufferedReader bf = new BufferedReader(inputReader);
            String line;
            String[] lineSplit;
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("::");
                failing.add(new TestSuite(lineSplit[0], lineSplit[1], true));
                passingClazz.add(lineSplit[0]);
            }
            bf.close();
            inputReader.close();

            passingPath = "/Bears-" + config.getCurrent() + "_passing.txt";
            file = new File(passingPath);
            inputReader = new InputStreamReader(Files.newInputStream(file.toPath()));
            bf = new BufferedReader(inputReader);
            while ((line = bf.readLine()) != null) {
                lineSplit = line.split("::");
                passingClazz.add(lineSplit[0]);
            }
            bf.close();
            inputReader.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private String generateCmd(Configuration config, String classNames, String mutant) {
//        if (config.getProjectName().equals("Closure")) {
//            dependence = projectRoot + "/build/lib/*";
//            dependence += ";" + projectRoot + "/lib/*";
//        } else {
//            if (!config.getProjectName().equals("Bears"))
//                dependence = "`cat " + projectRoot + "/dep.txt`";
//            else
//                dependence = "`cat " + config.getRootPath() + "/Bears-" + config.getCurrent() + "_dep.txt`";
//        }
//        if (!isLinux())
//            dependence = config.dependencies;
        String toTestClazz = "-DclassName=\"" + classNames + "\"";
        String ProcessFailingTest = "Counter";
        String cmd = "java " +
                "-cp \"";
        if (mutant != null)
            cmd += mutant + ";";
        cmd += config.getProjectInstrPath() + ";";
        if (!isLinux())
            cmd += config.getProjectTargetPath() + ";" +
                    config.getProjectTestPath() + ";";
        cmd += config.getDependency() + "\" " +
                toTestClazz + " " +
                ProcessFailingTest;
        if (isLinux())
            cmd = cmd.replaceAll(";", ":");
//        System.out.println(Arrays.toString(cmd));
        return cmd;
    }

    public String runCommands_tomem(Map<TestSuite, Map<Integer, Integer[]>> failingBBAccess,
                                    Map<TestSuite, Map<Integer, Integer[]>> passingBBAccess,
                                    String mutant,
                                    Set<String> suites) {
//        int numOfCores = Runtime.getRuntime().availableProcessors();
        ExecutorService es = Executors.newCachedThreadPool();
        String cmd = null;
        Set<String> usedSuites;
        if (suites == null)
            usedSuites = passingClazz;
        else
            usedSuites = suites;

        for (String className : usedSuites) {
            cmd = generateCmd(config, className, mutant);
//            int timeout =
            int timeout = config.getTimeout();
            if (mutant == null && isFailing(className)) { // statistical phas
                timeout = 60;
            }
            if(isLinux())
                cmd = "timeout " + (timeout + 1) + "s " + cmd;
            if (config.isDebug())
                System.out.println(cmd);
            Future<Map<TestSuite, Map<Integer, Integer[]>>> future = es.submit(new MyCallable_tomem_analyze(cmd, config));
            try {
                Map<TestSuite, Map<Integer, Integer[]>> clazzAccess = future.get(timeout, TimeUnit.SECONDS);
                if (clazzAccess.containsKey(null) || clazzAccess.isEmpty()) {
//                    cmd = cmd.replace("java -cp", "java -noverify -cp");
                    future = es.submit(new MyCallable_tomem_analyze(cmd, config));
                    clazzAccess = future.get();
                }
                if (clazzAccess.containsKey(null) || clazzAccess.isEmpty()) {
                    continue;
                }

                for (Map.Entry<TestSuite, Map<Integer, Integer[]>> enrty : clazzAccess.entrySet()) {
                    TestSuite key = enrty.getKey();
                    Map<Integer, Integer[]> access = enrty.getValue();
                    if (failing.contains(key))
                        failingBBAccess.put(key, access);
                    else {
                        if (mutant == null)
                            passing.add(key);
                        passingBBAccess.put(key, access);
                    }
                }
            } catch (InterruptedException | ExecutionException ignore) {
                future.cancel(true);
            } catch (TimeoutException t) {
                for (TestSuite testSuite : failing) {
                    if (testSuite.className.equals(className))
                        timeoutHit++;
                }
                if (timeoutHit > 15)
                    config.setSkip_mutation(true);
            }
        }
        es.shutdownNow();
        return cmd;
    }

    private boolean isFailing(String className) {
        for (TestSuite testSuite : failing) {
            if (testSuite.className.equals(className))
                return true;
        }
        return false;
    }

//    public Set<Integer> runCommandsFailingAccess(ProjectInfo project, Configuration config) {
//        Set<Integer> res = new HashSet<>();
//        ExecutorService es = Executors.newCachedThreadPool();
//        for (TestSuite suite : failing) {
//            String cmd = generateCmd(project, config, suite, true, false, true);
//            System.out.println(cmd);
//            Future<Set<Integer>> future = es.submit(new MyCallable_tomem_FailingAccess(cmd));
//            try {
//                res.addAll(future.get());
//            } catch (InterruptedException | ExecutionException e) {
//                e.printStackTrace();
//            }
//        }
//        es.shutdownNow();
//        return res;
//    }

}


class MyCallable_tofile implements Callable<String> {
    String cmd;

    public MyCallable_tofile(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public String call() throws Exception {
        Process p;
        if (isLinux())
            p = new ProcessBuilder().command("bash", "-c", cmd).start();
        else p = Runtime.getRuntime().exec(cmd);
        BufferedInputStream in = new BufferedInputStream(p.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line = br.readLine();
//        while ((line = br.readLine()) != null) {
//            System.out.println(line);
//        }
        in.close();
        br.close();
        return line;
    }
}

class MyCallable_tomem_analyze implements Callable<Map<TestSuite, Map<Integer, Integer[]>>> {
    String cmd;
    Configuration config;

    public MyCallable_tomem_analyze(String cmd, Configuration config) {
        this.cmd = cmd;
        this.config = config;
    }

    @Override
    public Map<TestSuite, Map<Integer, Integer[]>> call() throws Exception {
        Map<TestSuite, Map<Integer, Integer[]>> map = new HashMap<>();
        Process p;
        if (isLinux())
            p = new ProcessBuilder().directory(new File(config.getProjectRoot())).command("bash", "-c", cmd).start();
        else p = Runtime.getRuntime().exec(cmd);
        BufferedInputStream bi = new BufferedInputStream(p.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(bi));
        TestSuite currentSuite = null;
        Map<Integer, Integer[]> currentAccess = null;
        String line;
        while ((line = br.readLine()) != null) {
            if (line.length() == 0)
                continue;
//            if (line.equals("**end**"))
//                break;
            String[] frame = line.split("\t");
            if (line.contains("::")) {
                if (currentSuite != null)
                    map.put(currentSuite, currentAccess);
                String[] split = line.split("::");
                currentSuite = new TestSuite(split[0], split[1]);
                currentAccess = new HashMap<>();
                continue;
            }
            if (!Pattern.compile("^[-]?[\\d]*$").matcher(frame[0]).matches())
                continue;
            Integer[] in = new Integer[frame.length - 1];
            for (int i = 1; i < frame.length; i++)
                in[i - 1] = Integer.parseInt(frame[i]);
            currentAccess.put(Integer.parseInt(frame[0]), in);
        }
        map.put(currentSuite, currentAccess);
        bi.close();
        br.close();
        return map;
    }
}

class MyCallable_tomem_FailingAccess implements Callable<Set<Integer>> {
    String cmd;

    public MyCallable_tomem_FailingAccess(String cmd) {
        this.cmd = cmd;
    }

    @Override
    public Set<Integer> call() throws Exception {
        Process p;
        Set<Integer> set = new HashSet<>();
        if (isLinux())
            p = new ProcessBuilder().command("bash", "-c", cmd).start();
        else p = Runtime.getRuntime().exec(cmd);
        BufferedInputStream bi = new BufferedInputStream(p.getInputStream());
        BufferedReader br = new BufferedReader(new InputStreamReader(bi));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.length() == 0)
                continue;
            if (!Pattern.compile("^[-]?[\\d]*$").matcher(line).matches())
                continue;
            set.add(Integer.parseInt(line));
        }
        bi.close();
        br.close();
        return set;
    }
}