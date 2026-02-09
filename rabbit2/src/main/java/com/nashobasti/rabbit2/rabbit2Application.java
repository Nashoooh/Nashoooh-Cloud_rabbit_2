package com.nashobasti.rabbit2;

import com.nashobasti.rabbit2.service.RouteScheduleService;
import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@SpringBootApplication
public class rabbit2Application {

    public static void main(String[] args) {
        SpringApplication.run(rabbit2Application.class, args);
    }
}

@Component
class RabbitMQConsumerComponent {
    private final static String QUEUE_NAME = "cola_actualizaciones"; // Cola para actualizaciones de rutas
    
    @Autowired
    private RouteScheduleService routeScheduleService;

    @EventListener(ApplicationReadyEvent.class)
    public void startRabbitMQConsumer() {
        try {
            // Establecer conexión y canal a RabbitMQ
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();

            // Declarar la cola de la cual consumir con la propiedad durable
            boolean durable = true;

            // Declarar la cola de la cual consumir
            channel.queueDeclare(QUEUE_NAME, durable, false, false, null);
            System.out.println(" [*] Esperando mensajes de actualizaciones de rutas. Para salir presione CTRL+C");

            // Definir la función de callback para el consumidor
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                System.out.println(" [x] Recibido: '" + message + "'");
                
                // Procesar el mensaje y generar archivo JSON
                routeScheduleService.processScheduleMessage(message);
            };
            
            // Consumir mensajes de la cola
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
            
        } catch (IOException | TimeoutException e) {
            System.err.println("Error conectando a RabbitMQ: " + e.getMessage());
            System.err.println("Asegúrate de que RabbitMQ esté ejecutándose en localhost:5672");
        }
    }
}


