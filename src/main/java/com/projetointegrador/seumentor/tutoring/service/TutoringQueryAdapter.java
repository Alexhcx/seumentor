// src/main/java/com/projetointegrador/seumentor/tutoring/service/TutoringQueryAdapter.java
package com.projetointegrador.seumentor.tutoring.service;

import com.projetointegrador.seumentor.common.enums.DayWeek;
// import com.projetointegrador.seumentor.course.model.CourseArea; // Não usado diretamente aqui, mas o mapper pode usar
import com.projetointegrador.seumentor.course.model.Discipline;
import com.projetointegrador.seumentor.tutoring.api.TutoringQuery;
import com.projetointegrador.seumentor.tutoring.api.dto.*;
import com.projetointegrador.seumentor.tutoring.api.mapper.TutoringMapper;
import com.projetointegrador.seumentor.tutoring.enums.StatusTutoring;
import com.projetointegrador.seumentor.tutoring.model.MentorAvailability;
import com.projetointegrador.seumentor.tutoring.model.Tutoring;
import com.projetointegrador.seumentor.tutoring.model.TutoringParticipants;
import com.projetointegrador.seumentor.tutoring.model.TutoringRating;
import com.projetointegrador.seumentor.tutoring.repository.MentorAvailabilityRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringParticipantsRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRatingRepository;
import com.projetointegrador.seumentor.tutoring.repository.TutoringRepository;
import com.projetointegrador.seumentor.user.api.UserQuery;
import com.projetointegrador.seumentor.user.api.dtos.UserRepresentation;
import com.projetointegrador.seumentor.user.exception.UserNotFoundException;
import com.projetointegrador.seumentor.user.model.User;

