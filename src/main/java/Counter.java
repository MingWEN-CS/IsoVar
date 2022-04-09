import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Counter{
    /**
     * the name if test suite
     */
//    private static final String testSuiteName;
    public static final Set<Integer> methodAccess = new HashSet<>();
    /**
     * record the accessed basic block, key is the unique signature
     * for method, value is a int[], when access to index, int[index]
     * will be ++.
     * it can be used both for origin and mutants, when it used for
     * mutants, OriginBasicBlockAccess will be used
     */
    public static Map<Integer, Integer[]> basicBlockAccess = new HashMap<>();
    public static Integer[] arr;
    public static int h;

//    public Counter(Class<?> testClass) throws InitializationError {
//        super(testClass);
//    }
//
//    @Override
//    protected Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
//        return FailOnTimeout.builder()
//                .withTimeout(9, TimeUnit.SECONDS)
//                .build(next);
//    }


    public static void main(String[] args) throws ClassNotFoundException {
//        String className = "org.joda.time.TestPartial_Constructors";
//        String methodName = "testConstructorEx7_TypeArray_intArray";

        String className = System.getProperty("className");
        String methodName = System.getProperty("methodName");
        JUnitCore core = new JUnitCore();
        core.addListener(new MyRunListener());
        if (methodName != null) {
            core.run(Request.method(Class.forName(className), methodName));
        } else {
            core.run(Request.aClass(Class.forName(className)));
//            try {
//                Class<?> clazz = Class.forName(className);
//                // add timeout for each test method, in case of an infinite loop
//                core.run(Request.runner(new myBlockJUnit4ClassRunner(clazz)));
//            } catch (InitializationError e) {
//                e.printStackTrace();
//            }
        }
    }

//    static {
//        if (System.getProperties().containsKey("isWrite")) {
//            isWrite = Boolean.parseBoolean(System.getProperty("isWrite"));
//            output = System.getProperty("output");
//            if (isWrite && output == null)
//                System.err.println("pathToFailingTest or testSuiteName may not be set");
////        hook addShutdownHook，like junit's @AfterClass
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                if (isWrite) {
//                    Counter.generateFailingReport(System.getProperty("className")
//                            + "#" + System.getProperty("methodName"));
//                } else {
////                    System.out.println("***start**");
////                    for (Map.Entry<Integer, Integer[]> entry : basicBlockAccess.entrySet()) {
////                        System.out.print(entry.getKey() + "\t");
////                        Integer[] arr = entry.getValue();
////                        for (int i = 0; i < arr.length; i++) {
////                            if (i != arr.length - 1)
////                                System.out.print(arr[i] + "\t");
////                            else System.out.println(arr[i]);
////                        }
////                    }
////                    System.out.println("***end**");
//                }
//            }));
//        } else if (System.getProperties().containsKey("failingMethod")) {
//            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                for (Integer access : methodAccess) {
//                    System.out.println(access);
//                }
////                System.out.println("***end***");
//            }));
//        }
//    }

    // record basic block access
//    public static void r(int hash, int total, int index) {
//        if (basicBlockAccess.get(hash) == null) {
//            Integer[] array = new Integer[total];
//            Arrays.fill(array,0);
//            array[index]=1;
//            basicBlockAccess.put(hash, array);
//        } else {
//            basicBlockAccess.get(hash)[index]++;
//        }
//    }
    public static void r(int hash, int total, int index) {
        if (h == hash)
            arr[index]++;
        else {
            Integer[] in = basicBlockAccess.get(hash);
            if (in == null) {
                Integer[] array = new Integer[total];
                Arrays.fill(array, 0);
                array[index] = 1;
                arr = array;
                h = hash;
                basicBlockAccess.put(hash, array);
            } else {
                in[index]++;
                arr = in;
                h = hash;
            }
        }
    }

    public static void m(int hash) {
        methodAccess.add(hash);
    }

    /**
     * @return void
     * @description: 生成原始项目的Failing test报告，路径为test_report/Time/Time_1/类名#函数名.txt
     * 格式为类名
     * @date: 17:31
     */
    public static void generateFailingReport(String testSuiteName) {
        String filename = String.format("%s.txt", testSuiteName);
        File file = new File(filename);
        File fileParent = file.getParentFile();
        if (!fileParent.exists())
            fileParent.mkdirs();
        try {
            FileWriter fileWriter = new FileWriter(file);

            for (Map.Entry<Integer, Integer[]> entry : basicBlockAccess.entrySet()) {
                fileWriter.write(entry.getKey() + "\t");
                Integer[] arr = entry.getValue();
                for (int i = 0; i < arr.length; i++) {
                    if (i != arr.length - 1)
                        fileWriter.write(arr[i] + "\t");
                    else fileWriter.write(arr[i] + "\n");
                }
//            fileWriter.write("\n");
            }
//        fileWriter.write("ennnnnnnnnnnnnd");
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("successfully generate report at " + filename);
    }


}
