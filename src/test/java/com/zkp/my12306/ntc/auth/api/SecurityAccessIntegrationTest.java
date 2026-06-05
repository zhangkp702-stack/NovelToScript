package com.zkp.my12306.ntc.auth.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@WebAppConfiguration
class SecurityAccessIntegrationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void probe_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/probe"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin")
    void probe_withAuthentication_returnsOk() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("已认证"));
    }

    @Test
    void me_withoutSession_returnsUnauthorized() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_thenMe_withSession_returnsOk() throws Exception {
        MvcResult loginResult = mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType("application/json")
                                .content("{\"username\":\"admin\",\"password\":\"1233321\"}"))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(
                        MockMvcRequestBuilders.get("/api/auth/me")
                                .session(session))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"username\":\"admin\",\"authenticated\":true}"));
    }

    @Test
    void register_thenLogin_withNewAccount_returnsOk() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType("application/json")
                                .content("{\"account\":\"user_1001\",\"password\":\"123456\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("账户创建成功，请登录"));

        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/login")
                                .contentType("application/json")
                                .content("{\"username\":\"user_1001\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void register_duplicateAccount_returnsConflict() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType("application/json")
                                .content("{\"account\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("账户已经存在，请直接登陆"));
    }

    @Test
    void register_withBlankParams_returnsBadRequest() throws Exception {
        mockMvc.perform(
                        MockMvcRequestBuilders.post("/api/auth/register")
                                .contentType("application/json")
                                .content("{\"account\":\"   \",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("账号和密码不能为空"));
    }
}
