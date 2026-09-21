package com.tutorplatform.content.application;

import com.tutorplatform.content.domain.LessonMaterialType;
import java.util.UUID;

public record UpdateLessonMaterialCommand(
        LessonMaterialType materialType,
        String title,
        String content,
        UUID fileAssetId,
        String externalUrl,
        int position,
        long version) {}
