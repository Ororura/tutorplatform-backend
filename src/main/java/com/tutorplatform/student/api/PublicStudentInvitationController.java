package com.tutorplatform.student.api;

import com.tutorplatform.auth.api.CurrentUserResponse;
import com.tutorplatform.student.api.requrest.AcceptStudentInviteRequest;
import com.tutorplatform.student.api.response.PublicStudentInviteResponse;
import com.tutorplatform.student.application.PublicStudentInvitationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/student-invitations")
public class PublicStudentInvitationController implements PublicStudentInvitationApi {

    private final PublicStudentInvitationService invitationService;

    public PublicStudentInvitationController(PublicStudentInvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @Override
    @GetMapping(value = "/{token}", produces = MediaType.APPLICATION_JSON_VALUE)
    public PublicStudentInviteResponse getPublicStudentInvitation(@PathVariable String token) {
        return invitationService.getInvitation(token);
    }

    @Override
    @PostMapping(value = "/{token}/accept", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public CurrentUserResponse acceptStudentInvitation(
            @PathVariable String token,
            @Valid @RequestBody AcceptStudentInviteRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        return invitationService.acceptInvitation(token, request, servletRequest, servletResponse);
    }
}
