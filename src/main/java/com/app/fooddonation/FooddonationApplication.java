package com.app.fooddonation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FooddonationApplication {
	public static void main(String[] args) {
		SpringApplication.run(FooddonationApplication.class, args);
		System.out.println("🍽️ Food Donation Platform Started Successfully!");
		System.out.println("🔗 Access at: http://localhost:8080");
	}
}