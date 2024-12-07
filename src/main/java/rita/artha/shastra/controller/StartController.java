package rita.artha.shastra.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StartController {

    @GetMapping
    public String welcome(){
        return "Welcome Team";
    }
}
