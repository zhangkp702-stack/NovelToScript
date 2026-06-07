package com.zkp.my12306.ntc.script.api;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zkp.my12306.ntc.dto.ScriptRecordResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WebAppConfiguration
class ScriptRecordIntegrationTest {

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
    void saveAndListScriptRecord_success() throws Exception {
        String saveBody = """
                {
                  "workTitle": "雨夜归来",
                  "chapterNumber": 1,
                  "chapterContent": "第一章小说内容",
                  "scriptContent": "剧本标题：《雨夜归来》\\n场景一：开场\\n旁白：雨夜。",
                  "modelName": "mock-model"
                }
                """;

        String saveResponse = mockMvc.perform(post("/api/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ScriptRecordResponseDto saved = objectMapper.readValue(saveResponse, ScriptRecordResponseDto.class);
        assertEquals("雨夜归来", saved.workTitle());
        assertEquals(1, saved.chapterNumber());

        String listResponse = mockMvc.perform(get("/api/scripts")
                        .param("workTitle", "雨夜归来"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<ScriptRecordResponseDto> records = objectMapper.readValue(
                listResponse,
                new TypeReference<List<ScriptRecordResponseDto>>() {
                });
        assertEquals(1, records.size());
        assertEquals(saved.id(), records.get(0).id());

        mockMvc.perform(get("/api/scripts/{id}", saved.id()))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "tester")
    void save_withEmptyScriptContent_returnsBadRequest() throws Exception {
        String saveBody = """
                {
                  "workTitle": "测试",
                  "chapterNumber": 1,
                  "chapterContent": "内容",
                  "scriptContent": "   "
                }
                """;

        mockMvc.perform(post("/api/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "other-user")
    void getById_notOwner_returnsNotFound() throws Exception {
        String saveBody = """
                {
                  "workTitle": "隔离测试",
                  "chapterNumber": 1,
                  "chapterContent": "内容",
                  "scriptContent": "剧本内容"
                }
                """;

        String saveResponse = mockMvc.perform(post("/api/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(saveBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ScriptRecordResponseDto saved = objectMapper.readValue(saveResponse, ScriptRecordResponseDto.class);

        mockMvc.perform(get("/api/scripts/{id}", saved.id())
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("intruder")))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "tester")
    void save_sameChapter_updatesExistingRecord() throws Exception {
        String firstSave = """
                {
                  "workTitle": "更新测试",
                  "chapterNumber": 2,
                  "chapterContent": "旧内容",
                  "scriptContent": "旧剧本"
                }
                """;
        String secondSave = """
                {
                  "workTitle": "更新测试",
                  "chapterNumber": 2,
                  "chapterContent": "新内容",
                  "scriptContent": "新剧本"
                }
                """;

        String firstResponse = mockMvc.perform(post("/api/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstSave))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ScriptRecordResponseDto first = objectMapper.readValue(firstResponse, ScriptRecordResponseDto.class);

        String secondResponse = mockMvc.perform(post("/api/scripts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondSave))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        ScriptRecordResponseDto second = objectMapper.readValue(secondResponse, ScriptRecordResponseDto.class);

        assertEquals(first.id(), second.id());
        assertEquals("新剧本", second.scriptContent());
        assertEquals("新内容", second.chapterContent());
        assertTrue(second.updatedAt() != null && !second.updatedAt().isBlank());
    }
}
