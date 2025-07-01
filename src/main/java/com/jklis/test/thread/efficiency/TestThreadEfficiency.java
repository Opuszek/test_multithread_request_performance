package com.jklis.test.thread.efficiency;

import com.jklis.test.thread.efficiency.record.RequestResult;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import static java.util.function.Predicate.not;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TestThreadEfficiency {

    private static final HttpClient client = HttpClient
            .newBuilder()
            .build();
    private static final Logger LOGGER = Logger.getLogger(TestThreadEfficiency.class.getName());

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        String url = args[0];
        List<Integer> testThreads = Arrays.stream(args[1].split(","))
                .map(Integer::valueOf)
                .collect(Collectors.toList());
        int attemptsPerInstance = Integer.valueOf(args[2]);
        for (var threads : testThreads) {
            testEfficiency(url, threads, attemptsPerInstance);
        }

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

    private static void testEfficiency(String url, Integer threadNumber, Integer attempts) throws InterruptedException, ExecutionException {
        long start = System.currentTimeMillis();
        final ExecutorService executor
                = Executors.newFixedThreadPool(threadNumber);
        List<Future<RequestResult>> futures = IntStream.range(0, attempts)
                .mapToObj(i -> functionToCallable(test -> performGetRequest(test), url))
                .map(executor::submit)
                .collect(Collectors.toList());
        executor.shutdown();
        List<RequestResult> results = new ArrayList();
        for (var future : futures) {
            results.add(future.get());
        }
        long end = System.currentTimeMillis();
        String performanceDetails = String.format("Attempt with %d threads finished in %f seconds",
                threadNumber, (double)(end - start)/1000);
        String validityCheck = String.format("It returned %d valid and %d invalid results",
                results.stream().filter(RequestResult::isValid).count(),
                results.stream().filter(not(RequestResult::isValid)).count());
        LOGGER.log(Level.INFO, performanceDetails.concat(System.lineSeparator()).concat(validityCheck));
    }

    public static <T, R> Callable<R> functionToCallable(Function<T, R> f, T v) {
        return () -> f.apply(v);
    }

//    private static void logDetailedResults(Collection<RequestResult> results) {
//        Map<Boolean, List<RequestResult>> byValidity = results.stream().collect(
//                Collectors.partitioningBy(RequestResult::isValid));
//
//        LOGGER.log(Level.INFO, String.format("Attempt returned %d valid responses:",
//                byValidity.get(Boolean.TRUE).size()));
//        LOGGER.log(Level.INFO, prepareLogForResults(
//                byValidity.get(Boolean.TRUE)));
//
//        LOGGER.log(Level.INFO, String.format("And %d invalid responses:",
//                byValidity.get(Boolean.FALSE).size()));
//        LOGGER.log(Level.INFO, prepareLogForResults(
//                byValidity.get(Boolean.FALSE)));
//    }

//    private static String prepareLogForResults(Collection<RequestResult> results) {
//        return results.stream().collect(Collectors.toMap(RequestResult::body, v -> 1L, Long::sum))
//                .entrySet().stream().map(
//                        e -> String.format("Response %s occured %d times",
//                                e.getKey(), e.getValue())
//                ).collect(Collectors.joining("," + System.lineSeparator()));
//    }
    
}
