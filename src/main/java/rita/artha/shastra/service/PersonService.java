package rita.artha.shastra.service;

import rita.artha.shastra.entity.Person;
import rita.artha.shastra.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PersonService {
    private final PersonRepository personRepository;

    public List<Person> getAllPersons() {
        return personRepository.findAll();
    }

    public Optional<Person> getPersonById(Integer id) {
        return personRepository.findById(id);
    }

    public Person savePerson(Person person) {
        return personRepository.save(person);
    }

    public void deletePerson(Integer id) {
        personRepository.deleteById(id);
    }

    @Transactional
    public Person upsertPerson(String keycloakId, String fullName, String email,
                                String mobileNumber, String company) {
        Person person = personRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> Person.builder()
                        .keycloakId(keycloakId)
                        .createdAt(LocalDateTime.now())
                        .build());
        if (fullName     != null && !fullName.isBlank())     person.setFullName(fullName);
        if (email        != null && !email.isBlank())        person.setEmail(email);
        if (mobileNumber != null && !mobileNumber.isBlank()) person.setMobileNumber(mobileNumber);
        if (company      != null && !company.isBlank())      person.setCompany(company);
        person.setUpdatedAt(LocalDateTime.now());
        return personRepository.save(person);
    }
}

