package group102.insurancefraud.service;

import group102.insurancefraud.dto.request.PredictRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FraudDetectionService {

//    @Autowired
//    private RestTemplate restTemplate;
//
//    public String predictFraud(PredictRequest request) {
//
//        String url = "http://localhost:8000/predict";
//
//        return restTemplate.postForObject(
//                url,
//                request,
//                String.class
//        );
//    }
}
