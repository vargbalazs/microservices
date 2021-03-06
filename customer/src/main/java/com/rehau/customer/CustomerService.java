package com.rehau.customer;

import com.rehau.amqp.RabbitMQMessageProducer;
import com.rehau.clients.fraud.FraudCheckResponse;
import com.rehau.clients.fraud.FraudClient;
import com.rehau.clients.notification.NotificationClient;
import com.rehau.clients.notification.NotificationRequest;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@AllArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final FraudClient fraudClient;
//    private final NotificationClient notificationClient;
    private final RabbitMQMessageProducer rabbitMQMessageProducer;

    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .build();

        customerRepository.saveAndFlush(customer);

//        FraudCheckResponse fraudCheckResponse = restTemplate.getForObject(
//                "http://FRAUD/api/v1/fraud-check/{customerId}",
//                FraudCheckResponse.class, customer.getId()
//        );

        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

        if (fraudCheckResponse.isFraudster()) throw new IllegalStateException("fraudster");

        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("hi %s, welcome to rehau...", customer.getFirstName())
        );

//        notificationClient.sendNotification(
//                notificationRequest
//        );

        rabbitMQMessageProducer.publish(notificationRequest, "internal.exchange",
                "internal.notification.routing-key");
    }
}
