/*
 * Copyright (c) 2017-2018 The Regents of the University of California
 * Copyright (c) 2020-2021 Rohan Padhye
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.fuzz.junit.quickcheck;

import edu.umn.cs.spoton.GuidanceConfig;
import java.io.EOFException;
import java.io.InputStream;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.ParameterTypeContext;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.instrument.InstrumentationException;
import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import ru.vyarus.java.generics.resolver.GenericsResolver;
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.*;

/**
 *
 * A JUnit {@link Statement} that will be run using guided fuzz
 * testing.
 *
 * @author Rohan Padhye
 */
public class FuzzStatement extends Statement {
    private final FrameworkMethod method;
    private final TestClass testClass;
    private final MethodGenericsContext generics;
    private final GeneratorRepository generatorRepository;
    private final List<Class<?>> expectedExceptions;
    private final List<Throwable> failures = new ArrayList<>();
    private final Guidance guidance;
    private boolean skipExceptionSwallow;

    public FuzzStatement(FrameworkMethod method, TestClass testClass,
                         GeneratorRepository generatorRepository, Guidance fuzzGuidance) {
        this.method = method;
        this.testClass = testClass;
        this.generics = GenericsResolver.resolve(testClass.getJavaClass())
                .method(method.getMethod());
        this.generatorRepository = generatorRepository;
        this.expectedExceptions = Arrays.asList(method.getMethod().getExceptionTypes());
        this.guidance = fuzzGuidance;
        this.skipExceptionSwallow = Boolean.getBoolean("jqf.failOnDeclaredExceptions");
    }

