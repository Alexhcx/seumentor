// main/java/com/projetointegrador/seumentor/user/service/UserQueryAdapter.java
package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.course.model.CourseArea; // Importar CourseArea
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.*;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.MentorAvailability;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserQueryAdapter implements UserQuery {

    private final UserRepository userRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private static final Logger log = LoggerFactory.getLogger(UserQueryAdapter.class);

    // --- Métodos findById, findByEmail, getUserReferenceById, findAllUserRepresentations, findAvailabilityRepresentationById, findAvailabilitiesRepresentationByUserId (sem alterações) ---
    @Transactional(readOnly = true)
    @Override
    public Optional<UserRepresentation> findById(Long userId) {
        log.debug("Adapter: Finding user by ID: {}", userId);
        return userRepository.findById(userId).map(this::mapToRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserRepresentation> findByEmail(String email) {
        log.debug("Adapter: Finding user by email: {}", email);
        return userRepository.findByEmail(email).map(this::mapToRepresentation);
    }

    @Override
    @Transactional
    public User getUserReferenceById(Long userId) {
        log.debug("Adapter: Getting user reference by ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("Adapter: User reference requested for non-existent ID: {}", userId);
            throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        }
        return userRepository.getReferenceById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserRepresentation> findAllUserRepresentations() {
        log.debug("Adapter: Finding all user representations");
        return userRepository.findAll()
                .stream()
                .map(this::mapToRepresentation)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAvailabilityRepresentation> findAvailabilityRepresentationById(Long availabilityId) {
        log.debug("Adapter: Finding availability representation by ID: {}", availabilityId);
        return mentorAvailabilityRepository.findById(availabilityId)
                .map(this::mapToAvailabilityRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAvailabilityRepresentation> findAvailabilitiesRepresentationByUserId(Long userId) {
        log.debug("Adapter: Finding availability representations for user ID: {}", userId);
        if (!userRepository.existsById(userId)) {
            log.warn("Adapter: Availabilities requested for non-existent user ID: {}", userId);
            throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        }
        return mentorAvailabilityRepository.findByUserId(userId)
                .stream()
                .map(this::mapToAvailabilityRepresentation)
                .collect(Collectors.toList());
    }

    // --- MÉTODOS DE PERFIL DE MENTOR ATUALIZADOS ---

    @Override
    @Transactional(readOnly = true)
    public Optional<MentorProfileRepresentation> findMentorProfileById(Long mentorId) {
        log.debug("Adapter: Attempting to find mentor profile for ID: {}", mentorId);
        Optional<User> userOptional = userRepository.findById(mentorId);
        if (userOptional.isEmpty()) {
            log.warn("Adapter: Mentor profile not found: User not found with ID: {}", mentorId);
            return Optional.empty();
        }
        User user = userOptional.get();

        List<MentorAvailability> userAvailabilities = mentorAvailabilityRepository.findByUserId(mentorId);
        log.debug("Adapter: Found {} availabilities for mentor ID: {}", userAvailabilities.size(), mentorId);

        // Agrupa as disponibilidades pela Disciplina
        Map<Discipline, List<MentorAvailability>> groupedByDiscipline = userAvailabilities.stream()
                .filter(avail -> avail.getDiscipline() != null) // Garante que a disciplina não é nula
                .collect(Collectors.groupingBy(MentorAvailability::getDiscipline));

        // Mapeia cada grupo (disciplina + lista de availabilities) para o DTO de disciplina
        List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = groupedByDiscipline.entrySet().stream()
                .map(entry -> {
                    Discipline discipline = entry.getKey();
                    CourseArea courseArea = discipline.getCourseArea(); // Obter CourseArea da disciplina
                    String courseName = (courseArea != null) ? courseArea.getCourse() : "[Curso não definido]"; // Obter nome do curso
                    String disciplineName = discipline.getDisciplineName();

                    // Mapeia a lista de availabilities desse grupo para a lista de slots
                    List<AvailabilitySlotRepresentation> slots = entry.getValue().stream()
                            .map(avail -> new AvailabilitySlotRepresentation(
                                    avail.getDayOfWeek(),
                                    avail.getStartTime(),
                                    avail.getEndTime()
                                    // courseName foi removido daqui
                            ))
                            .collect(Collectors.toList());

                    // Cria o DTO da disciplina, agora incluindo o courseName
                    return new MentorDisciplineAvailabilityRepresentation(
                            disciplineName,
                            courseName, // Adicionado aqui
                            slots
                    );
                })
                .sorted(Comparator.comparing(MentorDisciplineAvailabilityRepresentation::disciplineName)) // Ordena pela disciplina
                .collect(Collectors.toList());

        log.info("Adapter: Successfully mapped mentor profile for ID: {}", mentorId);

        // Cria o perfil final do mentor, sem o courseName no nível raiz
        MentorProfileRepresentation profile = new MentorProfileRepresentation(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                // user.getCourseName(), // Removido
                disciplineAvailabilities
        );
        return Optional.of(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorProfileRepresentation> findAllMentorProfiles() {
        log.debug("Adapter: Attempting to find all mentor profiles.");

        // Busca todos os IDs de usuários que têm alguma disponibilidade (são mentores)
        List<Long> mentorIds = mentorAvailabilityRepository.findAll().stream()
                .map(MentorAvailability::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .collect(Collectors.toList());

        log.debug("Adapter: Found {} unique mentor IDs with availabilities.", mentorIds.size());
        if (mentorIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Busca os dados desses mentores
        List<User> mentors = userRepository.findAllById(mentorIds);
        Map<Long, User> mentorMap = mentors.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        log.debug("Adapter: Fetched details for {} mentors.", mentors.size());

        // Busca todas as disponibilidades desses mentores de uma vez
        List<MentorAvailability> allAvailabilities = mentorAvailabilityRepository.findByUserIdIn(mentorIds);
        log.debug("Adapter: Fetched {} total availabilities for the identified mentors.", allAvailabilities.size());

        // Agrupa as disponibilidades por ID do mentor
        Map<Long, List<MentorAvailability>> availabilitiesByMentorId = allAvailabilities.stream()
                .filter(avail -> avail.getUser() != null)
                .collect(Collectors.groupingBy(avail -> avail.getUser().getId()));

        // Constrói a lista final de perfis
        List<MentorProfileRepresentation> mentorProfiles = mentorIds.stream()
                .map(id -> {
                    User mentor = mentorMap.get(id);
                    if (mentor == null) return null; // Mentor pode ter sido deletado entre as buscas

                    List<MentorAvailability> mentorAvailabilities = availabilitiesByMentorId.getOrDefault(id, Collections.emptyList());

                    // Agrupa as disponibilidades do mentor atual pela Disciplina
                    Map<Discipline, List<MentorAvailability>> groupedByDiscipline = mentorAvailabilities.stream()
                            .filter(avail -> avail.getDiscipline() != null)
                            .collect(Collectors.groupingBy(MentorAvailability::getDiscipline));

                    // Mapeia cada grupo (disciplina + lista de availabilities) para o DTO de disciplina
                    List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = groupedByDiscipline.entrySet().stream()
                            .map(entry -> {
                                Discipline discipline = entry.getKey();
                                CourseArea courseArea = discipline.getCourseArea();
                                String courseName = (courseArea != null) ? courseArea.getCourse() : "[Curso não definido]";
                                String disciplineName = discipline.getDisciplineName();

                                List<AvailabilitySlotRepresentation> slots = entry.getValue().stream()
                                        .map(avail -> new AvailabilitySlotRepresentation(
                                                avail.getDayOfWeek(),
                                                avail.getStartTime(),
                                                avail.getEndTime()
                                                // courseName foi removido daqui
                                        ))
                                        .collect(Collectors.toList());

                                return new MentorDisciplineAvailabilityRepresentation(
                                        disciplineName,
                                        courseName, // Adicionado aqui
                                        slots
                                );
                            })
                            .sorted(Comparator.comparing(MentorDisciplineAvailabilityRepresentation::disciplineName))
                            .collect(Collectors.toList());

                    // Cria o perfil do mentor
                    return new MentorProfileRepresentation(
                            mentor.getId(),
                            mentor.getFirstName(),
                            mentor.getLastName(),
                            // mentor.getCourseName(), // Removido
                            disciplineAvailabilities
                    );
                })
                .filter(Objects::nonNull) // Remove mentores que não foram encontrados no mapa
                .collect(Collectors.toList());

        log.info("Adapter: Successfully mapped {} mentor profiles.", mentorProfiles.size());
        return mentorProfiles;
    }

    // --- MÉTODOS DE MAPEAMENTO PRIVADOS ---

    private UserRepresentation mapToRepresentation(User user) {
        // Mapeamento original mantido
        return new UserRepresentation(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getProfileImg(),
                user.getBirthday(),
                user.getCpf(),
                user.getPhone(),
                user.getCity(),
                user.getState(),
                user.getCountry(),
                user.getCourseName(),
                user.getSemester(),
                user.getUniversity());
    }

    // Mapeamento original para UserAvailabilityRepresentation (sem courseName)
    private UserAvailabilityRepresentation mapToAvailabilityRepresentation(MentorAvailability availability) {
        if (availability == null) {
            return null;
        }
        Discipline discipline = null;
        String disciplineName = "[Disciplina não carregada]";
        Long disciplineId = null;
        try {
            discipline = availability.getDiscipline();
            if (discipline != null) {
                disciplineId = discipline.getId();
                disciplineName = discipline.getDisciplineName();
            } else {
                disciplineName = "[Disciplina nula]";
            }
        } catch (EntityNotFoundException e) {
            log.warn("Adapter: Disciplina associada à disponibilidade {} não encontrada.", availability.getId());
            disciplineName = "[Disciplina inválida/removida]";
        } catch (Exception e) {
            log.error("Adapter: Erro ao acessar disciplina para disponibilidade {}: {}", availability.getId(), e.getMessage());
            disciplineName = "[Erro ao carregar disciplina]";
        }

        SimpleDisciplineRepresentation userApiDisciplineRep = new SimpleDisciplineRepresentation(
                disciplineId,
                disciplineName);

        return new UserAvailabilityRepresentation(
                availability.getId(),
                userApiDisciplineRep,
                availability.getDayOfWeek(),
                availability.getStartTime(),
                availability.getEndTime());
    }
}