package org.demo.reactive_ecommerce;

import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.List;

public class ReactiveTutorial {

    public Flux<Integer> getNumbers() {
        System.out.println("Creating Flux");
        return Flux.just(1, 2, 3);
    }

    public List<Integer> getNumbersWithList(){
        return Arrays.asList(1,2,3);
    }

    public Flux<Integer> getNumbersWithDefer(){
        return Flux.defer(() -> {
            System.out.println("Creating Flux");
            return Flux.just(1, 2, 3);
        });
    }

    public static void main(String[] args) {
        ReactiveTutorial tutorial = new ReactiveTutorial();
        Flux<Integer> numbers = tutorial.getNumbers();

        System.out.println("Waiting...");

        numbers.subscribe(System.out::println);
        numbers.subscribe(System.out::println);

        System.out.println(tutorial.getNumbersWithList());
    }
}
