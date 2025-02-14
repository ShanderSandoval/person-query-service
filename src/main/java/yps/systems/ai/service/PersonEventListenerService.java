package yps.systems.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import yps.systems.ai.model.Person;
import yps.systems.ai.repository.IPersonDocumentRepository;

import java.util.Optional;

@Service
public class PersonEventListenerService {

    private final IPersonDocumentRepository personRepository;

    @Autowired
    public PersonEventListenerService(IPersonDocumentRepository personRepository) {
        this.personRepository = personRepository;
    }
    @KafkaListener(topics = "${env.kafka.topicEvent}")
    public void listen(@Payload String payload, @Header("eventType") String eventType, @Header("source") String source) {
        System.out.println("Processing " + eventType + " event from " + source);
        switch (eventType) {
            case "CREATE_PERSON":
                try {
                    Person person = new ObjectMapper().readValue(payload, Person.class);
                    personRepository.save(person);
                } catch (JsonProcessingException e) {
                    System.err.println("Error parsing Person JSON: " + e.getMessage());
                }
                break;
            case "UPDATE_PERSON":
                try {
                    Person person = new ObjectMapper().readValue(payload, Person.class);
                    Optional<Person> personOptional = personRepository.findById(person.getId());
                    personOptional.ifPresent(existingPerson -> personRepository.save(person));
                } catch (JsonProcessingException e) {
                    System.err.println("Error parsing Person JSON: " + e.getMessage());
                }
                break;
            case "DELETE_PERSON":
                Optional<Person> personOptional = personRepository.findById(payload.replaceAll("\"", ""));
                personOptional.ifPresent(value -> personRepository.deleteById(value.getId()));
                break;
            default:
                System.out.println("Unknown event type: " + eventType);
        }
    }


}
