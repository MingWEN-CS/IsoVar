//import org.junit.Rule;
//import org.junit.rules.Timeout;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;

import java.util.HashMap;
import java.util.Map;

public class MyRunListener extends RunListener {

//    @Rule
//    protected Timeout globalTimeout = Timeout.seconds(30);

//    public MyRunListener(Counter counter) {
//        this.counter = counter;
//    }

    @Override
    public void testStarted(Description description) {
        synchronized (this) {
            Counter.basicBlockAccess = new HashMap<>();
            Counter.h = 0;
            Counter.arr = null;
        }
        System.out.println(description.getClassName() + "::" + description.getMethodName());
    }

    @Override
    public void testFinished(Description description) {
        for (Map.Entry<Integer, Integer[]> entry : Counter.basicBlockAccess.entrySet()) {
            System.out.print(entry.getKey() + "\t");
            Integer[] arr = entry.getValue();
            for (int i = 0; i < arr.length; i++) {
                if (i != arr.length - 1)
                    System.out.print(arr[i] + "\t");
                else System.out.println(arr[i]);
            }
        }
    }
    @Override
    public void testRunFinished(Result result) throws Exception {
        System.exit(0); // for Bears >= 98
    }
}
