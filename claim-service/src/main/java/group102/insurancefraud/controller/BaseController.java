package group102.insurancefraud.controller;

import group102.insurancefraud.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;

public abstract class BaseController {

    @ModelAttribute("userRole")
    public String userRole(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) return "";
        return userDetails.getRole();  // "STAFF" | "INVESTIGATOR" | "ADMIN"
    }
}