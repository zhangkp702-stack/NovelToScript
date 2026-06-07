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
    void generate_withEmptyChapter_returnsValidationError() throws Exception {
        String body = """
                {
                  "title": "测试",
                  "chapterNumber": 1,
                  "chapterContent": "   "
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
        assertEquals("EMPTY_CHAPTER", error.code());
        assertEquals(1, error.minChapters());
        assertEquals(0, error.filledCount());
    }

    @Test
    @WithMockUser(username = "tester")
    void generate_withInvalidChapterNumber_returnsValidationError() throws Exception {
        String body = """
                {
                  "title": "测试",
                  "chapterNumber": 0,
                  "chapterContent": "第一章"
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
        assertEquals("INVALID_CHAPTER_NUMBER", error.code());
    }
}
