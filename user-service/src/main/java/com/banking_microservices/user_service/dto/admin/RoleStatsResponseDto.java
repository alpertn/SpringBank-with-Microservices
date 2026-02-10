package com.banking_microservices.user_service.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleStatsResponseDto {
    private Map<String, Long> roleDistribution;
    private Long totalUsers;
}
