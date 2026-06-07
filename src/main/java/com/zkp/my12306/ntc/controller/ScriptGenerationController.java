package com.zkp.my12306.ntc.controller;

import com.zkp.my12306.ntc.dto.ErrorResponseDto;
import com.zkp.my12306.ntc.dto.ScriptGenerateRequestDto;
import com.zkp.my12306.ntc.dto.ScriptGenerateResponseDto;
import com.zkp.my12306.ntc.service.ScriptApplicationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/scripts")
public class ScriptGenerationController {
    private final ScriptApplicationService scriptApplicationService;

    public ScriptGenerationController(ScriptApplicationService scriptApplicationService) {
        this.scriptApplicationService = scriptApplicationService;
    }

    @PostMapping("/generate")
    public ResponseEntity<?> generate(@RequestBody ScriptGenerateRequestDto request, Authentication authentication) {
        try {
            ScriptGenerateResponseDto response = scriptApplicationService.generateScript(request, authentication.getName());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponseDto(ex.getMessage()));
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorResponseDto(resolveErrorMessage(ex)));
        }
    }

    private String resolveErrorMessage(Exception ex) {
        Throwable root = ex;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        if (root == ex) {
            return ex.getMessage();
        }
        return ex.getMessage() + "：" + root.getMessage();
    }
}
