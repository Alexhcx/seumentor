// main/java/com/projetointegrador/seumentor/user/service/UserQueryAdapter.java
package com.projetointegrador.seumentor.user.service;

import com.projetointegrador.seumentor.common.util.CPFUtils;
import com.projetointegrador.seumentor.course.model.CourseArea;
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringParticipationRepresentation;
import com.projetointegrador.seumentor.tutoring.api.dto.TutoringRepresentation;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.user.api.UserAvailabilityFinder;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.*;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.DayWeek;
import com.projetointegrador.seumentor.user.model.MentorAvailability;
import com.projetointegrador.seumentor.user.model.User;
import com.projetointegrador.seumentor.user.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired; // Add this
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class UserQueryAdapter implements UserQuery, UserAvailabilityFinder {

    private final UserRepository userRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final TutoringParticipantsRepository tutoringParticipantsRepository;
    private TutoringQuery tutoringQuery;

    private static final DateTimeFormatter ADAPTER_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ADAPTER_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Logger log = LoggerFactory.getLogger(UserQueryAdapter.class);

    @Autowired
    public UserQueryAdapter(UserRepository userRepository,
            MentorAvailabilityRepository mentorAvailabilityRepository,
            TutoringParticipantsRepository tutoringParticipantsRepository) {
        this.userRepository = userRepository;
        this.mentorAvailabilityRepository = mentorAvailabilityRepository;
        this.tutoringParticipantsRepository = tutoringParticipantsRepository;
    }

    @Autowired
    @Lazy
    public void setTutoringQuery(TutoringQuery tutoringQuery) {
        this.tutoringQuery = tutoringQuery;
    }

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

    @Override
    @Transactional(readOnly = true)
    public List<MentorAvailability> findAllAvailabilitiesByDayOfWeek(DayWeek dayOfWeek) {
        log.debug("Adapter (Impl UserAvailabilityFinder): Finding all mentor availabilities for day: {}", dayOfWeek);
        try {
            return mentorAvailabilityRepository.findByDayOfWeek(dayOfWeek);
        } catch (Exception e) {
            log.error(
                    "Adapter (Impl UserAvailabilityFinder): Error fetching availabilities from repository for day {}: {}",
                    dayOfWeek, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> getUserMentoringSessions(Long userId) {
        log.debug("Adapter: Getting mentoring sessions for user ID (as mentor): {}", userId);
        if (this.tutoringQuery == null) {
            log.error("TutoringQuery is not injected in UserQueryAdapter for getUserMentoringSessions");
            throw new IllegalStateException("TutoringQuery service not available");
        }
        if (!userRepository.existsById(userId)) {
            log.warn("Adapter: User not found with ID: {}", userId);
            throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        }
        return tutoringQuery.findAllTutoringsByMentorId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringParticipationRepresentation> getUserParticipationSessions(Long userId) {
        log.debug(
                "Adapter: Obtendo sessões de participação para o usuário ID: {} usando TutoringParticipationRepresentation",
                userId);

        // Verifica se o usuário existe primeiro
        if (!userRepository.existsById(userId)) {
            log.warn("Adapter: Usuário com ID: {} não encontrado ao buscar sessões de participação.", userId);
            throw new UserNotFoundException("Usuário não encontrado com ID: " + userId);
        }

        // Busca todas as participações (TutoringParticipants) para o userId
        List<TutoringParticipants> participations = tutoringParticipantsRepository.findByUserId(userId);

        if (participations.isEmpty()) {
            log.debug("Adapter: Nenhuma sessão de participação encontrada para o usuário ID: {}", userId);
            return Collections.emptyList();
        }

        // Mapeia cada entidade Tutoring (obtida de TutoringParticipants) para
        // TutoringParticipationRepresentation
        return participations.stream()
                .map(TutoringParticipants::getTutoring) // Obtém a entidade Tutoring de cada participação
                .filter(Objects::nonNull) // Garante que a tutoria associada não é nula
                .distinct() // Evita duplicatas se houver várias entradas de participação para a mesma
                            // tutoria (improvável)
                .map(this::mapToTutoringParticipationRepresentation) // Usa o novo método de mapeamento
                .collect(Collectors.toList());
    }

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

        Map<Discipline, List<MentorAvailability>> groupedByDiscipline = userAvailabilities.stream()
                .filter(avail -> avail.getDiscipline() != null)
                .collect(Collectors.groupingBy(MentorAvailability::getDiscipline));

        List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = groupedByDiscipline.entrySet()
                .stream()
                .map(entry -> {
                    Discipline discipline = entry.getKey();
                    CourseArea courseArea = discipline.getCourseArea(); // Assuming Discipline has getCourseArea()
                    String courseName = (courseArea != null) ? courseArea.getCourse() : "[Curso não definido]";
                    String disciplineName = discipline.getDisciplineName();

                    List<AvailabilitySlotRepresentation> slots = entry.getValue().stream()
                            .map(avail -> new AvailabilitySlotRepresentation(
                                    avail.getDayOfWeek(),
                                    avail.getStartTime(),
                                    avail.getEndTime()))
                            .collect(Collectors.toList());

                    return new MentorDisciplineAvailabilityRepresentation(
                            disciplineName,
                            courseName,
                            slots);
                })
                .sorted(Comparator.comparing(MentorDisciplineAvailabilityRepresentation::disciplineName))
                .collect(Collectors.toList());

        log.info("Adapter: Successfully mapped mentor profile for ID: {}", mentorId);

        MentorProfileRepresentation profile = new MentorProfileRepresentation(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                disciplineAvailabilities);
        return Optional.of(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorProfileRepresentation> findAllMentorProfiles() {
        log.debug("Adapter: Attempting to find all mentor profiles.");

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

        List<User> mentors = userRepository.findAllById(mentorIds);
        Map<Long, User> mentorMap = mentors.stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        log.debug("Adapter: Fetched details for {} mentors.", mentors.size());

        List<MentorAvailability> allAvailabilities = mentorAvailabilityRepository.findByUserIdIn(mentorIds);
        log.debug("Adapter: Fetched {} total availabilities for the identified mentors.", allAvailabilities.size());

        Map<Long, List<MentorAvailability>> availabilitiesByMentorId = allAvailabilities.stream()
                .filter(avail -> avail.getUser() != null)
                .collect(Collectors.groupingBy(avail -> avail.getUser().getId()));

        List<MentorProfileRepresentation> mentorProfiles = mentorIds.stream()
                .map(id -> {
                    User mentor = mentorMap.get(id);
                    if (mentor == null)
                        return null;

                    List<MentorAvailability> mentorAvailabilities = availabilitiesByMentorId.getOrDefault(id,
                            Collections.emptyList());

                    Map<Discipline, List<MentorAvailability>> groupedByDiscipline = mentorAvailabilities.stream()
                            .filter(avail -> avail.getDiscipline() != null)
                            .collect(Collectors.groupingBy(MentorAvailability::getDiscipline));

                    List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = groupedByDiscipline
                            .entrySet().stream()
                            .map(entry -> {
                                Discipline discipline = entry.getKey();
                                CourseArea courseArea = discipline.getCourseArea(); // Assuming Discipline has
                                                                                    // getCourseArea()
                                String courseName = (courseArea != null) ? courseArea.getCourse()
                                        : "[Curso não definido]";
                                String disciplineName = discipline.getDisciplineName();

                                List<AvailabilitySlotRepresentation> slots = entry.getValue().stream()
                                        .map(avail -> new AvailabilitySlotRepresentation(
                                                avail.getDayOfWeek(),
                                                avail.getStartTime(),
                                                avail.getEndTime()))
                                        .collect(Collectors.toList());

                                return new MentorDisciplineAvailabilityRepresentation(
                                        disciplineName,
                                        courseName,
                                        slots);
                            })
                            .sorted(Comparator.comparing(MentorDisciplineAvailabilityRepresentation::disciplineName))
                            .collect(Collectors.toList());

                    return new MentorProfileRepresentation(
                            mentor.getId(),
                            mentor.getFirstName(),
                            mentor.getLastName(),
                            disciplineAvailabilities);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Adapter: Successfully mapped {} mentor profiles.", mentorProfiles.size());
        return mentorProfiles;
    }

    private TutoringParticipationRepresentation mapToTutoringParticipationRepresentation(Tutoring tutoring) {
        if (tutoring == null) {
            return null;
        }

        String mentorName = "[Mentor Inválido]";
        Long mentorId = null;
        if (tutoring.getMentor() != null) {
            try {
                mentorId = tutoring.getMentor().getId();
                mentorName = tutoring.getMentor().getFirstName() + " " + tutoring.getMentor().getLastName();
            } catch (EntityNotFoundException e) {
                log.warn(
                        "Mentor (ID: {}) associado à tutoria {} não encontrado durante o mapeamento para participação.",
                        tutoring.getMentor() != null ? tutoring.getMentor().getId() : "null", tutoring.getId());
            }
        }

        String disciplineName = "[Disciplina Inválida]";
        Long disciplineId = null;
        if (tutoring.getDiscipline() != null) {
            try {
                disciplineId = tutoring.getDiscipline().getId();
                disciplineName = tutoring.getDiscipline().getDisciplineName();
            } catch (EntityNotFoundException e) {
                log.warn(
                        "Disciplina (ID: {}) associada à tutoria {} não encontrada durante o mapeamento para participação.",
                        tutoring.getDiscipline() != null ? tutoring.getDiscipline().getId() : "null", tutoring.getId());
            }
        }

        List<String> topicsList = new ArrayList<>();
        int numberOfParticipants = 0;
        if (tutoring.getTopics() != null && !tutoring.getTopics().isEmpty()) {
            numberOfParticipants = tutoring.getTopics().size();
            topicsList = tutoring.getTopics().stream()
                    .map(TutoringParticipants::getTopic)
                    .collect(Collectors.toList());
        }

        String formattedStartTime = tutoring.getStartTime() != null
                ? tutoring.getStartTime().format(ADAPTER_TIME_FORMATTER)
                : null;
        String formattedEndTime = tutoring.getEndTime() != null ? tutoring.getEndTime().format(ADAPTER_TIME_FORMATTER)
                : null;
        String formattedTutoringDate = tutoring.getTutoringDate() != null
                ? tutoring.getTutoringDate().format(ADAPTER_DATE_FORMATTER)
                : null;

        return new TutoringParticipationRepresentation(
                tutoring.getId(),
                mentorId,
                mentorName,
                disciplineId,
                disciplineName,
                tutoring.getTutoringClassType(),
                tutoring.getStatus(),
                formattedStartTime,
                formattedEndTime,
                formattedTutoringDate,
                tutoring.getLocal(),
                tutoring.getLinkVideo(),
                tutoring.getMaxParticipants(),
                numberOfParticipants,
                tutoring.getIsChatEnable(),
                topicsList);
    }

    private UserRepresentation mapToRepresentation(User user) {
        return new UserRepresentation(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getProfileImg(),
                user.getBirthday(),
                CPFUtils.formatar(user.getCpf()),
                user.getPhone(),
                user.getCity(),
                user.getState(),
                user.getCountry(),
                user.getCourseName(),
                user.getSemester(),
                user.getUniversity());
    }

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
            log.error("Adapter: Erro ao acessar disciplina para disponibilidade {}: {}", availability.getId(),
                    e.getMessage());
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
                availability.getEndTime(),
                availability.getIsAvailable(),
                availability.getTutoringClassType());
    }

    private String mapJavaDayOfWeekToDayWeekName(java.time.DayOfWeek javaDayOfWeek) {
        return switch (javaDayOfWeek) {
            case MONDAY -> DayWeek.SEGUNDA_FEIRA.name();
            case TUESDAY -> DayWeek.TERCA_FEIRA.name();
            case WEDNESDAY -> DayWeek.QUARTA_FEIRA.name();
            case THURSDAY -> DayWeek.QUINTA_FEIRA.name();
            case FRIDAY -> DayWeek.SEXTA_FEIRA.name();
            case SATURDAY -> DayWeek.SABADO.name();
            case SUNDAY -> DayWeek.DOMINGO.name();
        };
    }
}