package edu.berkeley.cs.jqf.fuzz.util;

import edu.berkeley.cs.jqf.fuzz.ei.state.AbstractExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.ei.state.FastExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.ei.state.JanalaExecutionIndexingState;

import edu.berkeley.cs.jqf.fuzz.ei.state.TypedJanalaEiState;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CoverageFactory {

    public static final String propFile = System.getProperty("janala.conf", "janala.conf") !=null? System.getProperty("janala.conf", "janala.conf") :
        System.getProperty("janala-analysis.conf", "janala-analysis.conf");

    private static boolean FAST_NON_COLLIDING_COVERAGE_ENABLED;
    private static boolean SPOTON_RUNNING;
    static
    {
        Properties properties = new Properties();
        try (InputStream propStream = new FileInputStream(propFile)) {
            properties.load(propStream);
        } catch (IOException e) {
            // Swallow exception and continue with defaults
            // System.err.println("Warning: No janala.conf file found");
        }
        properties.putAll(System.getProperties());
        FAST_NON_COLLIDING_COVERAGE_ENABLED = Boolean.parseBoolean(properties.getProperty("useFastNonCollidingCoverageInstrumentation", "false"));
        SPOTON_RUNNING = properties.getProperty("engine")!=null? (properties.getProperty("engine").equals("spotOn")): false;
    }

    public static ICoverage newInstance() {
        if (FAST_NON_COLLIDING_COVERAGE_ENABLED) {
            return new FastNonCollidingCoverage();
        } if(SPOTON_RUNNING){
            return new TypedEiCoverage();}
        else {
            return new Coverage();
        }
    }

    public static AbstractExecutionIndexingState newEIState() {
        if (FAST_NON_COLLIDING_COVERAGE_ENABLED) {
            return new FastExecutionIndexingState();
        }if(SPOTON_RUNNING)
            return new TypedJanalaEiState();
        else {
            return new JanalaExecutionIndexingState();
        }
    }
}
