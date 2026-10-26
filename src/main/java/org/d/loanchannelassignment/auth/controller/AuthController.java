package org.d.loanchannelassignment.auth.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.d.loanchannelassignment.auth.dto.LoginDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthController {

    @GetMapping("/test")
    String getTest() {

        return "Test API!!";
    }

//    @PostMapping("/login")
//    ResponseEntity<LoginDTO.Response> postLogin() {
//        log.info("LoginController");

//        return new ResponseEntity<LoginDTO.Response>(HttpStatus.OK, );
//    }
}
