package com.edurite.ai.context;

import com.edurite.career.entity.Career;
import com.edurite.career.repository.CareerRepository;
import com.edurite.student.entity.StudentProfile;
import com.edurite.university.entity.EntryRequirement;
import com.edurite.university.entity.Programme;
import com.edurite.university.entity.University;
import com.edurite.university.repository.EntryRequirementRepository;
import com.edurite.university.repository.ProgrammeRepository;
import com.edurite.university.repository.UniversityRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiGuidanceContextService {

    private final CareerRepository careerRepository;
    private final UniversityRepository universityRepository;
    private final ProgrammeRepository programmeRepository;
    private final EntryRequirementRepository entryRequirementRepository;

    public AiGuidanceContextService(
            CareerRepository careerRepository,
            UniversityRepository universityRepository,
            ProgrammeRepository programmeRepository,
            EntryRequirementRepository entryRequirementRepository
    ) {
        this.careerRepository = careerRepository;
        this.universityRepository = universityRepository;
        this.programmeRepository = programmeRepository;
        this.entryRequirementRepository = entryRequirementRepository;
    }

    public AiGuidanceContext build(StudentProfile profile) {
        List<String> careerOptions = careerRepository.findAll().stream().map(Career::getTitle).distinct().limit(20).toList();

        List<University> universities = universityRepository.findByActiveTrueOrderByNameAsc();
        Map<UUID, University> universityById = universities.stream().collect(java.util.stream.Collectors.toMap(University::getId, u -> u));
        List<Programme> programmes = programmeRepository.findByInstitutionIdIn(universityById.keySet());
        Map<UUID, List<EntryRequirement>> requirementsByProgramme = entryRequirementRepository.findByProgrammeIdIn(
                programmes.stream().map(Programme::getId).toList()
        ).stream().collect(java.util.stream.Collectors.groupingBy(EntryRequirement::getProgrammeId));

        List<AiGuidanceContext.UniversityOption> universityOptions = universities.stream().map(university -> {
            List<Programme> universityProgrammes = programmes.stream()
                    .filter(programme -> university.getId().equals(programme.getInstitutionId()))
                    .limit(10)
                    .toList();
            List<AiGuidanceContext.ProgrammeOption> programmeOptions = universityProgrammes.stream().map(programme -> {
                List<EntryRequirement> requirements = requirementsByProgramme.getOrDefault(programme.getId(), List.of());
                List<String> entryRequirements = requirements.stream().map(EntryRequirement::getRequirementText).distinct().toList();
                List<String> subjectRequirements = requirements.stream().map(EntryRequirement::getSubjectRequirements)
                        .filter(text -> text != null && !text.isBlank()).distinct().toList();
                return new AiGuidanceContext.ProgrammeOption(programme.getName(), programme.getLevel(), entryRequirements, subjectRequirements);
            }).toList();
            return new AiGuidanceContext.UniversityOption(university.getName(), university.getWebsite(), programmeOptions);
        }).toList();

        List<AiGuidanceContext.EntryRequirementOption> entryRequirementOptions = universityOptions.stream()
                .flatMap(university -> university.programmes().stream().flatMap(programme -> programme.entryRequirements().stream()
                        .map(req -> new AiGuidanceContext.EntryRequirementOption(university.name(), programme.name(), req, programme.subjectRequirements()))))
                .limit(40)
                .toList();

        List<String> programmeOptions = universityOptions.stream()
                .flatMap(option -> option.programmes().stream().map(AiGuidanceContext.ProgrammeOption::name))
                .distinct().limit(30).toList();

        Map<String, String> studentProfile = new HashMap<>();
        studentProfile.put("qualificationLevel", fallback(profile.getQualificationLevel()));
        studentProfile.put("interests", fallback(profile.getInterests()));
        studentProfile.put("skills", fallback(profile.getSkills()));
        studentProfile.put("location", fallback(profile.getLocation()));

        return new AiGuidanceContext(careerOptions, programmeOptions, universityOptions, entryRequirementOptions, studentProfile);
    }

    private String fallback(String value) {
        return value == null || value.isBlank() ? "not provided" : value;
    }

    public record AiGuidanceContext(
            List<String> careerOptions,
            List<String> programmeOptions,
            List<UniversityOption> universities,
            List<EntryRequirementOption> entryRequirements,
            Map<String, String> studentProfile
    ) {
        public record UniversityOption(String name, String website, List<ProgrammeOption> programmes) {
        }

        public record ProgrammeOption(String name, String level, List<String> entryRequirements, List<String> subjectRequirements) {
        }

        public record EntryRequirementOption(String university, String programme, String requirement, List<String> subjectRequirements) {
        }
    }
}
