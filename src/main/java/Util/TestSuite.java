package Util;

import static Instrument.Instrument.myHashCode;

public class TestSuite {

    public String className;
    public String methodName;
    public boolean isFailing = false;
    public double simBetweenTests = 0;

    public TestSuite(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public TestSuite(String className, String methodName, boolean isFailing) {
        this.className = className;
        this.methodName = methodName;
        this.isFailing = isFailing;
    }

    public TestSuite(TestSuite suite) {
        this.className = suite.className;
        this.methodName = suite.methodName;
    }

    public int compareTo(TestSuite o) {
        return Double.compare(this.simBetweenTests, o.simBetweenTests);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        TestSuite other = (TestSuite) obj;
        return className.equals(other.className) && methodName.equals(other.methodName);
    }

    @Override
    public int hashCode() {
        return myHashCode(className) +
                myHashCode(methodName);
    }
}

