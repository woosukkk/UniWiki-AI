package com.uniwiki.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CommunityPostImportRequestDto(
        @NotEmpty @Size(max = 100) List<@Valid CommunityPostImportItemDto> posts
) { }
