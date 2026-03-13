package com.edurite.ai.service;

import com.edurite.ai.dto.AiGuidanceRequest;
import com.edurite.ai.dto.AiGuidanceResponse;
import java.security.Principal;

public interface AiGuidanceService {

    AiGuidanceResponse generateGuidance(AiGuidanceRequest request, Principal principal);

    AiGuidanceResponse generateGuidanceFromProfile(Principal principal);
}
