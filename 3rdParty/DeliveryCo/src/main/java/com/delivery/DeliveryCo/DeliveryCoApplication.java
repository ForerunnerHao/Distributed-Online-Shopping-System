package com.delivery.DeliveryCo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeliveryCoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DeliveryCoApplication.class, args);
	}

}
// **DeliveryCo**
//   1. Notifies Store if request has been received (placed stt in Store)
//   2. Notifies Store if order has been picked up from the warehouse (preparing stt in Store)
//   3. Notifies Store if order is on the way to the warehouse (delivering stt in Store)
//   4. Notifies Store if order is delivered (delivered stt in Store)
//   5. Has a failed probability (5% or whatever):
//     * The DeliveryCo also might lose some of the packages at each stage of the delivery \\ process. 
// The package loss rate can be set around 5% and adjusted based on your demonstration plan.
//   
