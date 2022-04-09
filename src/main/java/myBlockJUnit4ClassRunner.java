import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.concurrent.TimeUnit;

public class myBlockJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    public myBlockJUnit4ClassRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Statement withPotentialTimeout(FrameworkMethod method, Object test, Statement next) {
        return FailOnTimeout.builder()
                .withTimeout(15, TimeUnit.SECONDS)
                .build(next);
    }

    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        super.runChild(method, notifier);
    }

    @Override
    public void run(RunNotifier notifier) {
        super.run(notifier);
    }
}