import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class TutoringQueryAdapter implements TutoringQuery {

    private final TutoringRepository tutoringRepository;
    private final TutoringRatingRepository tutoringRatingRepository;
    private final TutoringParticipantsRepository tutoringParticipantsRepository;
    private final MentorAvailabilityRepository mentorAvailabilityRepository;
    private final TutoringMapper tutoringMapper;
    private final UserQuery userQuery;

    private static final Logger log = LoggerFactory.getLogger(TutoringQueryAdapter.class);

    @Autowired
    public TutoringQueryAdapter(TutoringRepository tutoringRepository,
                                TutoringRatingRepository tutoringRatingRepository,
                                TutoringParticipantsRepository tutoringParticipantsRepository,
                                MentorAvailabilityRepository mentorAvailabilityRepository,
                                TutoringMapper tutoringMapper,
                                UserQuery userQuery) {
        this.tutoringRepository = tutoringRepository;
        this.tutoringRatingRepository = tutoringRatingRepository;
        this.tutoringParticipantsRepository = tutoringParticipantsRepository;
        this.mentorAvailabilityRepository = mentorAvailabilityRepository;
        this.tutoringMapper = tutoringMapper;
        this.userQuery = userQuery;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsTutoringForDiscipline(Long disciplineId) {
        return tutoringRepository.existsByDisciplineId(disciplineId);
    }

    private TutoringRepresentation enrichTutoringRepresentation(Tutoring tutoring) {
        if (tutoring == null) return null;
        TutoringRepresentation rep = tutoringMapper.toTutoringRepresentation(tutoring);
        if (rep != null) {
            int qtdParticipants = tutoringParticipantsRepository.countByTutoringId(tutoring.getId());
            return new TutoringRepresentation(
                rep.id(), rep.mentorId(), rep.mentorName(), rep.disciplineId(), rep.disciplineName(),
                rep.tutoringClassType(), rep.status(), rep.startTime(), rep.endTime(), rep.tutoringDate(),
                rep.local(), rep.linkVideo(), rep.maxParticipants(), qtdParticipants,
                rep.isChatEnable(), rep.participants()
            );
        }
        return null;
    }
    
    // Mantido caso você opte por alterar a interface TutoringQuery no futuro.
    private TutoringParticipationRepresentation enrichTutoringParticipationRepresentation(Tutoring tutoring) {
        if (tutoring == null) return null;
        TutoringParticipationRepresentation rep = tutoringMapper.toTutoringParticipationRepresentation(tutoring);
        if (rep != null) {
            int qtdParticipants = tutoringParticipantsRepository.countByTutoringId(tutoring.getId());
            return new TutoringParticipationRepresentation(
                rep.id(), rep.mentorId(), rep.mentorName(), rep.disciplineId(), rep.disciplineName(),
                rep.tutoringClassType(), rep.status(), rep.startTime(), rep.endTime(), rep.tutoringDate(),
                rep.local(), rep.linkVideo(), rep.maxParticipants(), qtdParticipants,
                rep.isChatEnable(), rep.topics()
            );
        }
        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TutoringRepresentation> findTutoringById(Long tutoringId) {
        log.debug("TutoringQueryAdapter: Finding Tutoring by ID: {}", tutoringId);
        // LEMBRETE: Assegure que o método findById no TutoringRepository (ou um método customizado como findByIdWithDetails)
        // realize o fetch das entidades relacionadas (mentor, discipline, topics, topics.user) para evitar LazyInitializationExceptions
        // ou N+1 queries. Isso pode ser feito com @EntityGraph ou uma @Query com JOIN FETCH.
        return tutoringRepository.findById(tutoringId) 
                .map(this::enrichTutoringRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findFilteredTutorings(Long mentorId, Long disciplineId, StatusTutoring status) {
        log.debug("TutoringQueryAdapter: Finding Tutorings with filters - MentorId: {}, DisciplineId: {}, Status: {}", mentorId, disciplineId, status);
        Specification<Tutoring> spec = TutoringSpecifications.buildSpecification(mentorId, disciplineId, status);
        List<Tutoring> tutorings = tutoringRepository.findAll(spec); 
        return tutorings.stream()
                .map(this::enrichTutoringRepresentation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    static class TutoringSpecifications {
        public static Specification<Tutoring> buildSpecification(Long mentorId, Long disciplineId, StatusTutoring status) {
            return (root, query, criteriaBuilder) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (mentorId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("mentor").get("id"), mentorId));
                }
                if (disciplineId != null) {
                    predicates.add(criteriaBuilder.equal(root.get("discipline").get("id"), disciplineId));
                }
                if (status != null) {
                    predicates.add(criteriaBuilder.equal(root.get("status"), status));
                }
                if (query.getResultType() != Long.class && query.getResultType() != long.class) { 
                     root.fetch("mentor", jakarta.persistence.criteria.JoinType.LEFT);
                     root.fetch("discipline", jakarta.persistence.criteria.JoinType.LEFT);
                     // Considere adicionar fetch para 'topics' e 'topics.user' se forem frequentemente acessados após esta query
                     // root.fetch("topics", jakarta.persistence.criteria.JoinType.LEFT).fetch("user", jakarta.persistence.criteria.JoinType.LEFT);
                }
                query.distinct(true); 
                return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
            };
        }

        public static Specification<Tutoring> buildAvailableSlotsSpecification(LocalDate date, Optional<Long> disciplineId, Long requestingUserId) {
             return (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                predicates.add(cb.equal(root.get("tutoringDate"), date));
                if (requestingUserId != null) { 
                    predicates.add(cb.notEqual(root.get("mentor").get("id"), requestingUserId));
                }
                disciplineId.ifPresent(discId -> predicates.add(cb.equal(root.get("discipline").get("id"), discId)));
                predicates.add(root.get("status").in(StatusTutoring.AGENDADA));

                if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                     root.fetch("mentor", jakarta.persistence.criteria.JoinType.LEFT);
                     root.fetch("discipline", jakarta.persistence.criteria.JoinType.LEFT);
                }
                query.distinct(true);
                return cb.and(predicates.toArray(new Predicate[0]));
            };
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findAllTutoringsByMentorId(Long mentorId) {
        log.debug("TutoringQueryAdapter: Finding all tutorings for mentor ID: {}", mentorId);
        if (mentorId == null) return Collections.emptyList();
        
        // Utilizando a Specification para consistência no fetch
        List<Tutoring> tutorings = tutoringRepository.findAll(TutoringSpecifications.buildSpecification(mentorId, null, null));
        return tutorings.stream()
                .map(this::enrichTutoringRepresentation)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ***** MÉTODO CORRIGIDO (ASSUMINDO QUE ESTE É O DA LINHA 188 INDICADA NO ERRO) *****
    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findAllTutoringsByParticipantId(Long userId) { // Nome e tipo de retorno alinhados com a interface TutoringQuery
        log.debug("TutoringQueryAdapter: Finding all tutorings by participant ID: {}", userId); // Mensagem de log atualizada
        if (userId == null) return Collections.emptyList();
        
        // LEMBRETE: Assegure que TutoringParticipantsRepository.findByUserId
        // (ou uma query customizada) faça o fetch eficiente de Tutoring e seus detalhes (mentor, discipline)
        // para evitar N+1 no mapeamento subsequente.
        // Por exemplo, em TutoringParticipantsRepository:
        // @Query("SELECT tp FROM TutoringParticipants tp JOIN FETCH tp.tutoring t JOIN FETCH t.mentor JOIN FETCH t.discipline WHERE tp.user.id = :userId")
        // List<TutoringParticipants> findByUserIdWithFetchedTutoringAndDetails(@Param("userId") Long userId);
        List<TutoringParticipants> participations = tutoringParticipantsRepository.findByUserId(userId); 
        
        return participations.stream()
                .map(TutoringParticipants::getTutoring) 
                .filter(Objects::nonNull) // Garante que a tutoria associada não é nula
                .distinct() // Evita duplicatas se houver várias participações na mesma tutoria (improvável)
                .map(this::enrichTutoringRepresentation) // Mapeia para TutoringRepresentation
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRatingRepresentation> findAllTutoringRatings() {
        log.debug("TutoringQueryAdapter: Finding all tutoring ratings.");
        // LEMBRETE: Assegure o fetch eficiente em TutoringRatingRepository.findAll()
        // (e.g., @EntityGraph ou @Query com JOIN FETCH para tutoring, tutoring.mentor, tutoring.topics.user)
        List<TutoringRating> ratings = tutoringRatingRepository.findAll(); 
        return ratings.stream()
                .map(rating -> {
                    Long raterUserId = null;
                    if (rating.getTutoring() != null && rating.getTutoring().getTopics() != null && !rating.getTutoring().getTopics().isEmpty()) {
                        User mentor = rating.getTutoring().getMentor();
                        raterUserId = rating.getTutoring().getTopics().stream()
                            .map(TutoringParticipants::getUser)
                            .filter(Objects::nonNull)
                            .filter(user -> mentor == null || (user.getId() != null && (mentor.getId() == null || !user.getId().equals(mentor.getId()))))
                            .map(User::getId)
                            .findFirst().orElse(null); 
                    }
                    return tutoringMapper.toTutoringRatingRepresentation(rating, raterUserId);
                })
                .collect(Collectors.toList());
    }

    @Override
    public TutoringRatingRepresentation mapToRatingRepresentation(TutoringRating rating, Long raterUserId) {
        return tutoringMapper.toTutoringRatingRepresentation(rating, raterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findTutoringAndAvailabilityByDate(LocalDate date) {
        log.info("TutoringQueryAdapter: Finding Tutorings and Availabilities for date: {}", date);
        List<TutoringRepresentation> results = new ArrayList<>();

        Specification<Tutoring> specTutoring = TutoringSpecifications.buildSpecification(null, null, null)
                                               .and((root, query, cb) -> cb.equal(root.get("tutoringDate"), date));
        
        List<Tutoring> tutoringsOnDate = tutoringRepository.findAll(specTutoring);
        results.addAll(tutoringsOnDate.stream()
            .map(this::enrichTutoringRepresentation)
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
        );
        // A chamada abaixo usa o método corrigido/referenciado
        List<MentorAvailability> availabilitiesOnDay = findAllAvailabilitiesByDayOfWeek(mapJavaDayOfWeekToDayWeekEnum(date.getDayOfWeek()));
        
        results.addAll(availabilitiesOnDay.stream()
                .filter(avail -> Boolean.TRUE.equals(avail.getIsAvailable()))
                .map(avail -> tutoringMapper.availabilityToTutoringRepresentation(avail, date))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList()));
        
        results.sort(Comparator.comparing(TutoringRepresentation::startTime, Comparator.nullsLast(String::compareTo)));
        log.info("TutoringQueryAdapter: Returning {} combined Tutorings/Availabilities for date {}", results.size(), date);
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TutoringRepresentation> findAvailableSlotsForUser(LocalDate date, Optional<Long> disciplineId, Long requestingUserId) {
        Specification<Tutoring> specConcrete = TutoringSpecifications.buildAvailableSlotsSpecification(date, disciplineId, requestingUserId);
        List<Tutoring> concreteTutoringsFromOthers = tutoringRepository.findAll(specConcrete);

        List<TutoringRepresentation> availableSlots = concreteTutoringsFromOthers.stream()
                .filter(tutoring -> isTutoringAvailableForUser(tutoring, requestingUserId))
                .map(this::enrichTutoringRepresentation)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(ArrayList::new));

        Set<String> concreteTutoringKeys = generateConcreteTutoringKeys(concreteTutoringsFromOthers, date);
        
        List<MentorAvailability> allAvailabilitiesOnDay = findAllAvailabilitiesByDayOfWeek(mapJavaDayOfWeekToDayWeekEnum(date.getDayOfWeek()));

        allAvailabilitiesOnDay.stream()
                .filter(avail -> isAvailabilityRelevantForSlots(avail, requestingUserId, disciplineId))
                .filter(avail -> !isAvailabilitySuperseded(avail, concreteTutoringKeys, date))
                .map(avail -> tutoringMapper.availabilityToTutoringRepresentation(avail, date))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(availableSlots::add);

        availableSlots.sort(Comparator.comparing(TutoringRepresentation::startTime, Comparator.nullsLast(String::compareTo)));
        return availableSlots;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserAvailabilityRepresentation> findMentorAvailabilityRepresentationById(Long availabilityId) {
        log.debug("TutoringQueryAdapter: Finding availability representation by ID: {}", availabilityId);
        // LEMBRETE: Assegure que MentorAvailabilityRepository.findById faça fetch da disciplina.
        // (e.g., @EntityGraph(attributePaths = {"discipline"}) em findById)
        return mentorAvailabilityRepository.findById(availabilityId) 
                .map(tutoringMapper::toUserAvailabilityRepresentation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAvailabilityRepresentation> findMentorAvailabilitiesByMentorId(Long mentorId) {
        log.debug("TutoringQueryAdapter: Finding availability representations for mentor ID: {}", mentorId);
        userQuery.findById(mentorId).orElseThrow(() -> new UserNotFoundException("Mentor não encontrado com ID: " + mentorId));
        // LEMBRETE: Assegure que MentorAvailabilityRepository.findByUserId faça fetch das disciplinas.
        // (e.g., @EntityGraph(attributePaths = {"discipline"}) em findByUserId)
        List<MentorAvailability> availabilities = mentorAvailabilityRepository.findByUserId(mentorId); 
        return tutoringMapper.toUserAvailabilityRepresentationList(availabilities);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MentorProfileRepresentation> findMentorProfileByMentorId(Long mentorId) {
        log.debug("TutoringQueryAdapter: Finding mentor profile for mentor ID: {}", mentorId);
        return userQuery.findById(mentorId).map(userRep -> {
            // LEMBRETE: Assegure que MentorAvailabilityRepository.findByUserId faça fetch de discipline e discipline.courseArea.
            // (e.g., @EntityGraph(attributePaths = {"discipline", "discipline.courseArea"}) em findByUserId)
            List<MentorAvailability> mentorAvailabilities = mentorAvailabilityRepository.findByUserId(mentorId); 
            
            Map<Discipline, List<MentorAvailability>> groupedByDiscipline = mentorAvailabilities.stream()
                    .filter(avail -> avail.getDiscipline() != null) 
                    .collect(Collectors.groupingBy(MentorAvailability::getDiscipline));

            List<MentorDisciplineAvailabilityRepresentation> disciplineAvailabilities = groupedByDiscipline.entrySet().stream()
                .map(entry -> tutoringMapper.toMentorDisciplineAvailabilityRepresentation(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(MentorDisciplineAvailabilityRepresentation::disciplineName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());
            
            return new MentorProfileRepresentation(
                userRep.id(),
                userRep.firstName(),
                userRep.lastName(),
                disciplineAvailabilities
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public List<MentorProfileRepresentation> findAllMentorProfiles() {
        log.debug("TutoringQueryAdapter: Finding all mentor profiles.");
        // LEMBRETE: Crie um método eficiente em MentorAvailabilityRepository para buscar IDs distintos de usuários (mentores).
        List<Long> mentorUserIds = mentorAvailabilityRepository.findAll().stream() // Implementação provisória
                                       .filter(ma -> ma.getUser() != null && ma.getUser().getId() != null)
                                       .map(ma -> ma.getUser().getId())
                                       .distinct()
                                       .collect(Collectors.toList());
        if (mentorUserIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        // LEMBRETE: Crie um método eficiente em UserQuery (e sua implementação) para buscar UserRepresentations por uma lista de IDs.
        Map<Long, UserRepresentation> userRepMap;
        List<UserRepresentation> mentorUserReps = userQuery.findAllUserRepresentations().stream()
             .filter(u -> mentorUserIds.contains(u.id()))
             .collect(Collectors.toList()); 
        userRepMap = mentorUserReps.stream().collect(Collectors.toMap(UserRepresentation::id, ur -> ur));

        // LEMBRETE: Crie um método eficiente em MentorAvailabilityRepository para buscar por lista de User IDs com fetch de discipline e courseArea.
        List<MentorAvailability> allAvailabilities = mentorAvailabilityRepository.findByUserIdIn(mentorUserIds); 

        Map<Long, List<MentorAvailability>> availabilitiesByMentorId = allAvailabilities.stream()
            .filter(avail -> avail.getUser() != null)
            .collect(Collectors.groupingBy(avail -> avail.getUser().getId()));

        return mentorUserIds.stream()
            .map(id -> {
                UserRepresentation userRep = userRepMap.get(id);
                if (userRep == null) return null;

                List<MentorAvailability> mentorAvs = availabilitiesByMentorId.getOrDefault(id, Collections.emptyList());
                 Map<Discipline, List<MentorAvailability>> groupedByDiscipline = mentorAvs.stream()
                    .filter(avail -> avail.getDiscipline() != null)
                    .collect(Collectors.groupingBy(MentorAvailability::getDiscipline));

                List<MentorDisciplineAvailabilityRepresentation> disciplineAvs = groupedByDiscipline.entrySet().stream()
                    .map(entry -> tutoringMapper.toMentorDisciplineAvailabilityRepresentation(entry.getKey(), entry.getValue()))
                    .sorted(Comparator.comparing(MentorDisciplineAvailabilityRepresentation::disciplineName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

                return new MentorProfileRepresentation(
                    userRep.id(), userRep.firstName(), userRep.lastName(), disciplineAvs
                );
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    // Este é o método que estava causando o erro de compilação original devido à falta em MentorAvailabilityRepository.
    // Corrigido para chamar o método existente findByDayOfWeek.
    @Override
    @Transactional(readOnly = true)
    public List<MentorAvailability> findAllAvailabilitiesByDayOfWeek(DayWeek dayOfWeek) {
        log.debug("TutoringQueryAdapter: Finding all mentor availabilities for day: {}", dayOfWeek);
        // LEMBRETE: Adicionar @EntityGraph(attributePaths = {"user", "discipline"})
        // ao método findByDayOfWeek em MentorAvailabilityRepository se o carregamento eager for necessário
        // para que o tutoringMapper.availabilityToTutoringRepresentation funcione corretamente sem N+1.
        return mentorAvailabilityRepository.findByDayOfWeek(dayOfWeek); 
    }

    private DayWeek mapJavaDayOfWeekToDayWeekEnum(java.time.DayOfWeek javaDayOfWeek) {
        if (javaDayOfWeek == null) {
            throw new IllegalArgumentException("java.time.DayOfWeek não pode ser nulo para mapeamento.");
        }
        try {
            return DayWeek.valueOf(javaDayOfWeek.name()); 
        } catch (IllegalArgumentException e) {
            log.error("Erro ao mapear java.time.DayOfWeek {} para enum DayWeek. Verifique se os nomes correspondem.", javaDayOfWeek.name(), e);
            throw new IllegalArgumentException("Mapeamento falhou para o dia da semana: " + javaDayOfWeek.name(), e);
        }
    }

    private boolean isTutoringAvailableForUser(Tutoring tutoring, Long requestingUserId) {
        if (tutoring == null || requestingUserId == null || tutoring.getStatus() != StatusTutoring.AGENDADA) return false;
        long currentParticipants = tutoringParticipantsRepository.countByTutoringId(tutoring.getId());
        boolean isFull = tutoring.getMaxParticipants() != null && currentParticipants >= tutoring.getMaxParticipants();
        if (isFull) return false;
        return !tutoringParticipantsRepository.existsByTutoringIdAndUserId(tutoring.getId(), requestingUserId);
    }

    private Set<String> generateConcreteTutoringKeys(List<Tutoring> tutorings, LocalDate date) {
        return tutorings.stream()
            .map(tutoring -> {
                Long mentorIdKey = (tutoring.getMentor() != null && tutoring.getMentor().getId() != null) ? tutoring.getMentor().getId() : -1L;
                Long disciplineIdKey = (tutoring.getDiscipline() != null && tutoring.getDiscipline().getId() != null) ? tutoring.getDiscipline().getId() : -1L;
                String startTimeStr = tutoring.getStartTime() != null ? tutoring.getStartTime().format(TutoringMapper.TIME_FORMATTER) : "null";
                String endTimeStr = tutoring.getEndTime() != null ? tutoring.getEndTime().format(TutoringMapper.TIME_FORMATTER) : "null";
                String dateStr = date != null ? date.format(TutoringMapper.DATE_FORMATTER) : "null-date";
                return String.format("%d-%d-%s-%s-%s", mentorIdKey, disciplineIdKey, dateStr, startTimeStr, endTimeStr);
            })
            .collect(Collectors.toSet());
    }

    private boolean isAvailabilityRelevantForSlots(MentorAvailability avail, Long requestingUserId, Optional<Long> disciplineId) {
        if (!Boolean.TRUE.equals(avail.getIsAvailable())) return false;
        if (avail.getUser() == null || avail.getUser().getId() == null) return false; 
        if (requestingUserId != null && avail.getUser().getId().equals(requestingUserId)) return false;
        
        if (disciplineId.isPresent()) {
            if (avail.getDiscipline() == null || avail.getDiscipline().getId() == null || 
                !avail.getDiscipline().getId().equals(disciplineId.get())) {
                return false;
            }
        }
        if (avail.getStartTime() == null || avail.getEndTime() == null) {
            log.warn("Disponibilidade ID {} com tempo de início ou fim nulo encontrada.", (avail.getId() != null ? avail.getId() : "N/A"));
            return false;
        }
        return true;
    }

    private boolean isAvailabilitySuperseded(MentorAvailability avail, Set<String> concreteTutoringKeys, LocalDate date) {
        Long availMentorId = (avail.getUser() != null && avail.getUser().getId() != null) ? avail.getUser().getId() : -1L;
        Long availDisciplineId = (avail.getDiscipline() != null && avail.getDiscipline().getId() != null) ? avail.getDiscipline().getId() : -1L;
        String availStartTimeStr = avail.getStartTime() != null ? avail.getStartTime().format(TutoringMapper.TIME_FORMATTER) : "null";
        String availEndTimeStr = avail.getEndTime() != null ? avail.getEndTime().format(TutoringMapper.TIME_FORMATTER) : "null";
        String dateStr = date != null ? date.format(TutoringMapper.DATE_FORMATTER) : "null-date";

        String availabilityKey = String.format("%d-%d-%s-%s-%s", availMentorId, availDisciplineId, dateStr, availStartTimeStr, availEndTimeStr);
        
        boolean isSuperseded = concreteTutoringKeys.contains(availabilityKey);
        if (isSuperseded) {
            log.trace("TutoringQueryAdapter: Availability (Key: {}) is superseded by a concrete tutoring slot.", availabilityKey);
        }
        return isSuperseded;
    }
}