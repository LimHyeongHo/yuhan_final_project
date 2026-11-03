package com.Nbbang.backend.domain.bookreport.controller;

import com.Nbbang.backend.domain.admin.interceptor.AdminAuthInterceptor;
import com.Nbbang.backend.domain.bookreport.config.DepartmentBookKeywordCatalog;
import com.Nbbang.backend.domain.bookreport.service.BookReportManualJobService;
import com.Nbbang.backend.domain.bookreport.service.BookReportManualWorker;
import com.Nbbang.backend.domain.product.entity.ProductCategory;
import com.Nbbang.backend.global.exception.GlobalExceptionHandler;
import com.Nbbang.backend.global.interceptor.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBookReportControllerTest {

    private DepartmentBookKeywordCatalog keywordCatalog;
    private BookReportManualJobService jobService;
    private BookReportManualWorker worker;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        keywordCatalog = mock(DepartmentBookKeywordCatalog.class);
        jobService = mock(BookReportManualJobService.class);
        worker = mock(BookReportManualWorker.class);
        AdminBookReportController controller =
                new AdminBookReportController(keywordCatalog, jobService, worker);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new AuthInterceptor(), new AdminAuthInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void rejectsARequestWithoutALoginSession() throws Exception {
        mockMvc.perform(post("/api/admin/book-reports/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"COMPUTER_SOFTWARE\"}"))
                .andExpect(status().isUnauthorized());

        verify(jobService, never()).createJob(ProductCategory.COMPUTER_SOFTWARE);
    }

    @Test
    void rejectsANonAdminSession() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_BUYER");

        mockMvc.perform(post("/api/admin/book-reports/generate")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"COMPUTER_SOFTWARE\"}"))
                .andExpect(status().isForbidden());

        verify(jobService, never()).createJob(ProductCategory.COMPUTER_SOFTWARE);
    }

    @Test
    void startsAnAsyncJobForAnAdmin() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_ADMIN");
        Map<String, Object> snapshot = Map.of(
                "jobId", "job-1",
                "category", "COMPUTER_SOFTWARE",
                "status", "QUEUED");
        when(jobService.createJob(ProductCategory.COMPUTER_SOFTWARE))
                .thenReturn(new BookReportManualJobService.StartResult(true, "job-1", snapshot));

        mockMvc.perform(post("/api/admin/book-reports/generate")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"computer_software\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.category").value("COMPUTER_SOFTWARE"))
                .andExpect(jsonPath("$.status").value("QUEUED"));

        verify(worker).run("job-1", ProductCategory.COMPUTER_SOFTWARE);
    }

    @Test
    void returnsConflictWithoutStartingAnotherWorker() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_ADMIN");
        Map<String, Object> active = Map.of(
                "jobId", "job-1",
                "category", "MECHANICAL_ENGINEERING",
                "status", "RUNNING");
        when(jobService.createJob(ProductCategory.COMPUTER_SOFTWARE))
                .thenReturn(new BookReportManualJobService.StartResult(false, "job-1", active));

        mockMvc.perform(post("/api/admin/book-reports/generate")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"COMPUTER_SOFTWARE\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.jobId").value("job-1"))
                .andExpect(jsonPath("$.status").value("RUNNING"));

        verify(worker, never()).run("job-1", ProductCategory.COMPUTER_SOFTWARE);
    }

    @Test
    void returnsAnExistingJobToAnAdmin() throws Exception {
        MockHttpSession session = authenticatedSession("ROLE_ADMIN");
        when(jobService.getJob("job-1")).thenReturn(Map.of(
                "jobId", "job-1",
                "status", "COMPLETED"));

        mockMvc.perform(get("/api/admin/book-reports/jobs/job-1").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    private MockHttpSession authenticatedSession(String role) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", "admin@yuhan.ac.kr");
        session.setAttribute("role", role);
        return session;
    }
}
