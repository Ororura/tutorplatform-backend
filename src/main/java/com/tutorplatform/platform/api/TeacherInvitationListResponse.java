package com.tutorplatform.platform.api;

import java.util.List;

public record TeacherInvitationListResponse(
    List<TeacherInvitationSummaryResponse> invitations
) {
}
