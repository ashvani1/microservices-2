package com.elite.springboot.orderservice.service;

import com.elite.springboot.orderservice.entity.Order;
import com.elite.springboot.orderservice.external.client.PaymentService;
import com.elite.springboot.orderservice.external.client.ProductService;
import com.elite.springboot.orderservice.external.request.PaymentRequest;
import com.elite.springboot.orderservice.model.OrderRequest;
import com.elite.springboot.orderservice.repository.IOrderRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import java.time.Instant;

@Service
@Log4j2
public class OrderService implements IOrderService{

    @Autowired
    private ProductService productService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    IOrderRepository orderRepository;
    @Override
    public long placeOrder(OrderRequest orderRequest) {

        //Order entity: we can take the order and save it with status created.
        //call product service: to block the product [reduce quantity of the product in DB]
        //call payment service to do payment. once payment is successful we can mark order as successful. else mark as pending/cancel

        //to save order we have to convert orderRequest into order entity.

        log.info("placing order request with input :: {}", orderRequest);
        //call product service
        productService.reduceQuantity(orderRequest.getProductId(), orderRequest.getQuantity());

        log.info("creating order with status created");
        Order order = Order.builder()
                .amount(orderRequest.getTotalAmount())
                .quantity(orderRequest.getQuantity())
                .orderDate(Instant.now())
                .orderStatus("CREATED")
                .productId(orderRequest.getProductId())
                .build();
        log.info("built order object {} ", order);
        Order orderCreated = orderRepository.save(order);
        log.info("calling payment service to complete to payment");
        PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getId())
                .paymentMode(orderRequest.getPaymentMode())
                .amount(orderRequest.getTotalAmount())
                .build();
       String orderStatus = null;

       try{
           paymentService.doPayment(paymentRequest);
           log.info("Payment is done successfully. Changing the order status to placed");
           orderStatus = "PLACED";
       }

       catch (Exception e) {
           log.error("Error occurred in payment. Changing order status to failed");
           orderStatus = "PAYMENT_FAILED";
       }

       order.setOrderStatus(orderStatus);
       orderRepository.save(order);
        return orderCreated.getId();
    }

}
