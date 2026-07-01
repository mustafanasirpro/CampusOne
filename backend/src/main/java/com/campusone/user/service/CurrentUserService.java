package com.campusone.user.service;

import com.campusone.academic.entity.Department;
import com.campusone.academic.entity.University;
import com.campusone.academic.repository.DepartmentRepository;
import com.campusone.academic.repository.UniversityRepository;
import com.campusone.common.exception.InvalidAcademicSelectionException;
import com.campusone.common.exception.ResourceNotFoundException;
import com.campusone.user.dto.request.PreferenceUpdateRequest;
import com.campusone.user.dto.request.UpdateProfileRequest;
import com.campusone.user.dto.request.UpdateSkillsRequest;
import com.campusone.user.dto.response.CurrentUserResponse;
import com.campusone.user.dto.response.PreferenceResponse;
import com.campusone.user.dto.response.PublicProfileResponse;
import com.campusone.user.entity.ProfileVisibility;
import com.campusone.user.entity.Skill;
import com.campusone.user.entity.StudentProfile;
import com.campusone.user.entity.User;
import com.campusone.user.entity.UserPreference;
import com.campusone.user.mapper.CurrentUserMapper;
import com.campusone.user.mapper.PreferenceMapper;
import com.campusone.user.mapper.PublicProfileMapper;
import com.campusone.user.repository.SkillRepository;
import com.campusone.user.repository.UserPreferenceRepository;
import com.campusone.user.repository.UserRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final CurrentUserMapper currentUserMapper;
    private final PublicProfileMapper publicProfileMapper;
    private final PreferenceMapper preferenceMapper;

    public CurrentUserService(
            UserRepository userRepository,
            UniversityRepository universityRepository,
            DepartmentRepository departmentRepository,
            SkillRepository skillRepository,
            UserPreferenceRepository userPreferenceRepository,
            CurrentUserMapper currentUserMapper,
            PublicProfileMapper publicProfileMapper,
            PreferenceMapper preferenceMapper) {
        this.userRepository = userRepository;
        this.universityRepository = universityRepository;
        this.departmentRepository = departmentRepository;
        this.skillRepository = skillRepository;
        this.userPreferenceRepository = userPreferenceRepository;
        this.currentUserMapper = currentUserMapper;
        this.publicProfileMapper = publicProfileMapper;
        this.preferenceMapper = preferenceMapper;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(UUID userId) {
        User user = requireProfile(userId);
        return toCurrentUserResponse(user);
    }

    @Transactional
    public CurrentUserResponse updateProfile(
            UUID userId,
            UpdateProfileRequest request) {
        User user = requireProfile(userId);
        StudentProfile profile = user.getStudentProfile();

        updateAcademicSelection(profile, request);
        if (request.fullName() != null) {
            profile.setFullName(request.fullName());
        }
        if (request.bio() != null) {
            profile.setBio(nullIfEmpty(request.bio()));
        }
        if (request.semester() != null) {
            profile.setSemester(request.semester());
        }
        if (request.avatarUrl() != null) {
            profile.setAvatarUrl(nullIfEmpty(request.avatarUrl()));
        }
        if (request.coverImageUrl() != null) {
            profile.setCoverImageUrl(nullIfEmpty(request.coverImageUrl()));
        }
        if (request.location() != null) {
            profile.setLocation(nullIfEmpty(request.location()));
        }
        if (request.visibility() != null) {
            profile.setVisibility(request.visibility());
        }

        updatePreferences(user, request.preferences());
        return toCurrentUserResponse(user);
    }

    @Transactional
    public CurrentUserResponse replaceSkills(
            UUID userId,
            UpdateSkillsRequest request) {
        User user = requireProfile(userId);
        List<String> requestedNames = request.skills();
        Set<String> normalizedNames = requestedNames.stream()
                .map(Skill::normalize)
                .collect(Collectors.toSet());

        Map<String, Skill> skillsByNormalizedName = new LinkedHashMap<>();
        skillRepository.findAllByNormalizedNameIn(normalizedNames)
                .forEach(skill -> skillsByNormalizedName.put(
                        skill.getNormalizedName(),
                        skill));

        List<Skill> missingSkills = requestedNames.stream()
                .filter(name -> !skillsByNormalizedName.containsKey(Skill.normalize(name)))
                .map(Skill::new)
                .toList();
        if (!missingSkills.isEmpty()) {
            skillRepository.saveAll(missingSkills);
            missingSkills.forEach(skill -> skillsByNormalizedName.put(
                    skill.getNormalizedName(),
                    skill));
        }

        List<Skill> orderedSkills = new ArrayList<>(requestedNames.size());
        requestedNames.forEach(name ->
                orderedSkills.add(skillsByNormalizedName.get(Skill.normalize(name))));
        user.getStudentProfile().replaceSkills(orderedSkills);

        return toCurrentUserResponse(user);
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getPublicProfile(
            UUID profileUserId,
            UUID viewerUserId) {
        User user = requireProfile(profileUserId);
        boolean owner = profileUserId.equals(viewerUserId);
        if (user.getStudentProfile().getVisibility() == ProfileVisibility.PRIVATE
                && !owner) {
            throw new ResourceNotFoundException("Profile");
        }
        return publicProfileMapper.toResponse(user);
    }

    private User requireProfile(UUID userId) {
        User user = userRepository.findProfileById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile"));
        if (user.getStudentProfile() == null) {
            throw new ResourceNotFoundException("Profile");
        }
        return user;
    }

    private CurrentUserResponse toCurrentUserResponse(User user) {
        PreferenceResponse preferences = userPreferenceRepository.findByUserId(user.getId())
                .map(preferenceMapper::toResponse)
                .orElseGet(preferenceMapper::defaults);
        return currentUserMapper.toResponse(user, preferences);
    }

    private void updateAcademicSelection(
            StudentProfile profile,
            UpdateProfileRequest request) {
        if (request.universityId() == null && request.departmentId() == null) {
            return;
        }

        UUID universityId = request.universityId() == null
                ? profile.getUniversity().getId()
                : request.universityId();
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new ResourceNotFoundException("University"));
        if (!university.isActive()) {
            throw new InvalidAcademicSelectionException(
                    "The selected university is not active.");
        }

        UUID departmentId = request.departmentId() == null
                ? profile.getDepartment().getId()
                : request.departmentId();
        Department department = departmentRepository
                .findByIdAndUniversityId(departmentId, universityId)
                .orElseThrow(() -> new InvalidAcademicSelectionException(
                        "The selected department does not belong to the selected university."));
        if (!department.isActive()) {
            throw new InvalidAcademicSelectionException(
                    "The selected department is not active.");
        }

        profile.setUniversity(university);
        profile.setDepartment(department);
    }

    private void updatePreferences(
            User user,
            PreferenceUpdateRequest request) {
        if (request == null) {
            return;
        }
        UserPreference preference = userPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> new UserPreference(user));
        preference.update(
                request.theme(),
                request.language(),
                request.compactMode());
        userPreferenceRepository.save(preference);
    }

    private String nullIfEmpty(String value) {
        return value.isEmpty() ? null : value;
    }
}
