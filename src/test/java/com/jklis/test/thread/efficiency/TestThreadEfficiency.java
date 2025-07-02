package com.jklis.test.thread.efficiency;

import com.jklis.test.thread.efficiency.record.RequestResult;
import com.jklis.test.thread.efficiency.utilities.Utilities;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

@State(Scope.Benchmark)
public class TestThreadEfficiency {

    @Param({"0", "1", "2"})
    public int numberOfThreadsOption;

    public int numberOfThreads;

    public final static int ATTEMPTS_NUMBER = Utilities.getAttempts();
    public final static String URL = Utilities.getURL();

    private static HttpClient client;

    @Setup
    public void setup() throws Exception {
        numberOfThreads = Utilities.getThreadOption(numberOfThreadsOption);
        client = HttpClient
                .newBuilder()
                .build();
    }

    @Benchmark
    @OutputTimeUnit(TimeUnit.SECONDS)
    @BenchmarkMode(Mode.AverageTime)
    @Fork(value = 1, warmups = 1)
    @Warmup(iterations = 1, time = 3, timeUnit = TimeUnit.SECONDS)
    @Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
    public void b(Blackhole blackhole) throws Exception {
        var result = testEfficiency(URL, numberOfThreads, ATTEMPTS_NUMBER);
        blackhole.consume(result);
    }

    private static RequestResult performGetRequest(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(url))
                    .GET()
                    .build();
            HttpResponse<String> response;
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();
            return new RequestResult(statusCode >= 200 && statusCode < 300,
                    response.statusCode(), response.body());
        } catch (Exception ex) {
            return new RequestResult(false,
                    String.format("Exception %s, Message:%s",
                            ex.getClass().getName(), ex.getMessage()));
        }
    }

    private static List<RequestResult> testEfficiency(String url, Integer threadNumber, Integer attempts)
            throws Exception {
        final ExecutorService executor
                = Executors.newFixedThreadPool(threadNumber);
        List<Future<RequestResult>> futures = IntStream.range(0, attempts)
                .mapToObj(i -> functionToCallable(test -> performGetRequest(test), url))
                .map(executor::submit)
                .collect(Collectors.toList());
        executor.shutdown();
        List<RequestResult> results = new ArrayList();
        for (var future : futures) {
            RequestResult result = future.get();
            if (!result.isValid()) {
                throw new Exception(String.format("Request returned with status code %s and body %s", 
                        result.statusCode(), result.body()));
            }
            results.add(future.get());
        }
        return results;
    }

    public static <T, R> Callable<R> functionToCallable(Function<T, R> f, T v) {
        return () -> f.apply(v);
    }

}