    /**
     * Run the test.
     *
     * @throws Throwable if the test fails
     */
    @Override
    public void evaluate() throws Throwable {
        double mutationTime = 0.0;
        double genTime = 0.0;
        double testTime = 0.0;
        double handleResultTime = 0.0;
        // Construct generators for each parameter
        List<Generator<?>> generators = Arrays.stream(method.getMethod().getParameters())
                .map(this::createParameterTypeContext)
                .map(generatorRepository::produceGenerator)
                .collect(Collectors.toList());

        // Keep fuzzing until no more input or I/O error with guidance
        try {

            // Keep fuzzing as long as guidance wants to
            while (guidance.hasInput()) {
                Result result = INVALID;
                Throwable error = null;

                // Initialize guided fuzzing using a file-backed random number source
                try {
                    Object[] args;
                    try {

                        // Generate input values
                        Instant beforeDate = Instant.now();
                        InputStream input;
                        try {
                            input = guidance.getInput();
                        } finally {
                            Instant afterDate = Instant.now();
                            mutationTime = Duration.between(beforeDate, afterDate).toNanos() /  1_000_000_000.0;
                        }

                        StreamBackedRandom randomFile = new StreamBackedRandom(input, Long.BYTES);
                        SourceOfRandomness random = new FastSourceOfRandomness(randomFile);
                        GenerationStatus genStatus = new NonTrackingGenerationStatus(random);
                        System.setProperty("GenerationBegin", "true");
                        beforeDate = Instant.now();
                        try{
                            args = generators.stream()
                                .map(g -> g.generate(random, genStatus))
                                .toArray();
                        }finally {
                            Instant afterDate = Instant.now();
                            genTime = Duration.between(beforeDate, afterDate).toNanos() /  1_000_000_000.0;
                        }
                        if(GuidanceConfig.getInstance().eiGenerationExperiment)
                            guidance.appendInputSizeStats();
                        // Let guidance observe the generated input args
                        guidance.observeGeneratedArgs(args);
                    } catch (IllegalStateException e) {
                        if (e.getCause() instanceof EOFException) {
                            // This happens when we reach EOF before reading all the random values.
                            // The only thing we can do is try again
                            System.out.println("This happens when we reach EOF before reading all the random values.");
                            continue;
                        } else {
//                            System.out.println("exception 1\n"+ Arrays.toString(e.getStackTrace()));
                            throw e;
                        }
                    } catch (AssumptionViolatedException | TimeoutException e) {
//                        System.out.println("exception 2\n"+ Arrays.toString(e.getStackTrace()));
                        // Propagate early termination of tests from generator
                        continue;
                    } catch (GuidanceException e) {
                        // Throw the guidance exception outside to stop fuzzing
//                        System.out.println("exception 3\n"+ Arrays.toString(e.getStackTrace()));
                        throw e;
                    } catch (Throwable e) {
                        // Throw the guidance exception outside to stop fuzzing
//                        System.out.println("exception 4\n"+ Arrays.toString(e.getStackTrace()));
                        throw new GuidanceException(e);
                    }

                    Instant beforeDate = Instant.now();
                    // Attempt to run the trial
                    try{
                        guidance.run(testClass, method, args);}
                    finally {
                        Instant afterDate = Instant.now();
                        testTime = Duration.between(beforeDate, afterDate).toNanos() /  1_000_000_000.0;
                        guidance.incrementTimeTookByChildrenForParent(mutationTime+genTime+testTime);
                    }
//                    guidance.appendGenDetails();
                    // If we reached here, then the trial must be a success
                    result = SUCCESS;
                } catch(InstrumentationException e) {
                    // Throw a guidance e xception outside to stop fuzzing
//                    System.out.println("exception 5\n"+ Arrays.toString(e.getStackTrace()));
                    throw new GuidanceException(e);
                } catch (GuidanceException e) {
                    // Throw the guidance exception outside to stop fuzzing
//                    System.out.println("exception 6\n"+ Arrays.toString(e.getStackTrace()));
                    throw e;
                } catch (AssumptionViolatedException e) {
//                    System.out.println("exception 7\n"+ Arrays.toString(e.getStackTrace()));
                    result = INVALID;
                    error = e;
                } catch (TimeoutException e) {
//                    System.out.println("exception 8\n"+ Arrays.toString(e.getStackTrace()));
                    result = TIMEOUT;
                    error = e;
                } catch (Throwable e) {
//                    System.out.println("exception 9\n"+ Arrays.toString(e.getStackTrace()));
                    // Check if this exception was expected
                    if (isExceptionExpected(e.getClass())) {
                        result = SUCCESS; // Swallow the error
                    } else {
                        result = FAILURE;
                        error = e;
                        failures.add(e);
                    }
                }

                // Inform guidance about the outcome of this trial
                try {
                    Instant beforeDate = Instant.now();
                    try {
                        guidance.handleResult(result, error);
                    }finally {
                        Instant afterDate = Instant.now();
                        handleResultTime = Duration.between(beforeDate, afterDate).toNanos() /  1_000_000_000.0;
                        guidance.appendGenFuzzStats(mutationTime, genTime, testTime, handleResultTime);
                    }
                } catch (GuidanceException e) {
//                    System.out.println("exception 10\n"+ Arrays.toString(e.getStackTrace()));
                    throw e; // Propagate
                } catch (Throwable e) {
//                    System.out.println("exception 11\n"+ Arrays.toString(e.getStackTrace()));
                    // Anything else thrown from handleResult is an internal error, so wrap
                    throw new GuidanceException(e);
                }


            }
        } catch (GuidanceException e) {
//            System.out.println("exception 12\n"+ Arrays.toString(e.getStackTrace()));
            throw e;
        }

        if (failures.size() > 0) {
            if (failures.size() == 1) {
                System.out.println("one failure encountered");
                throw failures.get(0);
            } else {
                // Not sure if we should report each failing run,
                // as there may be duplicates
                System.out.println("multiple failures encountered");
                throw new MultipleFailureException(failures);
            }
        }

    }

    /**
     * Returns whether an exception is expected to be thrown by a trial method
     *
     * @param e the class of an exception that is thrown
     * @return <code>true</code> if e is a subclass of any exception specified
     * in the <code>throws</code> clause of the trial method.
     */
    private boolean isExceptionExpected(Class<? extends Throwable> e) {
        if (skipExceptionSwallow) {
            return false;
        }
        for (Class<?> expectedException : expectedExceptions) {
            if (expectedException.isAssignableFrom(e)) {
                return true;
            }
        }
        return false;
    }

    private ParameterTypeContext createParameterTypeContext(Parameter parameter) {
        return ParameterTypeContext.forParameter(parameter, generics).annotate(parameter);
    }
}
