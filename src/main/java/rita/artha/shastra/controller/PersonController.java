package rita.artha.shastra.controller;

import rita.artha.shastra.entity.Person;
import rita.artha.shastra.service.PersonService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/persons")
@RequiredArgsConstructor
@Tag(name = "Person API", description = "Endpoints for managing persons")
public class PersonController {
    private final PersonService personService;

    @GetMapping
    public List<Person> getAllPersons() {
        return personService.getAllPersons();
    }
}
