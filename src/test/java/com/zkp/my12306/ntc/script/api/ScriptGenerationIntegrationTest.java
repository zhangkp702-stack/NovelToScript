package com.zkp.my12306.ntc.script.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkp.my12306.ntc.dto.ValidationErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WebAppConfiguration
class ScriptGenerationIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    @WithMockUser(username = "tester")
    void generate_withTwoChapters_returnsValidationError() throws Exception {
        String body = """
                {
                  "title": "测试",
                  "chapters": ["第一章", "第二章"]
                }
                """;

        String response = mockMvc.perform(post("/api/scripts/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ValidationErrorResponseDto error = objectMapper.readValue(response, ValidationErrorResponseDto.class);
        assertEquals("INSUFFICIENT_CHAPTERS", error.code());
        assertEquals(3, error.minChapters());
        assertEquals(2, error.filledCount());
    }

    @Test
    @WithMockUser(username = "tester")
    void generate_withChapterGap_returnsValidationError() throws Exception {
        String body = """
                {
                  "title": "测试",
                  "chapters": ["第一章", "", "第三章"]
                }
                """;

        String response = mockMvc.perform(post("/api/scripts/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ValidationErrorResponseDto error = objectMapper.readValue(response, ValidationErrorResponseDto.class);
        assertEquals("CHAPTER_GAP", error.code());
        assertEquals(2, error.invalidIndexes().get(0));
    }
}
