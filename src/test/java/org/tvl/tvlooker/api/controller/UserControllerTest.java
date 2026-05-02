package org.tvl.tvlooker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tvl.tvlooker.api.exception.GlobalExceptionHandler;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.dto.User;
import org.tvl.tvlooker.domain.model.enums.UserAuthority;
import org.tvl.tvlooker.service.UserService;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    private UUID testUserId;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        testUserId = UUID.randomUUID();
        testUser = User.builder()
                .id(testUserId)
                .username("testuser")
                .email("test@example.com")
                .name("Test User")
                .password("password123")
                .authority(UserAuthority.USER)
                .createdAt(Timestamp.from(Instant.now()))
                .build();
    }

    @Nested
    class GetAllUsers {

        @Test
        void givenUsersExist_whenGetAllUsers_thenReturnsPaginatedList() throws Exception {
            User secondUser = User.builder()
                    .id(testUserId)
                    .username("seconduser")
                    .email("second@test.com")
                    .authority(UserAuthority.USER)
                    .build();
            List<User> users = Arrays.asList(testUser, secondUser);
            Page<User> usersPage = new PageImpl<>(users, PageRequest.of(0, 50), users.size());

            when(userService.getAll(any(Pageable.class))).thenReturn(usersPage);

            mockMvc.perform(get("/api/v1/users")
                            .param("page", "0")
                            .param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.content", hasSize(2)))
                    .andExpect(jsonPath("$.content[0].id", is(testUserId.toString())))
                    .andExpect(jsonPath("$.totalItems", is(2)))
                    .andExpect(jsonPath("$.actualPage", is(0)));

            verify(userService, times(1)).getAll(any(Pageable.class));
        }

        @Test
        void givenNoUsers_whenGetAllUsers_thenReturnsEmptyPage() throws Exception {
            Page<User> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 50), 0);
            when(userService.getAll(any(Pageable.class))).thenReturn(emptyPage);

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content", hasSize(0)))
                    .andExpect(jsonPath("$.totalItems", is(0)));

            verify(userService, times(1)).getAll(any(Pageable.class));
        }
    }

    @Nested
    class GetUserById {

        @Test
        void givenUserExists_whenGetUserById_thenReturnsUser() throws Exception {
            when(userService.getById(testUserId)).thenReturn(testUser);

            mockMvc.perform(get("/api/v1/users/{id}", testUserId))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id", is(testUserId.toString())))
                    .andExpect(jsonPath("$.username", is("testuser")))
                    .andExpect(jsonPath("$.email", is("test@example.com")))
                    .andExpect(jsonPath("$.name", is("Test User")))
                    .andExpect(jsonPath("$.password").doesNotExist())
                    .andExpect(jsonPath("$.authority", is("USER")));

            verify(userService, times(1)).getById(testUserId);
        }

        @Test
        void givenUserNotExists_whenGetUserById_thenReturns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            when(userService.getById(nonExistentId))
                    .thenThrow(new UserNotFoundException("User not found with id: " + nonExistentId));

            mockMvc.perform(get("/api/v1/users/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("User Not Found")))
                    .andExpect(jsonPath("$.body.detail", containsString("User not found with id")));

            verify(userService, times(1)).getById(nonExistentId);
        }
    }

    @Nested
    class CreateUser {

        @Test
        void givenValidRequest_whenCreateUser_thenReturnsCreatedUser() throws Exception {
            String requestBody = """
                {
                    "username": "newuser",
                    "email": "new@example.com",
                    "name": "New User",
                    "password": "newpass123"
                }
                """;

            User createdUser = User.builder()
                    .id(UUID.randomUUID())
                    .username("newuser")
                    .email("new@example.com")
                    .name("New User")
                    .password("newpass123")
                    .authority(UserAuthority.USER)
                    .createdAt(Timestamp.from(Instant.now()))
                    .build();

            when(userService.create(any(User.class))).thenReturn(createdUser);

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("Location"))
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.username", is("newuser")))
                    .andExpect(jsonPath("$.email", is("new@example.com")))
                    .andExpect(jsonPath("$.name", is("New User")));

            verify(userService, times(1)).create(any(User.class));
        }

        @Test
        void givenInvalidRequest_whenCreateUser_thenReturns400() throws Exception {
            String requestBody = """
                    {
                        "email": "not-a-valid-email",
                        "name": "Invalid User"
                        }""";

            mockMvc.perform(post("/api/v1/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.body.title", is("Validation Failed")))
                    .andExpect(jsonPath("$.body.type", containsString("/errors/validation-failed")));
        }
    }

    @Nested
    class UpdateUser {

        @Test
        void givenValidRequest_whenUpdateUser_thenReturnsUpdatedUser() throws Exception {
            String requestBody = """
                {
                    "username": "updateduser",
                    "email": "updated@example.com",
                    "name": "Updated User",
                    "password": "updatedpass"
                }
                """;

            User updatedUser = User.builder()
                    .id(testUserId)
                    .username("updateduser")
                    .email("updated@example.com")
                    .name("Updated User")
                    .password("updatedpass")
                    .authority(UserAuthority.USER)
                    .createdAt(testUser.getCreatedAt())
                    .build();

            when(userService.update(eq(testUserId), any(User.class))).thenReturn(updatedUser);

            mockMvc.perform(patch("/api/v1/users/{id}", testUserId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.username", is("updateduser")))
                    .andExpect(jsonPath("$.email", is("updated@example.com")))
                    .andExpect(jsonPath("$.name", is("Updated User")));

            verify(userService, times(1)).update(eq(testUserId), any(User.class));
        }

        @Test
        void givenUserNotExists_whenUpdateUser_thenReturns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            String requestBody = """
                {
                    "username": "updateduser"
                }
                """;

            when(userService.update(eq(nonExistentId), any(User.class)))
                    .thenThrow(new UserNotFoundException("User not found with id: " + nonExistentId));

            mockMvc.perform(patch("/api/v1/users/{id}", nonExistentId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("User Not Found")));

            verify(userService, times(1)).update(eq(nonExistentId), any(User.class));
        }

    }

    @Nested
    class DeleteUser {

        @Test
        void givenUserExists_whenDeleteUser_thenReturns204() throws Exception {
            doNothing().when(userService).delete(testUserId);

            mockMvc.perform(delete("/api/v1/users/{id}", testUserId))
                    .andExpect(status().isNoContent());

            verify(userService, times(1)).delete(testUserId);
        }

        @Test
        void givenUserNotExists_whenDeleteUser_thenReturns404() throws Exception {
            UUID nonExistentId = UUID.randomUUID();
            doThrow(new UserNotFoundException("User not found with id: " + nonExistentId))
                    .when(userService).delete(nonExistentId);

            mockMvc.perform(delete("/api/v1/users/{id}", nonExistentId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.body.title", is("User Not Found")));

            verify(userService, times(1)).delete(nonExistentId);
        }
    }
}
