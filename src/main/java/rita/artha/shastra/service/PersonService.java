package rita.artha.shastra.service;

import rita.artha.shastra.entity.Person;
import rita.artha.shastra.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }
}

