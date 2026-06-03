package group102.insurancefraud.controller;

import group102.insurancefraud.dto.request.PredictRequest;
import group102.insurancefraud.service.FraudDetectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

//@RestController
//public class TestController {
//    @GetMapping("/hello")
//    public String Hello(){
//        return "Hello my friend";
//    }
//}
//
//@RestController
//@RequestMapping("/fraud")
//public class FraudController {
//
//    @Autowired
//    private FraudDetectionService service;
//
//    @PostMapping("/predict")
//    public String predict(@RequestBody PredictRequest request) {
//
//        return service.predictFraud(request);
//    }
//}
