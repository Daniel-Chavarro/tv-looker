package org.tvl.tvlooker.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.tvl.tvlooker.api.exception.GlobalExceptionHandler;
import org.tvl.tvlooker.domain.exception.UserNotFoundException;
import org.tvl.tvlooker.domain.model.User;
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
                .createdAt(Timestamp.from(Instant.now()))
                .build();
    }

    @Nested
    class GetAllUsers {

        @Test
        void givenUsersExist_whenGetAllUsers_thenReturnsUserList() throws Exception {
            User secondUser = User.builder()
                    .id(UUID.randomUUID())
                    .username("anotheruser")
                    .email("another@example.com")
                    .name("Another User")
                    .password("pass456")
                    .createdAt(Timestamp.from(Instant.now()))
                    .build();
            List<User> users = Arrays.asList(testUser, secondUser);

            when(userService.getAll()).thenReturn(users);

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].username", is("testuser")))
                    .andExpect(jsonPath("$[0].email", is("test@example.com")))
                    .andExpect(jsonPath("$[1].username", is("anotheruser")));

            verify(userService, times(1)).getAll();
        }

        @Test
        void givenNoUsers_whenGetAllUsers_thenReturnsEmptyList() throws Exception {
            when(userService.getAll()).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/users"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$", hasSize(0)));

            verify(userService, times(1)).getAll();
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
                    .andExpect(jsonPath("$.name", is("Test User")));

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
                    .createdAt(testUser.getCreatedAt())
                    .build();

            when(userService.update(eq(testUserId), any(User.class))).thenReturn(updatedUser);

            mockMvc.perform(put("/api/v1/users/{id}", testUserId)
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

            mockMvc.perform(put("/api/v1/users/{id}", nonExistentId)
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
